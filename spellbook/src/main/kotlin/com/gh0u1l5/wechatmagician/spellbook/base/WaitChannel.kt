package com.gh0u1l5.wechatmagician.spellbook.base

class WaitChannel {
    @Volatile private var done = false
    private val channel = java.lang.Object()

    fun wait(millis: Long = 0L): Boolean {
        synchronized(channel) {
            if (!done) {
                channel.wait(millis)
                return true
            }
            return false
        }
    }

    fun done() {
        synchronized(channel) {
            done = true
            channel.notifyAll()
        }
    }

    fun isDone() = synchronized(channel) { done }
}