/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settings.core.HideNonSystemOverlayMixin;
import com.android.settings.homepage.contextualcards.slices.BatteryFixSliceTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SettingsHomepageActivityTest {
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void setHomepageContainerPaddingTop_shouldBeSetPaddingTop() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final int searchBarHeight = activity.getResources().getDimensionPixelSize(
                R.dimen.search_bar_height);
        final int searchBarMargin = activity.getResources().getDimensionPixelSize(
                R.dimen.search_bar_margin);
        final View view = activity.findViewById(R.id.homepage_container);

        activity.setHomepageContainerPaddingTop();

        final int actualPaddingTop = view.getPaddingTop();
        assertThat(actualPaddingTop).isEqualTo(searchBarHeight + searchBarMargin * 2);
    }

    @Test
    public void launch_shouldHaveAnimationForIaFragment() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final FrameLayout frameLayout = activity.findViewById(R.id.main_content);

        assertThat(frameLayout.getLayoutTransition()).isNotNull();
    }

    @Test
    @Config(shadows = {
            BatteryFixSliceTest.ShadowBatteryStatsHelperLoader.class,
            BatteryFixSliceTest.ShadowBatteryTipLoader.class
    })
    public void onStart_isNotDebuggable_shouldHideSystemOverlay() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", false);

        final ActivityController<SettingsHomepageActivity> activityController =
                Robolectric.buildActivity(SettingsHomepageActivity.class).create();
        final SettingsHomepageActivity activity = spy(activityController.get());
        final Window window = mock(Window.class);
        when(activity.getWindow()).thenReturn(window);
        activity.getLifecycle().addObserver(new HideNonSystemOverlayMixin(activity));

        activityController.start();

        verify(window).addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Test
    @Config(shadows = {
            BatteryFixSliceTest.ShadowBatteryStatsHelperLoader.class,
            BatteryFixSliceTest.ShadowBatteryTipLoader.class,
    })
    public void onStop_isNotDebuggable_shouldRemoveHideSystemOverlay() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", false);

        final ActivityController<SettingsHomepageActivity> activityController =
                Robolectric.buildActivity(SettingsHomepageActivity.class).create();
        final SettingsHomepageActivity activity = spy(activityController.get());
        final Window window = mock(Window.class);
        when(activity.getWindow()).thenReturn(window);
        activity.getLifecycle().addObserver(new HideNonSystemOverlayMixin(activity));

        activityController.start();

        verify(window).addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        when(window.getAttributes()).thenReturn(layoutParams);

        activityController.stop();
        final ArgumentCaptor<WindowManager.LayoutParams> paramCaptor = ArgumentCaptor.forClass(
                WindowManager.LayoutParams.class);

        verify(window).setAttributes(paramCaptor.capture());
        assertThat(paramCaptor.getValue().privateFlags
                & SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS).isEqualTo(0);
    }
}
