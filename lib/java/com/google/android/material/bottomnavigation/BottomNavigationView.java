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
import static androidx.core.view.SemBlurCompat.BLUR_MODE_CANVAS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.oneui.common.BlurSupportable;
import androidx.appcompat.oneui.common.internal.policy.BlurInfoState;
import androidx.appcompat.oneui.common.internal.semblurinfo.SemBlurInfoStateBuilder;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.oneui.common.internal.semblurinfo.SemBlurInfoState;
import androidx.core.view.SemBlurCompat;
import androidx.core.view.SeslTouchTargetDelegate;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.internal.ViewUtils.RelativePadding;
import com.google.android.material.navigation.NavigationBarMenuView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.strategy.StrategyFactory;
import com.google.android.material.navigation.strategy.ViewTypeStrategy;

/**
 * <b>SESL variant</b><br><br>
 *
 * Represents a standard bottom navigation bar for application. It is an implementation of <a
 * href="https://m3.material.io/components/navigation-bar/overview">material design bottom
 * navigation</a>.
 *
 * <p>Bottom navigation bars make it easy for users to explore and switch between top-level views in
 * a single tap. They should be used when an application has three to five top-level destinations.
 *
 * <p>The bar can disappear on scroll, based on {@link
 * com.google.android.material.behavior.HideBottomViewOnScrollBehavior}, when it is placed within a
 * {@link CoordinatorLayout} and one of the children within the {@link CoordinatorLayout} is
 * scrolled. This behavior is only set if the {@code layout_behavior} property is set to {@link
 * HideBottomViewOnScrollBehavior}.
 *
 * <p>The bar contents can be populated by specifying a menu resource file. Each menu item title,
 * icon and enabled state will be used for displaying bottom navigation bar items. Menu items can
 * also be used for programmatically selecting which destination is currently active. It can be done
 * using {@code MenuItem#setChecked(true)}
 *
 * <pre>
 * layout resource file:
 * &lt;com.google.android.material.bottomnavigation.BottomNavigationView
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schema.android.com/apk/res/res-auto"
 *     android:id="@+id/navigation"
 *     android:layout_width="match_parent"
 *     android:layout_height="56dp"
 *     android:layout_gravity="start"
 *     app:menu="@menu/my_navigation_items" /&gt;
 *
 * res/menu/my_navigation_items.xml:
 * &lt;menu xmlns:android="http://schemas.android.com/apk/res/android"&gt;
 *     &lt;item android:id="@+id/action_search"
 *          android:title="@string/menu_search"
 *          android:icon="@drawable/ic_search" /&gt;
 *     &lt;item android:id="@+id/action_settings"
 *          android:title="@string/menu_settings"
 *          android:icon="@drawable/ic_add" /&gt;
 *     &lt;item android:id="@+id/action_navigation"
 *          android:title="@string/menu_navigation"
 *          android:icon="@drawable/ic_action_navigation_menu" /&gt;
 * &lt;/menu&gt;
 * </pre>
 *
 * <p>For more information, see the <a
 * href="https://github.com/material-components/material-components-android/blob/master/docs/components/BottomNavigation.md">component
 * developer guidance</a> and <a
 * href="https://material.io/components/navigation-bar/overview">design guidelines</a>.
 */
public class BottomNavigationView extends NavigationBarView implements BlurSupportable/*sesl9*/{
  static final int MAX_ITEM_COUNT = 5/*sesl*/;

  //Sesl
  private Drawable mBackgroundDrawable;
  private SemBlurInfoState mBlurInfo;
  @SemBlurCompat.SeslBlurMode
  private int mBlurMode = BLUR_MODE_CANVAS;
  @NavigationBarView.LabelVisibility
  private int mSavedLabelVisibilityMode = LABEL_VISIBILITY_AUTO;
  private boolean mHasSavedLabelVisibilityMode = false;
  boolean mIsFloatingStyle;
  private boolean mIsSmallScreen;
  private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListenerForTD;
  //sesl

  public BottomNavigationView(@NonNull Context context) {
    this(context, null);
  }

  public BottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.bottomNavigationStyle);
  }

  public BottomNavigationView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, R.style.Widget_Design_BottomNavigationView);
  }

  public BottomNavigationView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();

    /* Custom attributes */
    TintTypedArray attributes =
        ThemeEnforcement.obtainTintedStyledAttributes(
            context, attrs, R.styleable.BottomNavigationView, defStyleAttr, defStyleRes);

    setItemHorizontalTranslationEnabled(
        attributes.getBoolean(
            R.styleable.BottomNavigationView_itemHorizontalTranslationEnabled, true));

    //Sesl
    boolean isWrapContent =
        attributes.getBoolean(
            R.styleable.BottomNavigationView_seslMenuViewWrapContent, false);

    if (attributes.getBoolean(
        R.styleable.BottomNavigationView_seslBottomBarApplyBlur, false)) {
      applyBlurInfo(context);
    }

    SeslMisc.isOverlayThemeApplied(context);

    mIsFloatingStyle = isWrapContent;

    if (getMenuView() instanceof BottomNavigationMenuView bottomNavMenu) {
      bottomNavMenu.isWrapContent = isWrapContent;
      bottomNavMenu.setViewTypeChangeListener(this::updateStrategy);
      updateStrategy(bottomNavMenu.getViewType());
    }
    //sesl

