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
    private LocaleList mAppLocale;
    private String[] mAssetLocales;
    private LocaleList mPackageLocales;

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

        setupInitialLocales(
                /* appLocale= */ "en",
                /* simCountry= */ "tw",
                /* networkCountry= */ "jp",
                /* systemLocales= */ "en, uk, jp, ne",
                /* packageLocales= */ "pa, cn, tw, en",
                /* assetLocales= */ new String[]{"en", "ne", "ms", "pa"});
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
        Locale simCountryLocale = new Locale("zh", "TW");
        Locale networkCountryLocale = new Locale("ja", "JP");
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(mAppLocale.get(0)));
        assertTrue(helper.getSuggestedLocales().contains(simCountryLocale));
        assertTrue(helper.getSuggestedLocales().contains(networkCountryLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_withoutAppLocale_1stSuggestedLocaleIsSimCountryLocale() {
        Locale simCountryLocale = new Locale("zh", "TW");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "tw",
                /* networkCountry= */ "",
                /* systemLocales= */ "en, uk, jp, ne",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(simCountryLocale));
        assertFalse(helper.getSuggestedLocales().contains(mAppLocale.get(0)));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_withoutAppLocale_1stSuggestedLocaleIsNetworkCountryLocale() {
        Locale networkCountryLocale = new Locale("en", "GB");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "gb",
                /* systemLocales= */ "en, uk, jp, ne",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(networkCountryLocale));
        assertFalse(helper.getSuggestedLocales().contains(mAppLocale.get(0)));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noAppAndSimNetworkLocale_1stLocaleIsFirstOneInSystemLocales() {
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "",
                /* systemLocales= */ "en, uk, jp, ne",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Locale locale = Iterables.get(helper.getSuggestedLocales(), 0);
        assertTrue(locale.equals(mSystemLocales.get(0)));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_hasPackageAndSystemLocales_1stLocaleIs1stOneInSystemLocales() {
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "",
                /* systemLocales= */ "en, uk, jp, ne",
                /* packageLocales= */ "pa, cn, tw, en",
                /* assetLocales= */ new String[]{});
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

    @Test
    @UiThreadTest
    public void handleAllLocalesData_compareLocale() {
        //Use LocaleList.matchScore() to compare two locales.
        assertTrue(DummyAppLocaleDetailsHelper.compareLocale(Locale.forLanguageTag("en-US"),
                Locale.forLanguageTag("en-CA")));
        assertTrue(DummyAppLocaleDetailsHelper.compareLocale(Locale.forLanguageTag("zh-CN"),
                Locale.forLanguageTag("zh")));
        assertTrue(DummyAppLocaleDetailsHelper.compareLocale(Locale.forLanguageTag("zh-CN"),
                Locale.forLanguageTag("zh-Hans")));
        assertTrue(DummyAppLocaleDetailsHelper.compareLocale(Locale.forLanguageTag("zh-TW"),
                Locale.forLanguageTag("zh-Hant")));

        //Use Locale.equals() to compare two locales.
        assertFalse(Locale.forLanguageTag("en-US").equals(Locale.forLanguageTag("en-CA")));
        assertFalse(Locale.forLanguageTag("zh-CN").equals(Locale.forLanguageTag("zh")));
        assertFalse(Locale.forLanguageTag("zh-CN").equals(Locale.forLanguageTag("zh-Hans")));
        assertFalse(Locale.forLanguageTag("zh-TW").equals(Locale.forLanguageTag("zh-Hant")));
    }

    /**
     * Sets the initial Locale data
     *
     * @param appLocale      Application locale, it shall be a language tag.
     *                       example: "en"
     *
     * @param simCountry     The ISO-3166-1 alpha-2 country code equivalent for the SIM
     *                       provider's country code.
     *                       example: "us"
     *
     * @param networkCountry The ISO-3166-1 alpha-2 country code equivalent of the MCC
     *                       (Mobile Country Code) of the current registered operato
     *                       or the cell nearby.
     *                       example: "us"
     *
     * @param systemLocales  System locales, a locale list by a multiple language tags with comma.
     *                       example: "en, uk, jp"
     *
     * @param packageLocales PackageManager locales, a locale list by a multiple language tags with
     *                       comma.
     *                       example: "en, uk, jp"
     *
     * @param assetLocales   Asset locales, a locale list by a multiple language tags with String
     *                       array.
     *                       example: new String[] {"en", "ne", "ms", "pa"}
     */
    private void setupInitialLocales(String appLocale,
            String simCountry,
            String networkCountry,
            String systemLocales,
            String packageLocales,
            String[] assetLocales) {
        mAppLocale = LocaleList.forLanguageTags(appLocale);
        mSystemLocales = LocaleList.forLanguageTags(systemLocales);
        mAssetLocales = assetLocales;
        mPackageLocales = LocaleList.forLanguageTags(packageLocales);
        when(mTelephonyManager.getSimCountryIso()).thenReturn(simCountry);
        when(mTelephonyManager.getNetworkCountryIso()).thenReturn(networkCountry);
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

        @Override
        LocaleList getPackageLocales() {
            return mPackageLocales;
        }
    }
}
