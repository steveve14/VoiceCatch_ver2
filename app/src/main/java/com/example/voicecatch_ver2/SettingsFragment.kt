package com.example.voicecatch_ver2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var currentPhoneNumberTextView: TextView
    private val PREFS_NAME = "app_settings"
    private val PREF_KEY_PHONE_NUMBER = "target_phone_number"
    private val PREF_KEY_AUTO_SEND = "auto_send_enabled"

    // 연락처 선택 결과를 처리하는 런처
    // Fragment에서는 this 대신 registerForActivityResult를 직접 호출합니다.
    private val contactPickerLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val phoneNumber = getPhoneNumberFromUri(uri)
            if (phoneNumber != null) {
                savePhoneNumber(phoneNumber)
                updatePhoneNumberDisplay(phoneNumber)
                Toast.makeText(requireContext(), "번호가 저장되었습니다: $phoneNumber", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "선택한 연락처에 전화번호가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 연락처 읽기 권한 요청을 처리하는 런처
    private val requestContactPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "연락처 읽기 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 프래그먼트의 UI를 인플레이트합니다.
        // res/layout/fragment_settings.xml (또는 activity_setting.xml) 파일을 사용합니다.
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화는 onViewCreated에서 수행합니다.
        currentPhoneNumberTextView = view.findViewById(R.id.currentPhoneNumberTextView)
        val selectContactButton: Button = view.findViewById(R.id.selectContactButton)
        val autoSendSwitch: SwitchCompat = view.findViewById(R.id.autoSendSwitch)

        // 저장된 설정을 불러옵니다.
        // getSharedPreferences를 호출하기 위해 requireContext()를 사용합니다.
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAndDisplayPhoneNumber()

        // 스위치 상태를 불러와서 UI에 반영합니다.
        val isAutoSendEnabled = sharedPrefs.getBoolean(PREF_KEY_AUTO_SEND, false)
        autoSendSwitch.isChecked = isAutoSendEnabled

        // 스위치 상태 변경 리스너
        autoSendSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean(PREF_KEY_AUTO_SEND, isChecked).apply()
            val status = if (isChecked) "활성화" else "비활성화"
            Toast.makeText(requireContext(), "자동 전송이 $status 되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 버튼 클릭 리스너
        selectContactButton.setOnClickListener {
            // 권한 확인을 위해 requireContext()를 사용합니다.
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                contactPickerLauncher.launch(null)
            } else {
                requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun getPhoneNumberFromUri(uri: Uri): String? {
        var phoneNumber: String? = null
        // contentResolver를 사용하기 위해 requireActivity() 또는 requireContext()를 사용합니다.
        val contentResolver = requireActivity().contentResolver

        // 1. 선택된 연락처의 ID를 가져옵니다.
        val contactCursor = contentResolver.query(uri, null, null, null, null)
        contactCursor?.use { cCursor ->
            if (cCursor.moveToFirst()) {
                val idIndex = cCursor.getColumnIndex(ContactsContract.Contacts._ID)
                val hasPhoneIndex = cCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (idIndex < 0 || hasPhoneIndex < 0) return null

                val contactId = cCursor.getString(idIndex)
                val hasPhoneNumber = cCursor.getInt(hasPhoneIndex)

                // 2. 전화번호가 있는 경우, 해당 ID로 전화번호를 조회합니다.
                if (hasPhoneNumber > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            // 올바른 상수를 사용하여 전화번호 컬럼의 인덱스를 가져옵니다.
                            val phoneIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (phoneIndex >= 0) {
                                phoneNumber = pCursor.getString(phoneIndex)
                            }
                        }
                    }
                }
            }
        }
        return phoneNumber
    }

    private fun savePhoneNumber(phoneNumber: String) {
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(PREF_KEY_PHONE_NUMBER, phoneNumber).apply()
    }

    private fun loadAndDisplayPhoneNumber() {
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedNumber = sharedPrefs.getString(PREF_KEY_PHONE_NUMBER, null)
        updatePhoneNumberDisplay(savedNumber)
    }

    private fun updatePhoneNumberDisplay(phoneNumber: String?) {
        if (phoneNumber.isNullOrEmpty()) {
            currentPhoneNumberTextView.text = "설정된 번호가 없습니다."
        } else {
            currentPhoneNumberTextView.text = phoneNumber
        }
    }
}