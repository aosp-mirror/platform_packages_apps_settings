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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.net.TetheringManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class UsbDetailsFunctionsControllerTest {

    private UsbDetailsFunctionsController mDetailsFunctionsController;
    private Context mContext;
    private Lifecycle mLifecycle;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private SelectorWithWidgetPreference mRadioButtonPreference;

    @Mock
    private UsbBackend mUsbBackend;
    @Mock
    private UsbDetailsFragment mFragment;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private TetheringManager mTetheringManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mContext.getSystemService(TetheringManager.class)).thenReturn(mTetheringManager);

        mDetailsFunctionsController = new UsbDetailsFunctionsController(mContext, mFragment,
                mUsbBackend);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(mDetailsFunctionsController.getPreferenceKey());
        mScreen.addPreference(mPreferenceCategory);
        mDetailsFunctionsController.displayPreference(mScreen);

        mRadioButtonPreference = new SelectorWithWidgetPreference(mContext);
    }

    @Test
    public void displayRefresh_allAllowed_shouldCreatePrefs() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.displayPreference(mScreen);
        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_NONE, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();
        Iterator<Long> iter = UsbDetailsFunctionsController.FUNCTIONS_MAP.keySet().iterator();

        for (SelectorWithWidgetPreference pref : prefs) {
            assertThat(pref.getKey()).isEqualTo(UsbBackend.usbFunctionsToString(iter.next()));
        }
    }

    @Test
    public void displayRefresh_disconnected_shouldDisable() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(false, UsbManager.FUNCTION_NONE,
                POWER_ROLE_SINK, DATA_ROLE_DEVICE);
        assertThat(mPreferenceCategory.isEnabled()).isFalse();
    }

    @Test
    public void displayRefresh_onlyMidiAllowed_shouldCreateOnlyMidiPref() {
        when(mUsbBackend.areFunctionsSupported(UsbManager.FUNCTION_MIDI)).thenReturn(true);
        when(mUsbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).thenReturn(false);
        when(mUsbBackend.areFunctionsSupported(UsbManager.FUNCTION_PTP)).thenReturn(false);
        when(mUsbBackend.areFunctionsSupported(UsbManager.FUNCTION_RNDIS)).thenReturn(false);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_NONE, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();
        assertThat(prefs.size()).isEqualTo(1);
        assertThat(prefs.get(0).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MIDI));
    }

    @Test
    public void displayRefresh_mtpEnabled_shouldCheckSwitches() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_MTP, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();

        assertThat(prefs.get(0).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        assertThat(prefs.get(0).isChecked()).isTrue();
    }

    @Test
    public void displayRefresh_accessoryEnabled_shouldCheckSwitches() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_ACCESSORY, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();

        assertThat(prefs.get(0).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        assertThat(prefs.get(0).isChecked()).isTrue();
    }

    @Test
    public void displayRefresh_ncmEnabled_checksSwitches() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_NCM, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();

        assertThat(prefs.get(1).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
        assertThat(prefs.get(1).isChecked()).isTrue();
    }

    @Test
    public void onClickMtp_noneEnabled_shouldEnableMtp() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_NONE, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_NONE);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();
        prefs.get(0).performClick();

        assertThat(prefs.get(0).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        verify(mUsbBackend).setCurrentFunctions(UsbManager.FUNCTION_MTP);
        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_MTP, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        assertThat(prefs.get(0).isChecked()).isTrue();
    }

    @Test
    public void onClickMtp_ptpEnabled_shouldEnableMtp() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_PTP, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_PTP);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();
        prefs.get(0).performClick();

        assertThat(prefs.get(0).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        verify(mUsbBackend).setCurrentFunctions(UsbManager.FUNCTION_MTP);
        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_MTP, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        assertThat(prefs.get(0).isChecked()).isTrue();
        assertThat(prefs.get(3).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
        assertThat(prefs.get(3).isChecked()).isFalse();
    }

    @Ignore("b/313362757")
    @Test
    public void onClickNone_mtpEnabled_shouldDisableMtp() {
        when(mUsbBackend.areFunctionsSupported(anyLong())).thenReturn(true);

        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_MTP, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        when(mUsbBackend.getCurrentFunctions()).thenReturn(UsbManager.FUNCTION_MTP);
        List<SelectorWithWidgetPreference> prefs = getRadioPreferences();
        prefs.get(4).performClick();

        assertThat(prefs.get(4).getKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NONE));
        verify(mUsbBackend).setCurrentFunctions(UsbManager.FUNCTION_NONE);
        mDetailsFunctionsController.refresh(true, UsbManager.FUNCTION_NONE, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE);
        assertThat(prefs.get(0).isChecked()).isFalse();
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void isAvailable_isMonkey_shouldReturnFalse() {
        ShadowUtils.setIsUserAMonkey(true);
        assertThat(mDetailsFunctionsController.isAvailable()).isFalse();
    }

    private List<SelectorWithWidgetPreference> getRadioPreferences() {
        ArrayList<SelectorWithWidgetPreference> result = new ArrayList<>();
        for (int i = 0; i < mPreferenceCategory.getPreferenceCount(); i++) {
            result.add((SelectorWithWidgetPreference) mPreferenceCategory.getPreference(i));
        }
        return result;
    }

    @Test
    public void onRadioButtonClicked_functionRndis_startTetheringInvoked() {
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mDetailsFunctionsController.mOnStartTetheringCallback));
        assertThat(mDetailsFunctionsController.mPreviousFunction).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void onRadioButtonClicked_functionNcm_startsTethering() {
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NCM));
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        verify(mTetheringManager).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mDetailsFunctionsController.mOnStartTetheringCallback));
        assertThat(mDetailsFunctionsController.mPreviousFunction).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void onRadioButtonClicked_functionOther_setCurrentFunctionInvoked() {
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
        doReturn(UsbManager.FUNCTION_MTP).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        verify(mUsbBackend).setCurrentFunctions(UsbManager.FUNCTION_PTP);
        assertThat(mDetailsFunctionsController.mPreviousFunction).isEqualTo(
                UsbManager.FUNCTION_MTP);
    }

    @Test
    public void onRadioButtonClicked_functionMtp_inAccessoryMode_doNothing() {
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        doReturn(UsbManager.FUNCTION_ACCESSORY).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.mPreviousFunction = UsbManager.FUNCTION_ACCESSORY;
        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        assertThat(mDetailsFunctionsController.mPreviousFunction).isEqualTo(
                UsbManager.FUNCTION_ACCESSORY);
    }

    @Test
    public void onRadioButtonClicked_functionMtp_inAccessoryCombinationsMode_doNothing() {
        final long function = UsbManager.FUNCTION_ACCESSORY | UsbManager.FUNCTION_AUDIO_SOURCE;
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        doReturn(UsbManager.FUNCTION_ACCESSORY).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.mPreviousFunction = function;
        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        assertThat(mDetailsFunctionsController.mPreviousFunction).isEqualTo(function);
    }

    @Test
    public void onRadioButtonClicked_clickSameButton_doNothing() {
        mRadioButtonPreference.setKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
        doReturn(UsbManager.FUNCTION_PTP).when(mUsbBackend).getCurrentFunctions();

        mDetailsFunctionsController.onRadioButtonClicked(mRadioButtonPreference);

        verify(mUsbBackend, never()).setCurrentFunctions(UsbManager.FUNCTION_PTP);
        verify(mTetheringManager, never()).startTethering(eq(TetheringManager.TETHERING_USB),
                any(),
                eq(mDetailsFunctionsController.mOnStartTetheringCallback));
    }

    @Test
    public void onTetheringFailed_resetPreviousFunctions() {
        mDetailsFunctionsController.mPreviousFunction = UsbManager.FUNCTION_PTP;

        mDetailsFunctionsController.mOnStartTetheringCallback.onTetheringFailed(0);

        verify(mUsbBackend).setCurrentFunctions(UsbManager.FUNCTION_PTP);
    }
}
