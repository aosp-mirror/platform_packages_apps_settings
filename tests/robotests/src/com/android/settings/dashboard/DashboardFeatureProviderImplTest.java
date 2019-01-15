/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.testutils.shadow.ShadowTileUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.CategoryManager;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class DashboardFeatureProviderImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private CategoryManager mCategoryManager;
    @Mock
    private PackageManager mPackageManager;
    private FakeFeatureFactory mFeatureFactory;

    private Context mContext;
    private DashboardFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
            .thenReturn(new ResolveInfo());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mImpl = new DashboardFeatureProviderImpl(mContext);
    }

    @Test
    public void shouldHoldAppContext() {
        assertThat(mImpl.mContext).isEqualTo(mContext.getApplicationContext());
    }

    @Test
    public void bindPreference_shouldBindAllData() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.title = "title";
        tile.summary = "summary";
        tile.icon = Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));
        tile.metaData = new Bundle();
        tile.metaData.putString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS, "HI");
        tile.priority = 10;
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getTitle()).isEqualTo(tile.title);
        assertThat(preference.getSummary()).isEqualTo(tile.summary);
        assertThat(preference.getIcon()).isNotNull();
        assertThat(preference.getFragment())
                .isEqualTo(tile.metaData.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS));
        assertThat(preference.getOrder()).isEqualTo(-tile.priority);
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindIntent() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.priority = 10;
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));

        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getFragment()).isNull();
        assertThat(preference.getOnPreferenceClickListener()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(-tile.priority);
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindToProfileSelector() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));
        tile.userHandle.add(mock(UserHandle.class));
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));

        when(mActivity.getApplicationContext().getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);

        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);

        verify(mActivity).getFragmentManager();
    }

    @Test
    public void bindPreference_noFragmentMetadataSingleUser_shouldBindToDirectLaunchIntent() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));

        when(mActivity.getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);

        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);

        verify(mFeatureFactory.metricsFeatureProvider).logDashboardStartIntent(
                any(Context.class),
                any(Intent.class),
                eq(MetricsProto.MetricsEvent.SETTINGS_GESTURES));
        verify(mActivity)
                .startActivityForResultAsUser(any(Intent.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void bindPreference_toInternalSettingActivity_shouldBindToDirectLaunchIntentAndNotLog() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));
        tile.intent = new Intent();
        tile.intent.setComponent(
                new ComponentName(RuntimeEnvironment.application.getPackageName(), "class"));

        when(mActivity.getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);
        when(mActivity.getApplicationContext().getPackageName())
                .thenReturn(RuntimeEnvironment.application.getPackageName());

        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);
        verify(mFeatureFactory.metricsFeatureProvider).logDashboardStartIntent(
                any(Context.class),
                any(Intent.class),
                anyInt());
        verify(mActivity)
                .startActivityForResultAsUser(any(Intent.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void bindPreference_nullPreference_shouldIgnore() {
        final Tile tile = mock(Tile.class);
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                null, tile, "123", Preference.DEFAULT_ORDER);

        verifyZeroInteractions(tile);
    }

    @Test
    public void bindPreference_withNullKeyNullPriority_shouldGenerateKeyAndPriority() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, null /*key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getKey()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(Preference.DEFAULT_ORDER);
    }

    @Test
    public void bindPreference_noSummary_shouldSetSummaryToPlaceholder() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, null /*key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.summary_placeholder));
    }

    @Test
    public void bindPreference_hasSummary_shouldSetSummary() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.summary = "test";
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, null /*key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getSummary()).isEqualTo(tile.summary);
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class, ShadowThreadUtils.class})
    public void bindPreference_hasSummaryUri_shouldLoadSummaryFromContentProvider() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        tile.metaData = new Bundle();
        tile.metaData.putString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI,
                "content://com.android.settings/tile_summary");

        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, null /*key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getSummary()).isEqualTo(ShadowTileUtils.MOCK_SUMMARY);
    }

    @Test
    public void bindPreference_withNullKeyTileKey_shouldUseTileKey() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.key = "key";
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getKey()).isEqualTo(tile.key);
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class, ShadowThreadUtils.class})
    public void bindPreference_withIconUri_shouldLoadIconFromContentProvider() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.key = "key";
        tile.intent = new Intent();
        tile.intent.setComponent(
                new ComponentName(RuntimeEnvironment.application.getPackageName(), "class"));
        tile.metaData = new Bundle();
        tile.metaData.putString(TileUtils.META_DATA_PREFERENCE_ICON_URI,
                "content://com.android.settings/tile_icon");
        mImpl.bindIcon(preference, tile);

        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    public void bindPreference_withBaseOrder_shouldOffsetPriority() {
        final int baseOrder = 100;
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.priority = 10;
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, "123", baseOrder);

        assertThat(preference.getOrder()).isEqualTo(-tile.priority + baseOrder);
    }

    @Test
    public void bindPreference_withOrderMetadata_shouldUseOrderInMetadata() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final int testOrder = -30;
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.metaData.putInt(mImpl.META_DATA_KEY_ORDER, testOrder);
        tile.priority = 10;
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getOrder()).isEqualTo(testOrder);
    }

    @Test
    public void bindPreference_invalidOrderMetadata_shouldIgnore() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.metaData.putString(mImpl.META_DATA_KEY_ORDER, "hello");
        tile.priority = 10;
        mImpl.bindPreferenceToTile(mActivity, MetricsProto.MetricsEvent.VIEW_UNKNOWN,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getOrder()).isEqualTo(-tile.priority);
    }

    @Test
    public void bindPreference_withIntentActionMetadata_shouldSetLaunchAction() {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        final Preference preference = new Preference(RuntimeEnvironment.application);
        final Tile tile = new Tile();
        tile.key = "key";
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        tile.metaData = new Bundle();
        tile.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;
        mImpl.bindPreferenceToTile(activity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();
        ShadowActivity shadowActivity = shadowOf(activity);

        final Intent launchIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(launchIntent.getAction())
                .isEqualTo("TestAction");
        assertThat(launchIntent.getIntExtra(VisibilityLoggerMixin.EXTRA_SOURCE_METRICS_CATEGORY, 0))
                .isEqualTo(MetricsProto.MetricsEvent.SETTINGS_GESTURES);
    }

    @Test
    public void clickPreference_withUnresolvableIntent_shouldNotLaunchAnything() {
        ReflectionHelpers.setField(
                mImpl, "mPackageManager", RuntimeEnvironment.application.getPackageManager());
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        final ShadowApplication application = ShadowApplication.getInstance();
        final Preference preference = new Preference(application.getApplicationContext());
        final Tile tile = new Tile();
        tile.key = "key";
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        tile.metaData = new Bundle();
        tile.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;

        mImpl.bindPreferenceToTile(activity, MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();

        final ShadowActivity.IntentForResult launchIntent =
                shadowOf(activity).getNextStartedActivityForResult();

        assertThat(launchIntent).isNull();
    }

    @Test
    public void getPreferences_noCategory_shouldReturnNull() {
        mImpl = new DashboardFeatureProviderImpl(mActivity);
        ReflectionHelpers.setField(mImpl, "mCategoryManager", mCategoryManager);
        when(mCategoryManager.getTilesByCategory(mActivity, CategoryKey.CATEGORY_HOMEPAGE))
                .thenReturn(null);

        assertThat(mImpl.getPreferencesForCategory(null, null,
                MetricsProto.MetricsEvent.SETTINGS_GESTURES, CategoryKey.CATEGORY_HOMEPAGE))
                .isNull();
    }

    @Test
    public void getPreferences_noTileForCategory_shouldReturnNull() {
        mImpl = new DashboardFeatureProviderImpl(mActivity);
        ReflectionHelpers.setField(mImpl, "mCategoryManager", mCategoryManager);
        when(mCategoryManager.getTilesByCategory(mActivity, CategoryKey.CATEGORY_HOMEPAGE))
                .thenReturn(new DashboardCategory());

        assertThat(mImpl.getPreferencesForCategory(null, null,
                MetricsProto.MetricsEvent.SETTINGS_GESTURES, CategoryKey.CATEGORY_HOMEPAGE))
                .isNull();
    }

    @Test
    public void getPreferences_hasTileForCategory_shouldReturnPrefList() {
        mImpl = new DashboardFeatureProviderImpl(mActivity);
        ReflectionHelpers.setField(mImpl, "mCategoryManager", mCategoryManager);
        final DashboardCategory category = new DashboardCategory();
        category.addTile(new Tile());
        when(mCategoryManager
                .getTilesByCategory(any(Context.class), eq(CategoryKey.CATEGORY_HOMEPAGE)))
                .thenReturn(category);

        assertThat(mImpl.getPreferencesForCategory(mActivity,
                ShadowApplication.getInstance().getApplicationContext(),
                MetricsProto.MetricsEvent.SETTINGS_GESTURES,
                CategoryKey.CATEGORY_HOMEPAGE).isEmpty())
                .isFalse();
    }

    @Test
    public void testGetExtraIntentAction_shouldReturnNull() {
        assertThat(mImpl.getExtraIntentAction()).isNull();
    }

    @Test
    public void testShouldTintIcon_enabledInResources_shouldBeTrue() {
        assertThat(mImpl.shouldTintIcon()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testShouldTintIcon_disabledInResources_shouldBeFalse() {
        assertThat(mImpl.shouldTintIcon()).isFalse();
    }
}
