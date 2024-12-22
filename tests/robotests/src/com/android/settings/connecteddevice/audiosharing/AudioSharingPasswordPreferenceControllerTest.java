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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioSharingPasswordPreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_stream_password";
    private static final String SHARED_PREF_KEY = "default_password";
    private static final String BROADCAST_PASSWORD = "password";
    private static final String EDITTEXT_PASSWORD = "edittext_password";
    private static final String HIDDEN_PASSWORD = "********";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mProfileManager;
    @Mock private SharedPreferences mSharedPreferences;
    @Mock private SharedPreferences.Editor mEditor;
    @Mock private ContentResolver mContentResolver;
    @Mock private PreferenceScreen mScreen;
    private AudioSharingPasswordPreferenceController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private AudioSharingPasswordPreference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new AudioSharingPasswordPreferenceController(mContext, PREF_KEY);
        mPreference = spy(new AudioSharingPasswordPreference(mContext));
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_flagOn_available() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff_unsupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mContentResolver, never()).registerContentObserver(any(), anyBoolean(), any());
        verify(mSharedPreferences, never()).registerOnSharedPreferenceChangeListener(any());
    }

    @Test
    public void onStart_flagOn_registerCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mContentResolver).registerContentObserver(any(), anyBoolean(), any());
        verify(mSharedPreferences).registerOnSharedPreferenceChangeListener(any());
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mContentResolver, never()).unregisterContentObserver(any());
        verify(mSharedPreferences, never()).unregisterOnSharedPreferenceChangeListener(any());
    }

    @Test
    public void onStop_flagOn_registerCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mContentResolver).unregisterContentObserver(any());
        verify(mSharedPreferences).unregisterOnSharedPreferenceChangeListener(any());
    }

    @Test
    public void displayPreference_setupPreference_noPassword() {
        when(mSharedPreferences.getString(anyString(), anyString())).thenReturn(EDITTEXT_PASSWORD);
        when(mBroadcast.getBroadcastCode()).thenReturn(new byte[] {});

        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.isPassword()).isTrue();
        assertThat(mPreference.getDialogLayoutResource())
                .isEqualTo(R.layout.audio_sharing_password_dialog);
        assertThat(mPreference.getText()).isEqualTo(EDITTEXT_PASSWORD);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.audio_streams_no_password_summary));
        verify(mPreference).setValidator(any());
        verify(mPreference).setOnDialogEventListener(any());
    }

    @Test
    public void contentObserver_updatePreferenceOnChange() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.getBroadcastCode())
                .thenReturn(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        mController.onStart(mLifecycleOwner);
        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<ContentObserver> observerCaptor =
                ArgumentCaptor.forClass(ContentObserver.class);
        verify(mContentResolver)
                .registerContentObserver(any(), anyBoolean(), observerCaptor.capture());

        var observer = observerCaptor.getValue();
        assertThat(observer).isNotNull();
        observer.onChange(true);
        verify(mPreference).setText(anyString());
        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void sharedPrefChangeListener_updatePreferenceOnChange() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.getBroadcastCode())
                .thenReturn(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        mController.onStart(mLifecycleOwner);
        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        ArgumentCaptor<SharedPreferences.OnSharedPreferenceChangeListener> captor =
                ArgumentCaptor.forClass(SharedPreferences.OnSharedPreferenceChangeListener.class);
        verify(mSharedPreferences).registerOnSharedPreferenceChangeListener(captor.capture());

        var observer = captor.getValue();
        assertThat(captor).isNotNull();
        observer.onSharedPreferenceChanged(mSharedPreferences, SHARED_PREF_KEY);
        verify(mPreference).setText(anyString());
        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void displayPreference_setupPreference_hasPassword() {
        when(mBroadcast.getBroadcastCode())
                .thenReturn(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.isPassword()).isTrue();
        assertThat(mPreference.getDialogLayoutResource())
                .isEqualTo(R.layout.audio_sharing_password_dialog);
        assertThat(mPreference.getText()).isEqualTo(BROADCAST_PASSWORD);
        assertThat(mPreference.getSummary()).isEqualTo(HIDDEN_PASSWORD);
        verify(mPreference).setValidator(any());
        verify(mPreference).setOnDialogEventListener(any());
    }

    @Test
    public void onBindDialogView_updatePreference_isBroadcasting_noPassword() {
        when(mBroadcast.getBroadcastCode()).thenReturn(new byte[] {});
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        mController.onBindDialogView();
        ShadowLooper.idleMainLooper();

        verify(mPreference).setEditable(false);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void onBindDialogView_updatePreference_isNotBroadcasting_hasPassword() {
        when(mBroadcast.getBroadcastCode())
                .thenReturn(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        mController.displayPreference(mScreen);
        mController.onBindDialogView();
        ShadowLooper.idleMainLooper();

        verify(mPreference).setEditable(true);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceDataChanged_isBroadcasting_doNothing() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        mController.onPreferenceDataChanged(BROADCAST_PASSWORD, /* isPublicBroadcast= */ false);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast, never()).setBroadcastCode(any());
        verify(mFeatureFactory.metricsFeatureProvider, never()).action(any(), anyInt(), anyInt());
    }

    @Test
    public void onPreferenceDataChanged_noChange_doNothing() {
        when(mSharedPreferences.getString(anyString(), anyString())).thenReturn(EDITTEXT_PASSWORD);
        when(mBroadcast.getBroadcastCode()).thenReturn(new byte[] {});
        mController.displayPreference(mScreen);
        mController.onPreferenceDataChanged(EDITTEXT_PASSWORD, /* isPublicBroadcast= */ true);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast, never()).setBroadcastCode(any());
        verify(mFeatureFactory.metricsFeatureProvider, never()).action(any(), anyInt(), anyInt());
    }

    @Test
    public void onPreferenceDataChanged_updateToNonPublicBroadcast() {
        when(mSharedPreferences.getString(anyString(), anyString())).thenReturn(EDITTEXT_PASSWORD);
        when(mBroadcast.getBroadcastCode()).thenReturn(new byte[] {});
        mController.displayPreference(mScreen);
        mController.onPreferenceDataChanged(BROADCAST_PASSWORD, /* isPublicBroadcast= */ false);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast).setBroadcastCode(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        verify(mEditor).putString(anyString(), eq(BROADCAST_PASSWORD));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_STREAM_PASSWORD_UPDATED, 0);
    }

    @Test
    public void onPreferenceDataChanged_updateToPublicBroadcast() {
        when(mSharedPreferences.getString(anyString(), anyString())).thenReturn(EDITTEXT_PASSWORD);
        when(mBroadcast.getBroadcastCode())
                .thenReturn(BROADCAST_PASSWORD.getBytes(StandardCharsets.UTF_8));
        mController.displayPreference(mScreen);
        mController.onPreferenceDataChanged(EDITTEXT_PASSWORD, /* isPublicBroadcast= */ true);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast).setBroadcastCode("".getBytes(StandardCharsets.UTF_8));
        verify(mEditor, never()).putString(anyString(), eq(EDITTEXT_PASSWORD));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_STREAM_PASSWORD_UPDATED, 1);
    }

    @Test
    public void idTextValid_emptyString() {
        boolean valid = mController.isTextValid("");

        assertThat(valid).isFalse();
    }

    @Test
    public void idTextValid_validPassword() {
        boolean valid = mController.isTextValid(BROADCAST_PASSWORD);

        assertThat(valid).isTrue();
    }
}
