/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.google.android.material.navigation;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.core.view.MenuItemCompat.SESL_MENU_ITEM_TYPE_CHECKBOX;

import static com.google.android.material.navigation.NavigationBarView.SESL_TYPE_ICON_LABEL;
import static com.google.android.material.navigation.NavigationBarView.SESL_TYPE_ICON_ONLY;
import static com.google.android.material.navigation.NavigationBarView.SESL_TYPE_LABEL_ONLY;

import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.core.util.Pools;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.internal.TextScale;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.HashSet;
import android.widget.TextView;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SeslMenuItem;

/**
 * <b>SESL variant</b><br><br>
 *
 * Provides a view that will be use to render a menu view inside a {@link NavigationBarView}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class NavigationBarMenuView extends ViewGroup implements MenuView {
  //Sesl
  private static final String TAG = "NavigationBarMenuView";
  static final int BADGE_TYPE_OVERFLOW = 0;
  static final int BADGE_TYPE_DOT = 1;
  static final int BADGE_TYPE_N = 2;
  private ContentResolver mContentResolver;
  MenuBuilder mDummyMenu;
  private InternalBtnInfo mInvisibleBtns = null;
  NavigationBarItemView mOverflowButton = null;
  private MenuBuilder mOverflowMenu = null;
  private ColorDrawable mSBBTextColorDrawable;
  private MenuBuilder.Callback mSelectedCallback;
  private InternalBtnInfo mVisibleBtns = null;
  private int mMaxItemCount = 0;
  @StyleRes
  private int mSeslLabelTextAppearance;
  private int mViewType = SESL_TYPE_ICON_LABEL;
  private int mViewVisibleItemCount = 0;
  private int mVisibleItemCount = 0;

  private boolean mHasGroupDivider;
  private boolean mHasOverflowMenu = false;
  protected boolean mUseItemPool = true;
  private static final long ACTIVE_ANIMATION_DURATION_MS = 0;
  //sesl

  //Sesl7
  private int itemStateListAnimatorId = 0;
  private boolean mExclusiveCheckable = true;
  //sesl7

  private static final int ITEM_POOL_SIZE = 5;
  private static final int NO_PADDING = -1;

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
  private static final int[] DISABLED_STATE_SET = {-android.R.attr.state_enabled};

  @Nullable private final TransitionSet set;
  @NonNull private final OnClickListener onClickListener;
  private final Pools.Pool<NavigationBarItemView> itemPool =
      new Pools.SynchronizedPool<>(ITEM_POOL_SIZE);

  @NonNull
  private final SparseArray<OnTouchListener> onTouchListeners = new SparseArray<>(ITEM_POOL_SIZE);

  @NavigationBarView.LabelVisibility private int labelVisibilityMode;

  @Nullable private NavigationBarItemView[] buttons;
  private int selectedItemId = 0;
  private int selectedItemPosition = 0;

  @Nullable private ColorStateList itemIconTint;
  @Dimension private int itemIconSize;
  private ColorStateList itemTextColorFromUser;
  @Nullable private final ColorStateList itemTextColorDefault;
  @StyleRes private int itemTextAppearanceInactive;
  @StyleRes private int itemTextAppearanceActive;
  private boolean itemTextAppearanceActiveBoldEnabled;
  private Drawable itemBackground;
  @Nullable private ColorStateList itemRippleColor;
  private int itemBackgroundRes;
  @NonNull private SparseArray<BadgeDrawable> badgeDrawables =
      new SparseArray<>(ITEM_POOL_SIZE);
  private int itemPaddingTop = NO_PADDING;
  private int itemPaddingBottom = NO_PADDING;
  private int itemActiveIndicatorLabelPadding = NO_PADDING;
  private boolean itemActiveIndicatorEnabled;
  private int itemActiveIndicatorWidth;
  private int itemActiveIndicatorHeight;
  private int itemActiveIndicatorMarginHorizontal;
  private ShapeAppearanceModel itemActiveIndicatorShapeAppearance;
  private boolean itemActiveIndicatorResizeable = false;
  private ColorStateList itemActiveIndicatorColor;

  private NavigationBarPresenter presenter;
  private MenuBuilder menu;

  private final int dotBadgeSize;
  private final int iconModeMinPaddingHorizontal;
  private final int iconModePaddingHorizontal;
  private final int nBadgeTopMargin;
  private final int nBadgeStartMargin;
  private final Drawable dotBadgeBackground;
  private final Drawable nBadgeBackground;

  public NavigationBarMenuView(@NonNull Context context) {
    super(context);

    itemTextColorDefault = createDefaultColorStateList(android.R.attr.textColorSecondary);

    Resources res = context.getResources();
    dotBadgeSize = res.getDimensionPixelOffset(R.dimen.sesl_bottom_navigation_dot_badge_size);
    iconModeMinPaddingHorizontal = res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_mode_min_padding_horizontal);
    iconModePaddingHorizontal = res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_mode_padding_horizontal);
    nBadgeTopMargin = res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_N_badge_top_margin);
    nBadgeStartMargin = res.getDimensionPixelSize(R.dimen.sesl_bottom_navigation_N_badge_start_margin);
    dotBadgeBackground = res.getDrawable(R.drawable.sesl_dot_badge);
    nBadgeBackground = res.getDrawable(R.drawable.sesl_tab_n_badge);

    if (this.isInEditMode()) {
      set = null;
    } else {
      set = new AutoTransition();
      set.setOrdering(TransitionSet.ORDERING_TOGETHER);
      set.setDuration(ACTIVE_ANIMATION_DURATION_MS);//sesl
      set.addTransition(new TextScale());
    }

    onClickListener =
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            final NavigationBarItemView itemView = (NavigationBarItemView) v;
            MenuItem item = itemView.getItemData();
            if (!menu.performItemAction(item, presenter, 0)) {
              item.setChecked(mExclusiveCheckable || !item.isChecked());
            }
          }
        };

    mContentResolver = context.getContentResolver();//sesl

    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  @Override
  public void initialize(@NonNull MenuBuilder menu) {
    this.menu = menu;
  }

  @Override
  public int getWindowAnimations() {
    return 0;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
    infoCompat.setCollectionInfo(
        CollectionInfoCompat.obtain(
            /* rowCount= */ 1,
            /* columnCount= */ menu.getVisibleItems().size(),
            /* hierarchical= */ false,
            /* selectionMode = */ CollectionInfoCompat.SELECTION_MODE_SINGLE));
  }

  /**
   * Sets the tint which is applied to the menu items' icons.
   *
   * @param tint the tint to apply
   */
  public void setIconTintList(@Nullable ColorStateList tint) {
    itemIconTint = tint;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setIconTintList(tint);
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setIconTintList(tint);//sesl
    }
  }

  /**
   * Returns the tint which is applied for the menu item labels.
   *
   * @return the ColorStateList that is used to tint menu items' icons
   */
  @Nullable
  public ColorStateList getIconTintList() {
    return itemIconTint;
  }

  /**
   * Sets the size to provide for the menu item icons.
   *
   * <p>For best image resolution, use an icon with the same size set in this method.
   *
   * @param iconSize the size to provide for the menu item icons in pixels
   */
  public void setItemIconSize(@Dimension int iconSize) {
    this.itemIconSize = iconSize;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setIconSize(iconSize);
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setIconSize(iconSize);//sesl
    }
  }

  /** Returns the size in pixels provided for the menu item icons. */
  @Dimension
  public int getItemIconSize() {
    return itemIconSize;
  }

  /**
   * Sets the text color to be used for the menu item labels.
   *
   * @param color the ColorStateList used for menu item labels
   */
  public void setItemTextColor(@Nullable ColorStateList color) {
    itemTextColorFromUser = color;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setTextColor(color);
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setTextColor(color);//sesl
      setOverflowSpanColor(0, true);//sesl
    }
  }

  /**
   * Returns the text color used for menu item labels.
   *
   * @return the ColorStateList used for menu items labels
   */
  @Nullable
  public ColorStateList getItemTextColor() {
    return itemTextColorFromUser;
  }

  /**
   * Sets the text appearance to be used for inactive menu item labels.
   *
   * @param textAppearanceRes the text appearance ID used for inactive menu item labels
   */
  public void setItemTextAppearanceInactive(@StyleRes int textAppearanceRes) {
    this.itemTextAppearanceInactive = textAppearanceRes;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setTextAppearanceInactive(textAppearanceRes);
        // Set the text color if the user has set it, since itemTextColorFromUser takes precedence
        // over a color set in the text appearance.
        if (itemTextColorFromUser != null) {
          item.setTextColor(itemTextColorFromUser);
        }
      }
    }
    //Sesl
    if (mOverflowButton != null) {
      mOverflowButton.setTextAppearanceInactive(textAppearanceRes);
      if (itemTextColorFromUser != null) {
        mOverflowButton.setTextColor(itemTextColorFromUser);
      }
    }
    //sesl
  }

  /**
   * Returns the text appearance used for inactive menu item labels.
   *
   * @return the text appearance ID used for inactive menu item labels
   */
  @StyleRes
  public int getItemTextAppearanceInactive() {
    return itemTextAppearanceInactive;
  }

  /**
   * Sets the text appearance to be used for the active menu item label.
   *
   * @param textAppearanceRes the text appearance ID used for the active menu item label
   */
  public void setItemTextAppearanceActive(@StyleRes int textAppearanceRes) {
    this.itemTextAppearanceActive = textAppearanceRes;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setTextAppearanceActive(textAppearanceRes);
        // Set the text color if the user has set it, since itemTextColorFromUser takes precedence
        // over a color set in the text appearance.
        if (itemTextColorFromUser != null) {
          item.setTextColor(itemTextColorFromUser);
        }
      }
    }
    //Sesl
    if (mOverflowButton != null && itemTextColorFromUser != null) {
      mOverflowButton.setTextAppearanceActive(textAppearanceRes);
      mOverflowButton.setTextColor(itemTextColorFromUser);
    }
    //sesl
  }


  /**
   * Sets whether the active menu item label is bold.
   *
   * @param isBold whether the active menu item label is bold
   */
  public void setItemTextAppearanceActiveBoldEnabled(boolean isBold) {
    this.itemTextAppearanceActiveBoldEnabled = isBold;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setTextAppearanceActiveBoldEnabled(isBold);
      }
    }
  }

  /**
   * Returns the text appearance used for the active menu item label.
   *
   * @return the text appearance ID used for the active menu item label
   */
  @StyleRes
  public int getItemTextAppearanceActive() {
    return itemTextAppearanceActive;
  }

  /**
   * Sets the resource ID to be used for item backgrounds.
   *
   * @param background the resource ID of the background
   */
  public void setItemBackgroundRes(int background) {
    itemBackgroundRes = background;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) break;//sesl
        item.setItemBackground(background);
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setItemBackground(background);//sesl
    }
  }


  /**
   * Get the distance from the top of an item's icon/active indicator to the top of the navigation
   * bar item.
   */
  @Px
  public int getItemPaddingTop() {
    return itemPaddingTop;
  }

  /**
   * Set the distance from the top of an items icon/active indicator to the top of the navigation
   * bar item.
   */
  public void setItemPaddingTop(@Px int paddingTop) {
    itemPaddingTop = paddingTop;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setItemPaddingTop(paddingTop);
      }
    }
  }

  /**
   * Get the distance from the bottom of an item's label to the bottom of the navigation bar item.
   */
  @Px
  public int getItemPaddingBottom() {
    return itemPaddingBottom;
  }

  /**
   * Set the distance from the bottom of an item's label to the bottom of the navigation bar item.
   */
  public void setItemPaddingBottom(@Px int paddingBottom) {
    itemPaddingBottom = paddingBottom;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setItemPaddingBottom(paddingBottom);
      }
    }
  }

  /**
   * Get the distance between the item's active indicator container and the label.
   */
  @Px
  public int getActiveIndicatorLabelPadding() {
    return itemActiveIndicatorLabelPadding;
  }

  /**
   * Set the distance between the active indicator container and the item's label.
   */
  public void setActiveIndicatorLabelPadding(@Px int activeIndicatorLabelPadding) {
    itemActiveIndicatorLabelPadding = activeIndicatorLabelPadding;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorLabelPadding(activeIndicatorLabelPadding);
      }
    }
  }

  /**
   * Returns whether or not an active indicator is enabled for the navigation bar.
   *
   * @return true if the active indicator is enabled.
   */
  public boolean getItemActiveIndicatorEnabled() {
    return itemActiveIndicatorEnabled;
  }

  /**
   * Set whether or not an active indicator is enabled for the navigation bar.
   *
   * @param enabled true if an active indicator should be shown.
   */
  public void setItemActiveIndicatorEnabled(boolean enabled) {
    this.itemActiveIndicatorEnabled = enabled;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorEnabled(enabled);
      }
    }
  }

  /**
   * Get the width of the selected item's active indicator.
   *
   * @return The width, in pixels, of the active indicator.
   */
  @Px
  public int getItemActiveIndicatorWidth() {
    return itemActiveIndicatorWidth;
  }

  /**
   * Set the width to be used for the selected item's active indicator.
   *
   * @param width The width, in pixels, of the active indicator.
   */
  public void setItemActiveIndicatorWidth(@Px int width) {
    this.itemActiveIndicatorWidth = width;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorWidth(width);
      }
    }
  }

  /**
   * Get the height of the selected item's active indicator.
   *
   * @return The height, in pixels, of the active indicator.
   */
  @Px
  public int getItemActiveIndicatorHeight() {
    return itemActiveIndicatorHeight;
  }

  /**
   * Set the height to be used for the selected item's active indicator.
   *
   * @param height The height, in pixels, of the active indicator.
   */
  public void setItemActiveIndicatorHeight(@Px int height) {
    this.itemActiveIndicatorHeight = height;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorHeight(height);
      }
    }
  }

  /**
   * Get the margin that will be maintained at the start and end of the active indicator away from
   * the edges of its parent container.
   *
   * @return The horizontal margin, in pixels.
   */
  @Px
  public int getItemActiveIndicatorMarginHorizontal() {
    return itemActiveIndicatorMarginHorizontal;
  }

  /**
   * Set the horizontal margin that will be maintained at the start and end of the active indicator,
   * making sure the indicator remains the given distance from the edge of its parent container.
   *
   * @param marginHorizontal The horizontal margin, in pixels.
   */
  public void setItemActiveIndicatorMarginHorizontal(@Px int marginHorizontal) {
    itemActiveIndicatorMarginHorizontal = marginHorizontal;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorMarginHorizontal(marginHorizontal);
      }
    }
  }

  /**
   * Get the {@link ShapeAppearanceModel} of the active indicator drawable.
   *
   * @return The {@link ShapeAppearanceModel} of the active indicator drawable.
   */
  @Nullable
  public ShapeAppearanceModel getItemActiveIndicatorShapeAppearance() {
    return itemActiveIndicatorShapeAppearance;
  }

  /**
   * Set the {@link ShapeAppearanceModel} of the active indicator drawable.
   *
   * @param shapeAppearance The {@link ShapeAppearanceModel} of the active indicator drawable.
   */
  public void setItemActiveIndicatorShapeAppearance(
      @Nullable ShapeAppearanceModel shapeAppearance) {
    this.itemActiveIndicatorShapeAppearance = shapeAppearance;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorDrawable(createItemActiveIndicatorDrawable());
      }
    }
  }

  /**
   * Get whether the active indicator can be resized.
   */
  protected boolean isItemActiveIndicatorResizeable() {
    return this.itemActiveIndicatorResizeable;
  }

  /**
   * Set whether the active indicator can be resized. If true, the indicator will automatically
   * change size in response to label visibility modes.
   */
  protected void setItemActiveIndicatorResizeable(boolean resizeable) {
    this.itemActiveIndicatorResizeable = resizeable;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorResizeable(resizeable);
      }
    }
  }

  /**
   * Get the color of the active indicator drawable.
   *
   * @return A {@link ColorStateList} used as the color of the active indicator.
   */
  @Nullable
  public ColorStateList getItemActiveIndicatorColor() {
    return itemActiveIndicatorColor;
  }

  /**
   * Set the {@link ColorStateList} of the active indicator drawable.
   *
   * @param csl The {@link ColorStateList} used as the color of the active indicator.
   */
  public void setItemActiveIndicatorColor(@Nullable ColorStateList csl) {
    this.itemActiveIndicatorColor = csl;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        item.setActiveIndicatorDrawable(createItemActiveIndicatorDrawable());
      }
    }
  }

  /**
   * Create a drawable using the {@code itemActiveIndicatorShapeAppearance} and {@code
   * itemActiveIndicatorColor} to be used as an item's active indicator.
   *
   * <p>This method is called once per menu item so each item has a unique drawable instance which
   * can be manipulated/animated independently.
   *
   * @return A drawable to be used as a menu item's active indicator.
   */
  @Nullable
  private Drawable createItemActiveIndicatorDrawable() {
    if (itemActiveIndicatorShapeAppearance != null && itemActiveIndicatorColor != null) {
      MaterialShapeDrawable drawable =
          new MaterialShapeDrawable(itemActiveIndicatorShapeAppearance);
      drawable.setFillColor(itemActiveIndicatorColor);
      return drawable;
    }

    return null;
  }

  /**
   * Returns the resource ID for the background of the menu items.
   *
   * @return the resource ID for the background
   * @deprecated Use {@link #getItemBackground()} instead.
   */
  @Deprecated
  public int getItemBackgroundRes() {
    return itemBackgroundRes;
  }

  /**
   * Sets the drawable to be used for item backgrounds.
   *
   * @param background the drawable of the background
   */
  public void setItemBackground(@Nullable Drawable background) {
    itemBackground = background;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) {
          break;
        }
        item.setItemBackground(background);
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setItemBackground(background);
    }
  }

  /**
   * Returns the drawable for the background of the menu items.
   *
   * @return the drawable for the background
   */
  @Nullable
  public Drawable getItemBackground() {
    if (buttons != null && buttons.length > 0) {
      // Return button background instead of itemBackground if possible, so that the correct
      // drawable is returned if the background is set via #setItemBackgroundRes.
      return buttons[0].getBackground();
    } else {
      return itemBackground;
    }
  }

  /**
   * Sets the navigation items' label visibility mode.
   *
   * <p>The label is either always shown, never shown, or only shown when activated. Also supports
   * "auto" mode, which uses the item count to determine whether to show or hide the label.
   *
   * @param labelVisibilityMode mode which decides whether or not the label should be shown. Can be
   *     one of {@link NavigationBarView#LABEL_VISIBILITY_AUTO}, {@link
   *     NavigationBarView#LABEL_VISIBILITY_SELECTED}, {@link
   *     NavigationBarView#LABEL_VISIBILITY_LABELED}, or {@link
   *     NavigationBarView#LABEL_VISIBILITY_UNLABELED}
   * @see #getLabelVisibilityMode()
   */
  public void setLabelVisibilityMode(@NavigationBarView.LabelVisibility int labelVisibilityMode) {
    this.labelVisibilityMode = labelVisibilityMode;
  }

  /**
   * Returns the current label visibility mode.
   *
   * @see #setLabelVisibilityMode(int)
   */
  public int getLabelVisibilityMode() {
    return labelVisibilityMode;
  }

  /**
   * Sets an {@link android.view.View.OnTouchListener} for the item view associated with the
   * provided {@code menuItemId}.
   */
  @SuppressLint("ClickableViewAccessibility")
  public void setItemOnTouchListener(int menuItemId, @Nullable OnTouchListener onTouchListener) {
    if (onTouchListener == null) {
      onTouchListeners.remove(menuItemId);
    } else {
      onTouchListeners.put(menuItemId, onTouchListener);
    }
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) return;//sesl
        if (item.getItemData().getItemId() == menuItemId) {
          item.setOnTouchListener(onTouchListener);
        }
      }
    }
  }

  @Nullable
  public ColorStateList createDefaultColorStateList(int baseColorThemeAttr) {
    final TypedValue value = new TypedValue();
    if (!getContext().getTheme().resolveAttribute(baseColorThemeAttr, value, true)) {
      return null;
    }
    ColorStateList baseColor = AppCompatResources.getColorStateList(getContext(), value.resourceId);
    if (!getContext()
        .getTheme()
        .resolveAttribute(androidx.appcompat.R.attr.colorPrimary, value, true)) {
      return null;
    }
    int colorPrimary = value.data;
    int defaultColor = baseColor.getDefaultColor();
    return new ColorStateList(
        new int[][] {DISABLED_STATE_SET, CHECKED_STATE_SET, EMPTY_STATE_SET},
        new int[] {
            baseColor.getColorForState(DISABLED_STATE_SET, defaultColor), colorPrimary, defaultColor
        });
  }

  public void setPresenter(@NonNull NavigationBarPresenter presenter) {
    this.presenter = presenter;
  }

  //Sesl
  @SuppressLint("ClickableViewAccessibility")
  public void buildMenuView() {
    removeAllViews();
    TransitionManager.beginDelayedTransition(this, this.set);

    if (buttons != null && mUseItemPool) {
        for (NavigationBarItemView itemView : buttons) {
            if (itemView != null) {
                itemPool.release(itemView);
                itemView.removeBadge();
                seslRemoveBadge(itemView.getId());
            }
        }
    }

    if (mOverflowButton != null) {
      seslRemoveBadge(R.id.bottom_overflow);
    }

    int menuItemsCount = menu.size();

    if (menuItemsCount == 0) {
      selectedItemId = 0;
      selectedItemPosition = 0;
      buttons = null;
      mVisibleItemCount = 0;
      mOverflowButton = null;
      mOverflowMenu = null;
      mVisibleBtns = null;
      mInvisibleBtns = null;
    } else {
      removeUnusedBadges();
      boolean isShifting = isShifting(labelVisibilityMode, menu.getVisibleItems().size());

      buttons = new NavigationBarItemView[menuItemsCount];
      mVisibleBtns = new InternalBtnInfo(menuItemsCount);
      mInvisibleBtns = new InternalBtnInfo(menuItemsCount);
      mOverflowMenu = new MenuBuilder(getContext());
      mVisibleBtns.cnt = 0;
      mInvisibleBtns.cnt = 0;

      int visibleItemCount = 0;
      int invisibleItemCount = 0;

      for (int i = 0; i < menuItemsCount; i++) {
        // Temporarily suspend updates
        presenter.setUpdateSuspended(true);
        MenuItemImpl menuItem = (MenuItemImpl) menu.getItem(i);
        menuItem.setCheckable(true);
        presenter.setUpdateSuspended(false);

        if (menuItem.requiresOverflow()) {
          // Add to invisible buttons list
          mInvisibleBtns.originPos[mInvisibleBtns.cnt++] = i;

          // Increment invisible item count if the item is not visible
          if (!menuItem.isVisible()) {
            invisibleItemCount++;
          }
        } else {
          // Add to visible buttons list
          mVisibleBtns.originPos[mVisibleBtns.cnt++] = i;

          // Increment visible item count if the item is visible
          if (menuItem.isVisible()) {
            visibleItemCount++;
          }
        }
      }

      mHasOverflowMenu = mInvisibleBtns.cnt - invisibleItemCount > 0;
      if (mHasOverflowMenu) {
        visibleItemCount += 1;
      }

      if (visibleItemCount > mMaxItemCount) {
        visibleItemCount -= (mMaxItemCount - 1);

        if (mHasOverflowMenu) {
          visibleItemCount = visibleItemCount  - 1;
        }

        invisibleItemCount = mVisibleBtns.cnt - 1;

        for(int i = invisibleItemCount; i >= 0; --i) {
          MenuItemImpl menuItem = (MenuItemImpl)menu.getItem(mVisibleBtns.originPos[i]);
          if (!menuItem.isVisible()) {
            invisibleItemCount = mInvisibleBtns.cnt++;
            mInvisibleBtns.originPos[invisibleItemCount] = mVisibleBtns.originPos[i];
            --mVisibleBtns.cnt;
          } else {
            invisibleItemCount = mInvisibleBtns.cnt++;
            mInvisibleBtns.originPos[invisibleItemCount] = mVisibleBtns.originPos[i];
            --mVisibleBtns.cnt;

            invisibleItemCount = visibleItemCount  - 1;
            visibleItemCount  = invisibleItemCount;

            if (invisibleItemCount == 0) {
              break;
            }
          }
        }
      }

      mVisibleItemCount = 0;
      mViewVisibleItemCount = 0;

      for(int i = 0; i < mVisibleBtns.cnt; ++i) {
        this.buildInternalMenu(isShifting, mVisibleBtns.originPos[i]);
      }

      if (mInvisibleBtns.cnt > 0) {
        int currentPos = 0;
        invisibleItemCount = 0;

        while (currentPos < mInvisibleBtns.cnt) {
          MenuItemImpl overflowMenuItem = (MenuItemImpl) menu.getItem(mInvisibleBtns.originPos[currentPos]);

          if (overflowMenuItem != null) {
            // Get tooltip text from the title or content description
            CharSequence toolTipText = (overflowMenuItem.getTitle() != null)
                    ? overflowMenuItem.getTitle()
                    : overflowMenuItem.getContentDescription();

            // Add the overflow menu item to the menu and configure visibility and enabled status
            mOverflowMenu
                    .add(overflowMenuItem.getGroupId(), overflowMenuItem.getItemId(), overflowMenuItem.getOrder(), toolTipText)
                    .setVisible(overflowMenuItem.isVisible())
                    .setEnabled(overflowMenuItem.isEnabled());

            mOverflowMenu.setGroupDividerEnabled(mHasGroupDivider);

            // Update badge text if necessary
            overflowMenuItem.setBadgeText(overflowMenuItem.getBadgeText());

            // Count invisible items
            if (!overflowMenuItem.isVisible()) {
              invisibleItemCount++;
            }
          }

          currentPos++;
        }

        if (mInvisibleBtns.cnt - invisibleItemCount > 0) {
          mOverflowButton = ensureOverflowButton(isShifting);
          buttons[mVisibleBtns.cnt] = mOverflowButton;
          ++mVisibleItemCount;
          ++mViewVisibleItemCount;
          mOverflowButton.setVisibility(View.VISIBLE);
        }
      }


      if (mViewVisibleItemCount > mMaxItemCount) {
        Log.i(TAG, "Maximum number of visible items supported by BottomNavigationView is " + mMaxItemCount
                + ". Current visible count is " + mViewVisibleItemCount);
        mVisibleItemCount = mMaxItemCount;
        mViewVisibleItemCount = mMaxItemCount;
      }

      for (NavigationBarItemView button : buttons) {
        setShowButtonShape(button);
      }

      // Adjust `selectedItemPosition` if necessary
      selectedItemPosition = Math.min(mMaxItemCount - 1, selectedItemPosition);
      menu.getItem(selectedItemPosition).setChecked(true);

    }
  }


  public void updateMenuView() {

    if (menu == null || buttons == null || mVisibleBtns == null || mInvisibleBtns == null) {
      return;
    }

    int menuSize = menu.size();
    this.hideOverflowMenu();
    if (menuSize != this.mVisibleBtns.cnt + this.mInvisibleBtns.cnt) {
      this.buildMenuView();
      return;
    }

    int previousSelectedItemId = selectedItemId;

    for(int i = 0; i < mVisibleBtns.cnt; ++i) {
      MenuItem menuItem = menu.getItem(mVisibleBtns.originPos[i]);
      if (menuItem.isChecked()) {
        selectedItemId = menuItem.getItemId();
        selectedItemPosition = i;
      }

      if (menuItem instanceof SeslMenuItem) {
        SeslMenuItem seslMenuItem = (SeslMenuItem)menuItem;
        this.seslRemoveBadge(menuItem.getItemId());
        if (seslMenuItem.getBadgeText() != null) {
          this.seslAddBadge(seslMenuItem.getBadgeText(), menuItem.getItemId());
        }
      }
    }

    if (previousSelectedItemId != this.selectedItemId) {
      TransitionManager.beginDelayedTransition(this, this.set);
    }

    boolean isShifting = isShifting(labelVisibilityMode, menu.getVisibleItems().size());

    for(int i = 0; i < mVisibleBtns.cnt; i++) {
      this.presenter.setUpdateSuspended(true);
      this.buttons[i].setLabelVisibilityMode(labelVisibilityMode);
      this.buttons[i].setShifting(isShifting);
      this.buttons[i].initialize((MenuItemImpl)menu.getItem(mVisibleBtns.originPos[i]), 0);
      this.presenter.setUpdateSuspended(false);
    }

    boolean hasTextBadge = false;
    for(int i = 0; i < mInvisibleBtns.cnt; i++) {
      MenuItem menuItem = menu.getItem(mInvisibleBtns.originPos[i]);

      if (menuItem instanceof SeslMenuItem) {
        MenuBuilder overflowMenu = mOverflowMenu;

        if (overflowMenu != null) {
          SeslMenuItem seslMenuItem = (SeslMenuItem)menuItem;
          MenuItem overflowMenuItem = overflowMenu.findItem(menuItem.getItemId());
          if (overflowMenuItem instanceof SeslMenuItem) {
            overflowMenuItem.setTitle(menuItem.getTitle());
            ((SeslMenuItem)overflowMenuItem).setBadgeText(seslMenuItem.getBadgeText());
          }

          hasTextBadge |= (seslMenuItem.getBadgeText() != null);

        }
      }
    }

    if (hasTextBadge) {
      this.seslAddBadge("", R.id.bottom_overflow);
    } else {
      this.seslRemoveBadge(R.id.bottom_overflow);
    }
  }
  //sesl

  private NavigationBarItemView getNewItem() {
    NavigationBarItemView item = itemPool.acquire();
    if (item == null) {
      item = createNavigationBarItemView(getContext());
    }
    return item;
  }

  public int getSelectedItemId() {
    return selectedItemId;
  }

  protected boolean isShifting(
      @NavigationBarView.LabelVisibility int labelVisibilityMode, int childCount) {
    return labelVisibilityMode == NavigationBarView.LABEL_VISIBILITY_SELECTED;//sesl
  }

  void tryRestoreSelectedItemId(int itemId) {
    final int size = menu.size();
    for (int i = 0; i < size; i++) {
      MenuItem item = menu.getItem(i);
      if (itemId == item.getItemId()) {
        selectedItemId = itemId;
        selectedItemPosition = i;
        item.setChecked(true);
        break;
      }
    }
  }

  SparseArray<BadgeDrawable> getBadgeDrawables() {
    return badgeDrawables;
  }

  void restoreBadgeDrawables(SparseArray<BadgeDrawable> badgeDrawables) {
    for (int i = 0; i < badgeDrawables.size(); i++) {
      int key = badgeDrawables.keyAt(i);
      if (this.badgeDrawables.indexOfKey(key) < 0) {
        // badge doesn't exist yet, restore it
        this.badgeDrawables.append(key, badgeDrawables.get(key));
      }
    }
    if (buttons != null) {
      for (NavigationBarItemView itemView : buttons) {
        if (itemView != null){//sesl
          BadgeDrawable badge = this.badgeDrawables.get(itemView.getId());
          itemView.setBadge(badge);
        }
      }
    }
  }

  @Nullable
  public BadgeDrawable getBadge(int menuItemId) {
    return badgeDrawables.get(menuItemId);
  }

  /**
   * Creates an instance of {@link BadgeDrawable} if none exists. Initializes (if needed) and
   * returns the associated instance of {@link BadgeDrawable}.
   *
   * @param menuItemId Id of the menu item.
   * @return an instance of BadgeDrawable associated with {@code menuItemId}.
   */
  BadgeDrawable getOrCreateBadge(int menuItemId) {
    validateMenuItemId(menuItemId);
    BadgeDrawable badgeDrawable = badgeDrawables.get(menuItemId);
    // Create an instance of BadgeDrawable if none were already initialized for this menu item.
    if (badgeDrawable == null) {
      badgeDrawable = BadgeDrawable.create(getContext());
      badgeDrawables.put(menuItemId, badgeDrawable);
    }
    NavigationBarItemView itemView = findItemView(menuItemId);
    if (itemView != null) {
      itemView.setBadge(badgeDrawable);
    }
    return badgeDrawable;
  }

  void removeBadge(int menuItemId) {
    validateMenuItemId(menuItemId);
    BadgeDrawable badgeDrawable = badgeDrawables.get(menuItemId);//sesl
    NavigationBarItemView itemView = findItemView(menuItemId);
    if (itemView != null) {
      itemView.removeBadge();
    }
    if (badgeDrawable != null) {//sesl
      badgeDrawables.remove(menuItemId);//sesl
    }
  }

  private void setBadgeIfNeeded(@NonNull NavigationBarItemView child) {
    int childId = child.getId();
    if (!isValidId(childId)) {
      // Child doesn't have a valid id, do not set any BadgeDrawable on the view.
      return;
    }

    BadgeDrawable badgeDrawable = badgeDrawables.get(childId);
    if (badgeDrawable != null) {
      child.setBadge(badgeDrawable);
    }
  }

  private void removeUnusedBadges() {
    HashSet<Integer> activeKeys = new HashSet<>();
    // Remove keys from badgeDrawables that don't have a corresponding value in the menu.
    for (int i = 0; i < menu.size(); i++) {
      activeKeys.add(menu.getItem(i).getItemId());
    }

    for (int i = 0; i < badgeDrawables.size(); i++) {
      int key = badgeDrawables.keyAt(i);
      if (!activeKeys.contains(key)) {
        badgeDrawables.delete(key);
      }
    }
  }

  @Nullable
  public NavigationBarItemView findItemView(int menuItemId) {
    validateMenuItemId(menuItemId);
    if (buttons != null) {
      for (NavigationBarItemView itemView : buttons) {
        if (itemView == null) return null;//sesl
        if (itemView.getId() == menuItemId) {
          return itemView;
        }
      }
    }
    return null;
  }

  /** Returns reference to newly created {@link NavigationBarItemView}. */
  @NonNull
  protected abstract NavigationBarItemView createNavigationBarItemView(@NonNull Context context);

  protected int getSelectedItemPosition() {
    return selectedItemPosition;
  }

  @Nullable
  protected MenuBuilder getMenu() {
    return menu;
  }

  private boolean isValidId(int viewId) {
    return viewId != View.NO_ID;
  }

  private void validateMenuItemId(int viewId) {
    if (!isValidId(viewId)) {
      throw new IllegalArgumentException(viewId + " is not a valid view id");
    }
  }

  //Sesl
  protected void setOverflowSelectedCallback(MenuBuilder.Callback callback) {
    mSelectedCallback = callback;
  }

  boolean hasOverflowButton() {
    return mHasOverflowMenu;
  }

  MenuBuilder getOverflowMenu() {
    return mOverflowMenu;
  }

  void showOverflowMenu() {
    if (hasOverflowButton() && presenter != null) {
      presenter.showOverflowMenu(mOverflowMenu);
    }
  }

  void hideOverflowMenu() {
    if (hasOverflowButton() && presenter != null) {
      if (presenter.isOverflowMenuShowing()) {
        presenter.hideOverflowMenu();
      }
    }
  }

  void setMaxItemCount(int maxItemCount) {
    mMaxItemCount = maxItemCount;
  }

  private void setShowButtonShape(NavigationBarItemView itemView) {
    if (itemView != null) {
      ColorStateList itemTextColor = getItemTextColor();
      if (isShowButtonShapesEnabled()) {
        if (Build.VERSION.SDK_INT > 26) {
          final int color;
          if (mSBBTextColorDrawable != null) {
            color = mSBBTextColorDrawable.getColor();
          } else {
            color = getResources()
                .getColor(SeslMisc.isLightTheme(getContext()) ?
                    R.color.sesl_bottom_navigation_background_light
                    : R.color.sesl_bottom_navigation_background_dark, null);
          }
          itemView.setShowButtonShape(color, itemTextColor);

          if (mOverflowButton != null) {
            MenuItemImpl item = itemView.getItemData();
            if (item != null && mDummyMenu != null) {
              if (item.getItemId() == mDummyMenu.getItem(0).getItemId()) {
                setOverflowSpanColor(color, false);
              }
            }
          }
        } else {
          itemView.setShowButtonShape(0, itemTextColor);
        }
      }
    }
  }

  //Sesl7
  private NavigationBarItemView buildMenu(@NonNull MenuItemImpl menuItemImpl, boolean isShifting) {

    NavigationBarItemView item = getNewItem(menuItemImpl);

    item.setIconTintList(itemIconTint);
    item.setIconSize(itemIconSize);
    item.setTextColor(itemTextColorDefault);
    item.seslSetLabelTextAppearance(mSeslLabelTextAppearance);
    item.setTextAppearanceInactive(itemTextAppearanceInactive);
    item.setTextAppearanceActive(itemTextAppearanceActive);
    item.setTextColor(itemTextColorFromUser);

    if (itemBackground != null) {
      item.setItemBackground(itemBackground);
    } else {
      item.setItemBackground(itemBackgroundRes);
    }

    inflateStateListAnimator(item);

    item.setShifting(isShifting);
    item.setLabelVisibilityMode(this.labelVisibilityMode);
    item.initialize(menuItemImpl, 0);
    item.setItemPosition(this.mVisibleItemCount);

    return item;
  }

  private NavigationBarItemView getNewItem(@NonNull final MenuItemImpl menuItemImpl) {
    NavigationBarItemView item = itemPool.acquire();
    if (item != null) return item;

    final int viewType = getViewType();
    return new NavigationBarItemView(getContext(), viewType) {
      public int getItemLayoutResId() {
        if (menuItemImpl.getSeslNaviMenuItemType() == SESL_MENU_ITEM_TYPE_CHECKBOX) {
          return R.layout.sesl_bottom_navigation_item_checkbox;
        }
        switch (viewType) {
          case NavigationBarView.SESL_TYPE_LABEL_ONLY:
            return R.layout.sesl_bottom_navigation_item_text;
          //case NavigationBarView.SESL_TYPE_ICON_LABEL:
          //case NavigationBarView.SESL_TYPE_ICON_ONLY:
          default:
            return R.layout.sesl_bottom_navigation_item;
        }
        }

      @Override
      public void initialize(@NonNull MenuItemImpl item, int menuType) {
        super.initialize(item, menuType);
        item.setExclusiveCheckable(mExclusiveCheckable);
      }
    };
  }

  private void buildInternalMenu(boolean shifting, int index) {
    if (buttons != null) {

      if (index < 0 || index > menu.size() || !(menu.getItem(index) instanceof MenuItemImpl)) {
        String logMsg = "index" + "position is out of index (pos=" +
            "/size=" + menu.size() + ") or not instance of MenuItemImpl";
        Log.e(TAG, logMsg);
        return;
      }

      MenuItemImpl item = (MenuItemImpl) menu.getItem(index);
      NavigationBarItemView child = buildMenu(item, shifting);
      buttons[mVisibleItemCount] = child;
      child.setVisibility(menu.getItem(index).isVisible() ? View.VISIBLE : View.GONE);
      child.setOnClickListener(onClickListener);

      String badgeText = item.getBadgeText();
      if (badgeText != null) {
        seslAddBadge(badgeText, item.getItemId());
      } else {
        seslRemoveBadge(item.getItemId());
      }
      setBadgeIfNeeded(child);

      if (child.getParent() instanceof ViewGroup) {
        ((ViewGroup) child.getParent()).removeView(child);
      }
      addView(child);

      mVisibleItemCount++;
      if (child.getVisibility() == View.VISIBLE) {
        mViewVisibleItemCount++;
      }
    }
  }
 //sesl7

  private NavigationBarItemView ensureOverflowButton(boolean shifting) {
    mHasOverflowMenu = true;
    mDummyMenu = new MenuBuilder(getContext());

    MenuInflater inflater = new MenuInflater(getContext());
    inflater.inflate(R.menu.nv_dummy_overflow_menu_icon, mDummyMenu);

    if (mDummyMenu.size() <= 0 || !(mDummyMenu.getItem(0) instanceof MenuItemImpl)) {
      return null;
    }

    MenuItemImpl item = (MenuItemImpl) mDummyMenu.getItem(0);
    if (getViewType() == SESL_TYPE_ICON_LABEL) {
      item.setTooltipText(null);
    } else {
      item.setTooltipText(getResources().getString(R.string.sesl_more_item_label));
    }
    NavigationBarItemView child = buildMenu(item, shifting);
    inflateStateListAnimator(child);
    child.setBadgeType(BADGE_TYPE_OVERFLOW);
    child.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mOverflowMenu.setCallback(mSelectedCallback);
        presenter.showOverflowMenu(mOverflowMenu);
      }
    });
    child.setContentDescription(getResources()
        .getString(R.string.sesl_action_menu_overflow_description));
    if (getViewType() == NavigationBarView.SESL_TYPE_LABEL_ONLY) {
      initOverflowSpan(child);
    }
    if (child.getParent() instanceof ViewGroup) {
      ((ViewGroup) child.getParent()).removeView(child);
    }
    addView(child);
    return child;
  }



  protected void updateBadgeIfNeeded() {
    if (buttons != null) {
      for (NavigationBarItemView itemView : buttons) {
        if (itemView != null) {
          updateBadge(itemView);
        } else {
          return;
        }
      }
    }
  }


  private void updateBadge(NavigationBarItemView itemView) {
    if (itemView != null) {

      TextView badgeView = itemView.findViewById(R.id.notifications_badge);

      if (badgeView != null) {

        int badgeType = itemView.getBadgeType();

        int itemLabelWidth;
        int itemLabelHeight;
        TextView itemLabel = itemView.getLabel();
        if (itemLabel == null) {
          itemLabelWidth = 1;
          itemLabelHeight = 1;
        } else {
          itemLabelWidth = itemLabel.getWidth();
          itemLabelHeight = itemLabel.getHeight();
        }

        int badgeHeight;
        int badgeWidth;
        if (badgeType == BADGE_TYPE_DOT || badgeType == BADGE_TYPE_OVERFLOW /*BADGE_TYPE_OVERFLOW == BADGE_TYPE_DOT in sesl6*/) {
          if (badgeView.getBackground() != dotBadgeBackground) {
            badgeView.setBackground(dotBadgeBackground);
          }
          badgeHeight = dotBadgeSize;
          badgeWidth = dotBadgeSize;
        } else {
          if (badgeView.getBackground() != nBadgeBackground) {
            badgeView.setBackground(nBadgeBackground);
          }
          badgeView.measure(0, 0);
          badgeWidth = badgeView.getMeasuredWidth();
          badgeHeight = badgeView.getMeasuredHeight();
        }

        int badgeViewMeasureWidth = badgeView.getMeasuredWidth();
        int itemViewWidth = itemView.getWidth();

        int marginStart;
        int topMargin;
        if (getViewType() != SESL_TYPE_LABEL_ONLY) {
          if (badgeType == BADGE_TYPE_DOT) {
            marginStart = getItemIconSize() / 2;
            topMargin = dotBadgeSize;
          } else {
            int horizontalPadding = (this.mVisibleItemCount == this.mMaxItemCount)
                ? iconModeMinPaddingHorizontal
                : iconModePaddingHorizontal;
            marginStart = badgeViewMeasureWidth / 2 - horizontalPadding;
            topMargin = dotBadgeSize / 2;
          }
        } else {
          switch (badgeType) {
            case BADGE_TYPE_DOT:
              marginStart = (itemLabelWidth + badgeViewMeasureWidth) / 2;
              topMargin = (itemView.getHeight() - itemLabelHeight) / 2;
              break;
            case BADGE_TYPE_OVERFLOW:
              marginStart = ((itemLabelWidth - badgeViewMeasureWidth) - nBadgeStartMargin) / 2;
              topMargin = ((itemView.getHeight() - itemLabelHeight) / 2) - nBadgeTopMargin;
              break;
            default:
              marginStart = (itemLabelWidth + badgeViewMeasureWidth) / 2;
              topMargin = ((itemView.getHeight() - itemLabelHeight) / 2) - nBadgeTopMargin;
              if ((itemViewWidth/2 + marginStart + (badgeViewMeasureWidth/2)) > itemViewWidth) {
                marginStart += (itemViewWidth - (itemViewWidth / 2 + marginStart +  badgeViewMeasureWidth / 2));
              }
              break;
          }
        }

        MarginLayoutParams lp = (MarginLayoutParams) badgeView.getLayoutParams();
        if (lp.width != badgeWidth || lp.leftMargin != marginStart) {
          lp.width = badgeWidth;
          lp.height = badgeHeight;
          lp.topMargin = topMargin;
          lp.setMarginStart(marginStart);
          badgeView.setLayoutParams(lp);
        }
      }
    }
  }

  void seslAddBadge(String text, int menuItemId) {
    TextView badgeTextView;

    NavigationBarItemView itemView = findItemView(menuItemId);
    if (itemView != null) {
      View badgeContainer = itemView.findViewById(R.id.notifications_badge_container);
      if (badgeContainer != null) {
        badgeTextView = badgeContainer.findViewById(R.id.notifications_badge);
      } else {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        badgeContainer = inflater.inflate(R.layout.sesl_navigation_bar_badge_layout,
            this, false);
        itemView.addView(badgeContainer);
        badgeTextView = badgeContainer.findViewById(R.id.notifications_badge);
      }

      if (!isNumericValue(text)) {
        itemView.setBadgeNumberless(false);
      } else if (Integer.parseInt(text) > 999) {
        itemView.setBadgeNumberless(true);
        text = "999+";
      } else {
        itemView.setBadgeNumberless(false);
      }
    } else {
      badgeTextView = null;
    }

    if (badgeTextView != null) {
      badgeTextView.setText(text);
    }

    updateBadge(itemView);
  }

  void seslRemoveBadge(int menuItemId) {
    NavigationBarItemView itemView = findItemView(menuItemId);
    if (itemView != null) {
      View badgeContainer = itemView.findViewById(R.id.notifications_badge_container);
      if (badgeContainer != null) {
        itemView.removeView(badgeContainer);
      }
    }
  }

  private boolean isNumericValue(String value) {
    if (value == null) {
      return false;
    }
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (getViewType() != NavigationBarView.SESL_TYPE_LABEL_ONLY) {
      setItemIconSize(getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size));
      if (buttons != null) {
        for (NavigationBarItemView itemView : buttons) {
          if (itemView == null) {
            break;
          }
          itemView.updateLabelGroupTopMargin(
              getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size));
        }
      }
    }
    hideOverflowMenu();
  }

  class InternalBtnInfo {
    int cnt = 0;
    int[] originPos;

    InternalBtnInfo(int size) {
      originPos = new int[size];
    }
  }

  @RestrictTo(LIBRARY)
  public int getVisibleItemCount() {
    return mVisibleItemCount;
  }

  @RestrictTo(LIBRARY)
  public int getViewVisibleItemCount() {
    return mViewVisibleItemCount;
  }

  private boolean isShowButtonShapesEnabled() {
    return Settings.Global.getInt(mContentResolver, "show_button_background", 0) == 1;
  }

  @RestrictTo(LIBRARY_GROUP_PREFIX)
  public void setBackgroundColorDrawable(ColorDrawable d) {
    mSBBTextColorDrawable = d;
  }

  @RestrictTo(LIBRARY_GROUP_PREFIX)
  public ColorDrawable getBackgroundColorDrawable() {
    return mSBBTextColorDrawable;
  }

  void setViewType(int viewType) {
    mViewType = viewType;
  }

  @RestrictTo(LIBRARY)
  public int getViewType() {
    return mViewType;
  }

  private void setOverflowSpanColor(int color, boolean fromUser) {
    if (mOverflowButton != null) {
      SpannableStringBuilder labelImageSpan = mOverflowButton.getLabelImageSpan();
      if (labelImageSpan != null) {
        Drawable d = getContext().getDrawable(R.drawable.sesl_ic_menu_overflow_dark);

        ImageSpan[] spans = labelImageSpan
            .getSpans(0, labelImageSpan.length(), ImageSpan.class);
        if (spans != null) {
          for (ImageSpan span : spans) {
            labelImageSpan.removeSpan(span);
          }
        }

        ImageSpan imageSpan = new ImageSpan(d);
        d.setState(
            new int[]{
                android.R.attr.state_enabled,
                -android.R.attr.state_enabled
            }
        );
        if (fromUser) {
          d.setTintList(itemTextColorFromUser);
        } else {
          d.setTint(color);
        }
        d.setBounds(0, 0,
            getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size),
            getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size));
        labelImageSpan.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        mOverflowButton.setLabelImageSpan(labelImageSpan);
      }
    }
  }

  private void initOverflowSpan(NavigationBarItemView itemView) {
    Drawable d = getContext().getDrawable(R.drawable.sesl_ic_menu_overflow_dark);

    SpannableStringBuilder span = new SpannableStringBuilder(" ");
    ImageSpan imageSpan = new ImageSpan(d);
    d.setState(
        new int[]{
            android.R.attr.state_enabled,
            -android.R.attr.state_enabled
        }
    );
    d.setTintList(itemTextColorFromUser);
    d.setBounds(0, 0,
        getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size),
        getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_icon_size));
    span.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

    itemView.setLabelImageSpan(span);
  }

  public void seslSetLabelTextAppearance(@StyleRes int textAppearanceRes) {
    mSeslLabelTextAppearance = textAppearanceRes;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) {
          break;
        }
        item.setTextAppearanceInactive(textAppearanceRes);
        if (itemTextColorFromUser != null) {
          item.setTextColor(itemTextColorFromUser);
        }
      }
    }
    if (mOverflowButton != null) {
      mOverflowButton.setTextAppearanceInactive(textAppearanceRes);
      if (itemTextColorFromUser != null) {
        mOverflowButton.setTextColor(itemTextColorFromUser);
      }
    }
  }

  @StyleRes
  public int seslGetLabelTextAppearance() {
    return mSeslLabelTextAppearance;
  }

  void setGroupDividerEnabled(boolean enabled) {
    mHasGroupDivider = enabled;
    if (mOverflowMenu != null) {
      mOverflowMenu.setGroupDividerEnabled(enabled);
    } else {
      updateMenuView();
    }
  }
  //sesl

  //Sesl7
  private void inflateStateListAnimator(NavigationBarItemView itemView) {
    if (itemStateListAnimatorId != 0) {
      itemView.setStateListAnimator(AnimatorInflater.loadStateListAnimator(
          getContext(), itemStateListAnimatorId));
    }
  }

  public void setItemStateListAnimator(int stateListAnimator) {
    this.itemStateListAnimatorId = stateListAnimator;
    if (buttons != null) {
      for (NavigationBarItemView item : buttons) {
        if (item == null) {
          break;
        }
        inflateStateListAnimator(item);
      }
    }
    if (mOverflowButton != null) {
      inflateStateListAnimator(mOverflowButton);
    }
  }

  @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP_PREFIX})
  public void setExclusiveCheckable(boolean exclusiveCheckable) {
    this.mExclusiveCheckable = exclusiveCheckable;
  }

  //sesl7
}
