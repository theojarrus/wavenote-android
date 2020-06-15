package com.theost.wavenote.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat

/**
 * Get color
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ColorInt
internal fun resolveColor(context: Context, @ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(context, colorRes)
}

/**
 * Resolve a attribute value and return color
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ColorInt
internal fun resolveColorAttr(
    context: Context,
    @AttrRes attrRes: Int
): Int {
    val a = context.theme.obtainStyledAttributes(intArrayOf(attrRes))
    return a.getColor(0, 0)
}

/**
 * gotten from [https://github.com/afollestad/material-dialogs/blob/master/core/src/main/java/com/afollestad/materialdialogs/utils/MDUtil.kt]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Int.isColorDark(threshold: Double = 0.5): Boolean {
    if (this == Color.TRANSPARENT) {
        return false
    }
    val darkness =
        1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return darkness >= threshold
}
