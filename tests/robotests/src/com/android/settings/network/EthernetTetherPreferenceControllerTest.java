/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.EthernetManager;
import android.net.TetheringManager;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class EthernetTetherPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private EthernetManager mEthernetManager;
    @Mock
    private TetherEnabler mTetherEnabler;

    private Context mContext;
    private EthernetTetherPreferenceController mController;
    private SwitchPreference mPreference;
    private static final String ETHERNET_REGEX = "ethernet";

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreference = spy(SwitchPreference.class);
        when(mContext.getSystemService(Context.TETHERING_SERVICE)).thenReturn(mTetheringManager);
        when(mTetheringManager.getTetherableIfaces()).thenReturn(new String[]{ETHERNET_REGEX});
        when(mContext.getSystemService(EthernetManager.class)).thenReturn(mEthernetManager);
        mController = new EthernetTetherPreferenceController(mContext, "ethernet");
        mController.setTetherEnabler(mTetherEnabler);
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
    }

    @Test
    @Ignore
    public void lifecycle_shouldRegisterReceiverOnStart() {
        mController.onStart();

        verify(mEthernetManager).addInterfaceStateListener(any(),
                eq(mController.mEthernetListener));
    }

    @Test
    public void lifecycle_shouldAddListenerOnResume() {
        mController.onResume();
        verify(mTetherEnabler).addListener(mController);
    }

    @Test
    public void lifecycle_shouldRemoveListenerOnPause() {
        mController.onPause();
        verify(mTetherEnabler).removeListener(mController);
    }

    @Test
    public void lifecycle_shouldUnregisterReceiverOnStop() {
        mController.onStart();
        EthernetManager.InterfaceStateListener listener = mController.mEthernetListener;
        mController.onStop();

        verify(mEthernetManager).removeInterfaceStateListener(eq(listener));
    }

    @Test
    public void shouldEnable_noTetherable() {
        when(mTetheringManager.getTetherableIfaces()).thenReturn(new String[0]);
        assertThat(mController.shouldEnable()).isFalse();
    }

    @Test
    public void shouldShow_noEthernetInterface() {
        when(mContext.getSystemService(EthernetManager.class)).thenReturn(null);

        final EthernetTetherPreferenceController controller =
                new EthernetTetherPreferenceController(mContext, "ethernet");
        assertThat(controller.shouldShow()).isFalse();
    }

    @Test
    public void setChecked_shouldStartEthernetTethering() {
        mController.setChecked(true);
        verify(mTetherEnabler).startTethering(TetheringManager.TETHERING_ETHERNET);
    }

    @Test
    public void setUnchecked_shouldStopEthernetTethering() {
        mController.setChecked(false);
        verify(mTetherEnabler).stopTethering(TetheringManager.TETHERING_ETHERNET);
    }

    @Test
    public void switch_shouldCheckedWhenEthernetTethering() {
        mController.onTetherStateUpdated(TetherEnabler.TETHERING_ETHERNET_ON);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void switch_shouldUnCheckedWhenEthernetNotTethering() {
        mController.onTetherStateUpdated(TetherEnabler.TETHERING_OFF);
        assertThat(mController.isChecked()).isFalse();
    }
}
