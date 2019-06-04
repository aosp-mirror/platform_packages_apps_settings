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

package com.android.settings.homepage.contextualcards.deviceinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;
import com.android.settings.R;
import com.android.settings.testutils.SliceTester;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowTelephonyManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDataUsageUtils.class)
public class DataUsageSliceTest {
    private static final String DATA_USAGE_TITLE = "Data usage";
    private static final String DATA_USAGE_SUMMARY = "test_summary";

    @Mock
    private NetworkStatsManager mNetworkStatsManager;

    private Context mContext;
    private DataUsageSlice mDataUsageSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        when(mContext.getSystemService(NetworkStatsManager.class)).thenReturn(mNetworkStatsManager);

        mDataUsageSlice = spy(new DataUsageSlice(mContext));

        final TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        final ShadowTelephonyManager shadowTelephonyManager = Shadows.shadowOf(telephonyManager);
        shadowTelephonyManager.setTelephonyManagerForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID, telephonyManager);
    }

    @Test
    public void getSlice_hasSim_shouldBeCorrectSliceContent() {
        ShadowDataUsageUtils.HAS_SIM = true;
        doReturn(DATA_USAGE_TITLE).when(mDataUsageSlice).getDataUsageText(any());
        doReturn(DATA_USAGE_SUMMARY).when(mDataUsageSlice).getCycleTime(any());

        final Slice slice = mDataUsageSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.data_usage_summary_title));

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_data_usage);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedIcon.toString());
    }

    @Test
    public void getSlice_hasNoSim_shouldShowNoSimCard() {
        ShadowDataUsageUtils.HAS_SIM = false;

        final Slice slice = mDataUsageSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.data_usage_summary_title));

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertAnySliceItemContainsTitle(sliceItems,
                mContext.getString(R.string.no_sim_card));
    }
}
