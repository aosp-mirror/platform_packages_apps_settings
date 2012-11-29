/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppWidgetLoader<Item extends AppWidgetLoader.LabelledItem> {
    private static final String TAG = "AppWidgetAdapter";
    private static final boolean LOGD = AppWidgetPickActivity.LOGD;

    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    ItemConstructor<Item> mItemConstructor;

    interface LabelledItem {
        CharSequence getLabel();
    }

    public AppWidgetLoader(Context context, AppWidgetManager appWidgetManager,
            ItemConstructor<Item> itemConstructor) {
        mContext = context;
        mAppWidgetManager = appWidgetManager;
        mItemConstructor = itemConstructor;
    }

    /**
     * Create list entries for any custom widgets requested through
     * {@link AppWidgetManager#EXTRA_CUSTOM_INFO}.
     */
    void putCustomAppWidgets(List<Item> items, Intent intent) {
        // get and validate the extras they gave us
        ArrayList<AppWidgetProviderInfo> customInfo = null;
        ArrayList<Bundle> customExtras = null;
        try_custom_items: {
            customInfo = intent.getParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO);
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

            customExtras = intent.getParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS);
            if (customExtras == null) {
                customInfo = null;
                Log.e(TAG, "EXTRA_CUSTOM_INFO without EXTRA_CUSTOM_EXTRAS");
                break try_custom_items;
            }

            int customExtrasSize = customExtras.size();
            if (customInfoSize != customExtrasSize) {
                customInfo = null;
                customExtras = null;
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
        }

        if (LOGD) Log.d(TAG, "Using " + customInfo.size() + " custom items");
        putAppWidgetItems(customInfo, customExtras, items, 0, true);
    }


    /**
     * Create list entries for the given {@link AppWidgetProviderInfo} widgets,
     * inserting extras if provided.
     */
    void putAppWidgetItems(List<AppWidgetProviderInfo> appWidgets,
            List<Bundle> customExtras, List<Item> items, int categoryFilter,
            boolean ignoreFilter) {
        if (appWidgets == null) return;
        final int size = appWidgets.size();
        for (int i = 0; i < size; i++) {
            AppWidgetProviderInfo info = appWidgets.get(i);

            // We remove any widgets whose category isn't included in the filter
            if (!ignoreFilter && (info.widgetCategory & categoryFilter) == 0) {
                continue;
            }

            Item item = mItemConstructor.createItem(mContext, info,
                    customExtras != null ? customExtras.get(i) : null);

            items.add(item);
        }
    }

    public interface ItemConstructor<Item> {
        Item createItem(Context context, AppWidgetProviderInfo info, Bundle extras);
    }


    /**
     * Build and return list of items to be shown in dialog. This will mix both
     * installed {@link AppWidgetProviderInfo} and those provided through
     * {@link AppWidgetManager#EXTRA_CUSTOM_INFO}, sorting them alphabetically.
     */
    protected List<Item> getItems(Intent intent) {
        boolean sortCustomAppWidgets =
                intent.getBooleanExtra(AppWidgetManager.EXTRA_CUSTOM_SORT, true);

        List<Item> items = new ArrayList<Item>();

        // Default category is home screen
        int categoryFilter = intent.getIntExtra(AppWidgetManager.EXTRA_CATEGORY_FILTER,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);

        putInstalledAppWidgets(items, categoryFilter);

        // Sort all items together by label
        if (sortCustomAppWidgets) {
            putCustomAppWidgets(items, intent);
        }
        Collections.sort(items, new Comparator<Item>() {
            Collator mCollator = Collator.getInstance();

            public int compare(Item lhs, Item rhs) {
                return mCollator.compare(lhs.getLabel(), rhs.getLabel());
            }
        });
        if (!sortCustomAppWidgets) {
            List<Item> customItems = new ArrayList<Item>();
            putCustomAppWidgets(customItems, intent);
            items.addAll(customItems);
        }
        return items;
    }

    /**
     * Create list entries for installed {@link AppWidgetProviderInfo} widgets.
     */
    void putInstalledAppWidgets(List<Item> items, int categoryFilter) {
        List<AppWidgetProviderInfo> installed =
                mAppWidgetManager.getInstalledProviders(categoryFilter);
        putAppWidgetItems(installed, null, items, categoryFilter, false);
    }
}
