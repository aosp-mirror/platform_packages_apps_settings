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

import static org.mockito.Mockito.spy;

import android.app.ApplicationPackageManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.SettingsSlicesContract;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeIndexProvider;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.search.SearchIndexableData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowUtils.class,
        SlicesDatabaseAccessorTest.ShadowApplicationPackageManager.class,
        ShadowBluetoothAdapter.class, ShadowLockPatternUtils.class})
public class SlicesDatabaseAccessorTest {


    private Context mContext;
    private SlicesDatabaseAccessor mAccessor;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        ShadowUserManager.getShadow().setIsAdminUser(true);
        mAccessor = spy(new SlicesDatabaseAccessor(mContext));
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();

        // Register the fake a11y Service
        ShadowAccessibilityManager shadowAccessibilityManager = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(AccessibilityManager.class));
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testGetSliceDataFromKey_validKey_validSliceReturned() {
        String key = "key";
        SliceTestUtils.insertSliceToDb(mContext, key);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(SliceTestUtils.FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SliceTestUtils.FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(SliceTestUtils.FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(SliceTestUtils.FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(SliceTestUtils.FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(SliceTestUtils.FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isNull();
    }

    @Test
    public void testGetSliceDataFromKey_allowDynamicSummary_validSliceReturned() {
        String key = "key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(SliceTestUtils.FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SliceTestUtils.FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(SliceTestUtils.FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(SliceTestUtils.FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(SliceTestUtils.FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(SliceTestUtils.FAKE_CONTROLLER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceDataFromKey_invalidKey_errorThrown() {
        String key = "key";

        mAccessor.getSliceDataFromKey(key);
    }

    @Test
    public void testGetSliceFromUri_validUri_validSliceReturned() {
        final String key = "key";
        SliceTestUtils.insertSliceToDb(mContext, key);

        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath("action")
                .appendPath(key)
                .build();

        SliceData data = mAccessor.getSliceDataFromUri(uri);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(SliceTestUtils.FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SliceTestUtils.FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(SliceTestUtils.FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(SliceTestUtils.FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(SliceTestUtils.FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(uri);
        assertThat(data.getPreferenceController()).isEqualTo(SliceTestUtils.FAKE_CONTROLLER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceFromUri_invalidUri_errorThrown() {
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath("intent")
                .appendPath("durr")
                .build();
        mAccessor.getSliceDataFromUri(uri);
    }

    @Test
    public void getDescendantUris_platformSlice_doesNotReturnOEMSlice() {
        final String key = "oem_key";
        SliceTestUtils.insertSliceToDb(mContext, key, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        final List<Uri> keys = mAccessor.getSliceUris(SettingsSlicesContract.AUTHORITY,
                true /* isPublicSlice */);

        assertThat(keys).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_doesNotReturnPlatformSlice() {
        final String key = "platform_key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        final List<Uri> keys = mAccessor.getSliceUris(SettingsSliceProvider.SLICE_AUTHORITY,
                true /* isPublicSlice */);

        assertThat(keys).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_returnsOEMUriDescendant() {
        final String key = "oem_key";
        SliceTestUtils.insertSliceToDb(mContext, key, false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        final List<Uri> keys = mAccessor.getSliceUris(SettingsSliceProvider.SLICE_AUTHORITY,
                true /* isPublicSlice */);

        assertThat(keys).containsExactly(
                Uri.parse("content://com.android.settings.slices/action/oem_key"));
    }

    @Test
    public void getDescendantUris_platformSlice_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        final List<Uri> keys = mAccessor.getSliceUris(SettingsSlicesContract.AUTHORITY,
                true /* isPublicSlice */);

        assertThat(keys).containsExactly(
                Uri.parse("content://android.settings.slices/action/platform_key"));
    }

    @Test
    public void getSliceUris_publicSlice_returnPublicUri() {
        SliceTestUtils.insertSliceToDb(mContext, "test_public", false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        SliceTestUtils.insertSliceToDb(mContext, "test_private", false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, false /* isPublicSlice */);
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath("test_public")
                .build();

        final List<Uri> uri = mAccessor.getSliceUris(SettingsSliceProvider.SLICE_AUTHORITY, true);

        assertThat(uri).containsExactly(expectedUri);
    }

    @Test
    public void getSliceUris_nonPublicSlice_returnNonPublicUri() {
        SliceTestUtils.insertSliceToDb(mContext, "test_public", false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, true /* isPublicSlice */);
        SliceTestUtils.insertSliceToDb(mContext, "test_private", false /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */, false /* isPublicSlice */);
        final Uri expectedUri = new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath("test_private")
                .build();

        final List<Uri> uri = mAccessor.getSliceUris(SettingsSliceProvider.SLICE_AUTHORITY, false);

        assertThat(uri).containsExactly(expectedUri);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getSliceKeys_indexesDatabase() {
        // Force new indexing
        Locale.setDefault(new Locale("ca"));
        final SearchFeatureProvider provider = new SearchFeatureProviderImpl();
        final SlicesFeatureProvider sliceProvider = new SlicesFeatureProviderImpl();
        final FakeFeatureFactory factory = FakeFeatureFactory.setupForTest();
        factory.searchFeatureProvider = provider;
        factory.slicesFeatureProvider = sliceProvider;
        // Fake the indexable list.
        provider.getSearchIndexableResources().getProviderValues().clear();
        provider.getSearchIndexableResources().getProviderValues().add(
                new SearchIndexableData(FakeIndexProvider.class,
                        FakeIndexProvider.SEARCH_INDEX_DATA_PROVIDER));

        final SlicesDatabaseAccessor accessor = new SlicesDatabaseAccessor(mContext);
        final List<Uri> keys = accessor.getSliceUris(SettingsSliceProvider.SLICE_AUTHORITY,
                true /* isPublicSlice */);

        assertThat(keys).isNotEmpty();
    }

    @Test
    public void testGetSliceDataFromKey_defaultUnavailableSlice_validSliceReturned() {
        String key = "key";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(SliceTestUtils.FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SliceTestUtils.FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(SliceTestUtils.FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(SliceTestUtils.FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(SliceTestUtils.FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(SliceTestUtils.FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isNull();
    }

    @Test
    public void testGetSliceDataFromKey_customizeSubtitleOfUnavailableSlice_validSliceReturned() {
        String key = "key";
        String subtitle = "subtitle";
        SliceTestUtils.insertSliceToDb(mContext, key, true /* isPlatformSlice */, subtitle);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(SliceTestUtils.FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(SliceTestUtils.FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SliceTestUtils.FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(SliceTestUtils.FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(SliceTestUtils.FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(SliceTestUtils.FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(SliceTestUtils.FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isEqualTo(subtitle);
    }

    @Implements(ApplicationPackageManager.class)
    public static class ShadowApplicationPackageManager extends
            org.robolectric.shadows.ShadowApplicationPackageManager {

        @Implementation
        protected ComponentName getInstantAppResolverSettingsComponent() {
            return null;
        }
    }
}
