/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.dashboard;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.SettingsActivity;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.DashboardTile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SummaryLoader {
    private static final boolean DEBUG = DashboardSummary.DEBUG;
    private static final String TAG = "SummaryLoader";

    private final Activity mActivity;
    private final DashboardAdapter mAdapter;
    private final ArrayMap<SummaryProvider, DashboardTile> mSummaryMap = new ArrayMap<>();
    private final List<DashboardTile> mTiles = new ArrayList<>();

    public static final String SUMMARY_PROVIDER_FACTORY = "SUMMARY_PROVIDER_FACTORY";

    public SummaryLoader(Activity activity, DashboardAdapter adapter,
                  List<DashboardCategory> categories) {
        mActivity = activity;
        mAdapter = adapter;
        for (int i = 0; i < categories.size(); i++) {
            List<DashboardTile> tiles = categories.get(i).tiles;
            for (int j = 0; j < tiles.size(); j++) {
                DashboardTile tile = tiles.get(j);
                SummaryProvider provider = getSummaryProvider(tile);
                if (provider != null) {
                    mSummaryMap.put(provider, tile);
                }
            }
        }
    }

    public void setSummary(SummaryProvider provider, CharSequence summary) {
        DashboardTile tile = mSummaryMap.get(provider);
        tile.summary = summary;
        mAdapter.notifyChanged(tile);
    }

    public void setListening(boolean listening) {
        for (SummaryProvider provider : mSummaryMap.keySet()) {
            provider.setListening(listening);
        }
    }

    private SummaryProvider getSummaryProvider(DashboardTile tile) {
        if (!mActivity.getPackageName().equals(tile.intent.getComponent().getPackageName())) {
            // Not within Settings, can't load Summary directly.
            // TODO: Load summary indirectly.
            return null;
        }
        Bundle metaData = getMetaData(tile);
        if (metaData == null) {
            if (DEBUG) Log.d(TAG, "No metadata specified for " + tile.intent.getComponent());
            return null;
        }
        String clsName = metaData.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
        if (clsName == null) {
            if (DEBUG) Log.d(TAG, "No fragment specified for " + tile.intent.getComponent());
            return null;
        }
        try {
            Class<?> cls = Class.forName(clsName);
            Field field = cls.getField(SUMMARY_PROVIDER_FACTORY);
            SummaryProviderFactory factory = (SummaryProviderFactory) field.get(null);
            return factory.createSummaryProvider(mActivity, this);
        } catch (ClassNotFoundException e) {
            if (DEBUG) Log.d(TAG, "Couldn't find " + clsName, e);
        } catch (NoSuchFieldException e) {
            if (DEBUG) Log.d(TAG, "Couldn't find " + SUMMARY_PROVIDER_FACTORY, e);
        } catch (ClassCastException e) {
            if (DEBUG) Log.d(TAG, "Couldn't cast " + SUMMARY_PROVIDER_FACTORY, e);
        } catch (IllegalAccessException e) {
            if (DEBUG) Log.d(TAG, "Couldn't get " + SUMMARY_PROVIDER_FACTORY, e);
        }
        return null;
    }

    private Bundle getMetaData(DashboardTile tile) {
        // TODO: Cache this in TileUtils so this doesn't need to be loaded again.
        try {
            ActivityInfo activityInfo = mActivity.getPackageManager().getActivityInfo(
                    tile.intent.getComponent(), PackageManager.GET_META_DATA);
            return activityInfo.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public interface SummaryProvider {
        void setListening(boolean listening);
    }

    public interface SummaryProviderFactory {
        SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader);
    }
}
