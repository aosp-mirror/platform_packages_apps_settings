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

import android.provider.SearchIndexablesContract;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;


public class SearchIndexablesContractTest extends AndroidTestCase {
        @SmallTest
        public void testRawColumns_IncludesRank() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_RANK,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[0]);
        }

        @SmallTest
        public void testRawColumns_IncludesTitle() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_TITLE,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[1]);
        }

        @SmallTest
        public void testRawColumns_IncludesSummaryOn() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_SUMMARY_ON,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[2]);
        }

        @SmallTest
        public void testRawColumns_IncludesSummaryOff() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_SUMMARY_OFF,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[3]);
        }

        @SmallTest
        public void testRawColumns_IncludesEntries() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_ENTRIES,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[4]);
        }

        @SmallTest
        public void testRawColumns_IncludesKeywords() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_KEYWORDS,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[5]);
        }

        @SmallTest
        public void testRawColumns_IncludesScreenTitle() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_SCREEN_TITLE,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[6]);
        }

        @SmallTest
        public void testRawColumns_IncludesClassName() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_CLASS_NAME,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[7]);
        }

        @SmallTest
        public void testRawColumns_IncludesIcon() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_ICON_RESID,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[8]);
        }

        @SmallTest
        public void testRawColumns_IncludesIntentAction() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_INTENT_ACTION,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[9]);
        }

        @SmallTest
        public void testRawColumns_IncludesIntentTargetPackage() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_INTENT_TARGET_PACKAGE,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[10]);
        }

        @SmallTest
        public void testRawColumns_IncludesTargetClass() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_INTENT_TARGET_CLASS,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[11]);
        }

        @SmallTest
        public void testRawColumns_IncludesKey() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_KEY,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[12]);
        }

        @SmallTest
        public void testRawColumns_IncludesUserId() {
            assertEquals(SearchIndexablesContract.RawData.COLUMN_USER_ID,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[13]);
        }

        @SmallTest
        public void testRawColumns_IncludesPayloadType() {
            assertEquals(SearchIndexablesContract.RawData.PAYLOAD_TYPE,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[14]);
        }

        @SmallTest
        public void testRawColumns_IncludesPayload() {
            assertEquals(SearchIndexablesContract.RawData.PAYLOAD,
                    SearchIndexablesContract.INDEXABLES_RAW_COLUMNS[15]);
        }
}
