package com.example.voicecatch_ver2

// BertActivity.kt
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BertActivity : AppCompatActivity() {

    // UI 요소
    private lateinit var inputEditText: EditText
    private lateinit var classifyButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var progressBar: ProgressBar

    // 분류기 인스턴스
    private lateinit var textClassifier: TextClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bert)

        supportActionBar?.title = "스팸 분류기"

        // UI 요소 초기화
        inputEditText = findViewById(R.id.inputEditText)
        classifyButton = findViewById(R.id.classifyButton)
        resultTextView = findViewById(R.id.resultTextView)
        progressBar = findViewById(R.id.progressBar)

        // Application 클래스에서 싱글턴 분류기 인스턴스를 가져옴
        textClassifier = (application as SpamApplication).textClassifier

        // 분류기 초기화 (앱 실행 후 한 번만 수행됨)
        // 버튼을 비활성화했다가 초기화가 끝나면 활성화하여 안정성 확보
        classifyButton.isEnabled = false
        lifecycleScope.launch {
            try {
                textClassifier.initialize()
                classifyButton.isEnabled = true
                Toast.makeText(this@BertActivity, "분류기가 준비되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                resultTextView.text = "오류: 분류기 초기화 실패"
                resultTextView.visibility = View.VISIBLE
                Toast.makeText(this@BertActivity, "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // 분석하기 버튼 클릭 리스너 설정
        classifyButton.setOnClickListener {
            val inputText = inputEditText.text.toString()
            if (inputText.isNotBlank()) {
                // 분석 시작: 프로그레스바 보이기, 이전 결과 숨기기
                progressBar.visibility = View.VISIBLE
                resultTextView.visibility = View.GONE

                // 코루틴으로 텍스트 분류 실행
                lifecycleScope.launch {
                    val result = textClassifier.classify(inputText)
                    // 분석 완료: 프로그레스바 숨기기, 결과 보이기
                    progressBar.visibility = View.GONE
                    resultTextView.text = result
                    resultTextView.visibility = View.VISIBLE
                }
            } else {
                Toast.makeText(this, "분석할 텍스트를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}