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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.lang.reflect.Field;
import java.util.List;

public class SummaryLoader {
    private static final boolean DEBUG = DashboardSummary.DEBUG;
    private static final String TAG = "SummaryLoader";

    public static final String SUMMARY_PROVIDER_FACTORY = "SUMMARY_PROVIDER_FACTORY";

    private final Activity mActivity;
    private final ArrayMap<SummaryProvider, ComponentName> mSummaryProviderMap = new ArrayMap<>();
    private final ArrayMap<String, CharSequence> mSummaryTextMap = new ArrayMap<>();
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private final String mCategoryKey;

    private final Worker mWorker;
    private final Handler mHandler;
    private final HandlerThread mWorkerThread;

    private SummaryConsumer mSummaryConsumer;
    private boolean mListening;
    private boolean mWorkerListening;
    private ArraySet<BroadcastReceiver> mReceivers = new ArraySet<>();

    public SummaryLoader(Activity activity, List<DashboardCategory> categories) {
        mDashboardFeatureProvider = FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity);
        mCategoryKey = null;
        mHandler = new Handler();
        mWorkerThread = new HandlerThread("SummaryLoader", Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        mWorker = new Worker(mWorkerThread.getLooper());
        mActivity = activity;
        for (int i = 0; i < categories.size(); i++) {
            List<Tile> tiles = categories.get(i).tiles;
            for (int j = 0; j < tiles.size(); j++) {
                Tile tile = tiles.get(j);
                mWorker.obtainMessage(Worker.MSG_GET_PROVIDER, tile).sendToTarget();
            }
        }
    }

    public SummaryLoader(Activity activity, String categoryKey) {
        mDashboardFeatureProvider = FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity);
        mCategoryKey = categoryKey;
        mHandler = new Handler();
        mWorkerThread = new HandlerThread("SummaryLoader", Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        mWorker = new Worker(mWorkerThread.getLooper());
        mActivity = activity;

        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(categoryKey);
        if (category == null || category.tiles == null) {
            return;
        }

        List<Tile> tiles = category.tiles;
        for (Tile tile : tiles) {
            mWorker.obtainMessage(Worker.MSG_GET_PROVIDER, tile).sendToTarget();
        }
    }

    public void release() {
        mWorkerThread.quitSafely();
        // Make sure we aren't listening.
        setListeningW(false);
    }

    public void setSummaryConsumer(SummaryConsumer summaryConsumer) {
        mSummaryConsumer = summaryConsumer;
    }

    public void setSummary(SummaryProvider provider, final CharSequence summary) {
        final ComponentName component = mSummaryProviderMap.get(provider);
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                final Tile tile = getTileFromCategory(
                        mDashboardFeatureProvider.getTilesForCategory(mCategoryKey), component);

                if (tile == null) {
                    if (DEBUG) {
                        Log.d(TAG, "Can't find tile for " + component);
                    }
                    return;
                }
                if (DEBUG) {
                    Log.d(TAG, "setSummary " + tile.title + " - " + summary);
                }

                updateSummaryIfNeeded(tile, summary);
            }
        });
    }

    @VisibleForTesting
    void updateSummaryIfNeeded(Tile tile, CharSequence summary) {
        if (TextUtils.equals(tile.summary, summary)) {
            if (DEBUG) {
                Log.d(TAG, "Summary doesn't change, skipping summary update for " + tile.title);
            }
            return;
        }
        mSummaryTextMap.put(mDashboardFeatureProvider.getDashboardKeyForTile(tile), summary);
        tile.summary = summary;
        if (mSummaryConsumer != null) {
            mSummaryConsumer.notifySummaryChanged(tile);
        } else {
            if (DEBUG) {
                Log.d(TAG, "SummaryConsumer is null, skipping summary update for "
                        + tile.title);
            }
        }
    }

    /**
     * Only call from the main thread.
     */
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        // Unregister listeners immediately.
        for (int i = 0; i < mReceivers.size(); i++) {
            mActivity.unregisterReceiver(mReceivers.valueAt(i));
        }
        mReceivers.clear();
        mWorker.removeMessages(Worker.MSG_SET_LISTENING);
        mWorker.obtainMessage(Worker.MSG_SET_LISTENING, listening ? 1 : 0, 0).sendToTarget();
    }

    private SummaryProvider getSummaryProvider(Tile tile) {
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

    private Bundle getMetaData(Tile tile) {
        return tile.metaData;
    }

    /**
     * Registers a receiver and automatically unregisters it when the activity is stopping.
     * This ensures that the receivers are unregistered immediately, since most summary loader
     * operations are asynchronous.
     */
    public void registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mListening) {
                    return;
                }
                mReceivers.add(receiver);
                mActivity.registerReceiver(receiver, filter);
            }
        });
    }

    /**
     * Updates all tile's summary to latest cached version. This is necessary to handle the case
     * where category is updated after summary change.
     */
    public void updateSummaryToCache(DashboardCategory category) {
        if (category == null) {
            return;
        }
        for (Tile tile : category.tiles) {
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (mSummaryTextMap.containsKey(key)) {
                tile.summary = mSummaryTextMap.get(key);
            }
        }
    }

    private synchronized void setListeningW(boolean listening) {
        if (mWorkerListening == listening) return;
        mWorkerListening = listening;
        if (DEBUG) Log.d(TAG, "Listening " + listening);
        for (SummaryProvider p : mSummaryProviderMap.keySet()) {
            try {
                p.setListening(listening);
            } catch (Exception e) {
                Log.d(TAG, "Problem in setListening", e);
            }
        }
    }

    private synchronized void makeProviderW(Tile tile) {
        SummaryProvider provider = getSummaryProvider(tile);
        if (provider != null) {
            if (DEBUG) Log.d(TAG, "Creating " + tile);
            mSummaryProviderMap.put(provider, tile.intent.getComponent());
        }
    }

    private Tile getTileFromCategory(DashboardCategory category, ComponentName component) {
        if (category == null || category.tiles == null) {
            return null;
        }
        final int tileCount = category.tiles.size();
        for (int j = 0; j < tileCount; j++) {
            final Tile tile = category.tiles.get(j);
            if (component.equals(tile.intent.getComponent())) {
                return tile;
            }
        }
        return null;
    }



    public interface SummaryProvider {
        void setListening(boolean listening);
    }

    public interface SummaryConsumer {
        void notifySummaryChanged(Tile tile);
    }

    public interface SummaryProviderFactory {
        SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader);
    }

    private class Worker extends Handler {
        private static final int MSG_GET_PROVIDER = 1;
        private static final int MSG_SET_LISTENING = 2;

        public Worker(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_PROVIDER:
                    Tile tile = (Tile) msg.obj;
                    makeProviderW(tile);
                    break;
                case MSG_SET_LISTENING:
                    boolean listening = msg.arg1 != 0;
                    setListeningW(listening);
                    break;
            }
        }
    }
}
