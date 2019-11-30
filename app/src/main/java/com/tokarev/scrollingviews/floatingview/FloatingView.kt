package com.tokarev.scrollingviews.floatingview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes

@SuppressLint("ViewConstructor")
class FloatingView private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private lateinit var snapView: View
    private var isHidden: Boolean = false
    private var isSnapped: Boolean = false
    private var floatingContent: View? = null
    private var snapRules: List<SnapRule> = listOf(
        SnapRule(Side.Bottom, Position.Outside),
        SnapRule(Side.CenterHorizontal)
    )
    private var offsetRules: List<OffsetRule> = emptyList()
    private val snapViewRect = Rect()
    private val floatingViewParentRect = Rect()
    private val floatingViewRect = Rect()

    fun setFloatingContent(
        @LayoutRes floatingContentLayoutId: Int
    ): FloatingView = setFloatingContent(View.inflate(context, floatingContentLayoutId, null))

    fun setFloatingContent(
        floatingContentView: View
    ): FloatingView {
        floatingContent = floatingContentView
        floatingContent?.let {
            it.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        addView(floatingContent)

        return this
    }

    fun show() {
        isHidden = false
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
        isHidden = true
    }

    fun hideOnClick(): FloatingView {
        floatingContent?.setOnClickListener {
            visibility = View.GONE
            isHidden = true
        }

        return this
    }

    fun setOnFloatingContentClickListener(
        clickListener: OnClickListener
    ): FloatingView {
        floatingContent?.setOnClickListener(clickListener)

        return this
    }

    fun setOnFloatingContentClickListener(
        clickListener: (view: View) -> Unit
    ): FloatingView {
        floatingContent?.setOnClickListener { clickListener.invoke(this) }

        return this
    }

    fun setSnapRules(
        rules: List<SnapRule>
    ): FloatingView {
        snapRules = rules

        return this
    }

    fun setOffsetRulesInPx(
        rules: List<OffsetRule>
    ): FloatingView {
        offsetRules = rules

        return this
    }

    fun setOffsetRulesInDp(
        rules: List<OffsetRule>
    ): FloatingView {
        val mutableOffsetRules: MutableList<OffsetRule> = mutableListOf()
        rules.forEach { offsetRule ->
            val offsetInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                offsetRule.offset.toFloat(),
                resources.displayMetrics
            )
            mutableOffsetRules.add(OffsetRule(offsetRule.side, offsetInPx.toInt()))
        }
        offsetRules = mutableOffsetRules

        return this
    }

    fun snapToView(
        snapView: View
    ): FloatingView {
        this.snapView = snapView
        visibility = INVISIBLE
        snapView.post { updateFloatingContentPosition() }
        snapView.viewTreeObserver.addOnScrollChangedListener {
            if (!snapView.isShown) {
                visibility = GONE
                isHidden = true
            }
            if (isHidden) return@addOnScrollChangedListener

            updateFloatingContentPosition()
        }

        return this
    }

    fun snapViewScrolled() {
        updateFloatingContentPosition()
    }

    private fun updateFloatingContentPosition() {
        snapView.getGlobalVisibleRect(snapViewRect)
        floatingContent?.getGlobalVisibleRect(floatingViewRect)
        (floatingContent?.parent as? ViewGroup)?.getGlobalVisibleRect(floatingViewParentRect)

        floatingContent?.let {
            applySnapRules(it)
            applyOffsetRules(it)
        }

        changeVisibilityBySnapViewPosition()

        if (!isSnapped) {
            isSnapped = true
            show()
        }
    }

    private fun applySnapRules(
        floatingContentView: View
    ): Unit = snapRules.forEach { snapRule ->
        val side = snapRule.side
        val position = snapRule.position
        val topOffset = floatingViewParentRect.top
        val positionOffset = when {
            position == Position.Outside && side == Side.Left -> -floatingContentView.width
            position == Position.Outside && side == Side.Top -> -floatingContentView.height
            position == Position.Inside && side == Side.Right -> -floatingContentView.width
            position == Position.Inside && side == Side.Bottom -> -floatingContentView.height
            side == Side.CenterVertical -> -(floatingContentView.height / 2)
            side == Side.CenterHorizontal -> -(floatingContentView.width / 2)
            else -> 0
        }
        when (side) {
            Side.Left -> floatingContentView.x = snapViewRect.left.toFloat() + positionOffset
            Side.Right -> floatingContentView.x = snapViewRect.right.toFloat() + positionOffset
            Side.Top -> floatingContentView.y =
                snapViewRect.top.toFloat() - topOffset + positionOffset
            Side.Bottom -> floatingContentView.y =
                snapViewRect.bottom.toFloat() - topOffset + positionOffset
            Side.CenterVertical -> floatingContentView.y =
                snapViewRect.bottom.toFloat() - (snapViewRect.height() / 2) - topOffset + positionOffset
            Side.CenterHorizontal -> floatingContentView.x =
                snapViewRect.right.toFloat() - (snapViewRect.width() / 2) + positionOffset
        }
    }

    private fun changeVisibilityBySnapViewPosition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return

        if (snapView.top == snapViewRect.top) {
            visibility = GONE
        } else if (!isHidden) {
            visibility = VISIBLE
        }
    }

    private fun applyOffsetRules(
        floatingContentView: View
    ): Unit = offsetRules.forEach { offsetRule ->
        when (offsetRule.side) {
            Side.Left -> floatingContentView.x -= offsetRule.offset
            Side.Right -> floatingContentView.x += offsetRule.offset
            Side.Bottom -> floatingContentView.y += offsetRule.offset
            Side.Top -> floatingContentView.y -= offsetRule.offset
            else -> Unit
        }
    }

    companion object {

        fun attachToActivity(
            activity: Activity
        ): FloatingView {
            val floatingView = FloatingView(activity)
            val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
            rootLayout.addView(floatingView)

            return floatingView
        }

        fun attachToContainer(
            container: ViewGroup
        ): FloatingView {
            val floatingView = FloatingView(container.context)
            container.addView(floatingView)

            return floatingView
        }
    }

    enum class Side {
        Left, Top, Right, Bottom, CenterHorizontal, CenterVertical
    }

    enum class Position {
        Outside, Inside
    }

    data class SnapRule(
        val side: Side,
        val position: Position = Position.Outside
    )

    data class OffsetRule(
        val side: Side,
        val offset: Int = 0
    )
}