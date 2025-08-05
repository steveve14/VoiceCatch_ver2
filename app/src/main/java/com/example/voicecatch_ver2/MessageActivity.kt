package com.example.voicecatch_ver2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MessageActivity : AppCompatActivity() {

    private val PREFS_NAME = "app_settings"
    private val PREF_KEY_PHONE_NUMBER = "target_phone_number"
    private val PREF_KEY_AUTO_SEND = "auto_send_enabled" // 설정 액티비티와 동일한 키 사용

    // ... (requestSmsPermissionLauncher 코드는 이전과 동일)
    private val requestSmsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "SMS 전송 권한이 허용되었습니다. 다시 전송 버튼을 눌러주세요.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "SMS 전송 권한이 거부되어 메시지를 보낼 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_msg)

        val sendButton: Button = findViewById(R.id.sendButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsFragment::class.java))
        }

        sendButton.setOnClickListener {
            val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val phoneNumber = sharedPrefs.getString(PREF_KEY_PHONE_NUMBER, null)
            // 자동 전송 설정값을 불러옵니다.
            val isAutoSendEnabled = sharedPrefs.getBoolean(PREF_KEY_AUTO_SEND, false)
            val messageToSend = "보호자 설정 확인 메시지 입니다."

            if (phoneNumber.isNullOrEmpty()) {
                Toast.makeText(this, "수신번호가 설정되지 않았습니다. 설정 화면에서 번호를 선택해주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 설정값에 따라 분기 처리
            if (isAutoSendEnabled) {
                // 자동 전송 (백그라운드)
                sendSmsDirectly(phoneNumber, messageToSend)
            } else {
                // 수동 전송 (메시지 앱 열기)
                sendSmsViaApp(phoneNumber, messageToSend)
            }
        }
    }

    // 방법 1: 메시지 앱으로 보내기 (수동)
    private fun sendSmsViaApp(phoneNumber: String, message: String) {
        try {
            // Intent.ACTION_SENDTO를 사용하여 특정 수신인에게 데이터를 보내는 앱을 찾습니다.
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                // 'smsto:' 스킴(scheme)을 사용하여 SMS를 처리할 수 있는 앱을 타겟으로 합니다.
                // 이것이 수정된 부분입니다.
                data = Uri.parse("smsto:$phoneNumber")

                // 'sms_body'라는 키로 메시지 내용을 추가합니다.
                putExtra("sms_body", message)
            }
            startActivity(intent)
            Toast.makeText(this, "메시지 앱으로 이동합니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // ACTION_SENDTO를 처리할 수 있는 앱이 없는 경우 예외가 발생할 수 있습니다.
            e.printStackTrace()
            Toast.makeText(this, "메시지 앱을 찾을 수 없거나 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 방법 2: 바로 보내기 (자동)
    private fun sendSmsDirectly(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager: SmsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "'$phoneNumber'(으)로 '$message' 메시지를 전송했습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "메시지 전송에 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }
}