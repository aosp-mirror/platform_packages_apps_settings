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

package com.android.settings.devicelock;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.devicelock.DeviceLockManager;
import android.os.OutcomeReceiver;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public final class DeviceLockPreferenceControllerTest {

    private static final String TEST_PREFERENCE_KEY = "KEY";
    private static final Map<Integer, String> TEST_KIOSK_APPS = Map.of(0, "test");
    @Mock
    private DeviceLockManager mDeviceLockManager;
    @Captor
    private ArgumentCaptor<OutcomeReceiver<Map<Integer, String>, Exception>>
            mOutcomeReceiverArgumentCaptor;
    private DeviceLockPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        Context context = spy(mContext);
        when(context.getSystemService(DeviceLockManager.class)).thenReturn(mDeviceLockManager);
        mController = new DeviceLockPreferenceController(context, TEST_PREFERENCE_KEY);
    }

    @Test
    public void testUpdateState_preferenceBecomesInvisibleIfNoKioskAppsPresent() {
        Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        mController.updateState(preference);

        verify(mDeviceLockManager).getKioskApps(any(), mOutcomeReceiverArgumentCaptor.capture());
        OutcomeReceiver<Map<Integer, String>, Exception> outcomeReceiver =
                mOutcomeReceiverArgumentCaptor.getValue();

        outcomeReceiver.onResult(Collections.emptyMap());
        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void testUpdateState_preferenceBecomesVisibleIfKioskAppsPresent() {
        Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(false);

        mController.updateState(preference);

        verify(mDeviceLockManager).getKioskApps(any(), mOutcomeReceiverArgumentCaptor.capture());
        OutcomeReceiver<Map<Integer, String>, Exception> outcomeReceiver =
                mOutcomeReceiverArgumentCaptor.getValue();

        outcomeReceiver.onResult(TEST_KIOSK_APPS);
        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void testUpdateState_preferenceBecomesInvisibleIfDeviceLockManagerIsNotAvailable() {
        Context context = spy(mContext);
        when(context.getSystemService(DeviceLockManager.class)).thenReturn(null);
        mController = new DeviceLockPreferenceController(context, TEST_PREFERENCE_KEY);

        Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();
    }
}
