/*
 * Copyright 2024 The Android Open Source Project
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

package com.google.android.material.appbar;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.view.MotionEvent.TOOL_TYPE_MOUSE;
import static android.view.View.VISIBLE;
import static android.view.WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE;
import static android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowInsetsAnimationControlListener;
import android.view.WindowInsetsAnimationController;
import android.view.WindowInsetsController;
import android.view.WindowInsetsController.OnControllableInsetsChangedListener;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;
import androidx.reflect.content.res.SeslConfigurationReflector;
import androidx.reflect.view.SeslDecorViewReflector;

import com.google.android.material.R;
import com.google.android.material.internal.SeslContextUtils;
import com.google.android.material.internal.SeslDisplayUtils;

import java.util.List;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */
/**
 * {@link AppBarLayout.Behavior} that coordinates "immersive scroll" between an {@link AppBarLayout}
 * and the system bars (status/navigation bars) on Android R+.
 *
 * <p>When immersive scroll is enabled (via {@link AppBarLayout#seslIsActivatedImmsersiveScroll()}),
 * this behavior requests a {@link WindowInsetsAnimationController} from the
 * {@link WindowInsetsController} and drives it based on {@link AppBarLayout} offset changes. This
 * allows the app bar collapsing/expanding motion to be synchronized with showing/hiding system bars,
 * and also translates any optional bottom overlay view (see {@link #setBottomView(View)}).
 *
 * <h3>When immersive scroll is disabled</h3>
 * <ul>
 *   <li>If accessibility touch exploration is enabled.</li>
 *   <li>If a mouse is used to scroll.</li>
 *   <li>If the device is in DeX mode.</li>
 *   <li>If the activity is in multi-window mode.</li>
 *   <li>If a custom {@link WindowInsetsAnimation.Callback} is set via
 *       {@link #setWindowInsetsAnimationCallback(AppBarLayout, WindowInsetsAnimation.Callback)}.</li>
 * </ul>
 *
 * <p>This class is a SESL/Samsung extension and relies on platform APIs such as
 * {@link WindowInsetsAnimationController}.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public final class SeslImmersiveScrollBehavior extends AppBarLayout.Behavior {
  /** Log tag. */
  private static final String TAG = "SeslImmersiveScrollBehavior";
  /** Handler message used to start the delayed restore animation. */
  private static final int MSG_APPEAR_ANIMATION = 100;

  private WindowInsetsAnimationController mAnimationController;
  private AppBarLayout mAppBarLayout;
  private View mBottomArea;
  private CancellationSignal mCancellationSignal;
  private CollapsingToolbarLayout mCollapsingToolbarLayout;
  private View mContentView;
  private Context mContext;
  private CoordinatorLayout mCoordinatorLayout;
  private WindowInsetsAnimation.Callback mCustomWindowInsetsAnimation = null;
  private View mDecorView;
  private WindowInsets mDecorViewInset;
  private View mNavigationBarBg;
  private ValueAnimator mOffsetAnimator;
  private View mStatusBarBg;
  private View mTargetView;
  private WindowInsetsController mWindowInsetsController = null;
  private OnControllableInsetsChangedListener mOnInsetsChangedListener = null;
  private boolean mIsSetAutoRestore = true;

  private int mNavigationBarBgHeight;
  private int mPrevOffset;
  private int mPrevOrientation;
  private int mStatusBarHeight;

  private float mCurOffset = 0f;
  boolean mCalledHideShowOnLayoutChild = false;
  private boolean mCanImmersiveScroll;
  private boolean mIsMultiWindow;
  private boolean mNeedRestoreAnim = true;
  private boolean mShownAtDown;
  private boolean mToolIsMouse;
  private boolean isRoundedCornerHide = false;
  private boolean useCustomAnimationCallback = false;
  private boolean mNeedToCheckBottomViewMargin = false;
  //Sesl8
  private int mNavigationBarFrameHeight;
  private boolean mNeedInit = false;
  //sesl8

  //Custom
  private int mInitFitsSystemWindows = UNSET;
  private static final int UNSET = -1;
  private static final int FITS_SYSTEM_WINDOWS_ENABLED = 1;
  private static final int FITS_SYSTEM_WINDOWS_DISABLED = 0;
  //custom

  private final Handler mAnimationHandler
          = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(@NonNull Message msg) {
      if (msg.what == MSG_APPEAR_ANIMATION) {
        startRestoreAnimation();
      }
    }
  };

  private final AppBarLayout.OnOffsetChangedListener mOffsetChangedListener
          = new AppBarLayout.OnOffsetChangedListener() {

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
      if (mAppBarLayout != null && mAppBarLayout.isDetachedState()) {
        Log.e(TAG, "AppBarLayout was DetachedState. Skip onOffsetChanged");
        return;
      }

      if (!useCustomAnimationCallback) {
        float height;
        float navigationBarMaxTranslation;
        int updatedTotalScrollRange;

        int leftInset = 0;
        float newImmOffset = 0.0f;

        if (mCanImmersiveScroll) {
          int bottomAreaHeight = mBottomArea != null ? mBottomArea.getHeight() : 0;
          float collapsedHeight = appBarLayout.seslGetCollapsedHeight();

          final float bottomHeight = (float)(mNavigationBarBgHeight + bottomAreaHeight);
          final float scrollRatio = bottomHeight / (collapsedHeight == 0f ? 1f : collapsedHeight);
          final int totalScrollRange = appBarLayout.getTotalScrollRange();
          float scrolledRange = (totalScrollRange - appBarLayout.seslGetAdditionalScrollRange()) + verticalOffset - collapsedHeight;
          float scrollOffset = mStatusBarHeight + scrolledRange;

          float remainingScrollY = scrollRatio * scrolledRange * 2.0f;
          float topInset = Math.min(mStatusBarHeight, mStatusBarHeight + scrolledRange);
          float currentNavBarHeight = Math.max(Math.min(mNavigationBarBgHeight, mNavigationBarBgHeight + remainingScrollY), 0.0f);

          if ((float)appBarLayout.getBottom() <= collapsedHeight) {

            if (canImmersiveScroll()) {
              if (mBottomArea != null && mBottomArea.getVisibility() != View.GONE && bottomAreaHeight != 0) {
                float bottomAreaTranslation = Math.min(bottomAreaHeight + remainingScrollY, currentNavBarHeight);
                mBottomArea.setTranslationY(-Math.round(bottomAreaTranslation));
                if (mBottomArea.getVisibility() != VISIBLE) {
                  bottomAreaHeight = 0;
                }
                navigationBarMaxTranslation = Math.max(bottomAreaHeight + bottomAreaTranslation, 0.0f);
              } else {
                navigationBarMaxTranslation = Math.max(currentNavBarHeight, 0.0f);
              }

              updatedTotalScrollRange = appBarLayout.getTotalScrollRange();

              height = navigationBarMaxTranslation + updatedTotalScrollRange + verticalOffset;

              if (mNavigationBarBg != null) {
                if (!isHideCameraCutout(mDecorViewInset)) {
                  mNavigationBarBg.setTranslationY(-Math.min(0.0F, Math.round(remainingScrollY)));
                } else {
                  mNavigationBarBg.setTranslationY(0.0F);
                }
              } else if (mNavigationBarBgHeight != 0) {
                findSystemBarsBackground();
                if (mNavigationBarBg != null) {
                  mNavigationBarBg.setTranslationY(0.0F);
                }
              }

              if (mStatusBarBg != null) {
                mStatusBarBg.setTranslationY(Math.min(0.0f, scrolledRange));
              }

              if (mCurOffset != scrollOffset) {
                mCurOffset = scrollOffset;

                if (mAnimationController != null) {
                  if (mAnimationController.isFinished()) {
                    Log.e(TAG, "AnimationController is already finished by App side");
                  } else {
                    forceHideRoundedCorner((int) currentNavBarHeight);
                    int rightInset = 0;

                    if (SeslDisplayUtils.isPinEdgeEnabled(mContext)) {
                      Insets navBarInsets = mDecorViewInset.getInsets(WindowInsets.Type.navigationBars());
                      rightInset = SeslDisplayUtils.getPinnedEdgeWidth(mContext);
                      int activeEdgeArea = SeslDisplayUtils.getEdgeArea(mContext);

                      if (rightInset == navBarInsets.left && activeEdgeArea == 0) {
                        rightInset = 0;
                        leftInset = rightInset;
                      } else if (rightInset != navBarInsets.right || activeEdgeArea != 1) {
                        rightInset = 0;
                      }
                    }

                    float bottomInset = Math.max(
                            Math.min(mNavigationBarFrameHeight, remainingScrollY + mNavigationBarFrameHeight),
                            0.0f
                    );
                    final float animationProgress = (mNavigationBarFrameHeight - currentNavBarHeight)
                            / (mNavigationBarFrameHeight == 0 ? 1 : mNavigationBarFrameHeight);

                    mAnimationController.setInsetsAndAlpha(
                            Insets.of(leftInset, (int) topInset, rightInset, (int) bottomInset),
                            1.0F,
                            animationProgress
                    );
                  }
                }
              }
            } else {
              if (mStatusBarBg != null) {
                mStatusBarBg.setTranslationY(0.0F);
              }

              if (mNavigationBarBg != null) {
                mNavigationBarBg.setTranslationY(0.0F);
              }

              if (mAppBarLayout != null) {
                float appBarVisibleHeight = mAppBarLayout.getTotalScrollRange() + verticalOffset;
                if (mBottomArea != null) {
                  if (collapsedHeight == 0) {
                    collapsedHeight = 1.0F;
                  }
                  float bottomAreaTranslationY = (float) bottomAreaHeight - (mAppBarLayout.getBottom() * (float) bottomAreaHeight / collapsedHeight);
                  mBottomArea.setTranslationY(Math.max(bottomAreaTranslationY, 0.0F));
                  newImmOffset = (int)((appBarVisibleHeight + mBottomArea.getHeight()) - Math.max(bottomAreaTranslationY, 0.0f));
                } else {
                  newImmOffset = appBarVisibleHeight;
                }
              }

              finishWindowInsetsAnimationController();
              height = newImmOffset;
            }
          } else {
            float appBarVisibleHeight = mAppBarLayout != null ? mAppBarLayout.getTotalScrollRange() + verticalOffset : 0.0f;

            if (mIsMultiWindow && mBottomArea != null) {
              mBottomArea.setTranslationY(0.0f);
              appBarVisibleHeight += mBottomArea.getHeight();
            }

            height = appBarVisibleHeight;
            if (!mIsMultiWindow && mBottomArea != null && mDecorViewInset != null) {
              if (isNavigationBarBottomPosition()) {
                mBottomArea.setTranslationY(-mNavigationBarBgHeight);
                if (mNavigationBarBg != null && mNavigationBarBg.getTranslationY() != 0.0f) {
                  mNavigationBarBg.setTranslationY(0.0f);
                }
              } else if (mNavigationBarBg != null && mNavigationBarBg.getTranslationY() != 0.0f) {
                mBottomArea.setTranslationY(0.0f);
              }
              height += mBottomArea.getHeight() + mNavigationBarBgHeight;
            }
          }

          if (mAppBarLayout != null) {
            mAppBarLayout.onImmOffsetChanged((int) height);
          }
        } else {
          if (mStatusBarBg != null) {
            mStatusBarBg.setTranslationY(0.0F);
          }

          if (mNavigationBarBg != null) {
            mNavigationBarBg.setTranslationY(0.0F);
          }

          if (mBottomArea != null) {
            if (!mNeedToCheckBottomViewMargin || mDecorView == null) {
              mBottomArea.setTranslationY(0.0f);
            } else {
              mDecorViewInset = mDecorView.getRootWindowInsets();
              if (mDecorViewInset != null) {
                mBottomArea.setTranslationY(-mDecorViewInset.getInsets(WindowInsets.Type.navigationBars()).bottom);
              } else {
                mBottomArea.setTranslationY(0.0f);
              }
            }
          }

          if (mAppBarLayout != null) {
            mAppBarLayout.onImmOffsetChanged(0);
          }
        }
      }
    }
  };

  private final WindowInsetsAnimation.Callback mWindowAnimationCallback
          = new WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

    @NonNull
    @Override
    public WindowInsets onProgress(@NonNull WindowInsets windowInsets,
                                   @NonNull List<WindowInsetsAnimation> list) {
      return windowInsets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimation animation) {
      super.onEnd(animation);
      if (mContentView == null || mAppBarLayout.isDetachedState()) {
        return;
      }
      mDecorViewInset = mContentView.getRootWindowInsets();
      if (mDecorViewInset != null) {
        mContentView.dispatchApplyWindowInsets(mDecorViewInset);
      }
    }
  };


  private final WindowInsetsAnimationControlListener mWindowInsetsAnimationControlListener
          = new WindowInsetsAnimationControlListener() {

    @Override
    public void onCancelled(@Nullable WindowInsetsAnimationController windowInsetsAnimationController) {
      cancelWindowInsetsAnimationController();
    }

    @Override
    public void onFinished(@NonNull WindowInsetsAnimationController windowInsetsAnimationController) {
      resetWindowInsetsAnimationController();
    }

    @Override
    public void onReady(@NonNull WindowInsetsAnimationController windowInsetsAnimationController, int i) {
      if (mDecorView != null) {
        mCancellationSignal = null;
        mAnimationController = windowInsetsAnimationController;
        setInsetsAndAlphaToDefault();
      }
    }
  };

  /**
   * Creates a new behavior instance.
   *
   * <p>The behavior starts in an "eligible" state ({@code mCanImmersiveScroll = true}) and then
   * will dynamically enable/disable immersive scroll as conditions change (orientation,
   * accessibility, multi-window, etc.).
   */
  public SeslImmersiveScrollBehavior(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    mCanImmersiveScroll = true;

    mContext = context;
    updateSystemBarsHeight();
    updateAppBarHeightProportion();
  }

  /**
   * Animates the app bar (and any associated system bar insets animation) back to its collapsed
   * position.
   *
   * <p>This is typically used when the app bar is currently hidden and the system bars need to be
   * restored.
   */
  private void animateRestoreTopAndBottom(
      final CoordinatorLayout coordinatorLayout, final AppBarLayout appBarLayout, int offset) {
    mPrevOffset = offset;
    PathInterpolator pathInterpolator = new PathInterpolator(0.17f, 0.17f, 0.2f, 1.0f);
    float fSeslGetCollapsedHeight = (-mAppBarLayout.getHeight()) + mAppBarLayout.seslGetCollapsedHeight();
    final int[] iArr = {0};

    ValueAnimator valueAnimator = mOffsetAnimator;
    if (mOffsetAnimator == null) {
      ValueAnimator valueAnimator2 = new ValueAnimator();
      mOffsetAnimator = new ValueAnimator();
      valueAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          if (mTargetView == null) {
            Log.e(TAG, "mTargetView is null");
            return;
          }
          int iIntValue = (Integer) animation.getAnimatedValue();
          iArr[0] = mPrevOffset - iIntValue;
          mTargetView.scrollBy(0, -iArr[0]);
          setHeaderTopBottomOffset(coordinatorLayout, appBarLayout, iIntValue);
          mPrevOffset = iIntValue;
        }
      });
    } else {
      valueAnimator.cancel();
    }

    mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animator) {
        super.onAnimationEnd(animator);
        if (mNavigationBarBg != null) {
          mNavigationBarBg.setTranslationY(0.0f);
        }
        if (mAnimationController != null) {
          mAnimationController.finish(true);
        }
      }
    });
    mOffsetAnimator.setDuration(150L);
    mOffsetAnimator.setInterpolator(pathInterpolator);
    mOffsetAnimator.setStartDelay(0L);
    mOffsetAnimator.setIntValues(mNeedRestoreAnim ? -mAppBarLayout.getHeight() : (int) fSeslGetCollapsedHeight, (int) fSeslGetCollapsedHeight);
    mOffsetAnimator.start();
  }

  /**
   * Determines whether immersive scroll can run at this moment.
   *
   * <p>This method also performs side effects to keep internal state consistent (for example,
   * calling {@link #prepareImmersiveScroll(boolean, boolean)} and cancelling running controllers
   * when required).
   */
  private boolean canImmersiveScroll() {
    AppBarLayout appBarLayout;
    if (mAppBarLayout != null && Build.VERSION.SDK_INT >= 30 && !isDexEnabled() && !useCustomAnimationCallback) {
      if (mAppBarLayout.getIsMouse()) {
        prepareImmersiveScroll(false, false);
        return false;
      }
      if (isAccessibilityEnabled()) {
        Log.i(TAG, "Disable ImmersiveScroll due to accessibility enabled");
        updateOrientationState();
        prepareImmersiveScroll(false, true);
        return false;
      }
      if (mAppBarLayout.seslIsActivatedImmsersiveScroll()) {
        prepareImmersiveScroll(true, false);
        boolean isPortrait = !getCurrentNavbarCanMoveState() || updateOrientationState();
        Context context = mContext;
        if (context != null) {
          Activity activity = SeslContextUtils.getActivity(context);
          if (activity == null && (appBarLayout = mAppBarLayout) != null) {
            mContext = appBarLayout.getContext();
            activity = SeslContextUtils.getActivity(mAppBarLayout.getContext());
          }
          if (activity != null) {
            boolean isMultiWindow = activity.isInMultiWindowMode();
            if (mIsMultiWindow != isMultiWindow) {
              forceRestoreWindowInset(true);
              cancelWindowInsetsAnimationController();
            }
            mIsMultiWindow = isMultiWindow;
            if (isMultiWindow) {
              return false;
            }
          }
        }
        return isPortrait;
      }
      AppBarLayout appBarLayout2 = mAppBarLayout;
      if (appBarLayout2 != null && appBarLayout2.isImmersiveActivatedByUser()) {
        cancelWindowInsetsAnimationController();
      }
      prepareImmersiveScroll(false, false);
    }
    return false;
  }


  /**
   * Locates the platform-provided status/navigation bar background views within the decor view.
   *
   * <p>These views (when present) are translated to visually match the insets animation.
   */
  private void findSystemBarsBackground() {
    View decorView = mDecorView;
    if (decorView == null || mContext == null) {
      return;
    }

    mDecorViewInset = decorView.getRootWindowInsets();
    mDecorView.getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                mDecorView.getViewTreeObserver().removeOnPreDrawListener(this);
                mStatusBarBg = mDecorView.findViewById(android.R.id.statusBarBackground);
                mNavigationBarBg = mDecorView.findViewById(android.R.id.navigationBarBackground);
                return false;
              }
            });

    updateSystemBarsHeight();
  }

  /**
   * Finishes the current {@link WindowInsetsAnimationController} based on its current insets.
   *
   * <p>If the controller is at the shown/hidden state it will be finished accordingly. If the
   * controller does not exist but a request is pending (via {@link #startAnimationControlRequest()}),
   * the request cancellation signal is cancelled.
   */
  public void finishWindowInsetsAnimationController() {
    AppBarLayout appBarLayout = mAppBarLayout;
    if (appBarLayout == null) {
      return;
    }

    WindowInsetsAnimationController controller = mAnimationController;
    if (mContentView == null) {
      View rootView = appBarLayout.getRootView();
      mDecorView = rootView;
      mContentView = rootView.findViewById(android.R.id.content);
    }
    if (controller == null) {
      CancellationSignal cancellationSignal = mCancellationSignal;
      if (cancellationSignal != null) {
        cancellationSignal.cancel();
        return;
      }
      return;
    }
    int currentInsetsBottom = controller.getCurrentInsets().bottom;
    int shownStateInsetsBottom = controller.getShownStateInsets().bottom;
    int hiddenStateInsetsBottom = controller.getHiddenStateInsets().bottom;
    if (currentInsetsBottom == shownStateInsetsBottom) {
      controller.finish(true);
    } else if (currentInsetsBottom == hiddenStateInsetsBottom) {
      controller.finish(false);
    }
  }

  /**
   * Forces rounded corners to be hidden while the navigation bar is not fully shown.
   *
   * <p>This avoids a visual mismatch between the decor rounded corners and the in-progress insets
   * animation.
   */
  private void forceHideRoundedCorner(int bottomInset) {
    WindowInsetsAnimationController controller = mAnimationController;
    if (controller == null || mDecorView == null) {
      return;
    }
    boolean shouldHideRoundedCorner = bottomInset != controller.getShownStateInsets().bottom;
    if (shouldHideRoundedCorner != isRoundedCornerHide) {
      isRoundedCornerHide = shouldHideRoundedCorner;
      SeslDecorViewReflector.semSetForceHideRoundedCorner(mDecorView, shouldHideRoundedCorner);
    }
  }

  private boolean getCurrentNavbarCanMoveState() {
    try {
      return mContext.getApplicationContext().getResources()
              .getBoolean(Resources.getSystem().getIdentifier("config_navBarCanMove", "bool", "android"));
    } catch (Exception e) {
      Log.e(TAG, "ERROR, e : " + e.getMessage());
      return true;
    }
  }

  /** @return Whether touch exploration (accessibility) is enabled. */
  private boolean isAccessibilityEnabled() {
    Context context = mContext;
    if (context == null) {
      return false;
    }
    return ((AccessibilityManager) context.getSystemService("accessibility")).isTouchExplorationEnabled();
  }

  /** @return Whether Samsung DeX mode is enabled for the current configuration. */
  private boolean isDexEnabled() {
    Context context = mContext;
    if (context == null) {
      return false;
    }
    return SeslConfigurationReflector.isDexEnabled(context.getResources().getConfiguration());
  }

  /**
   * Returns whether the device is using gesture navigation.
   *
   * <p>This reads the platform config {@code config_navBarInteractionMode}.
   */
  public static boolean isGestureNavigateEnabled(Context context) throws Resources.NotFoundException {
    int integer = context.getResources()
            .getInteger(Resources.getSystem().getIdentifier("config_navBarInteractionMode", "integer", "android"));
    return integer == 2 || integer == 3;
  }

  private boolean isHideCameraCutout(WindowInsets windowInsets) {
    return windowInsets.getDisplayCutout() == null && windowInsets.getInsets(WindowInsets.Type.statusBars()).top == 0;
  }

  private boolean isLandscape() {
    AppBarLayout appBarLayout = mAppBarLayout;
    return appBarLayout != null && appBarLayout.getCurrentOrientation() == ORIENTATION_LANDSCAPE;
  }

  /**
   * Returns whether the navigation bar is positioned at the bottom (as opposed to the side).
   *
   * <p>On large-screen/landscape configurations the navigation bar may be on the side.
   */
  private boolean isNavigationBarBottomPosition() {
    if (mDecorViewInset == null) {
      if (mDecorView == null) {
        mDecorView = mAppBarLayout.getRootView();
      }
      mDecorViewInset = mDecorView.getRootWindowInsets();
    }
    WindowInsets windowInsets = mDecorViewInset;
    return windowInsets == null || windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom != 0;
  }

  private boolean isTouchInGestureNavigationArea(float touchVerticalPosition, WindowInsets windowInsets) {
    if (mDecorView == null) {
      return false;
    }
    int mandatoryGestureInsetsBottom = windowInsets.getInsets(WindowInsets.Type.mandatorySystemGestures()).bottom;
    Rect rect = new Rect();
    mDecorView.getWindowVisibleDisplayFrame(rect);
    return touchVerticalPosition > ((float) (rect.bottom - mandatoryGestureInsetsBottom));
  }

  /**
   * Updates internal immersive-scroll state and applies any required window/insets adjustments.
   *
   * @param canImmersiveScroll Whether immersive scroll should be considered enabled.
   * @param shouldRestoreWindowInset Whether system bars should be forced visible when disabling.
   */
  private void prepareImmersiveScroll(
      boolean canImmersiveScroll, boolean shouldRestoreWindowInset) {
    if (mCanImmersiveScroll != canImmersiveScroll) {
      mCanImmersiveScroll = canImmersiveScroll;
      forceRestoreWindowInset(shouldRestoreWindowInset);
      setupDecorsFitSystemWindowState(canImmersiveScroll);
      if (canImmersiveScroll != mAppBarLayout.getCanScroll()) {
        mAppBarLayout.setCanScroll(canImmersiveScroll);
      }
    }
  }

  private void resetWindowInsetsAnimationController() {
    mAnimationController = null;
    mCancellationSignal = null;
    mShownAtDown = false;
  }


  /**
   * Applies the default shown-state insets to the active {@link WindowInsetsAnimationController}.
   */
  public void setInsetsAndAlphaToDefault() {
    int pinnedEdgeWidth = 0;
    int leftInset = 0;
    if (SeslDisplayUtils.isPinEdgeEnabled(mContext)) {
      Insets navBarInsets = mDecorViewInset.getInsets(WindowInsets.Type.navigationBars());
      pinnedEdgeWidth = SeslDisplayUtils.getPinnedEdgeWidth(mContext);
      int edgeArea = SeslDisplayUtils.getEdgeArea(mContext);
      if (pinnedEdgeWidth == navBarInsets.left && edgeArea == 0) {
        pinnedEdgeWidth = 0;
        leftInset = pinnedEdgeWidth;
      } else if (pinnedEdgeWidth != navBarInsets.right || edgeArea != 1) {
        pinnedEdgeWidth = 0;
      }
    }
    mAnimationController.setInsetsAndAlpha(Insets.of(leftInset, mStatusBarHeight, pinnedEdgeWidth, mNavigationBarFrameHeight), 1.0f, 1.0f);
  }

  private void setWindowInsetsController() {
    View decorView = mDecorView;
    if (decorView != null && mAnimationController == null && mWindowInsetsController == null) {
      mWindowInsetsController = decorView.getWindowInsetsController();
    }
  }

  /**
   * Configures decor/system-window fitting and bar visibility based on immersive-scroll state.
   *
   * <p>When enabling immersive scroll this method disables {@code decorFitsSystemWindows} (if it
   * was initially enabled) so that insets can be controlled via the animation controller.
   */
  private void setupDecorsFitSystemWindowState(boolean canImmersiveScroll) {
    AppBarLayout appBarLayout;
    int statusBarHeight;

    if (mDecorView == null || (appBarLayout = mAppBarLayout) == null || useCustomAnimationCallback) {
      return;
    }
    if (mContext == null) {
      Context context = appBarLayout.getContext();
      mContext = context;
      if (context == null) {
        return;
      }
    }
    Activity activity = SeslContextUtils.getActivity(mContext);
    if (activity == null && (appBarLayout = mAppBarLayout) != null) {
      mContext = appBarLayout.getContext();
      activity = SeslContextUtils.getActivity(mAppBarLayout.getContext());
    }
    if (activity != null) {
      Window window = activity.getWindow();

      if (canImmersiveScroll) {

        WindowInsets decorViewInset = mDecorViewInset;
        if (decorViewInset == null || !isHideCameraCutout(decorViewInset)) {
          mAppBarLayout.setImmersiveTopInset(mStatusBarHeight);
        } else {
          mAppBarLayout.setImmersiveTopInset(0);
        }

        //Custom
        if (mInitFitsSystemWindows == UNSET) {
          mInitFitsSystemWindows = window.getDecorView().getFitsSystemWindows()
                  ? FITS_SYSTEM_WINDOWS_ENABLED : FITS_SYSTEM_WINDOWS_DISABLED;
          if (mInitFitsSystemWindows == FITS_SYSTEM_WINDOWS_ENABLED) {
            window.setDecorFitsSystemWindows(false);
            window.getDecorView().setFitsSystemWindows(false);
          }
        }

        if (decorViewInset == null
                || (statusBarHeight = decorViewInset.getInsets(WindowInsets.Type.statusBars()).top) == 0
                || statusBarHeight == mStatusBarHeight
        ) {
          return;
        }
        mStatusBarHeight = statusBarHeight;
        mAppBarLayout.setImmersiveTopInset(statusBarHeight);

      } else {

        mAppBarLayout.setImmersiveTopInset(0);

        //Custom
        if (mInitFitsSystemWindows == FITS_SYSTEM_WINDOWS_ENABLED) {
          window.setDecorFitsSystemWindows(true);
          window.getDecorView().setFitsSystemWindows(true);
          mInitFitsSystemWindows = UNSET;
        }

        if (!isNavigationBarBottomPosition() && isLandscape()) {
          if (mWindowInsetsController == null) {
            setWindowInsetsController();
          }

          WindowInsets decorViewInset = mDecorView.getRootWindowInsets();
          mDecorViewInset = decorViewInset;
          if (mWindowInsetsController != null && decorViewInset != null) {
            if (mDecorViewInset.getInsets(WindowInsets.Type.statusBars()).top != 0) {
              try {
                mWindowInsetsController.hide(WindowInsets.Type.statusBars());
              } catch (IllegalStateException e) {
                Log.w(TAG, "setupDecorsFitSystemWindowState: mWindowInsetsController.hide failed!");
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void layoutChild(
          @NonNull CoordinatorLayout parent, @NonNull AppBarLayout child, int layoutDirection) {
    super.layoutChild(parent, child, layoutDirection);

    if (mWindowInsetsController != null && mOnInsetsChangedListener == null) {
      OnControllableInsetsChangedListener listener = new OnControllableInsetsChangedListener() {
                public void onControllableInsetsChanged(@NonNull WindowInsetsController controller, @InsetsType int typeMask) {

                  if (isLandscape() && !isNavigationBarBottomPosition() && !mCalledHideShowOnLayoutChild) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.show(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    mCalledHideShowOnLayoutChild = true;
                  }

                  if (mIsSetAutoRestore && typeMask == WindowInsets.Type.ime()) {
                    mDecorViewInset = mDecorView.getRootWindowInsets();
                    if (mDecorViewInset != null && mDecorViewInset.isVisible(WindowInsets.Type.statusBars()) && isAppBarHide()) {
                      restoreTopAndBottom(true);
                    }
                  }
                }
              };

      mOnInsetsChangedListener = listener;
      mWindowInsetsController.addOnControllableInsetsChangedListener(listener);
    }

    AppBarLayout cachedAppBarLayout = mAppBarLayout;
    if (child != cachedAppBarLayout || mNeedInit) {
      initImmViews(parent, child);
    }
  }

  /**
   * Requests control over status and navigation bar insets animations.
   *
   * <p>This creates/uses a {@link CancellationSignal} and starts an indefinite
   * {@link WindowInsetsController#controlWindowInsetsAnimation(int, long, android.view.animation.Interpolator, CancellationSignal, WindowInsetsAnimationControlListener)}
   * request so that {@link #mOffsetChangedListener} can drive insets updates.
   */
  private void startAnimationControlRequest() {
    setWindowInsetsController();
    if (mCancellationSignal == null) {
      mCancellationSignal = new CancellationSignal();
    }
    int topBottomBars = WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars();
    if (!isHideCameraCutout(mDecorViewInset)) {
      try {
        mWindowInsetsController.hide(topBottomBars);
      } catch (IllegalStateException unused) {
        Log.w(TAG, "startAnimationControlRequest: mWindowInsetsController.hide failed!");
      }
    }
    mWindowInsetsController.setSystemBarsBehavior(2);
    mWindowInsetsController.controlWindowInsetsAnimation(topBottomBars, -1L, null, mCancellationSignal, mWindowInsetsAnimationControlListener);
  }

  private void startRestoreAnimation() {
    if (isAppBarHide()) {
      animateRestoreTopAndBottom(mCoordinatorLayout, mAppBarLayout, -mAppBarLayout.getUpNestedPreScrollRange());
    }
  }

  private void updateAppBarHeightProportion() {
    AppBarLayout appBarLayout = mAppBarLayout;
    if (appBarLayout == null) {
      return;
    }
    if (mContext == null) {
      Context context = appBarLayout.getContext();
      mContext = context;
      if (context == null) {
        return;
      }
    }
    Resources resources = mContext.getResources();
    float appBarProPortion = SeslAppBarHelper.Companion.getAppBarProPortion(mContext);
    float f = 0.0f;
    if (appBarProPortion != 0.0f) {
      f = ((float) mStatusBarHeight / resources.getDisplayMetrics().heightPixels) + appBarProPortion;
    }
    if (mCanImmersiveScroll) {
      mAppBarLayout.internalProportion(f);
    } else {
      mAppBarLayout.internalProportion(appBarProPortion);
    }
  }

  private boolean updateOrientationState() {
    if (mAppBarLayout != null) {
      final int currentOrientation
              = mAppBarLayout.getCurrentOrientation();

      if (mPrevOrientation != currentOrientation) {
        mPrevOrientation = currentOrientation;
        forceRestoreWindowInset(true);
        mCalledHideShowOnLayoutChild = false;
      }

      switch (currentOrientation) {
        case Configuration.ORIENTATION_PORTRAIT:
          return true;
        case Configuration.ORIENTATION_LANDSCAPE:
          return false;
        default:
          Log.e(TAG,
                  "ERROR, e : AppbarLayout Configuration is wrong");
          return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Updates cached system bar dimensions.
   *
   * <p>Values are primarily obtained from {@link WindowInsets} when available; otherwise falls back
   * to platform resources.
   */
  private void updateSystemBarsHeight() {
    Context context = mContext;
    if (context == null) {
      return;
    }
    Resources resources = context.getResources();

    int statusBarHeightResourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
    if (statusBarHeightResourceId > 0) {
      mStatusBarHeight = resources.getDimensionPixelSize(statusBarHeightResourceId);
    }

    int navigationBarHeightResourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    if (navigationBarHeightResourceId > 0) {
      mNavigationBarBgHeight = resources.getDimensionPixelSize(navigationBarHeightResourceId);
    }

    View decorView = mDecorView;

    if (decorView != null) {
      WindowInsets rootWindowInsets = decorView.getRootWindowInsets();
      mDecorViewInset = rootWindowInsets;
      if (rootWindowInsets != null) {
        mStatusBarHeight = rootWindowInsets.getInsets(WindowInsets.Type.statusBars()).top;
        int navigationBarHeight = mDecorViewInset.getInsets(WindowInsets.Type.navigationBars()).bottom;
        mNavigationBarFrameHeight = navigationBarHeight;
        mNavigationBarBgHeight = navigationBarHeight;
        if (mNavigationBarBg == null) {
          mNavigationBarBgHeight = 0;
        }
      }
    }
  }

  /**
   * Cancels any in-progress insets animation control request and finishes the controller.
   *
   * <p>The controller is finished in the state that matches visibility at the time of cancellation
   * (captured into {@link #mShownAtDown}).
   */
  public void cancelWindowInsetsAnimationController() {
    View decorView = mDecorView;
    if (decorView != null) {
      WindowInsets rootWindowInsets = decorView.getRootWindowInsets();
      mDecorViewInset = rootWindowInsets;
      if (rootWindowInsets != null) {
        mShownAtDown = rootWindowInsets.isVisible(
                WindowInsets.Type.statusBars()) || mDecorViewInset.isVisible(WindowInsets.Type.navigationBars());
      }
    }
    WindowInsetsAnimationController controller = mAnimationController;
    if (controller != null) {
      controller.finish(mShownAtDown);
    }
    CancellationSignal cancellationSignal = mCancellationSignal;
    if (cancellationSignal != null) {
      cancellationSignal.cancel();
    }
    resetWindowInsetsAnimationController();
  }

  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
    boolean z = motionEvent.getToolType(0) == 3;
    if (mToolIsMouse != z) {
      mToolIsMouse = z;
      AppBarLayout appBarLayout = mAppBarLayout;
      if (appBarLayout != null) {
        appBarLayout.seslSetIsMouse(z);
        dispatchImmersiveScrollEnabled();
      }
    }
    return super.dispatchGenericMotionEvent(motionEvent);
  }

  /**
   * Re-evaluates whether immersive scroll can run and applies any related configuration.
   *
   * @return {@code true} if immersive scroll is currently allowed and activated.
   */
  public boolean dispatchImmersiveScrollEnabled() {
    AppBarLayout appBarLayout = mAppBarLayout;
    if (appBarLayout == null || appBarLayout.isDetachedState()) {
      return false;
    }
    boolean canImmersiveScroll = canImmersiveScroll();
    setupDecorsFitSystemWindowState(canImmersiveScroll);
    updateAppBarHeightProportion();
    updateSystemBarsHeight();
    return canImmersiveScroll;
  }

  /**
   * Forces system bars to be shown if they are hidden or if the app bar is currently hidden.
   *
   * @param forceRestore If {@code true}, always request showing bars.
   */
  public void forceRestoreWindowInset(boolean forceRestore) {
    if (mWindowInsetsController != null) {
      mDecorViewInset = mDecorView.getRootWindowInsets();
      if (mDecorViewInset != null) {
        boolean isStatusBarsVisible = mDecorViewInset.isVisible(WindowInsets.Type.statusBars());
        boolean isNavigationBarsVisible = mDecorViewInset.isVisible(WindowInsets.Type.navigationBars());
        if (!isStatusBarsVisible || !isNavigationBarsVisible || isAppBarHide() || forceRestore) {
          try {
            mWindowInsetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
          } catch (IllegalStateException unused) {
            Log.w(TAG, "forceRestoreWindowInset: mWindowInsetsController.show failed!");
          }
        }
      }
    }
  }

  public boolean getCanImmersiveScrollState() {
    return mCanImmersiveScroll;
  }

  /**
   * Initializes references to the relevant views (decor/content/system bar backgrounds) and hooks
   * offset/insets callbacks.
   *
   * <p>This is called from {@link #layoutChild(CoordinatorLayout, AppBarLayout, int)} when a new
   * {@link AppBarLayout} instance is encountered or after {@link #release()}.
   */
  public void initImmViews(
      @NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout appBarLayout) {
    Log.d(TAG, "initImmViews mNeedInit=" + mNeedInit);
    int i = 0;
    mNeedInit = false;
    mCanImmersiveScroll = false;
    mAppBarLayout = appBarLayout;
    mCoordinatorLayout = coordinatorLayout;
    appBarLayout.addOnOffsetChangedListener(mOffsetChangedListener);
    if (!mAppBarLayout.isImmersiveActivatedByUser() && !isDexEnabled()) {
      mAppBarLayout.internalActivateImmersiveScroll(true, false);
    }
    View decorView = mAppBarLayout.getRootView();
    mDecorView = decorView;
    View contentView = decorView.findViewById(android.R.id.content);
    mContentView = contentView;

    if (useCustomAnimationCallback) {
      contentView.setWindowInsetsAnimationCallback(mCustomWindowInsetsAnimation);
    } else {
      contentView.setWindowInsetsAnimationCallback(mWindowAnimationCallback);
    }

    findSystemBarsBackground();
    dispatchImmersiveScrollEnabled();

    while (true) {
      if (i >= appBarLayout.getChildCount()) {
        break;
      }
      View childAt = appBarLayout.getChildAt(i);
      if (mCollapsingToolbarLayout != null) {
        break;
      }
      if (childAt instanceof CollapsingToolbarLayout) {
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) childAt;
        break;
      }
      i++;
    }

    View bottomArea = coordinatorLayout.findViewById(R.id.bottom_bar_overlay);
    if (mBottomArea == null || bottomArea != null) {
      mBottomArea = bottomArea;
    }
  }

  /** @return Whether the app bar is currently considered hidden/collapsed past its collapsed height. */
  public boolean isAppBarHide() {
    if (mAppBarLayout != null) {
      return mAppBarLayout.getPaddingBottom() + mAppBarLayout.getBottom() < mAppBarLayout.seslGetCollapsedHeight();
    }
    return false;
  }

  /**
   * Notifies the behavior that window insets have been applied/changed.
   *
   * <p>This cancels any running controller, refreshes cached system-bar heights and dispatches an
   * offset update to keep the immersive offset in sync.
   */
  public void notifyOnApplyWindowInsets() {
    if (mAppBarLayout != null) {
      cancelWindowInsetsAnimationController();
      updateSystemBarsHeight();
      mAppBarLayout.onOffsetChanged(getTopAndBottomOffset());
    }
  }

  /**
   * Notifies the behavior that the associated app bar has been detached.
   *
   * <p>Removes any listeners registered on the {@link WindowInsetsController} and clears controller
   * state.
   */
  public void notifyOnDetachedFromWindow() {
    Log.i(TAG, "DetachedFromWindow");
    OnControllableInsetsChangedListener controller = mOnInsetsChangedListener;
    if (controller != null) {
      mWindowInsetsController.removeOnControllableInsetsChangedListener(controller);
      mOnInsetsChangedListener = null;
    }
    resetWindowInsetsAnimationController();
  }

  /**
   * Releases transient references and callbacks.
   *
   * <p>After calling this, the behavior will reinitialize itself on the next layout pass.
   */
  public void release() {
    String str = TAG;
    Log.d(str, "release");
    if (mAnimationHandler.hasMessages(MSG_APPEAR_ANIMATION)) {
      Log.d(str, "release removeMessages");
      mAnimationHandler.removeMessages(MSG_APPEAR_ANIMATION);
    }
    View contentView = mContentView;
    if (contentView != null) {
      contentView.setWindowInsetsAnimationCallback(null);
    }
    mTargetView = null;
    mNeedInit = true;
  }

  /**
   * Restores (shows) the top and bottom areas after they have been hidden by immersive scroll.
   *
   * @param animate Whether to animate the restoration.
   */
  public void restoreTopAndBottom(boolean animate) {
    AppBarLayout appBarLayout;
    Log.i(TAG, " Restore top and bottom areas [Animate] " + animate);
    mNeedRestoreAnim = animate;
    if (mAppBarLayout != null && isAppBarHide()) {
      if (mAnimationHandler.hasMessages(MSG_APPEAR_ANIMATION)) {
        mAnimationHandler.removeMessages(MSG_APPEAR_ANIMATION);
      }
      mAnimationHandler.sendEmptyMessageDelayed(MSG_APPEAR_ANIMATION, 100L);
    }
    if (mBottomArea == null || mNavigationBarBg == null
            || mAnimationHandler.hasMessages(MSG_APPEAR_ANIMATION)
            || (appBarLayout = mAppBarLayout) == null
            || appBarLayout.seslIsActivatedImmsersiveScroll()
    ) {
      return;
    }
    mBottomArea.setTranslationY(0.0f);
  }

  /**
   * Enables/disables automatic restoration when system bars become controllable again.
   */
  public void setAutoRestoreTopAndBottom(boolean autoRestore) {
    mIsSetAutoRestore = autoRestore;
  }

  /**
   * Sets an optional bottom overlay view that should translate together with the navigation bar.
   *
   * <p>Typically this is a bottom bar container identified by {@code R.id.bottom_bar_overlay}.
   */
  public void setBottomView(@Nullable View view) {
    mBottomArea = view;
  }

  /**
   * Controls whether the behavior should adjust the bottom view translation using current
   * navigation bar insets when immersive scroll is disabled.
   */
  public void setNeedToCheckBottomViewMargin(boolean checkBottomViewMargin) {
    mNeedToCheckBottomViewMargin = checkBottomViewMargin;
  }

  /**
   * Sets a custom {@link WindowInsetsAnimation.Callback} on the content view.
   *
   * <p>Providing a custom callback disables immersive scroll, as the insets animation will be
   * driven externally.
   */
  public void setWindowInsetsAnimationCallback(
      @NonNull AppBarLayout appBarLayout, @Nullable WindowInsetsAnimation.Callback callback) {
    if (mContentView == null) {
      View rootView = appBarLayout.getRootView();
      mDecorView = rootView;
      mContentView = rootView.findViewById(android.R.id.content);
    }
    if (callback == null) {
      useCustomAnimationCallback = false;
    } else {
      mCustomWindowInsetsAnimation = callback;
      useCustomAnimationCallback = true;
    }
    if (!useCustomAnimationCallback) {
      mContentView.setPadding(0, 0, 0, 0);
      mContentView.setWindowInsetsAnimationCallback(mWindowAnimationCallback);
      return;
    }
    mContentView.setWindowInsetsAnimationCallback(mCustomWindowInsetsAnimation);
    prepareImmersiveScroll(false, false);
    View view = mBottomArea;
    if (view != null) {
      view.setTranslationY(0.0f);
    }
  }

  /**
   * Restores {@code decorFitsSystemWindows} when immersive scroll has been detached/disabled.
   */
  public void setupDecorFitsSystemWindow() {
    Log.i(TAG, "fits system window Immersive detached");
    Activity activity = SeslContextUtils.getActivity(mContext);
    if (activity != null && mAppBarLayout != null) {
      activity.getWindow().setDecorFitsSystemWindows(true);
      View view = mBottomArea;
      if (view != null) {
        view.setTranslationY(0.0f);
      }
    }
    View statusBarBg = mStatusBarBg;
    if (statusBarBg == null || statusBarBg.getTranslationY() == 0.0f) {
      return;
    }
    statusBarBg.setTranslationY(0.0f);
  }

  @Override
  public boolean onInterceptTouchEvent(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout appBarLayout, @NonNull MotionEvent motionEvent) {
    int toolType = motionEvent.getToolType(0);
    if (toolType == 0) {
      return super.onInterceptTouchEvent(coordinatorLayout, appBarLayout, motionEvent);
    }
    boolean isMouse = toolType == TOOL_TYPE_MOUSE;
    if (mToolIsMouse != isMouse) {
      mToolIsMouse = isMouse;
      appBarLayout.seslSetIsMouse(isMouse);
    }
    return super.onInterceptTouchEvent(coordinatorLayout, appBarLayout, motionEvent);
  }


  @Override
  public boolean onMeasureChild(@NonNull CoordinatorLayout parent, @NonNull AppBarLayout child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
    dispatchImmersiveScrollEnabled();
    return super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
  }

  @Override
  public void onNestedPreScroll(CoordinatorLayout parent, @NonNull AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
    mTargetView = target;
    if (mCancellationSignal == null) {
      super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type);
    } else {
      consumed[0] = dx;
      consumed[1] = dy;
    }
  }

  @Override
  public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout appBarLayout, @NonNull View target, int dxConsumed, int i2, int i3, int dyUnconsumed, int type, @NonNull int[] consumed) {
    mTargetView = target;
    super.onNestedScroll(coordinatorLayout, appBarLayout, target, dxConsumed, i2, i3, dyUnconsumed, type, consumed);
  }

  @Override
  public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout appBarLayout, @NonNull View view, @NonNull View view2, int i, int i2) {
    mTargetView = view2;
    if (dispatchImmersiveScrollEnabled() && mAnimationController == null) {
      startAnimationControlRequest();
    }
    return super.onStartNestedScroll(coordinatorLayout, appBarLayout, view, view2, i, i2);
  }

  @Override
  public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout appBarLayout, View view, int i) {
    mTargetView = view;
    super.onStopNestedScroll(coordinatorLayout, appBarLayout, view, i);
  }
}