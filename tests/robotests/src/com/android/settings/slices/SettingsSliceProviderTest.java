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
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.content.res.Configuration.UI_MODE_NIGHT_NO;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.app.slice.SliceManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.Utils;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowBinder;
import org.robolectric.shadows.ShadowPackageManager;

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
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowUtils.class,
        SlicesDatabaseAccessorTest.ShadowApplicationPackageManager.class,
        ShadowBluetoothAdapter.class, ShadowLockPatternUtils.class,
        SettingsSliceProviderTest.ShadowTheme.class})
public class SettingsSliceProviderTest {

    private static final String KEY = "KEY";
    private static final Uri INTENT_SLICE_URI =
            new Uri.Builder().scheme(SCHEME_CONTENT)
                    .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                    .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
                    .appendPath(KEY)
                    .build();
    private static final Uri ACTION_SLICE_URI =
            new Uri.Builder().scheme(SCHEME_CONTENT)
                    .authority(SettingsSlicesContract.AUTHORITY)
                    .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                    .appendPath(KEY)
                    .build();

    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");

    private Context mContext;
    private SettingsSliceProvider mProvider;
    private ShadowPackageManager mPackageManager;
    private ShadowUserManager mShadowUserManager;

    @Mock
    private SliceManager mManager;

    private static final List<Uri> SPECIAL_CASE_PLATFORM_URIS = Arrays.asList(
            CustomSliceRegistry.WIFI_SLICE_URI,
            CustomSliceRegistry.BLUETOOTH_URI,
            CustomSliceRegistry.LOCATION_SLICE_URI
    );

    private static final List<Uri> SPECIAL_CASE_OEM_URIS = Arrays.asList(
            CustomSliceRegistry.ZEN_MODE_SLICE_URI,
            CustomSliceRegistry.FLASHLIGHT_SLICE_URI,
            CustomSliceRegistry.MOBILE_DATA_SLICE_URI,
            CustomSliceRegistry.WIFI_CALLING_URI
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
        mProvider.mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(mContext);
        when(mProvider.getContext()).thenReturn(mContext);

        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();

        doReturn(mManager).when(mContext).getSystemService(SliceManager.class);
        when(mManager.getPinnedSlices()).thenReturn(Collections.emptyList());

        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mShadowUserManager = ShadowUserManager.getShadow();

        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @After
    public void cleanUp() {
        ShadowThreadUtils.reset();
        ShadowTheme.reset();
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInitialSliceReturned_emptySlice() {
        SliceTestUtils.insertSliceToDb(mContext, KEY);
        Slice slice = mProvider.onBindSlice(INTENT_SLICE_URI);

        assertThat(slice.getUri()).isEqualTo(INTENT_SLICE_URI);
        assertThat(slice.getItems()).isEmpty();
    }

    @Test
    public void testLoadSlice_returnsSliceFromAccessor() {
        SliceTestUtils.insertSliceToDb(mContext, KEY);

        mProvider.loadSlice(INTENT_SLICE_URI);
        SliceData data = mProvider.mSliceWeakDataCache.get(INTENT_SLICE_URI);

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
    }

    @Test
    public void loadSlice_registersIntentFilter() {
        SliceTestUtils.insertSliceToDb(mContext, KEY);

        mProvider.loadSlice(INTENT_SLICE_URI);

        verify(mProvider)
                .registerIntentToUri(eq(FakeToggleController.INTENT_FILTER), eq(INTENT_SLICE_URI));
    }

    @Ignore("b/314925256")
    @Test
    public void loadSlice_registersBackgroundListener() {
        SliceTestUtils.insertSliceToDb(mContext, KEY);

        mProvider.loadSlice(INTENT_SLICE_URI);

        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();

        assertThat(mProvider.mPinnedWorkers.get(INTENT_SLICE_URI).getClass())
                .isEqualTo(FakeToggleController.TestWorker.class);
    }

    @Test
    public void testLoadSlice_cachedEntryRemovedOnUnpinned() {
        SliceData data = getMockData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onSliceUnpinned(data.getUri());
        SliceTestUtils.insertSliceToDb(mContext, data.getKey());

        SliceData cachedData = mProvider.mSliceWeakDataCache.get(data.getUri());

        assertThat(cachedData).isNull();
    }

    @Test
    public void onBindSlice_mainThread_shouldNotOverrideStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        final StrictMode.ThreadPolicy oldThreadPolicy = StrictMode.getThreadPolicy();
        SliceData data = getMockData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());

        final StrictMode.ThreadPolicy newThreadPolicy = StrictMode.getThreadPolicy();

        assertThat(newThreadPolicy.toString()).isEqualTo(oldThreadPolicy.toString());
    }

