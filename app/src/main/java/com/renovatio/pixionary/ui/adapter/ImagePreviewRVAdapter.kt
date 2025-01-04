package com.renovatio.pixionary.ui.adapter

import android.graphics.Bitmap
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.renovatio.pixionary.ApplicationClass
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.ItemGalleryPreviewBinding

class ImagePreviewRVAdapter(val itemWidth: Int) : RecyclerView.Adapter<ImagePreviewRVAdapter.ViewHolder>() {

    private var imageUris = mutableListOf<Uri>()
    // dp to px
    private val itemHeightPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        HEIGHT_DP.toFloat(),
        ApplicationClass.getContext().resources.displayMetrics
    ).toInt()

    class ViewHolder(val binding : ItemGalleryPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUri: Uri, itemWidth : Int, itemHeight : Int){
            val request = ImageRequest.Builder(ApplicationClass.getContext())
                .data(imageUri)
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
        return imageUris.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageUris[position], itemWidth, itemHeightPx)
    }

    fun initImagePaths(newImageUris : List<Uri>){
        imageUris.clear()
        imageUris.addAll(newImageUris)
        notifyDataSetChanged()
    }

    companion object {
        const val SPAN_COUNT = 3
        const val HEIGHT_DP = 150
    }
}