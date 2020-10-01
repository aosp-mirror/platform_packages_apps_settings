/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSettings;


@RunWith(AndroidJUnit4.class)
public final class AirplaneModeEnablerTest {

    private Context mContext;

    @Mock
    private AirplaneModeChangedListener mAirplaneModeChangedListener;
    private AirplaneModeEnabler mAirplaneModeEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext,
                mAirplaneModeChangedListener);
    }

    @Test
    public void onRadioPowerStateChanged_beenInvoke_invokeOnAirplaneModeChanged() {
        mAirplaneModeEnabler.start();

        ShadowSettings.setAirplaneMode(true);

        mAirplaneModeEnabler.mPhoneStateListener.onRadioPowerStateChanged(
                TelephonyManager.RADIO_POWER_OFF);

        verify(mAirplaneModeChangedListener, times(1)).onAirplaneModeChanged(true);
    }

    private class AirplaneModeChangedListener
            implements AirplaneModeEnabler.OnAirplaneModeChangedListener {
        public void onAirplaneModeChanged(boolean isAirplaneModeOn) {}
    }
}
