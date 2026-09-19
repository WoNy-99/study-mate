package com.example.studymate;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class QuizActivity extends AppCompatActivity {

    private TextView questionText1, questionText2, questionText3;
    private Spinner optionSpinner1, optionSpinner2, optionSpinner3;
    private Button submitButton;
    private TextView resultText;

    private final String[] correctAnswers = new String[3];
    private final String[] explanations = new String[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // 🔗 View 연결
        questionText1 = findViewById(R.id.questionText1);
        questionText2 = findViewById(R.id.questionText2);
        questionText3 = findViewById(R.id.questionText3);

        optionSpinner1 = findViewById(R.id.optionSpinner1);
        optionSpinner2 = findViewById(R.id.optionSpinner2);
        optionSpinner3 = findViewById(R.id.optionSpinner3);

        submitButton = findViewById(R.id.submitButton);
        resultText = findViewById(R.id.resultText);

        // 📌 content:// URI로부터 텍스트 읽기
        Uri summaryUri = getIntent().getData();
        String summary = readTextFromUri(summaryUri);

        // ✅ 개선된 프롬프트 적용
        String prompt = "다음 텍스트 내용을 기반으로 객관식 퀴즈 3개를 생성해 주세요.\n\n" +
                "각 퀴즈는 반드시 아래의 형식과 예시를 정확히 따르세요. 특히 줄바꿈과 형식을 지켜 주세요.\n\n" +
                "형식:\n" +
                "문제: (문제 내용)\n" +
                "보기: [1. 보기1, 2. 보기2, 3. 보기3, 4. 보기4]\n" +
                "정답: (보기 번호 하나만 예: 2)\n" +
                "해설: (정답에 대한 간단한 이유 설명)\n\n" +
                "예시:\n" +
                "문제: 지구의 위성으로 옳은 것은?\n" +
                "보기: [1. 화성, 2. 금성, 3. 달, 4. 목성]\n" +
                "정답: 3\n" +
                "해설: 달은 지구의 유일한 자연 위성입니다.\n\n" +
                "아래는 퀴즈에 사용할 텍스트입니다:\n" + summary;

        GptApiHelper.requestQuiz(prompt, new GptApiHelper.GptCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d("GPT_RESPONSE", result);  // 디버깅용
                runOnUiThread(() -> parseQuizResult(result));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() ->
                        Toast.makeText(QuizActivity.this, "퀴즈 생성 실패: " + error, Toast.LENGTH_LONG).show());
            }
        });

        // 정답 제출 버튼
        submitButton.setOnClickListener(v -> checkAnswers());
    }

    private String readTextFromUri(Uri uri) {
        StringBuilder text = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (Exception e) {
            Toast.makeText(this, "요약 파일 읽기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("QuizActivity", "파일 읽기 오류", e);
        }
        return text.toString();
    }

    private void parseQuizResult(String result) {
        try {
            String[] lines = result.split("\n");
            ArrayList<String> questions = new ArrayList<>();
            ArrayList<String[]> options = new ArrayList<>();
            ArrayList<String> answers = new ArrayList<>();
            ArrayList<String> explainList = new ArrayList<>();

            String currentQuestion = "";
            String[] currentOptions = new String[4];

            for (String line : lines) {
                if (line.trim().startsWith("문제")) {
                    int colonIdx = line.indexOf(":");
                    currentQuestion = (colonIdx != -1 && colonIdx + 1 < line.length()) ?
                            line.substring(colonIdx + 1).trim() : "";
                } else if (line.startsWith("보기:")) {
                    String optionLine = line.replace("보기:", "").trim();
                    if (optionLine.startsWith("[") && optionLine.endsWith("]")) {
                        optionLine = optionLine.substring(1, optionLine.length() - 1);
                    }

                    String[] optionItems = optionLine.split(",");
                    for (int i = 0; i < 4; i++) {
                        if (i < optionItems.length && optionItems[i] != null) {
                            currentOptions[i] = optionItems[i].trim().replaceFirst("^\\d+\\.\\s*", "");
                        } else {
                            currentOptions[i] = "보기 없음";
                        }
                    }

                    options.add(currentOptions.clone());
                    questions.add(currentQuestion);

                } else if (line.startsWith("정답:")) {
                    String raw = line.replace("정답:", "").trim();
                    // 보기 번호를 인덱스로 변환 (예: 3 → options[i][2])
                    try {
                        int index = Integer.parseInt(raw) - 1;
                        if (index >= 0 && index < 4) {
                            answers.add(currentOptions[index]);
                        } else {
                            answers.add("보기 없음");
                        }
                    } catch (NumberFormatException e) {
                        answers.add("보기 없음");
                    }
                } else if (line.startsWith("해설:")) {
                    explainList.add(line.replace("해설:", "").trim());
                }
            }

            if (questions.size() >= 3) {
                questionText1.setText("Q1. " + questions.get(0));
                questionText2.setText("Q2. " + questions.get(1));
                questionText3.setText("Q3. " + questions.get(2));

                correctAnswers[0] = answers.get(0);
                correctAnswers[1] = answers.get(1);
                correctAnswers[2] = answers.get(2);

                explanations[0] = explainList.get(0);
                explanations[1] = explainList.get(1);
                explanations[2] = explainList.get(2);

                optionSpinner1.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, options.get(0)));
                optionSpinner2.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, options.get(1)));
                optionSpinner3.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, options.get(2)));
            } else {
                Toast.makeText(this, "퀴즈 파싱 실패: 문항 수 부족. (" + questions.size() + "개)", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "⚠️ 퀴즈 파싱 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkAnswers() {
        int correctCount = 0;
        StringBuilder feedback = new StringBuilder();

        String[] userAnswers = new String[3];
        boolean allSelected = true;

        try {
            userAnswers[0] = optionSpinner1.getSelectedItem().toString();
            userAnswers[1] = optionSpinner2.getSelectedItem().toString();
            userAnswers[2] = optionSpinner3.getSelectedItem().toString();
        } catch (Exception e) {
            allSelected = false;
        }

        if (!allSelected || userAnswers[0] == null || userAnswers[1] == null || userAnswers[2] == null) {
            Toast.makeText(this, "모든 문제에 대해 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < 3; i++) {
            String user = userAnswers[i].trim();
            String correct = correctAnswers[i].trim();

            Log.d("정답비교", "user=" + user + " / correct=" + correct);

            if (user.equals(correct)) {
                feedback.append("✅ Q").append(i + 1).append(" 정답입니다.\n")
                        .append("📘 해설: ").append(explanations[i]).append("\n\n");
                correctCount++;
            } else {
                feedback.append("❌ Q").append(i + 1).append(" 오답입니다.\n")
                        .append("🔹 정답: ").append(correct).append("\n")
                        .append("📘 해설: ").append(explanations[i]).append("\n\n");
            }
        }

        Toast.makeText(this, "총 " + correctCount + "문제 정답!", Toast.LENGTH_SHORT).show();

        if (resultText != null) {
            resultText.setText(feedback.toString());
        } else {
            Log.w("QuizActivity", "⚠️ resultText TextView를 찾을 수 없습니다.");
        }
    }
}
