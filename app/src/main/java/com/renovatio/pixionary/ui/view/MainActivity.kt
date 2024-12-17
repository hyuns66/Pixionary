package com.renovatio.pixionary.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.renovatio.pixionary.R
import com.renovatio.pixionary.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val drawerBehavior = BottomSheetBehavior.from(binding.drawer)
//        drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.toggleSearchOptionSw.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.motionRootLayout.setTransition(R.id.transition_document_home)
//                binding.root.setTransition(R.id.transition_progress_cv_down)
                binding.motionRootLayout.transitionToEnd()
            } else {
                binding.motionRootLayout.setTransition(R.id.transition_general_home)
//                binding.root.setTransition(R.id.transition_progress_cv_up)
                binding.motionRootLayout.transitionToEnd()
            }
        }
    }
}