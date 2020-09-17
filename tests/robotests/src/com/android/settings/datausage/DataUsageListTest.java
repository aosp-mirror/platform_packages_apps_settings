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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.AppItem;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.net.NetworkCycleChartData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DataUsageListTest {

    @Mock
    private MobileDataEnabledListener mMobileDataEnabledListener;
    @Mock
    private TemplatePreference.NetworkServices mNetworkServices;
    @Mock
    private LoaderManager mLoaderManager;

    private Activity mActivity;
    private DataUsageList mDataUsageList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        final ActivityController<Activity> mActivityController =
                Robolectric.buildActivity(Activity.class);
        mActivity = spy(mActivityController.get());
        mNetworkServices.mPolicyEditor = mock(NetworkPolicyEditor.class);
        mDataUsageList = spy(DataUsageList.class);
        mDataUsageList.mDataStateListener = mMobileDataEnabledListener;

        doReturn(mActivity).when(mDataUsageList).getContext();
        ReflectionHelpers.setField(mDataUsageList, "mDataStateListener",
                mMobileDataEnabledListener);
        ReflectionHelpers.setField(mDataUsageList, "services", mNetworkServices);
        doReturn(mLoaderManager).when(mDataUsageList).getLoaderManager();
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
    public void startAppDataUsage_shouldAddCyclesInfoToLaunchArguments() {
        final long startTime = 1521583200000L;
        final long endTime = 1521676800000L;
        final List<NetworkCycleChartData> data = new ArrayList<>();
        final NetworkCycleChartData.Builder builder = new NetworkCycleChartData.Builder();
        builder.setStartTime(startTime)
                .setEndTime(endTime);
        data.add(builder.build());
        ReflectionHelpers.setField(mDataUsageList, "mCycleData", data);
        final Spinner spinner = mock(Spinner.class);
        when(spinner.getSelectedItemPosition()).thenReturn(0);
        ReflectionHelpers.setField(mDataUsageList, "mCycleSpinner", spinner);
        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        mDataUsageList.startAppDataUsage(new AppItem());

        verify(mActivity).startActivity(intent.capture());
        final Bundle arguments =
                intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(arguments.getLong(AppDataUsage.ARG_SELECTED_CYCLE)).isEqualTo(endTime);
        final ArrayList<Long> cycles =
                (ArrayList) arguments.getSerializable(AppDataUsage.ARG_NETWORK_CYCLES);
        assertThat(cycles).hasSize(2);
        assertThat(cycles.get(0)).isEqualTo(endTime);
        assertThat(cycles.get(1)).isEqualTo(startTime);
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
        final LoadingViewController loadingViewController = mock(LoadingViewController.class);
        mDataUsageList.mLoadingViewController = loadingViewController;
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
        verify(mLoaderManager).destroyLoader(DataUsageList.LOADER_SUMMARY);
    }

    private View getHeader() {
        final View rootView = LayoutInflater.from(mActivity)
                .inflate(R.layout.preference_list_fragment, null, false);
        final FrameLayout pinnedHeader = rootView.findViewById(R.id.pinned_header);
        final View header = mActivity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, pinnedHeader, false);

        return header;
    }

    private Spinner getSpinner(View header) {
        final Spinner spinner = header.findViewById(R.id.filter_spinner);
        return spinner;
    }
}
