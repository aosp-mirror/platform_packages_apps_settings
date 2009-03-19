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
import android.os.Parcelable;
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
    ArrayList mCustomInfo;
    ArrayList mCustomExtras;
    Drawable mDefaultIcon = null;
    
    public AppWidgetPickActivity() {
        mAppWidgetManager = AppWidgetManager.getInstance(this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            setResultData(RESULT_CANCELED, null);
            finish();
        }

        mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);

        // get and validate the extras they gave us
        ArrayList<Parcelable> customInfo = null;
        ArrayList<AppWidgetProviderInfo> customExtras = null;
        try_custom_items: {
            customInfo = extras.getParcelableArrayList(AppWidgetManager.EXTRA_CUSTOM_INFO);
            if (customInfo == null || customInfo.size() == 0) {
                Log.i(TAG, "EXTRA_CUSTOM_INFO not present.");
                break try_custom_items;
            }

            int customInfoSize = customInfo.size();
            for (int i=0; i<customInfoSize; i++) {
                Parcelable p = customInfo.get(i);
                if (p == null || !(p instanceof AppWidgetProviderInfo)) {
                    customInfo = null;
                    Log.e(TAG, "error using EXTRA_CUSTOM_INFO index=" + i);
                    break try_custom_items;
                }
            }

            customExtras = extras.getParcelableArrayList(AppWidgetManager.EXTRA_CUSTOM_EXTRAS);
            if (customExtras == null) {
                customInfo = null;
                Log.e(TAG, "EXTRA_CUSTOM_INFO without EXTRA_CUSTOM_EXTRAS");
                break try_custom_items;
            }

            int customExtrasSize = customExtras.size();
            if (customInfoSize != customExtrasSize) {
                Log.e(TAG, "list size mismatch: EXTRA_CUSTOM_INFO: " + customInfoSize
                        + " EXTRA_CUSTOM_EXTRAS: " + customExtrasSize);
                break try_custom_items;
            }


            for (int i=0; i<customExtrasSize; i++) {
                Parcelable p = customExtras.get(i);
                if (p == null || !(p instanceof Bundle)) {
                    customInfo = null;
                    customExtras = null;
                    Log.e(TAG, "error using EXTRA_CUSTOM_EXTRAS index=" + i);
                    break try_custom_items;
                }
            }

            mCustomInfo = customInfo;
            mCustomExtras = customExtras;
        }

        // After the stuff with mCustomInfo
        super.onCreate(icicle);

        setResultData(RESULT_CANCELED, null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = intentForPosition(position);
        int result;
        if (intent.getExtras() != null) {
            // If there are any extras, it's because this entry is custom.
            // Don't try to bind it, just pass it back to the app.
            setResultData(RESULT_OK, intent);
        } else {
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
            setResultData(result, null);
        }
        finish();
    }

    void makeItems(List<AppWidgetProviderInfo> items, ArrayList<Bundle> extras,
            ArrayList<ListItem> result, IconResizer resizer, PackageManager pm) {
        final int N = items.size();
        for (int i=0; i<N; i++) {
            AppWidgetProviderInfo info = items.get(i);

            LauncherActivity.ListItem item = new LauncherActivity.ListItem();
            item.packageName = info.provider.getPackageName();
            item.className = info.provider.getClassName();
            if (extras != null) {
                item.extras = extras.get(i);
            }
            item.label = info.label;
            if (info.icon != 0) {
                Drawable d = pm.getDrawable(item.packageName, info.icon, null);
                if (d != null) {
                    item.icon = resizer.createIconThumbnail(d);
                } else {
                    Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                            + " for package: " + item.packageName);
                }
            }
            if (item.icon == null) {
                // (including error case above)
                if (mDefaultIcon == null) {
                    // TODO: Load standard icon.
                }
                item.icon = mDefaultIcon;
            }
            
            result.add(item);
        }
    }
    
    @Override
    public List<ListItem> makeListItems() {
        List<AppWidgetProviderInfo> installed = mAppWidgetManager.getInstalledProviders();
        PackageManager pm = getPackageManager();

        IconResizer resizer = new IconResizer();
        ArrayList<ListItem> result = new ArrayList();

        // the ones from the package manager
        makeItems(installed, null, result, resizer, pm);

        // the ones provided in the intent we were launched with
        if (mCustomInfo != null) {
            Log.d(TAG, "Using " + mCustomInfo.size() + " custom items");
            makeItems(mCustomInfo, mCustomExtras, result, resizer, pm);
        }

        // sort the results by name
        Collections.sort(result, new Comparator<ListItem>() {
                Collator mCollator = Collator.getInstance();
                public int compare(ListItem lhs, ListItem rhs) {
                    return mCollator.compare(lhs.label, rhs.label);
                }
            });

        return result;
    }

    void setResultData(int code, Intent intent) {
        Intent result = intent != null ? intent : new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(code, result);
    }
}

