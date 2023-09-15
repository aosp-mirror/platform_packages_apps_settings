/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.safetycenter;

import static android.safetycenter.SafetyCenterManager.ACTION_REFRESH_SAFETY_SOURCES;
import static android.safetycenter.SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID;
import static android.safetycenter.SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCE_IDS;
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_DEVICE_REBOOTED;
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.privatespace.PrivateSpaceSafetySource;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SafetySourceBroadcastReceiverTest {

    private static final String REFRESH_BROADCAST_ID = "REFRESH_BROADCAST_ID";

    private Context mApplicationContext;

    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock private LockPatternUtils mLockPatternUtils;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = ApplicationProvider.getApplicationContext();
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mApplicationContext))
                .thenReturn(mLockPatternUtils);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    public void onReceive_onRefresh_whenSafetyCenterIsDisabled_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {LockScreenSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void onReceive_onRefresh_whenSafetyCenterIsEnabled_withNoIntentAction_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {LockScreenSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void onReceive_onRefresh_whenSafetyCenterIsEnabled_withNullSourceIds_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void onReceive_onRefresh_whenSafetyCenterIsEnabled_withNoSourceIds_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS, new String[] {})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void onReceive_onRefresh_whenSafetyCenterIsEnabled_withNoBroadcastId_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {LockScreenSafetySource.SAFETY_SOURCE_ID});

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void onReceive_onRefresh_setsRefreshEvent() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {LockScreenSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<SafetyEvent> captor = ArgumentCaptor.forClass(SafetyEvent.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .setSafetySourceData(any(), any(), any(), captor.capture());

        assertThat(captor.getValue())
                .isEqualTo(
                        new SafetyEvent.Builder(SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
                                .setRefreshBroadcastId(REFRESH_BROADCAST_ID)
                                .build());
    }

    @Test
    public void onReceive_onRefresh_withLockscreenSourceId_setsLockscreenData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {LockScreenSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .setSafetySourceData(any(), captor.capture(), any(), any());

        assertThat(captor.getValue()).isEqualTo(LockScreenSafetySource.SAFETY_SOURCE_ID);
    }

    @Test
    public void onReceive_onRefresh_withBiometricsSourceId_setsBiometricData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {BiometricsSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .setSafetySourceData(any(), captor.capture(), any(), any());

        assertThat(captor.getValue()).isEqualTo(BiometricsSafetySource.SAFETY_SOURCE_ID);
    }

    /**
     *  Tests that on receiving the refresh broadcast request with the PS source id, the PS data
     * is set.
     */
    @Test
    public void onReceive_onRefresh_withPrivateSpaceSourceId_setsPrivateSpaceData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {PrivateSpaceSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .setSafetySourceData(any(), captor.capture(), any(), any());

        assertThat(captor.getValue()).isEqualTo(PrivateSpaceSafetySource.SAFETY_SOURCE_ID);
    }

    /** Tests that the PS source sets null data when it's disabled. */
    @Test
    public void onReceive_onRefresh_withPrivateSpaceFeatureDisabled_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        mSetFlagsRule.disableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        Intent intent =
                new Intent()
                        .setAction(ACTION_REFRESH_SAFETY_SOURCES)
                        .putExtra(
                                EXTRA_REFRESH_SAFETY_SOURCE_IDS,
                                new String[] {PrivateSpaceSafetySource.SAFETY_SOURCE_ID})
                        .putExtra(EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID, REFRESH_BROADCAST_ID);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper, times(1))
                .setSafetySourceData(any(), any(), captor.capture(), any());

        assertThat(captor.getValue()).isEqualTo(null);
    }

    @Test
    public void onReceive_onBootCompleted_setsBootCompleteEvent() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent = new Intent().setAction(Intent.ACTION_BOOT_COMPLETED);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<SafetyEvent> captor = ArgumentCaptor.forClass(SafetyEvent.class);
        verify(mSafetyCenterManagerWrapper, times(3))
                .setSafetySourceData(any(), any(), any(), captor.capture());

        SafetyEvent bootEvent = new SafetyEvent.Builder(SAFETY_EVENT_TYPE_DEVICE_REBOOTED).build();
        assertThat(captor.getAllValues())
                .containsExactlyElementsIn(Arrays.asList(bootEvent, bootEvent, bootEvent));
    }

    @Test
    public void onReceive_onBootCompleted_sendsAllSafetySourcesData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        Intent intent = new Intent().setAction(Intent.ACTION_BOOT_COMPLETED);

        new SafetySourceBroadcastReceiver().onReceive(mApplicationContext, intent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mSafetyCenterManagerWrapper, times(3))
                .setSafetySourceData(any(), captor.capture(), any(), any());
        List<String> safetySourceIdList = captor.getAllValues();

        assertThat(safetySourceIdList.stream().anyMatch(
                id -> id.equals(LockScreenSafetySource.SAFETY_SOURCE_ID))).isTrue();
        assertThat(safetySourceIdList.stream().anyMatch(
                id -> id.equals(BiometricsSafetySource.SAFETY_SOURCE_ID))).isTrue();
        assertThat(safetySourceIdList.stream().anyMatch(
                id -> id.equals(PrivateSpaceSafetySource.SAFETY_SOURCE_ID))).isTrue();
    }
}
