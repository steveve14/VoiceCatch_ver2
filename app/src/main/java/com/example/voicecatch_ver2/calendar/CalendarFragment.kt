package com.example.voicecatch_ver2.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.voicecatch_ver2.R
import com.example.voicecatch_ver2.data.AppDatabase
import com.example.voicecatch_ver2.data.RecordingDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    // --- UI 요소 ---
    private lateinit var tvDate: TextView
    private lateinit var gridView: GridView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // --- 데이터 및 로직 ---
    private lateinit var recordingDao: RecordingDao
    private val calendar = Calendar.getInstance() // 현재 달력을 제어하는 메인 Calendar 객체
    private val dayList = mutableListOf<Date>()   // GridView에 표시될 42개의 날짜를 담는 리스트

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // XML 레이아웃을 인플레이트
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 요소 초기화
        tvDate = view.findViewById(R.id.tv_date)
        gridView = view.findViewById(R.id.gridview)
        prevMonthButton = view.findViewById(R.id.btn_prev_month)
        nextMonthButton = view.findViewById(R.id.btn_next_month)
        progressBar = view.findViewById(R.id.calendar_progress_bar)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        // DAO 초기화
        recordingDao = AppDatabase.getDatabase(requireContext()).recordingDao()

        setupListeners()
        // 프래그먼트가 처음 생성될 때 달력을 업데이트
        updateCalendar()
    }

    private fun setupListeners() {
        // 이전 달 버튼 클릭 리스너
        prevMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        // 다음 달 버튼 클릭 리스너
        nextMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // GridView의 각 날짜 아이템 클릭 리스너
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedDate = dayList[position]
            val selectedCal = Calendar.getInstance().apply { time = selectedDate }

            // 현재 보고 있는 달과 다른 달의 날짜를 클릭했는지 확인
            if (selectedCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                // 기존의 BottomSheet을 재활용하여 해당 날짜의 녹음 목록을 보여줌
                showRecordingsForDay(
                    selectedCal.get(Calendar.YEAR),
                    selectedCal.get(Calendar.MONTH) + 1, // Calendar의 월은 0부터 시작하므로 +1
                    selectedCal.get(Calendar.DAY_OF_MONTH)
                )
            }
        }

        // 당겨서 새로고침 리스너
        swipeRefreshLayout.setOnRefreshListener {
            // 새로고침 시 현재 달력을 다시 로드
            updateCalendar()
        }
    }

    /**
     * 달력의 모든 UI를 현재 'calendar' 객체의 월에 맞게 업데이트하는 메인 함수
     */
    private fun updateCalendar() {
        progressBar.isVisible = true // 로딩 시작

        // 상단 날짜 텍스트뷰 업데이트 (예: 2024년 05월)
        val sdf = SimpleDateFormat("yyyy년 MM월", Locale.KOREA)
        tvDate.text = sdf.format(calendar.time)

        // 코루틴을 사용하여 DB 작업 및 UI 업데이트
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 현재 월의 보이스피싱 기록 가져오기 (DB 작업)
            val monthCalendar = calendar.clone() as Calendar
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfMonth = monthCalendar.timeInMillis

            monthCalendar.add(Calendar.MONTH, 1)
            val firstDayOfNextMonth = monthCalendar.timeInMillis

            val recordings = recordingDao.getRecordingsForDate(firstDayOfMonth, firstDayOfNextMonth)
            val phishingCounts = recordings
                .filter { it.aiResult == "보이스피싱 의심" }
                .groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.creationDate }
                    cal.get(Calendar.DAY_OF_MONTH) // 일(day)을 기준으로 그룹화
                }
                .mapValues { it.value.size } // 각 그룹의 크기(횟수)를 값으로 매핑

            // 2. GridView에 표시할 42개의 날짜 목록 생성
            dayList.clear()
            val monthCalendarForGrid = calendar.clone() as Calendar
            monthCalendarForGrid.set(Calendar.DAY_OF_MONTH, 1) // 현재 월의 1일로 설정
            val firstDayOfWeek = monthCalendarForGrid.get(Calendar.DAY_OF_WEEK) - 1 // 1(일)~7(토) -> 0~6
            monthCalendarForGrid.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek) // 달력의 첫 번째 칸에 표시될 날짜로 이동

            // 42개의 날짜(6주)를 dayList에 추가
            while (dayList.size < 42) {
                dayList.add(monthCalendarForGrid.time)
                monthCalendarForGrid.add(Calendar.DAY_OF_MONTH, 1)
            }

            // 3. 오늘 날짜가 현재 월에 포함되는지 확인
            val todayCal = Calendar.getInstance()
            val today = if (todayCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                todayCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                todayCal.get(Calendar.DAY_OF_MONTH)
            } else {
                -1 // 이번 달이 아니면 오늘 날짜를 특별히 표시하지 않음
            }

            // 4. 어댑터 생성 및 GridView에 설정
            val adapter = CalendarAdapter(
                requireContext(),
                dayList,
                phishingCounts,
                calendar.get(Calendar.MONTH), // 이번 달이 아닌 날짜를 회색 처리하기 위해
                today // 오늘 날짜를 강조하기 위해
            )
            gridView.adapter = adapter

            progressBar.isVisible = false // 로딩 완료
            swipeRefreshLayout.isRefreshing = false // 새로고침 아이콘 숨기기
        }
    }

    /**
     * 날짜를 클릭했을 때 해당 날짜의 녹음 목록을 보여주는 BottomSheet을 띄우는 함수
     */
    private fun showRecordingsForDay(year: Int, month: Int, day: Int) {
        val bottomSheet = RecordingsForDayBottomSheet.newInstance(year, month, day)
        // childFragmentManager를 사용해야 프래그먼트 내에서 BottomSheet을 제대로 관리할 수 있습니다.
        bottomSheet.show(childFragmentManager, RecordingsForDayBottomSheet.TAG)
    }
}