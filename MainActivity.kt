package com.privacy.display

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "PrivacyDisplayPrefs"
        const val KEY_OVERLAY_ACTIVE = "overlay_active"
        const val KEY_PRIVACY_LEVEL = "privacy_level"
        const val KEY_PATTERN_TYPE = "pattern_type"
        const val KEY_CENTER_CLARITY = "center_clarity"
        const val KEY_AMOLED_MODE = "amoled_mode"
        const val PERMISSION_REQUEST_CODE = 1001

        // Pattern types
        const val PATTERN_CHECKERBOARD = 0
        const val PATTERN_HORIZONTAL = 1
        const val PATTERN_VERTICAL = 2
        const val PATTERN_DOTS = 3
    }

    private lateinit var prefs: SharedPreferences

    // UI Elements
    private lateinit var btnToggle: Button
    private lateinit var statusIndicator: TextView
    private lateinit var statusDot: TextView
    private lateinit var sliderPrivacyLevel: SeekBar
    private lateinit var tvPrivacyLevelValue: TextView
    private lateinit var rgPatternType: RadioGroup
    private lateinit var rbCheckerboard: RadioButton
    private lateinit var rbHorizontal: RadioButton
    private lateinit var rbVertical: RadioButton
    private lateinit var rbDots: RadioButton
    private lateinit var switchCenterClarity: Switch
    private lateinit var switchAmoledMode: Switch
    private lateinit var tvPermissionStatus: TextView
    private lateinit var btnRequestPermission: Button
    private lateinit var layoutControls: LinearLayout
    private lateinit var tvPrivacyEffect: TextView
    private lateinit var sliderOpacity: SeekBar
    private lateinit var tvOpacityValue: TextView
    private lateinit var sliderPatternSize: SeekBar
    private lateinit var tvPatternSizeValue: TextView

    private var isOverlayActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        initViews()
        loadSettings()
        setupListeners()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        syncOverlayState()
    }

    private fun initViews() {
        btnToggle = findViewById(R.id.btnToggle)
        statusIndicator = findViewById(R.id.tvStatus)
        statusDot = findViewById(R.id.tvStatusDot)
        sliderPrivacyLevel = findViewById(R.id.sliderPrivacyLevel)
        tvPrivacyLevelValue = findViewById(R.id.tvPrivacyLevelValue)
        rgPatternType = findViewById(R.id.rgPatternType)
        rbCheckerboard = findViewById(R.id.rbCheckerboard)
        rbHorizontal = findViewById(R.id.rbHorizontal)
        rbVertical = findViewById(R.id.rbVertical)
        rbDots = findViewById(R.id.rbDots)
        switchCenterClarity = findViewById(R.id.switchCenterClarity)
        switchAmoledMode = findViewById(R.id.switchAmoledMode)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        btnRequestPermission = findViewById(R.id.btnRequestPermission)
        layoutControls = findViewById(R.id.layoutControls)
        tvPrivacyEffect = findViewById(R.id.tvPrivacyEffect)
        sliderOpacity = findViewById(R.id.sliderOpacity)
        tvOpacityValue = findViewById(R.id.tvOpacityValue)
        sliderPatternSize = findViewById(R.id.sliderPatternSize)
        tvPatternSizeValue = findViewById(R.id.tvPatternSizeValue)
    }

    private fun loadSettings() {
        isOverlayActive = prefs.getBoolean(KEY_OVERLAY_ACTIVE, false)
        val privacyLevel = prefs.getInt(KEY_PRIVACY_LEVEL, 50)
        val patternType = prefs.getInt(KEY_PATTERN_TYPE, PATTERN_CHECKERBOARD)
        val centerClarity = prefs.getBoolean(KEY_CENTER_CLARITY, true)
        val amoledMode = prefs.getBoolean(KEY_AMOLED_MODE, true)

        sliderPrivacyLevel.progress = privacyLevel
        updatePrivacyLevelDisplay(privacyLevel)

        sliderOpacity.progress = prefs.getInt("opacity", 60)
        updateOpacityDisplay(sliderOpacity.progress)

        sliderPatternSize.progress = prefs.getInt("pattern_size", 3)
        updatePatternSizeDisplay(sliderPatternSize.progress)

        when (patternType) {
            PATTERN_CHECKERBOARD -> rbCheckerboard.isChecked = true
            PATTERN_HORIZONTAL -> rbHorizontal.isChecked = true
            PATTERN_VERTICAL -> rbVertical.isChecked = true
            PATTERN_DOTS -> rbDots.isChecked = true
        }

        switchCenterClarity.isChecked = centerClarity
        switchAmoledMode.isChecked = amoledMode

        updateToggleButton()
    }

    private fun setupListeners() {
        btnToggle.setOnClickListener {
            if (hasOverlayPermission()) {
                toggleOverlay()
            } else {
                requestOverlayPermission()
            }
        }

        btnRequestPermission.setOnClickListener {
            requestOverlayPermission()
        }

        sliderPrivacyLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePrivacyLevelDisplay(progress)
                if (fromUser) saveSettings()
                if (isOverlayActive) updateOverlay()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateOpacityDisplay(progress)
                if (fromUser) saveSettings()
                if (isOverlayActive) updateOverlay()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderPatternSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePatternSizeDisplay(progress)
                if (fromUser) saveSettings()
                if (isOverlayActive) updateOverlay()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rgPatternType.setOnCheckedChangeListener { _, _ ->
            saveSettings()
            if (isOverlayActive) updateOverlay()
        }

        switchCenterClarity.setOnCheckedChangeListener { _, _ ->
            saveSettings()
            if (isOverlayActive) updateOverlay()
        }

        switchAmoledMode.setOnCheckedChangeListener { _, _ ->
            saveSettings()
            if (isOverlayActive) updateOverlay()
        }
    }

    private fun toggleOverlay() {
        isOverlayActive = !isOverlayActive
        prefs.edit().putBoolean(KEY_OVERLAY_ACTIVE, isOverlayActive).apply()

        if (isOverlayActive) {
            startOverlayService()
        } else {
            stopOverlayService()
        }

        updateToggleButton()
    }

    private fun startOverlayService() {
        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_START
            putExtras(buildServiceBundle())
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_STOP
        }
        startService(intent)
    }

    private fun updateOverlay() {
        if (!isOverlayActive) return
        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_UPDATE
            putExtras(buildServiceBundle())
        }
        startService(intent)
    }

    private fun buildServiceBundle(): android.os.Bundle {
        return android.os.Bundle().apply {
            putInt("privacy_level", sliderPrivacyLevel.progress)
            putInt("opacity", sliderOpacity.progress)
            putInt("pattern_size", sliderPatternSize.progress + 2)
            putInt("pattern_type", getSelectedPatternType())
            putBoolean("center_clarity", switchCenterClarity.isChecked)
            putBoolean("amoled_mode", switchAmoledMode.isChecked)
        }
    }

    private fun getSelectedPatternType(): Int {
        return when (rgPatternType.checkedRadioButtonId) {
            R.id.rbCheckerboard -> PATTERN_CHECKERBOARD
            R.id.rbHorizontal -> PATTERN_HORIZONTAL
            R.id.rbVertical -> PATTERN_VERTICAL
            R.id.rbDots -> PATTERN_DOTS
            else -> PATTERN_CHECKERBOARD
        }
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putInt(KEY_PRIVACY_LEVEL, sliderPrivacyLevel.progress)
            putInt(KEY_PATTERN_TYPE, getSelectedPatternType())
            putBoolean(KEY_CENTER_CLARITY, switchCenterClarity.isChecked)
            putBoolean(KEY_AMOLED_MODE, switchAmoledMode.isChecked)
            putInt("opacity", sliderOpacity.progress)
            putInt("pattern_size", sliderPatternSize.progress)
            apply()
        }
    }

    private fun updatePrivacyLevelDisplay(progress: Int) {
        val level = when {
            progress < 34 -> "Level 1 — Light"
            progress < 67 -> "Level 2 — Medium"
            else -> "Level 3 — Strong"
        }
        tvPrivacyLevelValue.text = level

        val effectText = when {
            progress < 34 -> "~20–30% readability reduction from distance"
            progress < 67 -> "~35–45% readability reduction from distance"
            else -> "~50–60% readability reduction from distance"
        }
        tvPrivacyEffect.text = effectText
    }

    private fun updateOpacityDisplay(progress: Int) {
        tvOpacityValue.text = "$progress%"
    }

    private fun updatePatternSizeDisplay(progress: Int) {
        val size = progress + 2
        tvPatternSizeValue.text = "${size}px"
    }

    private fun updateToggleButton() {
        if (isOverlayActive) {
            btnToggle.text = "DISABLE PRIVACY MODE"
            btnToggle.setBackgroundColor(getColor(R.color.colorAccentRed))
            statusIndicator.text = "ACTIVE"
            statusDot.text = "●"
            statusDot.setTextColor(getColor(R.color.colorGreen))
            statusIndicator.setTextColor(getColor(R.color.colorGreen))
        } else {
            btnToggle.text = "ENABLE PRIVACY MODE"
            btnToggle.setBackgroundColor(getColor(R.color.colorAccent))
            statusIndicator.text = "INACTIVE"
            statusDot.text = "●"
            statusDot.setTextColor(getColor(R.color.colorGray))
            statusIndicator.setTextColor(getColor(R.color.colorGray))
        }
    }

    private fun syncOverlayState() {
        val serviceRunning = PrivacyOverlayService.isRunning
        if (isOverlayActive != serviceRunning) {
            isOverlayActive = serviceRunning
            prefs.edit().putBoolean(KEY_OVERLAY_ACTIVE, isOverlayActive).apply()
            updateToggleButton()
        }
    }

    private fun checkPermissions() {
        if (hasOverlayPermission()) {
            tvPermissionStatus.text = "✓ Overlay permission granted"
            tvPermissionStatus.setTextColor(getColor(R.color.colorGreen))
            btnRequestPermission.visibility = android.view.View.GONE
            layoutControls.visibility = android.view.View.VISIBLE
            btnToggle.isEnabled = true
        } else {
            tvPermissionStatus.text = "⚠ Overlay permission required"
            tvPermissionStatus.setTextColor(getColor(R.color.colorYellow))
            btnRequestPermission.visibility = android.view.View.VISIBLE
            layoutControls.visibility = android.view.View.VISIBLE
            btnToggle.isEnabled = false
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissions()
        }
    }
}
