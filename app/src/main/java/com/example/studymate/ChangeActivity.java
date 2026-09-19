package com.example.studymate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.app.AlertDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ChangeActivity extends AppCompatActivity {

    private ImageButton saveButton, homeButton, summaryButton;
    private EditText changeInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);

        // 🔗 View 연결
        saveButton = findViewById(R.id.img_save);
        homeButton = findViewById(R.id.img_home);
        summaryButton = findViewById(R.id.img_summary);
        changeInput = findViewById(R.id.editText);

        // 전달된 텍스트 설정
        String receivedText = getIntent().getStringExtra("text");
        if (receivedText != null) {
            changeInput.setText(receivedText);
        }

        // 💾 저장 버튼
        saveButton.setOnClickListener(v -> showFilenameDialogAndSave());

        // 🏠 홈 버튼
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChangeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 📄 요약 화면 이동
        summaryButton.setOnClickListener(v -> {
            String editedText = changeInput.getText().toString();
            Intent intent = new Intent(ChangeActivity.this, SummaryActivity.class);
            intent.putExtra("text", editedText);
            startActivity(intent);
        });
    }

    // 파일명을 입력받는 다이얼로그를 띄우고 저장
    private void showFilenameDialogAndSave() {
        EditText input = new EditText(this);
        input.setHint("저장할 파일명을 입력하세요 (예: edited_note)");

        new AlertDialog.Builder(this)
                .setTitle("파일 이름 입력")
                .setView(input)
                .setPositiveButton("저장", (dialog, which) -> {
                    String filename = input.getText().toString().trim();
                    if (filename.isEmpty()) {
                        Toast.makeText(this, "파일명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // .txt 확장자 자동 추가
                    if (!filename.endsWith(".txt")) {
                        filename += ".txt";
                    }
                    boolean success = saveEditedTextToFile(changeInput.getText().toString(), filename);
                    if (success) {
                        Toast.makeText(this, "✅ 저장됨: " + filename, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ 저장 실패", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.cancel())
                .show();
    }

    // 텍스트 파일 저장 (파일명 지정)
    private boolean saveEditedTextToFile(String text, String filename) {
        try {
            File file = new File(getExternalFilesDir(null), filename);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(text);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
