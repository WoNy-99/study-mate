package com.example.studymate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;

public class DocumentActivity extends AppCompatActivity {
    private ImageButton saveButton, summaryButton, changeButton;
    private TextView documentText;
    private String filePath;
    private String sttResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

        saveButton = findViewById(R.id.btn_save);
        summaryButton = findViewById(R.id.btn_summarize);
        changeButton = findViewById(R.id.btn_edit);
        documentText = findViewById(R.id.tv_text_result);

        // RecordActivity에서 변환된 STT 결과와 파일 경로를 받음
        filePath = getIntent().getStringExtra("filePath");
        sttResult = getIntent().getStringExtra("stt_result");

        if (sttResult != null && !sttResult.trim().isEmpty()) {
            documentText.setText(sttResult.trim());
            Toast.makeText(this, "STT 변환 완료", Toast.LENGTH_SHORT).show();
        } else {
            documentText.setText("STT 결과가 없습니다.");
            Toast.makeText(this, "STT 결과가 없습니다.", Toast.LENGTH_SHORT).show();
        }

        summaryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SummaryActivity.class);
            intent.putExtra("text", documentText.getText().toString());
            startActivity(intent);
        });

        changeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeActivity.class);
            intent.putExtra("text", documentText.getText().toString());
            startActivity(intent);
        });

        saveButton.setOnClickListener(v -> showFilenameDialogAndSave());
    }

    // 파일명을 입력받는 다이얼로그를 띄우고, 저장
    private void showFilenameDialogAndSave() {
        EditText input = new EditText(this);
        input.setHint("저장할 파일명을 입력하세요 (예: lecture_note)");

        new AlertDialog.Builder(this)
                .setTitle("파일 이름 입력")
                .setView(input)
                .setPositiveButton("저장", (dialog, which) -> {
                    String filename = input.getText().toString().trim();
                    if (filename.isEmpty()) {
                        Toast.makeText(this, "파일명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 확장자 자동 추가(.txt)
                    if (!filename.endsWith(".txt")) {
                        filename += ".txt";
                    }
                    saveTextToFile(documentText.getText().toString(), filename);
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.cancel())
                .show();
    }

    // 입력한 파일명으로 텍스트 저장
    private void saveTextToFile(String text, String filename) {
        try {
            File file = new File(getExternalFilesDir(null), filename);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(text);
            }
            Toast.makeText(this, "저장 완료: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
