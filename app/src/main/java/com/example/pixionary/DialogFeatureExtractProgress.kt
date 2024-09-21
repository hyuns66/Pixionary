package com.example.pixionary

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.pixionary.databinding.DialogFeatureExtractProgressBinding

class DialogFeatureExtractProgress(private val context : Context) : DialogFragment() {
    private var _binding: DialogFeatureExtractProgressBinding? = null
    private val binding get() = _binding!!
    private var onDismissListener: (() -> Unit)? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction.
            val builder = AlertDialog.Builder(it)
            _binding = DialogFeatureExtractProgressBinding.inflate(layoutInflater)
            binding.progressBar.progress = 0
            binding.progressCountTv.text = context.getString(R.string.progress_count, 0, 0)

            builder.setView(binding.root)
                .setCancelable(false)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // 사이즈를 조절하고 싶을 때 사용 (use it when you want to resize dialog)
    private fun resize(dialog: Dialog, width: Float, height: Float){
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val size = Point()
            windowManager.defaultDisplay.getSize(size)

            val x = (size.x * width).toInt()
            val y = (size.y * height).toInt()
            dialog.window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val x = (rect.width() * width).toInt()
            val y = (rect.height() * height).toInt()
            dialog.window?.setLayout(x, y)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Dialog가 Dismiss 될 때 액티비티에서 처리할 작업 호출
        onDismissListener?.invoke()
    }

    fun updateProgress(progressCount: Int, maxCount : Int) {
        if (_binding == null){
            return
        }
        binding.progressBar.progress = progressCount * binding.progressBar.max / maxCount
        binding.progressCountTv.text = context.getString(R.string.progress_count, progressCount, maxCount)
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

}