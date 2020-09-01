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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.RadioButtonPreference;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class NetworkScorerPickerTest {

    private static final String TEST_SCORER_PACKAGE_1 = "Test Package 1";
    private static final String TEST_SCORER_CLASS_1 = "Test Class 1";
    private static final String TEST_SCORER_LABEL_1 = "Test Label 1";
    private static final String TEST_SCORER_PACKAGE_2 = "Test Package 2";

    private Context mContext;
    @Mock
    private NetworkScoreManager mNetworkScoreManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = new TestFragment(mContext, mPreferenceScreen, mNetworkScoreManager);
        mFragment.onAttach(mContext);
    }

    @Test
    public void testOnRadioButtonClicked_success() {
        RadioButtonPreference pref = new RadioButtonPreference(mContext);
        pref.setKey(TEST_SCORER_PACKAGE_1);
        when(mPreferenceScreen.getPreference(anyInt())).thenReturn(pref);
        when(mPreferenceScreen.getPreferenceCount()).thenReturn(1);
        when(mNetworkScoreManager.setActiveScorer(TEST_SCORER_PACKAGE_1)).thenReturn(true);
        when(mNetworkScoreManager.getActiveScorerPackage()).thenReturn(TEST_SCORER_PACKAGE_2);

        mFragment.onRadioButtonClicked(pref);

        verify(mNetworkScoreManager).setActiveScorer(TEST_SCORER_PACKAGE_1);
        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void testOnRadioButtonClicked_currentScorer_doNothing() {
        RadioButtonPreference pref = new RadioButtonPreference(mContext);
        pref.setKey(TEST_SCORER_PACKAGE_1);
        pref.setChecked(true);
        when(mPreferenceScreen.getPreference(anyInt())).thenReturn(pref);
        when(mPreferenceScreen.getPreferenceCount()).thenReturn(1);
        when(mNetworkScoreManager.setActiveScorer(TEST_SCORER_PACKAGE_1)).thenReturn(true);
        when(mNetworkScoreManager.getActiveScorerPackage()).thenReturn(TEST_SCORER_PACKAGE_1);

        mFragment.onRadioButtonClicked(pref);

        verify(mNetworkScoreManager, never()).setActiveScorer(any());
        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void testUpdateCandidates_noValidScorers_nonePreference() {
        when(mNetworkScoreManager.getAllValidScorers()).thenReturn(new ArrayList<>());
        ArgumentCaptor<RadioButtonPreference> arg =
                ArgumentCaptor.forClass(RadioButtonPreference.class);

        mFragment.updateCandidates();

        verify(mPreferenceScreen).addPreference(arg.capture());
        assertThat(arg.getValue().getTitle())
                .isEqualTo(mContext.getString(R.string.network_scorer_picker_none_preference));
        assertThat(arg.getValue().isChecked()).isTrue();
    }

    @Test
    public void testUpdateCandidates_validScorers_noActiveScorer() {
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE_1, TEST_SCORER_CLASS_1);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL_1, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScoreManager.getAllValidScorers()).thenReturn(
                Lists.newArrayList(scorerAppData));
        when(mNetworkScoreManager.getActiveScorerPackage()).thenReturn(null);

        ArgumentCaptor<RadioButtonPreference> arg =
                ArgumentCaptor.forClass(RadioButtonPreference.class);

        mFragment.updateCandidates();

        verify(mPreferenceScreen, times(2)).addPreference(arg.capture());

        final RadioButtonPreference nonePref = arg.getAllValues().get(0);
        assertThat(nonePref.getKey()).isNull();
        assertThat(nonePref.isChecked()).isTrue();

        final RadioButtonPreference testScorerPref = arg.getAllValues().get(1);
        assertThat(testScorerPref.getTitle()).isEqualTo(TEST_SCORER_LABEL_1);
        assertThat(testScorerPref.isChecked()).isFalse();
    }

    @Test
    public void testUpdateCandidates_validScorer() {
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE_1, TEST_SCORER_CLASS_1);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL_1, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScoreManager.getAllValidScorers()).thenReturn(
                Lists.newArrayList(scorerAppData));
        when(mNetworkScoreManager.getActiveScorerPackage()).thenReturn(TEST_SCORER_PACKAGE_1);
        ArgumentCaptor<RadioButtonPreference> arg =
                ArgumentCaptor.forClass(RadioButtonPreference.class);

        mFragment.updateCandidates();

        // The first preference added is the "none" preference and the second is the
        // pref for the test scorer.
        verify(mPreferenceScreen, times(2)).addPreference(arg.capture());
        // Returns the last captured value which is expected to be the test scorer pref.
        RadioButtonPreference pref = arg.getValue();
        assertThat(pref.getTitle()).isEqualTo(TEST_SCORER_LABEL_1);
        assertThat(pref.isChecked()).isTrue();
    }

    public static class TestFragment extends NetworkScorerPicker {

        private final Context mContext;
        private final PreferenceScreen mScreen;
        private final PreferenceManager mPrefManager;
        private final NetworkScoreManager mNetworkScoreManager;

        public TestFragment(Context context, PreferenceScreen preferenceScreen,
                NetworkScoreManager networkScoreManager) {
            mContext = context;
            mScreen = preferenceScreen;
            mNetworkScoreManager = networkScoreManager;
            mPrefManager = mock(PreferenceManager.class);
            when(mPrefManager.getContext()).thenReturn(context);
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPrefManager;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }

        @Override
        NetworkScoreManager createNetworkScorerManager(Context context) {
            return mNetworkScoreManager;
        }
    }
}
