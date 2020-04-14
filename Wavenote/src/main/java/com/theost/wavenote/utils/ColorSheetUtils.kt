package com.theost.wavenote.utils

import androidx.annotation.ColorInt

object ColorSheetUtils {

    /**
     * Converts color int to hex string
     *
     * @param color: Color int to convert
     * @return Hex string in this format "#FFFFFF"
     */
    fun colorToHex(@ColorInt color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }
}
