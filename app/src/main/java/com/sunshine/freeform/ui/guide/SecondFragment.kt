package com.sunshine.freeform.ui.guide

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.databinding.FragmentGuideSecondBinding
import com.sunshine.freeform.ui.freeform.FreeformConfig
import com.sunshine.freeform.ui.freeform.FreeformHelper
import com.sunshine.freeform.ui.splash.SplashActivity
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class SecondFragment : Fragment() {
    private lateinit var binding: FragmentGuideSecondBinding
    private val scope = MainScope()

    private var freeformStudyView: FreeformStudyViewNew? = null

    private var stepADone = false
    private var stepBDone = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuideSecondBinding.bind(inflater.inflate(R.layout.fragment_guide_second, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sp = requireActivity().getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)

        binding.fab.setOnClickListener {
            if (stepADone && stepBDone) {
                sp.edit().putInt("version", MiFreeform.VERSION).apply()
                startActivity(Intent(requireActivity(), SplashActivity::class.java))
                requireActivity().finish()
            } else {
                Snackbar
                    .make(binding.root, getString(R.string.no_step_done), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fab)
                    .show()
            }
        }

        binding.textViewGuideA.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.text_fade_in))

        scope.launch(Dispatchers.IO) {
            Thread.sleep(1000)
            withContext(Dispatchers.Main) {
                binding.buttonStepA.animate().alpha(1f).setDuration(750L).start()
                binding.buttonStepB.animate().alpha(1f).setDuration(750L).start()

                binding.buttonStepA.setOnClickListener {
                    binding.textViewStepA.visibility = View.VISIBLE
                    binding.buttonStepA.isEnabled = false
                    stepADone = true
                }

                binding.buttonStepB.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setTitle(getString(R.string.warn))
                        setMessage(getString(R.string.step_b_warn))
                        setPositiveButton(getString(R.string.to_read_step_a)) { _, _ ->}
                        setNegativeButton(getString(R.string.start_step_b)) { _, _ ->
                            if (freeformStudyView == null || callback == null) {
                                initFreeformStudyView()
                            }
                            freeformStudyView?.showWindow()
                            binding.buttonStepB.isEnabled = false
                        }
                        create().show()
                    }
                }
            }
        }
    }

    private fun initFreeformStudyView() {
        try {
            freeformStudyView = FreeformStudyViewNew(
                FreeformConfig(
                    useCustomConfig = false,
                    packageName = requireContext().packageName,
                    activityName = "${requireContext().packageName}.ui.guide.GuideStudyActivity",
                    userId = 0
                ),
                requireContext(),
                object : FreeformStudyViewNew.Callback {
                    override fun onMoveClick() {
                        GuideStudyActivity.callback?.onMoveClick()
                    }

                    override fun onCloseClick() {
                        GuideStudyActivity.callback?.onCloseClick()
                    }

                    override fun onBackClick() {
                        GuideStudyActivity.callback?.onBackClick()
                    }

                    override fun onToBackStageClick() {
                        GuideStudyActivity.callback?.onToBackStageClick()
                    }

                    override fun onToFullClick() {
                        GuideStudyActivity.callback?.onToFullClick()
                    }

                    override fun onScaleClick() {
                        GuideStudyActivity.callback?.onScaleClick()
                    }

                    override fun onSuccess() {
                        stepBDone = true
                    }
                }
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "$e", Toast.LENGTH_SHORT).show()
        }

        callback = object : FreeformStudyViewNew.Callback {
            override fun onMoveClick() {
                freeformStudyView?.setCanBack()
            }

            override fun onCloseClick() {
                freeformStudyView?.setCanToFullScreen()
            }

            override fun onBackClick() {
                freeformStudyView?.setCanClose()
            }

            override fun onToBackStageClick() {
                freeformStudyView?.setCanScale()
            }

            override fun onToFullClick() {
                freeformStudyView?.setCanToBackStage()
            }

            override fun onScaleClick() {
                freeformStudyView?.setGuideSuccess()
            }

            //借该回调用于首次设置可以移动
            override fun onSuccess() {
                freeformStudyView?.setCanMove()
            }

        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        //Android Q以下系统，当界面有悬浮窗时，会调用onPause，所以不应该再此处调用destroy。q220910.3
//        freeformStudyView?.destroy()
//        callback = null
//        binding.buttonStepB.isEnabled = true
    }

    override fun onStop() {
        super.onStop()
        freeformStudyView?.destroy()
        callback = null
        binding.buttonStepB.isEnabled = true
    }

    companion object {
        var callback: FreeformStudyViewNew.Callback? = null
    }

}