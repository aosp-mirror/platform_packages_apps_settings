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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.telephony.CarrierConfigManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.slices.SliceData;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiCallingSliceHelperTest {

    private Context mContext;
    @Mock
    private CarrierConfigManager mMockCarrierConfigManager;

    @Mock
    private ImsManager mMockImsManager;

    private final Uri mWfcURI = Uri.parse("content://com.android.settings.slices/wifi_calling");

    private FakeWifiCallingSliceHelper mWfcSliceHelper;
    private SettingsSliceProvider mProvider;
    private SliceBroadcastReceiver mReceiver;
    private FakeFeatureFactory mFeatureFactory;
    private SlicesFeatureProvider mSlicesFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        //setup for SettingsSliceProvider tests
        mProvider = spy(new SettingsSliceProvider());
        doReturn(mContext).when(mProvider).getContext();

        //setup for SliceBroadcastReceiver test
        mReceiver = spy(new SliceBroadcastReceiver());

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSlicesFeatureProvider = mFeatureFactory.getSlicesFeatureProvider();

        // Prevent crash in SliceMetadata.
        Resources resources = spy(mContext.getResources());
        doReturn(60).when(resources).getDimensionPixelSize(anyInt());
        doReturn(resources).when(mContext).getResources();

        mWfcSliceHelper = new FakeWifiCallingSliceHelper(mContext);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void test_CreateWifiCallingSlice_invalidSubId() {
        mWfcSliceHelper.setDefaultVoiceSubId(-1);

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(mWfcURI);

        testWifiCallingSettingsUnavailableSlice(slice, null,
                WifiCallingSliceHelper.getSettingsIntent(mContext));
    }

    @Test
    public void test_CreateWifiCallingSlice_wfcNotSupported() {
        doReturn(false).when(mMockImsManager).isWfcEnabledByPlatform();

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(mWfcURI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsUnavailableSlice(slice, null,
                WifiCallingSliceHelper.getSettingsIntent(mContext));
    }

    @Test
    public void test_CreateWifiCallingSlice_needsActivation() {
        /* In cases where activation is needed and the user action
        would be turning on the wifi calling (i.e. if wifi calling is
        turned off) we need to guide the user to wifi calling settings
        activity so the user can perform the activation there.(PrimaryAction)
         */
        doReturn(true).when(mMockImsManager).isWfcEnabledByPlatform();
        doReturn(true).when(mMockImsManager).isWfcProvisionedOnDevice();
        doReturn(false).when(mMockImsManager).isWfcEnabledByUser();
        doReturn(false).when(mMockImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(null).when(mMockCarrierConfigManager).getConfigForSubId(1);
        mWfcSliceHelper.setActivationAppIntent(new Intent()); // dummy Intent

        final Slice slice  = mWfcSliceHelper.createWifiCallingSlice(mWfcURI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsUnavailableSlice(slice, null,
                getActivityIntent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
    }

    @Test
    public void test_CreateWifiCallingSlice_success() {
        doReturn(true).when(mMockImsManager).isWfcEnabledByPlatform();
        doReturn(true).when(mMockImsManager).isWfcProvisionedOnDevice();
        doReturn(true).when(mMockImsManager).isWfcEnabledByUser();
        doReturn(true).when(mMockImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(null).when(mMockCarrierConfigManager).getConfigForSubId(1);

        final Slice slice = mWfcSliceHelper.createWifiCallingSlice(mWfcURI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsToggleSlice(slice, null);
    }

    @Test
    public void test_SettingSliceProvider_getsRightSliceWifiCalling() {
        doReturn(true).when(mMockImsManager).isWfcEnabledByPlatform();
        doReturn(true).when(mMockImsManager).isWfcProvisionedOnDevice();
        doReturn(true).when(mMockImsManager).isWfcEnabledByUser();
        doReturn(true).when(mMockImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(null).when(mMockCarrierConfigManager).getConfigForSubId(1);
        doReturn(mWfcSliceHelper).when(mSlicesFeatureProvider)
              .getNewWifiCallingSliceHelper(mContext);

        final Slice slice = mProvider.onBindSlice(mWfcURI);

        assertThat(mWfcSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testWifiCallingSettingsToggleSlice(slice, null);
    }

    @Test
    public void test_SliceBroadcastReceiver_toggleOffWifiCalling() {
        doReturn(true).when(mMockImsManager).isWfcEnabledByPlatform();
        doReturn(true).when(mMockImsManager).isWfcProvisionedOnDevice();
        doReturn(false).when(mMockImsManager).isWfcEnabledByUser();
        doReturn(true).when(mMockImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(mWfcSliceHelper).when(mSlicesFeatureProvider)
              .getNewWifiCallingSliceHelper(mContext);
        mWfcSliceHelper.setActivationAppIntent(null);

        ArgumentCaptor<Boolean> mWfcSettingCaptor = ArgumentCaptor.forClass(Boolean.class);

        // turn on Wifi calling setting
        Intent intent = new Intent(WifiCallingSliceHelper.ACTION_WIFI_CALLING_CHANGED);
        intent.putExtra(EXTRA_TOGGLE_STATE, true);

        // change the setting
        mReceiver.onReceive(mContext, intent);

        verify((mMockImsManager)).setWfcSetting(mWfcSettingCaptor.capture());

        // assert the change
        assertThat(mWfcSettingCaptor.getValue()).isTrue();
    }

    private void testWifiCallingSettingsUnavailableSlice(Slice slice,
            SliceData sliceData, PendingIntent expectedPrimaryAction) {
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
        assertTitle(sliceItems, mContext.getString(R.string.wifi_calling_settings_title));
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

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private void assertTitle(List<SliceItem> sliceItems, String title) {
        boolean hasTitle = false;
        for (SliceItem item : sliceItems) {
            List<SliceItem> titleItems = SliceQuery.findAll(item, FORMAT_TEXT, HINT_TITLE,
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

        private Intent mActivationAppIntent;
        FakeWifiCallingSliceHelper(Context context) {
            super(context);
            mActivationAppIntent = null;
        }

        @Override
        protected CarrierConfigManager getCarrierConfigManager(Context mContext) {
            return mMockCarrierConfigManager;
        }

        @Override
        protected ImsManager getImsManager(int subId) {
            return mMockImsManager;
        }

        protected int getDefaultVoiceSubId() {
            return mSubId;
        }

        protected void setDefaultVoiceSubId(int id) {
            mSubId = id;
        }

        @Override
        protected Intent getWifiCallingCarrierActivityIntent(int subId) {
            return mActivationAppIntent;
        }

        public void setActivationAppIntent(Intent intent) {
            mActivationAppIntent = intent;
        }
    }
}
