package com.example.voicecatch_ver2.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.voicecatch_ver2.file.FileAdapter
import com.example.voicecatch_ver2.file.OnFileClickListener
import com.example.voicecatch_ver2.R
import com.example.voicecatch_ver2.data.AppDatabase
import com.example.voicecatch_ver2.data.Recording
import com.example.voicecatch_ver2.file.OpenFileActivity
import kotlinx.coroutines.launch
import java.util.GregorianCalendar

class RecordingsForDayBottomSheet : BottomSheetDialogFragment(), OnFileClickListener {

    private lateinit var titleTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var fileAdapter: FileAdapter

    // 날짜 정보를 전달받기 위한 인수
    private val year by lazy { arguments?.getInt(ARG_YEAR) ?: 0 }
    private val month by lazy { arguments?.getInt(ARG_MONTH) ?: 0 }
    private val day by lazy { arguments?.getInt(ARG_DAY) ?: 0 }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_recordings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleTextView = view.findViewById(R.id.titleTextView)
        recyclerView = view.findViewById(R.id.recordingsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)

        setupRecyclerView()
        loadRecordings()
    }

    private fun setupRecyclerView() {
        // 기존의 FileAdapter와 OnFileClickListener를 그대로 활용합니다.
        fileAdapter = FileAdapter(emptyList(), this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = fileAdapter
    }

    private fun loadRecordings() {
        titleTextView.text = "${year}년 ${month}월 ${day}일 녹음 목록"

        // 선택된 날짜의 시작 시간 (자정)
        val startTime = GregorianCalendar(year, month - 1, day, 0, 0, 0).timeInMillis
        // 선택된 날짜의 종료 시간 (다음날 자정)
        val endTime = GregorianCalendar(year, month - 1, day + 1, 0, 0, 0).timeInMillis

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).recordingDao()
            val recordings = dao.getRecordingsForDate(startTime, endTime)

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

    // FileAdapter의 아이템 클릭 이벤트 처리
    override fun onFileClick(recording: Recording) {
        val intent = Intent(requireContext(), OpenFileActivity::class.java).apply {
            putExtra("RECORDING_ID", recording.id)
        }
        startActivity(intent)
        // BottomSheet 닫기
        dismiss()
    }

    companion object {
        const val TAG = "RecordingsForDayBottomSheet"
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month"
        private const val ARG_DAY = "day"

        fun newInstance(year: Int, month: Int, day: Int): RecordingsForDayBottomSheet {
            return RecordingsForDayBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                    putInt(ARG_DAY, day)
                }
            }
        }
    }
}