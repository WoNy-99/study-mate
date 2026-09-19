package com.example.studymate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnRecord, btnStorage, btnSchedule, btnOCR, btnFinal, btnQuiz;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> quizFilePickerLauncher;

    private static final String API_KEY = BuildConfig.AZURE_OCR_API_KEY;
    private static final String ENDPOINT = BuildConfig.AZURE_OCR_ENDPOINT;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.circle_button_record);
        btnStorage = findViewById(R.id.circle_button_open_storage);
        btnSchedule = findViewById(R.id.circle_button_schedule);
        btnOCR = findViewById(R.id.circle_button_ocr);
        btnFinal = findViewById(R.id.circle_button_final);
        btnQuiz = findViewById(R.id.circle_button_quiz);

        setButtonAppearance(R.id.btn_record, R.drawable.ic_mic_white, "녹음");
        setButtonAppearance(R.id.btn_open_storage, R.drawable.ic_folder_white, "저장소");
        setButtonAppearance(R.id.btn_schedule, R.drawable.ic_calendar_white, "시간표");
        setButtonAppearance(R.id.btn_ocr, R.drawable.ic_ocr_white, "OCR");
        setButtonAppearance(R.id.btn_final, R.drawable.ic_summary_white, "최종 요약");
        setButtonAppearance(R.id.btn_quiz, R.drawable.ic_quiz_white, "퀴즈");

        btnRecord.setOnClickListener(v -> startActivity(new Intent(this, RecordActivity.class)));
        btnSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        btnOCR.setOnClickListener(v -> startActivity(new Intent(this, FilePickerActivity.class)));
        btnFinal.setOnClickListener(v -> startActivity(new Intent(this, SummaryFinalActivity.class)));

        btnStorage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        Intent summaryIntent = new Intent(this, SummaryActivity.class);
                        summaryIntent.setAction(Intent.ACTION_VIEW);
                        summaryIntent.setData(uri);
                        startActivity(summaryIntent);
                    }
                });

        btnQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            quizFilePickerLauncher.launch(Intent.createChooser(intent, "요약문 파일 선택"));
        });

        quizFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Intent intent = new Intent(this, QuizActivity.class);
                            intent.setData(uri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "파일 URI를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        TextView desc = findViewById(R.id.manual_description);
        LinearLayout manualRecord = findViewById(R.id.Manual_Record);
        LinearLayout manualSchedule = findViewById(R.id.Manual_Schedule);
        LinearLayout manualOCR = findViewById(R.id.Manual_OCR);
        LinearLayout manualSummary = findViewById(R.id.Manual_Summary);
        LinearLayout manualQuiz = findViewById(R.id.Manual_Quiz);

        manualRecord.setOnClickListener(v -> {
            desc.setText("🎙 녹음 설명서\n\n\n\n\n" +
                    "StudyMate에서는 강의나 공부 내용을 녹음할 수 있습니다." +
                    " 녹음 버튼을 누르면 바로 음성 녹음이 시작되며," +
                    " 녹음이 끝난 후 자동으로 저장소에 파일이 저장됩니다." +
                    " 이 기능은 복습이나 메모용으로 활용할 수 있습니다.");
            desc.setVisibility(View.VISIBLE);
        });
        manualSchedule.setOnClickListener(v -> {
            desc.setText("📅 시간표 설명서 \n\n\n\n\n" +
                    "시간표 기능을 통해 학습 계획이나 과목별 일정을 관리할 수 있습니다." +
                    " 각 요일과 시간대에 과목이나 활동을 등록하고 확인할 수 있어," +
                    " 공부 습관을 체계적으로 관리하는 데 도움이 됩니다." +
                    " 또한, 해당 시간표의 강의 기준 2일전에 복습 알람이 자동으로 설정됩니다!");
            desc.setVisibility(View.VISIBLE);
        });
        manualOCR.setOnClickListener(v -> {
            desc.setText("🔍 OCR 설명서 \n\n\n\n\n" +
                    "OCR 기능은 사진이나 PDF에서 텍스트를 추출해주는 기능입니다." +
                    " 예를 들어 교재나 강의자료의 파일을 업로드하면" +
                    " 해당 이미지에서 글자를 인식하여 텍스트로 변환해줍니다." +
                    " 이렇게 변환된 텍스트는 녹음 요약과 합쳐저 통합 요약이 진행됩니다.");
            desc.setVisibility(View.VISIBLE);
        });
        manualSummary.setOnClickListener(v -> {
            desc.setText("📄 요약 설명서\n\n\n\n" +
                    "요약 기능은 텍스트 내용을 인공지능을 이용해 간결하게 정리해주는 기능입니다." +
                    " StudyMate의 요약은 총 2번 이루어집니다" +
                    " 1차 녹음 요약/ 2차 OCR 필기와 녹음을 합치는 최종 통합 요약으로 이루어집니다" +
                    " OCR 기능과 녹음으로부터 생성된 텍스트를 바탕으로" +
                    " 핵심 내용을 요약하여 학습에 도움이 되도록 제공합니다.");
            desc.setVisibility(View.VISIBLE);
        });
        manualQuiz.setOnClickListener(v -> {
            desc.setText("📝 퀴즈 설명서\n\n\n\n" +
                    "퀴즈 기능은 학습 내용을 복습할 수 있도록 자동으로 문제를 생성해줍니다." +
                    " 최종 통합된 요약 텍스트를 기반으로 인공지능이 객관식 문제를 만들어주며," +
                    " 각 문제에는 보기와 함께 정답과 해설도 포함되어 있어 학습에 효과적입니다." +
                    "\n" +
                    " 퀴즈를 통해 자신이 공부한 내용을 스스로 점검하고," +
                    " 취약한 부분을 파악하세요!");
            desc.setVisibility(View.VISIBLE);
        });
    }

    private void setButtonAppearance(int containerId, int iconResId, String label) {
        View container = findViewById(containerId);
        ImageButton icon = container.findViewById(R.id.circle_button);
        TextView labelView = container.findViewById(R.id.circle_button_label);

        if (icon != null) icon.setImageResource(iconResId);
        if (labelView != null) labelView.setText(label);
    }

    private void readTextAndLaunchQuiz(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder summaryText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                summaryText.append(line).append("\n");
            }
            inputStream.close();

            Intent intent = new Intent(this, QuizActivity.class);
            intent.setData(uri);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "파일 읽기 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private void sendOCRRequest(byte[] imageBytes) {
        RequestBody body = RequestBody.create(imageBytes, MediaType.parse("application/octet-stream"));

        Request request = new Request.Builder()
                .url(ENDPOINT + "vision/v3.2/read/analyze")
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Content-Type", "application/octet-stream")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "OCR 요청 실패", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String operationLocation = response.header("Operation-Location");
                    pollForResult(operationLocation);
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "OCR 응답 실패", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void pollForResult(String operationLocation) {
        Request request = new Request.Builder()
                .url(operationLocation)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .build();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "결과 요청 실패", Toast.LENGTH_SHORT).show());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            JSONArray lines = json.getJSONObject("analyzeResult")
                                    .getJSONArray("readResults")
                                    .getJSONObject(0)
                                    .getJSONArray("lines");

                            StringBuilder resultText = new StringBuilder();
                            for (int i = 0; i < lines.length(); i++) {
                                resultText.append(lines.getJSONObject(i).getString("text")).append("\n");
                            }

                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "✅ OCR 완료:\n" + resultText, Toast.LENGTH_LONG).show();
                                Log.d("OCR 결과", resultText.toString());
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "결과 파싱 실패", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "결과 응답 실패", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }, 3000);
    }
}
