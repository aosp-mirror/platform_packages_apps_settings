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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.widget.Filter;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.datetime.timezone.BaseTimeZoneAdapter.AdapterItem;
import com.android.settings.datetime.timezone.BaseTimeZoneAdapter.ItemViewHolder;
import com.android.settings.datetime.timezone.RegionSearchPicker.RegionItem;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import libcore.timezone.CountryZonesFinder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        RegionSearchPickerTest.ShadowBaseTimeZonePicker.class,
        RegionSearchPickerTest.ShadowFragment.class,
    }
)
public class RegionSearchPickerTest {

    @Test
    public void createAdapter_matchRegionName() {
        List<String> regionList = new ArrayList<>();
        regionList.add("US");
        CountryZonesFinder finder = mock(CountryZonesFinder.class);
        when(finder.lookupAllCountryIsoCodes()).thenReturn(regionList);

        RegionSearchPicker picker = new RegionSearchPicker();
        BaseTimeZoneAdapter adapter = picker.createAdapter(new TimeZoneData(finder));
        assertEquals(1, adapter.getItemCount());
        AdapterItem item = adapter.getDataItem(0);
        assertEquals("United States", item.getTitle().toString());
        assertThat(Arrays.asList(item.getSearchKeys())).contains("United States");
    }

    // Test RegionSearchPicker does not crash due to the wrong assumption that no view is clicked
    // before all views are updated and after internal data structure is updated for text filtering.
    // This test mocks the text filtering event and emit click event immediately
    // http://b/75322108
    @Test
    public void clickItemView_duringRegionSearch_shouldNotCrash() {
        List<String> regionList = new ArrayList<>();
        regionList.add("US");
        CountryZonesFinder finder = mock(CountryZonesFinder.class);
        when(finder.lookupAllCountryIsoCodes()).thenReturn(regionList);

        // Prepare the picker and adapter
        RegionSearchPicker picker = new RegionSearchPicker();
        BaseTimeZoneAdapter<RegionItem> adapter = picker.createAdapter(new TimeZoneData(finder));
        // Prepare and bind a new ItemViewHolder with United States
        ItemViewHolder viewHolder = (ItemViewHolder) adapter.onCreateViewHolder(
                new LinearLayout(RuntimeEnvironment.application), BaseTimeZoneAdapter.TYPE_ITEM);
        adapter.onBindViewHolder(viewHolder, 0);
        assertEquals(1, adapter.getItemCount());

        // Pretend to search for a unknown region and no result is found.
        FilterWrapper filterWrapper = new FilterWrapper(adapter.getFilter());
        filterWrapper.publishEmptyResult("Unknown region 1");

        // Assert that the adapter should have been updated with no item
        assertEquals(0, adapter.getItemCount());
        viewHolder.itemView.performClick(); // This should not crash
    }

    // FilterResults is a protected inner class. Use FilterWrapper to create an empty FilterResults
    // instance.
    private static class FilterWrapper extends Filter {

        private final BaseTimeZoneAdapter.ArrayFilter mFilter;

        private FilterWrapper(BaseTimeZoneAdapter.ArrayFilter filter) {
            mFilter = filter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            return null;
        }

        private void publishEmptyResult(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            filterResults.count = 0;
            filterResults.values = new ArrayList<>();
            publishResults(charSequence, filterResults);
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mFilter.publishResults(charSequence, filterResults);
        }
    }

    // Robolectric can't start android.app.Fragment with support library v4 resources. Pretend
    // the fragment has started, and provide the objects in context here.
    @Implements(BaseTimeZonePicker.class)
    public static class ShadowBaseTimeZonePicker extends ShadowFragment {

        @Implementation
        protected Locale getLocale() {
            return Locale.US;
        }
    }

    @Implements(Fragment.class)
    public static class ShadowFragment {

        private FragmentActivity mActivity = Robolectric.setupActivity(FragmentActivity.class);

        @Implementation
        public final FragmentActivity getActivity() {
            return mActivity;
        }
    }
}
