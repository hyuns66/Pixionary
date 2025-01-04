package com.renovatio.pixionary.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.renovatio.pixionary.ApplicationClass
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.FragmentGeneralSearchBinding
import com.renovatio.pixionary.ui.adapter.ImagePreviewRVAdapter
import com.renovatio.pixionary.ui.viewmodel.CameraViewModel
import com.renovatio.pixionary.ui.viewmodel.GeneralSearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.wait

@AndroidEntryPoint
class GeneralSearchFragment : Fragment() {
    private var _binding : FragmentGeneralSearchBinding? = null
    private val binding get() = _binding!!
    private val searchModel : GeneralSearchViewModel by viewModels()
    private val imagePreviewAdapter : ImagePreviewRVAdapter by lazy {
        val displayMetrics = ApplicationClass.getContext().resources.displayMetrics
        val displayWidth = displayMetrics!!.widthPixels
        ImagePreviewRVAdapter(
            displayWidth / ImagePreviewRVAdapter.SPAN_COUNT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneralSearchBinding.inflate(inflater, container, false)

        initialSearch()
        binding.searchEt.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchModel.generalSearch(v.text.toString())
                handled = true
            }
            handled
        }
        binding.mainSearchIv.setOnClickListener {
            searchModel.generalSearch(binding.searchEt.text.toString())
        }
        binding.galleryThumbnailRv.apply {
//            val preloadingCount = ImagePreviewRVAdapter.SPAN_COUNT * 20 // 사용자가 스크롤하는 동안 미리 로딩할 이미지의 수
            adapter = imagePreviewAdapter
            layoutManager = GridLayoutManager(
                requireContext(),
                ImagePreviewRVAdapter.SPAN_COUNT
            )
//            ).apply {
//                initialPrefetchItemCount = preloadingCount
//            }
            setItemViewCacheSize(ImagePreviewRVAdapter.SPAN_COUNT * 20)
            setHasFixedSize(true)   // 리사이클러뷰 크기 고정 (아이템 수에 변화가 없기 때문에 사용)
            itemAnimator = null   // 애니메이션 제거
        }
        initObservers()
        return binding.root
    }

    private fun initObservers(){
        searchModel.searchResults.observe(viewLifecycleOwner){
            imagePreviewAdapter.initImagePaths(it.map{ item -> item.uri})
        }
    }
    private fun initialSearch(){
        val args: GeneralSearchFragmentArgs by navArgs()
        binding.searchEt.setText(args.query)
        searchModel.generalSearch(args.query)
    }
}