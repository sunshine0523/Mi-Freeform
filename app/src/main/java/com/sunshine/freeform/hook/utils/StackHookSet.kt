package com.sunshine.freeform.hook.utils

/**
 * @author sunshine
 * @date 2021/3/18
 * 为了满足单一性栈，采用集合特性来实现栈
 */
class StackHookSet<T> {

    private val elementData = ArrayList<T>()

    /**
     * 将元素放到栈顶，但同时保证单一性
     * 时间复杂度O(n)
     */
    fun push(element: T) {
        elementData.remove(element)
        elementData.add(element)
    }

    fun pop(): T? {
        if (elementData.size <= 0) return null
        return elementData.removeAt(elementData.size - 1)
    }

    fun peek(): T {
        return elementData[elementData.size - 1]
    }

    fun size(): Int {
        return elementData.size
    }
}