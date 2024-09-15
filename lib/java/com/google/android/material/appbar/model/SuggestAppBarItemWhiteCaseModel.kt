package com.google.android.material.appbar.model

import android.content.Context
import androidx.annotation.RequiresApi
import com.google.android.material.R
import com.google.android.material.appbar.model.view.SuggestAppBarItemWhiteCaseView
import kotlin.reflect.KClass

@RequiresApi(23)
class SuggestAppBarItemWhiteCaseModel<T : SuggestAppBarItemWhiteCaseView?>(
    kclazz: KClass<Any>,
    context: Context,
    title: String?,
    onClickListener: OnClickListener?,
    buttonListModel: ButtonListModel
) : SuggestAppBarItemModel<T>(kclazz, context, title, onClickListener, buttonListModel) {

    override fun init(moduleView: T): T {
        return moduleView!!.apply {
            setModel(this@SuggestAppBarItemWhiteCaseModel)
            setTitle(title)
            setCloseClickListener(closeClickListener)
            setButtonModels(buttonListModel)
        }
    }

    class Builder(private val context: Context) {
        private var buttonStyle: ButtonStyle? = null
        private var buttons: List<ButtonModel> = ArrayList()
        private var closeClickListener: OnClickListener? = null
        private var title: String? = null

        private fun <T : SuggestAppBarItemWhiteCaseView?> create(): SuggestAppBarItemWhiteCaseModel<T> {
            throw NullPointerException()
        }

        fun build(): SuggestAppBarItemWhiteCaseModel<SuggestAppBarItemWhiteCaseView> {

            if (buttonStyle == null) {
                buttonStyle = ButtonStyle(
                    R.style.Basic_CollapsingToolbar_Button_Light,
                    R.style.Basic_CollapsingToolbar_Button
                )
            }

            return SuggestAppBarItemWhiteCaseModel(
                SuggestAppBarItemWhiteCaseView::class as KClass<Any>,
                context,
                title,
                closeClickListener,
                ButtonListModel(buttonStyle!!, this.buttons)
            )
        }

        @JvmOverloads
        fun setButtons(buttons: List<ButtonModel>, buttonStyle: ButtonStyle? = null): Builder {
            this.buttons = buttons
            buttonStyle?.let { this.buttonStyle = it }
            return this
        }

        fun setCloseClickListener(onClickListener: OnClickListener?): Builder {
            this.closeClickListener = onClickListener
            return this
        }

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }
    }

}
