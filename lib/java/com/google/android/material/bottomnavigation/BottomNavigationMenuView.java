/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.bottomnavigation;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static com.google.android.material.bottomnavigation.BottomNavigationView.MAX_ITEM_COUNT;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.view.menu.MenuBuilder;

import com.google.android.material.navigation.NavigationBarItemView;
import com.google.android.material.navigation.NavigationBarMenuView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.strategy.ViewTypeStrategy;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** @hide For internal use only.  <b>SESL Variant.</b> */
@RestrictTo(LIBRARY_GROUP)
public class BottomNavigationMenuView extends NavigationBarMenuView {
  private final int inactiveItemMaxWidth;
  private final int inactiveItemMinWidth;
  private int activeItemMaxWidth;
  private final int activeItemMinWidth;

  private boolean itemHorizontalTranslationEnabled;
  private final List<Integer> tempChildWidths = new ArrayList<>();

  private float mWidthPercent;//sesl

  //Sesl9
  boolean isWrapContent;
  private boolean isSmallScreenMode;
  @Nullable
  private Integer mLastAppliedItemBackgroundResId = null;
  @Nullable ViewTypeChangeListener onViewTypeChangeListener;
  public interface ViewTypeChangeListener {
    void onViewTypeChanged(int viewType);
  }
  //sesl9

