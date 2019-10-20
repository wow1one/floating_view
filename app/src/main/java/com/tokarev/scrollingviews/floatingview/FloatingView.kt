package com.tokarev.scrollingviews.floatingview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
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

    private var isHidden: Boolean = false
    private var floatingContent: View? = null
    private var snapRules: List<SnapRule> = listOf(
            SnapRule(SnapSide.Bottom, Position.Outside),
            SnapRule(SnapSide.CenterHorizontal)
    )

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

    fun snapToView(
            snapView: View
    ): FloatingView {
        snapView.post {
            updateFloatingContentPosition(snapView)
        }

        snapView.viewTreeObserver.addOnScrollChangedListener {
            if (isHidden) return@addOnScrollChangedListener

            updateFloatingContentPosition(snapView)
        }

        return this
    }

    private fun updateFloatingContentPosition(
            snapView: View
    ) {
        snapView.getGlobalVisibleRect(snapViewRect)
        floatingContent?.getGlobalVisibleRect(floatingViewRect)
        (floatingContent?.parent as? ViewGroup)?.getGlobalVisibleRect(floatingViewParentRect)
        val topOffset = floatingViewParentRect.top

        floatingContent?.let {
            snapRules.forEach { snapRule ->
                val side = snapRule.side
                val position = snapRule.position

                val positionOffset = when {
                    position == Position.Outside && side == SnapSide.Left -> -it.width
                    position == Position.Outside && side == SnapSide.Top -> -it.height
                    position == Position.Inside && side == SnapSide.Right -> -it.width
                    position == Position.Inside && side == SnapSide.Bottom -> -it.height
                    side == SnapSide.CenterVertical -> -(it.height / 2)
                    side == SnapSide.CenterHorizontal -> -(it.width / 2)
                    else -> 0
                }

                when (side) {
                    SnapSide.Left -> it.x = snapViewRect.left.toFloat() + positionOffset
                    SnapSide.Right -> it.x = snapViewRect.right.toFloat() + positionOffset
                    SnapSide.Top -> it.y = snapViewRect.top.toFloat() - topOffset + positionOffset
                    SnapSide.Bottom -> it.y = snapViewRect.bottom.toFloat() - topOffset + positionOffset
                    SnapSide.CenterVertical -> it.y = snapViewRect.bottom.toFloat() - (snapViewRect.height() / 2) - topOffset + positionOffset
                    SnapSide.CenterHorizontal -> it.x = snapViewRect.right.toFloat() - (snapViewRect.width() / 2) + positionOffset
                }
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
    }

    enum class SnapSide {
        Left, Top, Right, Bottom, CenterHorizontal, CenterVertical
    }

    enum class Position {
        Outside, Inside
    }

    data class SnapRule(
            val side: SnapSide,
            val position: Position = Position.Outside
    )
}