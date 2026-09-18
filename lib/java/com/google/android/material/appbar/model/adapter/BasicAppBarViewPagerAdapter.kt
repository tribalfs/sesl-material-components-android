package com.google.android.material.appbar.model.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.model.AppBarModel
import com.google.android.material.appbar.model.SuggestAppBarModel
import com.google.android.material.appbar.model.view.AppBarView
import com.google.android.material.appbar.model.view.SuggestAppBarItemView

//sesl9
abstract class BasicAppBarViewPagerAdapter<T : SuggestAppBarItemView, U : BasicAppBarViewPagerAdapter.BasicViewHolder<T>>(
    val context: Context
) : RecyclerView.Adapter<U>() {

    open class BasicViewHolder<V : SuggestAppBarItemView>(
        val appBarModuleView: V
    ) : RecyclerView.ViewHolder(appBarModuleView)

    open val data: MutableList<SuggestAppBarModel<T>> = ArrayList()

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = 0

    override fun onBindViewHolder(holder: U, position: Int) {
        data[position].init(holder.appBarModuleView)
    }

    override fun onViewRecycled(holder: U) {
        super.onViewRecycled(holder)
        val view = holder.appBarModuleView
        view.alpha = 1.0f
        view.scaleX = 1.0f
        view.scaleY = 1.0f
    }

    fun setDataModel(dataModel: List<SuggestAppBarModel<T>>) {
        data.clear()
        data.addAll(dataModel)
        notifyDataSetChanged()
    }

    fun removeDataModel(dataModel: AppBarModel<out AppBarView>) {
        val index = data.indexOf(dataModel)
        if (index != -1) {
            data.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun find(dataModel: AppBarModel<out AppBarView>): Int {
        return data.indexOf(dataModel)
    }
}
