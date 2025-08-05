package com.example.voicecatch_ver2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording)

    @Query("SELECT * FROM recordings ORDER BY creation_date DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): Recording?

    @Query("SELECT * FROM recordings WHERE creation_date >= :startTime AND creation_date < :endTime ORDER BY creation_date DESC")
    suspend fun getRecordingsForDate(startTime: Long, endTime: Long): List<Recording>

    @Query("""
        SELECT strftime('%Y-%m-%d', creation_date / 1000, 'unixepoch', 'localtime') as day, SUM(duration_millis) as totalDuration
        FROM recordings
        WHERE creation_date >= :startTime
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getDailyDurations(startTime: Long): List<DailyDuration>
}

data class DailyDuration(
    val day: String, // "YYYY-MM-DD" 형식의 날짜
    val totalDuration: Long // 해당 날짜의 총 녹음 시간 (밀리초)
)