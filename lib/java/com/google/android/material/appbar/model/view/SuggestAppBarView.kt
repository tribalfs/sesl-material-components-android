package com.google.android.material.appbar.model.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.util.SeslMisc.isLightTheme
import androidx.appcompat.util.theme.SeslThemeResourceHelper
import androidx.appcompat.util.theme.resource.SeslThemeResourceColor.ThemeResourceColor
import androidx.appcompat.util.theme.resource.SeslThemeResourceDrawable.OpenThemeResourceDrawable
import androidx.appcompat.util.theme.resource.SeslThemeResourceDrawable.ThemeResourceDrawable
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector
import com.google.android.material.R
import com.google.android.material.appbar.model.AppBarModel
import com.google.android.material.appbar.model.ButtonListModel
import com.google.android.material.appbar.model.ButtonModel
import com.google.android.material.appbar.model.SuggestAppBarModel

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * Base view class for displaying a single suggestion or action page within an expanded AppBar.
 *
 * This class extends [AppBarView] and is responsible for inflating and managing its own layout,
 * including adapting its appearance (such as colors and drawables) based on the current theme (light or dark).
 *
 * The view is composed of the following configurable components:
 * - A title, set via [setTitle].
 * - A subtitle, set via [setSubTitle].
 * - A top image, set via [setImage]
 * - An optional close button in the top-right corner, with its click listener set by [setCloseClickListener].
 * - One or more action buttons at the bottom, configured using [setButtonModules].
 *
 * The data and behavior for this view are provided by an associated [SuggestAppBarModel] (or subclass),
 * which is set via [setModel].
 *
 * @param context The context in which the view is running, providing access to resources, themes, etc.
 * @param attrs The attribute set from XML used to inflate the view, or null if created programmatically.
 *
 * @see AppBarView
 * @see SuggestAppBarModel
 */
open class SuggestAppBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarView(context, attrs) {

	val buttons: MutableList<Button> = ArrayList()
    private lateinit var model: SuggestAppBarModel<out SuggestAppBarView>

	var bottomLayout: ViewGroup? = null
	var close: ImageButton? = null
	var titleView: TextView? = null
	//Sesl9
	var subTitleView: TextView? = null
	var topImageView: ImageView? = null
	//sesl9

    init {
        inflate()
    }

	/**
	 * Inflates the layout using the `sesl_app_bar_suggest.xml` resource and adds it as a child view.
	 *
	 * This method initializes the main components of the inflated layout:
	 * - Title view
	 * - Close button
	 * - Bottom layout (for action buttons)
	 *
	 * It also disables hover popups for the close button (if present) and updates resources based on the current theme.
	 *
	 * This method is called during this view's initialization.
	 */
    override fun inflate() {
		val context = context

		val viewGroup = LayoutInflater.from(context).inflate(
			R.layout.sesl_app_bar_suggest, this, false
		) as? ViewGroup ?: return

		viewGroup.apply {
			titleView = findViewById(R.id.suggest_app_bar_title)
			subTitleView = findViewById(R.id.suggest_app_bar_sub_title)//sesl9
			topImageView = findViewById(R.id.suggest_app_bar_top_image)//sesl9
			close = findViewById<ImageButton?>(R.id.suggest_app_bar_close)?.also {
				SeslViewReflector.semSetHoverPopupType(
					it,
					SeslHoverPopupWindowReflector.getField_TYPE_NONE()
				);
			}
			bottomLayout = findViewById(R.id.suggest_app_bar_bottom_layout)
		}

        updateResource(context)
        addView(viewGroup)
    }

	private fun addMargin() {
		bottomLayout?.addView(
			View(context).apply {
				layoutParams = ViewGroup.LayoutParams(
					resources.getDimensionPixelOffset(com.google.android.material.R.dimen.sesl_appbar_button_side_margin),
					MATCH_PARENT
				)
			})
	}

	private fun generateButton(buttonModel: ButtonModel, style: Int): Button {
		return Button(context, null, 0, style).apply {
			text = buttonModel.text
			buttonModel.contentDescription?.let {
				contentDescription = it
			}
			setOnClickListener { v ->
				buttonModel.clickListener?.onClick(v, model)
			}
		}
	}

