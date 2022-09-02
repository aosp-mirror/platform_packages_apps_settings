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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TopLevelSettingsTest {
    private Context mContext;
    private TopLevelSettings mSettings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new TopLevelSettings());
        when(mSettings.getContext()).thenReturn(mContext);
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(null);
        mSettings.onAttach(mContext);
    }

    @Test
    public void shouldForceRoundedIcon_true() {
        assertThat(mSettings.shouldForceRoundedIcon()).isTrue();
    }

    @Test
    public void onCreatePreferences_shouldTintPreferenceIcon() {
        final Preference preference = new Preference(mContext);
        preference.setTitle(R.string.network_dashboard_title);
        final Drawable icon = spy(mContext.getDrawable(R.drawable.ic_settings_wireless));
        preference.setIcon(icon);
        final PreferenceScreen screen = spy(new PreferenceScreen(mContext, null /* attrs */));
        doReturn(1).when(screen).getPreferenceCount();
        doReturn(preference).when(screen).getPreference(anyInt());
        doReturn(screen).when(mSettings).getPreferenceScreen();
        doReturn(new PreferenceManager(mContext)).when(mSettings).getPreferenceManager();
        doReturn(0).when(mSettings).getPreferenceScreenResId();

        mSettings.onCreatePreferences(new Bundle(), "rootKey");

        verify(icon).setTint(anyInt());
    }
}
