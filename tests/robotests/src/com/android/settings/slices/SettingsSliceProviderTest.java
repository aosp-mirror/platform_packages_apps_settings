/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.slices;

import static android.content.ContentResolver.SCHEME_CONTENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.slice.SliceManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.flashlight.FlashlightSliceBuilder;
import com.android.settings.location.LocationSliceBuilder;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.wifi.WifiSlice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO Investigate using ShadowContentResolver.registerProviderInternal(String, ContentProvider)
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowThreadUtils.class, ShadowUtils.class,
        SlicesDatabaseAccessorTest.ShadowApplicationPackageManager.class,
        ShadowBluetoothAdapter.class, ShadowLockPatternUtils.class})
public class SettingsSliceProviderTest {

    private static final String KEY = "KEY";
    private static final String INTENT_PATH =
            SettingsSlicesContract.PATH_SETTING_INTENT + "/" + KEY;
    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final String SCREEN_TITLE = "screen title";
    private static final String FRAGMENT_NAME = "fragment name";
    private static final int ICON = R.drawable.ic_settings;
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String PREF_CONTROLLER = FakeToggleController.class.getName();

    private Context mContext;
    private SettingsSliceProvider mProvider;
    @Mock
    private SliceManager mManager;

    private static final List<Uri> SPECIAL_CASE_PLATFORM_URIS = Arrays.asList(
            WifiSlice.WIFI_URI,
            BluetoothSliceBuilder.BLUETOOTH_URI,
            LocationSliceBuilder.LOCATION_URI
    );

