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

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DashboardCategory mDashboardCategory;
    @Mock
    private FakeFeatureFactory mFakeFeatureFactory;
    @Mock
    private ProgressiveDisclosureMixin mDisclosureMixin;
    private TestFragment mTestFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FeatureFactory.getFactory(mContext);
        mDashboardCategory.tiles = new ArrayList<>();
        mDashboardCategory.tiles.add(new Tile());
        mTestFragment = new TestFragment(ShadowApplication.getInstance().getApplicationContext());
        when(mFakeFeatureFactory.dashboardFeatureProvider.getProgressiveDisclosureMixin(
                any(Context.class), eq(mTestFragment), any(Bundle.class)))
                .thenReturn(mDisclosureMixin);
        when(mFakeFeatureFactory.dashboardFeatureProvider.getTilesForCategory(anyString()))
                .thenReturn(mDashboardCategory);
        mTestFragment.onAttach(ShadowApplication.getInstance().getApplicationContext());
        when(mContext.getPackageName()).thenReturn("TestPackage");
    }

    @Test
    public void testPreferenceControllerGetterSetter_shouldAddAndGetProperly() {
        final TestPreferenceController controller = new TestPreferenceController(mContext);
        mTestFragment.addPreferenceController(controller);

        final TestPreferenceController retrievedController = mTestFragment.getPreferenceController
                (TestPreferenceController.class);

        assertThat(controller).isSameAs(retrievedController);
    }

    @Test
    public void displayTilesAsPreference_shouldAddTilesWithIntent() {
        when(mFakeFeatureFactory.dashboardFeatureProvider.getTilesForCategory(anyString()))
                .thenReturn(mDashboardCategory);
        when(mFakeFeatureFactory.dashboardFeatureProvider.getDashboardKeyForTile(any(Tile.class)))
                .thenReturn("test_key");
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mDisclosureMixin).addPreference(any(PreferenceScreen.class),
                any(Preference.class));
    }

    @Test
    public void displayTilesAsPreference_shouldNotAddTilesWithoutIntent() {
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, never()).addPreference(any(Preference.class));
    }

    @Test
    public void displayTilesAsPreference_withEmptyCategory_shouldNotAddTiles() {
        mDashboardCategory.tiles = null;
        mTestFragment.onCreatePreferences(new Bundle(), "rootKey");

        verify(mTestFragment.mScreen, never()).addPreference(any(Preference.class));
    }

    @Test
    public void onAttach_shouldCreatePlaceholderPreferenceController() {
        final PreferenceController controller = mTestFragment.getPreferenceController(
                DashboardTilePlaceholderPreferenceController.class);

        assertThat(controller).isNotNull();
    }

    @Test
    public void updateState_skipUnavailablePrefs() {
        final List<PreferenceController> preferenceControllers = mTestFragment.mControllers;
        final PreferenceController mockController1 = mock(PreferenceController.class);
        final PreferenceController mockController2 = mock(PreferenceController.class);
        preferenceControllers.add(mockController1);
        preferenceControllers.add(mockController2);
        when(mockController1.isAvailable()).thenReturn(false);
        when(mockController2.isAvailable()).thenReturn(true);

        mTestFragment.onAttach(ShadowApplication.getInstance().getApplicationContext());
        mTestFragment.onResume();

        verify(mockController1, never()).getPreferenceKey();
        verify(mockController2).getPreferenceKey();
    }

    public static class TestPreferenceController extends PreferenceController {

        public TestPreferenceController(Context context) {
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
        private final List<PreferenceController> mControllers;

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
            return 0;
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
        protected List<PreferenceController> getPreferenceControllers(Context context) {
            return mControllers;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }
    }

}
