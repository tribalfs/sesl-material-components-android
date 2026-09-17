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

package com.google.android.material.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.view.menu.BaseMenuPresenter;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SubMenuBuilder;
import androidx.core.view.ViewCompat;

import com.google.android.material.R;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.internal.ParcelableSparseArray;

/**
 * <b>SESL variant.</b><br>
 * <p>
 * For internal use only.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class NavigationBarPresenter extends BaseMenuPresenter/*sesl*/ {
  private NavigationBarMenuView menuView;
  private int id;

  //Sesl
  private static final int ANIM_UPDATE_DELAY = 180;
  private static final int ANIM_UPDATE_DURATION = 400;
  private static final int MSG_UPDATE_ANIMATION = 100;
  private Handler mAnimationHandler;
  private Context mContext;
  private OverflowPopup mOverflowPopup;
  private final PopupPresenterCallback mPopupPresenterCallback;
  private OpenOverflowRunnable mPostedOpenRunnable;
  private boolean mSetAnim;
  private MenuBuilder menu;
  private boolean updateSuspended;
  //sesl

  public void setMenuView(@NonNull NavigationBarMenuView menuView) {
    this.menuView = menuView;
  }

  @Override
  public void initForMenu(@NonNull Context context, @NonNull MenuBuilder menu) {
    menuView.initialize(menu);
    mContext = context;//sesl
  }

  @Override
  @Nullable
  public MenuView getMenuView(@Nullable ViewGroup root) {
    return menuView;
  }

  @Override
  public void updateMenuView(boolean cleared) {
    if (updateSuspended) {
      return;
    }
    //Sesl
    if (!mSetAnim) {
      if (cleared) {
        menuView.buildMenuView();
      } else {
        menuView.updateMenuView();
      }
      return;
    }
    if (!cleared) {
      menuView.postDelayed(() -> menuView.updateMenuView(), ANIM_UPDATE_DELAY);
      return;
    }

    if (mAnimationHandler.hasMessages(MSG_UPDATE_ANIMATION)) {
      mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
    }
    mAnimationHandler.sendEmptyMessage(MSG_UPDATE_ANIMATION);
    //sesl
  }

  @Override
  public void setCallback(@Nullable Callback cb) {}

  @Override
  public boolean onSubMenuSelected(@Nullable SubMenuBuilder subMenu) {
    return false;
  }

  @Override
  public void onCloseMenu(@Nullable MenuBuilder menu, boolean allMenusAreClosing) {}

  @Override
  public boolean flagActionItems() {
    return false;
  }

  @Override
  public boolean expandItemActionView(@Nullable MenuBuilder menu, @Nullable MenuItemImpl item) {
    return false;
  }

  @Override
  public boolean collapseItemActionView(@Nullable MenuBuilder menu, @Nullable MenuItemImpl item) {
    return false;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @NonNull
  @Override
  public Parcelable onSaveInstanceState() {
    SavedState savedState = new SavedState();
    savedState.selectedItemId = menuView.getSelectedItemId();
    savedState.badgeSavedStates =
        BadgeUtils.createParcelableBadgeStates(menuView.getBadgeDrawables());
    return savedState;
  }

  @Override
  public void onRestoreInstanceState(@NonNull Parcelable state) {
    if (state instanceof SavedState) {
      menuView.tryRestoreSelectedItemId(((SavedState) state).selectedItemId);
      SparseArray<BadgeDrawable> badgeDrawables =
          BadgeUtils.createBadgeDrawablesFromSavedStates(
              menuView.getContext(), ((SavedState) state).badgeSavedStates);
      menuView.restoreBadgeDrawables(badgeDrawables);
    }
  }

  public void setUpdateSuspended(boolean updateSuspended) {
    this.updateSuspended = updateSuspended;
  }

  static class SavedState implements Parcelable {
    int selectedItemId;
    @Nullable ParcelableSparseArray badgeSavedStates;

    SavedState() {}

    SavedState(@NonNull Parcel in) {
      selectedItemId = in.readInt();
      badgeSavedStates = in.readParcelable(getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
      out.writeInt(selectedItemId);
      out.writeParcelable(badgeSavedStates, /* parcelableFlags= */ 0);
    }

    public static final Creator<SavedState> CREATOR =
        new Creator<SavedState>() {
          @NonNull
          @Override
          public SavedState createFromParcel(@NonNull Parcel in) {
            return new SavedState(in);
          }

          @NonNull
          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }

  //Sesl
  public class OverflowPopup extends MenuPopupHelper {
    @Override
    public void onDismiss() {
      if (menu != null) {
        menu.close();
      }
      mOverflowPopup = null;
      super.onDismiss();
    }

    private OverflowPopup(Context context, MenuBuilder menuBuilder, View view, boolean z) {
      super(context, menuBuilder, view, z, androidx.appcompat.R.attr.actionOverflowBottomMenuStyle);
      setGravity(8388613);
      setPresenterCallback(NavigationBarPresenter.this.mPopupPresenterCallback);
      setAnchorView(view);
      seslSetOverlapAnchor(false);
      seslForceShowUpper(true);
    }
  }

  public class PopupPresenterCallback implements MenuPresenter.Callback {
    public PopupPresenterCallback() {
    }

    @Override
    public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
      if (menuBuilder instanceof SubMenuBuilder) {
        menuBuilder.getRootMenu().close(false);
      }
      MenuPresenter.Callback callback = NavigationBarPresenter.this.getCallback();
      if (callback != null) {
        callback.onCloseMenu(menuBuilder, z);
      }
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder menuBuilder) {
      MenuPresenter.Callback callback;
      return (menuBuilder == null || (callback = NavigationBarPresenter.this.getCallback()) == null || !callback.onOpenSubMenu(menuBuilder)) ? false : true;
    }
  }

  public NavigationBarPresenter(Context context) {
    super(context, androidx.appcompat.R.layout.sesl_action_menu_layout, androidx.appcompat.R.layout.sesl_action_menu_item_layout);
    this.updateSuspended = false;
    this.mSetAnim = false;
    this.mAnimationHandler = new Handler(Looper.getMainLooper()) { // from class: com.google.android.material.navigation.NavigationBarPresenter.1
      @Override // android.os.Handler
      public void handleMessage(Message message) {
        if (message.what == 100) {
          NavigationBarPresenter.this.updateMenuViewWithAnimate();
        }
      }
    };
    this.mPopupPresenterCallback = new PopupPresenterCallback();
  }

  public void updateMenuViewWithAnimate() {
    if (this.menuView == null) {
      return;
    }
    final PathInterpolator pathInterpolator = new PathInterpolator(0.33f, 0.0f, 0.1f, 1.0f);
    NavigationBarMenuView navigationBarMenuView = this.menuView;
    ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(navigationBarMenuView, "translationY", navigationBarMenuView.getHeight());
    objectAnimatorOfFloat.setDuration(400L);
    objectAnimatorOfFloat.setInterpolator(pathInterpolator);
    objectAnimatorOfFloat.start();
    objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animator) {
        NavigationBarPresenter.this.menuView.buildMenuView();
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(NavigationBarPresenter.this.menuView, "translationY", 0.0f);
        objectAnimatorOfFloat2.setDuration(400L);
        objectAnimatorOfFloat2.setInterpolator(pathInterpolator);
        objectAnimatorOfFloat2.start();
        super.onAnimationEnd(animator);
      }
    });
  }

  public boolean hideOverflowMenu() {
    Object obj;
    OpenOverflowRunnable openOverflowRunnable = this.mPostedOpenRunnable;
    if (openOverflowRunnable != null && (obj = this.mMenuView) != null) {
      ((View) obj).removeCallbacks(openOverflowRunnable);
      this.mPostedOpenRunnable = null;
      return true;
    }
    OverflowPopup overflowPopup = this.mOverflowPopup;
    if (overflowPopup == null) {
      return false;
    }
    overflowPopup.dismiss();
    return true;
  }

  public boolean isOverflowMenuShowing() {
    OverflowPopup overflowPopup = this.mOverflowPopup;
    return overflowPopup != null && overflowPopup.isShowing();
  }


  public void setAnimationEnable(boolean z) {
    this.mSetAnim = z;
  }

  public boolean showOverflowMenu(MenuBuilder menuBuilder) {
    if (isOverflowMenuShowing() || menuBuilder == null || this.menuView == null || this.mPostedOpenRunnable != null || menuBuilder.getNonActionItems().isEmpty()) {
      return false;
    }
    OverflowPopup overflowPopup = new OverflowPopup(this.mContext, menuBuilder, this.menuView.mOverflowButton, true);
    this.mOverflowPopup = overflowPopup;
    OpenOverflowRunnable openOverflowRunnable = new OpenOverflowRunnable(overflowPopup);
    this.mPostedOpenRunnable = openOverflowRunnable;
    this.menuView.post(openOverflowRunnable);
    super.onSubMenuSelected(null);
    return true;
  }

  @Override
  public void bindItemView(MenuItemImpl menuItemImpl, MenuView.ItemView itemView) {
  }

  public class OpenOverflowRunnable implements Runnable {
    private OverflowPopup mPopup;

    @Override
    public void run() {
      if (menu != null) {
        menu.changeMenuMode();
      }
      if (menuView != null) {
        int dimensionPixelSize = NavigationBarPresenter.this.mContext.getResources().getDimensionPixelSize(R.dimen.sesl_bottom_navigation_floating_overflow_menu_top_margin);
        boolean z = menuView.getLayoutDirection() == 1;
        int dimensionPixelSize2 = NavigationBarPresenter.this.menuView.mIsFloatingStyle ? NavigationBarPresenter.this.mContext.getResources().getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_menu_popup_offset_horizontal) : 0;
        if (NavigationBarPresenter.this.menuView.getWindowToken() != null) {
          OverflowPopup overflowPopup = this.mPopup;
          if (z) {
            dimensionPixelSize2 = -dimensionPixelSize2;
          }
          if (overflowPopup.tryShow(dimensionPixelSize2, -dimensionPixelSize)) {
            NavigationBarPresenter.this.mOverflowPopup = this.mPopup;
          }
        }
      }
      NavigationBarPresenter.this.mPostedOpenRunnable = null;
    }

    private OpenOverflowRunnable(OverflowPopup overflowPopup) {
      this.mPopup = overflowPopup;
    }
  }
}
