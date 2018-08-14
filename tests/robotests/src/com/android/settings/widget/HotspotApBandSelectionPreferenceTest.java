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

package com.android.settings.widget;

import static com.android.settingslib.CustomDialogPreference.CustomPreferenceDialogFragment;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class HotspotApBandSelectionPreferenceTest {
    private HotspotApBandSelectionPreference mPreference;
    private Context mContext;
    private Button mSaveButton;
    private View mLayout;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSaveButton = spy(new Button(mContext));

        final CustomPreferenceDialogFragment fragment = mock(CustomPreferenceDialogFragment.class);
        final AlertDialog dialog = mock(AlertDialog.class);
        when(fragment.getDialog()).thenReturn(dialog);
        when(dialog.getButton(anyInt())).thenReturn(mSaveButton);

        mPreference = new HotspotApBandSelectionPreference(mContext);
        ReflectionHelpers.setField(mPreference, "mFragment", fragment);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mLayout = inflater.inflate(R.layout.hotspot_ap_band_selection_dialog,
                new LinearLayout(mContext), false);
    }

    @Test
    public void getWifiBand_updatesBandPresetConfigProvided() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_ANY);
        mPreference.onBindDialogView(mLayout);

        // check that the boxes are set correctly when a pre-existing config is set
        assertThat(mPreference.getWifiBand()).isEqualTo(WifiConfiguration.AP_BAND_ANY);
    }

    @Test
    public void getWifiBand_updatesBandWhenBoxesToggled() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_ANY);
        mPreference.onBindDialogView(mLayout);

        assertThat(mPreference.getWifiBand()).isEqualTo(WifiConfiguration.AP_BAND_ANY);

        // make sure we have the expected box then toggle it
        mPreference.mBox2G.setChecked(false);

        // check that band is updated
        assertThat(mPreference.getWifiBand()).isEqualTo(WifiConfiguration.AP_BAND_5GHZ);
    }

    @Test
    public void onSaveInstanceState_skipWhenDialogGone() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_2GHZ);
        mPreference.onBindDialogView(mLayout);
        // remove the fragment to make the dialog unavailable
        ReflectionHelpers.setField(mPreference, "mFragment", null);

        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_ANY);
        mPreference.onBindDialogView(mLayout);

        // state should only be saved when the dialog is available
        Parcelable parcelable = mPreference.onSaveInstanceState();
        mPreference.onRestoreInstanceState(parcelable);
        assertThat(mPreference.mShouldRestore).isFalse();
    }

    @Test
    public void onSaveInstanceState_doesNotCrashWhenViewGone() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_2GHZ);
        mPreference.onBindDialogView(mLayout);
        // When the device dozes the view and dialog can become null
        mPreference.mBox5G = null;
        mPreference.mBox2G = null;
        ReflectionHelpers.setField(mPreference, "mFragment", null);

        // make sure it does not crash and state is not restored
        Parcelable parcelable = mPreference.onSaveInstanceState();
        mPreference.onRestoreInstanceState(parcelable);
        assertThat(mPreference.mShouldRestore).isFalse();
    }

    @Test
    public void onSaveInstanceState_presentWhenDialogPresent() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_2GHZ);
        mPreference.onBindDialogView(mLayout);

        Parcelable parcelable = mPreference.onSaveInstanceState();
        mPreference.onRestoreInstanceState(parcelable);
        assertThat(mPreference.mShouldRestore).isTrue();
    }

    @Test
    public void positiveButton_updatedCorrectly() {
        mPreference.setExistingConfigValue(WifiConfiguration.AP_BAND_ANY);
        mPreference.onBindDialogView(mLayout);

        // button is enabled whole time so far since we have a pre-existing selection
        verify(mSaveButton, never()).setEnabled(false);

        // clear all boxes and make sure it stays enabled until empty
        mPreference.mBox2G.setChecked(false);
        mPreference.mBox5G.setChecked(false);

        // button should be disabled now
        verify(mSaveButton, times(1)).setEnabled(false);
    }
}
