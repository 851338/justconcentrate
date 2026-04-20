package com.mobichill.justconcentration.view.language

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.utils.Utils

class LineDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.grey_line_color)
        strokeWidth = Utils.dpToPx(2, context).toFloat()
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var parentAnchorY: Float = 0f
    private var subOptionRects: List<RectData> = emptyList()

    // --- UPDATED VALUES ---
    // X-coordinate for the main vertical line, relative to LineDrawingView's left edge (32dp from phone edge - 10dp root margin)
    private val lineStartOffset = Utils.dpToPx(22, context).toFloat()
    private val curveRadius = Utils.dpToPx(8, context).toFloat() // Keep curve radius consistent
    // Length of the horizontal segment after the curve (calculated as 8dp)
    private val horizontalLineExtension = Utils.dpToPx(8, context).toFloat()
    // --- END UPDATED VALUES ---

    data class RectData(val top: Float, val bottom: Float, val centerY: Float)

    fun updateDrawingParameters(parentView: View, vararg subOptionViews: View) {
        // This calculation should determine the visual Y start of the line from the parent item.
        // If the line should visually start from the center of the `img_language` in the parent `container`:
        val parentImage = parentView.findViewById<View>(R.id.img_language)
        if (parentImage != null) {
            val parentImageCenterY = parentView.y + parentImage.y + parentImage.height / 2f
            parentAnchorY = parentImageCenterY - this.y
        } else {
            // Fallback if img_language is not found, or adjust based on specific design
            parentAnchorY = (parentView.y + Utils.dpToPx(24, context)) - this.y
        }


        val currentSubOptionRects = mutableListOf<RectData>()
        subOptionViews.forEach { subView ->
            if (subView.isVisible) {
                val subViewCenterY = subView.y + subView.height / 2f - this.y
                currentSubOptionRects.add(RectData(subViewCenterY - subView.height/2f, subViewCenterY + subView.height/2f, subViewCenterY))
            }
        }
        subOptionRects = currentSubOptionRects
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (subOptionRects.isEmpty()) return

        val lastChildCenterY = subOptionRects.last().centerY
        val verticalLineActualEndY = lastChildCenterY - curveRadius // Ensure clean termination

        // 1. Draw the main vertical stem
        canvas.drawLine(lineStartOffset, parentAnchorY, lineStartOffset, verticalLineActualEndY, linePaint)

        // 2. Draw horizontal branch lines for each child item
        subOptionRects.forEach { rectData ->
            val childCenterY = rectData.centerY

            val path = Path().apply {
                moveTo(lineStartOffset, childCenterY - curveRadius) // Start of the curve on the vertical line
                quadTo(
                    lineStartOffset, childCenterY, // Control point: inner corner
                    lineStartOffset + curveRadius, childCenterY // End point of curve, start of straight horizontal
                )
                lineTo(lineStartOffset + curveRadius + horizontalLineExtension, childCenterY) // Continue straight
            }
            canvas.drawPath(path, linePaint)
        }
    }
}