package top.niunaijun.blackboxa.view.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar

abstract class LoadingActivity : BaseActivity() {

    private var loadingDialog: Dialog? = null

    fun showLoading() {
        if (isFinishing || isDestroyed) return
        try {
            if (loadingDialog == null) {
                val container = FrameLayout(this).apply {
                    val pad = (24 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                    val progressBar = ProgressBar(this@LoadingActivity).apply {
                        isIndeterminate = true
                    }
                    val lp = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                    addView(progressBar, lp)
                }
                loadingDialog = Dialog(this).apply {
                    setContentView(container)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setOnKeyListener { _, keyCode, _ ->
                        keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
                    }
                }
            }
            if (loadingDialog?.isShowing != true) {
                loadingDialog?.show()
            }
        } catch (_: Throwable) {
        }
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        hideLoading()
        loadingDialog = null
        super.onDestroy()
    }
}