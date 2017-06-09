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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.R;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothPairingPreferenceControllerTest {
    private static final int ORDER = 1;
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceFragment mFragment;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private SettingsActivity mSettingsActivity;
    private Preference mPreference;

    private BluetoothPairingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        when(mFragment.getPreferenceScreen().getContext()).thenReturn(mContext);

        mPreference = new Preference(mContext);
        mPreference.setKey(BluetoothPairingPreferenceController.KEY_PAIRING);

        mController = new BluetoothPairingPreferenceController(mContext, mFragment,
                mSettingsActivity);
    }

    @Test
    public void testCreateBluetoothPairingPreference() {
        Preference pref = mController.createBluetoothPairingPreference(ORDER);

        assertThat(pref.getKey()).isEqualTo(BluetoothPairingPreferenceController.KEY_PAIRING);
        assertThat(pref.getIcon()).isEqualTo(mContext.getDrawable(R.drawable.ic_add));
        assertThat(pref.getOrder()).isEqualTo(ORDER);
        assertThat(pref.getTitle()).isEqualTo(
                mContext.getString(R.string.bluetooth_pairing_pref_title));
    }

    @Test
    public void testHandlePreferenceTreeClick_startFragment() {
        mController.handlePreferenceTreeClick(mPreference);

        verify(mSettingsActivity).startPreferencePanelAsUser(eq(mFragment), anyString(), any(),
                anyInt(), any(), any());
    }
}
