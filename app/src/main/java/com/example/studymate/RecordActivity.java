package com.example.studymate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_FILE = 1;

    private ImageButton recordButton;
    private TextView statusText;
    private TextView convertButton;
    private TextView loadFileButton;

    private boolean isRecording = false;
    private String filePath;
    private Thread recordingThread;
    private static final int SAMPLE_RATE = 44100;
    private boolean isConverting = false;

    private static final String SIGNED_URL = BuildConfig.GCS_SIGNED_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }

        recordButton = findViewById(R.id.btn_record);
        convertButton = findViewById(R.id.btn_convert);
        statusText = findViewById(R.id.tv_status);
        loadFileButton = findViewById(R.id.btn_load_file);

        convertButton.setVisibility(View.INVISIBLE);

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                recordButton.setImageResource(R.drawable.ic_stop);
            } else {
                stopRecording();
                recordButton.setImageResource(R.drawable.ic_play);
            }
        });

        // ... (onCreate, 녹음 코드 등 동일)
        convertButton.setOnClickListener(v -> {
            if (filePath == null) {
                Toast.makeText(this, "오디오 파일이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isConverting) {
                Toast.makeText(this, "변환이 이미 진행 중입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadWithSignedUrl();
        });

        loadFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "오디오 파일 선택"), PICK_AUDIO_FILE);
        });
    }

    // ⭐️ 입력창 없이 바로 업로드!
    private void uploadWithSignedUrl() {
        isConverting = true;
        convertButton.setEnabled(false);
        statusText.setText("GCS에 업로드 중...");

        new Thread(() -> {
            String sttResult = null;
            try {
                // 1. 업로드 (업로드 실패 시 Exception 발생)
                GCSUploader.uploadWithSignedUrl(new File(filePath), SIGNED_URL);

                // 2. 업로드 성공 후 GCS URI 생성 (실제 파일명/경로에 맞게 수정)
                String gcsUri = BuildConfig.GCS_URI;

                runOnUiThread(() -> statusText.setText("업로드 성공! (STT 변환 중...)"));

                // 3. STT 변환 요청
                String operationName = GoogleSTTRequester.requestSTT(gcsUri);

                // 4. 결과 polling
                sttResult = GoogleSTTRequester.pollSTTResult(operationName);

            } catch (Exception e) {
                // 오류 메시지로 sttResult 할당
                sttResult = "STT 변환 실패 또는 업로드 오류:\n" + e.getMessage();
            }

            String finalSttResult = sttResult;
            runOnUiThread(() -> {
                isConverting = false;
                convertButton.setEnabled(true);
                statusText.setText("처리 완료");
                // 항상 DocumentActivity로 이동
                Intent intent = new Intent(this, DocumentActivity.class);
                intent.putExtra("stt_result", finalSttResult);
                intent.putExtra("filePath", filePath);
                startActivity(intent);
            });
        }).start();
    }




    // ---- 이하 기존 녹음/파일처리 코드 동일 ----

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }
        filePath = getExternalFilesDir(null).getAbsolutePath() + "/recording_temp.wav";
        isRecording = true;
        statusText.setText("녹음 중...");

        recordingThread = new Thread(() -> {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            byte[] buffer = new byte[bufferSize];
            File wavFile = new File(filePath);

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(wavFile))) {
                audioRecord.startRecording();
                WaveHeaderWriter.writeWavHeader(out, SAMPLE_RATE, 1, 16);

                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                WaveHeaderWriter.updateWavHeader(wavFile);

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "녹음 실패", Toast.LENGTH_SHORT).show());
            }

            runOnUiThread(this::promptForFilename);
        });

        recordingThread.start();
    }

    private void stopRecording() {
        isRecording = false;
    }

    private void promptForFilename() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("파일 이름을 입력하세요");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("예: my_lecture");
        builder.setView(input);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();

            if (userFileName.isEmpty()) {
                userFileName = "recording_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            }

            if (!userFileName.endsWith(".wav")) {
                userFileName += ".wav";
            }

            File tempFile = new File(getExternalFilesDir(null), "recording_temp.wav");
            File renamedFile = new File(getExternalFilesDir(null), userFileName);

            try {
                copyFile(tempFile, renamedFile);
                filePath = renamedFile.getAbsolutePath();
                convertButton.setVisibility(View.VISIBLE);
                Toast.makeText(this, "앱 저장소에 저장 완료: " + userFileName, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "파일 복사 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> {
            dialog.cancel();
            filePath = null;
            convertButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "저장이 취소되었습니다", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    // 파일 복사 (모든 경우 앱 전용 저장소로 복사)
    private void copyFile(File source, File dest) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
             InputStream in = new FileInputStream(source)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    // 외부 Uri에서 파일 이름 추출
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "selected_audio.wav";
    }

    // 외부에서 불러온 파일을 앱 전용 저장소로 강제 복사
    private File copyUriToAppDir(Uri uri) throws IOException {
        String fileName = getFileNameFromUri(uri);
        if (!fileName.endsWith(".wav")) fileName += ".wav";
        File destination = new File(getExternalFilesDir(null), "imported_" + System.currentTimeMillis() + "_" + fileName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
        return destination;
    }

    // WAV 헤더 확인
    private boolean isValidWavFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[12];
            if (fis.read(header) != 12) return false;
            String riff = new String(header, 0, 4, "US-ASCII");
            String wave = new String(header, 8, 4, "US-ASCII");
            return "RIFF".equals(riff) && "WAVE".equals(wave);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                File copiedFile = copyUriToAppDir(uri); // 외부 파일을 앱 전용 저장소로 복사
                if (isValidWavFile(copiedFile)) {
                    filePath = copiedFile.getAbsolutePath();
                    Toast.makeText(this, "앱 저장소에 파일 복사 완료!", Toast.LENGTH_SHORT).show();
                    convertButton.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "WAV 파일이 아니거나 손상된 파일입니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "파일 복사 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
