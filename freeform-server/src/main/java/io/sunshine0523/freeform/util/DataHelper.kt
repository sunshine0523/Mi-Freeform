package io.sunshine0523.freeform.util

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import java.io.File

object DataHelper {
    private const val TAG = "MiFreeform/DataHelper"
    private val dataDir = File("${Environment.getDataDirectory()}/system/mi_freeform")
    private val dataFile = File(dataDir, "settings.json")
    private val gson = Gson()
    private var settings: Settings? = null

    init {
        runCatching {
            dataDir.mkdir()
            dataFile.createNewFile()
        }.onFailure {
            Log.e(TAG, "$it")
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
}

interface DataChangeListener {
    fun onChanged()
}