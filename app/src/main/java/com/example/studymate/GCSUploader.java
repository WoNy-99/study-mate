package com.example.studymate;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GCSUploader {

    /**
     * Signed URL로 GCS에 파일 업로드
     * @param wavFile   업로드할 파일
     * @param signedUrl 사전 서명된 URL (파이썬/Colab 등에서 미리 생성)
     * @throws IOException 업로드 실패시
     */

    public static void uploadWithSignedUrl(File wavFile, String signedUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("audio/wav");
        RequestBody body = RequestBody.create(mediaType, wavFile);

        Request request = new Request.Builder()
                .url(signedUrl)
                .put(body)      // GCS signed URL은 PUT 방식!
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("업로드 실패: " + response.code() + " " + response.message());
        }
    }
}
