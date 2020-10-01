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

package com.android.settings.wifi.calling;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.ImsMmTelManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.RowContent;
import androidx.slice.widget.SliceContent;
import androidx.slice.widget.SliceLiveData;

import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.network.ims.MockWifiCallingQueryImsState;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceData;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiCallingSliceHelperTest {
    private static final int SUB_ID = 1;

    private Context mContext;
    @Mock
    private CarrierConfigManager mMockCarrierConfigManager;

    @Mock
    private ImsManager mMockImsManager;

    @Mock
    private ImsMmTelManager mMockImsMmTelManager;

    private MockWifiCallingQueryImsState mQueryImsState;

    private FakeWifiCallingSliceHelper mWfcSliceHelper;
    private SettingsSliceProvider mProvider;
    private SliceBroadcastReceiver mReceiver;
    private FakeFeatureFactory mFeatureFactory;
    private SlicesFeatureProvider mSlicesFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        //setup for SettingsSliceProvider tests
        mProvider = spy(new SettingsSliceProvider());
        doReturn(mContext).when(mProvider).getContext();
        mProvider.onCreateSliceProvider();

        //setup for SliceBroadcastReceiver test
        mReceiver = spy(new SliceBroadcastReceiver());

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSlicesFeatureProvider = mFeatureFactory.getSlicesFeatureProvider();

        mQueryImsState = new MockWifiCallingQueryImsState(mContext, SUB_ID);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);

        mWfcSliceHelper = spy(new FakeWifiCallingSliceHelper(mContext));
        doReturn(mQueryImsState).when(mWfcSliceHelper).queryImsState(anyInt());

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void test_CreateWifiCallingSlice_invalidSubId() {
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsProvisionedOnDevice(false);
        mWfcSliceHelper.setDefaultVoiceSubId(-1);

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(
                CustomSliceRegistry.WIFI_CALLING_URI);

        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateWifiCallingSlice_wfcNotSupported() {
        mQueryImsState.setIsProvisionedOnDevice(false);

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(
                CustomSliceRegistry.WIFI_CALLING_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateWifiCallingSlice_needsActivation() {
        /* In cases where activation is needed and the user action
        would be turning on the wifi calling (i.e. if wifi calling is
        turned off) we need to guide the user to wifi calling settings
        activity so the user can perform the activation there.(PrimaryAction)
         */
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(false);
        mQueryImsState.setIsTtyOnVolteEnabled(false);
        when(mMockCarrierConfigManager.getConfigForSubId(1)).thenReturn(null);
        mWfcSliceHelper.setActivationAppIntent(new Intent()); // dummy Intent

        final Slice slice  = mWfcSliceHelper.createWifiCallingSlice(
                CustomSliceRegistry.WIFI_CALLING_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsUnavailableSlice(slice, null,
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY),
                mContext.getString(R.string.wifi_calling_settings_title));
    }

    @Test
    public void test_CreateWifiCallingSlice_success() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mMockCarrierConfigManager.getConfigForSubId(1)).thenReturn(null);

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(
                CustomSliceRegistry.WIFI_CALLING_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsToggleSlice(slice, null);
    }

    @Test
    public void test_SettingSliceProvider_getsRightSliceWifiCalling() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mMockCarrierConfigManager.getConfigForSubId(1)).thenReturn(null);
        when(mSlicesFeatureProvider.getNewWifiCallingSliceHelper(mContext))
                .thenReturn(mWfcSliceHelper);

        final Slice slice = mProvider.onBindSlice(CustomSliceRegistry.WIFI_CALLING_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsToggleSlice(slice, null);
    }

    @Test
    public void test_SliceBroadcastReceiver_toggleOnWifiCalling() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(false);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mSlicesFeatureProvider.getNewWifiCallingSliceHelper(mContext))
                .thenReturn(mWfcSliceHelper);
        mWfcSliceHelper.setActivationAppIntent(null);

        final ArgumentCaptor<Boolean> mWfcSettingCaptor = ArgumentCaptor.forClass(Boolean.class);

        // turn on Wifi calling setting
        final Intent intent = new Intent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_CHANGED);
        intent.putExtra(EXTRA_TOGGLE_STATE, true);

        // change the setting
        mReceiver.onReceive(mContext, intent);

        verify((mMockImsMmTelManager)).setVoWiFiSettingEnabled(mWfcSettingCaptor.capture());

        // assert the change
        assertThat(mWfcSettingCaptor.getValue()).isTrue();
    }

    @Test
    public void test_CreateWifiCallingPreferenceSlice_prefNotEditable() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        mWfcSliceHelper.setIsWifiCallingPrefEditable(false);

        final Slice slice = mWfcSliceHelper.createWifiCallingPreferenceSlice(
                CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateWifiCallingPreferenceSlice_wfcOff() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(false);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        mWfcSliceHelper.setIsWifiCallingPrefEditable(true);

        final Slice slice = mWfcSliceHelper.createWifiCallingPreferenceSlice(
                CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsUnavailableSlice(slice, null,
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY),
                mContext.getString(R.string.wifi_calling_mode_title));
    }

    @Test
    public void test_CreateWifiCallingPreferenceSlice_success() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mMockImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        mWfcSliceHelper.setIsWifiCallingPrefEditable(true);

        final Slice slice = mWfcSliceHelper.createWifiCallingPreferenceSlice(
                CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingPreferenceSlice(slice, null,
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
    }

    @Test
    public void test_SettingsSliceProvider_getWfcPreferenceSlice() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mMockImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mSlicesFeatureProvider.getNewWifiCallingSliceHelper(mContext))
                .thenReturn(mWfcSliceHelper);
        mWfcSliceHelper.setIsWifiCallingPrefEditable(true);

        final Slice slice = mProvider.onBindSlice(
                CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingPreferenceSlice(slice, null,
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
    }
    @Test
    public void test_SliceBroadcastReceiver_setWfcPrefCellularPref() {
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        when(mMockImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mSlicesFeatureProvider.getNewWifiCallingSliceHelper(mContext))
                .thenReturn(mWfcSliceHelper);
        mWfcSliceHelper.setIsWifiCallingPrefEditable(true);

        final ArgumentCaptor<Integer> mWfcPreferenceCaptor =
                ArgumentCaptor.forClass(Integer.class);

        // Change preference to Cellular pref
        final Intent intent = new Intent(
                WifiCallingSliceHelper.ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED);

        mReceiver.onReceive(mContext, intent);

        verify((mMockImsMmTelManager)).setVoWiFiModeSetting(mWfcPreferenceCaptor.capture());

        // assert the change
        assertThat(mWfcPreferenceCaptor.getValue()).isEqualTo(
                ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED);
    }

    private void testWifiCallingSettingsUnavailableSlice(Slice slice,
            SliceData sliceData, PendingIntent expectedPrimaryAction, String title) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        //Check there is no toggle action
        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        // Check whether the primary action is to open wifi calling settings activity
        final PendingIntent primaryPendingIntent =
                metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(expectedPrimaryAction);

        // Check the title
        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, title);
    }

    private void testWifiCallingSettingsToggleSlice(Slice slice,
            SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction mainToggleAction = toggles.get(0);

        // Check intent in Toggle Action
        final PendingIntent togglePendingIntent = mainToggleAction.getAction();
        final PendingIntent expectedToggleIntent = getBroadcastIntent(
                WifiCallingSliceHelper.ACTION_WIFI_CALLING_CHANGED);
        assertThat(togglePendingIntent).isEqualTo(expectedToggleIntent);

        // Check primary intent
        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        final PendingIntent expectedPendingIntent =
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY);
        assertThat(primaryPendingIntent).isEqualTo(expectedPendingIntent);

        // Check the title
        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, mContext.getString(R.string.wifi_calling_settings_title));
    }

    private void testWifiCallingPreferenceSlice(Slice slice, SliceData sliceData,
            PendingIntent expectedPrimaryAction) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        //Check there is no toggle action
        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        // Check whether the primary action is to open wifi calling settings activity
        final PendingIntent primaryPendingIntent =
                metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(expectedPrimaryAction);

        // Get all the rows
        final ListContent listContent = new ListContent(mContext, slice);
        final ArrayList<SliceContent> rowItems = listContent.getRowItems();

        assertThat(rowItems.size()).isEqualTo(4 /* 4 items including header */);

        // First row is HEADER
        RowContent rowContent = (RowContent) rowItems.get(0);
        assertThat(rowContent.getTitleItem().getText()).isEqualTo(mContext.getText(
                R.string.wifi_calling_mode_title));

        // next is WIFI_ONLY
        rowContent =  (RowContent) rowItems.get(1);
        assertThat(rowContent.getTitleItem().getText()).isEqualTo(mContext.getText(
                com.android.internal.R.string.wfc_mode_wifi_only_summary));

        // next is WIFI_PREFERRED
        rowContent =  (RowContent) rowItems.get(2);
        assertThat(rowContent.getTitleItem().getText()).isEqualTo(mContext.getText(
                com.android.internal.R.string.wfc_mode_wifi_preferred_summary));

        // next is CELLULAR_PREFERRED
        rowContent =  (RowContent) rowItems.get(3);
        assertThat(rowContent.getTitleItem().getText()).isEqualTo(mContext.getText(
                com.android.internal.R.string.wfc_mode_cellular_preferred_summary));
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(SETTINGS_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private void assertTitle(List<SliceItem> sliceItems, String title) {
        boolean hasTitle = false;
        for (SliceItem item : sliceItems) {
            final List<SliceItem> titleItems = SliceQuery.findAll(item, FORMAT_TEXT, HINT_TITLE,
                    null /* non-hints */);
            if (titleItems == null) {
                continue;
            }

            hasTitle = true;
            for (SliceItem subTitleItem : titleItems) {
                assertThat(subTitleItem.getText()).isEqualTo(title);
            }
        }
        assertThat(hasTitle).isTrue();
    }
    private class FakeWifiCallingSliceHelper extends WifiCallingSliceHelper {
        int mSubId = 1;
        boolean isWifiCallingPrefEditable = true;
        boolean isWifiOnlySupported = true;

        private Intent mActivationAppIntent;
        FakeWifiCallingSliceHelper(Context context) {
            super(context);
            mActivationAppIntent = null;
        }

        @Override
        protected CarrierConfigManager getCarrierConfigManager(Context mContext) {
            return mMockCarrierConfigManager;
        }

        protected ImsMmTelManager getImsMmTelManager(int subId) {
            return mMockImsMmTelManager;
        }

        protected int getDefaultVoiceSubId() {
            return mSubId;
        }

        private void setDefaultVoiceSubId(int id) {
            mSubId = id;
        }

        @Override
        protected Intent getWifiCallingCarrierActivityIntent(int subId) {
            return mActivationAppIntent;
        }
        @Override
        protected boolean isCarrierConfigManagerKeyEnabled(String key, int subId,
                boolean defaultValue) {
            if(key.equals(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL)) {
                return isWifiCallingPrefEditable;
            } else if(key.equals(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL)) {
                return isWifiOnlySupported;
            }
            return defaultValue;
        }

        private void setActivationAppIntent(Intent intent) {
            mActivationAppIntent = intent;
        }

        private void setIsWifiCallingPrefEditable(boolean isWifiCallingPrefEditable) {
            this.isWifiCallingPrefEditable = isWifiCallingPrefEditable;
        }
    }
}
