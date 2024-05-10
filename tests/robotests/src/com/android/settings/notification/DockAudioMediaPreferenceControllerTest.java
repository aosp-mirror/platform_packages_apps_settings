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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings.Global;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class DockAudioMediaPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private SoundSettings mSetting;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;

    private DockAudioMediaPreferenceController mController;
    private DropDownPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        when(mSetting.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        when(mActivity.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);
        when(mActivity.getResources().getString(anyInt())).thenReturn("test string");
        mPreference = new DropDownPreference(RuntimeEnvironment.application);
        mController = new DockAudioMediaPreferenceController(mContext, mSetting, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
        fakeDockState(Intent.EXTRA_DOCK_STATE_LE_DESK);
    }

    @Test
    public void isAvailable_hasDockSettings_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noDockSettings_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_undocked_shouldReturnFalse() {
        when(mContext.registerReceiver(nullable(BroadcastReceiver.class),
            any(IntentFilter.class))).thenReturn(null);
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_highEndDock_shouldReturnFalse() {
        fakeDockState(Intent.EXTRA_DOCK_STATE_HE_DESK);
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_lowEndDock_shouldReturnTrue() {
        fakeDockState(Intent.EXTRA_DOCK_STATE_LE_DESK);
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_dock_settings))
            .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_dockAudioDisabled_shouldSelectFirstItem() {
        Global.putInt(mContentResolver, Global.DOCK_AUDIO_MEDIA_ENABLED, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getValue()).isEqualTo("0");
    }

    @Test
    public void displayPreference_dockAudioEnabled_shouldSelectSecondItem() {
        Global.putInt(mContentResolver, Global.DOCK_AUDIO_MEDIA_ENABLED, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getValue()).isEqualTo("1");
    }

    @Test
    public void onPreferenceChanged_firstItemSelected_shouldDisableDockAudio() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, "0");

        assertThat(Global.getInt(mContentResolver, Global.DOCK_AUDIO_MEDIA_ENABLED, 0))
            .isEqualTo(0);
    }

    @Test
    public void onPreferenceChanged_secondItemSelected_shouldEnableDockAudio() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, "1");

        assertThat(Global.getInt(mContentResolver, Global.DOCK_AUDIO_MEDIA_ENABLED, 0))
            .isEqualTo(1);
    }

    private void fakeDockState(int dockState) {
        Intent intent = new Intent(Intent.ACTION_DOCK_EVENT);
        intent.putExtra(Intent.EXTRA_DOCK_STATE, dockState);
        when(mContext.registerReceiver(nullable(BroadcastReceiver.class),
            any(IntentFilter.class))).thenReturn(intent);
    }
}
