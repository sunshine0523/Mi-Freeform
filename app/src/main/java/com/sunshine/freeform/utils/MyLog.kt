package com.sunshine.freeform.utils

import java.lang.StringBuilder

/**
 * @author sunshine
 * @date 2021/3/17
 */
object MyLog {

    private val log = StringBuilder()

    fun d(tag: String, whatFun: String, log: String) {
        this.log.append("$tag\$$whatFun:$log\n")
    }

    fun e(tag: String, whatFun: String, log: String) {
        this.log.append("$tag\$$whatFun:$log\n")
    }
}