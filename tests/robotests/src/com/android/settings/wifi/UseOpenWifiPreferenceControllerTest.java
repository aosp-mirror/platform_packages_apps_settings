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

import static android.content.Context.NETWORK_SCORE_SERVICE;
import static android.provider.Settings.Global.USE_OPEN_WIFI_PACKAGE;

import static com.android.settings.wifi.UseOpenWifiPreferenceController
        .REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UseOpenWifiPreferenceControllerTest {

    private static ComponentName sEnableActivityComponent;
    private static NetworkScorerAppData sAppData;
    private static NetworkScorerAppData sAppDataNoActivity;

    @BeforeClass
    public static void beforeClass() {
        sEnableActivityComponent = new ComponentName("package", "activityClass");
        sAppData = new NetworkScorerAppData(0, null, null, sEnableActivityComponent, null);
        sAppDataNoActivity = new NetworkScorerAppData(0, null, null, null, null);
    }

    @Mock
    private Fragment mFragment;
    @Mock
    private NetworkScoreManager mNetworkScoreManager;
    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;
    private Context mContext;
    private UseOpenWifiPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        ShadowApplication.getInstance()
                .setSystemService(NETWORK_SCORE_SERVICE, mNetworkScoreManager);
    }

    private void createController() {
        mController = new UseOpenWifiPreferenceController(mContext);
        mController.setFragment(mFragment);
    }

    /**
     * Sets the scorers.
     *
     * @param scorers list of scorers returned by {@link NetworkScoreManager#getAllValidScorers()}.
     *                First scorer in the list is the active scorer.
     */
    private void setupScorers(@NonNull List<NetworkScorerAppData> scorers) {
        when(mNetworkScoreManager.getActiveScorerPackage())
                .thenReturn(sEnableActivityComponent.getPackageName());
        when(mNetworkScoreManager.getAllValidScorers()).thenReturn(scorers);
        when(mNetworkScoreManager.getActiveScorer()).thenReturn(scorers.get(0));
    }

    @Test
    public void testIsAvailable_returnsFalseWhenNoScorerSet() {
        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_returnsFalseWhenScorersNotSupported() {
        setupScorers(Lists.newArrayList(sAppDataNoActivity));
        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_returnsTrueIfActiveScorerSupported() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_returnsTrueIfNonActiveScorerSupported() {
        setupScorers(Lists.newArrayList(sAppDataNoActivity, sAppData));
        when(mNetworkScoreManager.getActiveScorer()).thenReturn(sAppDataNoActivity);
        createController();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_disableUseOpenWifiComponentBetweenCalls_returnsTrueThenReturnsFalse() {
        setupScorers(Lists.newArrayList(sAppData));
        createController();

        assertThat(mController.isAvailable()).isTrue();

        // Update NetworkScorerAppData so that it no longer has openWifiActivity.
        setupScorers(Lists.newArrayList(sAppDataNoActivity));

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void setChecked_withTrue_enableShouldStartEnableActivity() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        mController.setChecked(true);

        verify(mFragment).startActivityForResult(mIntentCaptor.capture(),
                eq(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY));
        final Intent activityIntent = mIntentCaptor.getValue();
        assertThat(activityIntent.getComponent()).isEqualTo(sEnableActivityComponent);
        assertThat(activityIntent.getAction()).isEqualTo(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
    }

    @Test
    public void setChecked_withFalse_disableShouldUpdateSetting() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                sEnableActivityComponent.getPackageName());
        createController();

        mController.setChecked(false);

        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo("");
    }

    @Test
    public void onActivityResult_nonmatchingRequestCode_shouldDoNothing() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        assertThat(mController.onActivityResult(234 /* requestCode */, Activity.RESULT_OK))
                .isEqualTo(false);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_nonOkResult_shouldDoNothing() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_CANCELED))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_okResult_updatesSetting() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_OK))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo(sEnableActivityComponent.getPackageName());
    }

    @Test
    public void updateState_noEnableActivity_preferenceDisabled_summaryChanged() {
        setupScorers(Lists.newArrayList(sAppDataNoActivity));
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                sEnableActivityComponent.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(false);
        verify(preference).setSummary(
                R.string.use_open_wifi_automatically_summary_scorer_unsupported_disabled);
    }

    @Test
    public void updateState_noScorer_preferenceDisabled_summaryChanged() {
        when(mNetworkScoreManager.getAllValidScorers()).thenReturn(new ArrayList<>());
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                sEnableActivityComponent.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(false);
        verify(preference).setSummary(
                R.string.use_open_wifi_automatically_summary_scoring_disabled);
    }

    @Test
    public void updateState_enableActivityExists_preferenceEnabled() {
        setupScorers(Lists.newArrayList(sAppData, sAppDataNoActivity));
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                sEnableActivityComponent.getPackageName());

        mController.updateState(preference);

        verify(preference).setChecked(true);
        verify(preference).setSummary(R.string.use_open_wifi_automatically_summary);
    }
}
