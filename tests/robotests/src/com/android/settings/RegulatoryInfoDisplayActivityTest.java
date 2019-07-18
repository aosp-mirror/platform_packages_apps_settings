/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.os.SystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;


@RunWith(RobolectricTestRunner.class)
public class RegulatoryInfoDisplayActivityTest {

    private static final String SKU_PROP_KEY = "ro.boot.hardware.sku";
    private static final String COO_PROP_KEY = "ro.boot.hardware.coo";

    private RegulatoryInfoDisplayActivity mRegulatoryInfoDisplayActivity;

    @Before
    public void setUp() {
        mRegulatoryInfoDisplayActivity = Robolectric.buildActivity(
                RegulatoryInfoDisplayActivity.class).create().get();
    }

    @Test
    public void getResourceId_noSkuProperty_shouldReturnDefaultLabel() {
        SystemProperties.set(SKU_PROP_KEY, "");

        final int expectedResId = getResourceId("regulatory_info");
        assertThat(mRegulatoryInfoDisplayActivity.getResourceId()).isEqualTo(expectedResId);
    }

    @Test
    public void getResourceId_noCooProperty_shouldReturnSkuLabel() {
        SystemProperties.set(SKU_PROP_KEY, "sku");
        SystemProperties.set(COO_PROP_KEY, "");

        final int expectedResId = getResourceId("regulatory_info_sku");
        assertThat(mRegulatoryInfoDisplayActivity.getResourceId()).isEqualTo(expectedResId);
    }

    @Test
    public void getResourceId_hasSkuAndCooProperties_shouldReturnCooLabel() {
        SystemProperties.set(SKU_PROP_KEY, "sku1");
        SystemProperties.set(COO_PROP_KEY, "coo");

        final int expectedResId = getResourceId("regulatory_info_sku1_coo");
        assertThat(mRegulatoryInfoDisplayActivity.getResourceId()).isEqualTo(expectedResId);
    }

    @Test
    public void getResourceId_noCorrespondingCooLabel_shouldReturnSkuLabel() {
        SystemProperties.set(SKU_PROP_KEY, "sku");
        SystemProperties.set(COO_PROP_KEY, "unknown");

        final int expectedResId = getResourceId("regulatory_info_sku");
        assertThat(mRegulatoryInfoDisplayActivity.getResourceId()).isEqualTo(expectedResId);
    }

    private int getResourceId(String resourceName) {
        return mRegulatoryInfoDisplayActivity.getResources().getIdentifier(resourceName, "drawable",
                mRegulatoryInfoDisplayActivity.getPackageName());
    }
}
