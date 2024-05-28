package com.example.findmyphoto

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.findmyphoto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    lateinit var returns_img : Array<FloatArray>
    private val galleryModel by viewModels<GalleryViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imgList = arrayListOf(
            R.drawable.img_1,
            R.drawable.img_2,
            R.drawable.img_3,
            R.drawable.img_4,
            R.drawable.img_5,
            R.drawable.img_6,
            R.drawable.img_7,
            R.drawable.img_8,
            R.drawable.img_9,
            R.drawable.img_10,
            R.drawable.img_11,
            R.drawable.img_12
        )

        binding.searchEt.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                searchImage(v.text.toString(), imgList)
            }
            handled
        }

        binding.show.visibility = View.GONE
        binding.show.setOnClickListener {
            it.visibility = View.GONE
        }

        val visionRunner = VisionTransformerRunner()
        val bitmapList = arrayListOf<Bitmap>()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        for (imgID in imgList) {
            val bitmap = BitmapFactory.decodeResource(this.resources, imgID, bmpFactoryOption)
            bitmapList.add(bitmap)
        }

        returns_img = visionRunner.runSession(bitmapList)

        // 권한이 있는지 확인하고, 없으면 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (hasPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                galleryModel.fetchImageItemList(this)
            } else {
                requestPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES, REQUEST_CODE_READ_MEDIA_IMAGES)
            }
        } else { // Android 13 미만
            if (hasPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                galleryModel.fetchImageItemList(this)
            } else {
                requestPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun searchImage(query : String, imgList : ArrayList<Int>){
        val textRunner = TextTransformerRunner()
        val returns = textRunner.runSession(arrayListOf(query))

        val calc = SimilarityCalculator(returns, returns_img)
        val answer_idx = calc.run()
        binding.show.setImageResource(imgList[answer_idx])
        binding.show.visibility = View.VISIBLE
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
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001
        private const val REQUEST_CODE_READ_MEDIA_IMAGES = 1002
    }
}