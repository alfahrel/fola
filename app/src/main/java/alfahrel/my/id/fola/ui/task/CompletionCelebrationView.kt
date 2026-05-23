package alfahrel.my.id.fola.ui.task

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CompletionCelebrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val color: Int,
        val radius: Float,
        var alpha: Float = 1f,
        val shape: Int
    )

    private val particles = mutableListOf<Particle>()
    private val paint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    fun burst(originX: Float, originY: Float, colors: IntArray) {
        animator?.cancel()
        particles.clear()

        val count = 22
        repeat(count) { i ->
            val angle  = (2 * Math.PI * i / count + Random.nextDouble(-0.3, 0.3)).toFloat()
            val speed  = Random.nextFloat() * 14f + 6f
            particles.add(
                Particle(
                    x      = originX,
                    y      = originY,
                    vx     = cos(angle) * speed,
                    vy     = sin(angle) * speed,
                    color  = colors[i % colors.size],
                    radius = Random.nextFloat() * 5f + 3f,
                    shape  = i % 3
                )
            )
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration     = 650
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                for (p in particles) {
                    p.x    += p.vx
                    p.y    += p.vy + t * 1.5f
                    p.alpha = (1f - t * 1.2f).coerceAtLeast(0f)
                }
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    particles.clear()
                    invalidate()
                }
                override fun onAnimationStart(a: android.animation.Animator)  {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            when (p.shape) {
                0 -> canvas.drawCircle(p.x, p.y, p.radius, paint)
                1 -> canvas.drawRect(p.x - p.radius * 0.8f, p.y - p.radius * 0.8f, p.x + p.radius * 0.8f, p.y + p.radius * 0.8f, paint)
                else -> {
                    val path = android.graphics.Path().apply {
                        moveTo(p.x, p.y - p.radius)
                        lineTo(p.x + p.radius * 0.87f, p.y + p.radius * 0.5f)
                        lineTo(p.x - p.radius * 0.87f, p.y + p.radius * 0.5f)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
