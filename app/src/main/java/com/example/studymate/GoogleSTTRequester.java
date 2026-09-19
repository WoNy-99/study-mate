package com.example.studymate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleSTTRequester {
    private static final String API_KEY = BuildConfig.GOOGLE_STT_API_KEY;

    // 타임아웃을 길게!
    private static OkHttpClient getLongTimeoutClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)   // 2분 연결 대기
                .readTimeout(15, TimeUnit.MINUTES)     // 최대 15분까지 대기 (STT 결과 polling)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();
    }

    // STT 변환 요청
    public static String requestSTT(String gcsUri) throws IOException {
        OkHttpClient client = getLongTimeoutClient();

        String url = "https://speech.googleapis.com/v1/speech:longrunningrecognize?key=" + API_KEY;

        JsonObject config = new JsonObject();
        config.addProperty("encoding", "LINEAR16");
        config.addProperty("languageCode", "ko-KR");
        config.addProperty("sampleRateHertz", 44100); // 녹음 파일 샘플링 속도에 맞춰 조정

        JsonObject audio = new JsonObject();
        audio.addProperty("uri", gcsUri);

        JsonObject reqBody = new JsonObject();
        reqBody.add("config", config);
        reqBody.add("audio", audio);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqBody.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("STT 요청 실패: " + response);

        String responseBody = response.body().string();
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!json.has("name")) throw new IOException("STT 요청 실패: name 없음\n" + responseBody);
        return json.get("name").getAsString();
    }

    // 1시간 이상 대용량 파일을 위한 polling (최대 15분까지 대기, 필요시 더 늘릴 것)
    public static String pollSTTResult(String operationName) throws IOException, InterruptedException {
        OkHttpClient client = getLongTimeoutClient();

        String url = "https://speech.googleapis.com/v1/operations/" + operationName + "?key=" + API_KEY;

        long pollingStart = System.currentTimeMillis();
        long pollingTimeout = 60 * 60 * 1000L; // 1시간(3600초)까지 대기

        while (true) {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("done") && json.get("done").getAsBoolean()) {
                if (json.has("response")) {
                    StringBuilder sb = new StringBuilder();
                    JsonArray results = json.getAsJsonObject("response").getAsJsonArray("results");
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject result = results.get(i).getAsJsonObject();
                        JsonArray alternatives = result.getAsJsonArray("alternatives");
                        if (alternatives.size() > 0) {
                            String transcript = alternatives.get(0).getAsJsonObject().get("transcript").getAsString();
                            sb.append(transcript).append("\n");
                        }
                    }
                    return sb.toString().trim();
                } else if (json.has("error")) {
                    return "STT 변환 실패: " + json.getAsJsonObject("error").toString();
                } else {
                    return "변환 결과 없음";
                }
            }

            // 너무 오래 대기하지 않도록 제한 (ex. 1시간)
            if (System.currentTimeMillis() - pollingStart > pollingTimeout) {
                throw new IOException("STT 변환 시간이 너무 오래 걸립니다 (1시간 초과).");
            }

            // 15초 후 재요청 (너무 짧게 하면 Google이 429 에러 줄 수 있음)
            Thread.sleep(15000);
        }
    }
}
