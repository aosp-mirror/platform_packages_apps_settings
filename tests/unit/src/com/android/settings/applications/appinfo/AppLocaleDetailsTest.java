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

package com.android.settings.applications.appinfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.LocaleManager;
import android.content.Context;
import android.os.LocaleList;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class AppLocaleDetailsTest {
    private static final String APP_PACKAGE_NAME = "app_package_name";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private LocaleManager mLocaleManager;

    private Context mContext;
    private LocaleList mSystemLocales;
    private Locale mSimLocale;
    private LocaleList mAppLocale;
    private String[] mAssetLocales;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(LocaleManager.class)).thenReturn(mLocaleManager);

        setupInitialLocales("en",
                "uk",
                "en, uk, jp, ne",
                new String[]{"en", "ne", "ms", "pa"});
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_localeManagerIsNull_noCrash() {
        when(mContext.getSystemService(LocaleManager.class)).thenReturn(null);

        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_1stLocaleOfSuggestedLocaleListIsAppLocale() {
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(mAppLocale.get(0)));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_2ndLocaleOfSuggestedLocaleListIsSimLocale() {
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 1);
        assertTrue(locale.equals(mSimLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_withoutAppLocale_1stLocaleOfSuggestedLocaleListIsSimLocal() {
        setupInitialLocales("",
                "uk",
                "en, uk, jp, ne",
                new String[]{"en", "ne", "ms", "pa"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(mSimLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noAppAndSimLocale_1stLocaleIsFirstOneInSystemLocales() {
        setupInitialLocales("",
                "",
                "en, uk, jp, ne",
                new String[]{"en", "ne", "ms", "pa"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(mSystemLocales.get(0)));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_supportLocaleListIsNotEmpty() {
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        assertFalse(helper.getSupportedLocales().isEmpty());
    }

    /**
     * Sets the initial Locale data
     *
     * @param appLocale     Application locale, it shall be a language tag.
     *                      example: "en"
     * @param simLocale     SIM carrier locale, it shall be a language tag.
     *                      example: "en"
     * @param systemLocales System locales, a locale list by a multiple language tags with comma.
     *                      example: "en, uk, jp"
     * @param assetLocales  Asset locales, a locale list by a multiple language tags with String
     *                      array.
     *                      example: new String[] {"en", "ne", "ms", "pa"}
     */
    private void setupInitialLocales(String appLocale,
            String simLocale,
            String systemLocales,
            String[] assetLocales) {
        mAppLocale = LocaleList.forLanguageTags(appLocale);
        mSimLocale = Locale.forLanguageTag(simLocale);
        mSystemLocales = LocaleList.forLanguageTags(systemLocales);
        mAssetLocales = assetLocales;
        when(mTelephonyManager.getSimLocale()).thenReturn(simLocale.isEmpty() ? null : mSimLocale);
        when(mLocaleManager.getApplicationLocales(anyString())).thenReturn(mAppLocale);
    }

    private class DummyAppLocaleDetailsHelper
            extends AppLocaleDetails.AppLocaleDetailsHelper {

        DummyAppLocaleDetailsHelper(Context context, String packageName) {
            super(context, packageName);
        }

        @Override
        String[] getAssetSystemLocales() {
            return mAssetLocales;
        }

        @Override
        LocaleList getCurrentSystemLocales() {
            return mSystemLocales;
        }
    }

}
