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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.testutils.shadow.ShadowActivityEmbeddingUtils;
import com.android.settings.testutils.shadow.ShadowPasswordUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class,
        SettingsHomepageActivityTest.ShadowSuggestionFeatureProviderImpl.class})
public class SettingsHomepageActivityTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        ShadowPasswordUtils.reset();
    }

    @Test
    public void launch_shouldHaveAnimationForIaFragment() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final FrameLayout frameLayout = activity.findViewById(R.id.main_content);

        assertThat(frameLayout.getLayoutTransition()).isNotNull();
    }

    @Test
    public void launch_configDisabled_shouldHideAvatar() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();

        final View avatarView = activity.findViewById(R.id.account_avatar);
        assertThat(avatarView.getVisibility()).isNotEqualTo(View.VISIBLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void launch_configEnabled_shouldShowAvatar() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();

        final View avatarView = activity.findViewById(R.id.account_avatar);
        assertThat(avatarView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void launch_LowRamDevice_shouldHideAvatar() {
        final ShadowActivityManager activityManager = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(true);

        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();

        final View avatarView = activity.findViewById(R.id.account_avatar);
        assertThat(avatarView.getVisibility()).isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void showHomepageWithSuggestion_showSuggestion() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final View viewRoot = activity.findViewById(R.id.settings_homepage_container);
        final View suggestionTile = activity.findViewById(R.id.suggestion_content);

        activity.showHomepageWithSuggestion(true);

        assertThat(viewRoot.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(suggestionTile.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void showHomepageWithSuggestion_hideSuggestion() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final View viewRoot = activity.findViewById(R.id.settings_homepage_container);
        final View suggestionTile = activity.findViewById(R.id.suggestion_content);

        activity.showHomepageWithSuggestion(false);

        assertThat(viewRoot.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(suggestionTile.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void showHomepageWithSuggestion_callTwice_shouldKeepPreviousVisibility() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final View suggestionTile = activity.findViewById(R.id.suggestion_content);

        activity.showHomepageWithSuggestion(false);
        activity.showHomepageWithSuggestion(true);

        assertThat(suggestionTile.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
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

    /** This test is for large screen devices Activity embedding. */
    @Test
    @Config(shadows = ShadowActivityEmbeddingUtils.class)
    public void onNewIntent_flagClearTop_shouldInitRules() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true);
        SettingsHomepageActivity activity =
                spy(Robolectric.buildActivity(SettingsHomepageActivity.class).get());
        doNothing().when(activity).reloadHighlightMenuKey();
        TopLevelSettings topLevelSettings = mock(TopLevelSettings.class);
        doReturn(topLevelSettings).when(activity).getMainFragment();

        activity.onNewIntent(new Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

        verify(activity).initSplitPairRules();
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void isCallingAppPermitted_emptyPermission_returnTrue() {
        SettingsHomepageActivity homepageActivity = spy(new SettingsHomepageActivity());

        assertTrue(homepageActivity.isCallingAppPermitted(""));
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void isCallingAppPermitted_noGrantedPermission_returnFalse() {
        SettingsHomepageActivity homepageActivity = spy(new SettingsHomepageActivity());

        assertFalse(homepageActivity.isCallingAppPermitted("android.permission.TEST"));
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void isCallingAppPermitted_grantedPermission_returnTrue() {
        SettingsHomepageActivity homepageActivity = spy(new SettingsHomepageActivity());
        String permission = "android.permission.TEST";
        ShadowPasswordUtils.addGrantedPermission(permission);

        assertTrue(homepageActivity.isCallingAppPermitted(permission));
    }

    @Implements(SuggestionFeatureProviderImpl.class)
    public static class ShadowSuggestionFeatureProviderImpl {

        @Implementation
        public Class<? extends Fragment> getContextualSuggestionFragment() {
            return Fragment.class;
        }
    }
}
