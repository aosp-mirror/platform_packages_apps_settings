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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.notification.VolumeSeekBarPreferenceController;
import com.android.settings.notification.zen.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.SliceBroadcastRelay;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

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
 * {@link #mSliceWeakDataCache}, and then an update sent via the Slice framework to the Slice.
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
     * Action passed for copy data for the Copyable Slices.
     */
    public static final String ACTION_COPY =
            "com.android.settings.slice.action.COPY";

    /**
     * Intent Extra passed for the key identifying the Setting Slice.
     */
    public static final String EXTRA_SLICE_KEY = "com.android.settings.slice.extra.key";

    /**
     * A list of custom slice uris that are supported publicly. This is a subset of slices defined
     * in {@link CustomSliceRegistry}. Things here are exposed publicly so all clients with proper
     * permission can use them.
     */
    private static final List<Uri> PUBLICLY_SUPPORTED_CUSTOM_SLICE_URIS =
            Arrays.asList(
                    CustomSliceRegistry.BLUETOOTH_URI,
                    CustomSliceRegistry.FLASHLIGHT_SLICE_URI,
                    CustomSliceRegistry.LOCATION_SLICE_URI,
                    CustomSliceRegistry.MOBILE_DATA_SLICE_URI,
                    CustomSliceRegistry.WIFI_CALLING_URI,
                    CustomSliceRegistry.WIFI_SLICE_URI,
                    CustomSliceRegistry.ZEN_MODE_SLICE_URI
            );

    private static final KeyValueListParser KEY_VALUE_LIST_PARSER = new KeyValueListParser(',');

    @VisibleForTesting
    SlicesDatabaseAccessor mSlicesDatabaseAccessor;

    @VisibleForTesting
    Map<Uri, SliceData> mSliceWeakDataCache;

    @VisibleForTesting
    final Map<Uri, SliceBackgroundWorker> mPinnedWorkers = new ArrayMap<>();

    private Boolean mNightMode;

    public SettingsSliceProvider() {
        super(READ_SEARCH_INDEXABLES);
    }

    @Override
    public boolean onCreateSliceProvider() {
        mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(getContext());
        mSliceWeakDataCache = new WeakHashMap<>();
        return true;
    }

    @Override
    public void onSlicePinned(Uri sliceUri) {
        if (CustomSliceRegistry.isValidUri(sliceUri)) {
            final Context context = getContext();
            final CustomSliceable sliceable = FeatureFactory.getFactory(context)
                    .getSlicesFeatureProvider().getSliceableFromUri(context, sliceUri);
            final IntentFilter filter = sliceable.getIntentFilter();
            if (filter != null) {
                registerIntentToUri(filter, sliceUri);
            }
            ThreadUtils.postOnMainThread(() -> startBackgroundWorker(sliceable, sliceUri));
            return;
        }

        if (CustomSliceRegistry.ZEN_MODE_SLICE_URI.equals(sliceUri)) {
            registerIntentToUri(ZenModeSliceBuilder.INTENT_FILTER, sliceUri);
            return;
        } else if (CustomSliceRegistry.BLUETOOTH_URI.equals(sliceUri)) {
            registerIntentToUri(BluetoothSliceBuilder.INTENT_FILTER, sliceUri);
            return;
        }

        // Start warming the slice, we expect someone will want it soon.
        loadSliceInBackground(sliceUri);
    }

    @Override
    public void onSliceUnpinned(Uri sliceUri) {
        final Context context = getContext();
        if (!VolumeSliceHelper.unregisterUri(context, sliceUri)) {
            SliceBroadcastRelay.unregisterReceivers(context, sliceUri);
        }
        ThreadUtils.postOnMainThread(() -> stopBackgroundWorker(sliceUri));
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

            final boolean nightMode = Utils.isNightMode(getContext());
            if (mNightMode == null) {
                mNightMode = nightMode;
                getContext().setTheme(R.style.Theme_SettingsBase);
            } else if (mNightMode != nightMode) {
                Log.d(TAG, "Night mode changed, reload theme");
                mNightMode = nightMode;
                getContext().getTheme().rebase();
            }

            // Before adding a slice to {@link CustomSliceManager}, please get approval
            // from the Settings team.
            if (CustomSliceRegistry.isValidUri(sliceUri)) {
                final Context context = getContext();
                return FeatureFactory.getFactory(context)
                        .getSlicesFeatureProvider().getSliceableFromUri(context, sliceUri)
                        .getSlice();
            }

            if (CustomSliceRegistry.WIFI_CALLING_URI.equals(sliceUri)) {
                return FeatureFactory.getFactory(getContext())
                        .getSlicesFeatureProvider()
                        .getNewWifiCallingSliceHelper(getContext())
                        .createWifiCallingSlice(sliceUri);
            } else if (CustomSliceRegistry.ZEN_MODE_SLICE_URI.equals(sliceUri)) {
                return ZenModeSliceBuilder.getSlice(getContext());
            } else if (CustomSliceRegistry.BLUETOOTH_URI.equals(sliceUri)) {
                return BluetoothSliceBuilder.getSlice(getContext());
            } else if (CustomSliceRegistry.ENHANCED_4G_SLICE_URI.equals(sliceUri)) {
                return FeatureFactory.getFactory(getContext())
                        .getSlicesFeatureProvider()
                        .getNewEnhanced4gLteSliceHelper(getContext())
                        .createEnhanced4gLteSlice(sliceUri);
            } else if (CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI.equals(sliceUri)) {
                return FeatureFactory.getFactory(getContext())
                        .getSlicesFeatureProvider()
                        .getNewWifiCallingSliceHelper(getContext())
                        .createWifiCallingPreferenceSlice(sliceUri);
            }

            final SliceData cachedSliceData = mSliceWeakDataCache.get(sliceUri);
            if (cachedSliceData == null) {
                loadSliceInBackground(sliceUri);
                return getSliceStub(sliceUri);
            }

            // Remove the SliceData from the cache after it has been used to prevent a memory-leak.
            if (!getPinnedSlices().contains(sliceUri)) {
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
        Uri finalUri = uri;

        if (isPrivateSlicesNeeded(finalUri)) {
            descendants.addAll(
                    mSlicesDatabaseAccessor.getSliceUris(finalUri.getAuthority(),
                            false /* isPublicSlice */));
            Log.d(TAG, "provide " + descendants.size() + " non-public slices");
            finalUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(finalUri.getAuthority())
                    .build();
        }

        final Pair<Boolean, String> pathData = SliceBuilderUtils.getPathData(finalUri);

        if (pathData != null) {
            // Uri has a full path and will not have any descendants.
            descendants.add(finalUri);
            return descendants;
        }

        final String authority = finalUri.getAuthority();
        final String path = finalUri.getPath();
        final boolean isPathEmpty = path.isEmpty();

        // Path is anything but empty, "action", or "intent". Return empty list.
        if (!isPathEmpty
                && !TextUtils.equals(path, "/" + SettingsSlicesContract.PATH_SETTING_ACTION)
                && !TextUtils.equals(path, "/" + SettingsSlicesContract.PATH_SETTING_INTENT)) {
            // Invalid path prefix, there are no valid Uri descendants.
            return descendants;
        }

        // Add all descendants from db with matching authority.
        descendants.addAll(mSlicesDatabaseAccessor.getSliceUris(authority, true /*isPublicSlice*/));

        if (isPathEmpty && TextUtils.isEmpty(authority)) {
            // No path nor authority. Return all possible Uris by adding all special slice uri
            descendants.addAll(PUBLICLY_SUPPORTED_CUSTOM_SLICE_URIS);
        } else {
            // Can assume authority belongs to the provider. Return all Uris for the authority.
            final List<Uri> customSlices = PUBLICLY_SUPPORTED_CUSTOM_SLICE_URIS.stream()
                    .filter(sliceUri -> TextUtils.equals(authority, sliceUri.getAuthority()))
                    .collect(Collectors.toList());
            descendants.addAll(customSlices);
        }
        grantWhitelistedPackagePermissions(getContext(), descendants);
        return descendants;
    }

    @Nullable
    @Override
    public PendingIntent onCreatePermissionRequest(@NonNull Uri sliceUri,
            @NonNull String callingPackage) {
        final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS)
                .setPackage(Utils.SETTINGS_PACKAGE_NAME);
        final PendingIntent noOpIntent = PendingIntent.getActivity(getContext(),
                0 /* requestCode */, settingsIntent, 0 /* flags */);
        return noOpIntent;
    }

    @VisibleForTesting
    static void grantWhitelistedPackagePermissions(Context context, List<Uri> descendants) {
        if (descendants == null) {
            Log.d(TAG, "No descendants to grant permission with, skipping.");
        }
        final String[] whitelistPackages =
                context.getResources().getStringArray(R.array.slice_whitelist_package_names);
        if (whitelistPackages == null || whitelistPackages.length == 0) {
            Log.d(TAG, "No packages to whitelist, skipping.");
            return;
        } else {
            Log.d(TAG, String.format(
                    "Whitelisting %d uris to %d pkgs.",
                    descendants.size(), whitelistPackages.length));
        }
        final SliceManager sliceManager = context.getSystemService(SliceManager.class);
        for (Uri descendant : descendants) {
            for (String toPackage : whitelistPackages) {
                sliceManager.grantSlicePermission(toPackage, descendant);
            }
        }
    }

    @Override
    public void shutdown() {
        ThreadUtils.postOnMainThread(() -> {
            SliceBackgroundWorker.shutdown();
        });
    }

    @VisibleForTesting
    void loadSlice(Uri uri) {
        long startBuildTime = System.currentTimeMillis();

        final SliceData sliceData;
        try {
            sliceData = mSlicesDatabaseAccessor.getSliceDataFromUri(uri);
        } catch (IllegalStateException e) {
            Log.d(TAG, "Could not create slicedata for uri: " + uri, e);
            return;
        }

        final BasePreferenceController controller = SliceBuilderUtils.getPreferenceController(
                getContext(), sliceData);

        final IntentFilter filter = controller.getIntentFilter();
        if (filter != null) {
            if (controller instanceof VolumeSeekBarPreferenceController) {
                // Register volume slices to a broadcast relay to reduce unnecessary UI updates
                VolumeSliceHelper.registerIntentToUri(getContext(), filter, uri,
                        ((VolumeSeekBarPreferenceController) controller).getAudioStream());
            } else {
                registerIntentToUri(filter, uri);
            }
        }

        ThreadUtils.postOnMainThread(() -> startBackgroundWorker(controller, uri));

        mSliceWeakDataCache.put(uri, sliceData);
        getContext().getContentResolver().notifyChange(uri, null /* content observer */);

        Log.d(TAG, "Built slice (" + uri + ") in: " +
                (System.currentTimeMillis() - startBuildTime));
    }

    @VisibleForTesting
    void loadSliceInBackground(Uri uri) {
        ThreadUtils.postOnBackgroundThread(() -> loadSlice(uri));
    }

    @VisibleForTesting
    /**
     * Registers an IntentFilter in SysUI to notify changes to {@param sliceUri} when broadcasts to
     * {@param intentFilter} happen.
     */
    void registerIntentToUri(IntentFilter intentFilter, Uri sliceUri) {
        SliceBroadcastRelay.registerReceiver(getContext(), sliceUri, SliceRelayReceiver.class,
                intentFilter);
    }

    @VisibleForTesting
    Set<String> getBlockedKeys() {
        final String value = Settings.Global.getString(getContext().getContentResolver(),
                Settings.Global.BLOCKED_SLICES);
        final Set<String> set = new ArraySet<>();

        try {
            KEY_VALUE_LIST_PARSER.setString(value);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad Settings Slices Whitelist flags", e);
            return set;
        }

        final String[] parsedValues = parseStringArray(value);
        Collections.addAll(set, parsedValues);
        return set;
    }

    @VisibleForTesting
    boolean isPrivateSlicesNeeded(Uri uri) {
        final String queryUri = getContext().getString(R.string.config_non_public_slice_query_uri);

        if (!TextUtils.isEmpty(queryUri) && TextUtils.equals(uri.toString(), queryUri)) {
            // check if the calling package is eligible for private slices
            final int callingUid = Binder.getCallingUid();
            final boolean hasPermission = getContext().checkPermission(
                    android.Manifest.permission.READ_SEARCH_INDEXABLES, Binder.getCallingPid(),
                    callingUid) == PackageManager.PERMISSION_GRANTED;
            final String callingPackage = getContext().getPackageManager()
                    .getPackagesForUid(callingUid)[0];
            return hasPermission && TextUtils.equals(callingPackage,
                    getContext().getString(R.string.config_settingsintelligence_package_name));
        }
        return false;
    }

    private void startBackgroundWorker(Sliceable sliceable, Uri uri) {
        final Class workerClass = sliceable.getBackgroundWorkerClass();
        if (workerClass == null) {
            return;
        }

        if (mPinnedWorkers.containsKey(uri)) {
            return;
        }

        Log.d(TAG, "Starting background worker for: " + uri);
        final SliceBackgroundWorker worker = SliceBackgroundWorker.getInstance(
                getContext(), sliceable, uri);
        mPinnedWorkers.put(uri, worker);
        worker.pin();
    }

    private void stopBackgroundWorker(Uri uri) {
        final SliceBackgroundWorker worker = mPinnedWorkers.get(uri);
        if (worker != null) {
            Log.d(TAG, "Stopping background worker for: " + uri);
            worker.unpin();
            mPinnedWorkers.remove(uri);
        }
    }

    /**
     * @return an empty {@link Slice} with {@param uri} to be used as a stub while the real
     * {@link SliceData} is loaded from {@link SlicesDatabaseHelper.Tables#TABLE_SLICES_INDEX}.
     */
    private static Slice getSliceStub(Uri uri) {
        // TODO: Switch back to ListBuilder when slice loading states are fixed.
        return new Slice.Builder(uri).build();
    }

    private static String[] parseStringArray(String value) {
        if (value != null) {
            String[] parts = value.split(":");
            if (parts.length > 0) {
                return parts;
            }
        }
        return new String[0];
    }
}
