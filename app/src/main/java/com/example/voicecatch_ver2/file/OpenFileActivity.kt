package com.example.voicecatch_ver2.file

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.voicecatch_ver2.R
import com.example.voicecatch_ver2.data.AppDatabase
import com.example.voicecatch_ver2.data.Recording
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenFileActivity : AppCompatActivity() {

    // --- 클래스 멤버 변수 (lateinit으로 선언) ---
    private lateinit var titleTextView: TextView
    private lateinit var fileSizeTextView: TextView
    private lateinit var fileDurationTextView: TextView
    private lateinit var fileDayTextView: TextView
    private lateinit var aiResultTextView: TextView
    private lateinit var contentTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressValueText: TextView

    private var fileUri: Uri? = null
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var progressRunnable: Runnable
    private lateinit var db: AppDatabase // 데이터베이스 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_openfile)

        // --- 1. UI 요소 초기화 (가장 먼저 수행) ---
        titleTextView = findViewById(R.id.file_name_title)
        fileSizeTextView = findViewById(R.id.file_size)
        fileDurationTextView = findViewById(R.id.file_duration)
        fileDayTextView = findViewById(R.id.file_day)
        aiResultTextView = findViewById(R.id.file_result)
        contentTextView = findViewById(R.id.content_text)
        progressBar = findViewById(R.id.audio_seekbar)
        progressValueText = findViewById(R.id.progress_value)

        val backButton: ImageButton = findViewById(R.id.back_button)
        val startButton: Button = findViewById(R.id.start_button)
        val stopButton: Button = findViewById(R.id.stop_button)
        val resetButton: Button = findViewById(R.id.reset_button)
        val copyButton: Button = findViewById(R.id.copy_button)
        val openButton: Button = findViewById(R.id.open_button)

        // --- 2. 데이터베이스 및 데이터 로딩 ---
        db = AppDatabase.getDatabase(this)
        val recordingId = intent.getLongExtra("RECORDING_ID", -1L)

        if (recordingId != -1L) {
            (this as LifecycleOwner).lifecycleScope.launch {
                val recording = db.recordingDao().getRecordingById(recordingId)
                recording?.let {
                    displayRecordingDetails(it)
                }
            }
        }

        // --- 3. 프로그레스 바 로직 정의 (사라졌던 기능) ---
        progressRunnable = object : Runnable {
            override fun run() {
                if (progressStatus < 100) {
                    progressStatus++
                    progressBar.progress = progressStatus
                    progressValueText.text = "$progressStatus%"
                    handler.postDelayed(this, 50)
                } else {
                    Toast.makeText(this@OpenFileActivity, "재생 완료", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- 4. 클릭 리스너 설정 ---
        backButton.setOnClickListener {
            finish()
        }

        startButton.setOnClickListener {
            Toast.makeText(this, "Start Clicked", Toast.LENGTH_SHORT).show()
            if (progressStatus < 100) {
                handler.post(progressRunnable)
            }
        }

        stopButton.setOnClickListener {
            Toast.makeText(this, "Stop Clicked", Toast.LENGTH_SHORT).show()
            handler.removeCallbacks(progressRunnable)
        }

        resetButton.setOnClickListener {
            Toast.makeText(this, "Progress Reset", Toast.LENGTH_SHORT).show()
            handler.removeCallbacks(progressRunnable)
            progressStatus = 0
            progressBar.progress = 0
            progressValueText.text = "0%"
        }

        copyButton.setOnClickListener {
            copyContentToClipboard()
        }

        openButton.setOnClickListener {
            openFileWithExternalApp()
        }
    }

    private fun displayRecordingDetails(recording: Recording) {
        // DB에서 가져온 데이터로 UI 업데이트
        titleTextView.text = recording.fileName
        contentTextView.text = recording.transcribedText
        fileSizeTextView.text = "크기: ${Formatter.formatShortFileSize(this, recording.fileSizeBytes)}"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(recording.durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(recording.durationMillis) % 60
        fileDurationTextView.text = "길이: ${String.format("%02d:%02d", minutes, seconds)}"

        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        fileDayTextView.text = "날짜: ${sdf.format(Date(recording.creationDate))}"

        aiResultTextView.text = "결과: ${recording.aiResult}"

        // 파일 열기를 위해 Uri 설정
        fileUri = Uri.parse(recording.filePathUri)
    }

    private fun copyContentToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToCopy = contentTextView.text
        val clip = ClipData.newPlainText("Copied Text", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "내용이 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun openFileWithExternalApp() {
        if (fileUri == null) {
            Toast.makeText(this, "파일 URI가 없어 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "이 파일을 열 수 있는 앱이 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // 메모리 누수 방지를 위해 핸들러 콜백 제거
        handler.removeCallbacks(progressRunnable)
    }
}