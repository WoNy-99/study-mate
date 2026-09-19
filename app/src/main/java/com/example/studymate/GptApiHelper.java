package com.example.studymate;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GptApiHelper {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;

    public interface GptCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public static void requestSummary(String prompt, GptCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        try {
            // 메시지 구성
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);

            // 전체 요청 구성
            JSONObject json = new JSONObject();
            json.put("model", "gpt-4o-mini");
            json.put("messages", new JSONArray().put(message));
            json.put("temperature", 0.7);

            // 요청 본문
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            // 요청 생성
            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            // 비동기 요청 실행
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Unexpected code: " + response);
                        return;
                    }

                    String result = response.body().string();
                    try {
                        JSONObject resJson = new JSONObject(result);
                        String reply = resJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        callback.onSuccess(reply.trim());
                    } catch (Exception e) {
                        callback.onFailure("Parsing error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Request build error: " + e.getMessage());
        }
    }
    // Quiz 생성
    public static void requestQuiz(String prompt, GptCallback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);

            JSONObject json = new JSONObject();
            json.put("model", "gpt-4o-mini");
            json.put("messages", new JSONArray().put(message));
            json.put("temperature", 0.7);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("GPT 응답 실패: " + response);
                        return;
                    }

                    String result = response.body().string();
                    try {
                        JSONObject resJson = new JSONObject(result);
                        String content = resJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        callback.onSuccess(content.trim());
                    } catch (Exception e) {
                        callback.onFailure("GPT 응답 파싱 오류: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("요청 생성 오류: " + e.getMessage());
        }
    }

}
