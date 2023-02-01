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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.usb.UsbManager;
import android.net.TetheringManager;
import android.os.Handler;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class UsbDefaultFragmentTest {

    @Mock
    private UsbBackend mUsbBackend;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private Handler mHandler;

    private UsbDefaultFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new TestFragment();
        mFragment.mUsbBackend = mUsbBackend;
        mFragment.mTetheringManager = mTetheringManager;
        mFragment.mHandler = mHandler;
    }

    @Test
    public void getDefaultKey_isNone_shouldReturnNone() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_NONE);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NONE));
    }

    @Test
    public void getDefaultKey_isMtp_shouldReturnMtp() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_MTP);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
    }

    @Test
    public void getDefaultKey_isPtp_shouldReturnPtp() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_PTP);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
    }

    @Test
    public void getDefaultKey_isRndis_shouldReturnRndis() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
    }

    @Test
    public void getDefaultKey_isMidi_shouldReturnMidi() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_MIDI);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MIDI));
    }

    @Test
    public void getDefaultKey_isNcm_returnsRndis() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_NCM);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
    }

    @Test
    public void setDefaultKey_isNone_shouldSetNone() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NONE));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_NONE);
    }

    @Test
    public void setDefaultKey_isMtp_shouldSetMtp() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MTP);
    }

    @Test
    public void setDefaultKey_isPtp_shouldSetPtp() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_PTP);
    }

    @Test
    public void setDefaultKey_isMidi_shouldSetMidi() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MIDI));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MIDI);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void setDefaultKey_isMonkey_shouldDoNothing() {
        ShadowUtils.setIsUserAMonkey(true);
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));

        verify(mUsbBackend, never()).setDefaultUsbFunctions(anyLong());
    }

    @Test
    public void setDefaultKey_functionRndis_startTetheringInvoked() {
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
        assertThat(mFragment.mPreviousFunctions).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void setDefaultKey_functionNcm_invokesStartTethering() {
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NCM));

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
        assertThat(mFragment.mPreviousFunctions).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void setDefaultKey_functionOther_setCurrentFunctionInvoked() {
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_PTP);
        assertThat(mFragment.mPreviousFunctions).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void onTetheringStarted_currentFunctionsIsRndis_setsRndisAsDefaultUsbFunctions() {
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);

        mFragment.mOnStartTetheringCallback.onTetheringStarted();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_RNDIS);
    }

    @Test
    public void onTetheringStarted_currentFunctionsIsNcm_setsNcmAsDefaultUsbFunctions() {
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_NCM);

        mFragment.mOnStartTetheringCallback.onTetheringStarted();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_NCM);
    }


    @Test
    public void onPause_receivedRndis_shouldSetRndis() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_RNDIS);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_RNDIS);
    }

    @Test
    public void onPause_receivedNone_shouldSetNone() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_NONE, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_NONE);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_NONE);
    }

    @Test
    public void onPause_receivedMtp_shouldSetMtp() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_MTP, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_MTP);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MTP);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_MTP);
    }

    @Test
    public void onPause_receivedPtp_shouldSetPtp() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_PTP, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_PTP);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_PTP);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_PTP);
    }

    @Test
    public void onPause_receivedMidi_shouldSetMidi() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_MIDI, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_MIDI);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MIDI);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_MIDI);
    }

    @Test
    public void onPause_receivedNcm_setsNcm() {
        mFragment.mIsStartTethering = true;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(/* connected */ true,
                UsbManager.FUNCTION_NCM, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_NCM);

        mFragment.onPause();

        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_NCM);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_NCM);
    }

    @Test
    public void usbIsPluginAndUsbTetheringIsOn_startTetheringIsInvoked() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);

        mFragment.mUsbConnectionListener.onUsbConnectionChanged(false /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
    }

    @Test
    public void usbIsPluginAndUsbTetheringIsOn_receivedNcm_startsTethering() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_NCM);

        mFragment.mUsbConnectionListener.onUsbConnectionChanged(/* connected */ false,
                UsbManager.FUNCTION_NCM, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(/* connected */ true,
                UsbManager.FUNCTION_NCM, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
    }

    @Test
    public void usbIsNotPluginAndUsbTetheringIsOn_startTetheringIsNotInvoked() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);

        mFragment.mUsbConnectionListener.onUsbConnectionChanged(false /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        verify(mTetheringManager, never()).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
    }

    @Test
    public void usbIsPluginAndUsbTetheringIsAlreadyStarted_startTetheringIsNotInvoked() {
        mFragment.mIsStartTethering = true;
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);

        mFragment.mUsbConnectionListener.onUsbConnectionChanged(false /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_RNDIS, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        verify(mTetheringManager, never()).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mFragment.mOnStartTetheringCallback));
    }

    @Test
    public void onUsbConnectionChanged_usbConfiguredIsTrue_updatesCurrentFunctions() {
        mFragment.mCurrentFunctions = UsbManager.FUNCTION_NONE;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(/* connected= */ true,
                UsbManager.FUNCTION_NCM, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_NCM);
    }

    @Test
    public void onUsbConnectionChanged_usbConfiguredIsFalse_doesNotUpdateCurrentFunctions() {
        mFragment.mCurrentFunctions = UsbManager.FUNCTION_NONE;
        mFragment.mUsbConnectionListener.onUsbConnectionChanged(/* connected= */ true,
                UsbManager.FUNCTION_NCM, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ false);
        assertThat(mFragment.mCurrentFunctions).isEqualTo(UsbManager.FUNCTION_NONE);
    }


    public static class TestFragment extends UsbDefaultFragment {
        public final PreferenceScreen mScreen;

        public TestFragment() {
            mScreen = mock(PreferenceScreen.class);
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }
    }
}
