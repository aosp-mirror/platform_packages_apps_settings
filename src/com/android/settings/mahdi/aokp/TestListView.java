/*
 * Copyright (C) 2013 XuiMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi.aokp;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TestListView extends DialogPreference {

       final Resources mRes;

       public TestListView(Context context, AttributeSet attrs) {
           super(context, attrs);
           mRes = context.getResources();
       }

       @Override
       protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
           final ListView list = new ListView(getContext());
           final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                             android.R.layout.simple_list_item_1, android.R.id.text1);
           adapter.add(mRes.getString(R.string.listview_test_instructions));
           final String itemText = mRes.getString(R.string.listview_test_item);
           for (int x = 1; x <= 200; x++) {
                adapter.add(itemText + " " + x);
           }
           list.setAdapter(adapter);
           builder.setView(list);
       }
}
