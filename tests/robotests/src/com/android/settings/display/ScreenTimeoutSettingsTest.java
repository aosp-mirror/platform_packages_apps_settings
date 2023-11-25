/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ScreenTimeoutSettingsTest {
    private static final String[] TIMEOUT_ENTRIES = new String[]{"15 secs", "30 secs"};
    private static final String[] TIMEOUT_VALUES = new String[]{"15000", "30000"};

    private ScreenTimeoutSettings mSettings;
    private Context mContext;
    private ContentResolver mContentResolver;
    private Resources mResources;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Mock
    AdaptiveSleepPermissionPreferenceController mPermissionPreferenceController;

    @Mock
    AdaptiveSleepPreferenceController mAdaptiveSleepPreferenceController;

    @Mock
    AdaptiveSleepCameraStatePreferenceController mAdaptiveSleepCameraStatePreferenceController;

    @Mock
    AdaptiveSleepBatterySaverPreferenceController mAdaptiveSleepBatterySaverPreferenceController;

    @Mock
    FooterPreference mDisableOptionsPreference;

    @Mock
    FooterPreference mPowerConsumptionPreference;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mContext = spy(getApplicationContext());
        mSettings = spy(new ScreenTimeoutSettings());
        mSettings.mContext = mContext;
        mContentResolver = mContext.getContentResolver();
        mResources = spy(mContext.getResources());

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.getAttentionServicePackageName()).thenReturn("some.package");
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);
        final ResolveInfo attentionServiceResolveInfo = new ResolveInfo();
        attentionServiceResolveInfo.serviceInfo = new ServiceInfo();
        when(mPackageManager.resolveService(isA(Intent.class), anyInt())).thenReturn(
                attentionServiceResolveInfo);

        doReturn(TIMEOUT_ENTRIES).when(mResources).getStringArray(R.array.screen_timeout_entries);
        doReturn(TIMEOUT_VALUES).when(mResources).getStringArray(R.array.screen_timeout_entries);
        doReturn(true).when(mResources).getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available);

        doReturn(null).when(mContext).getSystemService(DevicePolicyManager.class);
        doReturn(mResources).when(mContext).getResources();

        doReturn(mResources).when(mSettings).getResources();
        doReturn(mContext).when(mSettings).getContext();
        doReturn(mPreferenceScreen).when(mSettings).getPreferenceScreen();

        mSettings.mAdaptiveSleepController = mAdaptiveSleepPreferenceController;
        mSettings.mAdaptiveSleepPermissionController = mPermissionPreferenceController;
        mSettings.mAdaptiveSleepCameraStatePreferenceController =
                mAdaptiveSleepCameraStatePreferenceController;
        mSettings.mAdaptiveSleepBatterySaverPreferenceController =
                mAdaptiveSleepBatterySaverPreferenceController;
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ScreenTimeoutSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void getDefaultKey_returnCurrentTimeout() {
        long timeout = Long.parseLong(TIMEOUT_VALUES[1]);
        Settings.System.putLong(mContentResolver, SCREEN_OFF_TIMEOUT, timeout);

        String key = mSettings.getDefaultKey();

        assertThat(key).isEqualTo(TIMEOUT_VALUES[1]);
    }

    @Test
    public void updateCandidates_screenAttentionAvailable_showAdaptiveSleepPreference() {
        mSettings.updateCandidates();

        verify(mSettings.mAdaptiveSleepController).addToScreen(mPreferenceScreen);
    }

    @Test
    public void updateCandidates_screenAttentionNotAvailable_doNotShowAdaptiveSleepPreference() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available);

        mSettings.updateCandidates();

        verify(mSettings.mAdaptiveSleepController, never()).addToScreen(mPreferenceScreen);
    }

    @Test
    public void updateCandidates_AttentionServiceNotInstalled_doNoShowAdaptiveSleepPreference() {
        when(mPackageManager.resolveService(isA(Intent.class), anyInt())).thenReturn(null);

        verify(mSettings.mAdaptiveSleepController, never()).addToScreen(mPreferenceScreen);
    }

    @Test
    public void updateCandidates_enforcedAdmin_showDisabledByAdminPreference() {
        mSettings.mAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mSettings.mDisableOptionsPreference = mDisableOptionsPreference;
        mSettings.mPowerConsumptionPreference = mPowerConsumptionPreference;
        doNothing().when(mSettings).setupDisabledFooterPreference();
        doNothing().when(mSettings).setupPowerConsumptionFooterPreference();

        mSettings.updateCandidates();

        verify(mPreferenceScreen, atLeast(1)).addPreference(mDisableOptionsPreference);
        verify(mPreferenceScreen, never()).addPreference(mPowerConsumptionPreference);
    }

    @Test
    public void updateCandidates_withoutAdmin_showPowerConsumptionPreference() {
        mSettings.mAdmin = null;
        mSettings.mDisableOptionsPreference = mDisableOptionsPreference;
        mSettings.mPowerConsumptionPreference = mPowerConsumptionPreference;
        doNothing().when(mSettings).setupDisabledFooterPreference();
        doNothing().when(mSettings).setupPowerConsumptionFooterPreference();

        mSettings.updateCandidates();

        verify(mPreferenceScreen, never()).addPreference(mDisableOptionsPreference);
        verify(mPreferenceScreen, atLeast(1)).addPreference(mPowerConsumptionPreference);
    }

    @Test
    public void setDefaultKey_controlCurrentScreenTimeout() {
        mSettings.setDefaultKey(TIMEOUT_VALUES[0]);

        long timeout = Settings.System.getLong(mContentResolver, SCREEN_OFF_TIMEOUT,
                30000 /* default */);

        assertThat(Long.toString(timeout)).isEqualTo(TIMEOUT_VALUES[0]);
    }
}
