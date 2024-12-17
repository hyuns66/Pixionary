package com.renovatio.pixionary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import com.renovatio.pixionary.domain.usecase.VitWorkManagerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prepareUnSynchronizedImages: PrepareUnSynchronizedImagesUseCase,
    private val vitRunner: VitWorkManagerUseCase
    ) : ViewModel() {
}