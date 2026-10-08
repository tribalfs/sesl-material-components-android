/*
 * Copyright 2026 The Android Open Source Project
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

import static android.text.Layout.HYPHENATION_FREQUENCY_NORMAL;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.material.internal.CollapsingTextHelper;
import com.google.android.material.test.R;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for the SESL behavior of {@link CollapsingToolbarLayout}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.N)
public class CollapsingToolbarLayoutTest {

  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    context.setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
  }

  @Test
  public void constructor_specificMaxLinesTakePrecedenceOverLegacyMaxLines() {
    AttributeSet attributes =
        collapsingTitleAttributes()
            .addAttribute(R.attr.maxLines, "2")
            .addAttribute(R.attr.titleMaxLines, "3")
            .addAttribute(R.attr.subtitleMaxLines, "4")
            .build();

    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context, attributes);

    assertThat(layout.collapsingTitleHelper.getExpandedMaxLines()).isEqualTo(3);
    assertThat(layout.collapsingSubtitleHelper.getExpandedMaxLines()).isEqualTo(4);
  }

  @Test
  public void constructor_explicitTitleColorsTakePrecedenceOverTextAppearances() {
    int expandedColor = Color.rgb(18, 52, 86);
    int collapsedColor = Color.rgb(101, 67, 33);
    AttributeSet attributes =
        collapsingTitleAttributes()
            .addAttribute(
                R.attr.expandedTitleTextAppearance,
                "@style/TextAppearance.Test.CollapsingToolbar.Green")
            .addAttribute(
                R.attr.collapsedTitleTextAppearance,
                "@style/TextAppearance.Test.CollapsingToolbar.Green")
            .addAttribute(R.attr.expandedTitleTextColor, "#123456")
            .addAttribute(R.attr.collapsedTitleTextColor, "#654321")
            .build();

    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context, attributes);

    assertThat(layout.collapsingTitleHelper.getExpandedTextColor().getDefaultColor())
        .isEqualTo(expandedColor);
    assertThat(layout.collapsingTitleHelper.getCollapsedTextColor().getDefaultColor())
        .isEqualTo(collapsedColor);
  }

  @Test
  public void constructor_extendedTitleAppearanceDoesNotStyleCollapsingTitle() {
    CollapsingToolbarLayout defaultLayout =
        new CollapsingToolbarLayout(context, collapsingTitleAttributes().build());
    AttributeSet attributes =
        collapsingTitleAttributes()
            .addAttribute(
                R.attr.extendedTitleTextAppearance,
                "@style/TextAppearance.Test.CollapsingToolbar.Extended")
            .build();

    CollapsingToolbarLayout customExtendedLayout =
        new CollapsingToolbarLayout(context, attributes);

    assertThat(customExtendedLayout.getExpandedTitleTextSize())
        .isEqualTo(defaultLayout.getExpandedTitleTextSize());
  }

  @Test
  public void constructor_readsIndividualMarginsWhenTitlesAreDisabled() {
    AttributeSet attributes =
        Robolectric.buildAttributeSet()
            .addAttribute(R.attr.titleEnabled, "false")
            .addAttribute(R.attr.extendedTitleEnabled, "false")
            .addAttribute(R.attr.expandedTitleMarginStart, "11px")
            .addAttribute(R.attr.expandedTitleMarginTop, "12px")
            .addAttribute(R.attr.expandedTitleMarginEnd, "13px")
            .addAttribute(R.attr.expandedTitleMarginBottom, "14px")
            .addAttribute(R.attr.expandedTitleSpacing, "15px")
            .build();

    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context, attributes);

    assertThat(layout.getExpandedTitleMarginStart()).isEqualTo(11);
    assertThat(layout.getExpandedTitleMarginTop()).isEqualTo(12);
    assertThat(layout.getExpandedTitleMarginEnd()).isEqualTo(13);
    assertThat(layout.getExpandedTitleMarginBottom()).isEqualTo(14);
    assertThat(layout.getExpandedTitleSpacing()).isEqualTo(15);
  }

  @Test
  public void getSubTitle_returnsNullWhenExistingSubtitleIsHidden() {
    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context);
    layout.seslSetSubtitle("subtitle");
    assertThat(layout.getSubTitle().toString()).isEqualTo("subtitle");

    layout.seslSetSubtitle((CharSequence) null);

    assertThat(layout.getSubTitle()).isNull();
  }

  @Test
  public void extendedTitle_usesNormalHyphenationAtMinimumSdk() {
    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context);
    TextView extendedTitle = layout.findViewById(R.id.collapsing_appbar_extended_title);

    assertThat(extendedTitle.getHyphenationFrequency()).isEqualTo(HYPHENATION_FREQUENCY_NORMAL);
  }

  @Test
  public void entireSpaceCollapsedBounds_useStartMarginForLtrLeftBound() throws Exception {
    AttributeSet attributes =
        collapsingTitleAttributes()
            .addAttribute(R.attr.collapsedTitleGravityMode, "0")
            .build();
    CollapsingToolbarLayout layout = new CollapsingToolbarLayout(context, attributes);
    layout.setTitle("title");
    Toolbar toolbar = new Toolbar(context);
    toolbar.setTitleMarginStart(12);
    toolbar.setTitleMarginEnd(30);
    layout.addView(toolbar, new CollapsingToolbarLayout.LayoutParams(300, 100));

    int widthSpec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY);
    int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    layout.measure(widthSpec, heightSpec);
    layout.layout(0, 0, 300, 200);

    Field field = CollapsingTextHelper.class.getDeclaredField("collapsedBoundsForPlacement");
    field.setAccessible(true);
    Rect placementBounds = (Rect) field.get(layout.collapsingTitleHelper);
    assertThat(placementBounds).isNotNull();
    assertThat(placementBounds.left).isEqualTo(12);
  }

  private static Robolectric.AttributeSetBuilder collapsingTitleAttributes() {
    return Robolectric.buildAttributeSet()
        .addAttribute(R.attr.titleEnabled, "true")
        .addAttribute(R.attr.extendedTitleEnabled, "false");
  }
}
