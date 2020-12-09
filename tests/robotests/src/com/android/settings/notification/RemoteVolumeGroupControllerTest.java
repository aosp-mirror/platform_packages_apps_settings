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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageStats;
import android.media.MediaRouter2Manager;
import android.media.RoutingSessionInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.media.LocalMediaManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class RemoteVolumeGroupControllerTest {

    private static final String KEY_REMOTE_VOLUME_GROUP = "remote_media_group";
    private static final String TEST_SESSION_1_ID = "test_session_1_id";
    private static final String TEST_SESSION_1_NAME = "test_session_1_name";
    private static final String TEST_PACKAGE_NAME = "com.test";
    private static final String TEST_APPLICATION_LABEL = "APP Test Label";
    private static final int CURRENT_VOLUME = 30;
    private static final int MAX_VOLUME = 100;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private SharedPreferences mSharedPreferences;

    private final List<RoutingSessionInfo> mRoutingSessionInfos = new ArrayList<>();

    private Context mContext;
    private RemoteVolumeGroupController mController;
    private PreferenceCategory mPreferenceCategory;
    private ShadowPackageManager mShadowPackageManager;
    private ApplicationInfo mAppInfo;
    private PackageInfo mPackageInfo;
    private PackageStats mPackageStats;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new RemoteVolumeGroupController(mContext, KEY_REMOTE_VOLUME_GROUP);
        mController.mLocalMediaManager = mLocalMediaManager;
        mController.mRouterManager = mock(MediaRouter2Manager.class);
        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        mPreferenceCategory.setKey(mController.getPreferenceKey());

        when(mPreferenceCategory.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceManager.getSharedPreferences()).thenReturn(mSharedPreferences);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreferenceCategory);
        final RoutingSessionInfo remoteSessionInfo = mock(RoutingSessionInfo.class);
        when(remoteSessionInfo.getId()).thenReturn(TEST_SESSION_1_ID);
        when(remoteSessionInfo.getName()).thenReturn(TEST_SESSION_1_NAME);
        when(remoteSessionInfo.getVolumeMax()).thenReturn(MAX_VOLUME);
        when(remoteSessionInfo.getVolume()).thenReturn(CURRENT_VOLUME);
        when(remoteSessionInfo.getClientPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(remoteSessionInfo.isSystemSession()).thenReturn(false);
        mRoutingSessionInfos.add(remoteSessionInfo);
        when(mLocalMediaManager.getActiveMediaSession()).thenReturn(mRoutingSessionInfos);
    }

    @Test
    public void getAvailabilityStatus_withActiveSession_returnAvailableUnsearchable() {
        mController.displayPreference(mScreen);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_noActiveSession_returnConditionallyUnavailable() {
        mRoutingSessionInfos.clear();
        mController.displayPreference(mScreen);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_noActiveSession_checkPreferenceCount() {
        mRoutingSessionInfos.clear();
        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_withActiveSession_checkPreferenceCount() {
        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void displayPreference_withActiveSession_checkSeekBarTitle() {
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(TEST_SESSION_1_ID);

        assertThat(preference.getTitle()).isEqualTo(mContext.getText(
                R.string.remote_media_volume_option_title));
    }

    @Test
    public void displayPreference_withActiveSession_checkSeekBarMaxVolume() {
        mController.displayPreference(mScreen);
        final SeekBarPreference preference = mPreferenceCategory.findPreference(TEST_SESSION_1_ID);

        assertThat(preference.getMax()).isEqualTo(MAX_VOLUME);
    }

    @Test
    public void displayPreference_withActiveSession_checkSeekBarCurrentVolume() {
        mController.displayPreference(mScreen);
        final SeekBarPreference preference = mPreferenceCategory.findPreference(TEST_SESSION_1_ID);

        assertThat(preference.getProgress()).isEqualTo(CURRENT_VOLUME);
    }

    @Test
    public void displayPreference_withActiveSession_checkSwitcherPreferenceTitle() {
        initPackage();
        mShadowPackageManager.addPackage(mPackageInfo, mPackageStats);
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(
                RemoteVolumeGroupController.SWITCHER_PREFIX + TEST_SESSION_1_ID);

        assertThat(preference.getTitle()).isEqualTo(mContext.getString(
                R.string.media_output_label_title, Utils.getApplicationLabel(mContext,
                        TEST_PACKAGE_NAME)));
    }

    @Test
    public void displayPreference_withActiveSession_checkSwitcherPreferenceSummary() {
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(
                RemoteVolumeGroupController.SWITCHER_PREFIX + TEST_SESSION_1_ID);

        assertThat(preference.getSummary()).isEqualTo(TEST_SESSION_1_NAME);
    }

    private void initPackage() {
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mAppInfo = new ApplicationInfo();
        mAppInfo.flags = ApplicationInfo.FLAG_INSTALLED;
        mAppInfo.packageName = TEST_PACKAGE_NAME;
        mAppInfo.name = TEST_APPLICATION_LABEL;
        mPackageInfo = new PackageInfo();
        mPackageInfo.packageName = TEST_PACKAGE_NAME;
        mPackageInfo.applicationInfo = mAppInfo;
        mPackageStats = new PackageStats(TEST_PACKAGE_NAME);
    }
}
