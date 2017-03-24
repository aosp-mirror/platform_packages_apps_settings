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

package com.android.settings.network;

import static android.provider.Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkScorerAppData;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NetworkScorerPickerPreferenceControllerTest {

    private static final String TEST_SCORER_PACKAGE = "Test Package";
    private static final String TEST_SCORER_CLASS = "Test Class";
    private static final String TEST_SCORER_LABEL = "Test Label";

    private Context mContext;
    @Mock
    private NetworkScoreManagerWrapper mNetworkScorer;
    private NetworkScorerPickerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NetworkScorerPickerPreferenceController(mContext, mNetworkScorer);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_preferenceSetSummaryAsActiveScorerLabel() {
        Settings.System.putInt(mContext.getContentResolver(), NETWORK_RECOMMENDATIONS_ENABLED, 1);
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScorer.getActiveScorer()).thenReturn(scorerAppData);
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(TEST_SCORER_LABEL);
    }

    @Test
    public void updateState_noActiveScorer_preferenceSetSummaryToNone() {
        Settings.System.putInt(mContext.getContentResolver(), NETWORK_RECOMMENDATIONS_ENABLED, 1);
        when(mNetworkScorer.getActiveScorer()).thenReturn(null);
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(mContext.getString(
                R.string.network_scorer_picker_none_preference));
    }

    @Test
    public void updateState_networkRecommendationsDisabled_preferenceDisabled() {
        Settings.System.putInt(mContext.getContentResolver(), NETWORK_RECOMMENDATIONS_ENABLED, 0);
        when(mNetworkScorer.getActiveScorer()).thenReturn(null);
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setEnabled(false);
        verify(preference).setSummary(null);
    }
}
