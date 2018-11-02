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

import android.app.slice.SliceManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Pair;

import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.location.LocationSliceBuilder;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.WifiSliceBuilder;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.SliceBroadcastRelay;
import com.android.settingslib.utils.ThreadUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;

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

    @VisibleForTesting
    SlicesDatabaseAccessor mSlicesDatabaseAccessor;

    @VisibleForTesting
    Map<Uri, SliceData> mSliceWeakDataCache;
    @VisibleForTesting
    Map<Uri, SliceData> mSliceDataCache;

    private final KeyValueListParser mParser;

    final Set<Uri> mRegisteredUris = new ArraySet<>();

    public SettingsSliceProvider() {
        super(READ_SEARCH_INDEXABLES);
        mParser = new KeyValueListParser(',');
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
        if (WifiSliceBuilder.WIFI_URI.equals(sliceUri)) {
            registerIntentToUri(WifiSliceBuilder.INTENT_FILTER, sliceUri);
            return;
        } else if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(sliceUri)) {
            registerIntentToUri(ZenModeSliceBuilder.INTENT_FILTER, sliceUri);
            return;
        } else if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(sliceUri)) {
            registerIntentToUri(BluetoothSliceBuilder.INTENT_FILTER, sliceUri);
            return;
        }

        // Start warming the slice, we expect someone will want it soon.
        loadSliceInBackground(sliceUri);
    }

    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        if (mRegisteredUris.contains(sliceUri)) {
            Log.d(TAG, "Unregistering uri broadcast relay: " + sliceUri);
            SliceBroadcastRelay.unregisterReceivers(getContext(), sliceUri);
            mRegisteredUris.remove(sliceUri);
        }
        mSliceDataCache.remove(sliceUri);
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            if (!ThreadUtils.isMainThread()) {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .permitAll()
                        .build());
            }
            final Set<String> blockedKeys = getBlockedKeys();
            final String key = sliceUri.getLastPathSegment();
            if (blockedKeys.contains(key)) {
                Log.e(TAG, "Requested blocked slice with Uri: " + sliceUri);
                return null;
            }

            // If adding a new Slice, do not directly match Slice URIs.
            // Use {@link SlicesDatabaseAccessor}.
            if (WifiCallingSliceHelper.WIFI_CALLING_URI.equals(sliceUri)) {
                return FeatureFactory.getFactory(getContext())
                        .getSlicesFeatureProvider()
                        .getNewWifiCallingSliceHelper(getContext())
                        .createWifiCallingSlice(sliceUri);
            } else if (WifiSliceBuilder.WIFI_URI.equals(sliceUri)) {
                return WifiSliceBuilder.getSlice(getContext());
            } else if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(sliceUri)) {
                return ZenModeSliceBuilder.getSlice(getContext());
            } else if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(sliceUri)) {
                return BluetoothSliceBuilder.getSlice(getContext());
            } else if (LocationSliceBuilder.LOCATION_URI.equals(sliceUri)) {
                return LocationSliceBuilder.getSlice(getContext());
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
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
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
            descendants.addAll(buildUrisFromKeys(platformKeys, SettingsSlicesContract.AUTHORITY));
            descendants.addAll(buildUrisFromKeys(oemKeys, SettingsSliceProvider.SLICE_AUTHORITY));
            descendants.addAll(getSpecialCaseUris(true /* isPlatformSlice */));
            descendants.addAll(getSpecialCaseUris(false /* isPlatformSlice */));

            return descendants;
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
        descendants.addAll(buildUrisFromKeys(keys, authority));
        descendants.addAll(getSpecialCaseUris(isPlatformUri));
        return descendants;
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

        final SliceData sliceData;
        try {
            sliceData = mSlicesDatabaseAccessor.getSliceDataFromUri(uri);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Could not get slice data for uri: " + uri, e);
            return;
        }

        final BasePreferenceController controller = SliceBuilderUtils.getPreferenceController(
                getContext(), sliceData);

        final IntentFilter filter = controller.getIntentFilter();
        if (filter != null) {
            registerIntentToUri(filter, uri);
        }

        final List<Uri> pinnedSlices = getContext().getSystemService(
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

    private List<Uri> getSpecialCaseUris(boolean isPlatformUri) {
        if (isPlatformUri) {
            return getSpecialCasePlatformUris();
        }
        return getSpecialCaseOemUris();
    }

    private List<Uri> getSpecialCasePlatformUris() {
        return Arrays.asList(
                WifiSliceBuilder.WIFI_URI,
                BluetoothSliceBuilder.BLUETOOTH_URI,
                LocationSliceBuilder.LOCATION_URI
        );
    }

    private List<Uri> getSpecialCaseOemUris() {
        return Arrays.asList(
                ZenModeSliceBuilder.ZEN_MODE_URI
        );
    }

    @VisibleForTesting
    /**
     * Registers an IntentFilter in SysUI to notify changes to {@param sliceUri} when broadcasts to
     * {@param intentFilter} happen.
     */
    void registerIntentToUri(IntentFilter intentFilter, Uri sliceUri) {
        Log.d(TAG, "Registering Uri for broadcast relay: " + sliceUri);
        mRegisteredUris.add(sliceUri);
        SliceBroadcastRelay.registerReceiver(getContext(), sliceUri, SliceRelayReceiver.class,
                intentFilter);
    }

    @VisibleForTesting
    Set<String> getBlockedKeys() {
        final String value = Settings.Global.getString(getContext().getContentResolver(),
                Settings.Global.BLOCKED_SLICES);
        final Set<String> set = new ArraySet<>();

        try {
            mParser.setString(value);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad Settings Slices Whitelist flags", e);
            return set;
        }

        final String[] parsedValues = parseStringArray(value);
        Collections.addAll(set, parsedValues);
        return set;
    }

    private String[] parseStringArray(String value) {
        if (value != null) {
            String[] parts = value.split(":");
            if (parts.length > 0) {
                return parts;
            }
        }
        return new String[0];
    }
}
