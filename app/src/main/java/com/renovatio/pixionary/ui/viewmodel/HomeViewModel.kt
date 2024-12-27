package com.renovatio.pixionary.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.renovatio.pixionary.domain.usecase.LoadAllImageUrisUseCase
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import com.renovatio.pixionary.domain.usecase.VitWorkManagerUseCase
import com.renovatio.pixionary.util.VitBackgroundWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prepareUnSynchronizedImages: PrepareUnSynchronizedImagesUseCase,
    private val loadAllImageUris : LoadAllImageUrisUseCase,
    private val vitRunner: VitWorkManagerUseCase
    ) : ViewModel() {
    private val _imageItemUris = MutableLiveData<MutableList<Pair<String, Uri>>>(mutableListOf())
    val imageItemUris : LiveData<MutableList<Pair<String, Uri>>> get() = _imageItemUris
    private val _unSynchronizedItems = mutableListOf<MutableList<Pair<String, Uri>>>()
    val unSynchronizedItems get() = _unSynchronizedItems
    private val _totalProgressCount = MutableLiveData(0)
    val totalProgressCount get() = _totalProgressCount
    lateinit var vitProgress : StateFlow<Int>

    @SuppressLint("Range")
    fun fetchImageItemUris(context: Context) {
        _imageItemUris.value = loadAllImageUris(context)
    }

    // 전체 이미지들중 featureStore에 없는 이미지들만 골라서 input Data 구축
    fun detectUnSynchronizedImages(){
        _totalProgressCount.value = 0
        val dummyPair = imageItemUris.value!![0]
        _unSynchronizedItems.clear()
        _unSynchronizedItems.addAll(prepareUnSynchronizedImages(imageItemUris.value!!, dummyPair))
        startVitRunner()
        _totalProgressCount.value = _unSynchronizedItems.size * 12
    }

    private fun startVitRunner(){
        val workInfo = vitRunner()
        vitProgress = workInfo.map {
            if (it.state == WorkInfo.State.RUNNING) {
                it.progress.getInt(VitBackgroundWorker.PROGRESS_INFO_KEY, 0) // progress 값 반환
            } else {
                0 // 작업이 완료되었거나 실패했을 때 0으로 설정
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )
    }

}