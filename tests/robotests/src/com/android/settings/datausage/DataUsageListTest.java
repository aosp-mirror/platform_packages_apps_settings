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
 */

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.android.settings.datausage.lib.BillingCycleRepository;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.net.NetworkCycleChartData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = DataUsageListTest.ShadowDataUsageBaseFragment.class)
public class DataUsageListTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private MobileDataEnabledListener mMobileDataEnabledListener;
    @Mock
    private TemplatePreference.NetworkServices mNetworkServices;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private BillingCycleRepository mBillingCycleRepository;
    @Mock
    private DataUsageListHeaderController mDataUsageListHeaderController;

    private Activity mActivity;

    @Spy
    private TestDataUsageList mDataUsageList;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        final ActivityController<Activity> mActivityController =
                Robolectric.buildActivity(Activity.class);
        mActivity = spy(mActivityController.get());
        mNetworkServices.mPolicyEditor = mock(NetworkPolicyEditor.class);
        mDataUsageList.mDataStateListener = mMobileDataEnabledListener;

        doReturn(mActivity).when(mDataUsageList).getContext();
        doReturn(mUserManager).when(mActivity).getSystemService(UserManager.class);
        doReturn(false).when(mUserManager).isGuestUser();
        ReflectionHelpers.setField(mDataUsageList, "mDataStateListener",
                mMobileDataEnabledListener);
        ReflectionHelpers.setField(mDataUsageList, "services", mNetworkServices);
        doReturn(mLoaderManager).when(mDataUsageList).getLoaderManager();
        mDataUsageList.mLoadingViewController = mock(LoadingViewController.class);
        doNothing().when(mDataUsageList).updateSubscriptionInfoEntity();
        when(mBillingCycleRepository.isBandwidthControlEnabled()).thenReturn(true);
        mDataUsageList.mDataUsageListHeaderController = mDataUsageListHeaderController;
    }

    @Test
    public void onCreate_isNotGuestUser_shouldNotFinish() {
        mDataUsageList.mTemplate = mock(NetworkTemplate.class);
        doReturn(false).when(mUserManager).isGuestUser();
        doNothing().when(mDataUsageList).processArgument();

        mDataUsageList.onCreate(null);

        verify(mDataUsageList, never()).finish();
    }

    @Test
    public void onCreate_isGuestUser_shouldFinish() {
        doReturn(true).when(mUserManager).isGuestUser();

        mDataUsageList.onCreate(null);

        verify(mDataUsageList).finish();
    }

    @Test
    public void resume_shouldListenDataStateChange() {
        mDataUsageList.onCreate(null);
        ReflectionHelpers.setField(
                mDataUsageList, "mVisibilityLoggerMixin", mock(VisibilityLoggerMixin.class));
        ReflectionHelpers.setField(
                mDataUsageList, "mPreferenceManager", mock(PreferenceManager.class));

        mDataUsageList.onResume();

        verify(mMobileDataEnabledListener).start(anyInt());

        mDataUsageList.onPause();
    }

    @Test
    public void pause_shouldUnlistenDataStateChange() {
        mDataUsageList.onCreate(null);
        ReflectionHelpers.setField(
                mDataUsageList, "mVisibilityLoggerMixin", mock(VisibilityLoggerMixin.class));
        ReflectionHelpers.setField(
                mDataUsageList, "mPreferenceManager", mock(PreferenceManager.class));

        mDataUsageList.onResume();
        mDataUsageList.onPause();

        verify(mMobileDataEnabledListener).stop();
    }

    @Test
    public void processArgument_shouldGetTemplateFromArgument() {
        final Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mock(NetworkTemplate.class));
        args.putInt(DataUsageList.EXTRA_SUB_ID, 3);
        mDataUsageList.setArguments(args);

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mTemplate).isNotNull();
        assertThat(mDataUsageList.mSubId).isEqualTo(3);
    }

    @Test
    public void processArgument_fromIntent_shouldGetTemplateFromIntent() {
        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mock(NetworkTemplate.class));
        intent.putExtra(Settings.EXTRA_SUB_ID, 3);
        doReturn(intent).when(mDataUsageList).getIntent();

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mTemplate).isNotNull();
        assertThat(mDataUsageList.mSubId).isEqualTo(3);
    }

    @Test
    public void onLoadFinished_networkCycleDataCallback_shouldShowCycleSpinner() {
        mDataUsageList.mTemplate = mock(NetworkTemplate.class);
        mDataUsageList.onCreate(null);
        mDataUsageList.updatePolicy();
        List<NetworkCycleChartData> mockData = Collections.emptyList();

        mDataUsageList.mNetworkCycleDataCallbacks.onLoadFinished(null, mockData);

        verify(mDataUsageListHeaderController).updateCycleData(mockData);
        verify(mDataUsageListHeaderController).setConfigButtonVisible(true);
    }

    @Test
    public void onPause_shouldDestroyLoaders() {
        mDataUsageList.onPause();

        verify(mLoaderManager).destroyLoader(DataUsageList.LOADER_CHART_DATA);
    }

    @Implements(DataUsageBaseFragment.class)
    public static class ShadowDataUsageBaseFragment {
        @Implementation
        public void onCreate(Bundle icicle) {
            // do nothing
        }
    }

    public class TestDataUsageList extends DataUsageList {
        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return mock(clazz);
        }

        @Override
        public <T extends Preference> T findPreference(CharSequence key) {
            if (key.toString().equals("chart_data")) {
                return (T) mock(ChartDataUsagePreference.class);
            }
            return (T) mock(Preference.class);
        }

        @Override
        public Intent getIntent() {
            return new Intent();
        }

        @NonNull
        @Override
        BillingCycleRepository createBillingCycleRepository() {
            return mBillingCycleRepository;
        }

        @Override
        boolean isBillingCycleModifiable() {
            return true;
        }
    }
}
