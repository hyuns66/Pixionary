package com.renovatio.pixionary.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.renovatio.pixionary.domain.model.Feature
import com.renovatio.pixionary.domain.usecase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GeneralSearchViewModel @Inject constructor(
    private val search : SearchUseCase
) : ViewModel() {
    private val _searchResults = MutableLiveData<List<Feature>>(mutableListOf())
    val searchResults : LiveData<List<Feature>> get() = _searchResults

    fun generalSearch(query : String){
        _searchResults.value = search(query)
    }
}