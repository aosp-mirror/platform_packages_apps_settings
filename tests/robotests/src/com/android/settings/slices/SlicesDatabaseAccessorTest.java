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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.app.ApplicationPackageManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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

    private final String FAKE_TITLE = "title";
    private final String FAKE_SUMMARY = "summary";
    private final String FAKE_SCREEN_TITLE = "screen_title";
    private final String FAKE_KEYWORDS = "a, b, c";
    private final int FAKE_ICON = 1234;
    private final String FAKE_FRAGMENT_NAME = FakeIndexProvider.class.getName();
    private final String FAKE_CONTROLLER_NAME = FakePreferenceController.class.getName();

    private Context mContext;
    private SQLiteDatabase mDb;
    private SlicesDatabaseAccessor mAccessor;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        ShadowUserManager.getShadow().setIsAdminUser(true);
        mAccessor = spy(new SlicesDatabaseAccessor(mContext));
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
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
        insertSpecialCase(key);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isNull();
    }

    @Test
    public void testGetSliceDataFromKey_allowDynamicSummary_validSliceReturned() {
        String key = "key";
        insertSpecialCase(key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceDataFromKey_invalidKey_errorThrown() {
        String key = "key";

        mAccessor.getSliceDataFromKey(key);
    }

    @Test
    public void testGetSliceFromUri_validUri_validSliceReturned() {
        String key = "key";
        String path = "intent/" + key;
        insertSpecialCase(key);
        Uri uri = SliceBuilderUtils.getUri(path, false);

        SliceData data = mAccessor.getSliceDataFromUri(uri);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(uri);
        assertThat(data.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceFromUri_invalidUri_errorThrown() {
        Uri uri = SliceBuilderUtils.getUri("intent/durr", false);
        mAccessor.getSliceDataFromUri(uri);
    }

    @Test
    public void getDescendantUris_platformSlice_doesNotReturnOEMSlice() {
        final String key = "oem_key";
        final boolean isPlatformSlice = false;
        insertSpecialCase(key, isPlatformSlice);
        final List<String> keys = mAccessor.getSliceKeys(!isPlatformSlice);

        assertThat(keys).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_doesNotReturnPlatformSlice() {
        final String key = "platform_key";
        final boolean isPlatformSlice = true;
        insertSpecialCase(key, isPlatformSlice);
        final List<String> keys = mAccessor.getSliceKeys(!isPlatformSlice);

        assertThat(keys).isEmpty();
    }

    @Test
    public void getDescendantUris_oemSlice_returnsOEMUriDescendant() {
        final String key = "oem_key";
        final boolean isPlatformSlice = false;
        insertSpecialCase(key, isPlatformSlice);
        final List<String> keys = mAccessor.getSliceKeys(isPlatformSlice);

        assertThat(keys).containsExactly(key);
    }

    @Test
    public void getDescendantUris_platformSlice_returnsPlatformUriDescendant() {
        final String key = "platform_key";
        final boolean isPlatformSlice = true;
        insertSpecialCase(key, isPlatformSlice);
        final List<String> keys = mAccessor.getSliceKeys(isPlatformSlice);

        assertThat(keys).containsExactly(key);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getSliceKeys_indexesDatabase() {
        // Force new indexing
        Locale.setDefault(new Locale("ca"));
        final SearchFeatureProvider provider = new SearchFeatureProviderImpl();
        final SlicesFeatureProvider sliceProvider = spy(new SlicesFeatureProviderImpl());
        final FakeFeatureFactory factory = FakeFeatureFactory.setupForTest();
        factory.searchFeatureProvider = provider;
        factory.slicesFeatureProvider = sliceProvider;
        // Fake the indexable list.
        provider.getSearchIndexableResources().getProviderValues().clear();
        provider.getSearchIndexableResources().getProviderValues().add(
                FakeIndexProvider.class);

        final SlicesDatabaseAccessor accessor = new SlicesDatabaseAccessor(mContext);
        final List<String> keys = accessor.getSliceKeys(true);

        assertThat(keys).isNotEmpty();
    }

    @Test
    public void testGetSliceDataFromKey_defaultUnavailableSlice_validSliceReturned() {
        String key = "key";
        insertSpecialCase(key, true /* isPlatformSlice */,
                null /* customizedUnavailableSliceSubtitle */);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isNull();
    }

    @Test
    public void testGetSliceDataFromKey_customizeSubtitleOfUnavailableSlice_validSliceReturned() {
        String key = "key";
        String subtitle = "subtitle";
        insertSpecialCase(key, true /* isPlatformSlice */, subtitle);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(data.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(FAKE_KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
        assertThat(data.getUnavailableSliceSubtitle()).isEqualTo(subtitle);
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(key, true);
    }

    private void insertSpecialCase(String key, boolean isPlatformSlice) {
        insertSpecialCase(key, isPlatformSlice, null /*customizedUnavailableSliceSubtitle*/);
    }

    private void insertSpecialCase(String key, boolean isPlatformSlice,
            String customizedUnavailableSliceSubtitle) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, FAKE_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, FAKE_SUMMARY);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, FAKE_SCREEN_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.KEYWORDS, FAKE_KEYWORDS);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, FAKE_ICON);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, FAKE_FRAGMENT_NAME);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, FAKE_CONTROLLER_NAME);
        values.put(SlicesDatabaseHelper.IndexColumns.PLATFORM_SLICE, isPlatformSlice);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_TYPE, SliceData.SliceType.INTENT);
        values.put(SlicesDatabaseHelper.IndexColumns.UNAVAILABLE_SLICE_SUBTITLE,
                customizedUnavailableSliceSubtitle);

        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
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
