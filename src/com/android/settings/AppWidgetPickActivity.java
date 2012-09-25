/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays a list of {@link AppWidgetProviderInfo} widgets, along with any
 * injected special widgets specified through
 * {@link AppWidgetManager#EXTRA_CUSTOM_INFO} and
 * {@link AppWidgetManager#EXTRA_CUSTOM_EXTRAS}.
 * <p>
 * When an installed {@link AppWidgetProviderInfo} is selected, this activity
 * will bind it to the given {@link AppWidgetManager#EXTRA_APPWIDGET_ID},
 * otherwise it will return the requested extras.
 */
public class AppWidgetPickActivity extends ActivityPicker {
    private static final String TAG = "AppWidgetPickActivity";
    private static final boolean LOGD = false;

    private PackageManager mPackageManager;
    private AppWidgetManager mAppWidgetManager;
    List<PickAdapter.Item> mItems;
    
    /**
     * The allocated {@link AppWidgetManager#EXTRA_APPWIDGET_ID} that this
     * activity is binding.
     */
    private int mAppWidgetId;

    // Enable testing launcher widgets in keyguard.  For testing purposes only.
    private final boolean mIgnoreFilter = false || SystemProperties.getBoolean(
            "ro.keyguard_ignore_filter", false);

    @Override
    public void onCreate(Bundle icicle) {
        mPackageManager = getPackageManager();
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        
        super.onCreate(icicle);
        
        // Set default return data
        setResultData(RESULT_CANCELED, null);
        
        // Read the appWidgetId passed our direction, otherwise bail if not found
        final Intent intent = getIntent();
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        } else {
            finish();
        }
    }

    /**
     * Create list entries for any custom widgets requested through
     * {@link AppWidgetManager#EXTRA_CUSTOM_INFO}.
     */
    void putCustomAppWidgets(List<PickAdapter.Item> items) {
        final Bundle extras = getIntent().getExtras();
        
        // get and validate the extras they gave us
        ArrayList<AppWidgetProviderInfo> customInfo = null;
        ArrayList<Bundle> customExtras = null;
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
        }

        if (LOGD) Log.d(TAG, "Using " + customInfo.size() + " custom items");
        putAppWidgetItems(customInfo, customExtras, items, 0, 0, true);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = getIntentForPosition(which);
        PickAdapter.Item item = (PickAdapter.Item) mItems.get(which);

        int result;
        if (item.extras != null) {
            // If these extras are present it's because this entry is custom.
            // Don't try to bind it, just pass it back to the app.
            setResultData(RESULT_OK, intent);
        } else {
            try {
                Bundle options = null;
                if (intent.getExtras() != null) {
                    options = intent.getExtras().getBundle(
                            AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
                }
                mAppWidgetManager.bindAppWidgetId(mAppWidgetId, intent.getComponent(), options);
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

    /**
     * Create list entries for the given {@link AppWidgetProviderInfo} widgets,
     * inserting extras if provided.
     */
    void putAppWidgetItems(List<AppWidgetProviderInfo> appWidgets,
            List<Bundle> customExtras, List<PickAdapter.Item> items, int categoryFilter,
            int featuresFilter, boolean ignoreFilters) {
        if (appWidgets == null) return;
        final int size = appWidgets.size();
        for (int i = 0; i < size; i++) {
            AppWidgetProviderInfo info = appWidgets.get(i);

            // We remove any widgets whose category isn't included in the filter
            if (!ignoreFilters && (info.widgetCategory & categoryFilter) == 0) {
                continue;
            }

            // We remove any widgets who don't have all the features in the features filter
            if (!ignoreFilters && (info.widgetFeatures & featuresFilter) != featuresFilter) {
                continue;
            }

            CharSequence label = info.label;
            Drawable icon = null;

            if (info.icon != 0) {
                try {
                    final Resources res = getResources();
                    final int density = res.getDisplayMetrics().densityDpi;
                    int iconDensity;
                    switch (density) {
                        case DisplayMetrics.DENSITY_MEDIUM:
                            iconDensity = DisplayMetrics.DENSITY_LOW;
                        case DisplayMetrics.DENSITY_TV:
                            iconDensity = DisplayMetrics.DENSITY_MEDIUM;
                        case DisplayMetrics.DENSITY_HIGH:
                            iconDensity = DisplayMetrics.DENSITY_MEDIUM;
                        case DisplayMetrics.DENSITY_XHIGH:
                            iconDensity = DisplayMetrics.DENSITY_HIGH;
                        case DisplayMetrics.DENSITY_XXHIGH:
                            iconDensity = DisplayMetrics.DENSITY_XHIGH;
                        default:
                            // The density is some abnormal value.  Return some other
                            // abnormal value that is a reasonable scaling of it.
                            iconDensity = (int)((density*0.75f)+.5f);
                    }
                    Resources packageResources = mPackageManager.
                            getResourcesForApplication(info.provider.getPackageName());
                    icon = packageResources.getDrawableForDensity(info.icon, iconDensity);
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                            + " for provider: " + info.provider);
                }
                if (icon == null) {
                    Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                            + " for provider: " + info.provider);
                }
            }
            
            PickAdapter.Item item = new PickAdapter.Item(this, label, icon);

            item.packageName = info.provider.getPackageName();
            item.className = info.provider.getClassName();
            
            if (customExtras != null) {
                item.extras = customExtras.get(i);
            }

            items.add(item);
        }
    }

    /**
     * Build and return list of items to be shown in dialog. This will mix both
     * installed {@link AppWidgetProviderInfo} and those provided through
     * {@link AppWidgetManager#EXTRA_CUSTOM_INFO}, sorting them alphabetically.
     */
    @Override
    protected List<PickAdapter.Item> getItems() {
        final Intent intent = getIntent();
        boolean sortCustomAppWidgets =
                intent.getBooleanExtra(AppWidgetManager.EXTRA_CUSTOM_SORT, true);

        List<PickAdapter.Item> items = new ArrayList<PickAdapter.Item>();

        int categoryFilter = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN;
        if (intent.getExtras().containsKey(AppWidgetManager.EXTRA_CATEGORY_FILTER)) {
            categoryFilter = intent.getExtras().getInt(AppWidgetManager.EXTRA_CATEGORY_FILTER);
        }

        // If not specified, we don't filter on any specific
        int featuresFilter = AppWidgetProviderInfo.WIDGET_FEATURES_NONE;
        if (intent.getExtras().containsKey(AppWidgetManager.EXTRA_FEATURES_FILTER)) {
            featuresFilter = intent.getExtras().getInt(AppWidgetManager.EXTRA_CATEGORY_FILTER);
        }

        putInstalledAppWidgets(items, categoryFilter, featuresFilter);

        // Sort all items together by label
        if (sortCustomAppWidgets) {
            putCustomAppWidgets(items);
        }
        Collections.sort(items, new Comparator<PickAdapter.Item>() {
            Collator mCollator = Collator.getInstance();

            public int compare(PickAdapter.Item lhs, PickAdapter.Item rhs) {
                return mCollator.compare(lhs.label, rhs.label);
            }
        });
        if (!sortCustomAppWidgets) {
            List<PickAdapter.Item> customItems = new ArrayList<PickAdapter.Item>();
            putCustomAppWidgets(customItems);
            items.addAll(customItems);
        }
        mItems = items;
        return items;
    }

    /**
     * Create list entries for installed {@link AppWidgetProviderInfo} widgets.
     */
    void putInstalledAppWidgets(List<PickAdapter.Item> items, int categoryFilter, int featuresFilter) {
        List<AppWidgetProviderInfo> installed = mAppWidgetManager.getInstalledProviders();
        putAppWidgetItems(installed, null, items, categoryFilter, featuresFilter, mIgnoreFilter );
    }

    /**
     * Convenience method for setting the result code and intent. This method
     * correctly injects the {@link AppWidgetManager#EXTRA_APPWIDGET_ID} that
     * most hosts expect returned.
     */
    void setResultData(int code, Intent intent) {
        Intent result = intent != null ? intent : new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(code, result);
    }
}
