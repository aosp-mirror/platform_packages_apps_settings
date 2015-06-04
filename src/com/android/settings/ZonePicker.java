/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import libcore.icu.TimeZoneNames;

/**
 * The class displaying a list of time zones that match a filter string
 * such as "Africa", "Europe", etc. Choosing an item from the list will set
 * the time zone. Pressing Back without choosing from the list will not
 * result in a change in the time zone setting.
 */
public class ZonePicker extends ListFragment {
    private static final String TAG = "ZonePicker";

    public static interface ZoneSelectionListener {
        // You can add any argument if you really need it...
        public void onZoneSelected(TimeZone tz);
    }

    private static final String KEY_ID = "id";  // value: String
    private static final String KEY_DISPLAYNAME = "name";  // value: String
    private static final String KEY_GMT = "gmt";  // value: String
    private static final String KEY_OFFSET = "offset";  // value: int (Integer)
    private static final String XMLTAG_TIMEZONE = "timezone";

    private static final int HOURS_1 = 60 * 60000;

    private static final int MENU_TIMEZONE = Menu.FIRST+1;
    private static final int MENU_ALPHABETICAL = Menu.FIRST;

    private boolean mSortedByTimezone;

    private SimpleAdapter mTimezoneSortedAdapter;
    private SimpleAdapter mAlphabeticalAdapter;

    private ZoneSelectionListener mListener;

