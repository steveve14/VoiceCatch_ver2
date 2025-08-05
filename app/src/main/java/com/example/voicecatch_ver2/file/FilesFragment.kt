package com.example.voicecatch_ver2.file

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voicecatch_ver2.R
import java.io.File
import com.example.voicecatch_ver2.data.AppDatabase
import com.example.voicecatch_ver2.data.Recording
import com.example.voicecatch_ver2.data.RecordingDao
import kotlinx.coroutines.launch

class FilesFragment : Fragment(), OnFileClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var emptyView: TextView // 파일 없을 때 보여줄 뷰
    private lateinit var recordingDao: RecordingDao
    private val currentFileList = mutableListOf<FileListItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewFiles)
        emptyView = view.findViewById(R.id.emptyMessageTextView) // 레이아웃에 이 ID가 있어야 함

        recordingDao = AppDatabase.getDatabase(requireContext()).recordingDao()

        setupRecyclerView()
        observeRecordings()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(emptyList(), this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = fileAdapter
    }

    private fun observeRecordings() {
        // lifecycleScope를 사용하여 Flow를 안전하게 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            recordingDao.getAllRecordings().collect { recordings ->
                if (recordings.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    fileAdapter.updateData(recordings)
                }
            }
        }
    }

    fun createDummyRecording() {
        viewLifecycleOwner.lifecycleScope.launch {
            // ... (이전의 파일 생성 및 URI 얻는 로직) ...
            val fileName = "${System.currentTimeMillis()}_Dummy_Audio"
            val file = File(requireContext().externalCacheDir, fileName)
            file.createNewFile()
            val fileUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

            val newRecording = Recording(
                fileName = fileName,
                filePathUri = fileUri.toString(),
                transcribedText = "이것은 ${fileName}의 STT 결과 텍스트입니다.",
                durationMillis = (10000..180000).random().toLong(),
                fileSizeBytes = (1024..5120).random().toLong() * 1024,
                creationDate = System.currentTimeMillis(),
                aiResult = if (Math.random() > 0.7) "보이스피싱" else "일반"
            )
            recordingDao.insert(newRecording)
        }
    }

    fun createPhishingDummyRecording() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 파일 생성 로직은 동일
            val fileName = "${System.currentTimeMillis()}_PHISHING_DUMMY"
            val file = File(requireContext().externalCacheDir, fileName)
            file.createNewFile()
            val fileUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

            // Recording 객체 생성
            val newRecording = Recording(
                fileName = fileName,
                filePathUri = fileUri.toString(),
                transcribedText = "[주의] 이 녹음은 보이스피싱 의심 더미 데이터입니다. 아들, 나 핸드폰이 고장나서...",
                durationMillis = (60000..300000).random().toLong(), // 조금 더 길게
                fileSizeBytes = (2048..8192).random().toLong() * 1024,
                creationDate = System.currentTimeMillis(),
                // ❗️ aiResult를 '보이스피싱 의심'으로 고정
                aiResult = "보이스피싱 의심"
            )
            // DB에 삽입
            recordingDao.insert(newRecording)
        }
    }

    override fun onFileClick(recording: Recording) {
        val intent = Intent(requireContext(), OpenFileActivity::class.java)
        intent.putExtra("RECORDING_ID", recording.id) // 이제 ID를 전달
        startActivity(intent)
    }
}
