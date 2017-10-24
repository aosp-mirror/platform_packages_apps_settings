/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.provider.Settings.Global.USE_OPEN_WIFI_PACKAGE;
import static com.android.settings.wifi.UseOpenWifiPreferenceController.REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UseOpenWifiPreferenceControllerTest {
    private static ComponentName ENABLE_ACTIVITY_COMPONENT =
            new ComponentName("package", "activityClass");
    private static NetworkScorerAppData APP_DATA =
            new NetworkScorerAppData(0, null, null, ENABLE_ACTIVITY_COMPONENT, null);
    private static NetworkScorerAppData APP_DATA_NO_ACTIVITY =
            new NetworkScorerAppData(0, null, null, null, null);

    @Mock private Lifecycle mLifecycle;
    @Mock private Fragment mFragment;
    @Mock private NetworkScoreManagerWrapper mNetworkScoreManagerWrapper;
    @Captor private ArgumentCaptor<Intent> mIntentCaptor;
    private Context mContext;
    private UseOpenWifiPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
    }

    private void createController() {
        mController = new UseOpenWifiPreferenceController(
                mContext, mFragment, mNetworkScoreManagerWrapper, mLifecycle);
    }

    /**
     * Sets the scorers.
     * @param scorers list of scorers returned by {@link NetworkScoreManager#getAllValidScorers()}.
     *                First scorer in the list is the active scorer.
     */
    private void setupScorers(@NonNull List<NetworkScorerAppData> scorers) {
        when(mNetworkScoreManagerWrapper.getActiveScorerPackage()).thenReturn(
                ENABLE_ACTIVITY_COMPONENT.getPackageName());
        when(mNetworkScoreManagerWrapper.getAllValidScorers()).thenReturn(scorers);
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(scorers.get(0));
    }

    @Test
    public void testIsAvailable_returnsFalseWhenNoScorerSet() {
        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_returnsFalseWhenScorersNotSupported() {
        setupScorers(Lists.newArrayList(APP_DATA_NO_ACTIVITY));
        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_returnsTrueIfActiveScorerSupported() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_returnsTrueIfNonActiveScorerSupported() {
        setupScorers(Lists.newArrayList(APP_DATA_NO_ACTIVITY, APP_DATA));
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA_NO_ACTIVITY);
        createController();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChange_nonMatchingKey_shouldDoNothing() {
        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
    }

    @Test
    public void onPreferenceChange_notAvailable_shouldDoNothing() {
        createController();

        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
    }

    @Test
    public void onPreferenceChange_matchingKeyAndAvailable_enableShouldStartEnableActivity() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
        verify(mFragment).startActivityForResult(mIntentCaptor.capture(),
                eq(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY));
        Intent activityIntent = mIntentCaptor.getValue();
        assertThat(activityIntent.getComponent()).isEqualTo(ENABLE_ACTIVITY_COMPONENT);
        assertThat(activityIntent.getAction()).isEqualTo(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
    }

    @Test
    public void onPreferenceChange_matchingKeyAndAvailable_disableShouldUpdateSetting() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isTrue();
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo("");
    }

    @Test
    public void onActivityResult_nonmatchingRequestCode_shouldDoNothing() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        assertThat(mController.onActivityResult(234 /* requestCode */ , Activity.RESULT_OK))
                .isEqualTo(false);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_nonOkResult_shouldDoNothing() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_CANCELED))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_okResult_updatesSetting() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_OK))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo(ENABLE_ACTIVITY_COMPONENT.getPackageName());
    }

    @Test
    public void updateState_noEnableActivity_preferenceDisabled_summaryChanged() {
        setupScorers(Lists.newArrayList(APP_DATA_NO_ACTIVITY));
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(false);
        verify(preference).setSummary(
                R.string.use_open_wifi_automatically_summary_scorer_unsupported_disabled);
    }

    @Test
    public void updateState_noScorer_preferenceDisabled_summaryChanged() {
        when(mNetworkScoreManagerWrapper.getAllValidScorers()).thenReturn(new ArrayList<>());
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(false);
        verify(preference).setSummary(
                R.string.use_open_wifi_automatically_summary_scoring_disabled);
    }

    @Test
    public void updateState_enableActivityExists_preferenceEnabled() {
        setupScorers(Lists.newArrayList(APP_DATA, APP_DATA_NO_ACTIVITY));
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(true);
        verify(preference).setSummary(R.string.use_open_wifi_automatically_summary);
    }
}
