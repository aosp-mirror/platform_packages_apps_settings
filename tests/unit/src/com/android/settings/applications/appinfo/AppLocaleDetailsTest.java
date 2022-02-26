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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.LocaleManager;
import android.content.Context;
import android.os.LocaleList;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Unittest for ApplocaleDetails
 * TODO Need to add a unittest for the UI preference component.
 */
@RunWith(AndroidJUnit4.class)
public class AppLocaleDetailsTest {
    private static final String APP_PACKAGE_NAME = "app_package_name";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private LocaleManager mLocaleManager;

    private Context mContext;
    private Collection<Locale> mSystemLocales;
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
                /* appLocale= */ "en-gb",
                /* simCountry= */ "tw",
                /* networkCountry= */ "jp",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "pa, cn, zh-tw, en-gb, ja-jp",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "zh-tw", "ja-jp"});
    }

    @Test
    @UiThreadTest
    public void onRadioButtonClicked_setCurrentLocaleToSystem() {
        AppLocaleDetails appLocaleDetails = new AppLocaleDetails() {
            @Override
            void refreshUiInternal() {}
        };
        DummyAppLocaleDetailsHelper helper =
                spy(new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME));
        appLocaleDetails.mAppLocaleDetailsHelper = helper;
        RadioButtonPreference pref = new RadioButtonPreference(mContext);
        pref.setKey(AppLocaleDetails.KEY_SYSTEM_DEFAULT_LOCALE);

        appLocaleDetails.onRadioButtonClicked(pref);

        verify(helper).setAppDefaultLocale(LocaleList.forLanguageTags(""));
    }

    @Test
    @UiThreadTest
    public void onRadioButtonClicked_setCurrentLocaleForUserSelected() {
        AppLocaleDetails appLocaleDetails = new AppLocaleDetails() {
            @Override
            void refreshUiInternal() {}
        };
        DummyAppLocaleDetailsHelper helper =
                spy(new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME));
        appLocaleDetails.mAppLocaleDetailsHelper = helper;
        RadioButtonPreference pref = new RadioButtonPreference(mContext);
        pref.setKey("en");

        appLocaleDetails.onRadioButtonClicked(pref);

        verify(helper).setAppDefaultLocale("en");
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
    public void handleAllLocalesData_1stLocaleIsAppLocaleAndHasSimAndNetwork() {
        Locale simCountryLocale = new Locale("zh", "TW");
        Locale networkCountryLocale = new Locale("ja", "JP");
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        Locale locale = suggestedLocales.iterator().next();
        assertTrue(locale.equals(mAppLocale.get(0)));
        assertTrue(suggestedLocales.contains(simCountryLocale));
        assertTrue(suggestedLocales.contains(networkCountryLocale));
    }

    @Test
    @UiThreadTest
    public void
            handleAllLocalesData_noAppAndNoSupportedSimLocale_suggestedLocaleIsSupported() {
        Locale testEnAssetLocale = new Locale("en", "GB");
        Locale testJaAssetLocale = new Locale("ja", "JP");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "tw",
                /* networkCountry= */ "",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "ja-jp"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        assertTrue(suggestedLocales.contains(testEnAssetLocale));
        assertTrue(suggestedLocales.contains(testJaAssetLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noAppButHasSupportedSimLocale_1stSuggestedLocaleIsSim() {
        Locale simLocale = new Locale("zh", "tw");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "tw",
                /* networkCountry= */ "",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "ja-jp", "zh-tw"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        Locale locale = suggestedLocales.iterator().next();
        assertTrue(locale.equals(simLocale));
    }

    @Test
    @UiThreadTest
    public void
            handleAllLocalesData_noAppButHasSupportedNetworkLocale_1stSuggestedLocaleIsNetwork() {
        Locale networkLocale = new Locale("ja", "JP");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "jp",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "ja-jp"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        Locale locale = suggestedLocales.iterator().next();
        assertTrue(locale.equals(networkLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noAppSimOrNetworkLocale_suggestedLocalesHasSystemLocale() {
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "zh-tw", "ja-jp"});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);
        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        assertTrue(suggestedLocales.contains(Locale.forLanguageTag("ne")));
        // ru language is not present in the asset locales
        assertFalse(suggestedLocales.contains(Locale.forLanguageTag("ru")));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noAppButHasSimAndNetworkLocale_1stLocaleIsSimLocale() {
        Locale simCountryLocale = new Locale("zh", "TW");
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "tw",
                /* networkCountry= */ "jp",
                /* systemLocales= */ "en-gb, ru, ja-jp, ne, zh-tw",
                /* packageLocales= */ "",
                /* assetLocales= */ new String[]{"en-gb", "ne", "ms", "pa", "zh-tw", "ja-jp"});

        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);
        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        Locale locale = suggestedLocales.iterator().next();
        assertTrue(locale.equals(simCountryLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_noSupportedLocale_noSuggestedLocales() {
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

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        assertTrue(suggestedLocales.size() == 0);
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

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        Locale locale = suggestedLocales.iterator().next();
        Locale systemLocale = mSystemLocales.iterator().next();
        assertTrue(locale.equals(systemLocale));
    }

    @Test
    @UiThreadTest
    public void handleAllLocalesData_sameLocaleButDifferentRegion_notShowDuplicatedLocale() {
        setupInitialLocales(
                /* appLocale= */ "",
                /* simCountry= */ "",
                /* networkCountry= */ "",
                /* systemLocales= */ "en-us, en-gb, jp, ne",
                /* packageLocales= */ "pa, cn, tw, en-us, en-gb",
                /* assetLocales= */ new String[]{});
        DummyAppLocaleDetailsHelper helper =
                new DummyAppLocaleDetailsHelper(mContext, APP_PACKAGE_NAME);

        helper.handleAllLocalesData();

        Collection<Locale> suggestedLocales = helper.getSuggestedLocales();
        assertFalse(hasDuplicatedResult(suggestedLocales));
    }

    private boolean hasDuplicatedResult(Collection<Locale> locales) {
        Set<Locale> tempSet = new HashSet<>();
        for (Locale locale : locales) {
            if (!tempSet.add(locale)) {
                return true;
            }
        }
        return false;
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
        // forLanguageTags does not filter space to the input string. If there is any space included
        // in string, this will make locale fail to generate.
        systemLocales = systemLocales.replaceAll("\\s+", "");
        LocaleList listOfSystemLocales = LocaleList.forLanguageTags(systemLocales);
        mSystemLocales = new ArrayList<>();
        for (int i = 0; i < listOfSystemLocales.size(); i++) {
            mSystemLocales.add(listOfSystemLocales.get(i));
        }
        mAssetLocales = assetLocales;
        packageLocales = packageLocales.replaceAll("\\s+", "");
        mPackageLocales = LocaleList.forLanguageTags(packageLocales);
        when(mTelephonyManager.getSimCountryIso()).thenReturn(simCountry);
        when(mTelephonyManager.getNetworkCountryIso()).thenReturn(networkCountry);
        when(mLocaleManager.getApplicationLocales(anyString())).thenReturn(mAppLocale);
    }

    public class DummyAppLocaleDetailsHelper
            extends AppLocaleDetails.AppLocaleDetailsHelper {

        DummyAppLocaleDetailsHelper(Context context, String packageName) {
            super(context, packageName);
        }

        @Override
        String[] getAssetLocales() {
            return mAssetLocales;
        }

        @Override
        Collection<Locale> getCurrentSystemLocales() {
            return mSystemLocales;
        }

        @Override
        LocaleList getPackageLocales() {
            return mPackageLocales;
        }
    }
}
