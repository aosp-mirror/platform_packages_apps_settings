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

package com.android.settings.applications.manageapplications;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_ALL;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MAIN;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_NOTIFICATION;
import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.UserManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
public class ManageApplicationsTest {

    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private Menu mMenu;
    private MenuItem mAppReset;
    private MenuItem mSortRecent;
    private MenuItem mSortFrequent;
    private ManageApplications mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppReset = new RoboMenuItem(R.id.reset_app_preferences);
        mSortRecent = new RoboMenuItem(R.id.sort_order_recent_notification);
        mSortFrequent = new RoboMenuItem(R.id.sort_order_frequent_notification);
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mState);
        when(mState.newSession(any())).thenReturn(mSession);
        when(mState.getBackgroundLooper()).thenReturn(Looper.myLooper());

        mFragment = new ManageApplications();
    }

    @Test
    public void updateMenu_mainListType_showAppReset() {
        setUpOptionMenus();
        ReflectionHelpers.setField(mFragment, "mListType", LIST_TYPE_MAIN);
        ReflectionHelpers.setField(mFragment, "mOptionsMenu", mMenu);

        mFragment.updateOptionsMenu();
        assertThat(mMenu.findItem(R.id.reset_app_preferences).isVisible()).isTrue();
    }

    @Test
    public void updateMenu_batteryListType_hideAppReset() {
        setUpOptionMenus();
        ReflectionHelpers.setField(mFragment, "mListType", ManageApplications.LIST_TYPE_HIGH_POWER);
        ReflectionHelpers.setField(mFragment, "mOptionsMenu", mMenu);

        mFragment.updateOptionsMenu();
        assertThat(mMenu.findItem(R.id.reset_app_preferences).isVisible()).isFalse();
    }

    @Test
    public void updateMenu_hideNotificationOptions() {
        setUpOptionMenus();
        ReflectionHelpers.setField(mFragment, "mListType", LIST_TYPE_NOTIFICATION);
        ReflectionHelpers.setField(mFragment, "mOptionsMenu", mMenu);

        mFragment.updateOptionsMenu();
        assertThat(mMenu.findItem(R.id.sort_order_recent_notification).isVisible()).isFalse();
        assertThat(mMenu.findItem(R.id.sort_order_frequent_notification).isVisible()).isFalse();
    }


    @Test
    public void onCreateView_shouldNotShowLoadingContainer() {
        final ManageApplications fragment = spy(new ManageApplications());
        ReflectionHelpers.setField(fragment, "mResetAppsHelper",
                mock(ResetAppsHelper.class));
        doNothing().when(fragment).createHeader();

        final LayoutInflater layoutInflater = mock(LayoutInflater.class);
        final View view = mock(View.class);
        final View loadingContainer = mock(View.class);
        when(layoutInflater.inflate(anyInt(), eq(null))).thenReturn(view);
        when(view.findViewById(R.id.loading_container)).thenReturn(loadingContainer);

        fragment.onCreateView(layoutInflater, mock(ViewGroup.class), null);

        verify(loadingContainer, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void updateLoading_appLoaded_shouldNotDelayCallToHandleLoadingContainer() {
        final ManageApplications fragment = mock(ManageApplications.class);
        ReflectionHelpers.setField(fragment, "mLoadingContainer", mock(View.class));
        ReflectionHelpers.setField(fragment, "mListContainer", mock(View.class));
        when(fragment.getActivity()).thenReturn(mock(Activity.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, fragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        final LoadingViewController loadingViewController =
                mock(LoadingViewController.class);
        ReflectionHelpers.setField(adapter, "mLoadingViewController", loadingViewController);

        // app loading completed
        ReflectionHelpers.setField(adapter, "mHasReceivedLoadEntries", true);
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        appList.add(mock(ApplicationsState.AppEntry.class));
        when(mSession.getAllApps()).thenReturn(appList);

        adapter.updateLoading();

        verify(loadingViewController, never()).showLoadingViewDelayed();
    }

    @Test
    public void updateLoading_appNotLoaded_shouldDelayCallToHandleLoadingContainer() {
        final ManageApplications fragment = mock(ManageApplications.class);
        ReflectionHelpers.setField(fragment, "mLoadingContainer", mock(View.class));
        ReflectionHelpers.setField(fragment, "mListContainer", mock(View.class));
        when(fragment.getActivity()).thenReturn(mock(Activity.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, fragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        final LoadingViewController loadingViewController =
                mock(LoadingViewController.class);
        ReflectionHelpers.setField(adapter, "mLoadingViewController", loadingViewController);

        // app loading not yet completed
        ReflectionHelpers.setField(adapter, "mHasReceivedLoadEntries", false);

        adapter.updateLoading();

        verify(loadingViewController).showLoadingViewDelayed();
    }

    @Test
    public void shouldUseStableItemHeight_mainType_yes() {
        assertThat(ManageApplications.ApplicationsAdapter.shouldUseStableItemHeight(
                LIST_TYPE_MAIN))
                .isTrue();
        assertThat(ManageApplications.ApplicationsAdapter.shouldUseStableItemHeight(
                LIST_TYPE_NOTIFICATION))
                .isFalse();
    }

    @Test
    public void onRebuildComplete_shouldHideLoadingView() {
        final Context context = RuntimeEnvironment.application;
        final ManageApplications fragment = mock(ManageApplications.class);
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final View emptyView = mock(View.class);
        ReflectionHelpers.setField(fragment, "mRecyclerView", recyclerView);
        ReflectionHelpers.setField(fragment, "mEmptyView", emptyView);
        final View loadingContainer = mock(View.class);
        when(loadingContainer.getContext()).thenReturn(context);
        final View listContainer = mock(View.class);
        when(listContainer.getVisibility()).thenReturn(View.INVISIBLE);
        when(listContainer.getContext()).thenReturn(context);
        ReflectionHelpers.setField(fragment, "mLoadingContainer", loadingContainer);
        ReflectionHelpers.setField(fragment, "mListContainer", listContainer);
        when(fragment.getActivity()).thenReturn(mock(Activity.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, fragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        final LoadingViewController loadingViewController =
                mock(LoadingViewController.class);
        ReflectionHelpers.setField(adapter, "mLoadingViewController", loadingViewController);
        ReflectionHelpers.setField(adapter, "mAppFilter",
                AppFilterRegistry.getInstance().get(FILTER_APPS_ALL));

        // app loading not yet completed
        ReflectionHelpers.setField(adapter, "mHasReceivedLoadEntries", false);
        adapter.updateLoading();

        // app loading completed
        ReflectionHelpers.setField(adapter, "mHasReceivedLoadEntries", true);
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        appList.add(mock(ApplicationsState.AppEntry.class));
        when(mSession.getAllApps()).thenReturn(appList);

        adapter.onRebuildComplete(null);

        verify(loadingViewController).showContent(true /* animate */);
    }

    @Test
    public void notifyItemChange_recyclerViewIdle_shouldNotify() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState,
                        mock(ManageApplications.class),
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));

        adapter.onAttachedToRecyclerView(recyclerView);
        adapter.mOnScrollListener.onScrollStateChanged(recyclerView, SCROLL_STATE_IDLE);
        adapter.mOnScrollListener.postNotifyItemChange(0 /* index */);

        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void notifyItemChange_recyclerViewScrolling_shouldNotifyWhenIdle() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState,
                        mock(ManageApplications.class),
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));

        adapter.onAttachedToRecyclerView(recyclerView);
        adapter.mOnScrollListener.onScrollStateChanged(recyclerView, SCROLL_STATE_DRAGGING);
        adapter.mOnScrollListener.postNotifyItemChange(0 /* index */);

        verify(adapter, never()).notifyItemChanged(0);
        verify(adapter, never()).notifyDataSetChanged();

        adapter.mOnScrollListener.onScrollStateChanged(recyclerView, SCROLL_STATE_IDLE);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void applicationsAdapter_onBindViewHolder_updateSwitch_notifications() {
        ManageApplications manageApplications = mock(ManageApplications.class);
        when(manageApplications.getActivity()).thenReturn(mock(Activity.class));
        UserManager um = mock(UserManager.class);
        when(um.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(manageApplications, "mUserManager", um);
        manageApplications.mListType = LIST_TYPE_NOTIFICATION;
        ApplicationViewHolder holder = mock(ApplicationViewHolder.class);
        ReflectionHelpers.setField(holder, "itemView", mock(View.class));
        ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(mState,
                        manageApplications, mock(AppFilterItem.class),
                        mock(Bundle.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        appList.add(mock(ApplicationsState.AppEntry.class));
        ReflectionHelpers.setField(adapter, "mEntries", appList);

        adapter.onBindViewHolder(holder, 0);
        verify(holder).updateSwitch(any(), anyBoolean(), anyBoolean());
    }

    @Test
    public void applicationsAdapter_onBindViewHolder_updateSwitch_notNotifications() {
        ManageApplications manageApplications = mock(ManageApplications.class);
        manageApplications.mListType = LIST_TYPE_MAIN;
        ApplicationViewHolder holder = mock(ApplicationViewHolder.class);
        ReflectionHelpers.setField(holder, "itemView", mock(View.class));
        UserManager um = mock(UserManager.class);
        when(um.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(manageApplications, "mUserManager", um);
        ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(mState,
                        manageApplications, mock(AppFilterItem.class),
                        mock(Bundle.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        appList.add(mock(ApplicationsState.AppEntry.class));
        ReflectionHelpers.setField(adapter, "mEntries", appList);

        adapter.onBindViewHolder(holder, 0);
        verify(holder, never()).updateSwitch(any(), anyBoolean(), anyBoolean());
    }

    @Test
    public void sortOrderSavedOnRebuild() {
        ManageApplications manageApplications = mock(ManageApplications.class);
        when(manageApplications.getActivity()).thenReturn(mock(Activity.class));
        UserManager um = mock(UserManager.class);
        when(um.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(manageApplications, "mUserManager", um);
        manageApplications.mListType = LIST_TYPE_NOTIFICATION;
        manageApplications.mSortOrder = -1;
        ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(mState,
                        manageApplications, mock(AppFilterItem.class),
                        mock(Bundle.class));

        adapter.rebuild(mSortRecent.getItemId());
        assertThat(manageApplications.mSortOrder).isEqualTo(mSortRecent.getItemId());

        adapter.rebuild(mSortFrequent.getItemId());
        assertThat(manageApplications.mSortOrder).isEqualTo(mSortFrequent.getItemId());
    }

    private void setUpOptionMenus() {
        when(mMenu.findItem(anyInt())).thenAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final int id = (int) args[0];
            if (id == mAppReset.getItemId()) {
                return mAppReset;
            }
            if (id == mSortFrequent.getItemId()) {
                return mSortFrequent;
            }
            if (id == mSortRecent.getItemId()) {
                return mSortRecent;
            }
            return new RoboMenuItem(id);
        });
    }
}
