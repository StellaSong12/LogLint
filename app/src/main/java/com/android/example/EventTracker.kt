package com.android.example

import android.util.Log

object EventTracker {

    fun test() {
        trackEvent("123123")
    }

    fun trackEvent(s: String) {
        Log.i("~~~", "trackEvent: $s")
    }
}
