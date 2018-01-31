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

package com.android.settings.connecteddevice.usb;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.android.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbDetailsProfilesControllerTest {

    private UsbDetailsProfilesController mDetailsProfilesController;
    private Context mContext;
    private Lifecycle mLifecycle;
    private PreferenceCategory mPreference;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private List<String> mOptions;

    @Mock
    private UsbBackend mUsbBackend;
    @Mock
    private PreferenceFragment mFragment;
    @Mock
    private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);

        mOptions = Lists.newArrayList(UsbManager.USB_FUNCTION_MTP, UsbManager.USB_FUNCTION_PTP,
                UsbManager.USB_FUNCTION_MIDI, UsbDetailsProfilesController.KEY_POWER);
        mDetailsProfilesController = new UsbDetailsProfilesController(mContext, mFragment,
                mUsbBackend, mOptions, "usb_options");
        mPreference = new PreferenceCategory(mContext);
        mPreference.setKey(mDetailsProfilesController.getPreferenceKey());
        mScreen.addPreference(mPreference);
    }

    @Test
    public void testDisplayRefresh_allAllowed_shouldCreateSwitches() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_NONE);
        List<SwitchPreference> switches = getProfileSwitches();

        for (int i = 0; i < switches.size(); i++) {
            assertThat(switches.get(i).getKey().equals(mOptions.get(i)));
        }
    }

    @Test
    public void testDisplayRefresh_onlyMidiAllowed_shouldCreateOnlyMidiSwitch() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(UsbBackend.MODE_DATA_MIDI)).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(UsbBackend.MODE_DATA_MTP)).thenReturn(true);
        when(mUsbBackend.isModeDisallowedBySystem(UsbBackend.MODE_DATA_PTP)).thenReturn(true);
        when(mUsbBackend.isModeDisallowedBySystem(UsbBackend.MODE_POWER_SOURCE)).thenReturn(true);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_NONE);
        List<SwitchPreference> switches = getProfileSwitches();
        assertThat(switches.size()).isEqualTo(1);
        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MIDI);
    }

    @Test
    public void testDisplayRefresh_mtpEnabled_shouldCheckSwitches() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_MTP);
        List<SwitchPreference> switches = getProfileSwitches();

        assertThat(switches.get(0).getKey().equals(UsbManager.USB_FUNCTION_MTP));
        assertThat(switches.get(0).isChecked());
    }

    @Test
    public void testDisplayRefresh_mtpSupplyPowerEnabled_shouldCheckSwitches() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_MTP | UsbBackend.MODE_POWER_SOURCE);
        List<SwitchPreference> switches = getProfileSwitches();

        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
        assertThat(switches.get(0).isChecked());
        assertThat(switches.get(3).getKey()).isEqualTo(UsbDetailsProfilesController.KEY_POWER);
        assertThat(switches.get(3).isChecked());
    }

    @Test
    public void testOnClickMtp_noneEnabled_shouldEnableMtp() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_NONE);
        List<SwitchPreference> switches = getProfileSwitches();
        switches.get(0).performClick();

        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
        verify(mUsbBackend).setMode(UsbBackend.MODE_DATA_MTP);
        assertThat(switches.get(0).isChecked());
    }

    @Test
    public void testOnClickMtp_supplyingPowerEnabled_shouldEnableBoth() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_POWER_SOURCE);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbBackend.MODE_POWER_SOURCE);
        List<SwitchPreference> switches = getProfileSwitches();
        switches.get(0).performClick();

        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
        verify(mUsbBackend).setMode(UsbBackend.MODE_DATA_MTP | UsbBackend.MODE_POWER_SOURCE);
        assertThat(switches.get(0).isChecked());
        assertThat(switches.get(3).getKey()).isEqualTo(UsbDetailsProfilesController.KEY_POWER);
        assertThat(switches.get(3).isChecked());
    }

    @Test
    public void testOnClickMtp_ptpEnabled_shouldEnableMtpOnly() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_PTP);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbBackend.MODE_DATA_PTP);
        List<SwitchPreference> switches = getProfileSwitches();
        switches.get(0).performClick();

        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
        verify(mUsbBackend).setMode(UsbBackend.MODE_DATA_MTP);
        assertThat(switches.get(0).isChecked());
        assertThat(switches.get(1).getKey()).isEqualTo(UsbManager.USB_FUNCTION_PTP);
        assertThat(!switches.get(1).isChecked());
    }

    @Test
    public void testOnClickMtp_mtpEnabled_shouldDisableMtp() {
        when(mUsbBackend.isModeSupported(anyInt())).thenReturn(true);
        when(mUsbBackend.isModeDisallowed(anyInt())).thenReturn(false);
        when(mUsbBackend.isModeDisallowedBySystem(anyInt())).thenReturn(false);

        mDetailsProfilesController.displayPreference(mScreen);
        mDetailsProfilesController.refresh(UsbBackend.MODE_DATA_MTP);
        when(mUsbBackend.getCurrentMode()).thenReturn(UsbBackend.MODE_DATA_MTP);
        List<SwitchPreference> switches = getProfileSwitches();
        switches.get(0).performClick();

        assertThat(switches.get(0).getKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
        verify(mUsbBackend).setMode(UsbBackend.MODE_DATA_NONE);
        assertThat(!switches.get(0).isChecked());
    }

    private List<SwitchPreference> getProfileSwitches() {
        ArrayList<SwitchPreference> result = new ArrayList<>();
        for (int i = 0; i < mPreference.getPreferenceCount(); i++) {
            result.add((SwitchPreference) mPreference.getPreference(i));
        }
        return result;
    }
}
