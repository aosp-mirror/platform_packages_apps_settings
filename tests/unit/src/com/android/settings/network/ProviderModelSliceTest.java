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

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.network.ProviderModelSlice.PREF_HAS_TURNED_OFF_MOBILE_DATA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.widget.SliceLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Utils;
import com.android.settings.network.telephony.NetworkProviderWorker;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settings.wifi.slice.WifiSliceItem;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ProviderModelSliceTest {
    private static final Uri PROVIDER_MODEL_SLICE_URI =
            Uri.parse("content://com.android.settings.slices/action/provider_model");
    private static final int MOCK_SLICE_LEVEL = 3;
    private static final int SUB_ID = 2;

    private Context mContext;
    private MockProviderModelSlice mMockProviderModelSlice;
    List<WifiSliceItem> mWifiList = new ArrayList<>();
    private ListBuilder mListBuilder;
    private MockNetworkProviderWorker mMockNetworkProviderWorker;

    @Mock
    private UserManager mUserManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ProviderModelSliceHelper mProviderModelSliceHelper;
    @Mock
    private WifiSliceItem mMockWifiSliceItem1;
    @Mock
    private WifiSliceItem mMockWifiSliceItem2;
    @Mock
    private WifiSliceItem mMockWifiSliceItem3;
    @Mock
    ListBuilder.RowBuilder mMockCarrierRowBuild;
    @Mock
    WifiPickerTracker mWifiPickerTracker;
    AlertDialog mMockAlertDialog;

    private FakeFeatureFactory mFeatureFactory;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.wifiTrackerLibProvider
                .createWifiPickerTracker(
                        any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mWifiPickerTracker);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mMockNetworkProviderWorker = spy(new MockNetworkProviderWorker(mContext,
                PROVIDER_MODEL_SLICE_URI));
        mMockProviderModelSlice = spy(new MockProviderModelSlice(
                mContext, mMockNetworkProviderWorker));
        mMockAlertDialog =  new AlertDialog.Builder(mContext)
                .setTitle("")
                .create();
        mMockAlertDialog = spy(mMockAlertDialog);
        mMockProviderModelSlice.setMobileDataDisableDialog(mMockAlertDialog);
        mListBuilder = spy(new ListBuilder(mContext, PROVIDER_MODEL_SLICE_URI,
                ListBuilder.INFINITY).setAccentColor(-1));
        when(mProviderModelSliceHelper.createListBuilder(PROVIDER_MODEL_SLICE_URI)).thenReturn(
                mListBuilder);
        when(mProviderModelSliceHelper.getSubscriptionManager()).thenReturn(mSubscriptionManager);

        mWifiList = new ArrayList<>();
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);

        mockBuilder();
    }

    @Test
    @UiThreadTest
    public void getBroadcastIntent_shouldHaveFlagReceiverForeground() {
        final PendingIntent pendingIntent = mMockProviderModelSlice.getBroadcastIntent(mContext);

        final int flags = pendingIntent.getIntent().getFlags();
        assertThat(flags & Intent.FLAG_RECEIVER_FOREGROUND)
                .isEqualTo(Intent.FLAG_RECEIVER_FOREGROUND);
    }

    @Test
    @UiThreadTest
    public void getSlice_isGuestUser_shouldNotAddRow() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, never()).addRow(any());
    }

    @Test
    @UiThreadTest
    public void getSlice_noWifiAndHasCarrierNoData_oneCarrier() {
        mWifiList.clear();
        mMockNetworkProviderWorker.updateSelfResults(null);
        mockHelperCondition(false, true, false, null);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(1)).addRow(mMockCarrierRowBuild);
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isFalse();
    }

    @Test
    @UiThreadTest
    public void getSlice_noWifiAndNoCarrier_oneCarrier() {
        mWifiList.clear();
        mMockProviderModelSlice = new MockProviderModelSlice(mContext, null);
        mockHelperCondition(false, true, true, null);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(1)).addRow(mMockCarrierRowBuild);
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isFalse();
    }

    @Test
    @UiThreadTest
    public void getSlice_airplaneModeIsOn_oneWifiToggle() {
        mWifiList.clear();
        mMockNetworkProviderWorker.updateSelfResults(null);
        mockHelperCondition(true, false, false, null);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(1)).addRow(any(ListBuilder.RowBuilder.class));
        final SliceItem sliceTitle =
                SliceMetadata.from(mContext, slice).getListContent().getHeader().getTitleItem();
        assertThat(sliceTitle.getText()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "wifi_settings"));
    }

    @Test
    @UiThreadTest
    public void getSlice_haveTwoWifiAndOneCarrier_getFiveRow() {
        mWifiList.clear();
        mockWifiItemCondition(mMockWifiSliceItem1, "wifi1", "wifi1",
                WifiEntry.CONNECTED_STATE_CONNECTED, "wifi1_key", true);
        mWifiList.add(mMockWifiSliceItem1);
        mockWifiItemCondition(mMockWifiSliceItem2, "wifi2", "wifi2",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi2_key", true);
        mWifiList.add(mMockWifiSliceItem2);
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);
        mockHelperCondition(false, true, true, mWifiList.get(0));

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(1)).addRow(mMockCarrierRowBuild);
        verify(mListBuilder, times(5)).addRow(any(ListBuilder.RowBuilder.class));
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isTrue();
    }

    @Test
    @UiThreadTest
    public void getSlice_haveOneConnectedWifiAndTwoDisconnectedWifiAndNoCarrier_getFiveRow() {
        mWifiList.clear();
        mockWifiItemCondition(mMockWifiSliceItem1, "wifi1", "wifi1",
                WifiEntry.CONNECTED_STATE_CONNECTED, "wifi1_key", true);
        mWifiList.add(mMockWifiSliceItem1);
        mockWifiItemCondition(mMockWifiSliceItem2, "wifi2", "wifi2",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi2_key", true);
        mWifiList.add(mMockWifiSliceItem2);
        mockWifiItemCondition(mMockWifiSliceItem3, "wifi3", "wifi3",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi3_key", true);
        mWifiList.add(mMockWifiSliceItem3);
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);
        mockHelperCondition(false, false, false, mWifiList.get(0));

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(5)).addRow(any(ListBuilder.RowBuilder.class));
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isTrue();
    }

    @Test
    @UiThreadTest
    public void getSlice_haveTwoDisconnectedWifiAndNoCarrier_getFourRow() {
        mWifiList.clear();
        mockWifiItemCondition(mMockWifiSliceItem1, "wifi1", "wifi1",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi1_key", true);
        mWifiList.add(mMockWifiSliceItem1);
        mockWifiItemCondition(mMockWifiSliceItem2, "wifi2", "wifi2",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi2_key", true);
        mWifiList.add(mMockWifiSliceItem2);
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);
        mockHelperCondition(false, false, false, null);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        verify(mListBuilder, times(4)).addRow(any(ListBuilder.RowBuilder.class));
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isTrue();
    }

    @Test
    @UiThreadTest
    public void getSlice_haveEthernetAndCarrierAndTwoDisconnectedWifi_getSixRow() {
        mWifiList.clear();
        mockWifiItemCondition(mMockWifiSliceItem1, "wifi1", "wifi1",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi1_key", true);
        mWifiList.add(mMockWifiSliceItem1);
        mockWifiItemCondition(mMockWifiSliceItem2, "wifi2", "wifi2",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi2_key", true);
        mWifiList.add(mMockWifiSliceItem2);
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);
        mockHelperCondition(false, true, true, null);
        when(mMockNetworkProviderWorker.getInternetType())
                .thenReturn(InternetUpdater.INTERNET_ETHERNET);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        assertThat(mMockProviderModelSlice.hasCreateEthernetRow()).isTrue();
        verify(mListBuilder, times(1)).addRow(mMockCarrierRowBuild);
        verify(mListBuilder, times(6)).addRow(any(ListBuilder.RowBuilder.class));
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isTrue();
    }

    @Test
    @UiThreadTest
    public void getSlice_haveEthernetAndCarrierAndConnectedWifiAndDisconnectedWifi_getSixRow() {
        mWifiList.clear();
        mockWifiItemCondition(mMockWifiSliceItem1, "wifi1", "wifi1",
                WifiEntry.CONNECTED_STATE_CONNECTED, "wifi1_key", true);
        mWifiList.add(mMockWifiSliceItem1);
        mockWifiItemCondition(mMockWifiSliceItem2, "wifi2", "wifi2",
                WifiEntry.CONNECTED_STATE_DISCONNECTED, "wifi2_key", true);
        mWifiList.add(mMockWifiSliceItem2);
        mMockNetworkProviderWorker.updateSelfResults(mWifiList);
        mockHelperCondition(false, true, true, mWifiList.get(0));
        when(mMockNetworkProviderWorker.getInternetType())
                .thenReturn(InternetUpdater.INTERNET_ETHERNET);

        final Slice slice = mMockProviderModelSlice.getSlice();

        assertThat(slice).isNotNull();
        assertThat(mMockProviderModelSlice.hasCreateEthernetRow()).isTrue();
        verify(mListBuilder, times(1)).addRow(mMockCarrierRowBuild);
        verify(mListBuilder, times(6)).addRow(any(ListBuilder.RowBuilder.class));
        assertThat(mMockProviderModelSlice.hasSeeAllRow()).isTrue();
    }

    @Test
    public void getBackgroundWorkerClass_isGuestUser_returnNull() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        assertThat(mMockProviderModelSlice.getBackgroundWorkerClass()).isNull();
    }

    @Test
    public void getBackgroundWorkerClass_notGuestUser_returnWorkerClass() {
        when(mUserManager.isGuestUser()).thenReturn(false);

        assertThat(mMockProviderModelSlice.getBackgroundWorkerClass())
                .isEqualTo(NetworkProviderWorker.class);
    }

    @Test
    public void providerModelSlice_hasCorrectUri() {
        assertThat(mMockProviderModelSlice.getUri()).isEqualTo(PROVIDER_MODEL_SLICE_URI);
    }

    private void mockHelperCondition(boolean airplaneMode, boolean hasCarrier,
            boolean isDataSimActive, WifiSliceItem connectedWifiItem) {
        when(mProviderModelSliceHelper.isAirplaneModeEnabled()).thenReturn(airplaneMode);
        when(mProviderModelSliceHelper.hasCarrier()).thenReturn(hasCarrier);
        when(mProviderModelSliceHelper.isDataSimActive()).thenReturn(isDataSimActive);
        when(mProviderModelSliceHelper.getConnectedWifiItem(any())).thenReturn(connectedWifiItem);
    }

    private void mockWifiItemCondition(WifiSliceItem mockWifiItem, String title, String summary,
            int connectedState, String key, boolean shouldEditBeforeConnect) {
        when(mockWifiItem.getTitle()).thenReturn(title);
        when(mockWifiItem.getSummary()).thenReturn(summary);
        when(mockWifiItem.getConnectedState()).thenReturn(connectedState);
        when(mockWifiItem.getLevel()).thenReturn(MOCK_SLICE_LEVEL);
        when(mockWifiItem.getKey()).thenReturn(key);
        when(mockWifiItem.shouldEditBeforeConnect()).thenReturn(shouldEditBeforeConnect);
    }

    private void mockBuilder() {
        SliceAction mockSliceAction = getPrimarySliceAction();
        when(mMockCarrierRowBuild.getTitle()).thenReturn("mockRow");
        when(mMockCarrierRowBuild.getPrimaryAction()).thenReturn(mockSliceAction);
        when(mProviderModelSliceHelper.createCarrierRow(anyString())).thenReturn(
                mMockCarrierRowBuild);
    }

    private SliceAction getPrimarySliceAction() {
        return SliceAction.createDeeplink(
                getPrimaryAction(),
                Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT)),
                ListBuilder.ICON_IMAGE,
                ResourcesUtils.getResourcesString(mContext, "summary_placeholder"));
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = new Intent("android.settings.NETWORK_PROVIDER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, PendingIntent.FLAG_IMMUTABLE /* flags */);
    }

    public class MockNetworkProviderWorker extends NetworkProviderWorker {
        MockNetworkProviderWorker(Context context, Uri uri) {
            super(context, uri);
        }

        public void updateSelfResults(List<WifiSliceItem> results) {
            this.updateResults(results);
        }
    }

    public class MockProviderModelSlice extends ProviderModelSlice {
        private MockNetworkProviderWorker mNetworkProviderWorker;
        private boolean mHasCreateEthernetRow;
        private boolean mHasSeeAllRow;
        private AlertDialog mAlertDialog;

        MockProviderModelSlice(Context context, MockNetworkProviderWorker networkProviderWorker) {
            super(context);
            mNetworkProviderWorker = networkProviderWorker;
        }

        @Override
        ProviderModelSliceHelper getHelper() {
            return mProviderModelSliceHelper;
        }

        @Override
        NetworkProviderWorker getWorker() {
            return mNetworkProviderWorker;
        }

        @Override
        AlertDialog getMobileDataDisableDialog(int defaultSubId, String carrierName) {
            return mAlertDialog;
        }

        @Override
        ListBuilder.RowBuilder createEthernetRow() {
            mHasCreateEthernetRow = true;
            return super.createEthernetRow();
        }

        @Override
        protected ListBuilder.RowBuilder getSeeAllRow() {
            mHasSeeAllRow = true;
            return super.getSeeAllRow();
        }

        @Override
        public ListBuilder.RowBuilder getWifiSliceItemRow(WifiSliceItem wifiSliceItem) {
            return super.getWifiSliceItemRow(wifiSliceItem);
        }

        public boolean hasCreateEthernetRow() {
            return mHasCreateEthernetRow;
        }

        public boolean hasSeeAllRow() {
            return mHasSeeAllRow;
        }

        public void setMobileDataDisableDialog(AlertDialog alertDialog) {
            mAlertDialog = alertDialog;
        }
    }

    @Test
    @UiThreadTest
    public void onNotifyChange_FirstTimeDisableToggleState_showDialog() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, false);
        SharedPreferences sharedPref = mMockProviderModelSlice.getSharedPreference();
        when(mProviderModelSliceHelper.getMobileTitle()).thenReturn("mockRow");
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_HAS_TURNED_OFF_MOBILE_DATA, true);
            editor.apply();
        }

        mMockProviderModelSlice.onNotifyChange(intent);

        verify(mMockAlertDialog).show();
    }

    @Test
    @UiThreadTest
    public void onNotifyChange_EnableToggleState_doNotShowDialog() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, true);
        SharedPreferences sharedPref = mMockProviderModelSlice.getSharedPreference();
        when(mProviderModelSliceHelper.getMobileTitle()).thenReturn("mockRow");
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_HAS_TURNED_OFF_MOBILE_DATA, true);
            editor.apply();
        }

        mMockProviderModelSlice.onNotifyChange(intent);

        verify(mMockAlertDialog, never()).show();
    }

    @Test
    @UiThreadTest
    public void onNotifyChange_notFirstTimeDisableToggleState_doNotShowDialog() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, false);
        SharedPreferences sharedPref = mMockProviderModelSlice.getSharedPreference();
        when(mProviderModelSliceHelper.getMobileTitle()).thenReturn("mockRow");
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_HAS_TURNED_OFF_MOBILE_DATA, false);
            editor.apply();
        }

        mMockProviderModelSlice.onNotifyChange(intent);

        verify(mMockAlertDialog, never()).show();
    }

    @Test
    public void doCarrierNetworkAction_toggleActionSetDataEnabled_setCarrierNetworkEnabledTrue() {
        mMockProviderModelSlice.doCarrierNetworkAction(true /* isToggleAction */,
                true /* isDataEnabled */, SUB_ID);

        verify(mMockNetworkProviderWorker).setCarrierNetworkEnabledIfNeeded(true, SUB_ID);
    }

    @Test
    public void doCarrierNetworkAction_toggleActionSetDataDisabled_setCarrierNetworkEnabledFalse() {
        mMockProviderModelSlice.doCarrierNetworkAction(true /* isToggleAction */,
                false /* isDataEnabled */, SUB_ID);

        verify(mMockNetworkProviderWorker).setCarrierNetworkEnabledIfNeeded(false, SUB_ID);
    }

    @Test
    public void doCarrierNetworkAction_primaryActionAndDataEnabled_connectCarrierNetwork() {
        mMockProviderModelSlice.doCarrierNetworkAction(false /* isToggleAction */,
                true /* isDataEnabled */, SUB_ID);

        verify(mMockNetworkProviderWorker).connectCarrierNetwork();
    }

    @Test
    public void doCarrierNetworkAction_primaryActionAndDataDisabled_notConnectCarrierNetwork() {
        mMockProviderModelSlice.doCarrierNetworkAction(false /* isToggleAction */,
                false /* isDataEnabled */, SUB_ID);

        verify(mMockNetworkProviderWorker, never()).connectCarrierNetwork();
    }
}
