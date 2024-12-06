package com.google.android.material.appbar.model

import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.annotation.RequiresApi
import com.google.android.material.R
import com.google.android.material.appbar.model.view.SuggestAppBarView
import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass

@RequiresApi(23)
open class SuggestAppBarModel<T : SuggestAppBarView> internal constructor(
    @NotNull kclazz: KClass<T>,
    @NotNull context: Context,
    @NotNull @JvmField val title: String?,
    @NotNull  @JvmField val closeClickListener: OnClickListener?,
    @JvmField val buttonListModel: ButtonListModel
) : AppBarModel<T>(kclazz, context) {

    override fun init(moduleView: T): T {
        return moduleView.apply {
            setModel(this@SuggestAppBarModel)
            setTitle(title)
            setCloseClickListener(closeClickListener)
            setButtonModels(buttonListModel)
            updateResource(context)
        }
    }

    class Builder(private val context: Context) {
        private var buttonStyle: ButtonStyle? = null
        private var buttons: List<ButtonModel> = ArrayList()
        private var closeClickListener: OnClickListener? = null
        private var title: String? = null

        fun build(): SuggestAppBarModel<SuggestAppBarView> {
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
