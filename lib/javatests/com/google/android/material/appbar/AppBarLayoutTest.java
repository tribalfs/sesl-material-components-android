/*
 * Copyright (C) 2022 The Android Open Source Project
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

import com.google.android.material.test.R;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.customview.view.AbsSavedState;
import com.google.android.material.appbar.AppBarLayout.LayoutParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.N)
public class AppBarLayoutTest {

  private AppBarLayout appBarLayout;
  private View firstScrollableChild;
  private View secondScrollableChild;
  private View fixedChild;
  private CoordinatorLayout coordinatorLayout;

  @Before
  public void setUp() {
    AppCompatActivity activity = Robolectric.buildActivity(TestActivity.class).setup().get();
    appBarLayout =
        (AppBarLayout) activity.getLayoutInflater().inflate(R.layout.test_appbarlayout, null);
    appBarLayout.setUseFloatingToolbar(false);
    firstScrollableChild = appBarLayout.findViewById(R.id.firstScrollableChild);
    secondScrollableChild = appBarLayout.findViewById(R.id.secondScrollableChild);
    fixedChild = appBarLayout.findViewById(R.id.fixedChild);

    activity.setContentView(appBarLayout);

    // Wait until the layout is measured.
    getInstrumentation().waitForIdleSync();
  }

  @Test
  public void testTotalScrollRange_whenFirstChildScrollableAndVisible_onlyCountFirstChild() {
    assertThat(appBarLayout.getTotalScrollRange())
        .isEqualTo(getChildScrollRange(firstScrollableChild));
  }

  @Test
  public void testTotalScrollRange_whenFirstChildNotExitUntilCollapsed_countFirstTwoChildren() {
    // Total scroll range will include all children until the first exit-until-collapsed child
    setExitUntilCollapsed(firstScrollableChild, false);

    assertThat(appBarLayout.getTotalScrollRange())
        .isEqualTo(
            getChildScrollRange(firstScrollableChild) + getChildScrollRange(secondScrollableChild));
  }

  @Test
  public void testTotalScrollRange_whenFirstChildGone_onlyCountSecondChild() {
    firstScrollableChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getTotalScrollRange())
        .isEqualTo(getChildScrollRange(secondScrollableChild));
  }

  @Test
  public void testTotalScrollRange_noVisibleScrollableChild_returnZero() {
    firstScrollableChild.setVisibility(View.GONE);
    secondScrollableChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getTotalScrollRange()).isEqualTo(0);
  }

  @Test
  public void testTotalScrollRange_whenFirstChildNotScrollable_returnZero() {
    appBarLayout.removeView(firstScrollableChild);
    appBarLayout.removeView(secondScrollableChild);
    appBarLayout.addView(firstScrollableChild);

    assertThat(appBarLayout.getTotalScrollRange()).isEqualTo(0);
  }

  @Test
  public void testDownNestedPreScrollRange_noEnterAlwaysChild_returnZero() {
    assertThat(appBarLayout.getDownNestedPreScrollRange()).isEqualTo(0);
  }

  @Test
  public void testDownNestedPreScrollRange_whenFirstChildEnterAlways_onlyCountFirstChild() {
    setEnterAlways(firstScrollableChild, true);
    secondScrollableChild.setVisibility(View.GONE);
    fixedChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedPreScrollRange())
        .isEqualTo(getChildDownNestedPreScrollRange(firstScrollableChild));
  }

  @Test
  public void testDownNestedPreScrollRange_whenFirstChildEnterAlwaysButGone_returnZero() {
    setEnterAlways(firstScrollableChild, true);
    firstScrollableChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedPreScrollRange()).isEqualTo(0);
  }

  @Test
  public void testDownNestedPreScrollRange_whenFirstChildGone_onlyCountSecondChild() {
    setEnterAlways(firstScrollableChild, true);
    firstScrollableChild.setVisibility(View.GONE);
    setEnterAlways(secondScrollableChild, true);
    fixedChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedPreScrollRange())
        .isEqualTo(getChildDownNestedPreScrollRange(secondScrollableChild));
  }

  @Test
  public void
      testDownNestedPreScrollRange_whenFirstChildEnterAlwaysCollapsed_onlyCountFirstChild() {
    setEnterAlways(firstScrollableChild, true);
    setEnterAlwaysCollapsed(firstScrollableChild, true);
    secondScrollableChild.setVisibility(View.GONE);
    fixedChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedPreScrollRange())
        .isEqualTo(getChildDownNestedPreScrollRange(firstScrollableChild));
  }

  @Test
  public void downNestedPreScrollRange_fixedChildAfterQuickReturnChild_stopsAtFixedChild() {
    setEnterAlways(firstScrollableChild, true);

    assertThat(appBarLayout.getDownNestedPreScrollRange()).isEqualTo(0);
  }

  @Test
  public void testDownNestedScrollRange_whenFirstChildScrollableAndVisible_onlyCountFirstChild() {
    assertThat(appBarLayout.getDownNestedScrollRange())
        .isEqualTo(getChildDownNestedScrollRange(firstScrollableChild));
  }

  @Test
  public void testDownNestedRange_whenFirstChildNotExitUntilCollapsed_countFirstTwoChildren() {
    // Down nested scroll range will include all children until the first exit-until-collapsed child
    setExitUntilCollapsed(firstScrollableChild, false);

    assertThat(appBarLayout.getDownNestedScrollRange())
        .isEqualTo(
            getChildDownNestedScrollRange(firstScrollableChild)
                + getChildDownNestedScrollRange(secondScrollableChild));
  }

  @Test
  public void testDownNestedScrollRange_whenFirstChildGone_onlyCountSecondChild() {
    firstScrollableChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedScrollRange())
        .isEqualTo(getChildDownNestedScrollRange(secondScrollableChild));
  }

  @Test
  public void testDownNestedScrollRange_noVisibleScrollableChild_returnZero() {
    firstScrollableChild.setVisibility(View.GONE);
    secondScrollableChild.setVisibility(View.GONE);

    assertThat(appBarLayout.getDownNestedScrollRange()).isEqualTo(0);
  }

  @Test
  public void testDownNestedScrollRange_whenFirstChildNotScrollable_returnZero() {
    appBarLayout.removeView(firstScrollableChild);
    appBarLayout.removeView(secondScrollableChild);
    appBarLayout.addView(firstScrollableChild);

    assertThat(appBarLayout.getDownNestedScrollRange()).isEqualTo(0);
  }

  @Test
  public void testSetScrollEffectNone_returnsNull() {
    AppBarLayout.LayoutParams lp =
        (AppBarLayout.LayoutParams) firstScrollableChild.getLayoutParams();
    lp.setScrollEffect(LayoutParams.SCROLL_EFFECT_NONE);

    assertThat(lp.getScrollEffect()).isEqualTo(null);
  }

  @Test
  public void testSetScrollEffectCompress() {
    AppBarLayout.LayoutParams lp =
        (AppBarLayout.LayoutParams) firstScrollableChild.getLayoutParams();
    lp.setScrollEffect(LayoutParams.SCROLL_EFFECT_COMPRESS);

    assertThat(lp.getScrollEffect()).isInstanceOf(AppBarLayout.CompressChildScrollEffect.class);
  }

  @Test
  public void setExpanded_clearsLiftHiddenState() {
    appBarLayout.seslSetLiftHided(true);

    appBarLayout.setExpanded(true, false);

    assertThat(appBarLayout.seslIsLiftHided()).isFalse();
  }

  @Test
  public void setCollapsedHeight_invalidatesAndRecalculatesTotalScrollRange() {
    int previousRange = appBarLayout.getTotalScrollRange();
    int collapsedHeight = 42;

    appBarLayout.seslSetCollapsedHeight(collapsedHeight);

    assertThat(appBarLayout.getTotalScrollRange()).isNotEqualTo(previousRange);
    assertThat(appBarLayout.getTotalScrollRange())
        .isEqualTo(
            getChildFullHeight(firstScrollableChild, getLayoutParams(firstScrollableChild))
                - collapsedHeight);
  }

  @Test
  public void minimumVisibleOverlap_whenDoubledMinimumExceedsHeight_usesSingleMinimum() {
    appBarLayout.layout(0, 0, 300, 100);
    appBarLayout.setMinimumHeight(60);

    assertThat(appBarLayout.getMinimumHeightForVisibleOverlappingContent()).isEqualTo(60);
  }

  @Test
  public void compressEffect_whenFullyCompressed_usesAlphaWithoutChangingVisibility() {
    AppBarLayout.CompressChildScrollEffect effect =
        new AppBarLayout.CompressChildScrollEffect();

    effect.onOffsetChanged(appBarLayout, firstScrollableChild, -appBarLayout.getHeight() * 2f);

    assertThat(firstScrollableChild.getVisibility()).isEqualTo(View.VISIBLE);
    assertThat(firstScrollableChild.getAlpha()).isEqualTo(0.0f);
  }

  @Test
  public void saveScrollState_whenFullyHidden_marksHiddenInsteadOfScrolled() {
    AppBarLayout.Behavior behavior = attachToCoordinatorLayout();
    behavior.setTopAndBottomOffset(-appBarLayout.getHeight());

    AppBarLayout.BaseBehavior.SavedState state = behavior.saveScrollState(null, appBarLayout);

    assertThat(state).isNotNull();
    assertThat(state.fullyHided).isTrue();
    assertThat(state.fullyScrolled).isFalse();
  }

  @Test
  public void restoreScrollState_whenSavedChildWasRemoved_doesNotCrash() {
    AppBarLayout.Behavior behavior = attachToCoordinatorLayout();
    AppBarLayout.BaseBehavior.SavedState state =
        new AppBarLayout.BaseBehavior.SavedState(AbsSavedState.EMPTY_STATE);
    state.firstVisibleChildIndex = appBarLayout.getChildCount();
    behavior.restoreScrollState(state, true);

    behavior.onLayoutChild(coordinatorLayout, appBarLayout, View.LAYOUT_DIRECTION_LTR);
  }

  private static int getChildScrollRange(View child) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    return getChildFullHeight(child, lp)
        - (isExitUntilCollapsed(lp) ? child.getMinimumHeight() : 0);
  }

  private static int getChildDownNestedPreScrollRange(View child) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (isEnterAlwaysCollapsed(lp)) {
      return child.getMinimumHeight() + lp.topMargin + lp.bottomMargin;
    }
    return getChildScrollRange(child);
  }

  private static int getChildDownNestedScrollRange(View child) {
    return getChildScrollRange(child);
  }

  private static int getChildFullHeight(View child, LayoutParams lp) {
    return child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
  }

  private static LayoutParams getLayoutParams(View child) {
    return (LayoutParams) child.getLayoutParams();
  }

  private AppBarLayout.Behavior attachToCoordinatorLayout() {
    ((ViewGroup) appBarLayout.getParent()).removeView(appBarLayout);
    coordinatorLayout = new CoordinatorLayout(appBarLayout.getContext());
    AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
    CoordinatorLayout.LayoutParams params =
        new CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setBehavior(behavior);
    coordinatorLayout.addView(appBarLayout, params);

    int widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY);
    int heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY);
    coordinatorLayout.measure(widthSpec, heightSpec);
    coordinatorLayout.layout(0, 0, 1080, 1920);
    return behavior;
  }

  private static void setExitUntilCollapsed(View child, boolean exitUntilCollapsed) {
    enableFlag(child, LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED, exitUntilCollapsed);
  }

  private static void setEnterAlwaysCollapsed(View child, boolean enterAlwaysCollapsed) {
    enableFlag(child, LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED, enterAlwaysCollapsed);
  }

  private static void setEnterAlways(View child, boolean enterAlways) {
    enableFlag(child, LayoutParams.SCROLL_FLAG_ENTER_ALWAYS, enterAlways);
  }

  private static void enableFlag(View child, int flag, boolean enable) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (enable) {
      lp.scrollFlags = lp.scrollFlags | flag;
    } else {
      lp.scrollFlags = lp.scrollFlags & ~flag;
    }
  }

  private static boolean isExitUntilCollapsed(LayoutParams lp) {
    return (lp.scrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0;
  }

  private static boolean isEnterAlwaysCollapsed(LayoutParams lp) {
    return (lp.scrollFlags & LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED) != 0;
  }

  private static class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      setTheme(R.style.Theme_Material3_Light_NoActionBar);
    }
  }
}
