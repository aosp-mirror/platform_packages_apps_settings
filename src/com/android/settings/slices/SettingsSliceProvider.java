/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.slices;

import android.app.PendingIntent;
import android.app.slice.SliceManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.annotation.VisibleForTesting;
import android.support.v4.graphics.drawable.IconCompat;
import android.util.Log;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.WeakHashMap;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

/**
 * A {@link SliceProvider} for Settings to enabled inline results in system apps.
 *
 * <p>{@link SettingsSliceProvider} accepts a {@link Uri} with {@link #SLICE_AUTHORITY} and a
 * {@code String} key based on the setting intended to be changed. This provider builds a
 * {@link Slice} and responds to Slice actions through the database defined by
 * {@link SlicesDatabaseHelper}, whose data is written by {@link SlicesIndexer}.
 *
 * <p>When a {@link Slice} is requested, we start loading {@link SliceData} in the background and
 * return an stub {@link Slice} with the correct {@link Uri} immediately. In the background, the
 * data corresponding to the key in the {@link Uri} is read by {@link SlicesDatabaseAccessor}, and
 * the entire row is converted into a {@link SliceData}. Once complete, it is stored in
 * {@link #mSliceDataCache}, and then an update sent via the Slice framework to the Slice.
 * The {@link Slice} displayed by the Slice-presenter will re-query this Slice-provider and find
 * the {@link SliceData} cached to build the full {@link Slice}.
 *
 * <p>When an action is taken on that {@link Slice}, we receive the action in
 * {@link SliceBroadcastReceiver}, and use the
 * {@link com.android.settings.core.BasePreferenceController} indexed as
 * {@link SlicesDatabaseHelper.IndexColumns#CONTROLLER} to manipulate the setting.
 */
public class SettingsSliceProvider extends SliceProvider {

    private static final String TAG = "SettingsSliceProvider";

    /**
     * Authority for Settings slices not officially supported by the platform, but extensible for
     * OEMs.
     */
    public static final String SLICE_AUTHORITY = "com.android.settings.slices";

    public static final String PATH_WIFI = "wifi";
    public static final String ACTION_WIFI_CHANGED =
            "com.android.settings.slice.action.WIFI_CHANGED";

    /**
     * Action passed for changes to Toggle Slices.
     */
    public static final String ACTION_TOGGLE_CHANGED =
            "com.android.settings.slice.action.TOGGLE_CHANGED";

    /**
     * Action passed for changes to Slider Slices.
     */
    public static final String ACTION_SLIDER_CHANGED =
            "com.android.settings.slice.action.SLIDER_CHANGED";

    /**
     * Intent Extra passed for the key identifying the Setting Slice.
     */
    public static final String EXTRA_SLICE_KEY = "com.android.settings.slice.extra.key";

    /**
     * Boolean extra to indicate if the Slice is platform-defined.
     */
    public static final String EXTRA_SLICE_PLATFORM_DEFINED =
            "com.android.settings.slice.extra.platform";

    // TODO -- Associate slice URI with search result instead of separate hardcoded thing

    @VisibleForTesting
    SlicesDatabaseAccessor mSlicesDatabaseAccessor;

    @VisibleForTesting
    Map<Uri, SliceData> mSliceDataCache;

    @Override
    public boolean onCreateSliceProvider() {
        mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(getContext());
        mSliceDataCache = new WeakHashMap<>();
        return true;
    }

    @Override
    public Uri onMapIntentToUri(Intent intent) {
        try {
            return getContext().getSystemService(SliceManager.class).mapIntentToUri(
                    SliceDeepLinkSpringBoard.parse(
                            intent.getData(), getContext().getPackageName()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        // If adding a new Slice, do not directly match Slice URIs.
        // Use {@link SlicesDatabaseAccessor}.
        switch (path) {
            case "/" + PATH_WIFI:
                return createWifiSlice(sliceUri);
        }

        SliceData cachedSliceData = mSliceDataCache.get(sliceUri);
        if (cachedSliceData == null) {
            loadSliceInBackground(sliceUri);
            return getSliceStub(sliceUri);
        }

        // Remove the SliceData from the cache after it has been used to prevent a memory-leak.
        mSliceDataCache.remove(sliceUri);
        return SliceBuilderUtils.buildSlice(getContext(), cachedSliceData);
    }

    @VisibleForTesting
    void loadSlice(Uri uri) {
        long startBuildTime = System.currentTimeMillis();

        SliceData sliceData = mSlicesDatabaseAccessor.getSliceDataFromUri(uri);
        mSliceDataCache.put(uri, sliceData);
        getContext().getContentResolver().notifyChange(uri, null /* content observer */);

        Log.d(TAG, "Built slice (" + uri + ") in: " +
                (System.currentTimeMillis() - startBuildTime));
    }

    @VisibleForTesting
    void loadSliceInBackground(Uri uri) {
        ThreadUtils.postOnBackgroundThread(() -> {
            loadSlice(uri);
        });
    }

    /**
     * @return an empty {@link Slice} with {@param uri} to be used as a stub while the real
     * {@link SliceData} is loaded from {@link SlicesDatabaseHelper.Tables#TABLE_SLICES_INDEX}.
     */
    private Slice getSliceStub(Uri uri) {
        return new ListBuilder(getContext(), uri).build();
    }

    // TODO (b/70622039) remove this when the proper wifi slice is enabled.
    private Slice createWifiSlice(Uri sliceUri) {
        // Get wifi state
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        boolean wifiEnabled = false;
        String state;
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                state = getContext().getString(R.string.disconnected);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                state = wifiManager.getConnectionInfo().getSSID();
                wifiEnabled = true;
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                state = ""; // just don't show anything?
                break;
        }

        boolean finalWifiEnabled = wifiEnabled;
        return new ListBuilder(getContext(), sliceUri)
                .setColor(R.color.material_blue_500)
                .addRow(b -> b
                        .setTitle(getContext().getString(R.string.wifi_settings))
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.wifi_signal))
                        .setSubtitle(state)
                        .addEndItem(new SliceAction(getBroadcastIntent(ACTION_WIFI_CHANGED),
                                null, finalWifiEnabled))
                        .setPrimaryAction(
                                new SliceAction(getIntent(Intent.ACTION_MAIN),
                                        (IconCompat) null, null)))
                .build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
