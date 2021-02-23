/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.deviceinfo.hardwareinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.HardwareInfoPreferenceController;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class HardwareInfoPreferenceControllerTest {

    private static final String KEY = "device_model";

    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    @Mock
    private Resources mResources;
    private HardwareInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getResources()).thenReturn(mResources);
        mController = new HardwareInfoPreferenceController(mContext, KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_returnTrueIfVisible() {
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool", "config_show_device_model");

        when(mResources.getBoolean(boolId)).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_returnFalseIfNotVisible() {
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool", "config_show_device_model");

        when(mResources.getBoolean(boolId)).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updatePreference_summaryShouldContainBuildModel() {
        mController.updateState(mPreference);

        assertThat(containBuildModel(mPreference.getSummary())).isTrue();
    }

    private boolean containBuildModel(CharSequence result) {
        return result.toString().contains(Build.MODEL);
    }
}
