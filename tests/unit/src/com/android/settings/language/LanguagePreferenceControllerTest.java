/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.language;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Test;

public class LanguagePreferenceControllerTest {
    private Context mContext;
    private LanguagePreferenceController mController;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new LanguagePreferenceController(mContext, "key");
    }

    @Test
    public void getAvailabilityStatus_featureFlagOff_LanguageAndInputSettingsActivitydisabled() {
        mController.getAvailabilityStatus();

        assertFalse(isActivityEnable(mContext, Settings.LanguageAndInputSettingsActivity.class));
        assertTrue(isActivityEnable(mContext, Settings.LanguageSettingsActivity.class));
    }

    private static boolean isActivityEnable(Context context, Class klazz) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName =
                new ComponentName(context, klazz);
        int flag = packageManager.getComponentEnabledSetting(componentName);
        return flag == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}
