package com.example.studymate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.widget.ImageButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;

public class SummaryFinalActivity extends AppCompatActivity {

    private Spinner finalSpinner;
    private TextView summaryText;
    private ImageButton saveButton, callButton, homeButton;
    private Button finalSummarizeButton;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_final);

        // 🔗 View 연결
        finalSpinner = findViewById(R.id.finalSpinner);
        summaryText = findViewById(R.id.summaryText);
        saveButton = findViewById(R.id.image_save);
        callButton = findViewById(R.id.callButton);
        homeButton = findViewById(R.id.image_home);
        finalSummarizeButton = findViewById(R.id.btn_final_summarize);

        // 📘 스피너 항목 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.topic_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        finalSpinner.setAdapter(adapter);

        // ✅ 파일 불러오기 처리기 등록
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String newContent = readTextFromUri(uri);
                            String existing = summaryText.getText().toString();
                            summaryText.setText(existing + "\n\n" + newContent);  // ✨ 이어붙이기
                        }
                    }
                }
        );

        // 📂 불러오기 버튼
        callButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        // 🧠 최종 요약 버튼
        finalSummarizeButton.setOnClickListener(v -> {
            String topic = finalSpinner.getSelectedItem().toString();
            String lectureSummary = summaryText.getText().toString();
            String handwrittenText = loadOcrText();

            summaryText.setText("최종 요약 중입니다...");

            GptApiHelper.requestSummary(generateFinalPrompt(topic, lectureSummary, handwrittenText),
                    new GptApiHelper.GptCallback() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> summaryText.setText(result));
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> summaryText.setText("요약 실패: " + error));
                        }
                    });
        });

        // 💾 저장
        saveButton.setOnClickListener(v -> {
            String text = summaryText.getText().toString();
            promptSaveFileNameAndSave(text);
        });

        // 🏠 홈으로
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 전달된 텍스트 표시
        String receivedText = getIntent().getStringExtra("text");
        if (receivedText != null) {
            summaryText.setText(receivedText);
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                String content = readTextFromUri(uri);
                summaryText.setText(content);
            }
        }
    }

    private String generateFinalPrompt(String topic, String lectureSummary, String handwrittenText) {
        return "다음은 강의 요약과 강의 중 작성된 필기 내용입니다. 두 정보를 종합하여 "
                + "해당 주제(" + topic + ")에 대한 최종 요약을 작성해주세요. 다음 형식으로 정리해줘:\n\n"
                + "1. 핵심 개념 및 주요 논점\n"
                + "2. 필기 내용을 반영한 보완 설명\n"
                + "3. 전체 강의의 흐름 요약\n\n"
                + "📚 강의 요약:\n" + lectureSummary + "\n\n"
                + "📝 필기 내용 (OCR 결과):\n" + handwrittenText;
    }

    private String loadOcrText() {
        File file = new File(getExternalFilesDir(null), "ocr_text.txt");
        if (!file.exists()) return "⚠️ OCR 파일 없음.";
        return readTextFromFile(file);
    }

    private String readTextFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString().trim();
        } catch (IOException e) {
            return "⚠️ 파일 읽기 실패: " + e.getMessage();
        }
    }

    private String readTextFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString().trim();
        } catch (IOException e) {
            return "⚠️ URI 읽기 실패: " + e.getMessage();
        }
    }

    private void promptSaveFileNameAndSave(String text) {
        EditText input = new EditText(this);
        input.setHint("파일 이름 입력 (예: summary_final)");

        new android.app.AlertDialog.Builder(this)
                .setTitle("파일 이름 지정")
                .setView(input)
                .setPositiveButton("저장", (dialog, which) -> {
                    String filename = input.getText().toString().trim();
                    if (!filename.endsWith(".txt")) filename += ".txt";
                    try {
                        File file = new File(getExternalFilesDir(null), filename);
                        FileWriter writer = new FileWriter(file);
                        writer.write(text);
                        writer.close();
                        Toast.makeText(this, "✅ 저장됨: " + file.getName(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "❌ 저장 실패", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
