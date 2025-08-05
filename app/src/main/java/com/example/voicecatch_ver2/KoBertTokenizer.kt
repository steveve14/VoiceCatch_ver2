package com.example.voicecatch_ver2

import android.content.res.AssetManager
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class KoBertTokenizer(assetManager: AssetManager, vocabPath: String) {
    private val vocab: Map<String, Int>
    private val unkToken = "[UNK]"
    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val padToken = "[PAD]"

    private val unkTokenId: Int
    private val padTokenId: Int
    private val sepTokenId: Int
    private val clsTokenId: Int

    init {
        vocab = loadVocab(assetManager, vocabPath)

        // init 블록에서 특수 토큰의 ID를 안전하게 가져옵니다.
        // 만약 vocab에 토큰이 없다면 앱이 즉시 종료되도록 하여 문제를 빨리 파악하게 합니다.
        unkTokenId = vocab[unkToken] ?: throw IllegalStateException("Vocabulary does not contain the [UNK] token.")
        padTokenId = vocab[padToken] ?: throw IllegalStateException("Vocabulary does not contain the [PAD] token.")
        sepTokenId = vocab[sepToken] ?: throw IllegalStateException("Vocabulary does not contain the [SEP] token.")
        clsTokenId = vocab[clsToken] ?: throw IllegalStateException("Vocabulary does not contain the [CLS] token.")

        if (vocab.isEmpty()) {
            Log.e("KoBertTokenizer", "Vocabulary is empty! Check if vocab.txt is in assets and is not empty.")
        }
    }

    private fun loadVocab(assetManager: AssetManager, vocabPath: String): Map<String, Int> {
        val vocabMap = mutableMapOf<String, Int>()
        try {
            assetManager.open(vocabPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
                    if (line.isNotBlank()) {
                        // 라인 번호를 ID로 사용합니다.
                        vocabMap[line.trim()] = vocabMap.size
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("KoBertTokenizer", "Error loading vocabulary", e)
        }
        return vocabMap
    }

    // 간단한 서브워드 토크나이징 구현 (Greedy 방식)
    private fun tokenize(text: String): List<String> {
        val normalizedText = text.lowercase()
        val tokens = mutableListOf<String>()
        var start = 0
        while (start < normalizedText.length) {
            var end = normalizedText.length
            var foundToken: String? = null
            while (start < end) {
                var sub = normalizedText.substring(start, end)
                if (start > 0) {
                    sub = "##$sub"
                }
                if (vocab.containsKey(sub)) {
                    foundToken = sub
                    break
                }
                end--
            }
            if (foundToken == null) {
                tokens.add(unkToken)
                start++ // Move to the next character
            } else {
                tokens.add(foundToken)
                start = end
            }
        }
        return tokens
    }

    data class ModelInput(val inputIds: LongArray, val attentionMask: LongArray)

    fun encode(text: String, maxLength: Int): ModelInput {
        val tokens = tokenize(text)

        val tokenList = mutableListOf(clsToken)
        tokenList.addAll(tokens)
        if (tokenList.size > maxLength - 1) {
            tokenList.subList(maxLength - 1, tokenList.size).clear()
        }
        tokenList.add(sepToken)

        // vocab에서 가져온 ID(Int)를 Long으로 변환
        val tokenIds = tokenList.map { (vocab[it] ?: unkTokenId).toLong() }.toMutableList()
        // attentionMask도 Long 타입으로 생성 (1L, 0L)
        val attentionMask = MutableList(tokenIds.size) { 1L }

        if (tokenIds.size < maxLength) {
            val paddingNeeded = maxLength - tokenIds.size
            // padding ID도 Long으로 추가
            tokenIds.addAll(List(paddingNeeded) { padTokenId.toLong() })
            attentionMask.addAll(List(paddingNeeded) { 0L })
        }

        // toLongArray()를 사용하여 반환 타입을 맞춤
        return ModelInput(tokenIds.toLongArray(), attentionMask.toLongArray())
    }
}