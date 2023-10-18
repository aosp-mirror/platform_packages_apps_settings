/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.repository;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityManager;
import android.net.wifi.sharedconnectivity.app.SharedConnectivitySettingsState;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SharedConnectivityRepositoryTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private SharedConnectivityManager mManager;

    private SharedConnectivityRepository mRepository;
    private PendingIntent mIntent = PendingIntent
            .getActivity(mContext, 0, new Intent("test"), FLAG_IMMUTABLE);
    private SharedConnectivitySettingsState mState = new SharedConnectivitySettingsState.Builder()
            .setInstantTetherSettingsPendingIntent(mIntent).build();

    @Before
    public void setUp() {
        when(mContext.getSystemService(SharedConnectivityManager.class)).thenReturn(mManager);
        when(mManager.getSettingsState()).thenReturn(mState);

        mRepository = spy(new SharedConnectivityRepository(mContext, true /* isConfigEnabled */));
    }

    @Test
    public void constructor_configEnabled_registerCallback() {
        verify(mManager).registerCallback(any(), any());
    }

    @Test
    public void constructor_configNotEnabled_doNotRegisterCallback() {
        SharedConnectivityManager manager = mock(SharedConnectivityManager.class);
        when(mContext.getSystemService(SharedConnectivityManager.class)).thenReturn(manager);

        mRepository = new SharedConnectivityRepository(mContext, false /* isConfigEnabled */);

        verify(manager, never()).registerCallback(any(), any());
    }

    @Test
    public void isServiceAvailable_configEnabled_returnTrue() {
        mRepository = new SharedConnectivityRepository(mContext, true /* isConfigEnabled */);

        assertThat(mRepository.isServiceAvailable()).isTrue();
    }

    @Test
    public void isServiceAvailable_configNotEnabled_returnFalse() {
        mRepository = new SharedConnectivityRepository(mContext, false /* isConfigEnabled */);

        assertThat(mRepository.isServiceAvailable()).isFalse();
    }

    @Test
    public void getSettingsState_isNotNull() {
        assertThat(mRepository.getSettingsState()).isNotNull();
    }

    @Test
    public void handleLaunchSettings_managerNull_doNothing() {
        when(mContext.getSystemService(SharedConnectivityManager.class)).thenReturn(null);
        mRepository = spy(new SharedConnectivityRepository(mContext, true /* isConfigEnabled */));

        mRepository.handleLaunchSettings();

        verify(mRepository, never()).sendSettingsIntent(mIntent);
    }

    @Test
    public void handleLaunchSettings_stageNull_doNothing() {
        when(mManager.getSettingsState()).thenReturn(null);

        mRepository.handleLaunchSettings();

        verify(mRepository, never()).sendSettingsIntent(mIntent);
    }

    @Test
    public void handleLaunchSettings_intentNull_doNothing() {
        mState = new SharedConnectivitySettingsState.Builder()
                .setInstantTetherSettingsPendingIntent(null).build();
        when(mManager.getSettingsState()).thenReturn(mState);

        mRepository.handleLaunchSettings();

        verify(mRepository, never()).sendSettingsIntent(mIntent);
    }

    @Test
    public void handleLaunchSettings_allReady_sendSettingsIntent() {
        mRepository.handleLaunchSettings();

        verify(mRepository).sendSettingsIntent(mIntent);
    }
}
