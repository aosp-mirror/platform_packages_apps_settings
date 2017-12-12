/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAppSettingsTest {

    private Context mContext;

    private DefaultAppSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = new DefaultAppSettings();
        mFragment.onAttach(mContext);
    }

    @Test
    public void getPreferenceScreenResId_shouldUseAppDefaultSettingPrefLayout() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.app_default_settings);
    }

    @Test
    public void setListening_shouldUpdateSummary() {
        final SummaryLoader summaryLoader = mock(SummaryLoader.class);
        final DefaultAppSettings.SummaryProvider summaryProvider =
                new DefaultAppSettings.SummaryProvider(mContext, summaryLoader);
        final DefaultSmsPreferenceController defaultSms =
                mock(DefaultSmsPreferenceController.class);
        final DefaultBrowserPreferenceController defaultBrowser =
                mock(DefaultBrowserPreferenceController.class);
        final DefaultPhonePreferenceController defaultPhone =
                mock(DefaultPhonePreferenceController.class);
        ReflectionHelpers.setField(summaryProvider, "mDefaultSmsPreferenceController", defaultSms);
        ReflectionHelpers.setField(
                summaryProvider, "mDefaultBrowserPreferenceController", defaultBrowser);
        ReflectionHelpers.setField(
                summaryProvider, "mDefaultPhonePreferenceController", defaultPhone);

        // all available
        when(defaultSms.getDefaultAppLabel()).thenReturn("Sms1");
        when(defaultBrowser.getDefaultAppLabel()).thenReturn("Browser1");
        when(defaultPhone.getDefaultAppLabel()).thenReturn("Phone1");
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Sms1, Browser1, Phone1");

        // 2 available
        when(defaultSms.getDefaultAppLabel()).thenReturn(null);
        when(defaultBrowser.getDefaultAppLabel()).thenReturn("Browser1");
        when(defaultPhone.getDefaultAppLabel()).thenReturn("Phone1");
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Browser1, Phone1");

        when(defaultSms.getDefaultAppLabel()).thenReturn("Sms1");
        when(defaultBrowser.getDefaultAppLabel()).thenReturn(null);
        when(defaultPhone.getDefaultAppLabel()).thenReturn("Phone1");
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Sms1, Phone1");

        when(defaultSms.getDefaultAppLabel()).thenReturn("Sms1");
        when(defaultBrowser.getDefaultAppLabel()).thenReturn("Browser1");
        when(defaultPhone.getDefaultAppLabel()).thenReturn(null);
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Sms1, Browser1");

        // 1 available
        when(defaultSms.getDefaultAppLabel()).thenReturn(null);
        when(defaultBrowser.getDefaultAppLabel()).thenReturn("Browser1");
        when(defaultPhone.getDefaultAppLabel()).thenReturn(null);
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Browser1");

        when(defaultSms.getDefaultAppLabel()).thenReturn("Sms1");
        when(defaultBrowser.getDefaultAppLabel()).thenReturn(null);
        when(defaultPhone.getDefaultAppLabel()).thenReturn(null);
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Sms1");

        when(defaultSms.getDefaultAppLabel()).thenReturn(null);
        when(defaultBrowser.getDefaultAppLabel()).thenReturn(null);
        when(defaultPhone.getDefaultAppLabel()).thenReturn("Phone1");
        summaryProvider.setListening(true);
        verify(summaryLoader).setSummary(summaryProvider, "Phone1");

        // None available
        when(defaultSms.getDefaultAppLabel()).thenReturn(null);
        when(defaultBrowser.getDefaultAppLabel()).thenReturn(null);
        when(defaultPhone.getDefaultAppLabel()).thenReturn(null);
        summaryProvider.setListening(true);
        verify(summaryLoader, never()).setSummary(summaryProvider, eq(anyString()));

    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        final Context mockContext = mock(Context.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        final UserManager userManager = mock(UserManager.class, RETURNS_DEEP_STUBS);

        when(mockContext.getSystemService(Context.USER_SERVICE))
                .thenReturn(userManager);
        when(userManager.getUserInfo(anyInt()).isRestricted()).thenReturn(true);

        when(mockContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mock(TelephonyManager.class));
        when(mockContext.getPackageManager())
                .thenReturn(mock(PackageManager.class));
        final List<String> niks = DefaultAppSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mockContext);

        final int xmlId = (new DefaultAppSettings()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }
}
