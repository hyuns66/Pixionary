package com.renovatio.pixionary.ui.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.renovatio.pixionary.ApplicationClass
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.FragmentHomeBinding
import com.renovatio.pixionary.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeModel : HomeViewModel by viewModels()
    private lateinit var context: Context

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startFeatureExtracting()
            } else {
                // 권한이 거부됨
                Toast.makeText(context, "권한이 거부되었습니다. 앱을 사용하려면 권한이 필요합니다.", Toast.LENGTH_SHORT)
                    .show()
                exitProcess(0)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

//        val drawerBehavior = BottomSheetBehavior.from(binding.drawer)
//        drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.toggleSearchOptionSw.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.motionRootLayout.setTransition(R.id.transition_document_home)
//                binding.root.setTransition(R.id.transition_progress_cv_down)
                binding.motionRootLayout.transitionToEnd()
            } else {
                binding.motionRootLayout.setTransition(R.id.transition_general_home)
//                binding.root.setTransition(R.id.transition_progress_cv_up)
                binding.motionRootLayout.transitionToEnd()
            }
        }

        binding.captureDocumentButton.setOnClickListener {
            it.findNavController().navigate(R.id.cameraPreviewFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        startFeatureExtracting()
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    private fun initObservers(){
        homeModel.totalProgressCount.observe(viewLifecycleOwner) {
            if (it == 0){
                binding.progressTotalCountTv.text = ""
            } else {
                binding.progressTotalCountTv.text = it.toString()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                homeModel.vitProgress.collect{
                    updateProgress(it)
                }
            }
        }
    }

    private fun updateProgress(progressCount: Int) {
        val maxCount = homeModel.totalProgressCount.value!!
        if (maxCount == 0 || maxCount == progressCount){
            binding.progressCountTv.text = ApplicationClass.getContext().getString(R.string.extracting_progress_complete)
            binding.progressTotalCountTv.text = ""
            binding.progressBar.progress = binding.progressBar.max
        } else {
            binding.progressBar.progress = progressCount * binding.progressBar.max / maxCount
            binding.progressCountTv.text = ApplicationClass.getContext().getString(R.string.progress_count, progressCount)
        }
    }


    private fun startFeatureExtracting() {
        // 권한이 있는지 확인하고, 없으면 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                homeModel.fetchImageItemUris(context)
                homeModel.detectUnSynchronizedImages()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else { // Android 13 미만
            if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                homeModel.fetchImageItemUris(context)
                homeModel.detectUnSynchronizedImages()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    companion object {
//        private val REQUIRED_PERMISSIONS = arrayOf(
//            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}