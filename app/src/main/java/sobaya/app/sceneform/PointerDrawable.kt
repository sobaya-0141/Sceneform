package sobaya.app.sceneform

import android.graphics.*
import android.graphics.drawable.Drawable

class PointerDrawable : Drawable() {

    private val paint = Paint()
    var enabled = false

    override fun draw(canvas: Canvas) {

        val cx = canvas.width.toFloat()
        val cy = canvas.height.toFloat()

        if (enabled) {
            paint.color = Color.GREEN
            canvas.drawCircle(cx, cy, 10f, paint)
        } else {
            paint.color = Color.GRAY
            canvas.drawText("X", cx, cy, paint)
        }
    }

    override fun setAlpha(alpha: Int) {}

    override fun getOpacity() = PixelFormat.UNKNOWN

    override fun setColorFilter(colorFilter: ColorFilter?) {}
}