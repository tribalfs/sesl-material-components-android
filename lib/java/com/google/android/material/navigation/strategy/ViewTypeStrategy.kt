package com.google.android.material.navigation.strategy

import android.content.res.Resources
import com.google.android.material.R
import com.google.android.material.navigation.NavigationBarMenuView
import com.google.android.material.navigation.NavigationBarView

//sesl9
abstract class ViewTypeStrategy private constructor() {
	open val itemSeparatorMarginRes: Int = -1
	open val itemSeparatorPaddingRes: Int = -1
	open val selectedSidePaddingRes: Int = -1

	open val isFloatingStyle: Boolean
		get() = false

	abstract val minHeightRes: Int

	abstract val horizontalPaddingRes: Int

	open fun applyNavigationBarStyle(navigationBarView: NavigationBarView) {
		val menuView = navigationBarView.menuView
		val navigationBarMenuView = menuView as? NavigationBarMenuView
		if (navigationBarMenuView != null) {
			val resources = navigationBarMenuView.resources
			val minHeight = getDimensionPixelSize(resources, this.minHeightRes)
			navigationBarMenuView.setMinimumHeight(minHeight)
			updateNavigationBarPadding(navigationBarView)
			navigationBarView.setMinimumHeight(minHeight)
		}
	}

	fun getDimensionPixelSize(resources: Resources, res: Int): Int {
		if (res == -1) {
			return 0
		}
		return resources.getDimensionPixelSize(res)
	}

	open fun getItemMinWidth(resources: Resources, visibleCount: Int): Int {
		return 0
	}

	open fun getItemSeparatorMargin(resources: Resources, isMaxCount: Boolean): Int {
		return getDimensionPixelSize(resources, this.itemSeparatorMarginRes)
	}

	open fun getItemSeparatorPadding(resources: Resources, isMaxCount: Boolean): Int {
		return getDimensionPixelSize(resources, this.itemSeparatorPaddingRes)
	}

	fun getSelectedSidePadding(resources: Resources): Int {
		return getDimensionPixelSize(resources, this.selectedSidePaddingRes)
	}

	open fun getSmallScreenOuterPadding(resources: Resources, visibleCount: Int): Int {
		return 0
	}

	open fun updateNavigationBarPadding(navigationBarView: NavigationBarView) {
		val resources = navigationBarView.resources
		val padding = getDimensionPixelSize(resources, this.horizontalPaddingRes)
		navigationBarView.setPadding(
			padding,
			navigationBarView.paddingTop,
			padding,
			navigationBarView.paddingBottom
		)
	}

	open class IconLabelType : ViewTypeStrategy() {
		override val minHeightRes: Int = R.dimen.sesl_bottom_navigation_icon_mode_height
		override val horizontalPaddingRes: Int = R.dimen.sesl_navigation_bar_icon_text_mode_padding_horizontal
		open val horizontalPaddingMaxCaseRes: Int = R.dimen.sesl_navigation_bar_icon_text_mode_min_padding_horizontal
		override val itemSeparatorMarginRes: Int = R.dimen.sesl_bottom_navigation_icon_mode_padding_horizontal
		open val itemSeparatorMarginMaxCaseRes: Int = R.dimen.sesl_bottom_navigation_icon_mode_min_padding_horizontal

		override fun applyNavigationBarStyle(navigationBarView: NavigationBarView) {
			val menuView = navigationBarView.menuView
			val navigationBarMenuView =
				menuView as? NavigationBarMenuView
			if (navigationBarMenuView != null) {
				val resources = navigationBarMenuView.resources
				val minHeight = getDimensionPixelSize(resources, minHeightRes)
				navigationBarMenuView.setMinimumHeight(minHeight)
				updateNavigationBarPadding(navigationBarView)
				navigationBarView.setMinimumHeight(minHeight)
			}
		}

		override fun getItemSeparatorMargin(resources: Resources, isMaxCount: Boolean): Int {
			return getDimensionPixelSize(
				resources,
				if (isMaxCount) this.itemSeparatorMarginMaxCaseRes else itemSeparatorMarginRes
			)
		}

		override fun updateNavigationBarPadding(navigationBarView: NavigationBarView) {
			val menuView = navigationBarView.getMenuView()
			val navigationBarMenuView =
				menuView as? NavigationBarMenuView
			val visibleItemCount = navigationBarMenuView?.visibleItemCount ?: 0
			val resources = navigationBarView.resources
			val paddingRes =
				if (visibleItemCount == navigationBarView.getMaxItemCount())
					horizontalPaddingMaxCaseRes
				else
					horizontalPaddingRes
			val padding = getDimensionPixelSize(resources, paddingRes)
			navigationBarView.setPadding(
				padding,
				navigationBarView.paddingTop,
				padding,
				navigationBarView.paddingBottom
			)
		}
	}

