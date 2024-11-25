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

import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import androidx.core.content.ContextCompat;
import androidx.core.view.SeslTouchTargetDelegate;

import android.os.Build;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.reflect.view.inputmethod.SeslInputMethodManagerReflector;

import com.google.android.material.color.MaterialColors;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

/**
 * <b>SESL variant</b><br><br>
 *
 * @hide */
@RestrictTo(LIBRARY_GROUP)
public class SnackbarContentLayout extends LinearLayout implements ContentViewCallback {
  //Sesl
  private InputMethodManager mImm;
  private SnackbarContentLayout mSnackBarLayout;
  private WindowManager mWindowManager;
  private int mLastOrientation = Configuration.ORIENTATION_UNDEFINED;
  private int mWidthWithAction;
  private int maxWidth;
  private boolean mIsCoordinatorLayoutParent = false;
  //Sesl

  private TextView messageView;
  private Button actionView;

  private int maxInlineActionWidth;

  public SnackbarContentLayout(@NonNull Context context) {
    this(context, null);
  }

  public SnackbarContentLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    //Sesl
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SnackbarLayout);
    maxWidth = a.getDimensionPixelSize(R.styleable.SnackbarLayout_android_maxWidth, -1);
    maxInlineActionWidth =
        a.getDimensionPixelSize(R.styleable.SnackbarLayout_maxActionInlineWidth, -1);
    a.recycle();

    final Resources res = context.getResources();

    maxWidth
        = mWidthWithAction
            = (int) res.getFraction(R.dimen.sesl_config_prefSnackWidth,
                res.getDisplayMetrics().widthPixels, res.getDisplayMetrics().widthPixels);

    mSnackBarLayout = findViewById(R.id.snackbar_content_layout);
    mImm = ContextCompat.getSystemService(context, InputMethodManager.class);
    mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    seslSetTouchDelegateForSnackBar();
    //sesl
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    messageView = findViewById(R.id.snackbar_text);
    actionView = findViewById(R.id.snackbar_action);
  }

  public TextView getMessageView() {
    return messageView;
  }

  public Button getActionView() {
    return actionView;
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
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (getOrientation() == VERTICAL) {
        // The layout is by default HORIZONTAL. We only change it to VERTICAL when the action view
        // is too wide and ellipsizes the message text. When the condition is met, we should keep the
        // layout as VERTICAL.
        return;
    }

    //Sesl
    int measuredWidth;
    if (actionView.getVisibility() == VISIBLE) {
      //Remeasure to specified exact width
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mWidthWithAction, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else /*sesl*/if ((measuredWidth = getMeasuredWidth()) == 0) {
      widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    else  {
      //Remeasure to exact max width if it exceeds the max width
      if (maxWidth > 0 && getMeasuredWidth() > maxWidth) {
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      }
    }
    //sesl

    Resources resources = getResources();
    final int multiLineVPadding =
            resources.getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical_2lines);
    final int singleLineVPadding =
            resources.getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical);
    final Layout messageLayout = messageView.getLayout();
    final boolean isMultiLine = messageLayout != null && messageLayout.getLineCount() > 1;

    boolean remeasure = false;

    if (mSnackBarLayout == null) {
      if (isMultiLine
              && maxInlineActionWidth > 0
              && actionView.getMeasuredWidth() > maxInlineActionWidth) {
        remeasure |= updateViewsWithinLayout(
                VERTICAL, multiLineVPadding, multiLineVPadding - singleLineVPadding);
      } else {
        final int messagePadding = isMultiLine ? multiLineVPadding : singleLineVPadding;
        remeasure |= updateViewsWithinLayout(HORIZONTAL, messagePadding, messagePadding);
      }
    } else {
      //Sesl
      final float totalWidth = (float)(mSnackBarLayout.getPaddingLeft() +
              mSnackBarLayout.getPaddingRight() +
              messageView.getMeasuredWidth() +
              actionView.getMeasuredWidth());

      if (maxInlineActionWidth == -1 && actionView.getVisibility() == VISIBLE) {
        if ((totalWidth > (float)mWidthWithAction) || isMultiLine) {
          mSnackBarLayout.setOrientation(VERTICAL);

          actionView.setPadding(
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_left),
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_top),
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_left),
                  0);
        } else {
          mSnackBarLayout.setOrientation(HORIZONTAL);
          actionView.setPadding(
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_left),
                  0,
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_padding_right),
                  0);
        }
        remeasure = true;
      }

      if (mImm != null) {
        final int displayRotation = mWindowManager.getDefaultDisplay().getRotation();
        final boolean isLandscape = displayRotation == ROTATION_90 || displayRotation == ROTATION_270;
        if (isLandscape) {
          remeasure |= seslUpdateLayoutMarginsForLandscape((int) totalWidth);
        }else{
          remeasure |= seslUpdateLayoutMarginsForPortrait((int) totalWidth);
        }
      }else{
        remeasure |= seslUpdateLayoutMarginsForPortrait((int) totalWidth);
      }
      //sesl
    }

    if (remeasure) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

  }


  private boolean seslUpdateLayoutMarginsForPortrait(int totalWidth) {
    final MarginLayoutParams lp = (MarginLayoutParams) mSnackBarLayout.getLayoutParams();

    if (mIsCoordinatorLayoutParent){
      final ViewParent parent = mSnackBarLayout.getParent();
      if (parent instanceof ViewGroup) {
        final ViewGroup viewGroup = (ViewGroup) parent;
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
        mSnackBarLayout.setLayoutParams(lp);
        return true;
      }
    }
    return false;
  }


  private boolean seslUpdateLayoutMarginsForLandscape(int totalWidth) {
    final MarginLayoutParams lp = (MarginLayoutParams) mSnackBarLayout.getLayoutParams();

    if (SeslInputMethodManagerReflector.isInputMethodShown(mImm)) {
      lp.bottomMargin = seslGetNavibarHeight();
    } else {
      lp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_padding_bottom);
    }

    if (mIsCoordinatorLayoutParent) {
      final ViewParent parent = mSnackBarLayout.getParent();
      if (parent instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) parent;
        final int measuredWidth = viewGroup.getMeasuredWidth();
        final int paddingLeft = viewGroup.getPaddingLeft();
        final int paddingRight = viewGroup.getPaddingRight();
        final int totalMarginToSet = ((measuredWidth - Math.min(mWidthWithAction, totalWidth)) - paddingLeft) - paddingRight;
        if (totalMarginToSet > 0) {
          final int margin = totalMarginToSet / 2;
          lp.rightMargin = margin;
          lp.leftMargin = margin;
        } else {
          lp.rightMargin = 0;
          lp.leftMargin = 0;
        }
      }
    }
    mSnackBarLayout.setLayoutParams(lp);
    return true;
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
    messageView.animate().alpha(1f).setDuration(duration)
        .setStartDelay(delay).start();//sesl

    if (actionView.getVisibility() == VISIBLE) {
      actionView.setAlpha(0f);
      actionView.animate().alpha(1f).setDuration(duration)
          .setStartDelay(delay).start();//sesl
    }
  }

  @Override
  public void animateContentOut(int delay, int duration) {
    messageView.setAlpha(1f);
    messageView.animate().alpha(0f).setDuration(duration)
        .setStartDelay(delay).start();//sesl

    if (actionView.getVisibility() == VISIBLE) {
      actionView.setAlpha(1f);
      actionView.animate().alpha(0f).setDuration(duration)
          .setStartDelay(delay).start();//sesl
    }
  }

  public void setMaxInlineActionWidth(int width) {
    maxInlineActionWidth = width;
  }

  //Sesl
  @Override
  protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mLastOrientation != newConfig.orientation) {
      final Resources res = getContext().getResources();
      maxWidth
          = mWidthWithAction
          = (int) res.getFraction(R.dimen.sesl_config_prefSnackWidth,
          res.getDisplayMetrics().widthPixels, res.getDisplayMetrics().widthPixels);
      mLastOrientation = newConfig.orientation;
    }
  }

  private void seslSetTouchDelegateForSnackBar() {
    final ViewTreeObserver vto = getViewTreeObserver();
    if (vto != null) {
      vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          getViewTreeObserver().removeOnGlobalLayoutListener(this);
          if (mSnackBarLayout != null) {
            if (actionView != null
                    && actionView.getVisibility() == VISIBLE) {
              mSnackBarLayout.post(new Runnable() {
                @Override
                public void run() {
                  SeslTouchTargetDelegate delegate = new SeslTouchTargetDelegate(mSnackBarLayout);
                  final int margin = actionView.getMeasuredHeight() / 2;
                  delegate.addTouchDelegate(actionView,
                          SeslTouchTargetDelegate.ExtraInsets.of(margin, margin, margin, margin));
                  mSnackBarLayout.setTouchDelegate(delegate);
                }
              });
            }
          }
        }
      });
    }
  }

  protected void setIsCoordinatorLayoutParent(boolean isCoordinatorLayoutParent) {
    mIsCoordinatorLayoutParent = isCoordinatorLayoutParent;
  }

  private int seslGetNavibarHeight() {
    if (Build.VERSION.SDK_INT >= 30) {
      try {
        final android.graphics.Insets insets = mWindowManager.getCurrentWindowMetrics()
                .getWindowInsets()
                .getInsets(WindowInsets.Type.navigationBars());
        if (insets.bottom != 0) {
          return insets.bottom;
        }
      } catch (Exception ignored) {}
    }
    return getResources().getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_sip_padding_bottom);

  }
  //sesl
}
