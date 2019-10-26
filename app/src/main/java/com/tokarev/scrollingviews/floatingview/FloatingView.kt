package com.tokarev.scrollingviews.floatingview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
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
        visibility = View.VISIBLE
        isHidden = false
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

    fun setFloatingContentClickListener(
        clickListener: OnClickListener
    ) {
        floatingContent?.setOnClickListener(clickListener)
    }

    fun setFloatingContentClickListener(
        clickListener: (view: View) -> Unit
    ) {
        floatingContent?.apply {
            setOnClickListener { clickListener.invoke(this) }
        }
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
        snapView.post {
            updateFloatingContentPosition()
        }

        snapView.viewTreeObserver.addOnScrollChangedListener {
            if (!snapView.isShown) {
                visibility = View.GONE
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
    }

    private fun applySnapRules(floatingContentView: View) {
        val topOffset = floatingViewParentRect.top

        snapRules.forEach { snapRule ->
            val side = snapRule.side
            val position = snapRule.position
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
    }

    private fun applyOffsetRules(floatingContentView: View) {
        offsetRules.forEach { offsetRule ->
            when (offsetRule.side) {
                Side.Left -> floatingContentView.x -= offsetRule.offset
                Side.Right -> floatingContentView.x += offsetRule.offset
                Side.Bottom -> floatingContentView.y += offsetRule.offset
                Side.Top -> floatingContentView.y -= offsetRule.offset
                else -> Unit
            }
        }
    }

    companion object {

        fun attachToActivity(
            activity: Activity
        ): FloatingView {
            val floatingViewContainer = FloatingView(activity)
            val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
            rootLayout.addView(floatingViewContainer)

            return floatingViewContainer
        }

        fun attachToContainer(
            container: ViewGroup
        ): FloatingView {
            val floatingViewContainer = FloatingView(container.context)
            container.addView(floatingViewContainer)

            return floatingViewContainer
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