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

package com.android.settings.datetime;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.datetime.ZoneGetter;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * The class displaying a list of time zones that match a filter string
 * such as "Africa", "Europe", etc. Choosing an item from the list will set
 * the time zone. Pressing Back without choosing from the list will not
 * result in a change in the time zone setting.
 */
public class ZonePicker extends ListFragment implements Instrumentable {

    private static final int MENU_TIMEZONE = Menu.FIRST+1;
    private static final int MENU_ALPHABETICAL = Menu.FIRST;
    private final VisibilityLoggerMixin mVisibilityLoggerMixin =
            new VisibilityLoggerMixin(getMetricsCategory());

    private boolean mSortedByTimezone;

    private SimpleAdapter mTimezoneSortedAdapter;
    private SimpleAdapter mAlphabeticalAdapter;

    /**
     * Constructs an adapter with TimeZone list. Sorted by TimeZone in default.
     *
     * @param sortedByName use Name for sorting the list.
     */
    public static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName) {
        return constructTimezoneAdapter(context, sortedByName,
                R.layout.date_time_custom_list_item_2);
    }

    /**
     * Constructs an adapter with TimeZone list. Sorted by TimeZone in default.
     *
     * @param sortedByName use Name for sorting the list.
     */
    public static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName, int layoutId) {
        final String[] from = new String[] {
                ZoneGetter.KEY_DISPLAY_LABEL,
                ZoneGetter.KEY_OFFSET_LABEL
        };
        final int[] to = new int[] {android.R.id.text1, android.R.id.text2};

        final String sortKey = (sortedByName
                ? ZoneGetter.KEY_DISPLAY_LABEL
                : ZoneGetter.KEY_OFFSET);
        final MyComparator comparator = new MyComparator(sortKey);
        final List<Map<String, Object>> sortedList = ZoneGetter.getZonesList(context);
        Collections.sort(sortedList, comparator);
        final SimpleAdapter adapter = new SimpleAdapter(context,
                sortedList,
                layoutId,
                from,
                to);
        adapter.setViewBinder(new TimeZoneViewBinder());
        return adapter;
    }

    private static class TimeZoneViewBinder implements SimpleAdapter.ViewBinder {

        /**
         * Set the text to the given {@link CharSequence} as is, instead of calling toString, so
         * that additional information stored in the CharSequence is, like spans added to a
         * {@link android.text.SpannableString} are preserved.
         */
        @Override
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            TextView textView = (TextView) view;
            textView.setText((CharSequence) data);
            return true;
        }
    }

    /**
     * Searches {@link TimeZone} from the given {@link SimpleAdapter} object, and returns
     * the index for the TimeZone.
     *
     * @param adapter SimpleAdapter constructed by
     * {@link #constructTimezoneAdapter(Context, boolean)}.
     * @param tz TimeZone to be searched.
     * @return Index for the given TimeZone. -1 when there's no corresponding list item.
     * returned.
     */
    public static int getTimeZoneIndex(SimpleAdapter adapter, TimeZone tz) {
        final String defaultId = tz.getID();
        final int listSize = adapter.getCount();
        for (int i = 0; i < listSize; i++) {
            // Using HashMap<String, Object> induces unnecessary warning.
            final HashMap<?,?> map = (HashMap<?,?>)adapter.getItem(i);
            final String id = (String)map.get(ZoneGetter.KEY_ID);
            if (defaultId.equals(id)) {
                // If current timezone is in this list, move focus to it
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mVisibilityLoggerMixin.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZONE_PICKER;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mTimezoneSortedAdapter = constructTimezoneAdapter(activity, false);
        mAlphabeticalAdapter = constructTimezoneAdapter(activity, true);

        // Sets the adapter
        setSorting(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = view.findViewById(android.R.id.list);
        prepareCustomPreferencesList(list);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ALPHABETICAL, 0, R.string.zone_list_menu_sort_alphabetically)
            .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, MENU_TIMEZONE, 0, R.string.zone_list_menu_sort_by_timezone)
            .setIcon(R.drawable.ic_menu_3d_globe);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mSortedByTimezone) {
            menu.findItem(MENU_TIMEZONE).setVisible(false);
            menu.findItem(MENU_ALPHABETICAL).setVisible(true);
        } else {
            menu.findItem(MENU_TIMEZONE).setVisible(true);
            menu.findItem(MENU_ALPHABETICAL).setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mVisibilityLoggerMixin.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_TIMEZONE:
                setSorting(true);
                return true;

            case MENU_ALPHABETICAL:
                setSorting(false);
                return true;

            default:
                return false;
        }
    }

    static void prepareCustomPreferencesList(ListView list) {
        list.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        list.setClipToPadding(false);
        list.setDivider(null);
    }

    private void setSorting(boolean sortByTimezone) {
        final SimpleAdapter adapter =
                sortByTimezone ? mTimezoneSortedAdapter : mAlphabeticalAdapter;
        setListAdapter(adapter);
        mSortedByTimezone = sortByTimezone;
        final int defaultIndex = getTimeZoneIndex(adapter, TimeZone.getDefault());
        if (defaultIndex >= 0) {
            setSelection(defaultIndex);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        // Ignore extra clicks
        if (!isResumed()) return;
        final Map<?, ?> map = (Map<?, ?>)listView.getItemAtPosition(position);
        final String tzId = (String) map.get(ZoneGetter.KEY_ID);

        // Update the system timezone value
        final Activity activity = getActivity();
        final AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(tzId);

        getActivity().onBackPressed();

    }

    @Override
    public void onPause() {
        super.onPause();
        mVisibilityLoggerMixin.onPause();
    }

    private static class MyComparator implements Comparator<Map<?, ?>> {
        private String mSortingKey;

        public MyComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public void setSortingKey(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public int compare(Map<?, ?> map1, Map<?, ?> map2) {
            Object value1 = map1.get(mSortingKey);
            Object value2 = map2.get(mSortingKey);

            /*
             * This should never happen, but just in-case, put non-comparable
             * items at the end.
             */
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }

            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value != null) && (value instanceof Comparable);
        }
    }
}
