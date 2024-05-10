/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.sysprop.BluetoothProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowBluetoothDevice;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;

import com.google.common.collect.Lists;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Ignore
@Config(shadows = ShadowBluetoothDevice.class)
public class BluetoothDetailsProfilesControllerTest extends BluetoothDetailsControllerTestBase {

    private static final String LE_DEVICE_MODEL = "le_audio_headset";
    private static final String NON_LE_DEVICE_MODEL = "non_le_audio_headset";
    private BluetoothDetailsProfilesController mController;
    private List<LocalBluetoothProfile> mConnectableProfiles;
    private PreferenceCategory mProfiles;

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;

    @Override
    public void setUp() {
        super.setUp();

        mProfiles = spy(new PreferenceCategory(mContext));
        when(mProfiles.getPreferenceManager()).thenReturn(mPreferenceManager);

        mConnectableProfiles = new ArrayList<>();
        when(mLocalManager.getProfileManager()).thenReturn(mProfileManager);
        when(mCachedDevice.getConnectableProfiles()).thenAnswer(invocation ->
            new ArrayList<>(mConnectableProfiles)
        );

        setupDevice(mDeviceConfig);
        mController = new BluetoothDetailsProfilesController(mContext, mFragment, mLocalManager,
                mCachedDevice, mLifecycle);
        mProfiles.setKey(mController.getPreferenceKey());
        mController.mProfilesContainer = mProfiles;
        mScreen.addPreference(mProfiles);
        BluetoothProperties.le_audio_allow_list(Lists.newArrayList(LE_DEVICE_MODEL));
    }

    static class FakeBluetoothProfile implements LocalBluetoothProfile {

        private Set<BluetoothDevice> mConnectedDevices = new HashSet<>();
        private Map<BluetoothDevice, Boolean> mPreferred = new HashMap<>();
        private Context mContext;
        private int mNameResourceId;

        private FakeBluetoothProfile(Context context, int nameResourceId) {
            mContext = context;
            mNameResourceId = nameResourceId;
        }

        @Override
        public String toString() {
            return mContext.getString(mNameResourceId);
        }

        @Override
        public boolean accessProfileEnabled() {
            return true;
        }

        @Override
        public boolean isAutoConnectable() {
            return true;
        }

        @Override
        public int getConnectionStatus(BluetoothDevice device) {
            if (mConnectedDevices.contains(device)) {
                return BluetoothProfile.STATE_CONNECTED;
            } else {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }

        @Override
        public boolean isEnabled(BluetoothDevice device) {
            return mPreferred.getOrDefault(device, false);
        }

        @Override
        public int getConnectionPolicy(BluetoothDevice device) {
            return isEnabled(device)
                    ? BluetoothProfile.CONNECTION_POLICY_ALLOWED
                    : BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        }

        @Override
        public boolean setEnabled(BluetoothDevice device, boolean enabled) {
            mPreferred.put(device, enabled);
            return true;
        }

        @Override
        public boolean isProfileReady() {
            return true;
        }

        @Override
        public int getProfileId() {
            return 0;
        }

        @Override
        public int getOrdinal() {
            return 0;
        }

        @Override
        public int getNameResource(BluetoothDevice device) {
            return mNameResourceId;
        }

        @Override
        public int getSummaryResourceForDevice(BluetoothDevice device) {
            return Utils.getConnectionStateSummary(getConnectionStatus(device));
        }

        @Override
        public int getDrawableResource(BluetoothClass btClass) {
            return 0;
        }
    }

    /**
     * Creates and adds a mock LocalBluetoothProfile to the list of connectable profiles for the
     * device.
     * @param profileNameResId  the resource id for the name used by this profile
     * @param deviceIsPreferred  whether this profile should start out as enabled for the device
     */
    private LocalBluetoothProfile addFakeProfile(int profileNameResId,
            boolean deviceIsPreferred) {
        LocalBluetoothProfile profile = new FakeBluetoothProfile(mContext, profileNameResId);
        profile.setEnabled(mDevice, deviceIsPreferred);
        mConnectableProfiles.add(profile);
        when(mProfileManager.getProfileByName(eq(profile.toString()))).thenReturn(profile);
        return profile;
    }