  public BottomNavigationMenuView(@NonNull Context context) {
    super(context);

    FrameLayout.LayoutParams params =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.gravity = Gravity.CENTER;
    setLayoutParams(params);

    final Resources res = getResources();

    /*Sesl*/
    TypedValue outValue = new TypedValue();
    res.getValue(R.dimen.sesl_bottom_navigation_width_proportion, outValue, true);
    mWidthPercent = outValue.getFloat();
    /*sesl*/

    inactiveItemMaxWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_item_max_width/*sesl*/);
    inactiveItemMinWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_item_min_width/*sesl*/);
    activeItemMaxWidth =
        (int) (getResources().getDisplayMetrics().widthPixels * mWidthPercent);//sesl
    activeItemMinWidth =
        res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_active_item_min_width);//sesl

    mUseItemPool = false;//sesl
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width = MeasureSpec.getSize(widthMeasureSpec);
    // Use visible item count to calculate widths
    final int visibleCount = getCurrentVisibleContentItemCount();//Not used by sesl, as explained below
    // Use total item counts to measure children
    final int totalCount = getChildCount();
    tempChildWidths.clear();

    //Sesl
    final MenuBuilder menu = getMenu();
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    mWidthPercent = width / displayMetrics.density < 590.0F ? 1.0F : 0.75F;
    activeItemMaxWidth = (int)(displayMetrics.widthPixels * mWidthPercent);
    final int maxWidth = (int)(width * mWidthPercent);
    // Sesl uses total of action items + overflow button
    // to distribute available the width,
    // not `visibleCount` as Menuitem.isVisible() includes overflow items.
    int actionAndOverflowCount = 0;
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i).getVisibility() == View.VISIBLE) {
        actionAndOverflowCount++;
      }
    }
    int measuredWidth;
    //sesl

    int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    final int heightSpec = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY/*sesl9*/);

    if (getItemIconGravity() == NavigationBarView.ITEM_ICON_GRAVITY_TOP) {
      if (isShifting(getLabelVisibilityMode(), totalCount/*sesl*/)
          && isItemHorizontalTranslationEnabled()) {
        final View activeChild = getChildAt(getSelectedItemPosition());
        int activeItemWidth = activeItemMinWidth;
        if (activeChild.getVisibility() != View.GONE) {
          // Do an AT_MOST measure pass on the active child to get its desired width, and resize the
          // active child view based on that width
          activeChild.measure(
              MeasureSpec.makeMeasureSpec(activeItemMaxWidth, MeasureSpec.AT_MOST), heightSpec);
          activeItemWidth = max(activeItemWidth, activeChild.getMeasuredWidth());
        }
        final int inactiveCount = totalCount/*sesl*/ - (activeChild.getVisibility() != View.GONE ? 1 : 0);
        final int activeMaxAvailable = maxWidth - inactiveCount * inactiveItemMinWidth;
        final int activeWidth = min(activeMaxAvailable, min(activeItemWidth, activeItemMaxWidth));//sesl
        final int inactiveAvailable =
            (maxWidth/*sesl*/ - activeWidth) / (inactiveCount == 0 ? 1 : inactiveCount);
        final int inactiveWidth = min(inactiveAvailable, inactiveItemMaxWidth);
        int extra = maxWidth/*sesl*/ - activeWidth - (inactiveWidth * inactiveCount);

        for (int i = 0; i < totalCount; i++) {
          int tempChildWidth = 0;
          if (getChildAt(i).getVisibility() != View.GONE) {
            tempChildWidth = (i == getSelectedItemPosition()) ? activeWidth : inactiveWidth;
            // Account for integer division which sometimes leaves some extra pixel spaces.
            // e.g. If the nav was 10px wide, and 3 children were measured to be 3px-3px-3px, there
            // would be a 1px gap somewhere, which this fills in.
            if (extra > 0) {
              tempChildWidth++;
              extra--;
            }
          }
          tempChildWidths.add(tempChildWidth);
        }
      } else {
        //Sesl
        int maxAvailable = maxWidth / (actionAndOverflowCount == 0 ? 1 : actionAndOverflowCount);
        int childWidth;
        if (actionAndOverflowCount != 2) {
          childWidth = min(maxAvailable, activeItemMaxWidth);
        } else {
          childWidth = maxAvailable;
        }
        int extra = maxWidth - childWidth * actionAndOverflowCount;
        //sesl
        for (int i = 0; i < totalCount; i++) {
          int tempChildWidth = 0;
          if (getChildAt(i).getVisibility() != View.GONE) {
            tempChildWidth = childWidth;
            if (extra > 0) {
              tempChildWidth++;
              extra--;
            }
          }
          tempChildWidths.add(tempChildWidth);
        }
      }

      //Sesl9
      int itemMinWidth;
      int itemSeparatorPadding = 0;
      if (isWrapContent) {
        if (isSmallScreenMode) {
          itemMinWidth =
              getResources()
                  .getDimensionPixelSize(
                      R.dimen.sesl_navigation_bar_floating_small_screen_min_width);
        } else {
          ViewTypeStrategy viewTypeStrategy = mStrategy;
          itemMinWidth =
              viewTypeStrategy == null
                  ? 0
                  : viewTypeStrategy.getItemMinWidth(getResources(), actionAndOverflowCount);
        }

        if (getParent() instanceof BottomNavigationView bnv) {
          boolean isMaxCount = actionAndOverflowCount == bnv.getMaxItemCount();
          ViewTypeStrategy viewTypeStrategy = mStrategy;
          if (viewTypeStrategy != null && !isSmallScreenMode) {
            viewTypeStrategy.updateNavigationBarPadding(bnv);
            itemSeparatorPadding = mStrategy.getItemSeparatorPadding(getResources(), isMaxCount);
          }
        }

        measuredWidth = 0;
        for (int i = 0; i < totalCount; i++) {
          final View child = getChildAt(i);
          if (child != null && child.getVisibility() != GONE) {
            int padding = isSmallScreenMode ? 0 : itemSeparatorPadding;
            if (mStrategy != null) {
              child.setPadding(padding, child.getPaddingTop(), padding, child.getPaddingBottom());
            }
            if (isWrapContent) {
              child.setMinimumWidth((padding * 2) + itemMinWidth);
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(
                    tempChildWidths.get(i),
                    isWrapContent ? MeasureSpec.AT_MOST : MeasureSpec.EXACTLY),
                heightSpec);
            child.getLayoutParams().width = child.getMeasuredWidth();
            measuredWidth += child.getMeasuredWidth();
          }
        }
      } else {
        measuredWidth = 0;
        for (int i = 0; i < totalCount; i++) {
          View child = getChildAt(i);
          if (child != null && child.getVisibility() != GONE) {
            int padding = isSmallScreenMode ? 0 : itemSeparatorPadding;
            if (mStrategy != null) {
              child.setPadding(padding, child.getPaddingTop(), padding, child.getPaddingBottom());
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(
                    tempChildWidths.get(i), MeasureSpec.EXACTLY),
                heightSpec);
            child.getLayoutParams().width = child.getMeasuredWidth();
            measuredWidth += child.getMeasuredWidth();
          }
        }
      }
    } else {
      int count = totalCount == 0 ? 1 : totalCount;
      float totalWidth = (float) maxWidth;
      float widthCap = min((count + 3) / 10.0f, 0.9f) * totalWidth;
      float childCountF = (float) count;
      int minChildWidth = Math.round(widthCap / childCountF);
      int defaultChildWidth = Math.round(totalWidth / childCountF);

      measuredWidth = 0;
      for (int i = 0; i < totalCount; i++) {
        View child = getChildAt(i);
        if (child.getVisibility() != GONE) {
          child.measure(
              MeasureSpec.makeMeasureSpec(defaultChildWidth, MeasureSpec.AT_MOST), heightSpec);
          if (child.getMeasuredWidth() < minChildWidth) {
            child.measure(
                MeasureSpec.makeMeasureSpec(minChildWidth, MeasureSpec.EXACTLY), heightSpec);
          }
          measuredWidth += child.getMeasuredWidth();
        }
      }
    }

    if (isWrapContent) {
      int largestItemWidth = getLargestItemWidth();
      if (largestItemWidth != 0) {
        int totalWidthNeeded = largestItemWidth * actionAndOverflowCount;
        int maxAvailableWidth =
            min(
                getResources()
                    .getDimensionPixelSize(
                        R.dimen.sesl_bottom_navigation_floating_max_width),
                maxWidth)
                - getPaddingLeft()
                - getPaddingRight();

        boolean fitsInMax = totalWidthNeeded <= maxAvailableWidth;
        boolean remeasured = false;
        int remainingWidth = maxAvailableWidth;
        int remainingChildren = actionAndOverflowCount;

        for (int i = 0; i < totalCount; i++) {
          View child = getChildAt(i);
          if (child != null && child.getVisibility() != GONE) {
            LayoutParams params = child.getLayoutParams();
            int targetWidth;
            if (fitsInMax) {
              targetWidth = largestItemWidth;
            } else {
              targetWidth = remainingWidth / max(remainingChildren, 1);
            }
            if (params.width != targetWidth) {
              child.measure(
                  MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY), heightSpec);
              remeasured = true;
            }
            remainingWidth -= targetWidth;
            remainingChildren--;
          }
        }

        if (remeasured) {
          int newTotalWidth = 0;
          for (int i = 0; i < totalCount; i++) {
            View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
              newTotalWidth += child.getMeasuredWidth();
            }
          }
          measuredWidth = newTotalWidth;
        }
      }
    }

    setMeasuredDimension(measuredWidth, parentHeight);
    //sesl9
  }

  //sesl9
  private int getLargestItemWidth() {
    int iMax = 0;
    for (int i = 0; i < getChildCount(); i++) {
        View childAt = getChildAt(i);
        if (childAt.getVisibility() != 8) {
            iMax = max(iMax, childAt.getMeasuredWidth());
        }
    }
    return iMax;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    final int count = getChildCount();
    final int width = right - left;
    final int height = bottom - top;

    //Sesl9
    int itemSeparatorMargin =
        (mStrategy == null || this.isSmallScreenMode)
            ? 0
            : mStrategy.getItemSeparatorMargin(
                getResources(), getViewVisibleItemCount() == MAX_ITEM_COUNT);
    //sesl9

    int used = 0;
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == GONE) {
        continue;
      }
      if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
        //Sesl9
        int rightBound = width - used;
        child.layout((rightBound - child.getMeasuredWidth()) + itemSeparatorMargin, 0, rightBound - itemSeparatorMargin, height);
        //sesl9
      } else {
        //sesl9
        child.layout(used + itemSeparatorMargin, 0, (child.getMeasuredWidth() + used) - itemSeparatorMargin, height);
      }
      used += child.getMeasuredWidth();
    }
    updateBadgeIfNeeded();//sesl
  }

  /**
   * Sets whether the menu items horizontally translate on selection when the combined item widths
   * fill the screen.
   *
   * @param itemHorizontalTranslationEnabled whether the menu items horizontally translate on
   *     selection
   * @see #isItemHorizontalTranslationEnabled()
   */
  public void setItemHorizontalTranslationEnabled(boolean itemHorizontalTranslationEnabled) {
    this.itemHorizontalTranslationEnabled = itemHorizontalTranslationEnabled;
  }

  /**
   * Returns whether the menu items horizontally translate on selection when the combined item
   * widths fill the screen.
   *
   * @return whether the menu items horizontally translate on selection
   * @see #setItemHorizontalTranslationEnabled(boolean)
   */
  public boolean isItemHorizontalTranslationEnabled() {
    return itemHorizontalTranslationEnabled;
  }

  @Override
  @NonNull
  public NavigationBarItemView createNavigationBarItemView(@NonNull Context context) {
    return new BottomNavigationItemView(context);
  }


  //Sesl9
  @Override
  public boolean seslIsSmallScreenMode() {
    return this.isSmallScreenMode;
  }

  public void setSmallScreenMode(boolean smallScreenMode) {
    boolean changed = this.isSmallScreenMode != smallScreenMode;
    this.isSmallScreenMode = smallScreenMode;
    ViewTypeStrategy viewTypeStrategy = this.mStrategy;
    if (viewTypeStrategy != null) {
      int selectedSidePadding =
          smallScreenMode ? 0 : viewTypeStrategy.getSelectedSidePadding(getResources());
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        if (child instanceof NavigationBarItemView itemView) {
          itemView.setSelectedSidePadding(selectedSidePadding);
          itemView.seslSetSmallScreenTooltipEnabled(smallScreenMode);
        }
      }
    }
    if (changed) {
      requestLayout();
    }
  }

  public void setStrategy(@NonNull ViewTypeStrategy strategy) {
    this.mStrategy = strategy;
    this.mIsFloatingStyle = strategy.isFloatingStyle();
    int selectedSidePadding =
        this.isSmallScreenMode ? 0 : this.mStrategy.getSelectedSidePadding(getResources());
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child instanceof NavigationBarItemView) {
        ((NavigationBarItemView) child).setSelectedSidePadding(selectedSidePadding);
      }
    }
    if (this.mIsFloatingStyle) {
      setClipToPadding(false);
      setClipChildren(false);
    }
  }

  @Override
  public void setViewType(int viewType) {
    super.setViewType(viewType);
    if (this.onViewTypeChangeListener != null) {
      this.onViewTypeChangeListener.onViewTypeChanged(viewType);
    }
  }

  public void setViewTypeChangeListener(@Nullable ViewTypeChangeListener listener) {
    this.onViewTypeChangeListener = listener;
  }

  public void updateItemBackground(boolean force) {
    if (this.isWrapContent) {
      boolean isLightTheme = SeslMisc.isLightTheme(getContext());
      int resId =
          (getLabelVisibilityMode() == 2 && this.isSmallScreenMode)
              ? (isLightTheme
                 ? R.drawable.sesl_bottom_navigation_item_background_icon_light_small_screen
                 : R.drawable.sesl_bottom_navigation_item_background_icon_dark_small_screen)
              : (isLightTheme
                 ? R.drawable.sesl_bottom_navigation_item_background_icon_light
                 : R.drawable.sesl_bottom_navigation_item_background_icon_dark);

      if (force
          || this.mLastAppliedItemBackgroundResId == null
          || !this.mLastAppliedItemBackgroundResId.equals(resId)) {
        this.mLastAppliedItemBackgroundResId = resId;
        for (int i = 0; i < getChildCount(); i++) {
          View child = getChildAt(i);
          if (child instanceof NavigationBarItemView itemView) {
            itemView.setItemBackground(resId);
            itemView.jumpDrawablesToCurrentState();
            itemView.refreshDrawableState();
          }
        }
      }
    }
  }
  //sesl9
}