//    if (attributes.hasValue(R.styleable.BottomNavigationView_android_minHeight)) {
//      setMinimumHeight(
//          attributes.getDimensionPixelSize(R.styleable.BottomNavigationView_android_minHeight, 0));
//    }

    attributes.recycle();

//    // Not applied in sesl
//    applyWindowInsets();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(@NonNull MotionEvent event) {
    super.onTouchEvent(event);
    // Consume all events to avoid views under the BottomNavigationView from receiving touch events.
    return true;
  }

  private void applyWindowInsets() {
    ViewUtils.doOnApplyWindowInsets(
        this,
        new ViewUtils.OnApplyWindowInsetsListener() {
          @NonNull
          @Override
          public WindowInsetsCompat onApplyWindowInsets(
              View view,
              @NonNull WindowInsetsCompat insets,
              @NonNull RelativePadding initialPadding) {
            // Apply the bottom, start, and end padding for a BottomNavigationView
            // to dodge the system navigation bar
            initialPadding.bottom += insets.getSystemWindowInsetBottom();

            boolean isRtl = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            int systemWindowInsetLeft = insets.getSystemWindowInsetLeft();
            int systemWindowInsetRight = insets.getSystemWindowInsetRight();
            initialPadding.start += isRtl ? systemWindowInsetRight : systemWindowInsetLeft;
            initialPadding.end += isRtl ? systemWindowInsetLeft : systemWindowInsetRight;
            initialPadding.applyToView(view);
            return insets;
          }
        });
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    updateSmallScreenMode();//sesl
    int minHeightSpec = makeMinHeightSpec(heightMeasureSpec);
    super.onMeasure(widthMeasureSpec, minHeightSpec);
    //sesl
    /*if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
      setMeasuredDimension(
          getMeasuredWidth(),
          Math.max(
              getMeasuredHeight(),
              getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom()));
    }*/
  }

  private int makeMinHeightSpec(int measureSpec) {
    int minHeight = getSuggestedMinimumHeight();
    /*if (MeasureSpec.getMode(measureSpec) != MeasureSpec.EXACTLY && minHeight > 0) {
      minHeight += getPaddingTop() + getPaddingBottom();

      return MeasureSpec.makeMeasureSpec(
          Math.max(MeasureSpec.getSize(measureSpec), minHeight), MeasureSpec.AT_MOST);
    }

    return measureSpec;*/
    //sesl
    return MeasureSpec.makeMeasureSpec(
        getPaddingBottom() + getPaddingTop() + minHeight,
        MeasureSpec.EXACTLY);
  }

  /**
   * Sets whether the menu items horizontally translate on selection when the combined item widths
   * fill up the screen.
   *
   * @param itemHorizontalTranslationEnabled whether the items horizontally translate on selection
   * @see #isItemHorizontalTranslationEnabled()
   */
  public void setItemHorizontalTranslationEnabled(boolean itemHorizontalTranslationEnabled) {
    BottomNavigationMenuView menuView = (BottomNavigationMenuView) getMenuView();
    if (menuView.isItemHorizontalTranslationEnabled() != itemHorizontalTranslationEnabled) {
      menuView.setItemHorizontalTranslationEnabled(itemHorizontalTranslationEnabled);
      getPresenter().updateMenuView(false);
    }
  }

  /**
   * Returns whether the items horizontally translate on selection when the item widths fill up the
   * screen.
   *
   * @return whether the menu items horizontally translate on selection
   * @see #setItemHorizontalTranslationEnabled(boolean)
   */
  public boolean isItemHorizontalTranslationEnabled() {
    return ((BottomNavigationMenuView) getMenuView()).isItemHorizontalTranslationEnabled();
  }

  @Override
  public int getMaxItemCount() {
    return MAX_ITEM_COUNT;
  }

  /** @hide */
  @RestrictTo(LIBRARY_GROUP)
  @Override
  @NonNull
  protected NavigationBarMenuView createNavigationBarMenuView(@NonNull Context context) {
    return new BottomNavigationMenuView(context);
  }

  /**
   * Set a listener that will be notified when a bottom navigation item is selected. This listener
   * will also be notified when the currently selected item is reselected, unless an {@link
   * OnNavigationItemReselectedListener} has also been set.
   *
   * @param listener The listener to notify
   * @see #setOnNavigationItemReselectedListener(OnNavigationItemReselectedListener)
   * @deprecated Use {@link NavigationBarView#setOnItemSelectedListener(OnItemSelectedListener)}
   *     instead.
   */
  @Deprecated
  public void setOnNavigationItemSelectedListener(
      @Nullable OnNavigationItemSelectedListener listener) {
    setOnItemSelectedListener(listener);
  }

  /**
   * Set a listener that will be notified when the currently selected bottom navigation item is
   * reselected. This does not require an {@link OnNavigationItemSelectedListener} to be set.
   *
   * @param listener The listener to notify
   * @see #setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener)
   * @deprecated Use {@link NavigationBarView#setOnItemReselectedListener(OnItemReselectedListener)}
   *     instead.
   */
  @Deprecated
  public void setOnNavigationItemReselectedListener(
      @Nullable OnNavigationItemReselectedListener listener) {
    setOnItemReselectedListener(listener);
  }

  /**
   * Listener for handling selection events on bottom navigation items.
   *
   * @deprecated Use {@link NavigationBarView.OnItemSelectedListener} instead.
   */
  @Deprecated
  public interface OnNavigationItemSelectedListener extends OnItemSelectedListener {}

  /**
   * Listener for handling reselection events on bottom navigation items.
   *
   * @deprecated Use {@link NavigationBarView.OnItemReselectedListener} instead.
   */
  @Deprecated
  public interface OnNavigationItemReselectedListener extends OnItemReselectedListener {}

  //Sesl9
  private void applySmallScreenOuterPadding() {
    MenuView menuView = getMenuView();
    if (menuView instanceof BottomNavigationMenuView bottomNavMenu) {
      int visibleCount = 0;
      for (int i = 0; i < bottomNavMenu.getChildCount(); i++) {
        if (bottomNavMenu.getChildAt(i).getVisibility() == VISIBLE) {
          visibleCount++;
        }
      }
      int padding =
          StrategyFactory.INSTANCE.createStrategy(bottomNavMenu.getViewType(), this.mIsFloatingStyle)
              .getSmallScreenOuterPadding(getResources(), visibleCount);
      if (getPaddingLeft() == padding && getPaddingRight() == padding) {
        return;
      }
      setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
    }
  }

  private SemBlurInfoState generateBlurInfo(Context context) {
    float radius = context.getResources().getDimension(R.dimen.sesl_bottom_navigation_icon_only_mode_background_radius);
    SemBlurInfoStateBuilder builder =
        BlurInfoState.INSTANCE.generateFloatingComponentBlurInfoStateBuilder(context, this.mBlurMode);
    if (mBackgroundDrawable != null) {
      builder.nonBlurBackground(mBackgroundDrawable);
    }
    return builder.cornerRadius(radius).build();
  }

  private boolean isSmallScreen() {
    return getResources().getConfiguration().screenHeightDp <= 442
        && getResources().getConfiguration().screenWidthDp <= 400;
  }


  private void restoreDefaultByViewType() {
    MenuView menuView = getMenuView();
    if (menuView instanceof BottomNavigationMenuView) {
      int viewType = ((BottomNavigationMenuView) menuView).getViewType();
      if (viewType == 2) {
        setLabelVisibilityMode(2);
      } else if (viewType == 3) {
        setLabelVisibilityMode(1);
      } else {
        setLabelVisibilityMode(1);
      }
      updateStrategy(viewType);
    }
  }

  private boolean shouldApplySmallScreenMode() {
    if (!isSmallScreen() || !this.mIsFloatingStyle) {
      return false;
    }
    MenuView menuView = getMenuView();
    if (!(menuView instanceof BottomNavigationMenuView)) {
      return false;
    }
    int viewType = ((BottomNavigationMenuView) menuView).getViewType();
    return viewType == 1 || viewType == 2;
  }

  private void updateSmallScreenMode() {
    boolean shouldApply = shouldApplySmallScreenMode();
    MenuView menuView = getMenuView();
    if (this.mIsSmallScreen == shouldApply) {
      if (shouldApply && (menuView instanceof BottomNavigationMenuView)) {
        ((BottomNavigationMenuView) menuView).setSmallScreenMode(true);
        applySmallScreenOuterPadding();
        return;
      }
      return;
    }
    this.mIsSmallScreen = shouldApply;
    boolean isMenuView = menuView instanceof BottomNavigationMenuView;
    if (isMenuView) {
      ((BottomNavigationMenuView) menuView).setSmallScreenMode(shouldApply);
    }
    if (shouldApply) {
      if (!this.mHasSavedLabelVisibilityMode) {
        this.mSavedLabelVisibilityMode = getLabelVisibilityMode();
        this.mHasSavedLabelVisibilityMode = true;
      }
      setLabelVisibilityMode(2);
      applySmallScreenOuterPadding();
    } else if (this.mHasSavedLabelVisibilityMode) {
      setLabelVisibilityMode(this.mSavedLabelVisibilityMode);
      this.mHasSavedLabelVisibilityMode = false;
    } else {
      restoreDefaultByViewType();
    }
    getPresenter().updateMenuView(false);
    if (isMenuView) {
      ((BottomNavigationMenuView) menuView).updateItemBackground(true);
    }
  }

  private void updateStrategy(int viewType) {
    ViewTypeStrategy strategy =
        StrategyFactory.INSTANCE.createStrategy(viewType, this.mIsFloatingStyle);
    strategy.applyNavigationBarStyle(this);
    MenuView menuView = getMenuView();
    if (menuView instanceof BottomNavigationMenuView) {
      ((BottomNavigationMenuView) menuView).setStrategy(strategy);
      if (strategy.isFloatingStyle()) {
        setClipToPadding(false);
        setClipChildren(false);
      }
    }
  }

  @Override
  public boolean applyBlurInfo(@NonNull Context context) {
    if (Build.VERSION.SDK_INT < 35) {
      return false;
    }
    clearBlurInfo(context);
    SemBlurInfoState blurInfoState = generateBlurInfo(context);
    if (blurInfoState.applyBlurInfo(this)) {
      this.mBlurInfo = blurInfoState;
      return true;
    }
    this.mBlurInfo = null;
    return false;
  }

  @Override
  public void clearBlurInfo(Context context) {
    if (this.mBlurInfo != null) {
      this.mBlurInfo.clearBlurInfo(this);
      this.mBlurInfo = null;
    }
  }

  @Override
  public boolean isBlurApplied() {
    return this.mBlurInfo != null;
  }

  @Override
  public void setBlurMode(int mode) {
    this.mBlurMode = mode;
    applyBlurInfo(getContext());
  }

  @Override
  public void setBackground(Drawable background) {
    super.setBackground(background);
    this.mBackgroundDrawable = background;
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) {
      seslSetTouchDelegateForBottomBar();
    } else {
      seslRemoveListenerForTouchDelegate();
    }
  }

  @Override
  public void seslSetGroupDividerEnabled(boolean enabled) {
    super.seslSetGroupDividerEnabled(enabled);
  }

  private void seslSetTouchDelegateForBottomBar() {
    ViewTreeObserver vto = getViewTreeObserver();
    if (vto == null || mOnGlobalLayoutListenerForTD != null) return;

    mOnGlobalLayoutListenerForTD =
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            final BottomNavigationView bottomNavigationView = BottomNavigationView.this;
            if (bottomNavigationView != null) {
              bottomNavigationView.post(
                  new Runnable() {
                    @Override
                    public void run() {
                      SeslTouchTargetDelegate touchTargetDelegate =
                          new SeslTouchTargetDelegate(bottomNavigationView);
                      int childCount = bottomNavigationView.getChildCount();

                      View bottomNavMenuView = null;
                      int i = 0;
                      while (i < childCount) {
                        View child = bottomNavigationView.getChildAt(i);
                        if (child instanceof BottomNavigationMenuView) {
                          bottomNavMenuView = child;
                          break;
                        }
                        i++;
                      }

                      boolean shouldDelegate = false;
                      if (bottomNavMenuView != null
                          && bottomNavMenuView.getVisibility() == VISIBLE) {
                        ViewGroup menuGroup = (ViewGroup) bottomNavMenuView;
                        int subChildCount = menuGroup.getChildCount();
                        int j = 0;
                        while (j < subChildCount) {
                          View nmvChild = menuGroup.getChildAt(j);
                          if (nmvChild.getVisibility() == VISIBLE) {
                            int midHeight = nmvChild.getMeasuredHeight() / 2;
                            touchTargetDelegate.addTouchDelegate(
                                nmvChild,
                                SeslTouchTargetDelegate.ExtraInsets.of(
                                    j == 0 ? midHeight : 0,
                                    midHeight,
                                    j == subChildCount - 1 ? midHeight : 0,
                                    midHeight));
                            shouldDelegate = true;
                          }
                          j++;
                        }
                      }
                      if (shouldDelegate) {
                        bottomNavigationView.setTouchDelegate(touchTargetDelegate);
                      }
                    }
                  });
            }
          }
        };
    vto.addOnGlobalLayoutListener(mOnGlobalLayoutListenerForTD);
  }

  private void seslRemoveListenerForTouchDelegate() {
    if (mOnGlobalLayoutListenerForTD != null) {
      getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListenerForTD);
      mOnGlobalLayoutListenerForTD = null;
    }
  }
  //sesl9
}
