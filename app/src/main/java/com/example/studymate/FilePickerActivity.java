package com.example.studymate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.tom_roush.pdfbox.rendering.ImageType;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FilePickerActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<File> pdfFiles = new ArrayList<>();

    // PdfRenderer, ParcelFileDescriptor 전역 선언
    private PdfRenderer renderer = null;
    private ParcelFileDescriptor fileDescriptor = null;

    private static final String API_KEY = BuildConfig.AZURE_OCR_API_KEY;
    private static final String ENDPOINT = BuildConfig.AZURE_OCR_ENDPOINT;

    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        listView = new ListView(this);
        setContentView(listView);

        File dir = getExternalFilesDir(null);
        File[] files = dir != null ? dir.listFiles() : null;

        ArrayList<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase().endsWith(".pdf")) {
                    pdfFiles.add(f);
                    fileNames.add(f.getName());
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedPdf = pdfFiles.get(position);
            processPdfAndRunOCR(selectedPdf);
        });
    }

    private void processPdfAndRunOCR(File pdfFile) {
        new Thread(() -> {
            try {
                // PdfBox-Android로 문서 열기
                PDDocument document = PDDocument.load(pdfFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();
                StringBuilder[] pageTexts = new StringBuilder[pageCount];

                // 첫 페이지부터 순차 OCR
                runOnUiThread(() -> sendOCRSequentially_PdfBox(pdfFile.getName(), pdfRenderer, document, pageTexts, 0, pageCount));
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "PDF 처리 실패", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    // PdfBox로 한 페이지씩 이미지 뽑아서 OCR
    private void sendOCRSequentially_PdfBox(String pdfName, PDFRenderer pdfRenderer, PDDocument document, StringBuilder[] pageTexts, int currentPage, int totalPages) {
        if (currentPage >= totalPages) {
            StringBuilder allText = new StringBuilder();
            for (StringBuilder page : pageTexts) {
                if (page != null) allText.append(page);
            }
            saveTextAndLaunchSummary(pdfName, allText.toString());
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            int dpi = 300; // 선명도를 위한 고해상도(권장 200~300)
            Bitmap bitmap = pdfRenderer.renderImageWithDPI(currentPage, dpi, ImageType.ARGB); // OK!

            // 디버깅용 임시 저장
            File tempFile = new File(getExternalFilesDir(null), "test_page_" + currentPage + ".png");
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            Log.d("OCR", "Saved bitmap to: " + tempFile.getAbsolutePath());

            byte[] imageBytes = Utils.bitmapToByteArray(bitmap);
            Log.d("OCR", "Bitmap byte size: " + imageBytes.length);

            // 10MB 넘으면 스킵
            if (imageBytes.length > 10 * 1024 * 1024) {
                Log.e("OCR", "Bitmap too large, skipping page " + currentPage);
                pageTexts[currentPage] = new StringBuilder("[페이지 " + (currentPage + 1) + "]: 이미지 크기 초과로 OCR 생략\n\n");
                sendOCRSequentially_PdfBox(pdfName, pdfRenderer, document, pageTexts, currentPage + 1, totalPages);
                return;
            }

            sendOCRAsyncSequential_PdfBox(imageBytes, currentPage, pageTexts, totalPages, pdfName, pdfRenderer, document);
        } catch (Exception e) {
            Log.e("OCR", "PdfBox render error", e);
            pageTexts[currentPage] = new StringBuilder("[페이지 " + (currentPage + 1) + "]: 이미지 추출 실패\n\n");
            sendOCRSequentially_PdfBox(pdfName, pdfRenderer, document, pageTexts, currentPage + 1, totalPages);
        }
    }


    // OCR 요청
    private void sendOCRAsyncSequential_PdfBox(byte[] imageBytes, int pageIndex,
                                               StringBuilder[] pageTexts, int totalPages, String pdfName,
                                               PDFRenderer pdfRenderer, PDDocument document) {
        RequestBody body = RequestBody.create(imageBytes, MediaType.parse("application/octet-stream"));
        Request request = new Request.Builder()
                .url(ENDPOINT + "vision/v3.2/read/analyze")
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Content-Type", "application/octet-stream")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                String operationLocation = response.header("Operation-Location");
                if (operationLocation == null) return;

                pollForResultSequential_PdfBox(operationLocation, pageIndex, pageTexts, totalPages, pdfName, pdfRenderer, document);
            }
        });
    }

    // OCR 결과 polling (기존과 동일)
    private void pollForResultSequential_PdfBox(final String operationLocation, final int pageIndex,
                                                final StringBuilder[] pageTexts, final int totalPages,
                                                final String pdfName, final PDFRenderer pdfRenderer, final PDDocument document) {
        final int[] attempts = {0};
        final int maxAttempts = 20;
        final Handler handler = new Handler(getMainLooper());

        Runnable[] pollTask = new Runnable[1];
        pollTask[0] = new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url(operationLocation)
                        .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }

                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(response.body().string());
                                String status = json.optString("status", "");
                                Log.d("OCR", "Polling status: " + status + " (pageIndex=" + pageIndex + ", attempt=" + attempts[0] + ")");
                                if ("succeeded".equals(status)) {
                                    JSONArray lines = json.getJSONObject("analyzeResult")
                                            .getJSONArray("readResults")
                                            .getJSONObject(0)
                                            .getJSONArray("lines");

                                    Log.d("OCR", "lines count: " + lines.length() + " (pageIndex=" + pageIndex + ")");
                                    StringBuilder result = new StringBuilder();
                                    result.append("📄 Page ").append(pageIndex + 1).append(":\n");
                                    for (int i = 0; i < lines.length(); i++) {
                                        String text = lines.getJSONObject(i).getString("text");
                                        Log.d("OCR", "line " + i + ": " + text);
                                        result.append(text).append("\n");
                                    }
                                    result.append("\n\n");

                                    pageTexts[pageIndex] = result;
                                    runOnUiThread(() -> sendOCRSequentially_PdfBox(pdfName, pdfRenderer, document, pageTexts, pageIndex + 1, totalPages));
                                } else if (attempts[0] < maxAttempts) {
                                    attempts[0]++;
                                    handler.postDelayed(pollTask[0], 3000);
                                } else {
                                    Log.e("OCR", "Polling timed out for page " + (pageIndex + 1));
                                    runOnUiThread(() -> Toast.makeText(FilePickerActivity.this, "OCR 변환 시간이 초과되었습니다.", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                Log.e("OCR", "Exception during polling result parsing", e);
                            }
                        }
                    }
                });
            }
        };
        handler.post(pollTask[0]);
    }

    private void saveTextAndLaunchSummary(String pdfName, String content) {
        try {
            String txtName = pdfName.replace(".pdf", "_summary.txt");
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, txtName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();

            runOnUiThread(() -> {
                Intent intent = new Intent(FilePickerActivity.this, SummaryActivity.class);
                intent.putExtra("text", content);
                startActivity(intent);
            });

        } catch (Exception e) {
            Log.e("OCR", "❌ 저장 실패", e);
        }
    }
}
