package com.example.voicecatch_ver2.calendar

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat // ContextCompat를 사용하여 색상 리소스를 안전하게 가져옴
import com.example.voicecatch_ver2.R
import java.util.Calendar
import java.util.Date

// 날짜 데이터와 보이스피싱 카운트를 함께 담을 데이터 클래스 (참고용, 현재 코드에서는 직접 사용하지 않음)
data class DayData(val date: Date, val phishingCount: Int)

class CalendarAdapter(
    private val context: Context,
    private val dayList: List<Date?>, // null은 빈 칸을 의미
    private val phishingCounts: Map<Int, Int>, // <일, 횟수>
    private val currentMonth: Int,
    private val today: Int
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = dayList.size

    override fun getItem(position: Int): Any? = dayList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_calendar_day, parent, false)
            viewHolder = ViewHolder(
                view.findViewById(R.id.tv_day),
                view.findViewById(R.id.tv_phishing_count)
            )
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val date = dayList[position]

        if (date == null) {
            // 빈 칸일 경우, 뷰를 보이지 않게 처리하거나 텍스트를 비움
            viewHolder.dayText.text = ""
            viewHolder.countText.visibility = View.GONE
            view.background = null // 배경 초기화
        } else {
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 요일 정보 가져오기 (1:일, 2:월, ..., 7:토)

            viewHolder.dayText.text = day.toString()
            viewHolder.dayText.setTypeface(null, Typeface.NORMAL) // 기본 스타일 초기화
            view.background = null // 배경 초기화

            // --- 색상 및 스타일 설정 (우선순위 적용) ---

            if (month != currentMonth) {
                // 1순위: 이번 달이 아닌 날짜는 흐리게 처리
                viewHolder.dayText.setTextColor(ContextCompat.getColor(context, R.color.neutral_change)) // colors.xml에 light_gray 정의 추천
            } else {
                // 2순위: 이번 달 날짜는 요일에 따라 색상 설정
                when (dayOfWeek) {
                    Calendar.SUNDAY -> viewHolder.dayText.setTextColor(ContextCompat.getColor(context, R.color.GoogleRed))
                    Calendar.SATURDAY -> viewHolder.dayText.setTextColor(ContextCompat.getColor(context, R.color.GoogleBlue))
                    else -> viewHolder.dayText.setTextColor(Color.BLACK)
                }

                // 3순위: 오늘 날짜는 요일 색상을 덮어쓰고 강조
                if (day == today) {
                    viewHolder.dayText.setTypeface(null, Typeface.BOLD)
                    viewHolder.dayText.setTextColor(ContextCompat.getColor(context, R.color.active_nav_color))
                    // 필요하다면 배경도 추가
                    // viewHolder.dayText.setBackgroundResource(R.drawable.today_background_circle)
                }
            }

            // 보이스피싱 횟수 표시
            val count = phishingCounts[day] ?: 0
            if (count > 0 && month == currentMonth) {
                viewHolder.countText.visibility = View.VISIBLE
                viewHolder.countText.text = "${count}건"
            } else {
                viewHolder.countText.visibility = View.GONE
            }
        }
        return view
    }

    // ViewHolder 패턴을 사용하여 성능 향상
    private class ViewHolder(val dayText: TextView, val countText: TextView)
}