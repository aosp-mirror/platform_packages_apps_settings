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

import static android.Manifest.permission.READ_SEARCH_INDEXABLES;

import android.app.PendingIntent;
import android.app.slice.SliceManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
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
    Map<Uri, SliceData> mSliceWeakDataCache;
    @VisibleForTesting
    Map<Uri, SliceData> mSliceDataCache;

    public SettingsSliceProvider() {
        super(READ_SEARCH_INDEXABLES);
    }

    @Override
    public boolean onCreateSliceProvider() {
        mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(getContext());
        mSliceDataCache = new ConcurrentHashMap<>();
        mSliceWeakDataCache = new WeakHashMap<>();
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
    public void onSlicePinned(Uri sliceUri) {
        // Start warming the slice, we expect someone will want it soon.
        loadSliceInBackground(sliceUri);
    }

    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        mSliceDataCache.remove(sliceUri);
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

        SliceData cachedSliceData = mSliceWeakDataCache.get(sliceUri);
        if (cachedSliceData == null) {
            loadSliceInBackground(sliceUri);
            return getSliceStub(sliceUri);
        }

        // Remove the SliceData from the cache after it has been used to prevent a memory-leak.
        if (!mSliceDataCache.containsKey(sliceUri)) {
            mSliceWeakDataCache.remove(sliceUri);
        }
        return SliceBuilderUtils.buildSlice(getContext(), cachedSliceData);
    }

    /**
     * Get a list of all valid Uris based on the keys indexed in the Slices database.
     * <p>
     * This will return a list of {@link Uri uris} depending on {@param uri}, following:
     * 1. Authority & Full Path -> Only {@param uri}. It is only a prefix for itself.
     * 2. Authority & No path -> A list of authority/action/$KEY$, where
     * {@code $KEY$} is a list of all Slice-enabled keys for the authority.
     * 3. Authority & action path -> A list of authority/action/$KEY$, where
     * {@code $KEY$} is a list of all Slice-enabled keys for the authority.
     * 4. Empty authority & path -> A list of Uris with all keys for both supported authorities.
     * 5. Else -> Empty list.
     * <p>
     * Note that the authority will stay consistent with {@param uri}, and the list of valid Slice
     * keys depends on if the authority is {@link SettingsSlicesContract#AUTHORITY} or
     * {@link #SLICE_AUTHORITY}.
     *
     * @param uri The uri to look for descendants under.
     * @returns all valid Settings uris for which {@param uri} is a prefix.
     */
    @Override
    public Collection<Uri> onGetSliceDescendants(Uri uri) {
        final List<Uri> descendants = new ArrayList<>();
        final Pair<Boolean, String> pathData = SliceBuilderUtils.getPathData(uri);

        if (pathData != null) {
            // Uri has a full path and will not have any descendants.
            descendants.add(uri);
            return descendants;
        }

        final String authority = uri.getAuthority();
        final String pathPrefix = uri.getPath();
        final boolean isPathEmpty = pathPrefix.isEmpty();

        // No path nor authority. Return all possible Uris.
        if (isPathEmpty && TextUtils.isEmpty(authority)) {
            final List<String> platformKeys = mSlicesDatabaseAccessor.getSliceKeys(
                    true /* isPlatformSlice */);
            final List<String> oemKeys = mSlicesDatabaseAccessor.getSliceKeys(
                    false /* isPlatformSlice */);
            final List<Uri> allUris = buildUrisFromKeys(platformKeys,
                    SettingsSlicesContract.AUTHORITY);
            allUris.addAll(buildUrisFromKeys(oemKeys, SettingsSliceProvider.SLICE_AUTHORITY));

            return allUris;
        }

        // Path is anything but empty, "action", or "intent". Return empty list.
        if (!isPathEmpty
                && !TextUtils.equals(pathPrefix, "/" + SettingsSlicesContract.PATH_SETTING_ACTION)
                && !TextUtils.equals(pathPrefix,
                "/" + SettingsSlicesContract.PATH_SETTING_INTENT)) {
            // Invalid path prefix, there are no valid Uri descendants.
            return descendants;
        }

        // Can assume authority belongs to the provider. Return all Uris for the authority.
        final boolean isPlatformUri = TextUtils.equals(authority, SettingsSlicesContract.AUTHORITY);
        final List<String> keys = mSlicesDatabaseAccessor.getSliceKeys(isPlatformUri);
        return buildUrisFromKeys(keys, authority);
    }

    private List<Uri> buildUrisFromKeys(List<String> keys, String authority) {
        final List<Uri> descendants = new ArrayList<>();

        final Uri.Builder builder = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION);

        final String newUriPathPrefix = SettingsSlicesContract.PATH_SETTING_ACTION + "/";
        for (String key : keys) {
            builder.path(newUriPathPrefix + key);
            descendants.add(builder.build());
        }

        return descendants;
    }

    @VisibleForTesting
    void loadSlice(Uri uri) {
        long startBuildTime = System.currentTimeMillis();

        final SliceData sliceData = mSlicesDatabaseAccessor.getSliceDataFromUri(uri);
        List<Uri> pinnedSlices = getContext().getSystemService(
                SliceManager.class).getPinnedSlices();
        if (pinnedSlices.contains(uri)) {
            mSliceDataCache.put(uri, sliceData);
        }
        mSliceWeakDataCache.put(uri, sliceData);
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
        // TODO: Switch back to ListBuilder when slice loading states are fixed.
        return new Slice.Builder(uri).build();
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
                                new SliceAction(getIntent(Settings.ACTION_WIFI_SETTINGS),
                                        (IconCompat) null, null)))
                .build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
