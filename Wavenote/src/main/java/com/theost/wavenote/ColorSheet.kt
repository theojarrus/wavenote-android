package com.theost.wavenote

import android.graphics.drawable.GradientDrawable
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
import com.theost.wavenote.utils.ColorSheetTheme
import com.theost.wavenote.utils.resolveColor
import kotlinx.android.synthetic.main.color_sheet.*
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

    private var sheetCorners: Float = 0f
    private var colorAdapter: ColorAdapter? = null

    private var chk_text: CheckBox? = null

    override fun getTheme(): Int {
        return ColorSheetTheme.inferTheme(requireContext()).styleRes
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) dismiss()
        return inflater.inflate(R.layout.color_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialog = dialog as BottomSheetDialog?
                val bottomSheet =
                    dialog?.findViewById<FrameLayout>(materialR.id.design_bottom_sheet)
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

        if (sheetCorners == 0f) {
            sheetCorners = resources.getDimension(R.dimen.default_dialog_radius)
        }

        text_bold.isChecked = NoteEditorFragment.isCheckboxActive(text_bold)
        text_italic.isChecked = NoteEditorFragment.isCheckboxActive(text_italic)
        text_code.isChecked = NoteEditorFragment.isCheckboxActive(text_code)
        text_stroke.isChecked = NoteEditorFragment.isCheckboxActive(text_stroke)
        text_underline.isChecked = NoteEditorFragment.isCheckboxActive(text_underline)
        text_strikethrough.isChecked = NoteEditorFragment.isCheckboxActive(text_strikethrough)

        text_bold.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_bold) }
        text_italic.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_italic) }
        text_code.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_code) }
        text_stroke.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_stroke) }
        text_underline.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_underline) }
        text_strikethrough.setOnClickListener { NoteEditorFragment.onCheckboxClicked(text_strikethrough) }

        val gradientDrawable = GradientDrawable().apply {
            if (ColorSheetTheme.inferTheme(requireContext()) == ColorSheetTheme.LIGHT) {
                setColor(resolveColor(requireContext(), colorRes = R.color.dialogPrimary))
            } else {
                setColor(resolveColor(requireContext(), colorRes = R.color.dialogDarkPrimary))
            }

            cornerRadii =
                floatArrayOf(sheetCorners, sheetCorners, sheetCorners, sheetCorners, 0f, 0f, 0f, 0f)
        }
        view.background = gradientDrawable

        if (colorAdapter != null) {
            colorSheetList.adapter = colorAdapter
        }

        colorSheetClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        colorAdapter = null
    }

    /**
     * Set corner radius of sheet top left and right corners.
     *
     * @param radius: Takes a float value
     */
    fun cornerRadius(radius: Float): ColorSheet {
        this.sheetCorners = radius
        return this
    }

    /**
     * Set corner radius of sheet top left and right corners.
     *
     * @param radius: Takes a float value
     */
    fun cornerRadius(radius: Int): ColorSheet {
        return cornerRadius(radius.toFloat())
    }

    /**
     * Config color picker
     *
     * @param colors: Array of colors to show in color picker
     * @param selectedColor: Pass in the selected color from colors list, default value is null. You can pass [ColorSheet.NO_COLOR]
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