    /**
     * Constructs an adapter with TimeZone list. Sorted by TimeZone in default.
     *
     * @param sortedByName use Name for sorting the list.
     */
    public static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName) {
        return constructTimezoneAdapter(context, sortedByName,
                R.layout.date_time_setup_custom_list_item_2);
    }

    /**
     * Constructs an adapter with TimeZone list. Sorted by TimeZone in default.
     *
     * @param sortedByName use Name for sorting the list.
     */
    public static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName, int layoutId) {
        final String[] from = new String[] {KEY_DISPLAYNAME, KEY_GMT};
        final int[] to = new int[] {android.R.id.text1, android.R.id.text2};

        final String sortKey = (sortedByName ? KEY_DISPLAYNAME : KEY_OFFSET);
        final MyComparator comparator = new MyComparator(sortKey);
        final List<Map<String, Object>> sortedList = getZones(context);
        Collections.sort(sortedList, comparator);
        final SimpleAdapter adapter = new SimpleAdapter(context,
                sortedList,
                layoutId,
                from,
                to);

        return adapter;
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
            final String id = (String)map.get(KEY_ID);
            if (defaultId.equals(id)) {
                // If current timezone is in this list, move focus to it
                return i;
            }
        }
        return -1;
    }

    /**
     * @param item one of items in adapters. The adapter should be constructed by
     * {@link #constructTimezoneAdapter(Context, boolean)}.
     * @return TimeZone object corresponding to the item.
     */
    public static TimeZone obtainTimeZoneFromItem(Object item) {
        return TimeZone.getTimeZone((String)((Map<?, ?>)item).get(KEY_ID));
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.forcePrepareCustomPreferencesList(container, view, list, false);
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

    public void setZoneSelectionListener(ZoneSelectionListener listener) {
        mListener = listener;
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

    private static List<Map<String, Object>> getZones(Context context) {
        final Locale locale = Locale.getDefault();
        final Date now = new Date();

        // The display name chosen for each zone entry depends on whether the zone is one associated
        // with the country of the user's chosen locale. For "local" zones we prefer the "long name"
        // (e.g. "Europe/London" -> "British Summer Time" for people in the UK). For "non-local"
        // zones we prefer the exemplar location (e.g. "Europe/London" -> "London" for English
        // speakers from outside the UK). This heuristic is based on the fact that people are
        // typically familiar with their local timezones and exemplar locations don't always match
        // modern-day expectations for people living in the country covered. Large countries like
        // China that mostly use a single timezone (olson id: "Asia/Shanghai") may not live near
        // "Shanghai" and prefer the long name over the exemplar location. The only time we don't
        // follow this policy for local zones is when Android supplies multiple olson IDs to choose
        // from and the use of a zone's long name leads to ambiguity. For example, at the time of
        // writing Android lists 5 olson ids for Australia which collapse to 2 different zone names
        // in winter but 4 different zone names in summer. The ambiguity leads to the users
        // selecting the wrong olson ids.

        // Get the list of olson ids to display to the user.
        List<String> olsonIdsToDisplay = readTimezonesToDisplay(context);

        // Create a lookup of local zone IDs.
        Set<String> localZoneIds = new TreeSet<String>();
        for (String olsonId : TimeZoneNames.forLocale(locale)) {
            localZoneIds.add(olsonId);
        }

        // Work out whether the long names for the local entries that we would show by default would
        // be ambiguous.
        Set<String> localZoneNames = new TreeSet<String>();
        boolean localLongNamesAreAmbiguous = false;
        for (String olsonId : olsonIdsToDisplay) {
            if (localZoneIds.contains(olsonId)) {
                TimeZone tz = TimeZone.getTimeZone(olsonId);
                String zoneLongName = DateTimeSettings.getZoneLongName(locale, tz, now);
                boolean longNameIsUnique = localZoneNames.add(zoneLongName);
                if (!longNameIsUnique) {
                    localLongNamesAreAmbiguous = true;
                    break;
                }
            }
        }

        // Generate the list of zone entries to return.
        List<Map<String, Object>> zones = new ArrayList<Map<String, Object>>();
        for (String olsonId : olsonIdsToDisplay) {
            final TimeZone tz = TimeZone.getTimeZone(olsonId);
            // Exemplar location display is the default. The only time we intend to display the long
            // name is when the olsonId is local AND long names are not ambiguous.
            boolean isLocalZoneId = localZoneIds.contains(olsonId);
            boolean preferLongName = isLocalZoneId && !localLongNamesAreAmbiguous;
            String displayName = getZoneDisplayName(locale, tz, now, preferLongName);

            String gmtOffsetString = DateTimeSettings.getGmtOffsetString(locale, tz);
            int offsetMillis = tz.getOffset(now.getTime());
            Map<String, Object> displayEntry =
                    createDisplayEntry(tz, gmtOffsetString, displayName, offsetMillis);
            zones.add(displayEntry);
        }
        return zones;
    }

    private static Map<String, Object> createDisplayEntry(
            TimeZone tz, String gmtOffsetString, String displayName, int offsetMillis) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KEY_ID, tz.getID());
        map.put(KEY_DISPLAYNAME, displayName);
        map.put(KEY_GMT, gmtOffsetString);
        map.put(KEY_OFFSET, offsetMillis);
        return map;
    }

    private static List<String> readTimezonesToDisplay(Context context) {
        List<String> olsonIds = new ArrayList<String>();
        try (XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones)) {
            while (xrp.next() != XmlResourceParser.START_TAG) {
                continue;
            }
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                    if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                        return olsonIds;
                    }
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String olsonId = xrp.getAttributeValue(0);
                    olsonIds.add(olsonId);
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");
        }
        return olsonIds;
    }

    /**
     * Returns a name for the specific zone. If {@code preferLongName} is {@code true} then the
     * long display name for the timezone will be used, otherwise the exemplar location will be
     * preferred.
     */
    private static String getZoneDisplayName(Locale locale, TimeZone tz, Date now,
            boolean preferLongName) {
        String zoneNameString;
        if (preferLongName) {
            zoneNameString = DateTimeSettings.getZoneLongName(locale, tz, now);
        } else {
            zoneNameString = getZoneExemplarLocation(locale, tz);
            if (zoneNameString == null || zoneNameString.isEmpty()) {
                // getZoneExemplarLocation can return null.
                zoneNameString = DateTimeSettings.getZoneLongName(locale, tz, now);
            }
        }
        return zoneNameString;
    }

    private static String getZoneExemplarLocation(Locale locale, TimeZone tz) {
        return TimeZoneNames.getExemplarLocation(locale.toString(), tz.getID());
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        // Ignore extra clicks
        if (!isResumed()) return;
        final Map<?, ?> map = (Map<?, ?>)listView.getItemAtPosition(position);
        final String tzId = (String) map.get(KEY_ID);

        // Update the system timezone value
        final Activity activity = getActivity();
        final AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(tzId);
        final TimeZone tz = TimeZone.getTimeZone(tzId);
        if (mListener != null) {
            mListener.onZoneSelected(tz);
        } else {
            getActivity().onBackPressed();
        }
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
