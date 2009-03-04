/*
 * Copyright (C) 2009 Google Inc.
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

import android.app.LauncherActivity;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

/**
 * Displays a list of all activities matching the incoming {@link Intent.EXTRA_INTENT}
 * query, along with any applicable icons. 
 */
public class ActivityPicker extends LauncherActivity {
    
    @Override
    protected Intent getTargetIntent() {
        Intent intent = this.getIntent();
        Intent targetIntent = new Intent(Intent.ACTION_MAIN, null);
        targetIntent.addCategory(Intent.CATEGORY_DEFAULT);
        
        // Use a custom title for this dialog, if provided
        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            String title = intent.getStringExtra(Intent.EXTRA_TITLE);
            setTitle(title);
        }
        
        Parcelable parcel = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (parcel instanceof Intent) {
            targetIntent = (Intent) parcel;
        }
        
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = intentForPosition(position);
        setResult(RESULT_OK, intent);
        finish();
    }

}
