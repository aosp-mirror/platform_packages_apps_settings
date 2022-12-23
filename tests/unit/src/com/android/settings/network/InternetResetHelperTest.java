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

package com.android.settings.network;

import static com.android.settings.network.InternetResetHelper.RESTART_TIMEOUT_MS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.utils.HandlerInjector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class InternetResetHelperTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    InternetResetHelper.RecoveryWorker mRecoveryWorker;
    @Mock
    HandlerInjector mHandlerInjector;
    @Mock
    public NetworkMobileProviderController mMobileNetworkController;

    private InternetResetHelper mInternetResetHelper;
    private Preference mResettingPreference;
    private Preference mWifiTogglePreferences;
    private PreferenceCategory mConnectedWifiEntryPreferences;
    private PreferenceCategory mFirstWifiEntryPreference;
    private PreferenceCategory mWifiEntryPreferences;

    @Before
    public void setUp() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mRecoveryWorker.isRecovering()).thenReturn(false);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mResettingPreference = spy(new Preference(mContext));
        mWifiTogglePreferences = new Preference(mContext);
        mConnectedWifiEntryPreferences = spy(new PreferenceCategory(mContext));
        mFirstWifiEntryPreference = spy(new PreferenceCategory(mContext));
        mWifiEntryPreferences = spy(new PreferenceCategory(mContext));

        mInternetResetHelper = new InternetResetHelper(mContext, mock(Lifecycle.class),
                mMobileNetworkController,
                mWifiTogglePreferences,
                mConnectedWifiEntryPreferences,
                mFirstWifiEntryPreference,
                mWifiEntryPreferences,
                mResettingPreference);
        mInternetResetHelper.mHandlerInjector = mHandlerInjector;
        mInternetResetHelper.mRecoveryWorker = mRecoveryWorker;
    }

    @Test
    public void onResume_registerReceiver() {
        mInternetResetHelper.onResume();

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class),
                any(int.class));
    }

    @Test
    public void onPause_unregisterReceiver() {
        mInternetResetHelper.onResume();

        mInternetResetHelper.onPause();

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onDestroy_removeCallbacks() {
        mInternetResetHelper.onDestroy();

        verify(mHandlerInjector).removeCallbacks(any());
    }

    @Test
    public void updateWifiStateChange_wifiIsNotReadyAndWifiDisabled_doNothing() {
        mInternetResetHelper.mIsWifiReady = false;
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isFalse();
    }

    @Test
    public void updateWifiStateChange_wifiIsNotReadyAndWifiEnabled_updateWifiIsReady() {
        mInternetResetHelper.mIsWifiReady = false;
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isTrue();
    }

    @Test
    public void updateWifiStateChange_wifiIsReady_doNothing() {
        mInternetResetHelper.mIsWifiReady = true;

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isTrue();
    }

    @Test
    public void suspendPreferences_shouldShowResettingHideSubSys() {
        mInternetResetHelper.suspendPreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Hide subsystem preferences
        verify(mMobileNetworkController).hidePreference(true /* hide */, true /* immediately*/);
        assertThat(mWifiTogglePreferences.isVisible()).isFalse();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isFalse();
        assertThat(mFirstWifiEntryPreference.isVisible()).isFalse();
        assertThat(mWifiEntryPreferences.isVisible()).isFalse();
    }

    @Test
    public void resumePreferences_onlyRecoveryReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        when(mRecoveryWorker.isRecovering()).thenReturn(false);
        mInternetResetHelper.mIsWifiReady = false;

        mInternetResetHelper.resumePreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Show Mobile Network controller
        verify(mMobileNetworkController).hidePreference(false /* hide */, true /* immediately*/);
        // Hide Wi-Fi preferences
        assertThat(mWifiTogglePreferences.isVisible()).isFalse();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isFalse();
        assertThat(mFirstWifiEntryPreference.isVisible()).isFalse();
        assertThat(mWifiEntryPreferences.isVisible()).isFalse();
    }

    @Test
    public void resumePreferences_onlyWifiReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        when(mRecoveryWorker.isRecovering()).thenReturn(true);
        mInternetResetHelper.mIsWifiReady = true;

        mInternetResetHelper.resumePreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Show Wi-Fi preferences
        assertThat(mWifiTogglePreferences.isVisible()).isTrue();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isTrue();
        assertThat(mFirstWifiEntryPreference.isVisible()).isTrue();
        assertThat(mWifiEntryPreferences.isVisible()).isTrue();
        // Hide Mobile Network controller
        verify(mMobileNetworkController, never())
                .hidePreference(false /* hide */, true /* immediately*/);
    }

    @Test
    public void resumePreferences_allReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        when(mRecoveryWorker.isRecovering()).thenReturn(false);
        mInternetResetHelper.mIsWifiReady = true;

        mInternetResetHelper.resumePreferences();

        // Show subsystem preferences
        verify(mMobileNetworkController).hidePreference(false, true);
        assertThat(mWifiTogglePreferences.isVisible()).isTrue();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isTrue();
        assertThat(mFirstWifiEntryPreference.isVisible()).isTrue();
        assertThat(mWifiEntryPreferences.isVisible()).isTrue();
        // Hide resetting preference
        assertThat(mResettingPreference.isVisible()).isFalse();
    }

    @Test
    public void restart_recoveryNotAvailable_shouldDoTriggerSubsystemRestart() {
        when(mRecoveryWorker.isRecoveryAvailable()).thenReturn(false);

        mInternetResetHelper.restart();

        verify(mRecoveryWorker, never()).triggerRestart();
    }

    @Test
    public void restart_recoveryAvailable_triggerSubsystemRestart() {
        when(mRecoveryWorker.isRecoveryAvailable()).thenReturn(true);

        mInternetResetHelper.restart();

        verify(mHandlerInjector)
                .postDelayed(mInternetResetHelper.mTimeoutRunnable, RESTART_TIMEOUT_MS);
        verify(mRecoveryWorker).triggerRestart();
    }

    @Test
    public void checkRecovering_isRecovering_showResetting() {
        when(mRecoveryWorker.isRecovering()).thenReturn(true);

        mInternetResetHelper.checkRecovering();

        verify(mResettingPreference).setVisible(true);
    }

    @Test
    public void checkRecovering_isNotRecovering_doNotShowResetting() {
        when(mRecoveryWorker.isRecovering()).thenReturn(false);

        mInternetResetHelper.checkRecovering();

        verify(mResettingPreference, never()).setVisible(true);
    }
}
