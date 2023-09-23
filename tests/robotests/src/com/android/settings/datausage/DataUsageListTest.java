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
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.datausage.lib.BillingCycleRepository;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;

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

@RunWith(RobolectricTestRunner.class)
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
        mDataUsageList.mTemplate = mock(NetworkTemplate.class);

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
    }

    @Test
    @Config(shadows = ShadowDataUsageBaseFragment.class)
    public void onCreate_isNotGuestUser_shouldNotFinish() {
        doReturn(false).when(mUserManager).isGuestUser();
        doNothing().when(mDataUsageList).processArgument();

        mDataUsageList.onCreate(null);

        verify(mDataUsageList, never()).finish();
    }

    @Test
    @Config(shadows = ShadowDataUsageBaseFragment.class)
    public void onCreate_isGuestUser_shouldFinish() {
        doReturn(true).when(mUserManager).isGuestUser();

        mDataUsageList.onCreate(null);

        verify(mDataUsageList).finish();
    }

    @Test
    public void resume_shouldListenDataStateChange() {
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
    public void processArgument_shouldGetNetworkTypeFromArgument() {
        final Bundle args = new Bundle();
        args.putInt(DataUsageList.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_WIFI);
        args.putInt(DataUsageList.EXTRA_SUB_ID, 3);
        mDataUsageList.setArguments(args);

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mNetworkType).isEqualTo(ConnectivityManager.TYPE_WIFI);
    }

    @Test
    public void processArgument_fromIntent_shouldGetTemplateFromIntent() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mock(NetworkTemplate.class));
        intent.putExtra(Settings.EXTRA_SUB_ID, 3);
        when(activity.getIntent()).thenReturn(intent);
        doReturn(activity).when(mDataUsageList).getActivity();

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mTemplate).isNotNull();
        assertThat(mDataUsageList.mSubId).isEqualTo(3);
    }

    @Test
    public void onViewCreated_shouldHideCycleSpinner() {
        final View view = new View(mActivity);
        final View header = getHeader();
        final Spinner spinner = getSpinner(header);
        spinner.setVisibility(View.VISIBLE);
        doReturn(header).when(mDataUsageList).setPinnedHeaderView(anyInt());
        doReturn(view).when(mDataUsageList).getView();

        mDataUsageList.onViewCreated(view, null);

        assertThat(spinner.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onLoadFinished_networkCycleDataCallback_shouldShowCycleSpinner() {
        final Spinner spinner = getSpinner(getHeader());
        spinner.setVisibility(View.INVISIBLE);
        mDataUsageList.mCycleSpinner = spinner;
        assertThat(spinner.getVisibility()).isEqualTo(View.INVISIBLE);
        doNothing().when(mDataUsageList).updatePolicy();

        mDataUsageList.mNetworkCycleDataCallbacks.onLoadFinished(null, null);

        assertThat(spinner.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onPause_shouldDestroyLoaders() {
        mDataUsageList.onPause();

        verify(mLoaderManager).destroyLoader(DataUsageList.LOADER_CHART_DATA);
    }

    private View getHeader() {
        final View rootView = LayoutInflater.from(mActivity)
                .inflate(R.layout.preference_list_fragment, null, false);
        final FrameLayout pinnedHeader = rootView.findViewById(R.id.pinned_header);

        return mActivity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, pinnedHeader, false);
    }

    private Spinner getSpinner(View header) {
        return header.findViewById(R.id.filter_spinner);
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

        @NonNull
        @Override
        BillingCycleRepository createBillingCycleRepository() {
            return mBillingCycleRepository;
        }
    }
}
