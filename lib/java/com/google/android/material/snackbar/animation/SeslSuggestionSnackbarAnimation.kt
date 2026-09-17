package com.google.android.material.snackbar.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.R
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.math.MathUtils
import com.google.android.material.snackbar.SnackbarContentLayout
import kotlin.math.min

//sesl9
object SeslSuggestionSnackbarAnimation {

	private fun adjustContentDrawableBounds(content: SnackbarContentLayout, width: Int, height: Int) {
		if (content.isWindowBlurApplied || !content.isBlurApplied) {
			content.background?.let { bg ->
				bg.setBounds(0, 0, width, height)
				bg.invalidateSelf()
			}
		}
	}

	private fun resetPivots(content: SnackbarContentLayout) {
		listOfNotNull(content.messageView, content.actionView).forEach { tv ->
			tv.pivotX = tv.width / 2.0f
			tv.pivotY = tv.height / 2.0f
		}
	}

	private fun setContentDrawableAlpha(content: SnackbarContentLayout, alpha: Int) {
		if (content.isWindowBlurApplied) {
			content.background?.let { bg ->
				val mutated = bg.mutate()
				mutated.alpha = alpha
				mutated.invalidateSelf()
			}
		}
	}

	@JvmStatic
	fun startInAnimation(view: View, transitionHeight: Int, onShown: Runnable) {
		val snackbarContentLayout =
			view.findViewById<SnackbarContentLayout>(R.id.snackbar_content_layout) ?: return
		val textView = snackbarContentLayout.findViewById<TextView>(R.id.snackbar_text)
		val actionBtn = snackbarContentLayout.findViewById<Button>(R.id.snackbar_action)
		val context = snackbarContentLayout.context

		(snackbarContentLayout.parent as? ViewGroup)?.let { parent ->
			parent.clipChildren = false
			parent.clipToPadding = false
		}
		snackbarContentLayout.clipChildren = false
		snackbarContentLayout.clipToPadding = false

		view.alpha = 0.0f
		snackbarContentLayout.apply {
			alpha = 0.0f; scaleX = 1.0f; scaleY = 1.0f
		}

		setContentDrawableAlpha(snackbarContentLayout, 0)
		updateBackground(snackbarContentLayout, 0, 0, 0, 0)

		textView?.apply {
			alpha = 0.0f; scaleX = 1.0f; scaleY = 1.0f; isVisible = true
		}
		actionBtn?.apply {
			alpha = 0.0f; scaleX = 1.0f; scaleY = 1.0f
			isVisible = getText().length > 0//custom
		}

		snackbarContentLayout.post {
			startInAnimationInternal(
				snackbarContentLayout,
				onShown,
				context,
				view,
				transitionHeight,
				textView,
				actionBtn
			)
		}
	}

	private fun startInAnimationInternal(
		layout: SnackbarContentLayout,
		onShown: Runnable,
		context: Context,
		view: View,
		transitionHeight: Int,
		textView: TextView,
		actionBtn: Button?
	) {
		val measuredWidth = layout.measuredWidth
		val measuredHeight = layout.measuredHeight
		if (measuredWidth == 0 || measuredHeight == 0) {
			onShown.run()
			return
		}

		val maxRadius = context.resources.getDimension(R.dimen.sesl_design_snackbar_suggest_background_radius)
		val hasAction = actionBtn?.getText()?.isNotEmpty() == true

		updateContentBackground(layout, 100, 100, maxRadius)

		layout.setAlpha(1.0f)
		view.setAlpha(1.0f)
		view.translationY = view.height.toFloat()

		val translationAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y)
		translationAnim.cancel()
		translationAnim.setSpring(SpringForce().setStiffness(350.0f).setDampingRatio(1.0f))
		translationAnim.animateToFinalPosition(0.0f - transitionHeight)
		translationAnim.setStartVelocity(0.1f)
		translationAnim.start()

