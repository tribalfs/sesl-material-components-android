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
package com.google.android.material.snackbar;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.view.SemBlurCompat.CANVAS_BLUR_USE_TYPE_DYNAMIC;
import static com.google.android.material.snackbar.Snackbar.SESL_SNACKBAR_TYPE_DEFAULT;
import static com.google.android.material.snackbar.Snackbar.SESL_SNACKBAR_TYPE_SUGGESTION;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.graphics.Outline;
import android.os.Build;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.SemBlurCompat;
import androidx.core.view.SeslTouchTargetDelegate;
import androidx.reflect.feature.SeslFloatingFeatureReflector;
import androidx.reflect.view.inputmethod.SeslInputMethodManagerReflector;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.snackbar.Snackbar.SeslSnackBarType;

/** @hide <b>SESL variant.</b>*/
@RestrictTo(LIBRARY_GROUP)
public class SnackbarContentLayout extends LinearLayout implements ContentViewCallback {
  //Sesl
  private final InputMethodManager mImm;
  private final SnackbarContentLayout mSnackBarContentLayout;
  private final WindowManager mWindowManager;
  private int mWidthWithAction;
  private int maxWidth;
  private boolean mIsCoordinatorLayoutParent;
  //Sesl

  //Sesl7
  private boolean mIsSuggestMultiLine;
  private int mType = SESL_SNACKBAR_TYPE_DEFAULT;
  private int mDefaultActionMarginBottom;
  private int mDefaultActionMarginEnd;
  //sesl7

  //Sesl9
  private boolean mIsBlurApplied = true;
  @SemBlurCompat.SeslBlurMode
  private int mBlurMode = SemBlurCompat.BLUR_MODE_CANVAS;
  //sesl9

  private TextView messageView;
  private Button actionView;
  @Nullable private Button closeView;
  private final TimeInterpolator contentInterpolator;

  private int maxInlineActionWidth;

  public SnackbarContentLayout(@NonNull Context context) {
    this(context, null);
  }

