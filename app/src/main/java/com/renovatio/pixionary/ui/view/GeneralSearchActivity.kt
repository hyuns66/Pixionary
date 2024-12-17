package com.renovatio.pixionary.ui.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.renovatio.pixionary.ApplicationClass
import com.renovatio.pixionary.databinding.ActivityGeneralSearchBinding
import com.renovatio.pixionary.util.VisionTransformerRunner
import com.renovatio.pixionary.databinding.DialogUnsynchronizedAlertBinding
import com.renovatio.pixionary.ui.adapter.ImagePreviewRVAdapter
import com.renovatio.pixionary.ui.viewmodel.GalleryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class GeneralSearchActivity : AppCompatActivity() {

    lateinit var binding : ActivityGeneralSearchBinding
    private val galleryModel : GalleryViewModel by viewModels()
    private val imagePreviewAdapter : ImagePreviewRVAdapter by lazy {
        val displayMetrics = ApplicationClass.getContext().resources.displayMetrics
        val displayWidth = displayMetrics!!.widthPixels
        ImagePreviewRVAdapter(
            displayWidth / ImagePreviewRVAdapter.SPAN_COUNT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneralSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initObservers()
        fetchImageItemUris()
        val unSynchronizedCount = galleryModel.detectUnSynchronizedImages()
        if (unSynchronizedCount > 0){
            // AlertDialog 생성
            val dialogBinding = DialogUnsynchronizedAlertBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root) // 커스텀 뷰 설정
                .create()
            dialogBinding.dismissTv.setOnClickListener { dialog.dismiss() }
            dialogBinding.confirmTv.setOnClickListener {
                dialog.dismiss()
                showProgressDialog(unSynchronizedCount)
            }
            dialog.show()
        }
        binding.searchEt.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                galleryModel.searchFeatures(v.text.toString())
                handled = true
            }
            handled
        }
//        binding.mainSearchIv.setOnClickListener {
//            Log.d("dialog status ", "start dialog")
//            val progressDialog = DialogFeatureExtractProgress(this)
//            progressDialog.isCancelable = false
//            progressDialog.show(supportFragmentManager, "Feature-Extracting-Progress")
//            // 다이얼로그가 Dismiss 될 때 처리할 작업 설정
//            progressDialog.setOnDismissListener {
//                galleryModel.featureProgressCount.removeObservers(this)
//            }
//            val totalCount = galleryModel.prepareExtracting()
//            // WorkRequest 생성
//            val workRequest = OneTimeWorkRequestBuilder<VitBackgroundRunner>().build()
//            // WorkManager에 작업 enqueue
//            WorkManager.getInstance(this).enqueue(workRequest)
//
//            galleryModel.featureProgressCount.observe(this){
//                progressDialog.updateProgress(it, totalCount)
//                Log.d("dialog status featureProgressCount", it.toString())
//                Log.d("dialog status totalCount", totalCount.toString())
//                if (totalCount - it < VisionTransformerRunner.BATCH_SIZE){
//                    progressDialog.dismiss()
//                }
//            }
//            galleryModel.extractFeatures(this)
//        }

        binding.imagePreviewRv.apply {
//            val preloadingCount = ImagePreviewRVAdapter.SPAN_COUNT * 20 // 사용자가 스크롤하는 동안 미리 로딩할 이미지의 수
            adapter = imagePreviewAdapter
            layoutManager = GridLayoutManager(
                this@GeneralSearchActivity,
                ImagePreviewRVAdapter.SPAN_COUNT
            )
//            ).apply {
//                initialPrefetchItemCount = preloadingCount
//            }
            setItemViewCacheSize(ImagePreviewRVAdapter.SPAN_COUNT * 20)
            setHasFixedSize(true)   // 리사이클러뷰 크기 고정 (아이템 수에 변화가 없기 때문에 사용)
            itemAnimator = null   // 애니메이션 제거
        }
    }

    private fun initObservers(){
        galleryModel.searchResults.observe(this){
            imagePreviewAdapter.initImagePaths(it.map{ item -> item.path})
        }
    }

    private fun showProgressDialog(totalCount : Int){
        val progressDialog = DialogFeatureExtractProgress()
        progressDialog.isCancelable = false
        progressDialog.show(supportFragmentManager, "Feature-Extracting-Progress")
        // 다이얼로그가 Dismiss 될 때 처리할 작업 설정
        progressDialog.setOnDismissListener {
            galleryModel.featureProgressCount.removeObservers(this)
        }
        galleryModel.startVitRunner()
        lifecycleScope.launch(Dispatchers.Main) {
            galleryModel.vitProgress.collect { state ->
                progressDialog.updateProgress(state, totalCount)
                if (totalCount - state < VisionTransformerRunner.BATCH_SIZE){
                    progressDialog.dismiss()
                }
            }
        }

//        galleryModel.vitProgress.observe(this){
//            progressDialog.updateProgress(it, totalCount)
//            if (totalCount - it < VisionTransformerRunner.BATCH_SIZE){
//                progressDialog.dismiss()
//            }
//        }
//        galleryModel.extractFeatures(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun fetchImageItemUris(){
        // 권한이 있는지 확인하고, 없으면 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (hasPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                galleryModel.fetchImageItemUris(this)
            } else {
                requestPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    REQUEST_CODE_READ_MEDIA_IMAGES
                )
            }
        } else { // Android 13 미만
            if (hasPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                galleryModel.fetchImageItemUris(this)
            } else {
                requestPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_READ_EXTERNAL_STORAGE
                )
            }
        }

    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_READ_EXTERNAL_STORAGE, REQUEST_CODE_READ_MEDIA_IMAGES -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 권한이 허용되었으므로 이미지 아이템을 가져옴
                    Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                    galleryModel.fetchImageItemUris(this)
                } else {
                    // 권한이 거부됨
                    Toast.makeText(this, "권한이 거부되었습니다. 앱을 사용하려면 권한이 필요합니다.", Toast.LENGTH_SHORT)
                        .show()
                    exitProcess(0)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001
        private const val REQUEST_CODE_READ_MEDIA_IMAGES = 1002
    }
}