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

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_UNDEFINED;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static com.google.android.material.snackbar.Snackbar.SESL_SNACKBAR_TYPE_DEFAULT;
import static com.google.android.material.snackbar.Snackbar.SESL_SNACKBAR_TYPE_SUGGESTION;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.core.content.ContextCompat;
import androidx.core.view.SeslTouchTargetDelegate;
import androidx.core.view.ViewCompat;

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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar.SeslSnackBarType;

import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.reflect.view.inputmethod.SeslInputMethodManagerReflector;

/**
 * <b>SESL variant</b><br><br>
 *
 * @hide */
@RestrictTo(LIBRARY_GROUP)
public class SnackbarContentLayout extends LinearLayout implements ContentViewCallback {
  //Sesl
  private final InputMethodManager mImm;
  private final SnackbarContentLayout mSnackBarContentLayout;
  private final WindowManager mWindowManager;
  private int mLastOrientation = ORIENTATION_UNDEFINED;
  private int mWidthWithAction;
  private int maxWidth;
  private boolean mIsCoordinatorLayoutParent = false;
  //Sesl

  //Sesl7
  private boolean mIsSuggestMultiLine = false;
  private int mType = SESL_SNACKBAR_TYPE_DEFAULT;
  //sesl7

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

    final int widthPixels = res.getDisplayMetrics().widthPixels;

    maxWidth
        = mWidthWithAction
            = (int) res.getFraction(R.dimen.sesl_config_prefSnackWidth, widthPixels, widthPixels);

    mSnackBarContentLayout = findViewById(R.id.snackbar_content_layout);
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
    if (actionView.getVisibility() == VISIBLE
            && /*sesl7*/(mType != SESL_SNACKBAR_TYPE_SUGGESTION || mIsSuggestMultiLine)) {
      //Remeasure to specified exact width
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mWidthWithAction, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else /*sesl*/if ((measuredWidth = getMeasuredWidth()) == 0) {
      widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
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

    //Sesl7
    if (isMultiLine) {
      mIsSuggestMultiLine = true;
    }
    //sesl7

    if (mSnackBarContentLayout == null /*sesl*/) {
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
      final float totalWidth = (float)(mSnackBarContentLayout.getPaddingLeft() +
              mSnackBarContentLayout.getPaddingRight() +
              messageView.getMeasuredWidth() +
              actionView.getMeasuredWidth());

      if (maxInlineActionWidth == -1 && actionView.getVisibility() == VISIBLE) {
        if ((totalWidth > (float)mWidthWithAction) || isMultiLine) {
          mSnackBarContentLayout.setOrientation(VERTICAL);

          messageView.setPadding(
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_left),
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_top),
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_right),
                  resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_text_padding_bottom));

          LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) actionView.getLayoutParams();
          lp.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_margin_bottom));
          lp.setMarginEnd(resources.getDimensionPixelSize(R.dimen.sesl_design_snackbar_action_margin_end));
          actionView.setLayoutParams(lp);
        } else {
          mSnackBarContentLayout.setOrientation(HORIZONTAL);
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
    if (ViewCompat.isPaddingRelative(view)) {
      ViewCompat.setPaddingRelative(
          view,
          ViewCompat.getPaddingStart(view),
          topPadding,
          ViewCompat.getPaddingEnd(view),
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

  //Ses7
  protected void seslSetType(@SeslSnackBarType int type) {
    mType = type;
  }

  private boolean seslUpdateLayoutMarginsForPortrait(int totalWidth) {
    final MarginLayoutParams lp = (MarginLayoutParams) mSnackBarContentLayout.getLayoutParams();

    if (mIsCoordinatorLayoutParent){
      final ViewParent parent = mSnackBarContentLayout.getParent();
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
      lp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.sesl_design_snackbar_layout_padding_bottom);
    }

    if (mIsCoordinatorLayoutParent) {
      final ViewParent parent = mSnackBarContentLayout.getParent();
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
    mSnackBarContentLayout.setLayoutParams(lp);
    return true;
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
  //sesl7

}
