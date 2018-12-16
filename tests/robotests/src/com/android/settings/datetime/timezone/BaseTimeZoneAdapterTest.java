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

package com.android.settings.datetime.timezone;

import static com.google.common.truth.Truth.assertThat;

import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BaseTimeZoneAdapterTest {

    @Test
    public void testFilter() throws InterruptedException {
        TestItem US = new TestItem("United States");
        TestItem HK = new TestItem("Hong Kong");
        TestItem UK = new TestItem("United Kingdom", new String[] { "United Kingdom",
                "Great Britain"});
        TestItem secretCountry = new TestItem("no name", new String[] { "Secret"});
        List<TestItem> items = new ArrayList<>();
        items.add(US);
        items.add(HK);
        items.add(UK);
        items.add(secretCountry);

        TestTimeZoneAdapter adapter = new TestTimeZoneAdapter(items);
        assertSearch(adapter, "", items.toArray(new TestItem[0]));
        assertSearch(adapter, "Unit", US, UK);
        assertSearch(adapter, "kon", HK);
        assertSearch(adapter, "brit", UK);
        assertSearch(adapter, "sec", secretCountry);
    }

    private void assertSearch(TestTimeZoneAdapter adapter , String searchText, TestItem... items)
            throws InterruptedException {
        Observer observer = new Observer(adapter);
        adapter.getFilter().filter(searchText);
        observer.await();
        assertThat(adapter.getItemCount()).isEqualTo(items.length);
        for (int i = 0; i < items.length; i++) {
            assertThat(adapter.getDataItem(i)).isEqualTo(items[i]);
        }
    }

    private static class Observer extends AdapterDataObserver {

        private final CountDownLatch mLatch = new CountDownLatch(1);
        private final TestTimeZoneAdapter mAdapter;

        private Observer(TestTimeZoneAdapter adapter) {
            mAdapter = adapter;
            mAdapter.registerAdapterDataObserver(this);
        }

        @Override
        public void onChanged() {
            mAdapter.unregisterAdapterDataObserver(this);
            mLatch.countDown();
        }

        private void await() throws InterruptedException {
            mLatch.await(2L, TimeUnit.SECONDS);
        }
    }

    private static class TestTimeZoneAdapter extends BaseTimeZoneAdapter<TestItem> {

        private TestTimeZoneAdapter(List<TestItem> items) {
            super(items, position -> {}, Locale.US, false /* showItemSummary */,
                    null /* headerText */);
        }
    }

    private static class TestItem implements BaseTimeZoneAdapter.AdapterItem {

        private final String mTitle;
        private final String[] mSearchKeys;

        TestItem(String title) {
            this(title, new String[] { title });
        }

        TestItem(String title, String[] searchKeys) {
            mTitle = title;
            mSearchKeys = searchKeys;
        }

        @Override
        public CharSequence getTitle() {
            return mTitle;
        }

        @Override
        public CharSequence getSummary() {
            return null;
        }

        @Override
        public String getIconText() {
            return null;
        }

        @Override
        public String getCurrentTime() {
            return null;
        }

        @Override
        public long getItemId() {
            return 0;
        }

        @Override
        public String[] getSearchKeys() {
            return mSearchKeys;
        }
    }
}
