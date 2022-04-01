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
import android.os.HandlerThread;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.connectivity.ConnectivitySubsystemsRecoveryManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class InternetResetHelperTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    public HandlerThread mWorkerThread;
    @Mock
    public ConnectivitySubsystemsRecoveryManager mConnectivitySubsystemsRecoveryManager;
    @Mock
    public NetworkMobileProviderController mMobileNetworkController;

    private Context mContext;
    private InternetResetHelper mInternetResetHelper;
    private Preference mResettingPreference;
    private Preference mWifiTogglePreferences;
    private PreferenceCategory mConnectedWifiEntryPreferences;
    private PreferenceCategory mWifiEntryPreferences;

    private FakeHandlerInjector mFakeHandlerInjector;

    private static class FakeHandlerInjector extends InternetResetHelper.HandlerInjector {

        private Runnable mRunnable;

        FakeHandlerInjector(Context context) {
            super(context);
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMillis) {
            mRunnable = runnable;
        }

        public Runnable getRunnable() {
            return mRunnable;
        }
    }

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mResettingPreference = new Preference(mContext);
        mWifiTogglePreferences = new Preference(mContext);
        mConnectedWifiEntryPreferences = spy(new PreferenceCategory(mContext));
        mWifiEntryPreferences = spy(new PreferenceCategory(mContext));

        final Lifecycle lifecycle = mock(Lifecycle.class);
        mInternetResetHelper = new InternetResetHelper(mContext, lifecycle);
        mInternetResetHelper.mWorkerThread = mWorkerThread;
        mFakeHandlerInjector = new FakeHandlerInjector(mContext);
        mInternetResetHelper.mHandlerInjector = mFakeHandlerInjector;
        mInternetResetHelper.mConnectivitySubsystemsRecoveryManager =
                mConnectivitySubsystemsRecoveryManager;
        mInternetResetHelper.setResettingPreference(mResettingPreference);
        mInternetResetHelper.setMobileNetworkController(mMobileNetworkController);
        mInternetResetHelper.setWifiTogglePreference(mWifiTogglePreferences);
        mInternetResetHelper.addWifiNetworkPreference(mConnectedWifiEntryPreferences);
        mInternetResetHelper.addWifiNetworkPreference(mWifiEntryPreferences);
    }

    @Test
    public void onResume_registerReceiver() {
        mInternetResetHelper.onResume();

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onPause_unregisterReceiver() {
        mInternetResetHelper.onResume();

        mInternetResetHelper.onPause();

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onDestroy_quitWorkerThread() {
        mInternetResetHelper.onDestroy();

        verify(mWorkerThread).quit();
    }

    @Test
    public void onSubsystemRestartOperationEnd_recoveryIsNotReady_postResumeRunnable() {
        mInternetResetHelper.mIsRecoveryReady = false;

        mInternetResetHelper.onSubsystemRestartOperationEnd();

        assertThat(mInternetResetHelper.mIsRecoveryReady).isTrue();
        assertThat(mFakeHandlerInjector.getRunnable())
                .isEqualTo(mInternetResetHelper.mResumeRunnable);
    }

    @Test
    public void onSubsystemRestartOperationEnd_recoveryIsReady_doNothing() {
        mInternetResetHelper.mIsRecoveryReady = true;

        mInternetResetHelper.onSubsystemRestartOperationEnd();

        assertThat(mFakeHandlerInjector.getRunnable()).isNull();
    }

    @Test
    public void updateWifiStateChange_wifiIsNotReadyAndWifiDisabled_doNothing() {
        mInternetResetHelper.mIsWifiReady = false;
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isFalse();
        assertThat(mFakeHandlerInjector.getRunnable()).isNull();
    }

    @Test
    public void updateWifiStateChange_wifiIsNotReadyAndWifiEnabled_postResumeRunnable() {
        mInternetResetHelper.mIsWifiReady = false;
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isTrue();
        assertThat(mFakeHandlerInjector.getRunnable())
                .isEqualTo(mInternetResetHelper.mResumeRunnable);
    }

    @Test
    public void updateWifiStateChange_wifiIsReady_doNothing() {
        mInternetResetHelper.mIsWifiReady = true;

        mInternetResetHelper.updateWifiStateChange();

        assertThat(mInternetResetHelper.mIsWifiReady).isTrue();
        assertThat(mFakeHandlerInjector.getRunnable()).isNull();
    }

    @Test
    public void suspendPreferences_shouldShowResettingHideSubSys() {
        mInternetResetHelper.suspendPreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Hide subsystem preferences
        verify(mMobileNetworkController).hidePreference(true /* hide */, true /* immediately*/);
        assertThat(mWifiTogglePreferences.isVisible()).isFalse();
        verify(mConnectedWifiEntryPreferences).removeAll();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isFalse();
        verify(mWifiEntryPreferences).removeAll();
        assertThat(mWifiEntryPreferences.isVisible()).isFalse();
    }

    @Test
    public void resumePreferences_onlyRecoveryReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        mInternetResetHelper.mIsRecoveryReady = true;
        mInternetResetHelper.mIsWifiReady = false;

        mInternetResetHelper.resumePreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Show Mobile Network controller
        verify(mMobileNetworkController).hidePreference(false /* hide */, true /* immediately*/);
        // Hide Wi-Fi preferences
        assertThat(mWifiTogglePreferences.isVisible()).isFalse();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isFalse();
        assertThat(mWifiEntryPreferences.isVisible()).isFalse();
    }

    @Test
    public void resumePreferences_onlyWifiReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        mInternetResetHelper.mIsRecoveryReady = false;
        mInternetResetHelper.mIsWifiReady = true;

        mInternetResetHelper.resumePreferences();

        // Show resetting preference
        assertThat(mResettingPreference.isVisible()).isTrue();
        // Show Wi-Fi preferences
        assertThat(mWifiTogglePreferences.isVisible()).isTrue();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isTrue();
        assertThat(mWifiEntryPreferences.isVisible()).isTrue();
        // Hide Mobile Network controller
        verify(mMobileNetworkController, never())
                .hidePreference(false /* hide */, true /* immediately*/);
    }

    @Test
    public void resumePreferences_allReady_shouldShowSubSysHideResetting() {
        mInternetResetHelper.suspendPreferences();
        mInternetResetHelper.mIsRecoveryReady = true;
        mInternetResetHelper.mIsWifiReady = true;
        mInternetResetHelper.resumePreferences();

        // Show subsystem preferences
        verify(mMobileNetworkController).hidePreference(false, true);
        assertThat(mWifiTogglePreferences.isVisible()).isTrue();
        assertThat(mConnectedWifiEntryPreferences.isVisible()).isTrue();
        assertThat(mWifiEntryPreferences.isVisible()).isTrue();
        // Hide resetting preference
        assertThat(mResettingPreference.isVisible()).isFalse();
    }

    @Test
    public void restart_recoveryNotAvailable_shouldDoTriggerSubsystemRestart() {
        when(mConnectivitySubsystemsRecoveryManager.isRecoveryAvailable()).thenReturn(false);

        mInternetResetHelper.restart();

        verify(mConnectivitySubsystemsRecoveryManager, never())
                .triggerSubsystemRestart(any(), any());
    }

    @Test
    public void restart_recoveryAvailable_triggerSubsystemRestart() {
        when(mConnectivitySubsystemsRecoveryManager.isRecoveryAvailable()).thenReturn(true);

        mInternetResetHelper.restart();

        assertThat(mFakeHandlerInjector.getRunnable())
                .isEqualTo(mInternetResetHelper.mTimeoutRunnable);
        verify(mConnectivitySubsystemsRecoveryManager).triggerSubsystemRestart(any(), any());
    }
}
