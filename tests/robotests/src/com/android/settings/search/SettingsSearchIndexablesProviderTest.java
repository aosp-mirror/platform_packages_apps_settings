package com.android.settings.search;

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SEARCHABLE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchIndexablesContract;

import com.android.settings.R;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.dashboard.CategoryManager;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
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
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsSearchIndexablesProviderTest.ShadowCategoryManager.class)
public class SettingsSearchIndexablesProviderTest {

    private static final String PACKAGE_NAME = "com.android.settings";
    private static final String BASE_AUTHORITY = "content://" + PACKAGE_NAME + "/";

    private Context mContext;
    private SettingsSearchIndexablesProvider mProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mProvider = spy(new SettingsSearchIndexablesProvider());
        ProviderInfo info = new ProviderInfo();
        info.exported = true;
        info.grantUriPermissions = true;
        info.authority = PACKAGE_NAME;
        info.readPermission = Manifest.permission.READ_SEARCH_INDEXABLES;
        mProvider.attachInfo(RuntimeEnvironment.application, info);

        final SearchFeatureProvider featureProvider = new SearchFeatureProviderImpl();
        featureProvider.getSearchIndexableResources().getProviderValues().clear();
        featureProvider.getSearchIndexableResources().getProviderValues()
                .add(new SearchIndexableData(FakeSettingsFragment.class,
                        FakeSettingsFragment.SEARCH_INDEX_DATA_PROVIDER));
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = featureProvider;

        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "pkg";
        activityInfo.name = "class";
        activityInfo.metaData = new Bundle();
        activityInfo.metaData.putString(META_DATA_PREFERENCE_TITLE, "title");
        final DashboardCategory category = new DashboardCategory("key");
        when(mFakeFeatureFactory.dashboardFeatureProvider.getAllCategories())
                .thenReturn(Arrays.asList(category));
        category.addTile(new ActivityTile(activityInfo, category.key));
        ShadowCategoryManager.setDashboardCategory(category);
    }

    @After
    public void cleanUp() {
        ShadowCategoryManager.reset();
        mFakeFeatureFactory.searchFeatureProvider = mock(SearchFeatureProvider.class);
    }

    @Test
    public void testRawColumnFetched() {
        Uri rawUri = Uri.parse(BASE_AUTHORITY + SearchIndexablesContract.INDEXABLES_RAW_PATH);

        final Cursor cursor = mProvider.query(rawUri,
                SearchIndexablesContract.INDEXABLES_RAW_COLUMNS, null, null, null);

        cursor.moveToFirst();
        assertThat(cursor.getString(1)).isEqualTo(FakeSettingsFragment.TITLE);
        assertThat(cursor.getString(2)).isEqualTo(FakeSettingsFragment.SUMMARY_ON);
        assertThat(cursor.getString(3)).isEqualTo(FakeSettingsFragment.SUMMARY_OFF);
        assertThat(cursor.getString(4)).isEqualTo(FakeSettingsFragment.ENTRIES);
        assertThat(cursor.getString(5)).isEqualTo(FakeSettingsFragment.KEYWORDS);
        assertThat(cursor.getString(6)).isEqualTo(FakeSettingsFragment.SCREEN_TITLE);
        assertThat(cursor.getString(7)).isEqualTo(FakeSettingsFragment.CLASS_NAME);
        assertThat(cursor.getInt(8)).isEqualTo(FakeSettingsFragment.ICON);
        assertThat(cursor.getString(9)).isEqualTo(FakeSettingsFragment.INTENT_ACTION);
        assertThat(cursor.getString(10)).isEqualTo(FakeSettingsFragment.TARGET_PACKAGE);
        assertThat(cursor.getString(11)).isEqualTo(FakeSettingsFragment.TARGET_CLASS);
        assertThat(cursor.getString(12)).isEqualTo(FakeSettingsFragment.KEY);
    }

    @Test
    public void testResourcesColumnFetched() {
        Uri rawUri = Uri.parse(BASE_AUTHORITY + SearchIndexablesContract.INDEXABLES_XML_RES_PATH);

        final Cursor cursor = mProvider.query(rawUri,
                SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS, null, null, null);

        cursor.moveToFirst();
        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getInt(1)).isEqualTo(R.xml.display_settings);
        assertThat(cursor.getString(2)).isEqualTo(FakeSettingsFragment.CLASS_NAME);
        assertThat(cursor.getInt(3)).isEqualTo(0);
        assertThat(cursor.getString(4)).isNull();
        assertThat(cursor.getString(5)).isNull();
        assertThat(cursor.getString(6)).isNull();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testNonIndexablesColumnFetched() {
        final Uri rawUri = Uri.parse(
                BASE_AUTHORITY + SearchIndexablesContract.NON_INDEXABLES_KEYS_PATH);

        final List<String> keys = new ArrayList<>();

        try (Cursor cursor = mProvider.query(rawUri,
                SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS, null, null, null)) {
            while (cursor.moveToNext()) {
                keys.add(cursor.getString(0));
            }
        }

        assertThat(keys).hasSize(3);
        assertThat(keys).containsAtLeast("pref_key_1", "pref_key_3", "pref_key_5");
    }

    @Test
    public void refreshSearchEnabledState_classNotFoundInCategoryMap_hasInjectionRawData() {
        mProvider.refreshSearchEnabledState(mContext,
                ManagedProfileSettings.SEARCH_INDEX_DATA_PROVIDER);

        assertThat(mProvider.getInjectionIndexableRawData(mContext)).isNotEmpty();
    }

    @Test
    public void refreshSearchEnabledState_noDashboardCategory_hasInjectionRawData() {
        ShadowCategoryManager.setDashboardCategory(null);

        mProvider.refreshSearchEnabledState(mContext,
                TopLevelSettings.SEARCH_INDEX_DATA_PROVIDER);

        assertThat(mProvider.getInjectionIndexableRawData(mContext)).isNotEmpty();
    }

    @Test
    public void refreshSearchEnabledState_pageSearchEnabled_hasInjectionRawData() {
        mProvider.refreshSearchEnabledState(mContext,
                NetworkDashboardFragment.SEARCH_INDEX_DATA_PROVIDER);

        assertThat(mProvider.getInjectionIndexableRawData(mContext)).isNotEmpty();
    }

    @Test
    public void refreshSearchEnabledState_pageSearchDisable_noInjectionRawData() {
        mProvider.refreshSearchEnabledState(mContext,
                TopLevelSettings.SEARCH_INDEX_DATA_PROVIDER);

        assertThat(mProvider.getInjectionIndexableRawData(mContext)).isEmpty();
    }

    @Test
    public void isEligibleForIndexing_isSettingsInjectedItem_shouldReturnFalse() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = PACKAGE_NAME;
        activityInfo.name = "class";
        final ActivityTile activityTile = new ActivityTile(activityInfo,
                CategoryKey.CATEGORY_SYSTEM);

        assertThat(mProvider.isEligibleForIndexing(PACKAGE_NAME, activityTile)).isFalse();
    }

    @Test
    public void isEligibleForIndexing_normalInjectedItem_shouldReturnTrue() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "pkg";
        activityInfo.name = "class";
        final ActivityTile activityTile = new ActivityTile(activityInfo,
                CategoryKey.CATEGORY_CONNECT);

        assertThat(mProvider.isEligibleForIndexing(PACKAGE_NAME, activityTile)).isTrue();
    }

    @Test
    public void isEligibleForIndexing_disabledByMetadata_shouldReturnFalse() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = PACKAGE_NAME;
        activityInfo.name = "class";
        activityInfo.metaData = new Bundle();
        activityInfo.metaData.putBoolean(META_DATA_PREFERENCE_SEARCHABLE, false);
        final ActivityTile activityTile = new ActivityTile(activityInfo,
                CategoryKey.CATEGORY_CONNECT);

        assertThat(mProvider.isEligibleForIndexing(PACKAGE_NAME, activityTile)).isFalse();
    }

    @Implements(CategoryManager.class)
    public static class ShadowCategoryManager {

        private static DashboardCategory sCategory;

        @Resetter
        static void reset() {
            sCategory = null;
        }

        @Implementation
        public DashboardCategory getTilesByCategory(Context context, String categoryKey) {
            return sCategory;
        }

        static void setDashboardCategory(DashboardCategory category) {
            sCategory = category;
        }
    }
}
