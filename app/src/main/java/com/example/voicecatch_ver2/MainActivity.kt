package com.example.voicecatch_ver2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem // 이 import 문이 필요합니다.
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.example.voicecatch_ver2.file.FilesFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var headerTitle: TextView

    private lateinit var navHome: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navFiles: LinearLayout
    private lateinit var navSettings: LinearLayout

    private lateinit var navHomeIcon: ImageView
    private lateinit var navCalendarIcon: ImageView
    private lateinit var navFilesIcon: ImageView
    private lateinit var navSettingsIcon: ImageView

    private lateinit var navHomeLabel: TextView
    private lateinit var navCalendarLabel: TextView
    private lateinit var navFilesLabel: TextView
    private lateinit var navSettingsLabel: TextView

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        menuButton = findViewById(R.id.menu_button)

        // 기존 뷰 초기화
        setupViews()

        // 드로어 설정
        setupDrawer()

        // 기존 로직
        setupViewPager()
        setupBottomNavigation()
        updateBottomNavigationState(0)
        updateHeaderTitle(0)
    }

    private fun setupDrawer() {
        // 메뉴 버튼 클릭 시 드로어 열기
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // 드로어 메뉴 아이템 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(this)
    }

    // 드로어 메뉴 아이템이 클릭되었을 때 호출되는 함수
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // 1. 먼저 파일 탭으로 이동시킵니다.
        // 두 메뉴 모두 파일 탭으로 이동해야 하므로 when 문 밖으로 뺍니다.
        if (item.itemId == R.id.nav_drawer_create_dummy_file || item.itemId == R.id.nav_drawer_create_dummy_file_v) {
            viewPager.currentItem = 2
        }

        // 2. 약간의 딜레이 후, 선택된 메뉴에 맞는 동작을 수행합니다.
        Handler(Looper.getMainLooper()).postDelayed({
            when (item.itemId) {
                R.id.nav_drawer_create_dummy_file -> {
                    // FilesFragment를 찾아 일반 더미 생성 함수를 호출합니다.
                    findFilesFragmentAndExecute { fragment ->
                        fragment.createDummyRecording()
                        Toast.makeText(this, "일반 더미 파일 1개가 생성되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_drawer_create_dummy_file_v -> {
                    // FilesFragment를 찾아 보이스피싱 더미 생성 함수를 호출합니다.
                    findFilesFragmentAndExecute { fragment ->
                        fragment.createPhishingDummyRecording()
                        Toast.makeText(this, "보이스피싱 더미 파일 1개가 생성되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_drawer_messages -> {
                    // 1. MessageActivity로 이동할 Intent를 생성합니다.
                    val intent = Intent(this, MessageActivity::class.java)
                    // 2. 새로운 액티비티를 시작합니다.
                    startActivity(intent)
                }
                R.id.nav_drawer_whisper -> {
                    // 1. MessageActivity로 이동할 Intent를 생성합니다.
                    val intent = Intent(this, WhisperActivity::class.java)
                    // 2. 새로운 액티비티를 시작합니다.
                    startActivity(intent)
                }
                R.id.nav_drawer_bert -> {
                    // 1. MessageActivity로 이동할 Intent를 생성합니다.
                    val intent = Intent(this, BertActivity::class.java)
                    // 2. 새로운 액티비티를 시작합니다.
                    startActivity(intent)
                }
            }
        }, 100) // 0.1초 딜레이

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun findFilesFragmentAndExecute(action: (FilesFragment) -> Unit) {
        // ViewPager2가 프래그먼트를 생성할 때 사용하는 공식 태그는 "f" + position 입니다.
        val fragment = supportFragmentManager.findFragmentByTag("f2")
        if (fragment is FilesFragment) {
            // 프래그먼트를 찾았다면 전달받은 action을 실행합니다.
            action(fragment)
        } else {
            // 프래그먼트를 찾지 못했을 경우 사용자에게 알립니다.
            Toast.makeText(this, "파일 프래그먼트가 활성화되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 뒤로가기 버튼을 눌렀을 때 드로어가 열려있으면 먼저 닫히게 처리
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupViews() {
        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        menuButton = findViewById(R.id.menu_button)
        headerTitle = findViewById(R.id.header_title)

        // ViewPager
        viewPager = findViewById(R.id.view_pager)

        // Bottom Navigation
        navHome = findViewById(R.id.nav_home)
        navCalendar = findViewById(R.id.nav_calendar)
        navFiles = findViewById(R.id.nav_files)
        navSettings = findViewById(R.id.nav_settings)

        navHomeIcon = findViewById(R.id.nav_home_icon)
        navCalendarIcon = findViewById(R.id.nav_calendar_icon)
        navFilesIcon = findViewById(R.id.nav_files_icon)
        navSettingsIcon = findViewById(R.id.nav_settings_icon)

        navHomeLabel = findViewById(R.id.nav_home_label)
        navCalendarLabel = findViewById(R.id.nav_calendar_label)
        navFilesLabel = findViewById(R.id.nav_files_label)
        navSettingsLabel = findViewById(R.id.nav_settings_label)
    }

    private fun setupViewPager() {
        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigationState(position)
                updateHeaderTitle(position)
            }
        })
    }

    private fun setupBottomNavigation() {
        navHome.setOnClickListener { viewPager.currentItem = 0 }
        navCalendar.setOnClickListener { viewPager.currentItem = 1 }
        navFiles.setOnClickListener { viewPager.currentItem = 2 }
        navSettings.setOnClickListener { viewPager.currentItem = 3 }
    }

    private fun updateHeaderTitle(position: Int) {
        headerTitle.text = when (position) {
            0 -> "Home"
            1 -> "Calendar"
            2 -> "Files"
            3 -> "Settings"
            else -> ""
        }
    }

    private fun updateBottomNavigationState(position: Int) {
        val inactiveColor = ContextCompat.getColor(this, R.color.inactive_nav_color)
        navHomeIcon.setColorFilter(inactiveColor)
        navHomeLabel.setTextColor(inactiveColor)
        navCalendarIcon.setColorFilter(inactiveColor)
        navCalendarLabel.setTextColor(inactiveColor)
        navFilesIcon.setColorFilter(inactiveColor)
        navFilesLabel.setTextColor(inactiveColor)
        navSettingsIcon.setColorFilter(inactiveColor)
        navSettingsLabel.setTextColor(inactiveColor)

        val activeColor = ContextCompat.getColor(this, R.color.active_nav_color)
        when (position) {
            0 -> {
                navHomeIcon.setColorFilter(activeColor)
                navHomeLabel.setTextColor(activeColor)
            }
            1 -> {
                navCalendarIcon.setColorFilter(activeColor)
                navCalendarLabel.setTextColor(activeColor)
            }
            2 -> {
                navFilesIcon.setColorFilter(activeColor)
                navFilesLabel.setTextColor(activeColor)
            }
            3 -> {
                navSettingsIcon.setColorFilter(activeColor)
                navSettingsLabel.setTextColor(activeColor)
            }
        }
    }
}