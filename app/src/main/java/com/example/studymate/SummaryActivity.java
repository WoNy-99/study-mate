package com.example.studymate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SummaryActivity extends AppCompatActivity {
    private ImageButton saveButton, changeButton, homeButton;
    private Button summarizeButton;
    private TextView summaryText;
    private Spinner topicSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        saveButton = findViewById(R.id.saveButton);
        changeButton = findViewById(R.id.btn_edit);
        homeButton = findViewById(R.id.homeButton);
        summarizeButton = findViewById(R.id.btn_summarize);
        summaryText = findViewById(R.id.summaryText);
        topicSpinner = findViewById(R.id.topicSpinner);

        // 스피너 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.topic_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(adapter);

        // 요약 버튼
        summarizeButton.setOnClickListener(v -> {
            String selectedTopic = topicSpinner.getSelectedItem().toString();
            String sttText = summaryText.getText().toString();

            String prompt = generatePrompt(selectedTopic, sttText);
            summaryText.setText("요약 중입니다...");

            GptApiHelper.requestSummary(prompt, new GptApiHelper.GptCallback() {
                @Override public void onSuccess(String result) {
                    runOnUiThread(() -> summaryText.setText(result));
                }

                @Override public void onFailure(String error) {
                    runOnUiThread(() -> summaryText.setText("요약 실패: " + error));
                }
            });
        });

        // 편집 화면 이동
        changeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, ChangeActivity.class);
            intent.putExtra("text", summaryText.getText().toString());
            startActivity(intent);
        });

        // 홈으로 이동
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 저장 버튼 클릭 시 이름 입력 받기
        saveButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("파일 이름을 입력하세요");

            final EditText input = new EditText(this);
            input.setHint("예: summary_2025_05_04");
            builder.setView(input);

            builder.setPositiveButton("저장", (dialog, which) -> {
                String filename = input.getText().toString().trim();
                if (!filename.endsWith(".txt")) filename += ".txt";

                boolean success = saveEditedTextToFile(filename, summaryText.getText().toString());
                if (success) {
                    Toast.makeText(this, "✅ 저장됨: " + filename, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ 저장 실패", Toast.LENGTH_LONG).show();
                }
            });

            builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // 전달받은 텍스트 설정
        String receivedText = getIntent().getStringExtra("text");
        if (receivedText != null) {
            summaryText.setText(receivedText);
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                String content = readTextFromUri(uri);
                summaryText.setText(content);
                return;
            }
        }
    }

    private boolean saveEditedTextToFile(String filename, String text) {
        try {
            File file = new File(getExternalFilesDir(null), filename);
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String readTextFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder textBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                textBuilder.append(line).append("\n");
            }

            return textBuilder.toString().trim();

        } catch (IOException e) {
            e.printStackTrace();
            return "⚠️ 파일 읽기 실패: " + e.getMessage();
        }
    }

    private String generatePrompt(String topic, String lectureText) {
        switch (topic) {
            case "법률":
                return "다음은 법률 분야의 강의 원문입니다. 아래 기준에 따라 핵심 내용을 요약해줘:\n"
                        + "- 핵심 조항: 관련 법률의 주요 조항 및 그 의미\n"
                        + "- 판례: 관련된 판결 사례와 영향\n"
                        + "- 법적 용어: 자주 등장하는 용어와 정의\n\n"
                        + "강의 내용:\n" + lectureText;

            case "의학":
                return "다음은 의학 분야의 강의 원문입니다. 아래 기준에 따라 핵심 내용을 요약해줘:\n"
                        + "- 질병명과 정의\n"
                        + "- 진단 기준 및 절차\n"
                        + "- 주요 치료 방법\n\n"
                        + "강의 내용:\n" + lectureText;

            case "과학":
                return "다음은 과학 분야의 강의 원문입니다. 아래 기준에 따라 요약해줘:\n"
                        + "- 주요 개념의 정의\n"
                        + "- 실험 방법 및 절차\n"
                        + "- 인과 관계 설명\n\n"
                        + "강의 내용:\n" + lectureText;

            case "IT":
                return "다음은 IT 분야의 강의 원문입니다. 아래 기준에 따라 요약해줘:\n"
                        + "- 주요 기술 용어와 정의\n"
                        + "- 시스템 또는 프로세스 흐름\n"
                        + "- 핵심 아키텍처 및 기능\n\n"
                        + "강의 내용:\n" + lectureText;

            case "종교":
                return "다음은 종교 분야의 강의 원문입니다. 아래 기준에 따라 요약해줘:\n"
                        + "- 주요 교리 및 신념\n"
                        + "- 역사적 맥락 또는 발전 배경\n"
                        + "- 타 종교와의 비교 관점\n\n"
                        + "강의 내용:\n" + lectureText;

            default:
                return "다음은 강의 원문입니다. 핵심 개념과 내용을 요점 중심으로 요약해줘.\n\n"
                        + "강의 내용:\n" + lectureText;
        }
    }
}
