package com.renovatio.pixionary.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.renovatio.pixionary.databinding.FragmentGeneralHomeBinding

class GeneralSearchHomeFragment : Fragment() {
    private var _binding: FragmentGeneralHomeBinding? = null
    private val binding get() = _binding!! // null-safe 접근

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneralHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}