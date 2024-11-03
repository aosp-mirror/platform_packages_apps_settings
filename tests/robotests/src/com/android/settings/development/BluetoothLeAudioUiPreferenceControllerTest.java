/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.BluetoothLeAudioUiPreferenceController.VALUE_KEY;
import static com.android.settings.development.BluetoothLeAudioUiPreferenceController.VALUE_OFF;
import static com.android.settings.development.BluetoothLeAudioUiPreferenceController.VALUE_ON;
import static com.android.settings.development.BluetoothLeAudioUiPreferenceController.VALUE_UNSET;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            BluetoothLeAudioUiPreferenceControllerTest.ShadowBluetoothRebootDialogFragment.class
        })
public class BluetoothLeAudioUiPreferenceControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String SOURCE_SYSTEM_PROP_KEY =
            "bluetooth.profile.bap.broadcast.source.enabled";
    private static final String ASSIST_SYSTEM_PROP_KEY =
            "bluetooth.profile.bap.broadcast.assist.enabled";
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private DevelopmentSettingsDashboardFragment mFragment;
    @Mock private SwitchPreferenceCompat mPreference;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Context mContext;
    private BluetoothLeAudioUiPreferenceController mController;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.getApplication();
        SystemProperties.set(SOURCE_SYSTEM_PROP_KEY, "true");
        SystemProperties.set(ASSIST_SYSTEM_PROP_KEY, "true");
        // Reset value
        Settings.Global.putInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mController = spy(new BluetoothLeAudioUiPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    @DisableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void isAvailable_flagOff_returnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void isAvailable_flagOn_returnFalse() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void isAvailable_flagOn_propertyOff_returnFalse() {
        SystemProperties.set(SOURCE_SYSTEM_PROP_KEY, "false");
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void updateState_settingEnabled_checked() {
        Settings.Global.putInt(mContext.getContentResolver(), VALUE_KEY, VALUE_ON);
        mController.updateState(mPreference);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference).setChecked(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void updateState_settingDisabled_notChecked() {
        Settings.Global.putInt(mContext.getContentResolver(), VALUE_KEY, VALUE_OFF);
        mController.updateState(mPreference);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference).setChecked(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void updateState_featureSupported_enabled() {
        mController.updateState(mPreference);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void updateState_featureUnsupported_disabled() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        mController.updateState(mPreference);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference).setEnabled(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void onRebootDialogConfirmed_noChange_doNothing() {
        mController.onRebootDialogConfirmed();

        int result = Settings.Global.getInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        assertThat(result).isEqualTo(VALUE_UNSET);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void onRebootDialogConfirmed_hasChange_turnOn() {
        mController.onPreferenceChange(mPreference, true);
        mController.onRebootDialogConfirmed();

        int result = Settings.Global.getInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        assertThat(result).isEqualTo(VALUE_ON);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void onRebootDialogCanceled_hasChange_doNothing() {
        mController.onPreferenceChange(mPreference, true);
        mController.onRebootDialogCanceled();

        int result = Settings.Global.getInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        assertThat(result).isEqualTo(VALUE_UNSET);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void onBroadcastDisabled_currentValueOn_turnOff() {
        Settings.Global.putInt(mContext.getContentResolver(), VALUE_KEY, VALUE_ON);
        mController.updateState(mPreference);
        shadowOf(Looper.getMainLooper()).idle();
        mController.onBroadcastDisabled();

        int result = Settings.Global.getInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        assertThat(result).isEqualTo(VALUE_OFF);
    }

    @Test
    @EnableFlags(Flags.FLAG_AUDIO_SHARING_DEVELOPER_OPTION)
    public void onBroadcastDisabled_currentValueUnset_doNothing() {
        mController.updateState(mPreference);
        mController.onBroadcastDisabled();
        shadowOf(Looper.getMainLooper()).idle();

        int result = Settings.Global.getInt(mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET);
        assertThat(result).isEqualTo(VALUE_UNSET);
    }

    @Implements(BluetoothRebootDialog.class)
    public static class ShadowBluetoothRebootDialogFragment {

        /** Shadow implementation of BluetoothRebootDialog#show */
        @Implementation
        public static void show(DevelopmentSettingsDashboardFragment host) {
            // Do nothing.
        }
    }
}
