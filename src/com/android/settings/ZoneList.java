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

import android.app.AlarmManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * This activity displays a list of time zones that match a filter string
 * such as "Africa", "Europe", etc. Choosing an item from the list will set
 * the time zone. Pressing Back without choosing from the list will not 
 * result in a change in the time zone setting.
 */
public class ZoneList extends ListActivity {
    
    private static final String TAG = "ZoneList";
    private static final String KEY_ID = "id";
    private static final String KEY_DISPLAYNAME = "name";
    private static final String KEY_GMT = "gmt";
    private static final String KEY_OFFSET = "offset";
    private static final String XMLTAG_TIMEZONE = "timezone";

    private static final int HOURS_1 = 60 * 60000;
    private static final int HOURS_24 = 24 * HOURS_1;
    private static final int HOURS_HALF = HOURS_1 / 2;
    
    private static final int MENU_TIMEZONE = Menu.FIRST+1;
    private static final int MENU_ALPHABETICAL = Menu.FIRST;

    // Initial focus position
    private int mDefault;
    
    private boolean mSortedByTimezone;

    private SimpleAdapter mTimezoneSortedAdapter;
    private SimpleAdapter mAlphabeticalAdapter;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String[] from = new String[] {KEY_DISPLAYNAME, KEY_GMT};
        int[] to = new int[] {android.R.id.text1, android.R.id.text2};

        MyComparator comparator = new MyComparator(KEY_OFFSET);
        
        List<HashMap> timezoneSortedList = getZones();
        Collections.sort(timezoneSortedList, comparator);
        mTimezoneSortedAdapter = new SimpleAdapter(this,
                (List) timezoneSortedList,
                android.R.layout.simple_list_item_2,
                from,
                to);

        List<HashMap> alphabeticalList = new ArrayList<HashMap>(timezoneSortedList);
        comparator.setSortingKey(KEY_DISPLAYNAME);
        Collections.sort(alphabeticalList, comparator);
        mAlphabeticalAdapter = new SimpleAdapter(this,
                (List) alphabeticalList,
                android.R.layout.simple_list_item_2,
                from,
                to);
        
        // Sets the adapter
        setSorting(true);
        
        // If current timezone is in this list, move focus to it
        setSelection(mDefault);
        
        // Assume user may press Back
        setResult(RESULT_CANCELED);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ALPHABETICAL, 0, R.string.zone_list_menu_sort_alphabetically)
            .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, MENU_TIMEZONE, 0, R.string.zone_list_menu_sort_by_timezone)
            .setIcon(R.drawable.ic_menu_3d_globe);
        
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
     
        if (mSortedByTimezone) {
            menu.findItem(MENU_TIMEZONE).setVisible(false);
            menu.findItem(MENU_ALPHABETICAL).setVisible(true);
        } else {
            menu.findItem(MENU_TIMEZONE).setVisible(true);
            menu.findItem(MENU_ALPHABETICAL).setVisible(false);
        }

        return true;
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

    private void setSorting(boolean timezone) {
        setListAdapter(timezone ? mTimezoneSortedAdapter : mAlphabeticalAdapter);
        mSortedByTimezone = timezone;
    }
    
    private List<HashMap> getZones() {
        List<HashMap> myData = new ArrayList<HashMap>();
        long date = Calendar.getInstance().getTimeInMillis();
        try {
            XmlResourceParser xrp = getResources().getXml(R.xml.timezones);
            while (xrp.next() != XmlResourceParser.START_TAG)
                continue;
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                    if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                        return myData;
                    }
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String id = xrp.getAttributeValue(0);
                    String displayName = xrp.nextText();
                    addItem(myData, id, displayName, date);
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");
        }

        return myData;
    }

    protected void addItem(List<HashMap> myData, String id, String displayName, 
            long date) {
        HashMap map = new HashMap();
        map.put(KEY_ID, id);
        map.put(KEY_DISPLAYNAME, displayName);
        TimeZone tz = TimeZone.getTimeZone(id);
        int offset = tz.getOffset(date);
        int p = Math.abs(offset);
        StringBuilder name = new StringBuilder();
        name.append("GMT");
        
        if (offset < 0) {
            name.append('-');
        } else {
            name.append('+');
        }
        
        name.append(p / (HOURS_1));
        name.append(':');

        int min = p / 60000;
        min %= 60;

        if (min < 10) {
            name.append('0');
        }
        name.append(min);
        
        map.put(KEY_GMT, name.toString());
        map.put(KEY_OFFSET, offset);
        
        if (id.equals(TimeZone.getDefault().getID())) {
            mDefault = myData.size();
        }
        
        myData.add(map);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map map = (Map) l.getItemAtPosition(position);
        // Update the system timezone value
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone((String) map.get(KEY_ID));
        setResult(RESULT_OK);
        finish();
    }
    
    private static class MyComparator implements Comparator<HashMap> {
        private String mSortingKey; 
        
        public MyComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }
        
        public void setSortingKey(String sortingKey) {
            mSortingKey = sortingKey;
        }
        
        public int compare(HashMap map1, HashMap map2) {
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
