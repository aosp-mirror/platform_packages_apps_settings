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

package com.android.settings.localepicker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.res.Configuration;
import android.os.LocaleList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class LocaleFeatureProviderImplTest {
    private LocaleFeatureProviderImpl mLocaleFeatureProviderImpl;
    private Configuration mCacheConfig;

    @Before
    public void setUp() throws Exception {
        mLocaleFeatureProviderImpl = new LocaleFeatureProviderImpl();
        // Cache current configuration.
        mCacheConfig = ActivityManager.getService().getConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        // Recovery the configuration to the current device.
        ActivityManager.getService().updatePersistentConfigurationWithAttribution(mCacheConfig,
                ActivityThread.currentOpPackageName(), null);
    }

    @Test
    public void getLocaleNames_hasEnAndZh_resultIsEnglishAndChinese() throws Exception {
        LocaleList locales = LocaleList.forLanguageTags("en-US-u-mu-celsius,zh-TW");
        final Configuration config = new Configuration();
        config.setLocales(locales);
        ActivityManager.getService().updatePersistentConfigurationWithAttribution(config,
                ActivityThread.currentOpPackageName(), null);

        String result = mLocaleFeatureProviderImpl.getLocaleNames().trim();

        String expected1 =
                Locale.forLanguageTag("en-US-u-mu-celsius").stripExtensions().getDisplayName();
        String expected2 = Locale.forLanguageTag("zh-TW").getDisplayName();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    public void getLocaleNames_hasExtension_resultWithoutExtensionInfo() throws Exception {
        LocaleList locales = LocaleList.forLanguageTags("en-US-u-mu-celsius,zh-TW");
        final Configuration config = new Configuration();
        config.setLocales(locales);
        ActivityManager.getService().updatePersistentConfigurationWithAttribution(config,
                ActivityThread.currentOpPackageName(), null);

        String result = mLocaleFeatureProviderImpl.getLocaleNames().toLowerCase(Locale.ROOT);

        assertFalse(result.contains("celsius"));
    }
}
