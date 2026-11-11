/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.google.android.material.motion;

import com.google.android.material.test.R;

import static com.google.common.truth.Truth.assertThat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build.VERSION_CODES;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.activity.BackEventCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.UPSIDE_DOWN_CAKE)
@DoNotInstrument
public class MaterialBottomContainerBackHelperTest {

  private static final int HEIGHT = 100;

  // Progress 0 is what the back key (rather than a gesture) reports.
  private final BackEventCompat backEvent =
      new BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT);

  @Test
  public void finishBackProgressNotPersistent_translatedBelowItsHeight_doesNotThrow() {
    ApplicationProvider.getApplicationContext().setTheme(R.style.Theme_Material3_DayNight);
    AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
    FrameLayout view = new FrameLayout(activity);
    activity.setContentView(view, new LayoutParams(LayoutParams.MATCH_PARENT, HEIGHT));
    view.layout(0, 0, HEIGHT, HEIGHT);
    assertThat(view.getRootWindowInsets()).isNotNull();
    // Offset like InsetsAnimationCallback does for a soft input taller than the view.
    view.setTranslationY(3 * HEIGHT);

    assertThat(finishAndGetDuration(view)).isEqualTo(0);
  }

  @Test
  public void finishBackProgressNotPersistent_detachedView_doesNotThrow() {
    FrameLayout view = new FrameLayout(ApplicationProvider.getApplicationContext());
    view.layout(0, 0, HEIGHT, HEIGHT);

    assertThat(finishAndGetDuration(view)).isAtLeast(0);
  }

  private long finishAndGetDuration(FrameLayout view) {
    long[] duration = {-1};
    new MaterialBottomContainerBackHelper(view)
        .finishBackProgressNotPersistent(
            backEvent,
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationStart(Animator animation) {
                duration[0] = animation.getDuration();
              }
            });
    return duration[0];
  }
}
