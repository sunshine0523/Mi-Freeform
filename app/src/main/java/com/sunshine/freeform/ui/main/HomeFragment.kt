package com.sunshine.freeform.ui.main

import android.app.IActivityTaskManager
import android.app.TaskStackListener
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.databinding.FragmentHomeBinding
import com.sunshine.freeform.hook.utils.HookTest
import com.sunshine.freeform.service.KeepAliveService
import com.sunshine.freeform.ui.guide.GuideActivity
import com.sunshine.freeform.utils.PermissionUtils
import rikka.sui.Sui

class HomeFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private lateinit var accessibilityRFAR: ActivityResultLauncher<Intent>
    private lateinit var sp: SharedPreferences

    companion object {
        private const val TAG = "HomeFragment"
        private const val MY_COOLAPK_PAGE = "http://www.coolapk.com/u/810697"
        private const val COOLAPK_PACKAGE = "com.coolapk.market"
        private const val MARKET_ID = "market://details?id=com.sunshine.freeform"
        private const val QQ_CHANNEL = "https://qun.qq.com/qqweb/qunpro/share?_wv=3&_wwv=128&inviteCode=XKL1t&from=246610&biz=ka"
        private const val QQ_GROUP2 = "https://jq.qq.com/?_wv=1027&k=Ima6Egv1"
        private const val TELEGRAM_URL = "https://t.me/+8M3IrjRFiPE2NGE9"
        private const val OPEN_SOURCE_URL = "https://github.com/sunshine0523/Mi-FreeForm"
        private const val PAY_PAL_URL = "https://www.paypal.com/paypalme/mifreeform"
        private const val COMMON_QUESTION_ZH = "https://github.com/sunshine0523/Mi-FreeForm/blob/master/qa_zh-Hans.md"
        private const val OPEN_API_ZH = "https://github.com/sunshine0523/Mi-FreeForm/blob/master/open_api_zh-Hans.md"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sp = requireContext().getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        checkShizukuPermission()
        checkXposedPermission()
        checkAccessibilityPermission()
        accessibilityRFAR = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkAccessibilityPermission()
        }

        binding.materialCardViewXposedInfo.setOnClickListener(this)
        binding.materialCardViewShizukuInfo.setOnClickListener(this)
        binding.materialCardViewAccessibilityInfo.setOnClickListener(this)
        binding.buttonGuide.setOnClickListener(this)
        binding.buttonQuestion.setOnClickListener(this)
        binding.buttonOpenApi.setOnClickListener(this)
        binding.buttonDonateAlipay.setOnClickListener(this)
        binding.buttonDonateWechatPay.setOnClickListener(this)
        binding.buttonDonatePaypal.setOnClickListener(this)
        binding.buttonStar.setOnClickListener(this)
        binding.buttonCoolapk.setOnClickListener(this)
        binding.buttonQqGroup.setOnClickListener(this)
        binding.buttonQqGroup2.setOnClickListener(this)
        binding.buttonQqChannel.setOnClickListener(this)
        binding.buttonTelegram.setOnClickListener(this)
        binding.buttonOpenSource.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        checkAccessibilityPermission()
    }

    private fun checkAccessibilityPermission() {
        val result = PermissionUtils.isAccessibilitySettingsOn(requireContext())

        when (sp.getInt("service_type", KeepAliveService.SERVICE_TYPE)) {
            KeepAliveService.SERVICE_TYPE -> {
                if (!result) {
                    binding.materialCardViewAccessibilityInfo.visibility = View.VISIBLE
                    binding.infoAccessibilityBg.setBackgroundColor(resources.getColor(R.color.warn_color))
                    binding.imageViewAccessibilityService.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error_white))
                    binding.textViewAccessibilityServiceInfo.text = getString(R.string.accessibility_no_start)
                } else {
                    binding.materialCardViewAccessibilityInfo.visibility = View.GONE
                }
            }
            else -> {
                binding.materialCardViewAccessibilityInfo.visibility = View.GONE
            }
        }
    }

    private fun checkShizukuPermission(): Boolean {
        val result = MiFreeform.me?.isRunning?.value!!
        if (result) {
            binding.infoShizukuBg.setBackgroundColor(resources.getColor(R.color.success_color))
            binding.imageViewShizukuService.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_done))
            binding.textViewShizukuServiceInfo.text = if (Sui.isSui()) getString(R.string.sui_start_short) else getString(R.string.shizuku_start_short)
        } else {
            binding.infoShizukuBg.setBackgroundColor(resources.getColor(R.color.warn_color))
            binding.imageViewShizukuService.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error_white))
            binding.textViewShizukuServiceInfo.text = getString(R.string.shizuku_no_start)
        }
        return result
    }

    private fun checkXposedPermission() {
        val isActive = HookTest.checkXposed()
        if (isActive) {
            binding.infoXposedBg.setBackgroundColor(resources.getColor(R.color.success_color))
            binding.imageViewXposedService.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_done))
            binding.textViewXposedServiceInfo.text = getString(R.string.xposed_start_short)
            binding.textViewXposedServiceInfo.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.materialCardView_xposed_info -> {
                if (HookTest.checkXposed()) {
                    Snackbar.make(binding.root, getString(R.string.xposed_start), Snackbar.LENGTH_SHORT).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setTitle(getString(R.string.warn))
                        setMessage(getString(R.string.try_to_init_xposed))
                        create().show()
                    }
                }
            }
            R.id.materialCardView_shizuku_info -> {
                MiFreeform.me?.initShizuku()
                if (checkShizukuPermission()) {
                    Snackbar.make(binding.root, getString(R.string.shizuku_start), Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, getString(R.string.try_to_init_shizuku), Snackbar.LENGTH_SHORT).show()
                }

            }

            R.id.materialCardView_accessibility_info -> {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(getString(R.string.warn))
                    setMessage(getString(R.string.home_accessibility_warn_message))
                    setPositiveButton(getString(R.string.go_to_start_accessibility)) { _, _ ->
                        accessibilityRFAR.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    setNegativeButton(getString(R.string.go_to_change_service_type)) { _, _ ->
                        (requireActivity() as MainActivity).changeToSetting()
                    }
                    create().show()
                }
            }

            R.id.button_guide -> {
                startActivity(Intent(requireContext(), GuideActivity::class.java))
            }

            R.id.button_question -> {
                val uri = Uri.parse(COMMON_QUESTION_ZH)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }

            R.id.button_open_api -> {
                val uri = Uri.parse(OPEN_API_ZH)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }

            R.id.button_donate_alipay -> {
                try {
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    val payUrl = "HTTPS://QR.ALIPAY.COM/fkx18133hemtjbe1id3m558"
                    intent.data = Uri.parse("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + payUrl);
                    startActivity(intent)
                }
                catch (e: Exception) {
                    Snackbar.make(binding.root, getString(R.string.open_alipay_fail), Snackbar.LENGTH_SHORT).show()
                }
            }
            R.id.button_donate_wechat_pay -> {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(getString(R.string.donate_wechat_title))
                    val imageView = ImageView(requireContext()).apply {
                        setImageResource(R.mipmap.wechat)
                    }
                    setView(imageView)
                    setPositiveButton(getString(R.string.done)) {_, _ ->}
                    create().show()
                }
            }
            R.id.button_donate_paypal -> {
                val uri = Uri.parse(PAY_PAL_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_star -> {
                try {
                    val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_ID))
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    localIntent.`package` = COOLAPK_PACKAGE
                    startActivity(localIntent)
                } catch (e: Exception) {
                    try {
                        val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_ID))
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(localIntent)
                    }catch (e : Exception){
                        Toast.makeText(requireContext(), getString(R.string.start_market_fail), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            R.id.button_coolapk -> {
                try {
                    val str = MY_COOLAPK_PAGE
                    val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(str))
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    localIntent.`package` = COOLAPK_PACKAGE
                    startActivity(localIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.start_coolapk_fail), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.button_qq_group -> {
                try {
                    val intent = Intent()
                    val key = "qNbvThGAg7lPnCfLNWL-NKw0Teaso05e"
                    intent.data =
                        Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.start_qq_fail), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.button_qq_group2 -> {
                val uri = Uri.parse(QQ_GROUP2)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_qq_channel -> {
                val uri = Uri.parse(QQ_CHANNEL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_telegram -> {
                val uri = Uri.parse(TELEGRAM_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_open_source -> {
                val uri = Uri.parse(OPEN_SOURCE_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }
}