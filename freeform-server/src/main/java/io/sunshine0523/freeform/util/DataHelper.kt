package io.sunshine0523.freeform.util

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.lang.StringBuilder

object DataHelper {
    private const val TAG = "Mi-Freeform/DataHelper"
    private val dataDir = File("${Environment.getDataDirectory()}/system/mi_freeform")
    private val dataFile = File(dataDir, "settings.json")
    private val logFile = File(dataDir, "log.log")
    private val gson = Gson()
    private var settings: Settings? = null
    private val logs = StringBuilder()

    init {
        runCatching {
            dataDir.mkdir()
            dataFile.createNewFile()
            logFile.createNewFile()

            Thread {
                runCatching {
                    logFile.writeText("")
                }
            }.start()
        }.onFailure {
            MLog.e(TAG, "$it $dataDir")
        }
    }

    fun getSettings(): Settings {
        if (null == settings) {
            settings = runCatching {
                gson.fromJson(dataFile.readText(), Settings::class.java) ?: Settings()
            }.getOrElse {
                Settings()
            }
        }
        return settings!!
    }

    fun getSettingsString(): String {
        return gson.toJson(getSettings())
    }

    fun saveSettings(settings: Settings) {
        this.settings = settings
        runCatching {
            dataFile.writeText(gson.toJson(settings))
        }
    }

    fun saveSettings(settings: String, listener: DataChangeListener) {
        runCatching {
            this.settings = gson.fromJson(settings, Settings::class.java) ?: this.settings
            dataFile.writeText(settings)
            listener.onChanged()
        }
    }

    fun getLog(): String {
        return logs.toString()
    }

    fun appendLog(log: String) {
        Thread {
            runCatching {
                logs.append(log).append("\n")
                logFile.appendText("$log\n")
            }
        }.start()
    }

    fun clearLog() {
        Thread {
            runCatching {
                logs.clear()
                logFile.writeText("")
            }
        }.start()
    }
}

interface DataChangeListener {
    fun onChanged()
}