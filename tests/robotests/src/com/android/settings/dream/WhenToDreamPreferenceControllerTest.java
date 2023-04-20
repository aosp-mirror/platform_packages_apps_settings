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

package com.android.settings.dream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WhenToDreamPreferenceControllerTest {
    private static final String TEST_PACKAGE = "com.android.test";

    private WhenToDreamPreferenceController mController;
    private Context mContext;
    @Mock
    private DreamBackend mBackend;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new WhenToDreamPreferenceController(mContext, true, true);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mContext.getSystemService(PowerManager.class)).thenReturn(mPowerManager);
        when(mPowerManager.isAmbientDisplaySuppressedForTokenByApp(anyString(), anyInt()))
                .thenReturn(false);

        mApplicationInfo.uid = 1;
        when(mContext.getString(
                com.android.internal.R.string.config_defaultWellbeingPackage)).thenReturn(
                TEST_PACKAGE);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getApplicationInfo(TEST_PACKAGE, /* flag= */ 0)).thenReturn(
                mApplicationInfo);
    }

    @Test
    public void testUpdateSummary() {
        // Don't have to test the other settings because DreamSettings tests that all
        // @WhenToDream values map to the correct ResId
        final @WhenToDream int testSetting = DreamBackend.WHILE_CHARGING;
        final Preference mockPref = mock(Preference.class);
        when(mockPref.getContext()).thenReturn(mContext);
        when(mBackend.getWhenToDreamSetting()).thenReturn(testSetting);
        final int expectedResId = DreamSettings.getDreamSettingDescriptionResId(testSetting, true);

        mController.updateState(mockPref);
        verify(mockPref).setSummary(expectedResId);
    }

    @Test
    public void testBedtimeModeSuppression() {
        final Preference mockPref = mock(Preference.class);
        when(mockPref.getContext()).thenReturn(mContext);
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.WHILE_CHARGING);
        when(mPowerManager.isAmbientDisplaySuppressedForTokenByApp(anyString(), anyInt()))
                .thenReturn(true);

        assertTrue(AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime(mContext));

        mController.updateState(mockPref);
        verify(mockPref).setSummary(R.string.screensaver_settings_when_to_dream_bedtime);
    }
}
