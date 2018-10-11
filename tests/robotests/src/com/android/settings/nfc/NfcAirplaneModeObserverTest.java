/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.SwitchPreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowNfcAdapter.class})
public class NfcAirplaneModeObserverTest {

    Context mContext;
    private NfcAdapter mNfcAdapter;
    private SwitchPreference mNfcPreference;
    private NfcAirplaneModeObserver mNfcAirplaneModeObserver;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        mNfcPreference = new SwitchPreference(mContext);

        mNfcAirplaneModeObserver =
                new NfcAirplaneModeObserver(mContext, mNfcAdapter, mNfcPreference);
    }

    @Test
    public void NfcAirplaneModeObserver_airplaneOn_shouldDisableNfc() {
        ReflectionHelpers.setField(mNfcAirplaneModeObserver,
                "mAirplaneMode", 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 1);

        mNfcAirplaneModeObserver.onChange(false,
                NfcAirplaneModeObserver.AIRPLANE_MODE_URI);

        assertThat(mNfcAdapter.isEnabled()).isFalse();
    }

    @Test
    public void NfcAirplaneModeObserver_airplaneModeOnNfcToggleable_shouldEnablePreference() {
        ReflectionHelpers.setField(mNfcAirplaneModeObserver, "mAirplaneMode", 0);
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        Settings.Global.putString(contentResolver,
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, Settings.Global.RADIO_NFC);

        mNfcAirplaneModeObserver.onChange(false, NfcAirplaneModeObserver.AIRPLANE_MODE_URI);

        assertThat(mNfcPreference.isEnabled()).isTrue();
    }

    @Test
    public void NfcAirplaneModeObserver_airplaneModeOnNfcNotToggleable_shouldDisablePreference() {
        ReflectionHelpers.setField(mNfcAirplaneModeObserver, "mAirplaneMode", 0);
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        Settings.Global.putString(contentResolver,
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, Global.RADIO_WIFI);

        mNfcAirplaneModeObserver.onChange(false, NfcAirplaneModeObserver.AIRPLANE_MODE_URI);

        assertThat(mNfcPreference.isEnabled()).isFalse();
    }

    @Test
    public void NfcAirplaneModeObserver_airplaneModeOff_shouldEnablePreference() {
        ReflectionHelpers.setField(mNfcAirplaneModeObserver, "mAirplaneMode", 1);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        mNfcAirplaneModeObserver.onChange(false, NfcAirplaneModeObserver.AIRPLANE_MODE_URI);

        assertThat(mNfcPreference.isEnabled()).isTrue();
    }

    @Test
    public void NfcAirplaneModeObserver_airplaneModeOff_shouldNotEnableNfcAutomatically() {
        ReflectionHelpers.setField(mNfcAirplaneModeObserver, "mAirplaneMode", 1);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        mNfcAirplaneModeObserver.onChange(false, NfcAirplaneModeObserver.AIRPLANE_MODE_URI);

        assertThat(mNfcAdapter.isEnabled()).isFalse();
    }
}
