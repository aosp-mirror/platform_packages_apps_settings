/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecurityAdvancedSettingsTest {
    private static final String SCREEN_XML_RESOURCE_NAME = "security_advanced_settings";

    private Context mContext;
    private SecurityAdvancedSettings mSecurityAdvancedSettings;

    @Before
    @UiThreadTest
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = ApplicationProvider.getApplicationContext();

        mSecurityAdvancedSettings = new SecurityAdvancedSettings();
    }

    @Test
    public void getPreferenceXml_returnsAdvancedSettings() {
        assertThat(mSecurityAdvancedSettings.getPreferenceScreenResId())
                .isEqualTo(getXmlResId(SCREEN_XML_RESOURCE_NAME));
    }

    @Test
    public void getCategoryKey_whenCalled_returnsSecurity() {
        assertThat(mSecurityAdvancedSettings.getCategoryKey())
                .isEqualTo(CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
    }

    private int getXmlResId(String resName) {
        return ResourcesUtils.getResourcesId(mContext, "xml", resName);
    }
}