    @Test
    @Config(shadows = ShadowStrictMode.class)
    public void onBindSlice_backgroundThread_shouldOverrideStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);

        SliceData data = getMockData();
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
    public void onBindSlice_nightModeChanged_shouldReloadTheme() {
        mContext.getResources().getConfiguration().uiMode = UI_MODE_NIGHT_NO;
        final SliceData data = getMockData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());

        mContext.getResources().getConfiguration().uiMode = UI_MODE_NIGHT_YES;
        mProvider.onBindSlice(data.getUri());

        assertThat(ShadowTheme.isThemeRebased()).isTrue();
    }

    @Test
    public void onBindSlice_nightModeNotChanged_shouldNotReloadTheme() {
        mContext.getResources().getConfiguration().uiMode = UI_MODE_NIGHT_NO;
        SliceData data = getMockData();
        mProvider.mSliceWeakDataCache.put(data.getUri(), data);
        mProvider.onBindSlice(data.getUri());

        mContext.getResources().getConfiguration().uiMode = UI_MODE_NIGHT_NO;
        mProvider.onBindSlice(data.getUri());

        assertThat(ShadowTheme.isThemeRebased()).isFalse();
    }

    @Test
    public void onBindSlice_guestRestricted_returnsNull() {
        final String key = "enable_usb_tethering";
        mShadowUserManager.setGuestUser(true);
        final Uri testUri = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(key)
            .build();

        final Slice slice = mProvider.onBindSlice(testUri);

        assertThat(slice).isNull();
    }

    @Test
    public void onBindSlice_notGuestRestricted_returnsNotNull() {
        final String key = "enable_usb_tethering";
        final Uri testUri = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(key)
            .build();

        final Slice slice = mProvider.onBindSlice(testUri);

        assertThat(slice).isNotNull();
    }

    @Test
    public void getDescendantUris_fullActionUri_returnsSelf() {
        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(ACTION_SLICE_URI);

        assertThat(descendants).containsExactly(ACTION_SLICE_URI);
    }

    @Test
    public void getDescendantUris_fullIntentUri_returnsSelf() {

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(ACTION_SLICE_URI);

        assertThat(descendants).containsExactly(ACTION_SLICE_URI);
    }

    @Test
    public void getDescendantUris_wrongPath_returnsEmpty() {
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath("invalid_path")
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).isEmpty();
    }

    @Test
    public void getDescendantUris_invalidPath_returnsEmpty() {
        final String key = "platform_key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, "oem_key", false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, "platform_key", true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, key, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, key, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
    public void getDescendantUris_oemSliceNoPath_notContainPrivateUri() {
        final String key = "oem_key";
        SliceTestUtils.insertSliceToDb(mContext, key, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, false /* isPublicSlice */);
        final Uri uri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .build();
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(uri);

        assertThat(descendants).doesNotContain(expectedUri);
    }

    @Test
    public void getDescendantUris_platformSlice_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
        SliceTestUtils.insertSliceToDb(mContext, platformKey, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        SliceTestUtils.insertSliceToDb(mContext, oemKey, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
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
    @Config(qualifiers = "mcc999")
    public void getDescendantUris_privateSlicesNeeded_containsPrivateSliceUri() {
        final String privateKey = "test_private";
        final Uri specialUri = Uri.parse("content://com.android.settings.slices/test");
        doReturn(true).when(mProvider).isPrivateSlicesNeeded(specialUri);
        SliceTestUtils.insertSliceToDb(mContext, privateKey /* key */, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, false /* isPublicSlice */);
        final Collection<Uri> expectedUris = new HashSet<>();
        expectedUris.addAll(SPECIAL_CASE_OEM_URIS);
        expectedUris.add(new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(privateKey)
                .build());

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(specialUri);

        assertThat(descendants).containsExactlyElementsIn(expectedUris);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getDescendantUris_privateSlicesNotNeeded_notContainPrivateSliceUri() {
        final Uri specialUri = Uri.parse("content://com.android.settings.slices/test");
        doReturn(false).when(mProvider).isPrivateSlicesNeeded(specialUri);
        SliceTestUtils.insertSliceToDb(mContext,
                "test_private" /* key */, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, false /* isPublicSlice */);
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath("test_private")
                .build();

        final Collection<Uri> descendants = mProvider.onGetSliceDescendants(specialUri);

        assertThat(descendants).doesNotContain(expectedUri);
    }

    @Test
    public void onCreatePermissionRequest_returnsSettingIntent() {
        final PendingIntent pendingIntent = mProvider.onCreatePermissionRequest(
                CustomSliceRegistry.FLASHLIGHT_SLICE_URI, "com.android.whaaaat");
        final Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS)
                .setPackage(Utils.SETTINGS_PACKAGE_NAME);
        PendingIntent settingsPendingIntent =
                PendingIntent.getActivity(mContext, 0, settingsIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        assertThat(pendingIntent).isEqualTo(settingsPendingIntent);
    }

    @Test
    public void bindSlice_flashlightSlice_returnsFlashlightSlice() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.FLASHLIGHT_AVAILABLE, 1);

        final Slice flashlightSlice = mProvider.onBindSlice(
                CustomSliceRegistry.FLASHLIGHT_SLICE_URI);

        assertThat(flashlightSlice.getUri()).isEqualTo(CustomSliceRegistry.FLASHLIGHT_SLICE_URI);
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

    @Test
    @Config(qualifiers = "mcc998")
    public void grantAllowlistedPackagePermissions_noAllowlist_shouldNotGrant() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(Uri.parse("content://settings/slice"));

        SettingsSliceProvider.grantAllowlistedPackagePermissions(mContext, uris);

        verify(mManager, never()).grantSlicePermission(anyString(), any(Uri.class));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void grantAllowlistedPackagePermissions_hasPackageAllowlist_shouldGrant() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(Uri.parse("content://settings/slice"));

        SettingsSliceProvider.grantAllowlistedPackagePermissions(mContext, uris);

        verify(mManager)
                .grantSlicePermission("com.android.settings.slice_allowlist_package", uris.get(0));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isPrivateSlicesNeeded_incorrectUri_returnFalse() {
        final Uri uri = Uri.parse("content://com.android.settings.slices/test123");

        assertThat(mProvider.isPrivateSlicesNeeded(uri)).isFalse();
    }

    @Test
    public void isPrivateSlicesNeeded_noUri_returnFalse() {
        final Uri uri = Uri.parse("content://com.android.settings.slices/test");

        assertThat(mProvider.isPrivateSlicesNeeded(uri)).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isPrivateSlicesNeeded_correctUriWithPermissionAndIsSI_returnTrue() {
        final Uri uri = Uri.parse("content://com.android.settings.slices/test");
        ShadowBinder.setCallingUid(123);
        doReturn(PERMISSION_GRANTED)
                .when(mContext).checkPermission(anyString(), anyInt(), anyInt());
        mPackageManager.setPackagesForUid(123, new String[]{"com.android.settings.intelligence"});

        assertThat(mProvider.isPrivateSlicesNeeded(uri)).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isPrivateSlicesNeeded_correctUriWithPermissionNotSI_returnFalse() {
        final Uri uri = Uri.parse("content://com.android.settings.slices/test");
        ShadowBinder.setCallingUid(123);
        doReturn(PERMISSION_GRANTED)
                .when(mContext).checkPermission(anyString(), anyInt(), anyInt());
        mPackageManager.setPackagesForUid(123, new String[]{"com.android.settings.test"});

        assertThat(mProvider.isPrivateSlicesNeeded(uri)).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isPrivateSlicesNeeded_correctUriNoPermission_returnFalse() {
        final Uri uri = Uri.parse("content://com.android.settings.slices/test");
        ShadowBinder.setCallingUid(123);
        doReturn(PERMISSION_DENIED).when(mContext).checkPermission(anyString(), anyInt(), anyInt());
        mPackageManager.setPackagesForUid(123, new String[]{"com.android.settings.intelligence"});

        assertThat(mProvider.isPrivateSlicesNeeded(uri)).isFalse();
    }

    private static SliceData getMockData() {
        return new SliceData.Builder()
                .setKey(KEY)
                .setUri(URI)
                .setTitle(SliceTestUtils.FAKE_TITLE)
                .setSummary(SliceTestUtils.FAKE_SUMMARY)
                .setScreenTitle(SliceTestUtils.FAKE_SCREEN_TITLE)
                .setIcon(SliceTestUtils.FAKE_ICON)
                .setFragmentName(SliceTestUtils.FAKE_FRAGMENT_NAME)
                .setPreferenceControllerClassName(SliceTestUtils.FAKE_CONTROLLER_NAME)
                .setHighlightMenuRes(SliceTestUtils.FAKE_HIGHLIGHT_MENU_RES)
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
        protected static void setThreadPolicy(final StrictMode.ThreadPolicy policy) {
            sSetThreadPolicyCount++;
        }

        private static boolean isThreadPolicyOverridden() {
            return sSetThreadPolicyCount != 0;
        }
    }

    @Implements(Theme.class)
    public static class ShadowTheme {
        private static boolean sThemeRebased;

        @Resetter
        public static void reset() {
            sThemeRebased = false;
        }

        @Implementation
        public void rebase() {
            sThemeRebased = true;
        }

        static boolean isThemeRebased() {
            return sThemeRebased;
        }
    }
}
