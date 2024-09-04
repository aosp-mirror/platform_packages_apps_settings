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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.sysprop.BluetoothProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothDevice;
import com.android.settingslib.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.junit.Rule;
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
@Config(shadows = ShadowBluetoothDevice.class)
public class BluetoothDetailsProfilesControllerTest extends BluetoothDetailsControllerTestBase {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String LE_DEVICE_MODEL = "le_audio_headset";
    private static final String NON_LE_DEVICE_MODEL = "non_le_audio_headset";
    private BluetoothDetailsProfilesController mController;
    private List<LocalBluetoothProfile> mConnectableProfiles;
    private PreferenceCategory mProfiles;
    private BluetoothFeatureProvider mFeatureProvider;

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedBluetoothDeviceManager;

    @Mock
    private A2dpProfile mA2dpProfile;
    @Mock
    private LeAudioProfile mLeAudioProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;

    @Override
    public void setUp() {
        super.setUp();

        FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureProvider = fakeFeatureFactory.getBluetoothFeatureProvider();

        mProfiles = spy(new PreferenceCategory(mContext));
        when(mProfiles.getPreferenceManager()).thenReturn(mPreferenceManager);

        mConnectableProfiles = new ArrayList<>();
        when(mLocalManager.getProfileManager()).thenReturn(mProfileManager);
        when(mLocalManager.getCachedDeviceManager()).thenReturn(mCachedBluetoothDeviceManager);
        setUpMockProfiles();
        when(mCachedBluetoothDeviceManager.getCachedDevicesCopy())
                .thenReturn(ImmutableList.of(mCachedDevice));
        when(mCachedDevice.getUiAccessibleProfiles())
                .thenAnswer(invocation -> new ArrayList<>(mConnectableProfiles));
        when(mCachedDevice.getProfiles())
                .thenAnswer(invocation -> ImmutableList.of(mConnectableProfiles));

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

    /**
     * Returns the list of SwitchPreferenceCompat objects added to the screen - there should be one
     * per Bluetooth profile.
     */
    private List<SwitchPreferenceCompat> getProfileSwitches(boolean expectOnlyMConnectable) {
        if (expectOnlyMConnectable) {
            assertThat(mConnectableProfiles).isNotEmpty();
            assertThat(mProfiles.getPreferenceCount() - 1).isEqualTo(mConnectableProfiles.size());
        }
        List<SwitchPreferenceCompat> result = new ArrayList<>();
        for (int i = 0; i < mProfiles.getPreferenceCount(); i++) {
            final Preference preference = mProfiles.getPreference(i);
            if (preference instanceof SwitchPreferenceCompat) {
                result.add((SwitchPreferenceCompat) preference);
            }
        }
        return result;
    }

    private void verifyProfileSwitchTitles(List<SwitchPreferenceCompat> switches) {
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
        List<SwitchPreferenceCompat> switches = getProfileSwitches(true);
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
        List<SwitchPreferenceCompat> switches = getProfileSwitches(true);
        SwitchPreferenceCompat pref = switches.get(0);

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
        when(psp.getSummaryResourceForDevice(mDevice))
                .thenReturn(R.string.bluetooth_profile_pbap_summary);
        when(psp.toString()).thenReturn(PbapServerProfile.NAME);
        when(psp.isProfileReady()).thenReturn(true);
        when(mProfileManager.getPbapProfile()).thenReturn(psp);

        showScreen(mController);
        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreferenceCompat pref = switches.get(0);
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
        when(psp.getSummaryResourceForDevice(mDevice))
                .thenReturn(R.string.bluetooth_profile_pbap_summary);
        when(psp.toString()).thenReturn(PbapServerProfile.NAME);
        when(psp.isProfileReady()).thenReturn(true);
        when(mProfileManager.getPbapProfile()).thenReturn(psp);

        showScreen(mController);
        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreferenceCompat pref = switches.get(0);
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
        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.size()).isEqualTo(1);
        SwitchPreferenceCompat pref = switches.get(0);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_map));
        assertThat(pref.isChecked()).isFalse();

        pref.performClick();
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        assertThat(mDevice.getMessageAccessPermission()).isEqualTo(BluetoothDevice.ACCESS_ALLOWED);
    }

    private void setUpMockProfiles() {
        when(mA2dpProfile.toString()).thenReturn("A2DP");
        when(mProfileManager.getProfileByName(eq(mA2dpProfile.toString())))
                .thenReturn(mA2dpProfile);
        when(mA2dpProfile.getNameResource(any()))
                .thenReturn(R.string.bluetooth_profile_a2dp);
        when(mA2dpProfile.getHighQualityAudioOptionLabel(any())).thenReturn(
                mContext.getString(R.string.bluetooth_profile_a2dp_high_quality_unknown_codec));
        when(mA2dpProfile.isProfileReady()).thenReturn(true);
        when(mProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);

        when(mLeAudioProfile.toString()).thenReturn("LE_AUDIO");
        when(mLeAudioProfile.getNameResource(any()))
                .thenReturn(R.string.bluetooth_profile_le_audio);
        when(mLeAudioProfile.isProfileReady()).thenReturn(true);
        when(mProfileManager.getLeAudioProfile()).thenReturn(mLeAudioProfile);

        when(mHearingAidProfile.toString()).thenReturn("HearingAid");
        when(mHearingAidProfile.getNameResource(any()))
                .thenReturn(R.string.bluetooth_profile_hearing_aid);
        when(mHearingAidProfile.isProfileReady()).thenReturn(true);
        when(mProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
    }

    private void addA2dpProfileToDevice(boolean preferred, boolean supportsHighQualityAudio,
            boolean highQualityAudioEnabled) {
        when(mA2dpProfile.supportsHighQualityAudio(any())).thenReturn(supportsHighQualityAudio);
        when(mA2dpProfile.isHighQualityAudioEnabled(any())).thenReturn(highQualityAudioEnabled);
        when(mA2dpProfile.isEnabled(any())).thenReturn(preferred);
        mConnectableProfiles.add(mA2dpProfile);
    }

    private void addLeAudioProfileToDevice(boolean enabled) {
        when(mLeAudioProfile.isEnabled(any())).thenReturn(enabled);
        mConnectableProfiles.add(mLeAudioProfile);
    }

    private void addHearingAidProfileToDevice(boolean enabled) {
        when(mHearingAidProfile.isEnabled(any())).thenReturn(enabled);
        mConnectableProfiles.add(mHearingAidProfile);
    }

    private SwitchPreferenceCompat getHighQualityAudioPref() {
        return (SwitchPreferenceCompat) mScreen.findPreference(
                BluetoothDetailsProfilesController.HIGH_QUALITY_AUDIO_PREF_TAG);
    }

    @Test
    public void highQualityAudio_prefIsPresentWhenSupported() {
        setupDevice(makeDefaultDeviceConfig());
        addA2dpProfileToDevice(true, true, true);
        showScreen(mController);
        SwitchPreferenceCompat pref = getHighQualityAudioPref();
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
        addA2dpProfileToDevice(true, false, false);
        showScreen(mController);
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(2);
        SwitchPreferenceCompat pref = (SwitchPreferenceCompat) mProfiles.getPreference(0);
        assertThat(pref.getKey())
            .isNotEqualTo(BluetoothDetailsProfilesController.HIGH_QUALITY_AUDIO_PREF_TAG);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(com.android.settingslib.R.string.bluetooth_profile_a2dp));
    }

    @Test
    public void highQualityAudio_busyDeviceDisablesSwitch() {
        setupDevice(makeDefaultDeviceConfig());
        addA2dpProfileToDevice(true, true, true);
        when(mCachedDevice.isBusy()).thenReturn(true);
        showScreen(mController);
        SwitchPreferenceCompat pref = getHighQualityAudioPref();
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void highQualityAudio_mediaAudioDisabledAndReEnabled() {
        setupDevice(makeDefaultDeviceConfig());
        addA2dpProfileToDevice(true, true, true);
        showScreen(mController);
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(3);

        // Disabling media audio should cause the high quality audio switch to disappear, but not
        // the regular audio one.
        SwitchPreferenceCompat audioPref =
                (SwitchPreferenceCompat) mScreen.findPreference(mA2dpProfile.toString());
        audioPref.performClick();
        verify(mA2dpProfile).setEnabled(mDevice, false);
        when(mA2dpProfile.isEnabled(mDevice)).thenReturn(false);
        mController.onDeviceAttributesChanged();
        assertThat(audioPref.isVisible()).isTrue();
        SwitchPreferenceCompat highQualityAudioPref = getHighQualityAudioPref();
        assertThat(highQualityAudioPref.isVisible()).isFalse();

        // And re-enabling media audio should make high quality switch to reappear.
        audioPref.performClick();
        verify(mA2dpProfile).setEnabled(mDevice, true);
        when(mA2dpProfile.isEnabled(mDevice)).thenReturn(true);
        mController.onDeviceAttributesChanged();
        highQualityAudioPref = getHighQualityAudioPref();
        assertThat(highQualityAudioPref.isVisible()).isTrue();
    }

    @Test
    public void highQualityAudio_mediaAudioStartsDisabled() {
        setupDevice(makeDefaultDeviceConfig());
        addA2dpProfileToDevice(false, true, true);
        showScreen(mController);
        SwitchPreferenceCompat audioPref = mScreen.findPreference(mA2dpProfile.toString());
        SwitchPreferenceCompat highQualityAudioPref = getHighQualityAudioPref();
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

    @Test
    public void prefKeyInBlockingList_hideToggle() {
        setupDevice(makeDefaultDeviceConfig());

        addA2dpProfileToDevice(true, true, true);
        when(mFeatureProvider.getInvisibleProfilePreferenceKeys(any(), any()))
                .thenReturn(ImmutableSet.of("A2DP"));

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.get(0).isVisible()).isFalse();
    }

    @Test
    public void prefKeyNotInBlockingList_showToggle() {
        setupDevice(makeDefaultDeviceConfig());

        addA2dpProfileToDevice(true, true, true);
        when(mFeatureProvider.getInvisibleProfilePreferenceKeys(any(), any()))
                .thenReturn(ImmutableSet.of("LE_AUDIO"));

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.get(0).isVisible()).isTrue();
    }

    @Test
    public void classicAudioDeviceWithLeAudio_showLeAudioToggle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_HIDE_LE_AUDIO_TOGGLE_FOR_LE_AUDIO_ONLY_DEVICE);
        setupDevice(makeDefaultDeviceConfig());
        addLeAudioProfileToDevice(false);
        addA2dpProfileToDevice(false, false, false);

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.get(0).isVisible()).isTrue();
    }

    @Test
    public void leAudioOnlyDevice_hideLeAudioToggle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_HIDE_LE_AUDIO_TOGGLE_FOR_LE_AUDIO_ONLY_DEVICE);
        setupDevice(makeDefaultDeviceConfig());
        addLeAudioProfileToDevice(false);

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.get(0).isVisible()).isFalse();
    }

    @Test
    public void ashaHearingAid_hideAshaToggle() {
        setupDevice(makeDefaultDeviceConfig());
        addHearingAidProfileToDevice(true);

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.isEmpty()).isTrue();
    }
    @Test
    public void ashaHearingAidWithLeAudio_showLeAudioToggle() {
        setupDevice(makeDefaultDeviceConfig());
        addHearingAidProfileToDevice(false);
        addLeAudioProfileToDevice(true);

        showScreen(mController);

        List<SwitchPreferenceCompat> switches = getProfileSwitches(false);
        assertThat(switches.getFirst().getTitle()).isEqualTo(
                mContext.getString(mLeAudioProfile.getNameResource(mDevice)));
    }
}
