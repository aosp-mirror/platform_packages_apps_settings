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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZonePicker extends ListActivity {

    private ArrayAdapter<CharSequence> mFilterAdapter;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFilterAdapter = ArrayAdapter.createFromResource(this,
                R.array.timezone_filters, android.R.layout.simple_list_item_1);
        setListAdapter(mFilterAdapter);
    }
    
    protected void addItem(List<Map> data, String name, String zone) {
        HashMap temp = new HashMap();
        temp.put("title", name);
        temp.put("zone", zone);
        data.add(temp);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String filter = (String) mFilterAdapter.getItem(position);
        // If All is chosen, reset the filter
        if (filter.equals("All")) {
            filter = null;
        }
        Intent zoneList = new Intent();
        zoneList.setClass(this, ZoneList.class);
        zoneList.putExtra("filter", filter);
        
        startActivityForResult(zoneList, 0);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If subactivity has resulted in a timezone selection, close this act.
        if (resultCode == RESULT_OK) {
            finish();
        }
    }    
}
