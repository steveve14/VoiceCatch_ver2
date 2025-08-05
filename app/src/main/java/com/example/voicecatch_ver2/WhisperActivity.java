package com.example.voicecatch_ver2;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.voicecatch_ver2.asr.Player;
import com.example.voicecatch_ver2.asr.Recorder;
import com.example.voicecatch_ver2.asr.Whisper;
import com.example.voicecatch_ver2.utils.WaveUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WhisperActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String DEFAULT_MODEL_TO_USE = "fast-whisper-base-ko.tflite";
    private static final String MULTILINGUAL_VOCAB_FILE = "filters_vocab_multilingual.bin";
    private static final String[] EXTENSIONS_TO_COPY = {"tflite", "bin", "wav"};
    private static final int FILE_SELECT_CODE = 1;

    // Whisper UI 및 로직 변수들
    private TextView tvStatus;
    private TextView tvResult;
    private TextView tvSelectedFileName;
    private TextView spnrTfliteFiles;
    private Button btnRecord;
    private Button btnPlay;
    private Button btnTranscribe;
    private Button btnSelectFile;
    private FloatingActionButton fabCopy;

    private Player mPlayer = null;
    private Recorder mRecorder = null;
    private Whisper mWhisper = null;

    private File sdcardDataFolder = null;
    private File selectedWaveFile = null;
    private File selectedTfliteFile = null;
    private long startTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Bert UI 및 로직 변수들 (통합)
    private Button btnAnalyzeWithBert;
    private TextView tvResultBert;
    private TextClassifier textClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whisper);

        // 1. 모든 UI 요소 초기화 (가장 먼저 수행)
        initializeUI();

        // 2. 파일 및 모델 경로 설정
        sdcardDataFolder = this.getExternalFilesDir(null);
        copyAssetsToSdcard(this, sdcardDataFolder, EXTENSIONS_TO_COPY);
        selectedTfliteFile = new File(sdcardDataFolder, DEFAULT_MODEL_TO_USE);

        if (spnrTfliteFiles != null) {
            spnrTfliteFiles.setText(DEFAULT_MODEL_TO_USE);
        }

        // 3. 기본 오디오 파일 설정
        setupDefaultAudioFile();

        // 4. 버튼 리스너 설정
        setupButtonListeners();

        // 5. Whisper 및 Bert 관련 클래스 초기화
        initializeModules();

        // 6. 권한 확인
        checkRecordPermission();
    }

    private void initializeUI() {
        // Whisper UI
        tvStatus = findViewById(R.id.tvStatus);
        tvResult = findViewById(R.id.tvResult);
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName);
        btnRecord = findViewById(R.id.btnRecord);
        btnPlay = findViewById(R.id.btnPlay);
        btnTranscribe = findViewById(R.id.btnTranscb);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        fabCopy = findViewById(R.id.fabCopy);
        spnrTfliteFiles = findViewById(R.id.spnrTfliteFiles);

        // Bert UI
        btnAnalyzeWithBert = findViewById(R.id.btnAnalyzeWithBert);
        tvResultBert = findViewById(R.id.tvResult_bert);
    }

    private void setupDefaultAudioFile() {
        selectedWaveFile = new File(sdcardDataFolder, WaveUtil.RECORDING_FILE);
        if (selectedWaveFile.exists()) {
            tvSelectedFileName.setText(WaveUtil.RECORDING_FILE);
            btnPlay.setEnabled(true);
            btnTranscribe.setEnabled(true);
        } else {
            tvSelectedFileName.setText("기본 파일 없음");
            btnPlay.setEnabled(false);
            btnTranscribe.setEnabled(false);
        }
    }

    // 버튼 리스너들을 설정하는 헬퍼 함수
    private void setupButtonListeners() {
        btnSelectFile.setOnClickListener(v -> selectAudioFile());
        btnRecord.setOnClickListener(v -> {
            if (mRecorder != null && mRecorder.isInProgress()) stopRecording();
            else startRecording();
        });
        btnPlay.setOnClickListener(v -> {
            if (selectedWaveFile == null || !selectedWaveFile.exists()) {
                Toast.makeText(this, "먼저 파일을 선택하거나 녹음해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mPlayer != null && !mPlayer.isPlaying()) mPlayer.initializePlayer(selectedWaveFile.getAbsolutePath());
            else if (mPlayer != null) mPlayer.stopPlayback();
        });
        btnTranscribe.setOnClickListener(v -> {
            if (selectedWaveFile == null || !selectedWaveFile.exists()) {
                Toast.makeText(this, "먼저 파일을 선택하거나 녹음해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 이미 작업이 진행 중인지 확인 (이중 방어)
            if (mWhisper != null && mWhisper.isInProgress()) {
                Toast.makeText(this, "이미 변환 작업이 진행 중입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mRecorder != null && mRecorder.isInProgress()) {
                stopRecording();
            }

            // 버튼을 즉시 비활성화하여 중복 클릭 방지
            btnTranscribe.setEnabled(false);
            tvStatus.setText("음성 변환 요청 중..."); // 사용자에게 상태 알림

            // 변환 시작
            if (mWhisper != null) {
                startTranscription(selectedWaveFile.getAbsolutePath());
            } else {
                // 예외 상황: Whisper가 초기화되지 않음
                Toast.makeText(this, "오류: Whisper 엔진이 준비되지 않았습니다.", Toast.LENGTH_SHORT).show();
                btnTranscribe.setEnabled(true); // 버튼 다시 활성화
            }
            // --- 수정된 부분 끝 ---
        });

        fabCopy.setOnClickListener(v -> {
            String textToCopy = tvResult.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "텍스트가 복사되었습니다.", Toast.LENGTH_SHORT).show();
        });
        btnAnalyzeWithBert.setOnClickListener(v -> {
            String inputText = tvResult.getText().toString();
            if (!inputText.trim().isEmpty() && !inputText.equals("음성 변환 결과가 여기에 표시됩니다.")) {
                performClassification(inputText);
            } else {
                Toast.makeText(this, "분석할 텍스트가 없습니다. 먼저 음성 변환을 실행해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeModules() {
        // Whisper 모듈 초기화
        mPlayer = new Player(this);
        mRecorder = new Recorder(this);
        initWhisperModel(selectedTfliteFile);
        setupRecorderListener();
        setupPlayerListener();

        // Bert 모듈 초기화
        textClassifier = ((SpamApplication) getApplication()).getTextClassifier();
        btnAnalyzeWithBert.setEnabled(false);
        tvStatus.setText("BERT 분류기 초기화 중...");
        new Thread(() -> {
            try {
                textClassifier.initialize();
                runOnUiThread(() -> {
                    btnAnalyzeWithBert.setEnabled(true);
                    tvStatus.setText("상태: 대기 중");
                    Toast.makeText(WhisperActivity.this, "분류기가 준비되었습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText("오류: 분류기 초기화 실패");
                    Toast.makeText(WhisperActivity.this, "초기화 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Bert 스팸 분류 수행 함수
    private void performClassification(String text) {
        tvStatus.setText("BERT 분석 중...");
        tvResultBert.setText("");
        tvResultBert.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final String result = textClassifier.classify(text);
            runOnUiThread(() -> {
                tvStatus.setText("상태: 대기 중");
                tvResultBert.setText(result);
            });
        }).start();
    }


    // Whisper 모델 초기화
    private void initWhisperModel(File modelFile) {
        File vocabFile = new File(sdcardDataFolder, MULTILINGUAL_VOCAB_FILE);
        mWhisper = new Whisper(this);
        mWhisper.loadModel(modelFile, vocabFile, true);
        mWhisper.setListener(new Whisper.WhisperListener() {
            @Override
            public void onUpdateReceived(String message) {
                if (message.equals(Whisper.MSG_PROCESSING)) {
                    handler.post(() -> {
                        tvStatus.setText(message);
                        tvResult.setText("");
                    });
                    startTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onResultReceived(String result) {
                long timeTaken = System.currentTimeMillis() - startTime;
                handler.post(() -> {
                    tvStatus.setText("변환 완료: " + timeTaken + "ms");
                    tvResult.append(result);

                    btnTranscribe.setEnabled(true);
                });
            }
        });
    }


    // Recorder 리스너 설정
    private void setupRecorderListener() {
        mRecorder.setListener(new Recorder.RecorderListener() {
            @Override
            public void onUpdateReceived(String message) {
                handler.post(() -> tvStatus.setText(message));
                if (message.equals(Recorder.MSG_RECORDING)) {
                    handler.post(() -> {
                        tvResult.setText("");
                        btnRecord.setText(R.string.stop);
                        tvSelectedFileName.setText("녹음 중...");
                        btnPlay.setEnabled(false);
                        btnTranscribe.setEnabled(false);
                    });
                } else if (message.equals(Recorder.MSG_RECORDING_DONE)) {
                    handler.post(() -> {
                        btnRecord.setText(R.string.record);
                        selectedWaveFile = new File(sdcardDataFolder, WaveUtil.RECORDING_FILE);
                        tvSelectedFileName.setText(WaveUtil.RECORDING_FILE);
                        btnPlay.setEnabled(true);
                        btnTranscribe.setEnabled(true);
                    });
                }
            }
            @Override
            public void onDataReceived(float[] samples) {}
        });
    }

    // Player 리스너 설정
    private void setupPlayerListener() {
        mPlayer.setListener(new Player.PlaybackListener() {
            @Override
            public void onPlaybackStarted() { handler.post(() -> btnPlay.setText(R.string.stop)); }
            @Override
            public void onPlaybackStopped() { handler.post(() -> btnPlay.setText(R.string.play)); }
        });
    }

    // 파일 선택 및 권한 관련 함수들
    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "변환할 오디오 파일 선택"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "파일 관리자 앱을 설치해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                File copiedFile = copyUriToAppStorage(uri);
                if (copiedFile != null) {
                    selectedWaveFile = copiedFile;
                    tvSelectedFileName.setText(getFileName(uri));
                    btnPlay.setEnabled(true);
                    btnTranscribe.setEnabled(true);
                    Toast.makeText(this, "파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File copyUriToAppStorage(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            File file = new File(sdcardDataFolder, "selected_audio.tmp");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void checkRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "녹음 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 유틸리티 함수들
    private void startRecording() {
        checkRecordPermission();
        File waveFile = new File(sdcardDataFolder, WaveUtil.RECORDING_FILE);
        mRecorder.setFilePath(waveFile.getAbsolutePath());
        mRecorder.start();
    }

    private void stopRecording() {
        if (mRecorder != null) mRecorder.stop();
    }

    private void startTranscription(String waveFilePath) {
        if (mWhisper != null) {
            mWhisper.setFilePath(waveFilePath);
            mWhisper.setAction(Whisper.ACTION_TRANSCRIBE);
            mWhisper.start();
        }
    }

    private void stopTranscription() {
        if (mWhisper != null) mWhisper.stop();
    }

    private static void copyAssetsToSdcard(Context context, File destFolder, String[] extensions) {
        // --- 수정된 부분 시작 ---
        // 1. 에셋 하위 폴더 경로를 지정합니다.
        final String ASSET_SUBFOLDER = "whisper";

        AssetManager assetManager = context.getAssets();
        try {
            // 2. 최상위 폴더("") 대신 지정된 하위 폴더("whisper")의 파일 목록을 가져옵니다.
            String[] assetFiles = assetManager.list(ASSET_SUBFOLDER);

            if (assetFiles == null || assetFiles.length == 0) {
                Log.e("CopyAssets", "Asset subfolder '" + ASSET_SUBFOLDER + "' not found or is empty.");
                return;
            }

            for (String assetFileName : assetFiles) {
                for (String extension : extensions) {
                    if (assetFileName.endsWith("." + extension)) {
                        // 3. 파일을 열 때도 하위 폴더 경로를 포함한 전체 경로를 사용합니다.
                        String sourcePath = ASSET_SUBFOLDER + File.separator + assetFileName;
                        File outFile = new File(destFolder, assetFileName);

                        // 파일이 이미 존재하면 복사하지 않음
                        if (outFile.exists()) {
                            Log.d("CopyAssets", assetFileName + " already exists. Skipping.");
                            break;
                        }

                        // assets에서 파일을 읽어 대상 폴더에 쓰기
                        try (InputStream inputStream = assetManager.open(sourcePath);
                             OutputStream outputStream = new FileOutputStream(outFile)) {

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            Log.d("CopyAssets", "Copied " + assetFileName + " to " + destFolder.getAbsolutePath());
                        }
                        break; // 일치하는 확장자를 찾았으므로 더 이상 확인할 필요 없음
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}