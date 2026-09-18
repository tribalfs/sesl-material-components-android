package com.google.android.material.appbar.model

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.google.android.material.R
import com.google.android.material.appbar.model.view.SuggestAppBarView
import kotlin.reflect.KClass

/*
 * Original code by Samsung, all rights reserved to the original author. Added in sesl7
 */
/**
 * A model class extending [AppBarModel] that manages the data and behavior of a [SuggestAppBarView]
 * or its subclasses, designed to display suggestion or action content.
 *
 * This class handles setting the main title, subtitle, image icon/drawable, action buttons, and
 * click listeners for both the close button and action buttons within a suggestion app bar card.
 *
 * Use the [Builder] class to construct instances of [SuggestAppBarModel].
 *
 * @param T The type of [SuggestAppBarView] associated with this model.
 * @param kclazz The Kotlin class ([KClass]) of the [SuggestAppBarView] implementation.
 * @param context The context used to access resources and layout inflaters.
 * @param title The primary title text to be displayed. Can be null.
 * @param titleMaxLine Maximum number of lines for the primary title. Defaults to 0.
 * @param subTitle The subtitle text to be displayed beneath the title. Can be null.
 * @param subTitleMaxLine Maximum number of lines for the subtitle. Defaults to 0.
 * @param imageDrawable Optional icon or image drawable displayed alongside the text content. Can be null.
 * @param closeClickListener The click listener invoked when the close/dismiss button is clicked. Can be null.
 * @param buttonListModel The [ButtonListModel] containing action button models and styling information.
 *
 * @see AppBarModel
 * @see SuggestAppBarView
 */
open class SuggestAppBarModel<T : SuggestAppBarView> @JvmOverloads constructor(
    kclazz: KClass<T>,
    context: Context,
    val title: String? = null,
    val titleMaxLine: Int = 0,
    val subTitle: String? = null,
    val subTitleMaxLine: Int = 0,
    val imageDrawable: Drawable? = null,
    val closeClickListener: OnClickListener? = null,
    val buttonListModel: ButtonListModel
) : AppBarModel<T>(kclazz, context) {

    override fun init(moduleView: T): T {
        return moduleView.apply {
            setModel(this@SuggestAppBarModel)
            setTitle(this@SuggestAppBarModel.title, this@SuggestAppBarModel.titleMaxLine)
            setSubTitle(this@SuggestAppBarModel.subTitle, this@SuggestAppBarModel.subTitleMaxLine)
            setImage(this@SuggestAppBarModel.imageDrawable)
            setCloseClickListener(this@SuggestAppBarModel.closeClickListener)
            setButtonModules(this@SuggestAppBarModel.buttonListModel)
            updateResource(context)
        }
    }

    /**
     * Builder class for constructing instances of [SuggestAppBarModel].
     *
     * @param context The context used to access resources.
     */
    class Builder(private val context: Context) {
        private var buttonStyle: ButtonStyle? = null
        private var buttons: List<ButtonModel> = emptyList()
        private var closeClickListener: OnClickListener? = null
        private var title: String? = null
        private var titleMaxLine: Int = 0
        private var subTitle: String? = null
        private var subTitleMaxLine: Int = 0
        private var imageDrawable: Drawable? = null

        private val default_total_max_line_limit_for_image_case = 3
        private val default_total_max_line_limit_for_no_image_case = 4
        private val default_title_max_line_without_sub_title = 3
        private val default_title_max_line_with_sub_title_for_image_case = 1
        private val default_title_max_line_with_sub_title_for_no_image_case = 1
        private val default_sub_title_max_line_for_no_image_case = 3
        private val default_sub_title_max_line_for_image_case = 2

        @JvmOverloads
        fun setTitle(title: String?, maxLine: Int = 0): Builder {
            this.title = title
            this.titleMaxLine = maxLine
            return this
        }

        @JvmOverloads
        fun setSubTitle(subTitle: String?, maxLine: Int = 0): Builder {
            this.subTitle = subTitle
            this.subTitleMaxLine = maxLine
            return this
        }

        fun setImage(drawable: Drawable?): Builder {
            this.imageDrawable = drawable
            return this
        }

        fun setCloseClickListener(onClickListener: OnClickListener?): Builder {
            this.closeClickListener = onClickListener
            return this
        }

        @JvmOverloads
        fun setButtons(buttons: List<ButtonModel>, buttonStyle: ButtonStyle? = null): Builder {
            this.buttons = buttons
            if (buttonStyle != null) {
                this.buttonStyle = buttonStyle
            }
            return this
        }

        private fun checkMaxLine() {
            if (TextUtils.isEmpty(subTitle)) {
                titleMaxLine = default_title_max_line_without_sub_title
                return
            }
            if (imageDrawable != null) {
                if (titleMaxLine == 0 || subTitleMaxLine == 0 || titleMaxLine + subTitleMaxLine > default_total_max_line_limit_for_image_case) {
                    titleMaxLine = default_title_max_line_with_sub_title_for_image_case
                    subTitleMaxLine = default_sub_title_max_line_for_image_case
                }
            } else {
                if (titleMaxLine == 0 || subTitleMaxLine == 0 || titleMaxLine + subTitleMaxLine > default_total_max_line_limit_for_no_image_case) {
                    titleMaxLine = default_title_max_line_with_sub_title_for_no_image_case
                    subTitleMaxLine = default_sub_title_max_line_for_no_image_case
                }
            }
        }

        fun build(): SuggestAppBarModel<SuggestAppBarView> {
            checkMaxLine()
            if (buttonStyle == null) {
                buttonStyle = ButtonStyle(
                    R.style.Basic_CollapsingToolbar_Button_Light,
                    R.style.Basic_CollapsingToolbar_Button
                )
            }
            return SuggestAppBarModel(
                SuggestAppBarView::class,
                context,
                title,
                titleMaxLine,
                subTitle,
                subTitleMaxLine,
                imageDrawable,
                closeClickListener,
                ButtonListModel(buttonStyle!!, this.buttons)
            )
        }
    }
}
