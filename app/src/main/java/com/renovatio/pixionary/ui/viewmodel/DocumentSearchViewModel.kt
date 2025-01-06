package com.renovatio.pixionary.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.renovatio.pixionary.domain.model.Document
import com.renovatio.pixionary.domain.usecase.DocumentSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DocumentSearchViewModel @Inject constructor(
    private val search : DocumentSearchUseCase
) : ViewModel() {
    private val _searchResults = MutableLiveData<List<Document>>(mutableListOf())
    val searchResults : LiveData<List<Document>> get() = _searchResults

    fun documentSearch(query : String){
        _searchResults.value = search(query)
    }
}