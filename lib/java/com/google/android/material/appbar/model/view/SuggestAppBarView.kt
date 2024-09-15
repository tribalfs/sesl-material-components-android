package com.google.android.material.appbar.model.view

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslMisc
import com.google.android.material.R
import com.google.android.material.appbar.model.AppBarModel
import com.google.android.material.appbar.model.ButtonListModel
import com.google.android.material.appbar.model.ButtonModel
import com.google.android.material.appbar.model.SuggestAppBarModel
import org.jetbrains.annotations.NotNull

@RequiresApi(23)
open class SuggestAppBarView @JvmOverloads constructor(
    @NotNull context: Context,
    @Nullable attrs: AttributeSet? = null
) : AppBarView(context, attrs) {

    @NotNull
    private val buttons = mutableListOf<Button>()

    private var model: SuggestAppBarModel<out SuggestAppBarView?>? = null

    @Nullable
    @JvmField
    var bottomLayout: ViewGroup? = null

    @Nullable
    @JvmField
    var closeButton: ImageButton? = null

    @Nullable
    @JvmField
    var titleView: TextView? = null

    init {
        inflate()
    }

    override fun inflate() {
        val context = context

        val viewGroup = LayoutInflater.from(context).inflate(
            R.layout.sesl_app_bar_suggest, this, false) as?  ViewGroup ?: return

        viewGroup.apply {
            titleView = findViewById(R.id.suggest_app_bar_title)
            closeButton = findViewById(R.id.suggest_app_bar_close)
            bottomLayout = findViewById(R.id.suggest_app_bar_bottom_layout)
        }

        updateResource(context)
        addView(viewGroup)
    }

    private fun addMargin() {
        bottomLayout?.addView(
            View(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    resources.getDimensionPixelOffset(R.dimen.sesl_appbar_button_side_margin),
                    MATCH_PARENT
                )
            })
    }

    private fun generateButton(buttonModel: ButtonModel, buttonStyleRes: Int): Button {
        return Button(context, null, 0, buttonStyleRes).apply {
            text = buttonModel.text
            contentDescription = buttonModel.contentDescription
            setOnClickListener { v ->
                buttonModel.clickListener?.onClick(v, model!!)
            }
        }
    }


    fun setButtonModels(buttonListModel: ButtonListModel) {
        bottomLayout?.removeAllViews()
        buttons.clear()

        val buttonModels = buttonListModel.buttonModels
        val buttonStyle = buttonListModel.buttonStyle

        for (i in buttonModels.indices) {

            val button = generateButton(
                buttonModels[i], if (isLightTheme()) buttonStyle.defStyleRes else buttonStyle.defStyleResDark
            ).apply {
                maxWidth = resources.getDimensionPixelSize(
                    if (buttonModels.size > 1) { R.dimen.sesl_appbar_button_max_width
                    } else R.dimen.sesl_appbar_button_max_width_multi
                )
            }

            if (i != 0) addMargin()

            buttons.add(button)
            bottomLayout?.addView(button)

        }
    }

    fun setCloseClickListener(onClickListener: AppBarModel.OnClickListener?) {
        closeButton?.apply {
            visibility = if (onClickListener != null) VISIBLE else GONE
            setOnClickListener { v ->
                onClickListener?.onClick(v, model!!)
            }
        }
    }

    fun setModel(model: SuggestAppBarModel<out SuggestAppBarView?>) {
        this.model = model
    }

    override fun updateResource(@NotNull context: Context) {
        titleView?.apply {
            setTextColor(
                resources.getColor(if (isLightTheme()) R.color.sesl_appbar_suggest_title
                else R.color.sesl_appbar_suggest_title_dark, context.theme))
        }

        closeButton?.setBackgroundResource(
            getCloseRes(isLightTheme()))
    }

    private fun getCloseRes(isLightMode: Boolean): Int {
        return if (Build.VERSION.SDK_INT >= 29) {
            if (isLightMode) R.drawable.sesl_close_button_recoil_background
            else R.drawable.sesl_close_button_recoil_background_dark
        }else {
            if (isLightMode) R.drawable.sesl_ic_close else R.drawable.sesl_ic_close_dark
        }
    }

    fun setTitle(title: String?) {
        titleView?.apply {
            text = title
            visibility = if (TextUtils.isEmpty(title)) GONE else VISIBLE
        }
    }

    fun getButtons(): List<Button> {
        return this.buttons
    }

    private var _isLightTheme: Boolean? = null
    private fun isLightTheme(): Boolean{
        if (_isLightTheme == null) {
            _isLightTheme = SeslMisc.isLightTheme(context)
        }
        return _isLightTheme!!
    }
}
