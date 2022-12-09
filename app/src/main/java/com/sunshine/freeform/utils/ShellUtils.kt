package com.sunshine.freeform.utils

import android.util.Log
import com.sunshine.freeform.utils.ShellUtils
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.AssertionError
import java.lang.Exception
import java.lang.StringBuilder

/**
 * @author Trinea, sunshine0523
 * @date 2013-5-16
 * http://www.trinea.cn
 * 米窗声明：该类部分作者并非米窗，作者信息和源代码地址见上
 */
class ShellUtils private constructor() {

    class CommandResult(
        /** 运行结果  */
        var result: Int,
        /** 运行成功结果  */
        var successMsg: String?,
        /** 运行失败结果  */
        var errorMsg: String?
    )

    companion object {
        private const val COMMAND_SU = "su"
        private const val COMMAND_SH = "sh"
        private const val COMMAND_EXIT = "exit\n"
        private const val COMMAND_LINE_END = "\n"
        fun execCommand(command: String, isRoot: Boolean): CommandResult {
            return execCommand(arrayOf(command), isRoot, false)
        }

        fun execCommandWithShizuku(command: String, isRoot: Boolean): CommandResult {
            return execCommand(arrayOf(command), isRoot, true)
        }

        private fun execCommand(
            commands: Array<String>?,
            isRoot: Boolean,
            useShizuku: Boolean
        ): CommandResult {
            var result = -1
            if (commands == null || commands.isEmpty()) {
                return CommandResult(result, null, null)
            }
            var process: Process? = null
            var successResult: BufferedReader? = null
            var errorResult: BufferedReader? = null
            var successMsg: StringBuilder? = null
            var errorMsg: StringBuilder? = null
            var os: DataOutputStream? = null
            try {
                process = Runtime.getRuntime().exec(
                    if (isRoot) COMMAND_SU else COMMAND_SH
                )
                os = DataOutputStream(process.outputStream)
                for (command in commands) {
                    os.write(command.toByteArray())
                    os.writeBytes(COMMAND_LINE_END)
                    os.flush()
                }
                os.writeBytes(COMMAND_EXIT)
                os.flush()
                result = process.waitFor()
                // get command result
                successMsg = StringBuilder()
                errorMsg = StringBuilder()
                successResult = BufferedReader(
                    InputStreamReader(
                        process.inputStream
                    )
                )
                errorResult = BufferedReader(
                    InputStreamReader(
                        process.errorStream
                    )
                )
                var s: String?
                while (successResult.readLine().also { s = it } != null) {
                    successMsg.append(s)
                }
                while (errorResult.readLine().also { s = it } != null) {
                    errorMsg.append(s)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    os?.close()
                    successResult?.close()
                    errorResult?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                process?.destroy()
            }
            return CommandResult(result, successMsg?.toString(), errorMsg?.toString())
        }
    }
}