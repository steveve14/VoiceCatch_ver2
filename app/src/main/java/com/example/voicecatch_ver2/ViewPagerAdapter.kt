package com.example.voicecatch_ver2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.voicecatch_ver2.calendar.CalendarFragment
import com.example.voicecatch_ver2.file.FilesFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CalendarFragment()
            2 -> FilesFragment()
            3 -> SettingsFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}