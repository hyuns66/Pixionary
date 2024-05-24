package com.example.findmyphoto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.findmyphoto.databinding.ActivityMainBinding
import java.lang.Thread.sleep

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding

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
        val visionRunner = VisionTransformerRunner()
        val bitmapList = arrayListOf<Bitmap>()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        for (imgID in imgList){
            val bitmap = BitmapFactory.decodeResource(this.resources, imgID, bmpFactoryOption)
            bitmapList.add(bitmap)
        }

//        val textRunner = TextTransformerRunner()
//        val returns = textRunner.runSession(arrayListOf("my name is king."))
//
//        visionRunner.runSession(bitmapList)

        val returns = arrayOf(floatArrayOf(3f, 4f, 5f))
        val calc = SimilarityCalculator(returns)
        calc.normalizeVector(returns)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}