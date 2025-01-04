package com.renovatio.pixionary.ui.view

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.common.util.concurrent.ListenableFuture
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.FragmentCameraPreviewBinding
import com.renovatio.pixionary.databinding.FragmentGeneralHomeBinding
import com.renovatio.pixionary.ui.viewmodel.CameraViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CameraPreviewFragment : Fragment() {
    private var _binding: FragmentCameraPreviewBinding? = null
    private val binding get() = _binding!! // null-safe 접근
    private lateinit var safeContext: Context
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val cameraModel : CameraViewModel by navGraphViewModels(R.id.nav_graph) { defaultViewModelProviderFactory }
    private var camera: Camera? = null
    private var imageCapture : ImageCapture? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openPreview() // 권한이 허용되었을 때 동작
        } else {
            Toast.makeText(safeContext, "카메라 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            val transaction = parentFragmentManager.beginTransaction()
            transaction.remove(this) // 현재 Fragment를 제거
            transaction.commit()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Request camera permissions
        if (allPermissionsGranted()) {
            openPreview()
        } else {
            checkCameraPermission()
        }
        binding.cameraShutterBtn.setOnClickListener {
            capturePhoto()
        }
        binding.cameraPreview.scaleType = PreviewView.ScaleType.FIT_CENTER
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openPreview(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 프리뷰 설정
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraPreview.surfaceProvider
            }
            // 카메라 선택 (후면 카메라)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder().build()

            try {
                // 카메라를 바인딩 해제
                cameraProvider.unbindAll()

                // 카메라를 Preview와 바인딩
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "사용 중 오류 발생: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(safeContext))
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(safeContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    // 이미지 미리보기 화면에 전달 (예: Fragment나 Activity로 전달)
                    cameraModel.setCapturedImage(image)
                    binding.root.findNavController().navigate(R.id.cameraCapturePreviewFragment)

                    // 캡처 후 이미지 해제
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Pixionary : Capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun checkCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA) // 권한 요청
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG = "CameraXFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        var isOffline = false // prevent app crash when goes offline
    }

}