    private static final List<Uri> SPECIAL_CASE_OEM_URIS = Arrays.asList(
            ZenModeSliceBuilder.ZEN_MODE_URI,
            FlashlightSliceBuilder.FLASHLIGHT_URI
    );

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        // Register the fake a11y Service
        ShadowAccessibilityManager shadowAccessibilityManager = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(AccessibilityManager.class));
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());

        mProvider = spy(new SettingsSliceProvider());
        ShadowStrictMode.reset();
        mProvider.mSliceWeakDataCache = new HashMap<>();
        mProvider.mSliceDataCache = new HashMap<>();
        mProvider.mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(mContext);
        mProvider.mCustomSliceManager = spy(new CustomSliceManager(mContext));
        when(mProvider.getContext()).thenReturn(mContext);

        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();

        doReturn(mManager).when(mContext).getSystemService(SliceManager.class);
        when(mManager.getPinnedSlices()).thenReturn(Collections.emptyList());

        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @After
    public void cleanUp() {
        ShadowThreadUtils.reset();
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInitialSliceReturned_emptySlice() {
        insertSpecialCase(KEY);
        final Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);
        Slice slice = mProvider.onBindSlice(uri);

        assertThat(slice.getUri()).isEqualTo(uri);
        assertThat(slice.getItems()).isEmpty();
    }

    @Test
    public void testLoadSlice_returnsSliceFromAccessor() {
        insertSpecialCase(KEY);
        final Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        mProvider.loadSlice(uri);
        SliceData data = mProvider.mSliceWeakDataCache.get(uri);

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
    }

    @Test
    public void loadSlice_registersIntentFilter() {
        insertSpecialCase(KEY);
        final Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        mProvider.loadSlice(uri);

        verify(mProvider).registerIntentToUri(eq(FakeToggleController.INTENT_FILTER), eq(uri));
    }

    @Test
    public void testLoadSlice_doesNotCacheWithoutPin() {
        insertSpecialCase(KEY);
        final Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        mProvider.loadSlice(uri);
        SliceData data = mProvider.mSliceDataCache.get(uri);

        assertThat(data).isNull();
    }

    @Test
    public void testLoadSlice_cachesWithPin() {
        insertSpecialCase(KEY);
        final Uri uri = SliceBuilderUtils.getUri(INTENT_PATH, false);
        when(mManager.getPinnedSlices()).thenReturn(Arrays.asList(uri));

        mProvider.loadSlice(uri);
        SliceData data = mProvider.mSliceDataCache.get(uri);

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
    }

    @Test
    public void testLoadSlice_cachedEntryRemovedOnBuild() {
        SliceData data = getDummyData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());
        insertSpecialCase(data.getKey());

        SliceData cachedData = mProvider.mSliceWeakDataCache.get(data.getUri());

        assertThat(cachedData).isNull();
    }

    @Test
    public void onBindSlice_mainThread_shouldNotOverrideStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        final StrictMode.ThreadPolicy oldThreadPolicy = StrictMode.getThreadPolicy();
        SliceData data = getDummyData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());

        final StrictMode.ThreadPolicy newThreadPolicy = StrictMode.getThreadPolicy();

        assertThat(newThreadPolicy.toString()).isEqualTo(oldThreadPolicy.toString());
    }

    @Test
    @Config(shadows = ShadowStrictMode.class)
    public void onBindSlice_backgroundThread_shouldOverrideStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);

        SliceData data = getDummyData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());

        assertThat(ShadowStrictMode.isThreadPolicyOverridden()).isTrue();
    }

    @Test
    public void onBindSlice_requestsBlockedSlice_returnsNull() {
        final String blockedKey = "blocked_key";
        final Set<String> blockedSet = new ArraySet<>();
        blockedSet.add(blockedKey);
        doReturn(blockedSet).when(mProvider).getBlockedKeys();
        final Uri blockedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(blockedKey)
                .build();

        final Slice slice = mProvider.onBindSlice(blockedUri);

        assertThat(slice).isNull();
    }

    @Test
    public void testLoadSlice_cachedEntryRemovedOnUnpin() {
        SliceData data = getDummyData();
        mProvider.mSliceDataCache.put(data.getUri(), data);
        mProvider.onSliceUnpinned(data.getUri());
        insertSpecialCase(data.getKey());

        SliceData cachedData = mProvider.mSliceWeakDataCache.get(data.getUri());

        assertThat(cachedData).isNull();
    }

    @Test
    public void getDescendantUris_fullActionUri_returnsSelf() {
        final Uri uri = SliceBuilderUtils.getUri(
                SettingsSlicesContract.PATH_SETTING_ACTION + "/key", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(uri);
    }

    @Test
    public void getDescendantUris_fullIntentUri_returnsSelf() {
        final Uri uri = SliceBuilderUtils.getUri(
                SettingsSlicesContract.PATH_SETTING_ACTION + "/key", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactly(uri);
    }

    @Test
    public void getDescendantUris_wrongPath_returnsEmpty() {
        final Uri uri = SliceBuilderUtils.getUri("invalid_path", true);

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_invalidPath_returnsEmpty() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath("invalid")
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);
        descendants.removeAll(SPECIAL_CASE_OEM_URIS);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_platformSlice_doesNotReturnOEMSlice() {
        insertSpecialCase("oem_key", false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);
        descendants.removeAll(SPECIAL_CASE_PLATFORM_URIS);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_doesNotReturnPlatformSlice() {
        insertSpecialCase("platform_key", true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);
        descendants.removeAll(SPECIAL_CASE_OEM_URIS);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_returnsOEMUriDescendant() {
        final String key = "oem_key";
        insertSpecialCase(key, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_OEM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void getDescendantUris_oemSliceNoPath_returnsOEMUriDescendant() {
        final String key = "oem_key";
        insertSpecialCase(key, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_OEM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void getDescendantUris_platformSlice_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_PLATFORM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void getDescendantUris_platformSliceNoPath_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        insertSpecialCase(key, true /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .build();
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_PLATFORM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void getDescendantUris_noAuthorityNorPath_returnsAllUris() {
        final String platformKey = "platform_key";
        final String oemKey = "oemKey";
        insertSpecialCase(platformKey, true /* isPlatformSlice */);
        insertSpecialCase(oemKey, false /* isPlatformSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .build();
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_PLATFORM_URIS);
        expectedUris.addAll(SPECIAL_CASE_OEM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(platformKey)
                .build());
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(oemKey)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void bindSlice_wifiSlice_returnsWifiSlice() {
        final Slice wifiSlice = mProvider.onBindSlice(WifiSlice.WIFI_URI);

        assertThat(wifiSlice.getUri()).isEqualTo(WifiSlice.WIFI_URI);
    }

    @Test
    public void bindSlice_flashlightSlice_returnsFlashlightSlice() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.FLASHLIGHT_AVAILABLE, 1);

        final Slice flashlightSlice = mProvider.onBindSlice(FlashlightSliceBuilder.FLASHLIGHT_URI);

        assertThat(flashlightSlice.getUri()).isEqualTo(FlashlightSliceBuilder.FLASHLIGHT_URI);
    }

    @Test
    public void onSlicePinned_noIntentRegistered_specialCaseUri_doesNotCrash() {
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(SettingsSlicesContract.KEY_LOCATION)
                .build();

        mProvider.onSlicePinned(uri);
    }

    private SliceBackgroundWorker initBackgroundWorker(Uri uri) {
        final SliceBackgroundWorker worker = spy(new SliceBackgroundWorker(
                mContext.getContentResolver(), uri) {
            @Override
            protected void onSlicePinned() {
            }

            @Override
            protected void onSliceUnpinned() {
            }

            @Override
            public void close() {
            }
        });
        final WifiSlice wifiSlice = spy(new WifiSlice(mContext));
        when(wifiSlice.getBackgroundWorker()).thenReturn(worker);
        when(mProvider.mCustomSliceManager.getSliceableFromUri(uri)).thenReturn(wifiSlice);
        return worker;
    }

    @Test
    public void onSlicePinned_backgroundWorker_started() {
        final Uri uri = WifiSlice.WIFI_URI;
        final SliceBackgroundWorker worker = initBackgroundWorker(uri);

        mProvider.onSlicePinned(uri);

        verify(worker).onSlicePinned();
    }

    @Test
    public void onSlicePinned_backgroundWorker_stopped() {
        final Uri uri = WifiSlice.WIFI_URI;
        final SliceBackgroundWorker worker = initBackgroundWorker(uri);

        mProvider.onSlicePinned(uri);
        mProvider.onSliceUnpinned(uri);

        verify(worker).onSliceUnpinned();
    }

    @Test
    public void shutdown_backgroundWorker_closed() throws IOException {
        final Uri uri = WifiSlice.WIFI_URI;
        final SliceBackgroundWorker worker = initBackgroundWorker(uri);

        mProvider.onSlicePinned(uri);
        mProvider.shutdown();

        verify(worker).close();
    }

    @Test
    public void grantWhitelistedPackagePermissions_noWhitelist_shouldNotGrant() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(Uri.parse("content://settings/slice"));

        mProvider.grantWhitelistedPackagePermissions(mContext, uris);

        verify(mManager, never()).grantSlicePermission(anyString(), any(Uri.class));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void grantWhitelistedPackagePermissions_hasPackageWhitelist_shouldGrant() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(Uri.parse("content://settings/slice"));

        mProvider.grantWhitelistedPackagePermissions(mContext, uris);

        verify(mManager)
                .grantSlicePermission("com.android.settings.slice_whitelist_package", uris.get(0));
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(key, true);
    }

    private void insertSpecialCase(String key, boolean isPlatformSlice) {
        final ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, R.drawable.ic_settings);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, "test");
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, PREF_CONTROLLER);
        values.put(SlicesDatabaseHelper.IndexColumns.PLATFORM_SLICE, isPlatformSlice);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_TYPE, SliceData.SliceType.INTENT);
        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        db.beginTransaction();
        try {
            db.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    private static SliceData getDummyData() {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .build();
    }

    @Implements(value = StrictMode.class)
    public static class ShadowStrictMode {

        private static int sSetThreadPolicyCount;

        @Resetter
        public static void reset() {
            sSetThreadPolicyCount = 0;
        }

        @Implementation
        public static void setThreadPolicy(final StrictMode.ThreadPolicy policy) {
            sSetThreadPolicyCount++;
        }

        public static boolean isThreadPolicyOverridden() {
            return sSetThreadPolicyCount != 0;
        }
    }
}