package top.niunaijun.blackboxa.view.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import top.niunaijun.blackbox.utils.ErrorLogCapturer
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.BaseActivity

class ErrorLogActivity : BaseActivity() {

    private lateinit var logTextView: TextView
    private lateinit var verticalScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0D1520"))
        }

        val toolbar = Toolbar(this).apply {
            setBackgroundColor(Color.parseColor("#1565C0"))
            setTitleTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(toolbar)
        initToolbar(toolbar, R.string.error_logs, true)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#132030"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun createActionButton(label: String, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1976D2"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8
                    marginEnd = 8
                }
                setOnClickListener { onClick() }
            }
        }

        buttonBar.addView(createActionButton("Refresh") { refreshLogs(true) })
        buttonBar.addView(createActionButton("Copy") { copyLogs() })
        buttonBar.addView(createActionButton("Share") { shareLogs() })
        buttonBar.addView(createActionButton("Clear") {
            ErrorLogCapturer.clearLogs()
            refreshLogs(false)
            toast("Error logs cleared")
        })
        root.addView(buttonBar)

        verticalScrollView = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val horizontalScrollView = HorizontalScrollView(this).apply {
            isFillViewport = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        logTextView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11.5f
            setTextColor(Color.parseColor("#E3F2FD"))
            setPadding(28, 24, 28, 36)
            setTextIsSelectable(true)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        horizontalScrollView.addView(logTextView)
        verticalScrollView.addView(horizontalScrollView)
        root.addView(verticalScrollView)

        setContentView(root)
        refreshLogs(true)
    }

    override fun onResume() {
        super.onResume()
        refreshLogs(false)
    }

    private fun refreshLogs(scrollToBottom: Boolean) {
        val text = ErrorLogCapturer.readLogs()
        logTextView.text = text
        if (scrollToBottom) {
            verticalScrollView.post {
                verticalScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun copyLogs() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = logTextView.text?.toString().orEmpty()
        cm?.setPrimaryClip(ClipData.newPlainText("Clone Box Error Logs", text))
        toast("Copied error logs to clipboard")
    }

    private fun shareLogs() {
        val text = logTextView.text?.toString().orEmpty()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Clone Box Startup & Runtime Error Logs")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share Error Logs"))
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ErrorLogActivity::class.java))
        }
    }
}
