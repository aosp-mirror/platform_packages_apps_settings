/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class WallpaperPreferenceControllerTest {
    private static final String TEST_KEY = "test_key";

    private Intent mWallpaperIntent;
    private Intent mStylesAndWallpaperIntent;
    private FragmentActivity mContext;
    private ShadowPackageManager mShadowPackageManager;

    private WallpaperPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Robolectric.buildActivity(FragmentActivity.class).get();
        SettingsShadowResources.overrideResource(
                R.string.config_wallpaper_picker_package, "bogus.package.for.testing");
        SettingsShadowResources.overrideResource(
                R.string.config_styles_and_wallpaper_picker_class, "bogus.package.class");
        mWallpaperIntent =  new Intent().setComponent(new ComponentName(
                mContext.getString(R.string.config_wallpaper_picker_package),
                mContext.getString(R.string.config_wallpaper_picker_class)));
        mStylesAndWallpaperIntent = new Intent().setComponent(new ComponentName(
                mContext.getString(R.string.config_wallpaper_picker_package),
                mContext.getString(R.string.config_styles_and_wallpaper_picker_class)));
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new WallpaperPreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void isAvailable_wallpaperPickerEnabled_shouldReturnTrue() {
        mShadowPackageManager.setResolveInfosForIntent(
                mWallpaperIntent, Lists.newArrayList(mock(ResolveInfo.class)));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_wallpaperPickerDisabled_shouldReturnFalse() {
        mShadowPackageManager.setResolveInfosForIntent(
                mWallpaperIntent, Lists.newArrayList());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void areStylesAvailable_noComponentSpecified() {
        SettingsShadowResources.overrideResource(
                R.string.config_styles_and_wallpaper_picker_class, "");
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent, Lists.newArrayList());

        assertThat(mController.areStylesAvailable()).isFalse();
    }

    @Test
    public void areStylesAvailable_componentUnresolveable() {
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent, Lists.newArrayList());

        assertThat(mController.areStylesAvailable()).isFalse();
    }

    @Test
    public void areStylesAvailable_componentResolved() {
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent,
                Lists.newArrayList(mock(ResolveInfo.class)));

        assertThat(mController.areStylesAvailable()).isTrue();
    }

    @Test
    public void getKeywords_withoutStyles() {
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent, Lists.newArrayList());

        assertThat(mController.getKeywords())
                .contains(mContext.getString(R.string.keywords_wallpaper));
        assertThat(mController.getKeywords())
                .doesNotContain(mContext.getString(R.string.keywords_styles));
    }

    @Test
    public void getKeywords_withStyles() {
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent,
                Lists.newArrayList(mock(ResolveInfo.class)));

        assertThat(mController.areStylesAvailable()).isTrue();
        assertThat(mController.getKeywords())
                .contains(mContext.getString(R.string.keywords_wallpaper));
        assertThat(mController.getKeywords())
                .contains(mContext.getString(R.string.keywords_styles));
    }

    @Test
    public void handlePreferenceTreeClick_wallpaperOnly() {
        mShadowPackageManager.setResolveInfosForIntent(
                mWallpaperIntent, Lists.newArrayList(mock(ResolveInfo.class)));
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent, Lists.newArrayList());
        Preference preference = new Preference(mContext);
        preference.setKey(TEST_KEY);

        mController.handlePreferenceTreeClick(preference);

        assertThat(Shadows.shadowOf(mContext)
                .getNextStartedActivityForResult().intent.getComponent().getClassName())
                .isEqualTo(mContext.getString(R.string.config_wallpaper_picker_class));
    }

    @Test
    public void handlePreferenceTreeClick_stylesAndWallpaper() {
        mShadowPackageManager.setResolveInfosForIntent(
                mWallpaperIntent, Lists.newArrayList());
        mShadowPackageManager.setResolveInfosForIntent(
                mStylesAndWallpaperIntent, Lists.newArrayList(mock(ResolveInfo.class)));
        Preference preference = new Preference(mContext);
        preference.setKey(TEST_KEY);

        mController.handlePreferenceTreeClick(preference);

        assertThat(Shadows.shadowOf(mContext)
                .getNextStartedActivityForResult().intent.getComponent().getClassName())
                .isEqualTo(mContext.getString(R.string.config_styles_and_wallpaper_picker_class));
    }
}