	/**
	 * Configures and displays a list of buttons in the bottom layout of this view.
	 * This method clears any existing buttons, then generates and adds new buttons based on the
	 * provided [ButtonListModel]. The appearance of the buttons (style, max width) is
	 * dynamically adjusted based on the current theme (light/dark) and the number of buttons.
	 *
	 * @param buttonListModel The [ButtonListModel] containing the data for the buttons to be displayed,
	 *                        including their text, content descriptions, click listeners, and style.
	 */
	fun setButtonModules(buttonListModel: ButtonListModel) {
		bottomLayout?.removeAllViews()
		buttons.clear()

		val buttonModels = buttonListModel.buttonModels
		val buttonStyle = buttonListModel.buttonStyle.let {
			if (isLightTheme(context)) it.defStyleRes else it.defStyleResDark
		}

		for (i in buttonModels.indices) {
			val button = generateButton(buttonModels[i], buttonStyle).apply {
				maxWidth = resources.getDimensionPixelSize(
					if (buttonModels.size > 1) R.dimen.sesl_appbar_button_max_width
					else R.dimen.sesl_appbar_button_max_width_multi
				)
			}
			if (i != 0) addMargin()
			buttons.add(button)
			bottomLayout?.addView(button)
		}
	}

	/**
	 * Sets a click listener for the close button.
	 * If the listener is null, the close button will be hidden. Otherwise, it will be visible.
	 * When the close button is clicked, the provided listener's `onClick` method will be invoked.
	 *
	 * @param onClickListener The [AppBarModel.OnClickListener] to be invoked when the close button is clicked.
	 *                        Pass `null` to remove the listener and hide the button.
	 */
	fun setCloseClickListener(onClickListener: AppBarModel.OnClickListener?) {
		close?.apply {
			visibility = if (onClickListener != null) VISIBLE else GONE
			setOnClickListener { v -> onClickListener?.onClick(v, model) }
		}
	}

	/**
	 * Sets the data model for this view.
	 * The model contains the data and logic that this view will display and interact with.
	 *
	 * @param model The [SuggestAppBarModel] to be associated with this view.
	 */
	fun setModel(model: SuggestAppBarModel<out SuggestAppBarView>) {
		this.model = model
	}

	override fun updateResource(context: Context) {
		titleView?.setTextColor(getAppBarSuggestTitleColor(context))
		subTitleView?.setTextColor(getAppBarSuggestSubTitleColor(context))//sesl9
		close?.apply {
			resources.getString(androidx.appcompat.R.string.sesl_appbar_suggest_dismiss)
				.let { contentDescription = it; TooltipCompat.setTooltipText(this, it) }
			background = getCloseDrawable(context);
		}
	}

	private fun getCloseDrawable(context: Context): Drawable? {
		return SeslThemeResourceHelper.getDrawable(
			context,
			OpenThemeResourceDrawable(
				ThemeResourceDrawable(R.drawable.sesl_close_button_recoil_background, R.drawable.sesl_close_button_recoil_background_dark),
				ThemeResourceDrawable(R.drawable.sesl_close_button_recoil_background_for_theme, R.drawable.sesl_close_button_recoil_background_dark_for_theme)
			)
		)
	}

	private fun getAppBarSuggestTitleColor(context: Context): Int {
		return SeslThemeResourceHelper.getColorInt(
			context,
			ThemeResourceColor(R.color.sesl_appbar_suggest_title, R.color.sesl_appbar_suggest_title_dark)
		)
	}

	/**
	 * Sets the title text to be shown in the title area.
	 * If the title is null or empty, the title view will be hidden.
	 *
	 * @param title The title string to display.
	 * @param titleMaxLines The maximum number of lines the title text can span.
	 */
    fun setTitle(title: String?, titleMaxLines: Int) {
        titleView?.let {
            it.text = title
            it.isGone = TextUtils.isEmpty(title)
	        //sesl9
            if (titleMaxLines > 0) {
                it.maxLines = titleMaxLines
            }
        }
    }

	//Sesl9
	private fun getAppBarSuggestSubTitleColor(context: Context): Int {
		return SeslThemeResourceHelper.getColorInt(
			context,
			ThemeResourceColor(R.color.sesl_appbar_suggest_sub_title, R.color.sesl_appbar_suggest_sub_title_dark)
		)
	}

	/**
	 * Sets the subtitle text to be shown in the title area.
	 * If the subtitle is null or empty, the subtitle view will be hidden.
	 *
	 * @param subTitle The subtitle string to display.
	 * @param subTitleMaxLines The maximum number of lines the title text can span.
	 */
    fun setSubTitle(subTitle: String?, subTitleMaxLines: Int) {
        subTitleView?.let {
            it.text = subTitle
            it.isGone = TextUtils.isEmpty(subTitle)
            if (subTitleMaxLines > 0) {
                it.maxLines = subTitleMaxLines
            }
        }
    }

    fun setImage(drawable: Drawable?) {
        topImageView?.let {
            it.setImageDrawable(drawable)
            it.visibility = if (drawable != null) View.VISIBLE else View.GONE
        }
    }
	//sesl9
}
