/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingDeviceItemTest {
    private static final String TEST_NAME = "test";
    private static final int TEST_GROUP_ID = 1;
    private static final boolean TEST_IS_ACTIVE = true;

    @Test
    public void createItem_new() {
        AudioSharingDeviceItem item =
                new AudioSharingDeviceItem(TEST_NAME, TEST_GROUP_ID, TEST_IS_ACTIVE);
        assertThat(item.getName()).isEqualTo(TEST_NAME);
        assertThat(item.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(item.isActive()).isEqualTo(TEST_IS_ACTIVE);
    }

    @Test
    public void createItem_withParcel() {
        AudioSharingDeviceItem item =
                new AudioSharingDeviceItem(TEST_NAME, TEST_GROUP_ID, TEST_IS_ACTIVE);
        Parcel parcel = Parcel.obtain();
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AudioSharingDeviceItem newItem = new AudioSharingDeviceItem(parcel);
        assertThat(newItem.getName()).isEqualTo(TEST_NAME);
        assertThat(newItem.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(newItem.isActive()).isEqualTo(TEST_IS_ACTIVE);
    }

    @Test
    public void describeContents_returnsZero() {
        AudioSharingDeviceItem item =
                new AudioSharingDeviceItem(TEST_NAME, TEST_GROUP_ID, TEST_IS_ACTIVE);
        assertThat(item.describeContents()).isEqualTo(0);
    }

    @Test
    public void creator_newArray() {
        assertThat(AudioSharingDeviceItem.CREATOR.newArray(2)).hasLength(2);
    }

    @Test
    public void creator_createFromParcel() {
        AudioSharingDeviceItem item =
                new AudioSharingDeviceItem(TEST_NAME, TEST_GROUP_ID, TEST_IS_ACTIVE);
        Parcel parcel = Parcel.obtain();
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AudioSharingDeviceItem itemFromParcel =
                AudioSharingDeviceItem.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        assertThat(itemFromParcel.getName()).isEqualTo(TEST_NAME);
        assertThat(itemFromParcel.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(itemFromParcel.isActive()).isEqualTo(TEST_IS_ACTIVE);
    }
}
