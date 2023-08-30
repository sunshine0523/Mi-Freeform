package com.sunshine.freeform.utils

/**
 * @author KindBrave
 * @since 2023/8/29
 */
fun <T> List<T>.contains(element: T, predicate: (T, T) -> Boolean): Boolean {
    for (item in this) {
        if (predicate(item, element)) {
            return true
        }
    }
    return false
}