/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingsPreferenceCategoryControllerTest {
    private static final String KEY = "telephony_satellite_settings_category_key";
    private static final int TEST_SUB_ID = 0;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext = null;
    private SatelliteSettingsPreferenceCategoryController mController = null;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new SatelliteSettingsPreferenceCategoryController(mContext, KEY);
    }

    @Test
    @Ignore("b/382664790")
    public void getAvailabilityStatus_default_returnUnsupported() {
        int result = mController.getAvailabilityStatus(TEST_SUB_ID);
        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @Ignore("b/382664790")
    public void setPreferenceTitle_hasSmsService_showMessaging() {
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(KEY);
        preferenceScreen.addPreference(preferenceCategory);
        preferenceCategory.addPreference(new Preference(mContext));
        mController.displayPreference(preferenceScreen);

        assertEquals(preferenceCategory.getTitle(), "Satellite connectivity");
    }
}
