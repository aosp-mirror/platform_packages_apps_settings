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

package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowEntityHeaderController.class)
public class AppInfoWithHeaderTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;

    private FakeFeatureFactory mFactory;
    private TestFragment mAppInfoWithHeader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);

        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mFactory.metricsFeatureProvider.getMetricsCategory(any(Object.class)))
                .thenReturn(MetricsProto.MetricsEvent.SETTINGS_APP_NOTIF_CATEGORY);
        mAppInfoWithHeader = new TestFragment();
        ShadowEntityHeaderController.setUseMock(mHeaderController);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void testAppHeaderIsAdded() {
        mAppInfoWithHeader.onActivityCreated(null);

        verify(mAppInfoWithHeader.mScreen).addPreference(any(LayoutPreference.class));
    }

    @Test
    public void packageRemoved_noAppEntry_shouldFinishActivity() {
        BroadcastReceiver packageRemovedReceiver =
                ReflectionHelpers.getField(mAppInfoWithHeader, "mPackageRemovedReceiver");
        ReflectionHelpers.setField(mAppInfoWithHeader, "mAppEntry", null);

        final Intent packageRemovedBroadcast = new Intent();
        packageRemovedBroadcast.setData(Uri.parse("package:com.android.settings"));
        packageRemovedReceiver.onReceive(RuntimeEnvironment.application, packageRemovedBroadcast);

        assertThat(mAppInfoWithHeader.mPackageRemovedCalled).isTrue();
    }

    @Test
    public void packageRemoved_appEntryMatchesPackageName_shouldFinishActivity() {
        BroadcastReceiver packageRemovedReceiver =
                ReflectionHelpers.getField(mAppInfoWithHeader, "mPackageRemovedReceiver");
        final ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "com.android.settings";
        ReflectionHelpers.setField(mAppInfoWithHeader, "mAppEntry", entry);

        final Intent packageRemovedBroadcast = new Intent();
        packageRemovedBroadcast.setData(Uri.parse("package:" + entry.info.packageName));
        packageRemovedReceiver.onReceive(RuntimeEnvironment.application, packageRemovedBroadcast);

        assertThat(mAppInfoWithHeader.mPackageRemovedCalled).isTrue();
    }

    public static class TestFragment extends AppInfoWithHeader {

        PreferenceManager mManager;
        PreferenceScreen mScreen;
        Context mShadowContext;
        boolean mPackageRemovedCalled;

        public TestFragment() {
            mPm = mock(PackageManager.class);
            mManager = mock(PreferenceManager.class);
            mScreen = mock(PreferenceScreen.class);
            mPackageInfo = new PackageInfo();
            mPackageInfo.applicationInfo = new ApplicationInfo();
            mShadowContext = ShadowApplication.getInstance().getApplicationContext();
            ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                    (InstantAppDataProvider) (info -> false));
            when(mManager.getContext()).thenReturn(mShadowContext);
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected boolean refreshUi() {
            return false;
        }

        @Override
        protected AlertDialog createDialog(int id, int errorCode) {
            return null;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mManager;
        }

        @Override
        public Context getContext() {
            return mShadowContext;
        }

        @Override
        protected void onPackageRemoved() {
            mPackageRemovedCalled = true;
        }
    }

}
