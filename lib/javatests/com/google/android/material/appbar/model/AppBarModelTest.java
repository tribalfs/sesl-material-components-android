package com.google.android.material.appbar.model;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.material.appbar.model.view.AppBarView;
import kotlin.jvm.internal.ClassReference;
import kotlin.reflect.KClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link AppBarModel}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.N)
public class AppBarModelTest {

  /** A minimal app bar view, built the way suggestion views are: (Context, AttributeSet). */
  public static class TestAppBarView extends AppBarView {
    public TestAppBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
    }

    @Override
    public void inflate() {}

    @Override
    public void updateResource(@NonNull Context context) {}
  }

  @Test
  public void create_buildsTheViewWithoutKotlinReflection() {
    Context context = ApplicationProvider.getApplicationContext();
    // ClassReference is the KClass an app gets without kotlin-reflect: its constructors throw
    // KotlinReflectionNotSupportedError, which create() used to hit.
    @SuppressWarnings("unchecked")
    KClass<TestAppBarView> type = (KClass<TestAppBarView>) (KClass<?>) new ClassReference(TestAppBarView.class);
    AppBarModel<TestAppBarView> model = new AppBarModel<>(type, context);

    TestAppBarView view = model.create();

    assertThat(view).isInstanceOf(TestAppBarView.class);
    assertThat(view.getContext()).isSameInstanceAs(context);
  }
}
