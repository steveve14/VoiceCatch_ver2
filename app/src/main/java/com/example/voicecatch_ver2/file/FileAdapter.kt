package com.example.voicecatch_ver2.file

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.voicecatch_ver2.R
import com.example.voicecatch_ver2.data.Recording

// 아이템 클릭 이벤트를 처리하기 위한 인터페이스
interface OnFileClickListener {
    fun onFileClick(recording: Recording)
}

class FileAdapter(
    private var recordings: List<Recording>,
    private val listener: OnFileClickListener
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileNameTextView: TextView = itemView.findViewById(R.id.fileNameTextView)

        fun bind(recording: Recording) {
            fileNameTextView.text = recording.fileName
            itemView.setOnClickListener {
                listener.onFileClick(recording)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(recordings[position])
    }

    override fun getItemCount(): Int = recordings.size

    fun updateData(newRecordings: List<Recording>) {
        this.recordings = newRecordings
        notifyDataSetChanged()
    }
}