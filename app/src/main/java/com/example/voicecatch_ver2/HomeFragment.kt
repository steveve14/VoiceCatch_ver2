package com.example.voicecatch_ver2

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.voicecatch_ver2.data.AppDatabase
import com.example.voicecatch_ver2.data.DailyDuration
import com.example.voicecatch_ver2.data.RecordingDao
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    // --- UI 요소 ---
    private lateinit var chart: LineChart
    private lateinit var totalAttemptsValue: TextView
    private lateinit var normalCallsValue: TextView
    private lateinit var phishingAttemptsValue: TextView
    private lateinit var statsMainValue: TextView

    // --- 데이터 ---
    private lateinit var dao: RecordingDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // DAO는 프래그먼트 뷰가 생성되기 전에 초기화하는 것이 안전합니다.
        dao = AppDatabase.getDatabase(requireContext()).recordingDao()
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 요소 ID 찾아오기
        chart = view.findViewById(R.id.activity_chart)
        totalAttemptsValue = view.findViewById(R.id.total_attempts_value)
        normalCallsValue = view.findViewById(R.id.normal_calls_value)
        phishingAttemptsValue = view.findViewById(R.id.phishing_attempts_value)
        statsMainValue = view.findViewById(R.id.stats_main_value)

        // 데이터 로딩 및 UI 업데이트 시작
        observeAllRecordingsForStats()
        loadChartDataForThisWeek()
    }

    /**
     * 전체 녹음 데이터를 실시간으로 관찰하여 하단의 통계 카드(전체, 일반, 보이스피싱)를 업데이트합니다.
     */
    private fun observeAllRecordingsForStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllRecordings().collectLatest { recordings ->
                // "보이스피싱 의심" 개수 계산
                val phishingCount = recordings.count { it.aiResult == "보이스피싱 의심" }
                // "일반" 개수 계산
                val normalCount = recordings.count { it.aiResult == "일반" }
                // 전체 개수
                val totalCount = recordings.size

                // UI 업데이트
                totalAttemptsValue.text = totalCount.toString()
                normalCallsValue.text = normalCount.toString()
                phishingAttemptsValue.text = phishingCount.toString()
            }
        }
    }

    /**
     * "이번 주" 데이터를 로드하여 상단의 활동 통계 카드와 차트를 업데이트하는 메인 함수
     */
    private fun loadChartDataForThisWeek() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. 이번 주의 시작(월요일) 타임스탬프 계산
            val calendar = Calendar.getInstance()
            // 주의 시작을 월요일로 설정 (대한민국 표준)
            calendar.firstDayOfWeek = Calendar.MONDAY

            // 이번 주의 첫 번째 날(월요일)로 이동
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            // 날짜의 시작 시간(00:00:00)으로 설정
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val weekStartTime = calendar.timeInMillis // 이번 주 월요일 0시의 타임스탬프

            // 2. DB에서 이번 주 데이터 가져오기 (월요일 0시 이후의 모든 데이터)
            val dailyDurations = dao.getDailyDurations(weekStartTime)

            // 3. 활동 통계 카드 (이번 주 총 녹음 시간) 업데이트
            val totalThisWeekDuration = dailyDurations.sumOf { it.totalDuration }
            statsMainValue.text = formatDuration(totalThisWeekDuration)

            // 4. 차트 데이터 설정 및 그리기
            setupChartForThisWeek(dailyDurations)
        }
    }

    /**
     * "이번 주" 데이터로 차트를 그리는 함수
     * @param dailyData DB에서 가져온 날짜별 녹음 시간 데이터 리스트
     */
    private fun setupChartForThisWeek(dailyData: List<DailyDuration>) {
        if (!isAdded) return // 프래그먼트가 액티비티에 붙어있지 않으면 중단하여 오류 방지

        // 차트 플레이스홀더를 숨기고 실제 차트를 보이게 함
        view?.findViewById<View>(R.id.chart_placeholder)?.visibility = View.GONE
        chart.visibility = View.VISIBLE

        // 1. 이번 주의 날짜 목록(월~일)을 "YYYY-MM-DD" 형식의 문자열로 생성
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val thisWeekDates = (0..6).map {
            val dateString = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1) // 다음 날로 이동
            dateString
        }

        // 2. DB 데이터를 날짜(String)를 Key로 하여 빠르게 찾을 수 있도록 Map으로 변환
        val dataMap = dailyData.associateBy { it.day }

        // 3. 차트에 표시할 데이터(Entry) 리스트 생성
        val entries = ArrayList<Entry>()
        thisWeekDates.forEachIndexed { index, dayString ->
            // 해당 날짜의 녹음 시간을 분(minute) 단위로 변환 (데이터가 없으면 0)
            val durationMinutes = (dataMap[dayString]?.totalDuration ?: 0L) / 60000f
            entries.add(Entry(index.toFloat(), durationMinutes))
        }

        // 4. 차트 데이터셋 스타일링
        val dataSet = LineDataSet(entries, "Weekly Activity")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_line_color)
        dataSet.lineWidth = 3f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        val fillDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill)
        dataSet.fillDrawable = fillDrawable

        // 5. 차트 전체 옵션 설정 및 데이터 적용
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.setTouchEnabled(false)

        // 차트를 다시 그려서 변경사항을 화면에 반영
        chart.invalidate()
    }

    /**
     * 밀리초(Long)를 사람이 읽기 쉬운 "00h 00m" 형식의 문자열로 변환하는 헬퍼 함수
     */
    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
    }
}