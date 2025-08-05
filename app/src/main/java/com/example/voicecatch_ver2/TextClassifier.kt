package com.example.voicecatch_ver2

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class TextClassifier(private val context: Context) {

    private lateinit var tflite: Interpreter
    private lateinit var tokenizer: KoBertTokenizer
    private var isInitialized = false

    private val modelPath = "bert/spam-distilkobert-final.tflite"
    private val vocabPath = "bert/vocab.txt"
    private val maxSeqLength = 256

    // --- 수정된 부분 1 ---
    // withContext를 제거하여 일반 함수로 변경합니다.
    fun initialize() {
        if (isInitialized) return

        // withContext(Dispatchers.IO) { ... } 제거
        try {
            val model = loadModelFile(context.assets, modelPath)
            val options = Interpreter.Options()
            tflite = Interpreter(model, options)
            tokenizer = KoBertTokenizer(context.assets, vocabPath)
            isInitialized = true // 초기화 완료 플래그 설정
            Log.d(TAG, "Classifier initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing classifier.", e)
            throw e // 예외를 다시 던져서 호출자(Activity)가 처리하도록 함
        }
    }

    // --- 수정된 부분 2 ---
    // withContext를 제거하여 일반 함수로 변경합니다.
    fun classify(text: String): String {
        // return withContext(Dispatchers.Default) { ... } 제거
        val modelInput = tokenizer.encode(text, maxSeqLength)

        // 1. 모델 입력 준비
        val inputIds2D = Array(1) { modelInput.inputIds }
        val attentionMask2D = Array(1) { modelInput.attentionMask }

        // 2. 입력들을 담을 최상위 배열
        val inputsArray = arrayOf(inputIds2D, attentionMask2D)

        // 3. 출력들을 받을 맵
        val outputLogits = Array(1) { FloatArray(2) }
        val outputMap = mapOf<Int, Any>(0 to outputLogits)

        try {
            // 4. 모델 실행
            tflite.runForMultipleInputsOutputs(inputsArray, outputMap)
        } catch (e: Exception) {
            Log.e(TAG, "TFLite inference failed.", e)
            // return@withContext 대신 일반 return 사용
            return "추론 실패: ${e.message}"
        }

        // 5. 결과 후처리 및 반환
        return postprocess(outputLogits[0])
    }

    // 리소스 해제 함수 (변경 없음)
    fun close() {
        if (::tflite.isInitialized) {
            tflite.close()
            Log.d(TAG, "TFLite interpreter closed.")
        }
    }

    // (이하 헬퍼 함수들은 변경 없음)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun postprocess(logits: FloatArray): String {
        val probabilities = softmax(logits)
        val hamProbability = probabilities[0]
        val spamProbability = probabilities[1]

        val resultText = if (spamProbability > hamProbability) {
            "결과: 스팸 메시지 (확률: ${"%.2f".format(spamProbability * 100)}%)"
        } else {
            "결과: 정상 메시지 (확률: ${"%.2f".format(hamProbability * 100)}%)"
        }

        Log.d(TAG, "Logits: ${logits.joinToString()}, Probabilities: ${probabilities.joinToString()}")
        return resultText
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0.0f
        val exps = logits.map { kotlin.math.exp(it - maxLogit) }
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }

    companion object {
        private const val TAG = "TextClassifier"
    }
}