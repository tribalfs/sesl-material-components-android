package com.google.android.material.appbar.model

import android.content.Context
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.google.android.material.R
import com.google.android.material.appbar.model.view.SuggestAppBarItemView
import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass

@RequiresApi(23)
open class SuggestAppBarItemModel<T : SuggestAppBarItemView?>(
    @NotNull kclazz: KClass<Any>,
    @NotNull context: Context,
    @Nullable title: String?,
    @Nullable onClickListener: OnClickListener?,
    @NotNull buttonListModel: ButtonListModel
) : SuggestAppBarModel<T>(kclazz, context, title, onClickListener, buttonListModel) {

    override fun init(moduleView: T): T {
        return moduleView!!.apply {
            setModel(this@SuggestAppBarItemModel)
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

        private fun <T : SuggestAppBarItemView?> create(): SuggestAppBarItemModel<T> {
            throw NullPointerException()
        }

        fun build(): SuggestAppBarItemModel<SuggestAppBarItemView> {

            if (buttonStyle == null) {
                buttonStyle = ButtonStyle(
                    R.style.Basic_CollapsingToolbar_Button_Light,
                    R.style.Basic_CollapsingToolbar_Button
                )
            }

            return SuggestAppBarItemModel(
                SuggestAppBarItemView::class as KClass<Any>,
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