    /** Returns the list of SwitchPreference objects added to the screen - there should be one per
     *  Bluetooth profile.
     */
    private List<SwitchPreference> getProfileSwitches(boolean expectOnlyMConnectable) {
        if (expectOnlyMConnectable) {
            assertThat(mConnectableProfiles).isNotEmpty();
            assertThat(mProfiles.getPreferenceCount() - 1).isEqualTo(mConnectableProfiles.size());
        }
        List<SwitchPreference> result = new ArrayList<>();
        for (int i = 0; i < mProfiles.getPreferenceCount(); i++) {
            final Preference preference = mProfiles.getPreference(i);
            if (preference instanceof SwitchPreference) {
                result.add((SwitchPreference) preference);
            }
        }
        return result;
    }

     private void verifyProfileSwitchTitles(List<SwitchPreference> switches) {
        for (int i = 0; i < switches.size(); i++) {
            String expectedTitle =
                mContext.getString(mConnectableProfiles.get(i).getNameResource(mDevice));
            assertThat(switches.get(i).getTitle()).isEqualTo(expectedTitle);
        }
    }

    @Test
    public void oneProfile() {
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_a2dp, true);
        showScreen(mController);
        verifyProfileSwitchTitles(getProfileSwitches(true));
    }

    @Test
    public void multipleProfiles() {
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_a2dp, true);
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_headset, false);
        showScreen(mController);
        List<SwitchPreference> switches = getProfileSwitches(true);
        verifyProfileSwitchTitles(switches);
        assertThat(switches.get(0).isChecked()).isTrue();
        assertThat(switches.get(1).isChecked()).isFalse();

        // Both switches should be enabled.
        assertThat(switches.get(0).isEnabled()).isTrue();
        assertThat(switches.get(1).isEnabled()).isTrue();

        // Make device busy.
        when(mCachedDevice.isBusy()).thenReturn(true);
        mController.onDeviceAttributesChanged();

        // There should have been no new switches added.
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(3);

        // Make sure both switches got disabled.
        assertThat(switches.get(0).isEnabled()).isFalse();
        assertThat(switches.get(1).isEnabled()).isFalse();
    }

    @Test
    public void disableThenReenableOneProfile() {
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_a2dp, true);
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_headset, true);
        showScreen(mController);
        List<SwitchPreference> switches = getProfileSwitches(true);
        SwitchPreference pref = switches.get(0);

        // Clicking the pref should cause the profile to become not-preferred.
        assertThat(pref.isChecked()).isTrue();
        pref.performClick();
        assertThat(pref.isChecked()).isFalse();
        assertThat(mConnectableProfiles.get(0).isEnabled(mDevice)).isFalse();

        // Make sure no new preferences were added.
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(3);

        // Clicking the pref again should make the profile once again preferred.
        pref.performClick();
        assertThat(pref.isChecked()).isTrue();
        assertThat(mConnectableProfiles.get(0).isEnabled(mDevice)).isTrue();

        // Make sure we still haven't gotten any new preferences added.
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(3);
    }

    @Test
    public void disconnectedDeviceOneProfile() {
        setupDevice(makeDefaultDeviceConfig().setConnected(false).setConnectionSummary(null));
        addFakeProfile(com.android.settingslib.R.string.bluetooth_profile_a2dp, true);
        showScreen(mController);
        verifyProfileSwitchTitles(getProfileSwitches(true));
    }

    @Test
    public void pbapProfileStartsEnabled() {
        setupDevice(makeDefaultDeviceConfig());
        mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
        PbapServerProfile psp = mock(PbapServerProfile.class);
        when(psp.getNameResource(mDevice))
                .thenReturn(com.android.settingslib.R.string.bluetooth_profile_pbap);
        when(psp.toString()).thenReturn(PbapServerProfile.NAME);
        when(psp.isProfileReady()).thenReturn(true);
        when(mProfileManager.getPbapProfile()).thenReturn(psp);

        showScreen(mController);
        List<SwitchPreference> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreference pref = switches.get(0);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_pbap));
        assertThat(pref.isChecked()).isTrue();

        pref.performClick();
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        assertThat(mDevice.getPhonebookAccessPermission())
                .isEqualTo(BluetoothDevice.ACCESS_REJECTED);
    }

    @Test
    public void pbapProfileStartsDisabled() {
        setupDevice(makeDefaultDeviceConfig());
        mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
        PbapServerProfile psp = mock(PbapServerProfile.class);
        when(psp.getNameResource(mDevice))
                .thenReturn(com.android.settingslib.R.string.bluetooth_profile_pbap);
        when(psp.toString()).thenReturn(PbapServerProfile.NAME);
        when(psp.isProfileReady()).thenReturn(true);
        when(mProfileManager.getPbapProfile()).thenReturn(psp);

        showScreen(mController);
        List<SwitchPreference> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreference pref = switches.get(0);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_pbap));
        assertThat(pref.isChecked()).isFalse();

        pref.performClick();
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        assertThat(mDevice.getPhonebookAccessPermission())
                .isEqualTo(BluetoothDevice.ACCESS_ALLOWED);
    }

    @Test
    public void mapProfile() {
        setupDevice(makeDefaultDeviceConfig());
        MapProfile mapProfile = mock(MapProfile.class);
        when(mapProfile.getNameResource(mDevice))
                .thenReturn(com.android.settingslib.R.string.bluetooth_profile_map);
        when(mapProfile.isProfileReady()).thenReturn(true);
        when(mProfileManager.getMapProfile()).thenReturn(mapProfile);
        when(mProfileManager.getProfileByName(eq(mapProfile.toString()))).thenReturn(mapProfile);
        mDevice.setMessageAccessPermission(BluetoothDevice.ACCESS_REJECTED);
        showScreen(mController);
        List<SwitchPreference> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreference pref = switches.get(0);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_map));
        assertThat(pref.isChecked()).isFalse();

        pref.performClick();
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        assertThat(mDevice.getMessageAccessPermission()).isEqualTo(BluetoothDevice.ACCESS_ALLOWED);
    }

    private A2dpProfile addMockA2dpProfile(boolean preferred, boolean supportsHighQualityAudio,
            boolean highQualityAudioEnabled) {
        A2dpProfile profile = mock(A2dpProfile.class);
        when(mProfileManager.getProfileByName(eq(profile.toString()))).thenReturn(profile);
        when(profile.getNameResource(mDevice))
                .thenReturn(com.android.settingslib.R.string.bluetooth_profile_a2dp);
        when(profile.getHighQualityAudioOptionLabel(mDevice)).thenReturn(
                mContext.getString(com.android.settingslib.R
                        .string.bluetooth_profile_a2dp_high_quality_unknown_codec));
        when(profile.supportsHighQualityAudio(mDevice)).thenReturn(supportsHighQualityAudio);
        when(profile.isHighQualityAudioEnabled(mDevice)).thenReturn(highQualityAudioEnabled);
        when(profile.isEnabled(mDevice)).thenReturn(preferred);
        when(profile.isProfileReady()).thenReturn(true);
        mConnectableProfiles.add(profile);
        return profile;
    }

    private SwitchPreference getHighQualityAudioPref() {
        return (SwitchPreference) mScreen.findPreference(
                BluetoothDetailsProfilesController.HIGH_QUALITY_AUDIO_PREF_TAG);
    }

    @Test
    public void highQualityAudio_prefIsPresentWhenSupported() {
        setupDevice(makeDefaultDeviceConfig());
        addMockA2dpProfile(true, true, true);
        showScreen(mController);
        SwitchPreference pref = getHighQualityAudioPref();
        assertThat(pref.getKey()).isEqualTo(
                BluetoothDetailsProfilesController.HIGH_QUALITY_AUDIO_PREF_TAG);

        // Make sure the preference works when clicked on.
        pref.performClick();
        A2dpProfile profile = (A2dpProfile) mConnectableProfiles.get(0);
        verify(profile).setHighQualityAudioEnabled(mDevice, false);
        pref.performClick();
        verify(profile).setHighQualityAudioEnabled(mDevice, true);
    }

    @Test
    public void highQualityAudio_prefIsAbsentWhenNotSupported() {
        setupDevice(makeDefaultDeviceConfig());
        addMockA2dpProfile(true, false, false);
        showScreen(mController);
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        SwitchPreference pref = (SwitchPreference) mProfiles.getPreference(0);
        assertThat(pref.getKey())
            .isNotEqualTo(BluetoothDetailsProfilesController.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_a2dp));
    }

    @Test
    public void highQualityAudio_busyDeviceDisablesSwitch() {
        setupDevice(makeDefaultDeviceConfig());
        addMockA2dpProfile(true, true, true);
        when(mCachedDevice.isBusy()).thenReturn(true);
        showScreen(mController);
        SwitchPreference pref = getHighQualityAudioPref();
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void highQualityAudio_mediaAudioDisabledAndReEnabled() {
        setupDevice(makeDefaultDeviceConfig());
        A2dpProfile audioProfile = addMockA2dpProfile(true, true, true);
        showScreen(mController);
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(3);

        // Disabling media audio should cause the high quality audio switch to disappear, but not
        // the regular audio one.
        SwitchPreference audioPref =
            (SwitchPreference) mScreen.findPreference(audioProfile.toString());
        audioPref.performClick();
        verify(audioProfile).setEnabled(mDevice, false);
        when(audioProfile.isEnabled(mDevice)).thenReturn(false);
        mController.onDeviceAttributesChanged();
        assertThat(audioPref.isVisible()).isTrue();
        SwitchPreference highQualityAudioPref = getHighQualityAudioPref();
        assertThat(highQualityAudioPref.isVisible()).isFalse();

        // And re-enabling media audio should make high quality switch to reappear.
        audioPref.performClick();
        verify(audioProfile).setEnabled(mDevice, true);
        when(audioProfile.isEnabled(mDevice)).thenReturn(true);
        mController.onDeviceAttributesChanged();
        highQualityAudioPref = getHighQualityAudioPref();
        assertThat(highQualityAudioPref.isVisible()).isTrue();
    }

    @Test
    public void highQualityAudio_mediaAudioStartsDisabled() {
        setupDevice(makeDefaultDeviceConfig());
        A2dpProfile audioProfile = addMockA2dpProfile(false, true, true);
        showScreen(mController);
        SwitchPreference audioPref = mScreen.findPreference(audioProfile.toString());
        SwitchPreference highQualityAudioPref = getHighQualityAudioPref();
        assertThat(audioPref).isNotNull();
        assertThat(audioPref.isChecked()).isFalse();
        assertThat(highQualityAudioPref).isNotNull();
        assertThat(highQualityAudioPref.isVisible()).isFalse();
    }

    @Test
    public void onResume_addServiceListener() {
        mController.onResume();

        verify(mProfileManager).addServiceListener(mController);
    }

    @Test
    public void onPause_removeServiceListener() {
        mController.onPause();

        verify(mProfileManager).removeServiceListener(mController);
    }

    @Test
    public void isDeviceInAllowList_returnTrue() {
        assertThat(mController.isModelNameInAllowList(LE_DEVICE_MODEL)).isTrue();
    }

    @Test
    public void isDeviceInAllowList_returnFalse() {
        assertThat(mController.isModelNameInAllowList(null)).isFalse();
        assertThat(mController.isModelNameInAllowList(NON_LE_DEVICE_MODEL)).isFalse();
    }
}
