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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class,
        })
public class AudioSharingCompatibilityPreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_stream_compatibility";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private TwoStatePreference mPreference;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    private AudioSharingCompatibilityPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        LocalBluetoothManager localBluetoothManager = Utils.getLocalBtManager(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(localBluetoothManager.getEventManager()).thenReturn(mBtEventManager);
        when(localBluetoothManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        mController = new AudioSharingCompatibilityPreferenceController(mContext, PREF_KEY);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowThreadUtils.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mBtProfileManager, never()).addServiceListener(mController);
    }

    @Test
    public void onStart_flagOnProfileNotReady_registerProfileCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.onStart(mLifecycleOwner);
        verify(mBroadcast, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mBtProfileManager).addServiceListener(mController);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBroadcast, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStop_flagOn_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mBtProfileManager).removeServiceListener(mController);
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mBroadcast, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mBtProfileManager, never()).removeServiceListener(mController);
    }

    @Test
    public void onServiceConnected_updateSwitch() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(true);

        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        mController.onServiceConnected();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mBtProfileManager).removeServiceListener(mController);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void getAvailabilityStatus_flagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_KEY);
    }

    @Test
    public void getSliceHighlightMenuRes_returnsZero() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(0);
    }

    @Test
    public void displayPreference_broadcastOn_Disabled() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(false);
        String expected =
                mContext.getString(
                        R.string.audio_sharing_stream_compatibility_disabled_description);
        verify(mPreference).setSummary(eq(expected));
    }

    @Test
    public void displayPreference_broadcastOff_Enabled() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(true);
        String expected =
                mContext.getString(R.string.audio_sharing_stream_compatibility_description);
        verify(mPreference).setSummary(eq(expected));
    }

    @Test
    public void isChecked_returnsTrue() {
        when(mBroadcast.getImproveCompatibility()).thenReturn(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_returnsFalse() {
        when(mBroadcast.getImproveCompatibility()).thenReturn(false);
        assertThat(mController.isChecked()).isFalse();
        mBroadcast = null;
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setCheckedToNewValue_returnsTrue() {
        when(mBroadcast.getImproveCompatibility()).thenReturn(true);
        doNothing().when(mBroadcast).setImproveCompatibility(anyBoolean());
        boolean setChecked = mController.setChecked(false);
        verify(mBroadcast).setImproveCompatibility(false);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_SHARING_IMPROVE_COMPATIBILITY, false);
        assertThat(setChecked).isTrue();
    }

    @Test
    public void setCheckedToCurrentValue_returnsFalse() {
        when(mBroadcast.getImproveCompatibility()).thenReturn(true);
        boolean setChecked = mController.setChecked(true);
        verify(mBroadcast, never()).setImproveCompatibility(anyBoolean());
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
        assertThat(setChecked).isFalse();
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_refreshPreference() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(true);
        String expected =
                mContext.getString(R.string.audio_sharing_stream_compatibility_description);
        verify(mPreference).setSummary(eq(expected));

        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.mBroadcastCallback.onBroadcastStarted(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(false);
        expected =
                mContext.getString(
                        R.string.audio_sharing_stream_compatibility_disabled_description);
        verify(mPreference).setSummary(eq(expected));

        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.mBroadcastCallback.onBroadcastStopped(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify one extra setEnabled/setSummary is called other than the first call in
        // displayPreference.
        verify(mPreference, times(2)).setEnabled(true);
        expected = mContext.getString(R.string.audio_sharing_stream_compatibility_description);
        verify(mPreference, times(2)).setSummary(eq(expected));
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_doNothing() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mPreference).setEnabled(true);
        String expected =
                mContext.getString(R.string.audio_sharing_stream_compatibility_description);
        verify(mPreference).setSummary(eq(expected));

        // Verify no extra setEnabled/setSummary is called other than call in displayPreference.
        mController.mBroadcastCallback.onBroadcastMetadataChanged(/* reason= */ 1, mMetadata);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onPlaybackStarted(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onPlaybackStopped(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onBroadcastStopFailed(/* reason= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
        mController.mBroadcastCallback.onBroadcastUpdateFailed(
                /* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference).setSummary(any());
    }
}
