package com.theost.wavenote.utils

import android.content.res.Resources

internal val Float.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)
