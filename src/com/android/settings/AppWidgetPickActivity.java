/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.util.Log;

import java.text.Collator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AppWidgetPickActivity extends LauncherActivity
{
    private static final String TAG = "AppWidgetPickActivity";

    AppWidgetManager mAppWidgetManager;
    int mAppWidgetId;
    
    public AppWidgetPickActivity() {
        mAppWidgetManager = AppWidgetManager.getInstance(this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle extras = getIntent().getExtras();
        mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);

        setResultData(RESULT_CANCELED);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = intentForPosition(position);
        int result;
        try {
            mAppWidgetManager.bindAppWidgetId(mAppWidgetId, intent.getComponent());
            result = RESULT_OK;
        } catch (IllegalArgumentException e) {
            // This is thrown if they're already bound, or otherwise somehow
            // bogus.  Set the result to canceled, and exit.  The app *should*
            // clean up at this point.  We could pass the error along, but
            // it's not clear that that's useful -- the widget will simply not
            // appear.
            result = RESULT_CANCELED;
        }
        setResultData(result);
        finish();
    }
    
    @Override
    public List<ListItem> makeListItems() {
        List<AppWidgetProviderInfo> installed = mAppWidgetManager.getInstalledProviders();
        PackageManager pm = getPackageManager();

        Drawable defaultIcon = null;
        IconResizer resizer = new IconResizer();

        ArrayList<ListItem> result = new ArrayList();
        final int N = installed.size();
        for (int i=0; i<N; i++) {
            AppWidgetProviderInfo info = installed.get(i);

            LauncherActivity.ListItem item = new LauncherActivity.ListItem();
            item.packageName = info.provider.getPackageName();
            item.className = info.provider.getClassName();
            
            item.label = info.label;
            if (info.icon != 0) {
                Drawable d = pm.getDrawable( item.packageName, info.icon, null);
                if (d != null) {
                    item.icon = resizer.createIconThumbnail(d);
                } else {
                    Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                            + " for package: " + item.packageName);
                }
            }
            if (item.icon == null) {
                // (including error case above)
                if (defaultIcon == null) {
                    // TODO: Load standard icon.
                }
                item.icon = defaultIcon;
            }
            
            result.add(item);
        }

        Collections.sort(result, new Comparator<ListItem>() {
                Collator mCollator = Collator.getInstance();
                public int compare(ListItem lhs, ListItem rhs) {
                    return mCollator.compare(lhs.label, rhs.label);
                }
            });
        return result;
    }

    void setResultData(int code) {
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(code, result);
    }
}