  public SnackbarContentLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    //Sesl
    contentInterpolator =
        MotionUtils.resolveThemeInterpolator(
            context,
            R.attr.motionEasingEmphasizedInterpolator,
            AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);//sesl9
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SnackbarLayout);
    maxWidth = a.getDimensionPixelSize(R.styleable.SnackbarLayout_android_maxWidth, -1);
    maxInlineActionWidth =
        a.getDimensionPixelSize(R.styleable.SnackbarLayout_maxActionInlineWidth, -1);
    a.recycle();

    final Resources res = context.getResources();

    int availableWidth = res.getDisplayMetrics().widthPixels - seslGetHorizontalInsets();

    maxWidth
        = mWidthWithAction
        = (int) res.getFraction(R.dimen.sesl_config_prefSnackWidth, availableWidth, availableWidth);

    mSnackBarContentLayout = findViewById(R.id.snackbar_content_layout);
    mImm = context.getSystemService(InputMethodManager.class);
    mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    seslSetTouchDelegateForSnackBar();
    //sesl
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    messageView = findViewById(R.id.snackbar_text);
    actionView = findViewById(R.id.snackbar_action);
    closeView = findViewById(R.id.mtrl_snackbar_close);
    //sesl9
    if (actionView != null) {
      LayoutParams lp = (LayoutParams) actionView.getLayoutParams();
      this.mDefaultActionMarginEnd = lp.getMarginEnd();
      this.mDefaultActionMarginBottom = lp.bottomMargin;
    }
  }

  public TextView getMessageView() {
    return messageView;
  }

  public Button getActionView() {
    return actionView;
  }

  @Nullable
  public Button getCloseView() {
    return closeView;
  }

  void updateActionTextColorAlphaIfNeeded(float actionTextColorAlpha) {
    if (actionTextColorAlpha != 1) {
      int originalActionTextColor = actionView.getCurrentTextColor();
      int colorSurface = MaterialColors.getColor(this, R.attr.colorSurface);
      int actionTextColor =
          MaterialColors.layer(colorSurface, originalActionTextColor, actionTextColorAlpha);
      actionView.setTextColor(actionTextColor);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    //Sesl9
    boolean remeasure;
    int widthWithActionMeasureSpec = MeasureSpec.makeMeasureSpec(mWidthWithAction, MeasureSpec.AT_MOST);
    int initialWidth = getMeasuredWidth();
    super.onMeasure(widthWithActionMeasureSpec, heightMeasureSpec);

    if (actionView.getVisibility() == VISIBLE
        && (mType != SESL_SNACKBAR_TYPE_SUGGESTION || mIsSuggestMultiLine)) {
      widthWithActionMeasureSpec = MeasureSpec.makeMeasureSpec(mWidthWithAction, MeasureSpec.EXACTLY);
      super.onMeasure(widthWithActionMeasureSpec, heightMeasureSpec);
    } else if (getMeasuredWidth() == 0) {
      widthWithActionMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
      super.onMeasure(widthWithActionMeasureSpec, heightMeasureSpec);
    } else if (maxWidth > 0) {
      int measuredWidth = getMeasuredWidth();
      if (measuredWidth > maxWidth) {
        widthWithActionMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
        super.onMeasure(widthWithActionMeasureSpec, heightMeasureSpec);
      }
    }

    final int multiLineVPadding =
        getResources().getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical_2lines);
    final int singleLineVPadding =
        getResources().getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical);
    final Layout messageLayout = messageView.getLayout();
    final boolean isMultiLine = messageLayout != null && messageLayout.getLineCount() > 1;
    mIsSuggestMultiLine = isMultiLine;

    boolean shouldUpdateMargins = true;

    if (mSnackBarContentLayout != null) {
      float contentWidth =
          actionView.getMeasuredWidth()
              + messageView.getMeasuredWidth()
              + mSnackBarContentLayout.getPaddingRight()
              + mSnackBarContentLayout.getPaddingLeft();

      if (maxInlineActionWidth == -1 && actionView.getVisibility() == VISIBLE) {
        if (contentWidth > mWidthWithAction || isMultiLine || mIsSuggestMultiLine) {
          mSnackBarContentLayout.setOrientation(VERTICAL);
          messageView.setPadding(
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_left),
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_top),
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_right),
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_bottom));
          LayoutParams actionLp = (LayoutParams) actionView.getLayoutParams();
          actionLp.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_margin_bottom));
          actionLp.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_margin_end));
          actionView.setLayoutParams(actionLp);
        } else {
          mSnackBarContentLayout.setOrientation(HORIZONTAL);
          actionView.setPadding(
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_left),
              0,
              getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_right),
              0);
          LayoutParams actionLp = (LayoutParams) actionView.getLayoutParams();
          actionLp.setMargins(actionLp.leftMargin, actionLp.topMargin, actionLp.rightMargin, mDefaultActionMarginBottom);
          actionLp.setMarginEnd(mDefaultActionMarginEnd);
          actionView.setLayoutParams(actionLp);
        }
        remeasure = true;
      } else {
        remeasure = false;
      }

      int displayRotation = mWindowManager.getDefaultDisplay().getRotation();
      if (displayRotation != 1 && displayRotation != 3) {
        shouldUpdateMargins = false;
      }
      shouldUpdateMargins =
          ((mImm == null || !shouldUpdateMargins)
              ? seslUpdateLayoutMarginsForPortrait((int) contentWidth)
              : seslUpdateLayoutMarginsForLandscape((int) contentWidth)) | remeasure;
    } else if (isMultiLine
        && maxInlineActionWidth > 0
        && actionView.getMeasuredWidth() > maxInlineActionWidth) {
      if (!updateViewsWithinLayout(
          VERTICAL, multiLineVPadding, multiLineVPadding - singleLineVPadding)) {
        shouldUpdateMargins = false;
      }
    } else {
      final int messagePadding = isMultiLine ? multiLineVPadding : singleLineVPadding;
      if (!updateViewsWithinLayout(HORIZONTAL, messagePadding, messagePadding)) {
        shouldUpdateMargins = false;
      }
    }

    if (shouldUpdateMargins) {
      super.onMeasure(widthWithActionMeasureSpec, heightMeasureSpec);
      if (mSnackBarContentLayout != null && mType == SESL_SNACKBAR_TYPE_SUGGESTION) {
        clipSnackBarContentLayout();
      }
    }
    //sesl9
  }

  private boolean updateViewsWithinLayout(
      final int orientation, final int messagePadTop, final int messagePadBottom) {
    boolean changed = false;
    if (orientation != getOrientation()) {
      setOrientation(orientation);
      changed = true;
    }
    if (messageView.getPaddingTop() != messagePadTop
        || messageView.getPaddingBottom() != messagePadBottom) {
      updateTopBottomPadding(messageView, messagePadTop, messagePadBottom);
      changed = true;
    }
    return changed;
  }

  private static void updateTopBottomPadding(
      @NonNull View view, int topPadding, int bottomPadding) {
    if (view.isPaddingRelative()) {
      view.setPaddingRelative(
          view.getPaddingStart(),
          topPadding,
          view.getPaddingEnd(),
          bottomPadding);
    } else {
      view.setPadding(view.getPaddingLeft(), topPadding, view.getPaddingRight(), bottomPadding);
    }
  }

  @Override
  public void animateContentIn(int delay, int duration) {
    messageView.setAlpha(0f);
    messageView.animate().alpha(1f).setDuration(duration).
        setInterpolator(contentInterpolator).setStartDelay(delay).start();

    if (actionView.getVisibility() == VISIBLE) {
      actionView.setAlpha(0f);
      actionView.animate().alpha(1f).setDuration(duration).
          setInterpolator(contentInterpolator).setStartDelay(delay).start();
    }
  }

  @Override
  public void animateContentOut(int delay, int duration) {
    messageView.setAlpha(1f);
    messageView.animate().alpha(0f).setDuration(duration).
        setInterpolator(contentInterpolator).setStartDelay(delay).start();

    if (actionView.getVisibility() == VISIBLE) {
      actionView.setAlpha(1f);
      actionView.animate().alpha(0f).setDuration(duration).
          setInterpolator(contentInterpolator).setStartDelay(delay).start();
    }
  }

  public void setMaxInlineActionWidth(int width) {
    maxInlineActionWidth = width;
  }

  //Sesl
  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    //Sesl9
    Resources res = getContext().getResources();
    int availableWidth = res.getDisplayMetrics().widthPixels - seslGetHorizontalInsets();
    int fraction =
        (int) res.getFraction(R.dimen.sesl_config_prefSnackWidth, availableWidth, availableWidth);
    this.mWidthWithAction = fraction;
    this.maxWidth = fraction;
    //sesl9
  }

  private void seslSetTouchDelegateForSnackBar() {
    final ViewTreeObserver vto = getViewTreeObserver();
    if (vto != null) {
      vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          getViewTreeObserver().removeOnGlobalLayoutListener(this);
          if (mSnackBarContentLayout != null) {
            if (actionView != null
                && actionView.getVisibility() == VISIBLE) {
              mSnackBarContentLayout.post(new Runnable() {
                @Override
                public void run() {
                  SeslTouchTargetDelegate delegate = new SeslTouchTargetDelegate(mSnackBarContentLayout);
                  final int margin = actionView.getMeasuredHeight() / 2;
                  delegate.addTouchDelegate(actionView,
                      SeslTouchTargetDelegate.ExtraInsets.of(margin, margin, margin, margin));
                  mSnackBarContentLayout.setTouchDelegate(delegate);
                }
              });
            }
          }
        }
      });
    }
  }
  //sesl

  //Sesl7
  protected void seslSetType(@SeslSnackBarType int type) {
    mType = type;
  }

  //Sesl9
  public void invalidateSnackbarContentLayout() {
    if (this.mSnackBarContentLayout != null) {
      this.mSnackBarContentLayout.invalidate();
    }
  }

  public boolean isBlurApplied() {
    return this.mIsBlurApplied;
  }

  public boolean isWindowBlurApplied() {
    return this.mIsBlurApplied && this.mBlurMode == SemBlurCompat.BLUR_MODE_WINDOW;
  }

  public boolean seslApplyBlurInfo(boolean disableBlur, @SemBlurCompat.SeslBlurMode int blurMode) {
    this.mIsBlurApplied = !disableBlur;
    this.mBlurMode = blurMode;
    if (disableBlur) {
      return false;
    }
    return applySnackBarBlur(blurMode);
  }

  private boolean applySnackBarBlur(@SemBlurCompat.SeslBlurMode int blurMode) {
    if (Build.VERSION.SDK_INT >= 35) {
      if (this.mSnackBarContentLayout != null) {
        String surfaceTransitionFlag =
            SeslFloatingFeatureReflector.getString(
                "SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG", "FALSE");
        if (!"FALSE".equalsIgnoreCase(surfaceTransitionFlag)) {
          boolean windowBlurApplied = isWindowBlurApplied();
          if (SemBlurCompat.setBlurEffectPreset(
              this.mSnackBarContentLayout,
              blurMode,
              new SemBlurCompat.CurveParameter(240, 0.4f, -15.0f, 0.0f, 235.0f, 31.2f, 112.8f),
              windowBlurApplied ? SemBlurCompat.BLUR_MODE_WINDOW : null,
              windowBlurApplied
                  ? (float) getContext().getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_suggest_background_radius)
                  : null,
              CANVAS_BLUR_USE_TYPE_DYNAMIC)) {
            clipSnackBarContentLayout();
            this.mSnackBarContentLayout.setBackgroundTintList(ColorStateList.valueOf(0));
            this.mSnackBarContentLayout.invalidate();
            return true;
          }
        }
      }
    }
    return false;
  }

  private void clipSnackBarContentLayout() {
    final float cornerRadius = getContext().getResources().getDimensionPixelSize(R.dimen.sesl_design_snackbar_suggest_background_radius);
    if (mSnackBarContentLayout != null) {
      mSnackBarContentLayout.setOutlineProvider(new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
          outline.setRoundRect(0, 0, mSnackBarContentLayout.getMeasuredWidth(), mSnackBarContentLayout.getMeasuredHeight(), cornerRadius);
        }
      });
      mSnackBarContentLayout.setClipToOutline(true);
    }
  }
  //sesl9

  private boolean seslUpdateLayoutMarginsForPortrait(int totalWidth) {
    final MarginLayoutParams lp = (MarginLayoutParams) mSnackBarContentLayout.getLayoutParams();

    if (mIsCoordinatorLayoutParent) {
      final ViewParent parent = mSnackBarContentLayout.getParent();
      if (parent instanceof ViewGroup viewGroup) {
        final int measuredWidth = viewGroup.getMeasuredWidth();
        final int paddingLeft = viewGroup.getPaddingLeft();
        final int paddingRight = viewGroup.getPaddingRight();
        final int totalMarginToSet = ((measuredWidth - Math.min(mWidthWithAction, totalWidth)) - paddingLeft) - paddingRight;
        if (totalMarginToSet > 0) {
          final int sideMargin = totalMarginToSet / 2;
          lp.rightMargin = sideMargin;
          lp.leftMargin = sideMargin;
        } else {
          lp.rightMargin = 0;
          lp.leftMargin = 0;
        }
        mSnackBarContentLayout.setLayoutParams(lp);
        return true;
      }
    }
    return false;
  }

  private boolean seslUpdateLayoutMarginsForLandscape(int totalWidth) {
    final MarginLayoutParams lp = (MarginLayoutParams) mSnackBarContentLayout.getLayoutParams();

    if (SeslInputMethodManagerReflector.isInputMethodShown(mImm)) {
      lp.bottomMargin = seslGetNavibarHeight();
    } else {
      lp.bottomMargin =
          getResources()
              .getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_padding_bottom);
    }

    if (mIsCoordinatorLayoutParent) {
      final ViewParent parent = mSnackBarContentLayout.getParent();
      if (parent instanceof ViewGroup) {
        final ViewGroup viewGroup = (ViewGroup) parent;
        final int measuredWidth = viewGroup.getMeasuredWidth();
        final int paddingLeft = viewGroup.getPaddingLeft();
        final int paddingRight = viewGroup.getPaddingRight();
        final int totalMarginToSet =
            ((measuredWidth - Math.min(mWidthWithAction, totalWidth)) - paddingLeft) - paddingRight;
        if (totalMarginToSet > 0) {
          final int sideMargin = totalMarginToSet / 2;
          lp.rightMargin = sideMargin;
          lp.leftMargin = sideMargin;
        } else {
          lp.rightMargin = 0;
          lp.leftMargin = 0;
        }
      }
    }
    mSnackBarContentLayout.setLayoutParams(lp);
    return true;
  }

  void setIsCoordinatorLayoutParent(boolean isCoordinatorLayoutParent) {
    this.mIsCoordinatorLayoutParent = isCoordinatorLayoutParent;
  }

  private int seslGetHorizontalInsets() {
    if (Build.VERSION.SDK_INT < 30 || mWindowManager == null) {
      return 0;
    }
    Insets insets =
        mWindowManager
            .getCurrentWindowMetrics()
            .getWindowInsets()
            .getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
    return insets.left + insets.right;
  }

  private int seslGetNavibarHeight() {
    if (Build.VERSION.SDK_INT < 30) {
      return getResources()
          .getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_sip_padding_bottom);
    }
    //Sesl9
    try {
      int navBarHeight =
          mWindowManager
              .getCurrentWindowMetrics()
              .getWindowInsets()
              .getInsets(WindowInsets.Type.navigationBars())
              .bottom;
      return navBarHeight == 0
          ? getResources()
          .getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_sip_padding_bottom)
          : navBarHeight;
    } catch (Exception unused) {
      return getResources()
          .getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_sip_padding_bottom);
    }
    //sesl9
  }
}
