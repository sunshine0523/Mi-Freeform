package com.sunshine.freeform.ui.guide

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import com.sunshine.freeform.R
import com.sunshine.freeform.databinding.FragmentGuideFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirstFragment : Fragment() {
    private lateinit var binding: FragmentGuideFirstBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuideFirstBinding.bind(inflater.inflate(R.layout.fragment_guide_first, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewHello.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.text_fade_in))
        binding.textViewWelcome.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.text_fade_in))

        GlobalScope.launch(Dispatchers.IO) {
            Thread.sleep(1000)
            launch(Dispatchers.Main) {
                binding.buttonNext.animate().alpha(1f).setDuration(750L).start()
                binding.buttonNext.setOnClickListener {
                    findNavController().navigate(R.id.action_first_to_second)
                }
            }
        }
    }
}