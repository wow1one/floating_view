package com.tokarev.scrollingviews

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.tokarev.scrollingviews.floatingview.FloatingView


class MainActivity : AppCompatActivity() {

    private lateinit var scrollingView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scrollingView = findViewById(R.id.scrolling_view)

        val floatingContentView = View.inflate(this, R.layout.layout_floating_view, null)
        val scaleAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.resize_action)
        floatingContentView.findViewById<View>(R.id.animated_view).startAnimation(scaleAnimation)

        val floatingView = FloatingView
                .attachToActivity(this)
                .setFloatingContent(floatingContentView)
                .setSnapRules(
                        listOf(
                                FloatingView.SnapRule(FloatingView.SnapSide.Top, FloatingView.Position.Outside),
                                FloatingView.SnapRule(FloatingView.SnapSide.CenterHorizontal)
                        )
                )
                .snapToView(scrollingView)

        floatingView.setFloatingContentClickListener {
            floatingView.visibility = View.GONE
        }
    }
}
