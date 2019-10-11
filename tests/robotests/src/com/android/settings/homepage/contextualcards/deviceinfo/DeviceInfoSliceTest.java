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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.telephony.SubscriptionInfo;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SliceTester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DeviceInfoSliceTest {

    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private Context mContext;
    private DeviceInfoSlice mDeviceInfoSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mDeviceInfoSlice = spy(new DeviceInfoSlice(mContext));
    }

    @Test
    public void getSlice_hasSubscriptionInfo_shouldBeCorrectSliceContent() {
        final String phoneNumber = "1111111111";
        doReturn(mSubscriptionInfo).when(mDeviceInfoSlice).getFirstSubscriptionInfo();
        doReturn(phoneNumber).when(mDeviceInfoSlice).getPhoneNumber();

        final Slice slice = mDeviceInfoSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.device_info_label));

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_info_outline_24dp);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedIcon.toString());

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, phoneNumber);
    }

    @Test
    public void getSlice_hasNoSubscriptionInfo_shouldShowUnknown() {
        final Slice slice = mDeviceInfoSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.device_info_label));

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertAnySliceItemContainsTitle(sliceItems,
                mContext.getString(R.string.device_info_default));
    }
}
