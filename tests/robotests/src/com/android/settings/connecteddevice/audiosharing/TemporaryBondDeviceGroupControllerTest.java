/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests for {@link TemporaryBondDeviceGroupController}. */
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowBluetoothAdapter.class,
                ShadowBluetoothUtils.class
        })
public class TemporaryBondDeviceGroupControllerTest {
    private static final String KEY = "temp_bond_device_list";
    private static final String PREFERENCE_KEY_1 = "pref_key_1";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private TemporaryBondDeviceGroupUpdater mBluetoothDeviceUpdater;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    @Mock
    private LocalBluetoothManager mLocalBtManager;
    @Mock
    private BluetoothEventManager mEventManager;
    @Mock private PreferenceScreen mScreen;


    private PreferenceGroup mPreferenceGroup;
    private Context mContext;
    private Preference mPreference;
    private TemporaryBondDeviceGroupController mTemporaryBondDeviceGroupController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY_1);
        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(KEY)).thenReturn(mPreferenceGroup);

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getEventManager()).thenReturn(mEventManager);
        ShadowBluetoothAdapter shadowBluetoothAdapter = Shadow.extract(
                BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

        mTemporaryBondDeviceGroupController = spy(new TemporaryBondDeviceGroupController(mContext));
        mTemporaryBondDeviceGroupController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mTemporaryBondDeviceGroupController.setPreferenceGroup(mPreferenceGroup);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI)
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onStart_flagOff_doNothing() {
        mTemporaryBondDeviceGroupController.onStart(mLifecycleOwner);

        verify(mEventManager, never()).registerCallback(any(BluetoothCallback.class));
        verify(mBluetoothDeviceUpdater, never()).registerCallback();
        verify(mBluetoothDeviceUpdater, never()).refreshPreference();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI)
    public void onStart_audioSharingUINotAvailable_doNothing() {
        mTemporaryBondDeviceGroupController.onStart(mLifecycleOwner);

        verify(mEventManager, never()).registerCallback(any(BluetoothCallback.class));
        verify(mBluetoothDeviceUpdater, never()).registerCallback();
        verify(mBluetoothDeviceUpdater, never()).refreshPreference();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void onStart_registerCallbacks() {
        mTemporaryBondDeviceGroupController.onStart(mLifecycleOwner);

        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mBluetoothDeviceUpdater).refreshPreference();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void onStop_unregisterCallbacks() {
        mTemporaryBondDeviceGroupController.onStop(mLifecycleOwner);

        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
        verify(mBluetoothDeviceUpdater).unregisterCallback();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI)
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void displayPreference_flagOff_doNothing() {
        mTemporaryBondDeviceGroupController.displayPreference(mScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        verify(mBluetoothDeviceUpdater, never()).forceUpdate();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI)
    public void displayPreference_audioSharingUINotAvailable_doNothing() {
        mTemporaryBondDeviceGroupController.displayPreference(mScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        verify(mBluetoothDeviceUpdater, never()).forceUpdate();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void displayPreference_updateDeviceList() {
        mTemporaryBondDeviceGroupController.displayPreference(mScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        verify(mBluetoothDeviceUpdater).setPrefContext(mContext);
        verify(mBluetoothDeviceUpdater).forceUpdate();
    }

    @Test
    public void onDeviceAdded_firstAdd_becomeVisibleAndPreferenceAdded() {
        mTemporaryBondDeviceGroupController.onDeviceAdded(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onDeviceRemoved_lastRemove_becomeInvisibleAndPreferenceRemoved() {
        mPreferenceGroup.addPreference(mPreference);

        mTemporaryBondDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onDeviceRemoved_notLastRemove_stillVisible() {
        mPreferenceGroup.setVisible(true);
        mPreferenceGroup.addPreference(mPreference);
        mPreferenceGroup.addPreference(new Preference(mContext));

        mTemporaryBondDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mTemporaryBondDeviceGroupController.getPreferenceKey()).isEqualTo(KEY);
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void getAvailabilityStatus_returnsAvailable() {
        assertThat(mTemporaryBondDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }
}