	open class FloatingIconLabelType : IconLabelType() {
		override val minHeightRes: Int = R.dimen.sesl_bottom_navigation_floating_height
		override val horizontalPaddingRes: Int = R.dimen.sesl_navigation_bar_floating_icon_text_mode_inner_padding_horizontal
		override val horizontalPaddingMaxCaseRes: Int = R.dimen.sesl_navigation_bar_floating_icon_text_mode_inner_padding_horizontal_count_5
		override val itemSeparatorPaddingRes: Int = R.dimen.sesl_bottom_navigation_floating_padding_horizontal
		override val itemSeparatorMarginRes: Int = -1
		override val itemSeparatorMarginMaxCaseRes: Int = itemSeparatorMarginRes
		override val selectedSidePaddingRes: Int = R.dimen.sesl_bottom_navigation_floating_icon_text_selected_side_padding
		override val isFloatingStyle: Boolean = true
		open val itemSeparatorPaddingMaxCaseRes: Int = R.dimen.sesl_bottom_navigation_floating_padding_horizontal_icon_text_count_5


		override fun getItemMinWidth(resources: Resources, visibleCount: Int): Int {
			val resId: Int
			if (visibleCount == 1) {
				resId = R.dimen.sesl_navigation_bar_floating_icon_text_min_width_count_1
			} else if (visibleCount == 2) {
				resId = R.dimen.sesl_navigation_bar_floating_icon_text_min_width_count_2
			} else {
				resId = R.dimen.sesl_navigation_bar_floating_icon_text_min_width_count_over_3
			}
			return resources.getDimensionPixelSize(resId)
		}

		override fun getItemSeparatorPadding(resources: Resources, isMaxCount: Boolean): Int {
			return getDimensionPixelSize(
				resources,
				if (isMaxCount) this.itemSeparatorPaddingMaxCaseRes else itemSeparatorPaddingRes
			)
		}

		override fun getSmallScreenOuterPadding(resources: Resources, visibleCount: Int): Int {
			val resId =
				if (visibleCount <= 1)
					R.dimen.sesl_bottom_navigation_small_screen_outer_padding_single
				else
					R.dimen.sesl_bottom_navigation_small_screen_outer_padding_multi
			return resources.getDimensionPixelSize(resId)
		}
	}

	open class IconOnlyType : ViewTypeStrategy() {
		override val minHeightRes: Int = R.dimen.sesl_bottom_navigation_icon_only_mode_height
		override val horizontalPaddingRes: Int = R.dimen.sesl_navigation_bar_floating_icon_only_mode_inner_padding_horizontal
	}

	class FloatingIconOnlyType : IconOnlyType() {
		override val minHeightRes: Int = R.dimen.sesl_bottom_navigation_floating_height
		override val horizontalPaddingRes: Int = R.dimen.sesl_navigation_bar_floating_icon_only_mode_inner_padding_horizontal
		override val itemSeparatorMarginRes: Int = -1
		override val itemSeparatorPaddingRes: Int = R.dimen.sesl_bottom_navigation_floating_padding_horizontal
		override val selectedSidePaddingRes: Int = R.dimen.sesl_bottom_navigation_floating_icon_only_selected_side_padding
		override val isFloatingStyle = true

		override fun getItemMinWidth(resources: Resources, visibleCount: Int): Int {
			val resId: Int
			if (visibleCount == 1) {
				resId = R.dimen.sesl_navigation_bar_floating_icon_only_min_width_count_1
			} else if (visibleCount == 2) {
				resId = R.dimen.sesl_navigation_bar_floating_icon_only_min_width_count_2
			} else {
				resId = R.dimen.sesl_navigation_bar_floating_icon_only_min_width_count_over_3
			}
			return resources.getDimensionPixelSize(resId)
		}

		override fun getSmallScreenOuterPadding(resources: Resources, visibleCount: Int): Int {
			val resId =
				if (visibleCount <= 1)
					R.dimen.sesl_bottom_navigation_small_screen_outer_padding_single
				else
					R.dimen.sesl_bottom_navigation_small_screen_outer_padding_multi
			return resources.getDimensionPixelSize(resId)
		}
	}

	class LabelOnlyType : ViewTypeStrategy() {
		override val minHeightRes: Int = R.dimen.sesl_bottom_navigation_text_mode_height
		override val horizontalPaddingRes: Int = R.dimen.sesl_navigation_bar_text_mode_padding_horizontal
	}
}
