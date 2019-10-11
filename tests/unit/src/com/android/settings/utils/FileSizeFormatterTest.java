/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.utils;

import static com.android.settings.utils.FileSizeFormatter.GIGABYTE_IN_BYTES;
import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FileSizeFormatterTest {
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void formatFileSize_zero() throws Exception {
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                0 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("0.00 GB");
    }

    @Test
    public void formatFileSize_smallSize() throws Exception {
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                MEGABYTE_IN_BYTES * 11 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("0.01 GB");
    }

    @Test
    public void formatFileSize_lessThanOneSize() throws Exception {
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                MEGABYTE_IN_BYTES * 155 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("0.16 GB");
    }

    @Test
    public void formatFileSize_greaterThanOneSize() throws Exception {
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                MEGABYTE_IN_BYTES * 1551 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("1.6 GB");
    }

    @Test
    public void formatFileSize_greaterThanTen() throws Exception {
        // Should round down due to truncation
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                GIGABYTE_IN_BYTES * 15 + MEGABYTE_IN_BYTES * 50 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("15 GB");
    }

    @Test
    public void formatFileSize_handlesNegativeFileSizes() throws Exception {
        assertThat(
                        FileSizeFormatter.formatFileSize(
                                mContext,
                                MEGABYTE_IN_BYTES * -155 /* size */,
                                com.android.internal.R.string.gigabyteShort,
                                GIGABYTE_IN_BYTES))
                .isEqualTo("-0.16 GB");
    }
}
