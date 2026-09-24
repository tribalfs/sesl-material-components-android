package com.google.android.material.oneui.dividerbuttonlayout

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.annotation.RequiresApi
import androidx.appcompat.oneui.common.BlurSupportable
import androidx.appcompat.oneui.common.internal.policy.BlurInfoState
import androidx.appcompat.oneui.common.internal.util.MaxFontScaleRatio
import androidx.appcompat.oneui.common.internal.util.checkMaxFontScale
import androidx.appcompat.util.SeslMisc
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView
import androidx.core.oneui.common.internal.semblurinfo.SemBlurInfoState
import androidx.core.view.SemBlurCompat.BLUR_MODE_CANVAS
import androidx.core.view.SemBlurCompat.SeslBlurMode
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslTextViewReflector
import com.google.android.material.R
import com.google.android.material.internal.ThemeEnforcement
import com.google.android.material.oneui.common.internal.MaterialLogTag
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.common.internal.resource.OpenThemeResourceDrawableRes
import com.google.android.material.oneui.common.internal.resource.ThemeResourceDrawableRes

/**
 * A Samsung One UI horizontal button bar component with dividers between items that automatically adjusts button sizes,
 * text font scaling, hover popups, and blur effects according to One UI design specifications.
 */
class DividerButtonLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = if (SeslMisc.isLightTheme(context)) {
		R.style.Widget_Design_DividerButtonLayout_Light
	} else R.style.Widget_Design_DividerButtonLayout
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), MenuView, BlurSupportable,
	MaterialLogTag {

	interface OnMenuItemClickListener {
		fun onMenuItemClick(item: MenuItem): Boolean
	}

	private var applyBlur: Boolean = false
	private var backgroundDrawable: Drawable? = null
	private var blurInfo: SemBlurInfoState? = null
	@SeslBlurMode
	private var blurMode: Int = BLUR_MODE_CANVAS
	private var menuBuilder: MenuBuilder = MenuBuilder(context)
	private val menuInflater by lazy { SupportMenuInflater(context) }
	private var onMenuItemClickListener: OnMenuItemClickListener? = null
	private val presenter by lazy { DividerButtonPresenter() }
	private var prevAvailableWidth: Int = 0
	private val syncLargestItemWidthConcept: Boolean = false
	override val logTag: String = "DividerButtonLayout"

	init {
		presenter.menuView = this
		menuBuilder.addMenuPresenter(presenter)

		val a = ThemeEnforcement.obtainStyledAttributes(
			context,
			attrs,
			R.styleable.DividerButtonLayout,
			defStyleAttr,
			0
		)

		if (a.hasValue(R.styleable.DividerButtonLayout_menu)) {
			inflateMenu((a.getResourceId(R.styleable.DividerButtonLayout_menu, 0)))
		}

		if (a.hasValue(R.styleable.DividerButtonLayout_seslApplyBlur)) {
			applyBlur = a.getBoolean(R.styleable.DividerButtonLayout_seslApplyBlur, true)
		}

		if (applyBlur) {
			applyBlurInfo(context)
		}

		isClickable = true
		isFocusable = false
		a.recycle()
	}

	private fun createDivider(context: Context): Divider {
		val divider = Divider(context)
		divider.layoutParams = LinearLayout.LayoutParams(
			divider.resources.getDimensionPixelSize(R.dimen.sesl_divider_button_layout_divider_width),
			divider.resources.getDimensionPixelSize(R.dimen.sesl_divider_button_layout_divider_height)
		).apply {
			gravity = Gravity.CENTER
		}
		return divider
	}

	private fun createDividerButton(context: Context, menuItem: MenuItemImpl): DividerButton {
		val btn = DividerButton(context)
		btn.initialize(menuItem)
		btn.setOnClickListener {
			onMenuItemClickListener?.onMenuItemClick(menuItem)
		}
		return btn
	}

	@RequiresApi(35)
	private fun generateBlurInfo(context: Context): SemBlurInfoState {
		val dimension =
			context.resources.getDimension(R.dimen.sesl_divider_button_layout_background_radius)
		val builder = BlurInfoState.generateFloatingComponentBlurInfoStateBuilder(context, blurMode)
		backgroundDrawable?.let { builder.nonBlurBackground(it) }
		builder.cornerRadius(dimension)
		return builder.build()
	}

	private fun getDividers(): List<Divider> {
		val list = mutableListOf<Divider>()
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child is Divider) list.add(child)
		}
		return list
	}

	private fun getDividerButtons(): List<DividerButton> {
		val list = mutableListOf<DividerButton>()
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child is DividerButton) list.add(child)
		}
		return list
	}

	private fun updateButtonSize(availableWidth: Int): Boolean {
		val buttons = getDividerButtons()
		var maxW = 0
		for (btn in buttons) {
			if (btn.visibility == View.VISIBLE) {
				maxW = maxOf(maxW, btn.measuredWidth)
			}
		}
		if (maxW == 0) return false

		val dividers = getDividers()
		val dividerW = dividers.firstOrNull()?.measuredWidth ?: 0
		val totalWidth = buttons.size * maxW + dividers.size * dividerW

		var changed = false
		for (btn in buttons) {
			val lp = btn.layoutParams as? LinearLayout.LayoutParams ?: continue
			if (totalWidth <= availableWidth) {
				if (lp.width != maxW) {
					lp.width = maxW
					changed = true
				}
				lp.weight = 0.0f
			} else {
				lp.width = 0
				lp.weight = 1.0f
				changed = true
			}
		}
		return changed
	}

	private fun addButton(button: DividerButton) {
		if (childCount > 0) {
			addView(createDivider(context))
		}
		addView(button, LinearLayout.LayoutParams(-2, -1))
	}

	override fun applyBlurInfo(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < 35) return false
		clearBlurInfo(context)
		val state = generateBlurInfo(context)
		val applied = state.applyBlurInfo(this)
		this.blurInfo = if (applied) state else null
		return applied
	}

	internal fun buildMenuView() {
		removeAllViews()
		val visibleItems = menuBuilder.visibleItems
		for (item in visibleItems) {
			addButton(createDividerButton(context, item))
		}
	}

	override fun clearBlurInfo(context: Context) {
		blurInfo?.clearBlurInfo(this)
		blurInfo = null
	}

	fun getMenu(): Menu = menuBuilder

	override fun getWindowAnimations(): Int = 0

	private fun inflateMenu(@MenuRes resId: Int) {
		presenter.updateSuspended = true
		menuInflater.inflate(resId, menuBuilder)
		presenter.updateSuspended = false
		presenter.updateMenuView(true)
	}

	override fun initialize(menu: MenuBuilder) {}

	override fun isBlurApplied(): Boolean = blurInfo != null

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val h = resources.getDimensionPixelSize(R.dimen.sesl_divider_button_layout_button_height)
		val mode = MeasureSpec.getMode(heightMeasureSpec)
		var newHeightSpec = heightMeasureSpec
		if (mode == MeasureSpec.UNSPECIFIED) {
			newHeightSpec =
				MeasureSpec.makeMeasureSpec(paddingTop + paddingBottom + h, MeasureSpec.EXACTLY)
		} else if (mode == MeasureSpec.AT_MOST) {
			if (MeasureSpec.getSize(heightMeasureSpec) >= h) {
				minimumHeight = h
			}
		}
		super.onMeasure(widthMeasureSpec, newHeightSpec)

		if (syncLargestItemWidthConcept) {
			val buttons = getDividerButtons()
			val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
			var hasWeight = false
			for (btn in buttons) {
				val lp = btn.layoutParams as? LinearLayout.LayoutParams
				if (lp?.weight == 1.0f) {
					hasWeight = true
					break
				}
			}
			if (prevAvailableWidth < available && hasWeight) {
				for (btn in buttons) {
					val lp = btn.layoutParams as? LinearLayout.LayoutParams
					if (lp != null) {
						lp.weight = 0.0f
						lp.width = -2
					}
				}
				super.onMeasure(widthMeasureSpec, newHeightSpec)
				debug("Size has increased, but it is layout_weight. remeasure($prevAvailableWidth -> $available)")
			}
			if (updateButtonSize(available)) {
				super.onMeasure(widthMeasureSpec, newHeightSpec)
			}
			prevAvailableWidth = available
		}
	}

	fun removeButton(button: DividerButton) {
		val index = indexOfChild(button)
		if (index != -1) {
			removeViewAt(index)
			if (index > 0 && getChildAt(index - 1) is Divider) {
				removeViewAt(index - 1)
				return
			}
			if (index < childCount && getChildAt(index) is Divider) {
				removeViewAt(index)
			}
		}
	}

	override fun setBackground(background: Drawable?) {
		super.setBackground(background)
		this.backgroundDrawable = background
	}

	override fun setBlurMode(@SeslBlurMode semBlurInfoMode: Int) {
		this.blurMode = semBlurInfoMode
		applyBlurInfo(context)
	}

	fun setOnMenuItemClickListener(listener: OnMenuItemClickListener) {
		this.onMenuItemClickListener = listener
	}

	internal fun updateMenuView() {
		presenter.updateSuspended = true
		val buttons = getDividerButtons()
		val visibleItems = menuBuilder.visibleItems
		if (visibleItems.size != buttons.size) {
			debug("updateMenuView size changed(${visibleItems.size} -> ${buttons.size})")
			buildMenuView()
			return
		}
		visibleItems.forEachIndexed { i, item ->
			buttons[i].initialize(item)
		}
		presenter.updateSuspended = false
	}

	class Divider @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = 0
	) : View(context, attrs, defStyleAttr) {
		init {
			val bgRes = OpenThemeResourceDrawableRes(
				ThemeResourceDrawableRes(
					R.drawable.sesl_divider_button_layout_divider_background_light,
					R.drawable.sesl_divider_button_layout_divider_background_dark
				),
				ThemeResourceDrawableRes(
					R.drawable.sesl_divider_button_layout_divider_background_for_theme,
					0
				)
			).getResource(context)
			setBackgroundResource(bgRes)
		}
	}

	class DividerButton @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = 0,
		defStyleRes: Int = if (SeslMisc.isLightTheme(context))
			R.style.Widget_Design_DividerButtonLayout_DividerButton_Light
		else R.style.Widget_Design_DividerButtonLayout_DividerButton
	) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

		private var itemData: MenuItemImpl? = null
		private var maxWidth = context.resources.getDimensionPixelSize(R.dimen.sesl_divider_button_layout_button_max_width)
		private val textBaseSizeDimenRes = R.dimen.sesl_divider_button_layout_button_text_size
		var textView: TextView? = null

		init {
			LayoutInflater.from(context)
				.inflate(R.layout.sesl_divider_button_layout_divier_button, this, true)

			val a = ThemeEnforcement.obtainStyledAttributes(
				context,
				attrs,
				R.styleable.DividerButton,
				defStyleAttr,
				defStyleRes
			)
			val text = a.getText(R.styleable.DividerButton_android_text)?.toString()
			val appearance = a.getResourceId(R.styleable.DividerButton_android_textAppearance, -1)
			a.recycle()

			val tv = findViewById<TextView>(R.id.textview)
			if (tv != null) {
				tv.text = text
				if (appearance != -1) {
					TextViewCompat.setTextAppearance(tv, appearance)
				}
				SeslViewReflector.semSetHoverPopupType(tv, 2)
				textView = tv
			}
		}

		internal fun initialize(itemData: MenuItemImpl) {
			this.itemData = itemData
			id = itemData.itemId
			textView?.isVisible = itemData.actionView == null
			isEnabled = itemData.isEnabled
			contentDescription = itemData.contentDescription

			val actionView = itemData.actionView
			if (actionView == null) {
				textView?.let { tv ->
					tv.text = itemData.title?.toString()
					tv.checkMaxFontScale(textBaseSizeDimenRes, MaxFontScaleRatio.LARGE)
					SeslTextViewReflector.semSetButtonShapeEnabled(tv, true)
				}
				maxWidth =
					context.resources.getDimensionPixelSize(R.dimen.sesl_divider_button_layout_button_max_width)
				return
			}

			(actionView.parent as? ViewGroup)?.removeView(actionView)
			val lp = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
			lp.gravity = Gravity.CENTER
			addView(actionView, lp)
		}

		override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
			super.onInitializeAccessibilityNodeInfo(info)
			if (contentDescription != null) {
				info.className = "android.widget.Button"
			}
		}

		override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			val width = measuredWidth.coerceIn(suggestedMinimumWidth, maxWidth)
			setMeasuredDimension(View.resolveSizeAndState(width, widthMeasureSpec, 0), measuredHeight)
		}

		override fun setEnabled(enabled: Boolean) {
			super.setEnabled(enabled)
			textView?.isEnabled = enabled
		}
	}
}
