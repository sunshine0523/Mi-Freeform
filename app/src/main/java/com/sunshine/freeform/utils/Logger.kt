/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */
package com.sunshine.freeform.utils

import android.util.Log
import java.io.IOException
import java.util.Locale
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class Logger {
    private val TAG: String
    private val LOGGER: Logger?

    constructor(TAG: String) {
        this.TAG = TAG
        LOGGER = null
    }

    constructor(TAG: String, file: String?) {
        this.TAG = TAG
        LOGGER = Logger.getLogger(TAG)
        try {
            val fh = FileHandler(file)
            fh.formatter = SimpleFormatter()
            LOGGER.addHandler(fh)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun isLoggable(tag: String?, level: Int): Boolean {
        return true
    }

    fun v(msg: String?) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(Log.VERBOSE, msg)
        }
    }

    fun v(fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(Log.VERBOSE, String.format(Locale.ENGLISH, fmt!!, *args))
        }
    }

    fun v(msg: String, tr: Throwable?) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(
                Log.VERBOSE, """
     $msg
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun d(msg: String?) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(Log.DEBUG, msg)
        }
    }

    fun d(fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(Log.DEBUG, String.format(Locale.ENGLISH, fmt!!, *args))
        }
    }

    fun d(msg: String, tr: Throwable?) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(
                Log.DEBUG, """
     $msg
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun i(msg: String?) {
        if (isLoggable(TAG, Log.INFO)) {
            println(Log.INFO, msg)
        }
    }

    fun i(fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.INFO)) {
            println(Log.INFO, String.format(Locale.ENGLISH, fmt!!, *args))
        }
    }

    fun i(msg: String, tr: Throwable?) {
        if (isLoggable(TAG, Log.INFO)) {
            println(
                Log.INFO, """
     $msg
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun w(msg: String?) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, msg)
        }
    }

    fun w(fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, String.format(Locale.ENGLISH, fmt!!, *args))
        }
    }

    fun w(tr: Throwable?, fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.WARN)) {
            println(
                Log.WARN, """
     ${String.format(Locale.ENGLISH, fmt!!, *args)}
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun w(msg: String, tr: Throwable?) {
        if (isLoggable(TAG, Log.WARN)) {
            println(
                Log.WARN, """
     $msg
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun e(msg: String?) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, msg)
        }
    }

    fun e(fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, String.format(Locale.ENGLISH, fmt!!, *args))
        }
    }

    fun e(msg: String, tr: Throwable?) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(
                Log.ERROR, """
     $msg
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun e(tr: Throwable?, fmt: String?, vararg args: Any?) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(
                Log.ERROR, """
     ${String.format(Locale.ENGLISH, fmt!!, *args)}
     ${Log.getStackTraceString(tr)}
     """.trimIndent()
            )
        }
    }

    fun println(priority: Int, msg: String?): Int {
        LOGGER?.info(msg)
        return Log.println(priority, TAG, msg!!)
    }
}