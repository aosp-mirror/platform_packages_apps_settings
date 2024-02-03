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

import static android.content.Intent.EXTRA_USER;

import static com.android.settingslib.drawer.SwitchesProvider.EXTRA_SWITCH_SET_CHECKED_ERROR;
import static com.android.settingslib.drawer.TileUtils.META_DATA_KEY_ORDER;
import static com.android.settingslib.drawer.TileUtils.META_DATA_KEY_PROFILE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SWITCH_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;
import static com.android.settingslib.drawer.TileUtils.PROFILE_ALL;
import static com.android.settingslib.drawer.TileUtils.PROFILE_PRIMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Pair;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.homepage.TopLevelHighlightMixin;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityEmbeddingUtils;
import com.android.settings.testutils.shadow.ShadowSplitController;
import com.android.settings.testutils.shadow.ShadowTileUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.ProviderTile;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class DashboardFeatureProviderImplTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String KEY = "key";
    private static final String SWITCH_URI = "content://com.android.settings/tile_switch";

    private final Application mApplication = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    private FakeFeatureFactory mFeatureFactory;

    private Context mContext;
    private ActivityInfo mActivityInfo;
    private ProviderInfo mProviderInfo;
    private Bundle mSwitchMetaData;
    private DashboardFeatureProviderImpl mImpl;
    private boolean mForceRoundedIcon;
    private DashboardFragment mFragment;

    @Before
    public void setUp() {
        mContext = spy(mApplication);
        doReturn(mApplication).when(mActivity).getApplicationContext();
        mForceRoundedIcon = false;
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = mContext.getPackageName();
        mActivityInfo.name = "class";
        mActivityInfo.metaData = new Bundle();
        mActivityInfo.metaData.putInt(META_DATA_PREFERENCE_TITLE, R.string.settings_label);
        mActivityInfo.metaData.putInt(META_DATA_PREFERENCE_SUMMARY,
                R.string.about_settings_summary);

        mProviderInfo = new ProviderInfo();
        mProviderInfo.packageName = mContext.getPackageName();
        mProviderInfo.name = "provider";
        mProviderInfo.authority = "com.android.settings";
        mSwitchMetaData = new Bundle();
        mSwitchMetaData.putInt(META_DATA_PREFERENCE_TITLE, R.string.settings_label);
        mSwitchMetaData.putInt(META_DATA_PREFERENCE_SUMMARY, R.string.about_settings_summary);
        mSwitchMetaData.putString(META_DATA_PREFERENCE_KEYHINT, KEY);
        mSwitchMetaData.putString(META_DATA_PREFERENCE_SWITCH_URI, SWITCH_URI);

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mImpl = new DashboardFeatureProviderImpl(mContext);
        mFragment = new TestFragment();
    }

    @Test
    public void shouldHoldAppContext() {
        assertThat(mImpl.mContext).isEqualTo(mContext.getApplicationContext());
    }

    @Test
    public void bindPreference_shouldBindAllData() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = spy(new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE));
        mActivityInfo.metaData.putInt(META_DATA_KEY_ORDER, 10);
        doReturn(Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)))
                .when(tile).getIcon(any(Context.class));
        mActivityInfo.metaData.putString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS, "HI");
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getTitle()).isEqualTo(mContext.getText(R.string.settings_label));
        assertThat(preference.getSummary())
                .isEqualTo(mContext.getText(R.string.about_settings_summary));
        assertThat(preference.getIcon()).isNotNull();
        assertThat(preference.getFragment()).isEqualTo(
                mActivityInfo.metaData.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS));
        assertThat(preference.getOrder()).isEqualTo(tile.getOrder());
    }

    @Test
    public void bindPreference_shouldBindAllSwitchData() {
        final Preference preference = new SwitchPreference(mApplication);
        final Tile tile = spy(new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE,
                mSwitchMetaData));
        mSwitchMetaData.putInt(META_DATA_KEY_ORDER, 10);
        doReturn(Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)))
                .when(tile).getIcon(any(Context.class));
        final List<DynamicDataObserver> observers = mImpl.bindPreferenceToTileAndGetObservers(
                mActivity, mFragment, mForceRoundedIcon, preference, tile, null /* key*/,
                Preference.DEFAULT_ORDER);

        assertThat(preference.getTitle()).isEqualTo(mContext.getText(R.string.settings_label));
        assertThat(preference.getSummary())
                .isEqualTo(mContext.getText(R.string.about_settings_summary));
        assertThat(preference.getKey()).isEqualTo(KEY);
        assertThat(preference.getIcon()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(tile.getOrder());
        assertThat(observers.get(0).getUri().toString()).isEqualTo(SWITCH_URI);
    }

    @Test
    public void bindPreference_providerTileWithPendingIntent_shouldBindIntent() {
        final Preference preference = new SwitchPreference(mApplication);
        Bundle metaData = new Bundle();
        metaData.putInt(META_DATA_PREFERENCE_TITLE, R.string.settings_label);
        metaData.putInt(META_DATA_PREFERENCE_SUMMARY, R.string.about_settings_summary);
        metaData.putInt(META_DATA_KEY_ORDER, 10);
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, KEY);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE, metaData);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(mApplication, 0, new Intent("test"), 0);
        tile.pendingIntentMap.put(UserHandle.CURRENT, pendingIntent);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getFragment()).isNull();
        assertThat(preference.getOnPreferenceClickListener()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(tile.getOrder());
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindIntent() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.metaData.putInt(META_DATA_KEY_ORDER, 10);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getFragment()).isNull();
        assertThat(preference.getOnPreferenceClickListener()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(tile.getOrder());
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindToProfileSelector() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));
        tile.userHandle.add(mock(UserHandle.class));

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);

        verify(mActivity).getSupportFragmentManager();
    }

    @Test
    public void bindPreference_noFragmentMetadataSingleUser_shouldBindToDirectLaunchIntent() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));

        when(mActivity.getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);

        verify(mFeatureFactory.metricsFeatureProvider).logStartedIntent(
                any(Intent.class),
                eq(MetricsEvent.SETTINGS_GESTURES));
        verify(mActivity)
                .startActivityAsUser(any(Intent.class), any(UserHandle.class));
    }

    @Test
    public void bindPreference_toInternalSettingActivity_shouldBindToDirectLaunchIntentAndNotLog() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.packageName = mApplication.getPackageName();
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.getOnPreferenceClickListener().onPreferenceClick(null);
        verify(mFeatureFactory.metricsFeatureProvider).logStartedIntent(
                any(Intent.class),
                anyInt());
        verify(mActivity)
                .startActivityAsUser(any(Intent.class), any(UserHandle.class));
    }

    @Test
    public void bindPreference_nullPreference_shouldIgnore() {
        final Tile tile = mock(Tile.class);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                null /* keys */, tile, "123", Preference.DEFAULT_ORDER);

        verifyNoInteractions(tile);
    }

    @Test
    public void bindPreference_withNullKeyNullPriority_shouldGenerateKeyAndPriority() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, null /* key */,
                Preference.DEFAULT_ORDER);

        assertThat(preference.getKey()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(Preference.DEFAULT_ORDER);
    }

    @Test
    public void bindPreference_noSummary_shouldSetNullSummary() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.metaData.remove(META_DATA_PREFERENCE_SUMMARY);

        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getSummary()).isNull();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindPreference_hasSummaryUri_shouldLoadSummaryFromContentProviderAndHaveObserver() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final String uriString = "content://com.android.settings/tile_summary";
        mActivityInfo.metaData.putString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, uriString);

        final List<DynamicDataObserver> observers = mImpl.bindPreferenceToTileAndGetObservers(
                mActivity, mFragment, mForceRoundedIcon, preference, tile, null /* key */,
                Preference.DEFAULT_ORDER);

        assertThat(observers.get(0).getUri().toString()).isEqualTo(uriString);
        assertThat(preference.getSummary()).isNotEqualTo(ShadowTileUtils.MOCK_TEXT);

        observers.get(0).updateUi();

        assertThat(preference.getSummary()).isEqualTo(ShadowTileUtils.MOCK_TEXT);
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindPreference_hasTitleUri_shouldLoadFromContentProviderAndHaveObserver() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final String uriString =  "content://com.android.settings/tile_title";
        mActivityInfo.metaData.putString(TileUtils.META_DATA_PREFERENCE_TITLE_URI, uriString);

        final List<DynamicDataObserver> observers = mImpl.bindPreferenceToTileAndGetObservers(
                mActivity, mFragment, mForceRoundedIcon, preference, tile, null /* key */,
                Preference.DEFAULT_ORDER);

        assertThat(observers.get(0).getUri().toString()).isEqualTo(uriString);
        assertThat(preference.getTitle()).isNotEqualTo(ShadowTileUtils.MOCK_TEXT);

        observers.get(0).updateUi();

        assertThat(preference.getTitle()).isEqualTo(ShadowTileUtils.MOCK_TEXT);
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindPreference_onCheckedChanged_shouldPutStateToContentProvider() {
        final SwitchPreference preference = new SwitchPreference(mApplication);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE,
                mSwitchMetaData);
        final Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_SWITCH_SET_CHECKED_ERROR, false);
        ShadowTileUtils.setResultBundle(bundle);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        preference.callChangeListener(false);

        assertThat(ShadowTileUtils.getProviderChecked()).isFalse();

        preference.callChangeListener(true);

        assertThat(ShadowTileUtils.getProviderChecked()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindPreference_onCheckedChangedError_shouldRevertCheckedState() {
        final SwitchPreference preference = new SwitchPreference(mApplication);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE,
                mSwitchMetaData);
        final Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_SWITCH_SET_CHECKED_ERROR, true);
        ShadowTileUtils.setResultBundle(bundle);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        preference.callChangeListener(true);

        assertThat(preference.isChecked()).isFalse();

        preference.callChangeListener(false);

        assertThat(preference.isChecked()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindPreference_callbackOnChanged_shouldLoadFromContentProvider() {
        final SwitchPreference preference = new SwitchPreference(mApplication);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE,
                mSwitchMetaData);
        final List<DynamicDataObserver> observers = mImpl.bindPreferenceToTileAndGetObservers(
                mActivity, mFragment, mForceRoundedIcon, preference, tile, null /* key */,
                Preference.DEFAULT_ORDER);
        observers.get(0).updateUi();

        ShadowTileUtils.setProviderChecked(false);
        observers.get(0).onDataChanged();

        assertThat(preference.isChecked()).isFalse();

        ShadowTileUtils.setProviderChecked(true);
        observers.get(0).onDataChanged();

        assertThat(preference.isChecked()).isTrue();
    }

    @Test
    public void bindPreference_withNullKeyTileKey_shouldUseTileKey() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getKey()).isEqualTo(tile.getKey(mContext));
    }

    @Test
    public void bindIcon_withStaticIcon_shouldLoadStaticIcon() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.packageName = mApplication.getPackageName();
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putInt(META_DATA_PREFERENCE_ICON, R.drawable.ic_add_40dp);

        mImpl.bindIcon(preference, tile, false /* forceRoundedIcon */);

        final Bitmap preferenceBmp = Utils.createIconWithDrawable(preference.getIcon()).getBitmap();
        final Drawable staticIcon = Icon.createWithResource(mActivityInfo.packageName,
                R.drawable.ic_add_40dp).loadDrawable(preference.getContext());
        final Bitmap staticIconBmp = Utils.createIconWithDrawable(staticIcon).getBitmap();
        assertThat(preferenceBmp.sameAs(staticIconBmp)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindIcon_withIconUri_shouldLoadIconFromContentProvider() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.packageName = mApplication.getPackageName();
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_ICON_URI,
                "content://com.android.settings/tile_icon");

        mImpl.bindIcon(preference, tile, false /* forceRoundedIcon */);

        assertThat(preference.getIcon()).isNotNull();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindIcon_withStaticIconAndIconUri_shouldLoadIconFromContentProvider() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.packageName = mApplication.getPackageName();
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putInt(META_DATA_PREFERENCE_ICON, R.drawable.ic_add_40dp);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_ICON_URI,
                "content://com.android.settings/tile_icon");

        mImpl.bindIcon(preference, tile, false /* forceRoundedIcon */);

        final Bitmap preferenceBmp = Utils.createIconWithDrawable(preference.getIcon()).getBitmap();
        final Drawable staticIcon = Icon.createWithResource(mActivityInfo.packageName,
                R.drawable.ic_add_40dp).loadDrawable(preference.getContext());
        final Bitmap staticIconBmp = Utils.createIconWithDrawable(staticIcon).getBitmap();
        assertThat(preferenceBmp.sameAs(staticIconBmp)).isFalse();

        final Pair<String, Integer> iconInfo = TileUtils.getIconFromUri(
                mContext, "pkg", null /* uri */, null /* providerMap */);
        final Drawable iconFromUri = Icon.createWithResource(iconInfo.first, iconInfo.second)
                .loadDrawable(preference.getContext());
        final Bitmap iconBmpFromUri = Utils.createIconWithDrawable(iconFromUri).getBitmap();
        assertThat(preferenceBmp.sameAs(iconBmpFromUri)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowTileUtils.class})
    public void bindIcon_noIcon_shouldNotLoadIcon() {
        final Preference preference = new Preference(mApplication);
        mActivityInfo.packageName = mApplication.getPackageName();
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");

        mImpl.bindIcon(preference, tile, false /* forceRoundedIcon */);

        assertThat(preference.getIcon()).isNull();
    }

    @Test
    public void bindPreference_withBaseOrder_shouldOffsetOrder() {
        final int baseOrder = 100;
        final Preference preference = new Preference(mApplication);
        mActivityInfo.metaData.putInt(META_DATA_KEY_ORDER, 10);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", baseOrder);

        assertThat(preference.getOrder()).isEqualTo(tile.getOrder() + baseOrder);
    }

    @Test
    public void bindPreference_withOrderMetadata_shouldUseOrderInMetadata() {
        final Preference preference = new Preference(mApplication);
        final int testOrder = -30;
        mActivityInfo.metaData.putInt(META_DATA_KEY_ORDER, 10);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putInt(META_DATA_KEY_ORDER, testOrder);
        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getOrder()).isEqualTo(testOrder);
    }

    @Test
    public void bindPreference_invalidOrderMetadata_shouldIgnore() {
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_KEY_ORDER, "hello");

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);

        assertThat(preference.getOrder()).isEqualTo(Preference.DEFAULT_ORDER);
    }

    @Test
    public void bindPreference_withIntentActionMetadata_shouldSetLaunchAction() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;
        mImpl.bindPreferenceToTileAndGetObservers(activity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        final Intent launchIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(launchIntent.getAction())
                .isEqualTo("TestAction");
        assertThat(
                launchIntent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, 0))
                .isEqualTo(MetricsEvent.SETTINGS_GESTURES);
    }

    /** This test is for large screen devices Activity embedding. */
    @Test
    @Config(shadows = {ShadowActivityEmbeddingUtils.class, ShadowSplitController.class})
    public void bindPreference_clickHighlightedPreference_shouldNotStartActivity() {
        ShadowSplitController.setIsActivityEmbedded(true);
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true);
        mFeatureFactory.searchFeatureProvider = new SearchFeatureProviderImpl();

        String clickPrefKey = "highlight_pref_key";
        String highlightMixinPrefKey = "highlight_pref_key";
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        Preference preference = new Preference(mApplication);
        Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;

        TopLevelSettings largeScreenTopLevelSettings = spy(new TopLevelSettings(
                new TestTopLevelHighlightMixin(highlightMixinPrefKey,
                true /* activityEmbedded */)));
        doReturn(true).when(largeScreenTopLevelSettings).isActivityEmbedded();
        largeScreenTopLevelSettings.setHighlightPreferenceKey(clickPrefKey);

        mImpl.bindPreferenceToTileAndGetObservers(activity, largeScreenTopLevelSettings,
                mForceRoundedIcon, preference, tile, clickPrefKey, Preference.DEFAULT_ORDER);
        preference.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getNextStartedActivityForResult()).isEqualTo(null);
    }

    /** This test is for large screen devices Activity embedding. */
    @Test
    @Config(shadows = {ShadowActivityEmbeddingUtils.class, ShadowSplitController.class})
    public void bindPreference_clickNotHighlightedPreference_shouldStartActivity() {
        ShadowSplitController.setIsActivityEmbedded(true);
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true);
        mFeatureFactory.searchFeatureProvider = new SearchFeatureProviderImpl();

        String clickPrefKey = "not_highlight_pref_key";
        String highlightMixinPrefKey = "highlight_pref_key";
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        Preference preference = new Preference(mApplication);
        Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;

        TopLevelSettings largeScreenTopLevelSettings = spy(new TopLevelSettings(
                new TestTopLevelHighlightMixin(highlightMixinPrefKey,
                true /* activityEmbedded */)));
        doReturn(true).when(largeScreenTopLevelSettings).isActivityEmbedded();
        largeScreenTopLevelSettings.setHighlightPreferenceKey(clickPrefKey);

        mImpl.bindPreferenceToTileAndGetObservers(activity, largeScreenTopLevelSettings,
                mForceRoundedIcon, preference, tile, clickPrefKey, Preference.DEFAULT_ORDER);
        preference.performClick();

        Intent launchIntent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
        assertThat(launchIntent.getAction()).isEqualTo("TestAction");
    }

    @Test
    public void clickPreference_withUnresolvableIntent_shouldNotLaunchAnything() {
        ReflectionHelpers.setField(
                mImpl, "mPackageManager", mApplication.getPackageManager());
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
        final Preference preference = new Preference(mApplication);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString("com.android.settings.intent.action", "TestAction");
        tile.userHandle = null;

        mImpl.bindPreferenceToTileAndGetObservers(activity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();

        final ShadowActivity.IntentForResult launchIntent =
                Shadows.shadowOf(activity).getNextStartedActivityForResult();

        assertThat(launchIntent).isNull();
    }

    @Test
    public void clickPreference_providerTileWithPendingIntent_singleUser_executesPendingIntent() {
        final Preference preference = new SwitchPreference(mApplication);
        Bundle metaData = new Bundle();
        metaData.putInt(META_DATA_PREFERENCE_TITLE, R.string.settings_label);
        metaData.putInt(META_DATA_PREFERENCE_SUMMARY, R.string.about_settings_summary);
        metaData.putInt(META_DATA_KEY_ORDER, 10);
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, KEY);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE, metaData);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(mApplication, 0, new Intent("test"), 0);
        tile.pendingIntentMap.put(UserHandle.CURRENT, pendingIntent);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();

        Intent nextStartedActivity =
                Shadows.shadowOf(mApplication).peekNextStartedActivity();
        assertThat(nextStartedActivity).isNotNull();
        assertThat(nextStartedActivity.getAction()).isEqualTo("test");
    }

    @Test
    public void clickPreference_providerTileWithPendingIntent_multiUser_showsProfileDialog() {
        final Preference preference = new SwitchPreference(mApplication);
        Bundle metaData = new Bundle();
        metaData.putInt(META_DATA_PREFERENCE_TITLE, R.string.settings_label);
        metaData.putInt(META_DATA_PREFERENCE_SUMMARY, R.string.about_settings_summary);
        metaData.putInt(META_DATA_KEY_ORDER, 10);
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, KEY);
        final Tile tile = new ProviderTile(mProviderInfo, CategoryKey.CATEGORY_HOMEPAGE, metaData);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(mApplication, 0, new Intent("test"), 0);
        tile.pendingIntentMap.put(UserHandle.CURRENT, pendingIntent);
        tile.pendingIntentMap.put(new UserHandle(10), pendingIntent);

        mImpl.bindPreferenceToTileAndGetObservers(mActivity, mFragment, mForceRoundedIcon,
                preference, tile, "123", Preference.DEFAULT_ORDER);
        preference.performClick();

        Fragment dialogFragment =
                mActivity.getSupportFragmentManager().findFragmentByTag("select_profile");
        assertThat(dialogFragment).isNotNull();
        Intent nextStartedActivity =
                Shadows.shadowOf(mApplication).peekNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_shouldShow() {
        ShadowUserManager.getShadow().addUser(10, "Someone", 0);

        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(new UserHandle(10));
        tile.userHandle = handles;
        mImpl.openTileIntent(mActivity, tile);

        verify(mActivity, never())
                .startActivity(any(Intent.class));
        verify(mActivity).getSupportFragmentManager();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_explicitMetadataShouldShow() {
        ShadowUserManager.getShadow().addUser(10, "Someone", 0);

        mActivityInfo.metaData.putString(META_DATA_KEY_PROFILE, PROFILE_ALL);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(new UserHandle(10));
        tile.userHandle = handles;
        mImpl.openTileIntent(mActivity, tile);

        verify(mActivity, never())
                .startActivity(any(Intent.class));
        verify(mActivity).getSupportFragmentManager();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_shouldNotShow() {
        ShadowUserManager.getShadow().addUser(10, "Someone", 0);

        mActivityInfo.metaData.putString(META_DATA_KEY_PROFILE, PROFILE_PRIMARY);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(new UserHandle(10));
        tile.userHandle = handles;
        mImpl.openTileIntent(mActivity, tile);

        verify(mActivity)
                .startActivity(any(Intent.class));
        verify(mActivity, never()).getSupportFragmentManager();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_validUserHandleShouldNotShow() {
        final int userId = 10;
        ShadowUserManager.getShadow().addUser(userId, "Someone", 0);

        final UserHandle userHandle = new UserHandle(userId);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.getIntent().putExtra(EXTRA_USER, userHandle);
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(userHandle);
        tile.userHandle = handles;

        mImpl.openTileIntent(mActivity, tile);

        final ArgumentCaptor<UserHandle> argument = ArgumentCaptor.forClass(UserHandle.class);
        verify(mActivity)
                .startActivityAsUser(any(Intent.class), argument.capture());
        assertThat(argument.getValue().getIdentifier()).isEqualTo(userId);
        verify(mActivity, never()).getSupportFragmentManager();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_invalidUserHandleShouldShow() {
        ShadowUserManager.getShadow().addUser(10, "Someone", 0);

        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.getIntent().putExtra(EXTRA_USER, new UserHandle(30));
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(new UserHandle(10));
        tile.userHandle = handles;

        mImpl.openTileIntent(mActivity, tile);

        verify(mActivity, never())
                .startActivityAsUser(any(Intent.class), any(UserHandle.class));
        verify(mActivity).getSupportFragmentManager();
    }

    @Test
    public void openTileIntent_profileSelectionDialog_unresolvableWorkProfileIntentShouldNotShow() {
        final int userId = 10;
        ShadowUserManager.getShadow().addUser(userId, "Someone", 0);
        final UserHandle userHandle = new UserHandle(userId);
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        final ArrayList<UserHandle> handles = new ArrayList<>();
        handles.add(new UserHandle(0));
        handles.add(userHandle);
        tile.userHandle = handles;
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), eq(0)))
                .thenReturn(new ResolveInfo());
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), eq(userId)))
                .thenReturn(null);

        mImpl.openTileIntent(mActivity, tile);

        final ArgumentCaptor<UserHandle> argument = ArgumentCaptor.forClass(UserHandle.class);
        verify(mActivity)
                .startActivityAsUser(any(Intent.class), argument.capture());
        assertThat(argument.getValue().getIdentifier()).isEqualTo(0);
        verify(mActivity, never()).getSupportFragmentManager();
    }

    private static class TestFragment extends DashboardFragment {

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.SETTINGS_GESTURES;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return R.xml.gestures;
        }

        @Override
        protected String getLogTag() {
            return "TestFragment";
        }
    }

    private static class TestTopLevelHighlightMixin extends TopLevelHighlightMixin {
        private final String mHighlightPreferenceKey;

        TestTopLevelHighlightMixin(String highlightPreferenceKey,
                boolean activityEmbedded) {
            super(activityEmbedded);
            mHighlightPreferenceKey = highlightPreferenceKey;
        }

        @Override
        public String getHighlightPreferenceKey() {
            return mHighlightPreferenceKey;
        }
    }
}
