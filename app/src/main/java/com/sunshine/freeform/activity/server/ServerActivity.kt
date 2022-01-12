package com.sunshine.freeform.activity.server

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.BuildConfig
import com.sunshine.freeform.R
import com.sunshine.freeform.base.BaseActivity
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.utils.ShellUtils
import com.sunshine.freeform.utils.TagUtils
import kotlinx.android.synthetic.main.activity_server.*
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui
import java.io.File
import java.io.FileOutputStream

class ServerActivity : BaseActivity() {

    private lateinit var viewModel: ServerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        setTitle(getString(R.string.server_label))

        viewModel = ViewModelProvider(this).get(ServerViewModel::class.java)

        releaseServerFile()

        button_start_server_by_root.setOnClickListener {
            startServer()
        }

        button_start_server_by_adb.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.apply {
                setTitle(getString(R.string.dialog_title))
                setMessage(getString(R.string.adb_start_message))
                setPositiveButton(getString(R.string.copy)) { _, _ ->
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val cd = ClipData.newPlainText(getString(R.string.start_server_command_title), getString(R.string.start_server_command))
                    cm.setPrimaryClip(cd)
                    Snackbar.make(root, getString(R.string.copy_done), Snackbar.LENGTH_SHORT).show()
                    setResult(TagUtils.SERVER_START)
                }
            }
            builder.create().show()
        }

        button_stop_server_by_root.setOnClickListener {
            //关闭服务进程
            val pid = ShellUtils.execCommand(
                "ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'",
                true
            ).successMsg
            ShellUtils.execRootCmdSilent("kill -9 $pid")
            Snackbar.make(root, getString(R.string.server_stop), Snackbar.LENGTH_SHORT).show()
            setResult(TagUtils.SERVER_STOP)
        }

        button_stop_server_by_adb.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.apply {
                setTitle(getString(R.string.dialog_title))
                setMessage(getString(R.string.adb_stop_message))
                setPositiveButton(getString(R.string.copy)) { _, _ ->
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val cd = ClipData.newPlainText(getString(R.string.stop_server_command_title), getString(R.string.stop_server_command))
                    cm.setPrimaryClip(cd)
                    Snackbar.make(root, getString(R.string.copy_done), Snackbar.LENGTH_SHORT).show()
                    setResult(TagUtils.SERVER_STOP)
                }
            }
            builder.create().show()
        }

        button_log.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        button_start_server_by_sui.setOnClickListener {
            //如果测试是成功的，那么就将按键设为不可点击
            if (PermissionUtils.checkPermission(0, this)) {
                button_start_server_by_sui.text = "Sui服务开启"
                button_start_server_by_sui.isEnabled = false
            }
        }
    }

    private fun releaseServerFile() {
        Thread(Runnable {
            val serverVersion = viewModel.getServerVersion()
            //如果存在，直接开启，不存在就复制过去，如果软件更新了，服务端可能也更新，所以要重新创建
            val longVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

            val targetFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString() + "/freeform-server.jar")

            //服务版本比实际版本低，就刷新
            if (serverVersion < longVersionCode || !targetFile.exists()) {
                viewModel.putServerVersion(longVersionCode)

                if (targetFile.exists()) targetFile.delete()
                targetFile.createNewFile()
                val fos = FileOutputStream(targetFile, false)
                val inputStream = assets.open("freeform-server.jar")
                var hasRead: Int
                while (inputStream.read().also { hasRead = it } != -1) {
                    fos.write(hasRead)
                }
                inputStream.close()
                fos.close()
            }
        }, "releaseServerFileThread").start()
    }

    //root启动服务
    private fun startServer() {
        val filePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString() + "/freeform-server.jar"
        val localFile = ShellUtils.execCommand("find $filePath", true)

        if (!viewModel.serviceIsClose()) {
            Snackbar.make(root, getString(R.string.server_running), Snackbar.LENGTH_SHORT).show()
        } else {
            if (localFile.result == 0) {
                val result = ShellUtils.execCommand("CLASSPATH=$filePath nohup app_process / com.sunshine.freeform.Server >/dev/null 2>&1 &", true).result
                if (result == 0) {
                    Snackbar.make(root, getString(R.string.start_success), Snackbar.LENGTH_SHORT).show()
                    setResult(TagUtils.SERVER_START)
                } else {
                    Snackbar.make(root, getString(R.string.start_fail), Snackbar.LENGTH_SHORT).show()
                }

            } else {
                Snackbar.make(root, getString(R.string.get_file_fail), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (perm in permissions) {
            if (ShizukuProvider.PERMISSION == perm) {
                if (requestCode == 0 && grantResults[0] == RESULT_OK) {
                    Sui.init(BuildConfig.APPLICATION_ID)
                    setResult(TagUtils.SERVER_START_BY_SUI)
                }
            }
        }
    }
}