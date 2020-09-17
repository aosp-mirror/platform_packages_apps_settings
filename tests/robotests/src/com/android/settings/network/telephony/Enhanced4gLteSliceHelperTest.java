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

package com.android.settings.network.telephony;

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
import android.telephony.SubscriptionManager;
import android.telephony.ims.ProvisioningManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.network.ims.MockVolteQueryImsState;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSubscriptionManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class Enhanced4gLteSliceHelperTest {
    private static final int SUB_ID = 1;

    @Mock
    private CarrierConfigManager mMockCarrierConfigManager;

    @Mock
    private ProvisioningManager mProvisioningManager;

    private ShadowSubscriptionManager mShadowSubscriptionManager;
    private MockVolteQueryImsState mQueryImsState;

    private Context mContext;
    private FakeEnhanced4gLteSliceHelper mEnhanced4gLteSliceHelper;
    private SettingsSliceProvider mProvider;
    private SliceBroadcastReceiver mReceiver;
    private FakeFeatureFactory mFeatureFactory;
    private SlicesFeatureProvider mSlicesFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mShadowSubscriptionManager = Shadow.extract(mContext.getSystemService(
                SubscriptionManager.class));
        mShadowSubscriptionManager.setDefaultVoiceSubscriptionId(SUB_ID);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSlicesFeatureProvider = mFeatureFactory.getSlicesFeatureProvider();

        //setup for SettingsSliceProvider tests
        mProvider = spy(new SettingsSliceProvider());
        doReturn(mContext).when(mProvider).getContext();
        mProvider.onCreateSliceProvider();

        //setup for SliceBroadcastReceiver test
        mReceiver = spy(new SliceBroadcastReceiver());

        mQueryImsState = spy(new MockVolteQueryImsState(mContext, SUB_ID));
        mQueryImsState.setEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        mQueryImsState.setServiceStateReady(true);
        mQueryImsState.setIsEnabledByUser(true);

        mEnhanced4gLteSliceHelper = spy(new FakeEnhanced4gLteSliceHelper(mContext));
        doReturn(mQueryImsState).when(mEnhanced4gLteSliceHelper).queryImsState(anyInt());

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_invalidSubId() {
        mQueryImsState.setEnabledByPlatform(false);
        mQueryImsState.setIsProvisionedOnDevice(false);
        mQueryImsState.setIsEnabledByUser(false);
        mShadowSubscriptionManager.setDefaultVoiceSubscriptionId(-1);

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_enhanced4gLteNotSupported() {
        mQueryImsState.setEnabledByPlatform(false);

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(mEnhanced4gLteSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_success() {
        mQueryImsState.setEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        when(mMockCarrierConfigManager.getConfigForSubId(1)).thenReturn(null);

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(mEnhanced4gLteSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testEnhanced4gLteSettingsToggleSlice(slice);
    }

    @Test
    public void test_SettingSliceProvider_getsRightSliceEnhanced4gLte() {
        mQueryImsState.setEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        when(mMockCarrierConfigManager.getConfigForSubId(1)).thenReturn(null);
        when(mSlicesFeatureProvider.getNewEnhanced4gLteSliceHelper(mContext))
                .thenReturn(mEnhanced4gLteSliceHelper);

        final Slice slice = mProvider.onBindSlice(CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(mEnhanced4gLteSliceHelper.getDefaultVoiceSubId()).isEqualTo(1);
        testEnhanced4gLteSettingsToggleSlice(slice);
    }

    @Test
    @Ignore
    public void test_SliceBroadcastReceiver_toggleOffEnhanced4gLte() {
        mQueryImsState.setEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByUser(false);
        when(mSlicesFeatureProvider.getNewEnhanced4gLteSliceHelper(mContext))
                .thenReturn(mEnhanced4gLteSliceHelper);

        final ArgumentCaptor<Boolean> mEnhanced4gLteSettingCaptor = ArgumentCaptor.forClass(
                Boolean.class);

        // turn on Enhanced4gLte setting
        final Intent intent = new Intent(Enhanced4gLteSliceHelper.ACTION_ENHANCED_4G_LTE_CHANGED);
        intent.putExtra(EXTRA_TOGGLE_STATE, true);

        // change the setting
        mReceiver.onReceive(mContext, intent);

        verify(mEnhanced4gLteSliceHelper).setEnhanced4gLteModeSetting(anyInt(),
                mEnhanced4gLteSettingCaptor.capture());

        // assert the change
        assertThat(mEnhanced4gLteSettingCaptor.getValue()).isTrue();
    }

    private void testEnhanced4gLteSettingsUnavailableSlice(Slice slice,
            PendingIntent expectedPrimaryAction) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        //Check there is no toggle action
        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        // Check whether the primary action is to open Enhanced4gLte settings activity
        final PendingIntent primaryPendingIntent =
                metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(expectedPrimaryAction);

        // Check the title
        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, mContext.getString(R.string.enhanced_4g_lte_mode_title));
    }

    private void testEnhanced4gLteSettingsToggleSlice(Slice slice) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction mainToggleAction = toggles.get(0);

        // Check intent in Toggle Action
        final PendingIntent togglePendingIntent = mainToggleAction.getAction();
        final PendingIntent expectedToggleIntent = getBroadcastIntent(
                Enhanced4gLteSliceHelper.ACTION_ENHANCED_4G_LTE_CHANGED);
        assertThat(togglePendingIntent).isEqualTo(expectedToggleIntent);

        // Check primary intent
        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        final PendingIntent expectedPendingIntent =
                getActivityIntent(Enhanced4gLteSliceHelper.ACTION_MOBILE_NETWORK_SETTINGS_ACTIVITY);
        assertThat(primaryPendingIntent).isEqualTo(expectedPendingIntent);

        // Check the title
        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, mContext.getString(R.string.enhanced_4g_lte_mode_title));
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

    private class FakeEnhanced4gLteSliceHelper extends Enhanced4gLteSliceHelper {
        int mSubId = SUB_ID;

        FakeEnhanced4gLteSliceHelper(Context context) {
            super(context);
        }

        @Override
        protected CarrierConfigManager getCarrierConfigManager() {
            return mMockCarrierConfigManager;
        }

        protected int getDefaultVoiceSubId() {
            return mSubId;
        }

        private void setDefaultVoiceSubId(int id) {
            mSubId = id;
        }
    }
}
