package com.example.voicecatch_ver2.data // 데이터를 모아둘 새 패키지 생성 (권장)

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path_uri")
    val filePathUri: String, // 파일의 Content URI 저장

    @ColumnInfo(name = "transcribed_text")
    val transcribedText: String, // STT 결과 (녹음 내용)

    @ColumnInfo(name = "duration_millis")
    val durationMillis: Long, // 길이 (밀리초 단위)

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long, // 크기 (바이트 단위)

    @ColumnInfo(name = "creation_date")
    val creationDate: Long, // 생성 날짜 (Timestamp)

    @ColumnInfo(name = "ai_result")
    val aiResult: String // 예: "보이스피싱", "일반 통화", "미판별"
)