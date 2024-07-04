package com.example.pixionary

import android.graphics.Bitmap
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.pixionary.databinding.ItemGalleryPreviewBinding

class ImagePreviewRVAdapter(val itemWidth: Int) : RecyclerView.Adapter<ImagePreviewRVAdapter.ViewHolder>() {

    private var imagePaths = mutableListOf<String>()
    // dp to px
    private val itemHeightPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        HEIGHT_DP.toFloat(),
        ApplicationClass.getContext().resources.displayMetrics
    ).toInt()

    class ViewHolder(val binding : ItemGalleryPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath : String, itemWidth : Int, itemHeight : Int){
            val request = ImageRequest.Builder(ApplicationClass.getContext())
                .data(imagePath)
                .crossfade(true)
                .target(binding.itemIv)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .placeholder(R.drawable.img_gallery_preview_placeholder)
                .size(itemWidth, itemHeight)
//                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            ApplicationClass.imageLoader.enqueue(request)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return imagePaths.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imagePaths[position], itemWidth, itemHeightPx)
    }

    fun initImagePaths(newImgPaths : MutableList<String>){
        imagePaths.clear()
        imagePaths.addAll(newImgPaths)
        Log.d("imagePaimimimimi", imagePaths[0])
        notifyDataSetChanged()
    }

    companion object {
        const val SPAN_COUNT = 3
        const val HEIGHT_DP = 150
    }
}