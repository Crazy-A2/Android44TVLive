package com.tvlive.app.ui.osd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.tvlive.app.R
import com.tvlive.app.player.DecoderMode
import com.tvlive.app.ui.presenter.SettingsPresenter

class SettingsOverlay(
    private val container: ViewGroup,
    private val presenter: SettingsPresenter,
    private val osdManager: OsdManager
) {

    private var rootView: View? = null
    private lateinit var settingsPanel: LinearLayout
    private lateinit var settingsList: LinearLayout
    private var isShowing = false
    private var currentPage = "main"

    fun show() {
        if (isShowing) return
        if (rootView == null) {
            rootView = LayoutInflater.from(container.context)
                .inflate(R.layout.settings_overlay_content, container, true)
            settingsPanel = rootView!!.findViewById(R.id.settings_panel)
            settingsList = rootView!!.findViewById(R.id.settings_list)
        }
        showMainMenu()
        container.visibility = View.VISIBLE
        isShowing = true
        osdManager.show(OsdManager.OsdState.SETTINGS)
        settingsPanel.requestFocus()
    }

    fun close() {
        container.visibility = View.GONE
        isShowing = false
        currentPage = "main"
        osdManager.hide()
    }

    fun isVisible(): Boolean = isShowing

    private fun showMainMenu() {
        currentPage = "main"
        settingsList.removeAllViews()

        addMenuItem("解码模式", presenter.getDecoderMode().value) {
            showDecoderModePage()
        }
        addMenuItem("源管理", "") {
            showSourceManagementPage()
        }
        addMenuItem("关于", "") {
            showAboutPage()
        }
    }

    private fun showDecoderModePage() {
        currentPage = "decoder"
        settingsList.removeAllViews()
        addBackButton()

        for (mode in presenter.getDecoderModes()) {
            val isSelected = mode == presenter.getDecoderMode()
            val label = if (isSelected) "✓ ${mode.value}" else mode.value
            val item = createMenuItem(label)
            if (isSelected) {
                item.setBackgroundColor(0x66FFFFFF.toInt())
            }
            item.setOnClickListener {
                presenter.setDecoderMode(mode)
                showMainMenu()
            }
            settingsList.addView(item)
        }
    }

    private fun showSourceManagementPage() {
        currentPage = "sources"
        settingsList.removeAllViews()
        addBackButton()

        for (config in presenter.getSourceConfigs()) {
            val status = if (config.isEnabled) "[开]" else "[关]"
            val label = "$status ${config.name} (${config.format})"
            val item = createMenuItem(label)
            item.setOnClickListener {
                presenter.toggleSourceConfig(config.id)
                showSourceManagementPage()
            }
            settingsList.addView(item)
        }

        // 添加源按钮
        val addBtn = Button(settingsList.context).apply {
            text = "+ 添加源"
            setTextColor(0xFF4CAF50.toInt())
            setBackgroundColor(0x00000000.toInt())
            setPadding(24, 16, 24, 16)
            isFocusable = true
            setOnClickListener { showAddSourcePage() }
        }
        settingsList.addView(addBtn)

        // 删除已禁用源的按钮
        val disabled = presenter.getSourceConfigs().filter { !it.isEnabled }
        if (disabled.isNotEmpty()) {
            val deleteBtn = Button(settingsList.context).apply {
                text = "删除已禁用源"
                setTextColor(0xFFE53935.toInt())
                setBackgroundColor(0x00000000.toInt())
                setPadding(24, 16, 24, 16)
                isFocusable = true
                setOnClickListener {
                    for (cfg in disabled) {
                        presenter.deleteSourceConfig(cfg.id)
                    }
                    showSourceManagementPage()
                }
            }
            settingsList.addView(deleteBtn)
        }
    }

    private fun showAddSourcePage() {
        currentPage = "add_source"
        settingsList.removeAllViews()
        addBackButton()

        val nameInput = EditText(settingsList.context).apply {
            hint = "源名称"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x88FFFFFF.toInt())
            setSingleLine()
        }
        settingsList.addView(nameInput)

        val urlInput = EditText(settingsList.context).apply {
            hint = "源 URL"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x88FFFFFF.toInt())
            setSingleLine()
        }
        settingsList.addView(urlInput)

        val saveBtn = Button(settingsList.context).apply {
            text = "保存"
            setTextColor(0xFF4CAF50.toInt())
            setBackgroundColor(0x00000000.toInt())
            isFocusable = true
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val format = if (url.endsWith(".json")) "json" else "m3u"
                    presenter.addSourceConfig(name, url, format)
                    showSourceManagementPage()
                }
            }
        }
        settingsList.addView(saveBtn)
    }

    private fun showAboutPage() {
        currentPage = "about"
        settingsList.removeAllViews()
        addBackButton()

        addTextItem("电视直播 v${presenter.getAppVersion()}")
        addTextItem("兼容 Android 4.4+")
        addTextItem("基于 IJKPlayer")
    }

    private fun addMenuItem(label: String, sublabel: String, onClick: () -> Unit) {
        val row = LinearLayout(settingsList.context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            isFocusable = true
            setOnClickListener { onClick() }
            setOnFocusChangeListener { v, hasFocus ->
                setBackgroundColor(if (hasFocus) 0x44FFFFFF.toInt() else 0x00000000.toInt())
            }
        }
        row.addView(TextView(settingsList.context).apply {
            text = label
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (sublabel.isNotEmpty()) {
            row.addView(TextView(settingsList.context).apply {
                text = sublabel
                textSize = 18f
                setTextColor(0xFFAAAAAA.toInt())
            })
        }
        settingsList.addView(row)
        settingsList.addView(createDivider())
    }

    private fun createMenuItem(label: String): View {
        return TextView(settingsList.context).apply {
            text = label
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(16, 16, 16, 16)
            isFocusable = true
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                setBackgroundColor(if (hasFocus) 0x44FFFFFF.toInt() else 0x00000000.toInt())
            }
        }
    }

    private fun addTextItem(text: String) {
        settingsList.addView(TextView(settingsList.context).apply {
            this.text = text
            textSize = 18f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(16, 12, 16, 12)
        })
    }

    private fun addBackButton() {
        val backBtn = Button(settingsList.context).apply {
            text = "← 返回"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x00000000.toInt())
            setPadding(24, 16, 24, 16)
            isFocusable = true
            setOnClickListener { showMainMenu() }
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                setBackgroundColor(if (hasFocus) 0x44FFFFFF.toInt() else 0x00000000.toInt())
            }
        }
        settingsList.addView(backBtn)
        settingsList.addView(createDivider())
    }

    private fun createDivider(): View {
        return View(settingsList.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(0x44444444.toInt())
        }
    }
}
