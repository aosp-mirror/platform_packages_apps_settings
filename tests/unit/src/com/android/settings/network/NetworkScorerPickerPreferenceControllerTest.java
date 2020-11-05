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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class NetworkScorerPickerPreferenceControllerTest {

    private static final String TEST_SCORER_PACKAGE = "Test Package";
    private static final String TEST_SCORER_CLASS = "Test Class";
    private static final String TEST_SCORER_LABEL = "Test Label";

    private Context mContext;
    @Mock
    private NetworkScoreManager mNetworkScorer;

    private NetworkScorerPickerPreferenceController mController;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mNetworkScorer).when(mContext).getSystemService(Context.NETWORK_SCORE_SERVICE);

        mController = new NetworkScorerPickerPreferenceController(mContext, "test_key");
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_preferenceSetSummaryAsActiveScorerLabel() {

        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);

        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);

        when(mNetworkScorer.getAllValidScorers())
                .thenReturn(Collections.singletonList(scorerAppData));

        when(mNetworkScorer.getActiveScorer()).thenReturn(scorerAppData);

        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(TEST_SCORER_LABEL);
    }

    @Test
    public void updateState_scorersAvailable_noActiveScorer_preferenceSetSummaryToNone() {
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScorer.getAllValidScorers())
                .thenReturn(Collections.singletonList(scorerAppData));
        when(mNetworkScorer.getActiveScorer()).thenReturn(null);

        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        Context context = ApplicationProvider.getApplicationContext();

        verify(preference).setSummary(
                resourceString("network_scorer_picker_none_preference"));
    }

    @Test
    public void updateState_scorersAvailable_preferenceEnabled() {
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScorer.getAllValidScorers())
                .thenReturn(Collections.singletonList(scorerAppData));

        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setEnabled(true);
    }

    @Test
    public void updateState_noScorersAvailable_preferenceDisabled() {
        when(mNetworkScorer.getAllValidScorers())
                .thenReturn(Collections.emptyList());
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setEnabled(false);
        verify(preference).setSummary(null);
    }

    public int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    public String resourceString(String name) {
        return mContext.getResources().getString(resourceId("string", name));
    }
}
