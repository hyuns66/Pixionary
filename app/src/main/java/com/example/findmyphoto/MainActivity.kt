package com.example.findmyphoto

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.load
import com.example.findmyphoto.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    private val galleryModel by viewModels<GalleryViewModel>{
        val visionTransformerRunner = VisionTransformerRunner()
        val featureRepository = FeatureRepository(ApplicationClass.getFeatureStore())
        GalleryViewModel.provideFactory(visionTransformerRunner, featureRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val displayMetrics = ApplicationClass.getContext().resources.displayMetrics
        val displayWidth = displayMetrics!!.widthPixels
        val imagePreviewAdapter = ImagePreviewRVAdapter(
            displayWidth / ImagePreviewRVAdapter.SPAN_COUNT
        )
        galleryModel.imageItemPaths.observe(this){
            imagePreviewAdapter.initImagePaths(it)
        }
        binding.searchEt.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            val imageFeatures = galleryModel.getImageFeaturesVector()
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val similarities = searchImage(v.text.toString(), imageFeatures)
                galleryModel.updateImagePaths(similarities)
                handled = true
            }
            handled
        }
        binding.mainSearchIv.setOnClickListener {
            val progressDialog = DialogFeatureExtractProgress(this)
            progressDialog.isCancelable = false
            progressDialog.show(supportFragmentManager, "Feature-Extracting-Progress")
            // 다이얼로그가 Dismiss 될 때 처리할 작업 설정
            progressDialog.setOnDismissListener {
                galleryModel.featureProgressCount.removeObservers(this)
            }
            val totalCount = galleryModel.imageItemPaths.value!!.size
            galleryModel.featureProgressCount.observe(this){
                progressDialog.updateProgress(it, totalCount)
                if (totalCount - it < VisionTransformerRunner.BATCH_SIZE){
                    progressDialog.dismiss()
                }
            }
            galleryModel.extractFeatures(this)
        }

        // 권한이 있는지 확인하고, 없으면 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (hasPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                galleryModel.fetchImageItemList(this)
            } else {
                requestPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    REQUEST_CODE_READ_MEDIA_IMAGES
                )
            }
        } else { // Android 13 미만
            if (hasPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                galleryModel.fetchImageItemList(this)
            } else {
                requestPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    REQUEST_CODE_READ_EXTERNAL_STORAGE
                )
            }
        }

        binding.imagePreviewRv.apply {
//            val preloadingCount = ImagePreviewRVAdapter.SPAN_COUNT * 20 // 사용자가 스크롤하는 동안 미리 로딩할 이미지의 수
            adapter = imagePreviewAdapter
            layoutManager = GridLayoutManager(
                this@MainActivity,
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

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun searchImage(query : String, imgFeatures : Array<FloatArray>) : ArrayList<Float>{
        val textRunner = TextTransformerRunner()
        val returns = textRunner.runSession(arrayListOf(query))

        val calc = SimilarityCalculator(returns, imgFeatures)
        return calc.run()
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
                    galleryModel.fetchImageItemList(this)
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