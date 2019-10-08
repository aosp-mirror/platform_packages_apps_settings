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
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.slices.BlockingSlicePrefController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
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
    private ActivityInfo mActivityInfo;
    private DashboardCategory mDashboardCategory;
    private Context mContext;
    private TestFragment mTestFragment;
    private List<AbstractPreferenceController> mControllers;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = "pkg";
        mActivityInfo.name = "class";
        mActivityInfo.metaData = new Bundle();
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "injected_tile_key");
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mDashboardCategory = new DashboardCategory("key");
        mDashboardCategory.addTile(new Tile(mActivityInfo, mDashboardCategory.key));
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

        assertThat(controller).isSameAs(retrievedController);
    }

    @Test
    public void testPreferenceControllerSetter_shouldAddAndNotReplace() {
        final TestPreferenceController controller1 = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller1);
        final TestPreferenceController controller2 = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller2);

        final TestPreferenceController retrievedController = mTestFragment.use
                (TestPreferenceController.class);

        assertThat(controller1).isSameAs(retrievedController);
    }

    @Test
    public void displayTilesAsPreference_shouldAddTilesWithIntent() {
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(mDashboardCategory);
        when(mFakeFeatureFactory.dashboardFeatureProvider
                .getDashboardKeyForTile(nullable(Tile.class)))
                .thenReturn("test_key");
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen).addPreference(nullable(Preference.class));
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
                .getDashboardKeyForTile(nullable(Tile.class)))
                .thenReturn("test_key");
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
    public void checkUiBlocker_noUiBlocker_controllerIsNull() {
        mTestFragment.mBlockerController = null;
        mControllers.add(new TestPreferenceController(mContext));

        mTestFragment.checkUiBlocker(mControllers);

        assertThat(mTestFragment.mBlockerController).isNull();
    }

    @Test
    public void checkUiBlocker_hasUiBlocker_controllerNotNull() {
        mTestFragment.mBlockerController = null;
        mControllers.add(new TestPreferenceController(mContext));
        mControllers.add(new BlockingSlicePrefController(mContext, "pref_key"));

        mTestFragment.checkUiBlocker(mControllers);

        assertThat(mTestFragment.mBlockerController).isNotNull();
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

    public static class TestFragment extends DashboardFragment {

        private final PreferenceManager mPreferenceManager;
        private final Context mContext;
        private final List<AbstractPreferenceController> mControllers;

        public final PreferenceScreen mScreen;

        public TestFragment(Context context) {
            mContext = context;
            mPreferenceManager = mock(PreferenceManager.class);
            mScreen = mock(PreferenceScreen.class);
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
    }
}
