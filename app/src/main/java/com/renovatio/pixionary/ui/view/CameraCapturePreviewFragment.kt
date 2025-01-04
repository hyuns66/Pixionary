package com.renovatio.pixionary.ui.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.FragmentCameraCapturePreviewBinding
import com.renovatio.pixionary.databinding.FragmentCameraPreviewBinding
import com.renovatio.pixionary.ui.viewmodel.CameraViewModel
import com.renovatio.pixionary.util.DimensionUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CameraCapturePreviewFragment : Fragment() {
    private var _binding: FragmentCameraCapturePreviewBinding? = null
    private val binding get() = _binding!! // null-safe 접근
    private val cameraModel : CameraViewModel by navGraphViewModels(R.id.nav_graph) { defaultViewModelProviderFactory }
    private lateinit var context : Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraCapturePreviewBinding.inflate(inflater, container, false)

        val behavior = BottomSheetBehavior.from(
            binding.recognizeTextBottomSheet
        )
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED // 초기 상태 설정
        behavior.peekHeight = DimensionUtil.dpToPx(150f, context).toInt() // 기본 높이
        behavior.isHideable = false // 숨길 수 있는지 여부 설정

        binding.backIv.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.imgSaveTv.setOnClickListener{
            cameraModel.saveImage()
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initObservers(){
        cameraModel.imageBitmap.observe(viewLifecycleOwner) {
            binding.capturePreviewIv.setImageBitmap(it)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cameraModel.recognizeTextResult.collect {
                    when {
                        it == null -> {
                            binding.recognizedTextTv.text = context.getString(R.string.recognizing_text_alert)
                        }
                        it.isSuccess -> {
                            binding.recognizedTextTv.text = it.getOrNull()
                        }
                        it.isFailure -> {
                            Toast.makeText(context, it.exceptionOrNull()!!.message, Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cameraModel.toastMessageEvents.collectLatest {
                    Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}