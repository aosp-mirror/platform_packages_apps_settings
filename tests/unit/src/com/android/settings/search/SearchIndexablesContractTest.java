/*
 * Copyright (C) 2016 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.provider.SearchIndexablesContract;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SearchIndexablesContractTest {

    @Test
    public void testRawColumns_matchContractIndexing() {
        assertThat(SearchIndexablesContract.RawData.COLUMN_RANK)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[0]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_TITLE)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[1]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_SUMMARY_ON)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[2]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_SUMMARY_OFF)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[3]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_ENTRIES)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[4]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_KEYWORDS)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[5]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_SCREEN_TITLE)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[6]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_CLASS_NAME)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[7]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_ICON_RESID)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[8]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_INTENT_ACTION)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[9]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_INTENT_TARGET_PACKAGE)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[10]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_INTENT_TARGET_CLASS)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[11]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_KEY)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[12]);
        assertThat(SearchIndexablesContract.RawData.COLUMN_USER_ID)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[13]);
        assertThat(SearchIndexablesContract.RawData.PAYLOAD_TYPE)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[14]);
        assertThat(SearchIndexablesContract.RawData.PAYLOAD)
                .isEqualTo(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[15]);
    }
}