		Handler(Looper.getMainLooper()).postDelayed(
			object : Runnable {
				override fun run() {
					textView.setAlpha(0.0f)
					actionBtn?.setAlpha(0.0f)

					val interpolator = android.view.animation.AnimationUtils.loadInterpolator(
						context,
						R.interpolator.sesl_snackbar_suggestion_interpolator
					)

					textView.animate()
						.alpha(1.0f)
						.setDuration(150L)
						.setInterpolator(interpolator)
						.setStartDelay(150L)
						.start()

					if (hasAction /*custom*/) {
						actionBtn.animate()
							.alpha(1.0f)
							.setDuration(150L)
							.setInterpolator(interpolator)
							.setStartDelay(150L)
							.start()
					}

					val sizeAnim =
						SpringAnimation(layout, object : FloatPropertyCompat<SnackbarContentLayout>("size") {
							private var _value = 0.0f

							override fun getValue(layout: SnackbarContentLayout?): Float {
								return this._value
							}

							override fun setValue(layout: SnackbarContentLayout, value: Float) {
								val amount = min(4.0f * value, 1.0f)
								val interpolatedWidth =
									MathUtils.lerp(100f, measuredWidth.toFloat(), amount).toInt()
								val interpolatedHeight =
									MathUtils.lerp(100f, measuredHeight.toFloat(), amount).toInt()
								updateContentBackground(layout, interpolatedWidth, interpolatedHeight, maxRadius)
								this._value = value
							}
						})

					sizeAnim.setStartValue(0f)
					sizeAnim.setSpring(SpringForce().setStiffness(50f).setDampingRatio(0.72f))
					sizeAnim.animateToFinalPosition(1f)
					sizeAnim.addEndListener { _, _, _, _ -> onShown.run() }
					sizeAnim.start()

					//custom - ensure translationAnim wouldn't overlap/conflict
					//with the succeeding finalTranslationAnim
					translationAnim.cancel()

					val finalTranslationAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y)
					finalTranslationAnim.cancel()
					finalTranslationAnim.setSpring(SpringForce().setStiffness(300f).setDampingRatio(0.72f))
					finalTranslationAnim.animateToFinalPosition(0f)
					finalTranslationAnim.setStartVelocity(0.1f)
					finalTranslationAnim.start()
				}
			}, 200L
		)
	}

	private fun updateContentBackground(view: View, width: Int, height: Int, maxRadius: Float) {
		val background = view.background as GradientDrawable

		var radius = min(width, height) / 2.0f
		if (radius > maxRadius) {
			radius = maxRadius
		}
		background.setCornerRadius(radius)

		//Update bounds
		val bounds = background.getBounds()
		val centerX = bounds.centerX()
		val centerY = bounds.centerY()
		val widthRadius = width / 2
		val heightRadius = height / 2
		background.setBounds(
			centerX - widthRadius,
			centerY - heightRadius,
			centerX + widthRadius,
			centerY + heightRadius
		)

		background.invalidateSelf()
	} //sesl7

	@JvmStatic
	fun startOutAnimation(view: View, onHidden: Runnable) {
		val snackbarContentLayout =
			view.findViewById<SnackbarContentLayout>(R.id.snackbar_content_layout) ?: run {
				onHidden.run()
				return
			}
		val context = snackbarContentLayout.context

		(view.getTag(R.id.tag_y_suggest_anim) as? SpringAnimation)?.cancel()
		view.animate().cancel()

		val transitionY = ObjectAnimator.ofFloat(
			view,
			View.TRANSLATION_Y,
			view.translationY + context.resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_suggest_out_transition_height)
		).apply {
			duration = 350L
			interpolator = android.view.animation.PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f)
		}

		val alphaAnim = ValueAnimator.ofFloat(view.alpha, 0.0f).apply {
			duration = 150L
			interpolator = AnimationUtils.LINEAR_INTERPOLATOR
			addUpdateListener { anim ->
				val alpha = anim.animatedValue as Float
				view.alpha = alpha
				snackbarContentLayout.alpha = alpha
				setContentDrawableAlpha(snackbarContentLayout, (alpha * 255.0f).toInt())
			}
		}

		AnimatorSet().apply {
			playTogether(transitionY, alphaAnim)
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					view.setTag(R.id.tag_y_suggest_anim, null)
					onHidden.run()
				}
			})
			start()
		}
	}

	private fun updateBackground(content: View, width: Int, height: Int, targetW: Int, targetH: Int) {
		val radius =
			content.context.resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_suggest_background_radius)
				.toFloat()
		val snackbarContentLayout = content as? SnackbarContentLayout ?: return
		if (targetW == 0 || targetH == 0) return

		val deltaX = (targetW - width) / 2.0f
		val deltaY = (targetH - height) / 2.0f
		snackbarContentLayout.translationX = deltaX
		snackbarContentLayout.translationY = deltaY

		val targetWidthFloat = targetW.toFloat()
		val targetHeightFloat = targetH.toFloat()
		val scaleX = width / targetWidthFloat
		val scaleY = height / targetHeightFloat
		val centerX = targetWidthFloat / 2.0f
		val centerY = targetHeightFloat / 2.0f

		listOfNotNull(
			snackbarContentLayout.messageView,
			snackbarContentLayout.actionView
		).forEach { tv ->
			tv.translationX = -deltaX
			tv.translationY = -deltaY
			tv.pivotX = centerX - tv.left
			tv.pivotY = centerY - tv.top
			tv.scaleX = scaleX
			tv.scaleY = scaleY
		}

		adjustContentDrawableBounds(snackbarContentLayout, width, height)
		snackbarContentLayout.outlineProvider = object : ViewOutlineProvider() {
			override fun getOutline(v: View, outline: Outline) {
				outline.setRoundRect(0, 0, width, height, radius)
			}
		}
		snackbarContentLayout.clipToOutline = true
		content.invalidate()
	}
}
