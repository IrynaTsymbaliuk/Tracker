package com.tracker.core.common

val Any.TAG: String
    get() = javaClass.simpleName.let {
        if (it.length <= 23) it else it.substring(0, 23)
    }
