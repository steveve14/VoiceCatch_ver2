package com.example.voicecatch_ver2

import android.app.Application

class SpamApplication : Application() {
    val textClassifier by lazy {
        TextClassifier(applicationContext)
    }
}