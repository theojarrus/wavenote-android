package com.theost.wavenote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theost.wavenote.models.Note
import com.theost.wavenote.adapters.ColorAdapter
import com.theost.wavenote.utils.ColorThemeUtils
import kotlinx.android.synthetic.main.bottom_sheet_color.*
import com.google.android.material.R as materialR

/**
 * Listener for color picker
 *
 * returns color selected from the sheet. If noColorOption is enabled and user selects the option,
 * it will return [ColorSheet.NO_COLOR]
 */
typealias ColorPickerListener = ((color: Int) -> Unit)?

@Suppress("unused")
class ColorSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ColorSheet"
        const val NO_COLOR = -1
    }

    private var colorAdapter: ColorAdapter? = null

    private var chk_text: CheckBox? = null

    override fun getTheme(): Int {
        return ColorThemeUtils.inferTheme(requireContext()).styleRes
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) dismiss()
        return inflater.inflate(R.layout.bottom_sheet_color, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialog = dialog as BottomSheetDialog?
                val bottomSheet = dialog?.findViewById<FrameLayout>(materialR.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            dismiss()
                        }
                    }
                })
            }
        })

        text_bold.isChecked = Note.isIsTextStyleBold()
        text_italic.isChecked =  Note.isIsTextStyleItalic()
        text_mono.isChecked =  Note.isIsTextStyleMono()
        text_stroke.isChecked = Note.isIsTextStyleStroke()
        text_underline.isChecked =  Note.isIsTextStyleUnderline()
        text_strikethrough.isChecked =  Note.isIsTextStyleStrikethrough()

        text_bold.setOnClickListener { Note.setIsTextStyleBold(text_bold.isChecked) }
        text_italic.setOnClickListener { Note.setIsTextStyleItalic(text_italic.isChecked) }
        text_mono.setOnClickListener { Note.setIsTextStyleMono(text_mono.isChecked) }
        text_stroke.setOnClickListener { Note.setIsTextStyleStroke(text_stroke.isChecked) }
        text_underline.setOnClickListener { Note.setIsTextStyleUnderline(text_underline.isChecked) }
        text_strikethrough.setOnClickListener { Note.setIsTextStyleStrikethrough(text_strikethrough.isChecked) }

        if (colorAdapter != null) {
            colorSheetList.adapter = colorAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        colorAdapter = null
    }

    /**
     * Config color picker
     *
     * @param colors: Array of dialogColors to show in color picker
     * @param selectedColor: Pass in the selected color from dialogColors list, default value is null. You can pass [ColorSheet.NO_COLOR]
     * to select noColorOption in the sheet.
     * @param noColorOption: Gives a option to set the [selectedColor] to [NO_COLOR]
     * @param listener: [ColorPickerListener]
     */
    fun colorPicker(
        colors: IntArray,
        @ColorInt selectedColor: Int? = null,
        noColorOption: Boolean = false,
        listener: ColorPickerListener
    ): ColorSheet {
        colorAdapter = ColorAdapter(this, colors, selectedColor, noColorOption, listener)
        return this
    }

    /**
     * Shows color sheet
     */
    fun show(fragmentManager: FragmentManager) {
        this.show(fragmentManager, TAG)
    }
}
