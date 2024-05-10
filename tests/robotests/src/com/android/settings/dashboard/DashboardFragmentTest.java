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

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.DASHBOARD_CONTAINER;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_GROUP_KEY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_PENDING_INTENT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SWITCH_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager.OnActivityResultListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.slices.BlockingSlicePrefController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.ProviderTile;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class DashboardFragmentTest {

    @Mock
    private FakeFeatureFactory mFakeFeatureFactory;
    private DashboardCategory mDashboardCategory;
    private Context mContext;
    private TestFragment mTestFragment;
    private List<AbstractPreferenceController> mControllers;
    private ActivityTile mActivityTile;
    private ProviderTile mProviderTile;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "pkg";
        activityInfo.name = "class";
        activityInfo.metaData = new Bundle();
        activityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key");
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mDashboardCategory = new DashboardCategory("key");
        mActivityTile = new ActivityTile(activityInfo, mDashboardCategory.key);
        mDashboardCategory.addTile(mActivityTile);

        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = "pkg";
        providerInfo.name = "provider";
        providerInfo.authority = "authority";
        final Bundle metaData = new Bundle();
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key2");
        metaData.putString(META_DATA_PREFERENCE_SWITCH_URI, "uri");
        mProviderTile = new ProviderTile(providerInfo, mDashboardCategory.key, metaData);
        mDashboardCategory.addTile(mProviderTile);

        mTestFragment = new TestFragment(RuntimeEnvironment.application);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(mDashboardCategory);
        mTestFragment.onAttach(RuntimeEnvironment.application);
        when(mContext.getPackageName()).thenReturn("TestPackage");
        mControllers = new ArrayList<>();
    }

    @Test
    public void testPreferenceControllerGetterSetter_shouldAddAndGetProperly() {
        final TestPreferenceController controller = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller);

        final TestPreferenceController retrievedController = mTestFragment.use
                (TestPreferenceController.class);

        assertThat(controller).isSameInstanceAs(retrievedController);
    }

    @Test
    public void testPreferenceControllerSetter_shouldAddAndNotReplace() {
        final TestPreferenceController controller1 = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller1);
        final TestPreferenceController controller2 = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller2);

        final TestPreferenceController retrievedController = mTestFragment.use
                (TestPreferenceController.class);

        assertThat(controller1).isSameInstanceAs(retrievedController);
    }

    @Test
    public void useAll_returnsAllControllersOfType() {
        final TestPreferenceController controller1 = new TestPreferenceController(mContext);
        final TestPreferenceController controller2 = new TestPreferenceController(mContext);
        final SubTestPreferenceController controller3 = new SubTestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller1);
        mTestFragment.addPreferenceController(controller2);
        mTestFragment.addPreferenceController(controller3);

        final List<TestPreferenceController> retrievedControllers = mTestFragment.useAll(
                TestPreferenceController.class);

        assertThat(retrievedControllers).containsExactly(controller1, controller2);
    }

    @Test
    public void displayTilesAsPreference_shouldAddTilesWithIntent() {
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(mDashboardCategory);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(any(ActivityTile.class)))
                .thenReturn("test_key");
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(any(ProviderTile.class)))
                .thenReturn("test_key2");
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, times(2)).addPreference(nullable(Preference.class));
    }

    @Test
    public void displayTilesAsPreference_withGroup_shouldAddTilesIntoGroup() {
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = "pkg";
        providerInfo.name = "provider";
        providerInfo.authority = "authority";
        final Bundle groupTileMetaData = new Bundle();
        groupTileMetaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_group_key");
        ProviderTile groupTile = new ProviderTile(providerInfo, mDashboardCategory.key,
                groupTileMetaData);
        mDashboardCategory.addTile(groupTile);

        final Bundle subTileMetaData = new Bundle();
        subTileMetaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key3");
        subTileMetaData.putString(META_DATA_PREFERENCE_GROUP_KEY, "injected_tile_group_key");
        subTileMetaData.putParcelable(
                META_DATA_PREFERENCE_PENDING_INTENT,
                PendingIntent.getActivity(mContext, 0, new Intent(), 0));
        ProviderTile subTile = new ProviderTile(providerInfo, mDashboardCategory.key,
                subTileMetaData);
        mDashboardCategory.addTile(subTile);

        PreferenceCategory groupPreference = mock(PreferenceCategory.class);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(mDashboardCategory);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(any(Tile.class)))
                .then(invocation -> ((Tile) invocation.getArgument(0)).getKey(mContext));
        when(mTestFragment.mScreen.findPreference("injected_tile_group_key"))
                .thenReturn(groupPreference);
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, times(3)).addPreference(nullable(Preference.class));
        verify(groupPreference).addPreference(nullable(Preference.class));
    }

    @Test
    public void displayTilesAsPreference_shouldNotAddTilesWithoutIntent() {
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, never()).addPreference(nullable(Preference.class));
    }

    @Test
    public void displayTilesAsPreference_withEmptyCategory_shouldNotAddTiles() {
        mDashboardCategory.removeTile(0);
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, never()).addPreference(nullable(Preference.class));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void displayTilesAsPreference_shouldNotAddSuppressedTiles() {
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(mDashboardCategory);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(any(ActivityTile.class)))
                .thenReturn("test_key");
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(any(ProviderTile.class)))
                .thenReturn("test_key2");
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, never()).addPreference(nullable(Preference.class));
    }

    @Test
    public void onAttach_shouldCreatePlaceholderPreferenceController() {
        final AbstractPreferenceController controller = mTestFragment.use(
                DashboardTilePlaceholderPreferenceController.class);

        assertThat(controller).isNotNull();
    }

    @Test
    @Config(shadows = ShadowPreferenceFragmentCompat.class)
    public void onStart_shouldRegisterDynamicDataObservers() {
        final DynamicDataObserver observer = new TestDynamicDataObserver();
        mTestFragment.mDashboardTilePrefKeys.put("key", Arrays.asList(observer));

        mTestFragment.onStart();

        verify(mTestFragment.getContentResolver()).registerContentObserver(observer.getUri(), false,
                observer);
    }

    @Test
    @Config(shadows = ShadowPreferenceFragmentCompat.class)
    public void onStop_shouldUnregisterDynamicDataObservers() {
        final DynamicDataObserver observer = new TestDynamicDataObserver();
        mTestFragment.registerDynamicDataObservers(Arrays.asList(observer));

        mTestFragment.onStop();

        verify(mTestFragment.getContentResolver()).unregisterContentObserver(observer);
    }

    @Test
    public void updateState_skipUnavailablePrefs() {
        final List<AbstractPreferenceController> preferenceControllers = mTestFragment.mControllers;
        final AbstractPreferenceController mockController1 =
                mock(AbstractPreferenceController.class);
        final AbstractPreferenceController mockController2 =
                mock(AbstractPreferenceController.class);
        when(mockController1.getPreferenceKey()).thenReturn("key1");
        when(mockController2.getPreferenceKey()).thenReturn("key2");
        preferenceControllers.add(mockController1);
        preferenceControllers.add(mockController2);
        when(mockController1.isAvailable()).thenReturn(false);
        when(mockController2.isAvailable()).thenReturn(true);
        mTestFragment.onAttach(RuntimeEnvironment.application);
        mTestFragment.onResume();

        verify(mockController1).getPreferenceKey();
        verify(mockController2, times(2)).getPreferenceKey();
    }

    @Test
    public void updateState_doesNotSkipControllersOfSameClass() {
        final AbstractPreferenceController mockController1 =
                mock(AbstractPreferenceController.class);
        final AbstractPreferenceController mockController2 =
                mock(AbstractPreferenceController.class);
        mTestFragment.addPreferenceController(mockController1);
        mTestFragment.addPreferenceController(mockController2);
        when(mockController1.isAvailable()).thenReturn(true);
        when(mockController2.isAvailable()).thenReturn(true);

        mTestFragment.updatePreferenceStates();

        verify(mockController1).getPreferenceKey();
        verify(mockController2).getPreferenceKey();
    }

    @Test
    public void onExpandButtonClick_shouldLogAdvancedButtonExpand() {
        final MetricsFeatureProvider metricsFeatureProvider
                = mFakeFeatureFactory.getMetricsFeatureProvider();
        mTestFragment.onExpandButtonClick();

        verify(metricsFeatureProvider).action(SettingsEnums.PAGE_UNKNOWN,
                MetricsEvent.ACTION_SETTINGS_ADVANCED_BUTTON_EXPAND,
                DASHBOARD_CONTAINER, null, 0);
    }

    @Test
    public void updatePreferenceVisibility_prefKeyNull_shouldNotCrash() {
        final Map<Class, List<AbstractPreferenceController>> prefControllers = new HashMap<>();
        final List<AbstractPreferenceController> controllerList = new ArrayList<>();
        controllerList.add(new TestPreferenceController(mContext));
        prefControllers.put(TestPreferenceController.class, controllerList);
        mTestFragment.mBlockerController = new UiBlockerController(Arrays.asList("pref_key"));

        // Should not crash
        mTestFragment.updatePreferenceVisibility(prefControllers);
    }

    @Test
    public void forceUpdatePreferences_prefKeyNull_shouldNotCrash() {
        mTestFragment.addPreferenceController(new TestPreferenceController(mContext));

        // Should not crash
        mTestFragment.forceUpdatePreferences();
    }

    @Test
    public void checkUiBlocker_noUiBlocker_controllerIsNull() {
        mTestFragment.mBlockerController = null;
        mControllers.add(new TestPreferenceController(mContext));

        mTestFragment.checkUiBlocker(mControllers);

        assertThat(mTestFragment.mBlockerController).isNull();
    }

    @Test
    public void checkUiBlocker_hasUiBlockerAndControllerIsAvailable_controllerNotNull() {
        final BlockingSlicePrefController controller =
                new BlockingSlicePrefController(mContext, "pref_key");
        controller.setSliceUri(Uri.parse("testUri"));
        mTestFragment.mBlockerController = null;
        mControllers.add(new TestPreferenceController(mContext));
        mControllers.add(controller);

        mTestFragment.checkUiBlocker(mControllers);

        assertThat(mTestFragment.mBlockerController).isNotNull();
    }

    @Test
    public void checkUiBlocker_hasUiBlockerAndControllerIsNotAvailable_controllerIsNull() {
        mTestFragment.mBlockerController = null;
        mControllers.add(new TestPreferenceController(mContext));
        mControllers.add(new BlockingSlicePrefController(mContext, "pref_key"));

        mTestFragment.checkUiBlocker(mControllers);

        assertThat(mTestFragment.mBlockerController).isNull();
    }

    @Ignore("b/313569889")
    @Test
    public void createPreference_isProviderTile_returnSwitchPreference() {
        final Preference pref = mTestFragment.createPreference(mProviderTile);

        assertThat(pref).isInstanceOf(SwitchPreference.class);
    }

    @Test
    public void createPreference_isActivityTile_returnPreference() {
        final Preference pref = mTestFragment.createPreference(mActivityTile);

        assertThat(pref).isInstanceOf(Preference.class);
        assertThat(pref).isNotInstanceOf(PrimarySwitchPreference.class);
        assertThat(pref).isNotInstanceOf(SwitchPreference.class);
        assertThat(pref.getWidgetLayoutResource()).isEqualTo(0);
    }

    @Test
    public void createPreference_isActivityTileAndHasSwitch_returnPrimarySwitchPreference() {
        mActivityTile.getMetaData().putString(META_DATA_PREFERENCE_SWITCH_URI, "uri");

        final Preference pref = mTestFragment.createPreference(mActivityTile);

        assertThat(pref).isInstanceOf(PrimarySwitchPreference.class);
    }

    @Test
    public void createPreference_isProviderTileWithPendingIntent_returnPreferenceWithIcon() {
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = "pkg";
        providerInfo.name = "provider";
        providerInfo.authority = "authority";
        final Bundle metaData = new Bundle();
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key2");
        ProviderTile providerTile = new ProviderTile(providerInfo, mDashboardCategory.key,
                metaData);
        providerTile.pendingIntentMap.put(
                UserHandle.CURRENT, PendingIntent.getActivity(mContext, 0, new Intent(), 0));

        final Preference pref = mTestFragment.createPreference(providerTile);

        assertThat(pref).isInstanceOf(Preference.class);
        assertThat(pref).isNotInstanceOf(PrimarySwitchPreference.class);
        assertThat(pref).isNotInstanceOf(SwitchPreference.class);
        assertThat(pref.getWidgetLayoutResource())
                .isEqualTo(R.layout.preference_external_action_icon);
    }

    @Test
    public void createPreference_isProviderTileWithPendingIntentAndSwitch_returnPrimarySwitch() {
        mProviderTile.pendingIntentMap.put(
                UserHandle.CURRENT, PendingIntent.getActivity(mContext, 0, new Intent(), 0));

        final Preference pref = mTestFragment.createPreference(mProviderTile);

        assertThat(pref).isInstanceOf(PrimarySwitchPreference.class);
    }

    @Test
    public void createPreference_isGroupTile_returnPreferenceCategory_logTileAdded() {
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.packageName = "pkg";
        providerInfo.name = "provider";
        providerInfo.authority = "authority";
        final Bundle metaData = new Bundle();
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key2");
        ProviderTile providerTile =
                new ProviderTile(providerInfo, mDashboardCategory.key, metaData);
        MetricsFeatureProvider metricsFeatureProvider =
                mFakeFeatureFactory.getMetricsFeatureProvider();
        when(metricsFeatureProvider.getAttribution(any())).thenReturn(123);

        final Preference pref = mTestFragment.createPreference(providerTile);

        assertThat(pref).isInstanceOf(PreferenceCategory.class);
        verify(metricsFeatureProvider)
                .action(
                        123,
                        SettingsEnums.ACTION_SETTINGS_GROUP_TILE_ADDED_TO_SCREEN,
                        mTestFragment.getMetricsCategory(),
                        "injected_tile_key2",
                        0);
    }

    @Test
    public void onActivityResult_test() {
        final int requestCode = 10;
        final int resultCode = 1;
        final TestOnActivityResultPreferenceController activityResultPref = spy(
                new TestOnActivityResultPreferenceController(mContext));
        mTestFragment.addPreferenceController(activityResultPref);

        mTestFragment.onActivityResult(requestCode, resultCode, null);

        verify(activityResultPref).onActivityResult(requestCode, resultCode, null);
    }

    public static class TestPreferenceController extends AbstractPreferenceController
            implements PreferenceControllerMixin {

        private TestPreferenceController(Context context) {
            super(context);
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        public void updateNonIndexableKeys(List<String> keys) {
        }
    }

    public static class SubTestPreferenceController extends TestPreferenceController {

        private SubTestPreferenceController(Context context) {
            super(context);
        }
    }

    public static class TestOnActivityResultPreferenceController extends
            TestPreferenceController implements OnActivityResultListener {

        private TestOnActivityResultPreferenceController(Context context) {
            super(context);
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            return true;
        }
    }

    private static class TestFragment extends DashboardFragment {

        private final PreferenceManager mPreferenceManager;
        private final Context mContext;
        private final List<AbstractPreferenceController> mControllers;
        private final ContentResolver mContentResolver;

        public final PreferenceScreen mScreen;

        public TestFragment(Context context) {
            mContext = context;
            mPreferenceManager = mock(PreferenceManager.class);
            mScreen = mock(PreferenceScreen.class);
            mContentResolver = mock(ContentResolver.class);
            mControllers = new ArrayList<>();

            when(mPreferenceManager.getContext()).thenReturn(mContext);
            ReflectionHelpers.setField(
                    this, "mVisibilityLoggerMixin", mock(VisibilityLoggerMixin.class));
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public int getMetricsCategory() {
            return DASHBOARD_CONTAINER;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }

        @Override
        protected String getLogTag() {
            return "TEST_FRAG";
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return mControllers;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        protected ContentResolver getContentResolver() {
            return mContentResolver;
        }

    }

    private static class TestDynamicDataObserver extends DynamicDataObserver {

        @Override
        public Uri getUri() {
            return Uri.parse("content://abc");
        }

        @Override
        public void onDataChanged() {
        }
    }

    @Implements(PreferenceFragmentCompat.class)
    public static class ShadowPreferenceFragmentCompat {

        @Implementation
        public void onStart() {
            // do nothing
        }

        @Implementation
        public void onStop() {
            // do nothing
        }
    }
}
