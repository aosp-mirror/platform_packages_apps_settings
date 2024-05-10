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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAppUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.testutils.shadow.ShadowInteractionJankMonitor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowAppUtils.class,
        ShadowInteractionJankMonitor.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ManageApplicationsTest {

    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private Menu mMenu;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private Resources mResources;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private MenuItem mAppReset;
    private MenuItem mSortRecent;
    private MenuItem mSortFrequent;
    private ManageApplications mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mAppReset = new RoboMenuItem(R.id.reset_app_preferences);
        mSortRecent = new RoboMenuItem(R.id.sort_order_recent_notification);
        mSortFrequent = new RoboMenuItem(R.id.sort_order_frequent_notification);
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mState);
        when(mState.newSession(any())).thenReturn(mSession);
        when(mState.getBackgroundLooper()).thenReturn(Looper.myLooper());

        mFragment = spy(new ManageApplications());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        ReflectionHelpers.setField(mFragment, "mUserManager",
                mContext.getSystemService(UserManager.class));
        when(mActivity.getResources()).thenReturn(mResources);
        when(mActivity.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mActivity.getLayoutInflater()).thenReturn(LayoutInflater.from(mContext));
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
    public void onCreateOptionsMenu_shouldSetSearchQueryListener() {
        final SearchView searchView = mock(SearchView.class);
        final MenuItem searchMenu = mock(MenuItem.class);
        final MenuItem helpMenu = mock(MenuItem.class);
        when(searchMenu.getActionView()).thenReturn(searchView);
        when(mMenu.findItem(R.id.search_app_list_menu)).thenReturn(searchMenu);
        when(mMenu.add(anyInt() /* groupId */, anyInt() /* itemId */, anyInt() /* order */,
                anyInt() /* titleRes */)).thenReturn(helpMenu);
        doReturn("Test").when(mFragment).getText(anyInt() /* resId */);
        doNothing().when(mFragment).updateOptionsMenu();

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(searchView).setOnQueryTextListener(mFragment);
    }

    @Test
    public void onCreateOptionsMenu_hasExpandSearchFlag_shouldExpandSearchView() {
        final SearchView searchView = mock(SearchView.class);
        final MenuItem searchMenu = mock(MenuItem.class);
        final MenuItem helpMenu = mock(MenuItem.class);
        when(searchMenu.getActionView()).thenReturn(searchView);
        when(mMenu.findItem(R.id.search_app_list_menu)).thenReturn(searchMenu);
        when(mMenu.add(anyInt() /* groupId */, anyInt() /* itemId */, anyInt() /* order */,
                anyInt() /* titleRes */)).thenReturn(helpMenu);
        doReturn("Test").when(mFragment).getText(anyInt() /* resId */);
        doNothing().when(mFragment).updateOptionsMenu();

        mFragment.mExpandSearch = true;
        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(searchMenu).expandActionView();
    }

    @Test
    public void onQueryTextChange_shouldFilterSearchInApplicationsAdapter() {
        final ManageApplications.ApplicationsAdapter adapter =
                mock(ManageApplications.ApplicationsAdapter.class);
        final String query = "Test App";
        ReflectionHelpers.setField(mFragment, "mApplications", adapter);

        mFragment.onQueryTextChange(query);

        verify(adapter).filterSearch(query);
    }

    @Test
    public void updateLoading_appLoaded_shouldNotDelayCallToHandleLoadingContainer() {
        ReflectionHelpers.setField(mFragment, "mLoadingContainer", mock(View.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
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
        ReflectionHelpers.setField(mFragment, "mLoadingContainer", mock(View.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
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
    public void onRebuildComplete_shouldHideLoadingView() {
        final Context context = RuntimeEnvironment.application;
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final View emptyView = mock(View.class);
        ReflectionHelpers.setField(mFragment, "mRecyclerView", recyclerView);
        ReflectionHelpers.setField(mFragment, "mEmptyView", emptyView);
        final View loadingContainer = mock(View.class);
        when(loadingContainer.getContext()).thenReturn(context);
        final View listContainer = mock(View.class);
        when(listContainer.getVisibility()).thenReturn(View.INVISIBLE);
        when(listContainer.getContext()).thenReturn(context);
        ReflectionHelpers.setField(mFragment, "mLoadingContainer", loadingContainer);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
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
        ReflectionHelpers.setField(
                mFragment, "mFilterAdapter", mock(ManageApplications.FilterSpinnerAdapter.class));

        adapter.onRebuildComplete(null);

        verify(loadingViewController).showEmpty(false /* animate */);
    }

    @Test
    public void onRebuildComplete_hasSearchQuery_shouldFilterSearch() {
        final String query = "Test";
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final View emptyView = mock(View.class);
        final View loadingContainer = mock(View.class);
        ReflectionHelpers.setField(mFragment, "mRecyclerView", recyclerView);
        ReflectionHelpers.setField(mFragment, "mEmptyView", emptyView);
        ReflectionHelpers.setField(mFragment, "mLoadingContainer", loadingContainer);
        final SearchView searchView = mock(SearchView.class);
        ReflectionHelpers.setField(mFragment, "mSearchView", searchView);
        when(searchView.isVisibleToUser()).thenReturn(true);
        when(searchView.getQuery()).thenReturn(query);
        final View listContainer = mock(View.class);
        when(listContainer.getVisibility()).thenReturn(View.VISIBLE);
        ReflectionHelpers.setField(
                mFragment, "mFilterAdapter", mock(ManageApplications.FilterSpinnerAdapter.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        appList.add(mock(ApplicationsState.AppEntry.class));
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL),
                        null /* savedInstanceState */));

        adapter.onRebuildComplete(appList);

        verify(adapter).filterSearch(query);
    }

    @Ignore("b/313583754")
    @Test
    public void notifyItemChange_recyclerViewIdle_shouldNotify() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));

        adapter.onAttachedToRecyclerView(recyclerView);
        adapter.mOnScrollListener.onScrollStateChanged(recyclerView, SCROLL_STATE_IDLE);
        adapter.mOnScrollListener.postNotifyItemChange(0 /* index */);

        verify(adapter).notifyItemChanged(0);
    }

    @Ignore("b/313583754")
    @Test
    public void notifyItemChange_recyclerViewScrolling_shouldNotifyWhenIdle() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
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
    public void applicationsAdapter_onBindViewHolder_notifications_wrongExtraInfo() {
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        mFragment.mListType = LIST_TYPE_NOTIFICATION;
        ApplicationViewHolder holder = mock(ApplicationViewHolder.class);
        ReflectionHelpers.setField(holder, "itemView", mock(View.class));
        ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(mState,
                        mFragment, mock(AppFilterItem.class),
                        mock(Bundle.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);
        appEntry.extraInfo = mock(AppFilterItem.class);
        appList.add(appEntry);
        ReflectionHelpers.setField(adapter, "mEntries", appList);
        ReflectionHelpers.setField(adapter, "mContext", mContext);

        adapter.onBindViewHolder(holder, 0);
        // no crash? yay!
    }

    @Test
    public void applicationsAdapter_onBindViewHolder_updateSwitch_notifications() {
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        mFragment.mListType = LIST_TYPE_NOTIFICATION;
        ApplicationViewHolder holder = mock(ApplicationViewHolder.class);
        ReflectionHelpers.setField(holder, "itemView", mock(View.class));
        ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(mState,
                        mFragment, mock(AppFilterItem.class),
                        mock(Bundle.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);
        appList.add(appEntry);
        ReflectionHelpers.setField(adapter, "mEntries", appList);
        ReflectionHelpers.setField(adapter, "mContext", mContext);

        adapter.onBindViewHolder(holder, 0);
        verify(holder).updateSwitch(any(), anyBoolean(), anyBoolean());
    }

    @Test
    public void applicationsAdapter_onBindViewHolder_updateSwitch_notNotifications() {
        mFragment.mListType = LIST_TYPE_MAIN;
        ApplicationViewHolder holder = mock(ApplicationViewHolder.class);
        ReflectionHelpers.setField(holder, "itemView", mock(View.class));
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ManageApplications.ApplicationsAdapter adapter = new ManageApplications.ApplicationsAdapter(
                mState, mFragment, mock(AppFilterItem.class), mock(Bundle.class));
        final ArrayList<ApplicationsState.AppEntry> appList = new ArrayList<>();
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);
        appList.add(appEntry);
        ReflectionHelpers.setField(adapter, "mEntries", appList);
        ReflectionHelpers.setField(adapter, "mContext", mContext);

        adapter.onBindViewHolder(holder, 0);
        verify(holder, never()).updateSwitch(any(), anyBoolean(), anyBoolean());
    }

    @Test
    public void applicationsAdapter_filterSearch_emptyQuery_shouldShowFullList() {
        final ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(
                        mState, mFragment, mock(AppFilterItem.class), Bundle.EMPTY);
        final String[] appNames = {"Apricot", "Banana", "Cantaloupe", "Fig", "Mango"};
        ReflectionHelpers.setField(adapter, "mOriginalEntries", getTestAppList(appNames));

        adapter.filterSearch("");

        assertThat(adapter.getItemCount()).isEqualTo(5);
    }

    @Ignore("b/313583754")
    @Test
    public void applicationsAdapter_filterSearch_noMatch_shouldShowEmptyList() {
        final ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(
                        mState, mFragment, mock(AppFilterItem.class), Bundle.EMPTY);
        final String[] appNames = {"Apricot", "Banana", "Cantaloupe", "Fig", "Mango"};
        ReflectionHelpers.setField(adapter, "mOriginalEntries", getTestAppList(appNames));

        adapter.filterSearch("orange");

        assertThat(adapter.getItemCount()).isEqualTo(0);
    }

    @Test
    public void applicationsAdapter_filterSearch_shouldShowMatchedItemsOnly() {
        final ManageApplications.ApplicationsAdapter adapter =
                new ManageApplications.ApplicationsAdapter(
                        mState, mFragment, mock(AppFilterItem.class), Bundle.EMPTY);
        final String[] appNames = {"Apricot", "Banana", "Cantaloupe", "Fig", "Mango"};
        ReflectionHelpers.setField(adapter, "mOriginalEntries", getTestAppList(appNames));

        adapter.filterSearch("an");

        assertThat(adapter.getItemCount()).isEqualTo(3);
        assertThat(adapter.getAppEntry(0).label).isEqualTo("Banana");
        assertThat(adapter.getAppEntry(1).label).isEqualTo("Cantaloupe");
        assertThat(adapter.getAppEntry(2).label).isEqualTo("Mango");
    }

    @Test
    public void sortOrderSavedOnRebuild() {
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        mFragment.mListType = LIST_TYPE_NOTIFICATION;
        mFragment.mSortOrder = -1;
        ManageApplications.ApplicationsAdapter adapter = new ManageApplications.ApplicationsAdapter(
                mState, mFragment, mock(AppFilterItem.class), mock(Bundle.class));

        adapter.rebuild(mSortRecent.getItemId(), false);
        assertThat(mFragment.mSortOrder).isEqualTo(mSortRecent.getItemId());

        adapter.rebuild(mSortFrequent.getItemId(), false);
        assertThat(mFragment.mSortOrder).isEqualTo(mSortFrequent.getItemId());

        adapter.rebuild(mSortFrequent.getItemId(), true);
        assertThat(mFragment.mSortOrder).isEqualTo(mSortFrequent.getItemId());
    }

    @Test
    public void updateFilterView_hasFilterSet_shouldShowFilterAndHavePaddingTop() {
        mFragment.mRecyclerView = new RecyclerView(mContext);
        mFragment.mSpinnerHeader = new View(mContext);
        mFragment.mFilterAdapter = new ManageApplications.FilterSpinnerAdapter(mFragment);

        mFragment.mFilterAdapter.updateFilterView(true);

        assertThat(mFragment.mSpinnerHeader.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void updateFilterView_noFilterSet_shouldHideFilterAndNoPaddingTop() {
        mFragment.mRecyclerView = new RecyclerView(mContext);
        mFragment.mSpinnerHeader = new View(mContext);
        mFragment.mFilterAdapter = new ManageApplications.FilterSpinnerAdapter(mFragment);

        mFragment.mFilterAdapter.updateFilterView(false);

        assertThat(mFragment.mSpinnerHeader.getVisibility()).isEqualTo(View.GONE);
        assertThat(mFragment.mRecyclerView.getPaddingTop()).isEqualTo(0);
    }

    @Test
    public void onSaveInstanceState_noSearchView_shouldNotSetBundleValue() {
        final Bundle bundle = new Bundle();
        ReflectionHelpers.setField(mFragment, "mResetAppsHelper", mock(ResetAppsHelper.class));
        ReflectionHelpers.setField(mFragment, "mFilter", mock(AppFilterItem.class));
        ReflectionHelpers.setField(mFragment, "mApplications",
                mock(ManageApplications.ApplicationsAdapter.class));

        mFragment.onSaveInstanceState(bundle);

        assertThat(bundle.containsKey(ManageApplications.EXTRA_EXPAND_SEARCH_VIEW)).isFalse();
    }

    @Test
    public void onSaveInstanceState_searchViewSet_shouldSetBundleValue() {
        final SearchView searchView = mock(SearchView.class);
        final Bundle bundle = new Bundle();
        ReflectionHelpers.setField(mFragment, "mResetAppsHelper", mock(ResetAppsHelper.class));
        ReflectionHelpers.setField(mFragment, "mFilter", mock(AppFilterItem.class));
        ReflectionHelpers.setField(mFragment, "mApplications",
                mock(ManageApplications.ApplicationsAdapter.class));
        ReflectionHelpers.setField(mFragment, "mSearchView", searchView);
        when(searchView.isIconified()).thenReturn(true);

        mFragment.onSaveInstanceState(bundle);

        assertThat(bundle.containsKey(ManageApplications.EXTRA_EXPAND_SEARCH_VIEW)).isTrue();
        assertThat(bundle.getBoolean(ManageApplications.EXTRA_EXPAND_SEARCH_VIEW)).isFalse();
    }

    @Test
    public void createHeader_batteryListType_hasCorrectItems() {
        ReflectionHelpers.setField(mFragment, "mListType", ManageApplications.LIST_TYPE_HIGH_POWER);
        ReflectionHelpers.setField(mFragment, "mRootView",
                LayoutInflater.from(mContext).inflate(R.layout.manage_applications_apps, null));
        mFragment.mRecyclerView = new RecyclerView(mContext);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        ReflectionHelpers.setField(mFragment, "mApplications", adapter);

        mFragment.createHeader();

        assertThat(mFragment.mFilterAdapter.getCount()).isEqualTo(2);
        assertThat(mFragment.mFilterAdapter.getItem(0)).isEqualTo(
                mContext.getString(R.string.high_power_filter_on));
        assertThat(mFragment.mFilterAdapter.getItem(1)).isEqualTo(
                mContext.getString(R.string.filter_all_apps));
    }

    @Test
    public void createHeader_notificationListType_hasCorrectItems() {
        ReflectionHelpers.setField(mFragment, "mListType", LIST_TYPE_NOTIFICATION);
        ReflectionHelpers.setField(mFragment, "mRootView",
                LayoutInflater.from(mContext).inflate(R.layout.manage_applications_apps, null));
        mFragment.mRecyclerView = new RecyclerView(mContext);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        ReflectionHelpers.setField(mFragment, "mApplications", adapter);

        mFragment.createHeader();

        assertThat(mFragment.mFilterAdapter.getCount()).isEqualTo(4);
        assertThat(mFragment.mFilterAdapter.getItem(0)).isEqualTo(
                mContext.getString(R.string.sort_order_recent_notification));
        assertThat(mFragment.mFilterAdapter.getItem(1)).isEqualTo(
                mContext.getString(R.string.sort_order_frequent_notification));
        assertThat(mFragment.mFilterAdapter.getItem(2)).isEqualTo(
                mContext.getString(R.string.filter_all_apps));
        assertThat(mFragment.mFilterAdapter.getItem(3)).isEqualTo(
                mContext.getString(R.string.filter_notif_blocked_apps));
    }

    @Test
    public void onItemSelected_powerWhiteApps_returnCorrectValue() {
        ReflectionHelpers.setField(mFragment, "mListType", ManageApplications.LIST_TYPE_HIGH_POWER);
        ReflectionHelpers.setField(mFragment, "mRootView",
                LayoutInflater.from(mContext).inflate(R.layout.manage_applications_apps, null));
        mFragment.mRecyclerView = new RecyclerView(mContext);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        ReflectionHelpers.setField(mFragment, "mApplications", adapter);
        mFragment.createHeader();

        mFragment.onItemSelected(null, null, 0, 0);

        AppFilter filter = ReflectionHelpers.getField(adapter, "mCompositeFilter");
        assertThat(filter.filterApp(createPowerAllowListApp(false))).isFalse();
        assertThat(filter.filterApp(createPowerAllowListApp(true))).isTrue();
    }

    @Test
    public void onItemSelected_allApps_returnCorrectValue() {
        ReflectionHelpers.setField(mFragment, "mListType", ManageApplications.LIST_TYPE_HIGH_POWER);
        ReflectionHelpers.setField(mFragment, "mRootView",
                LayoutInflater.from(mContext).inflate(R.layout.manage_applications_apps, null));
        mFragment.mRecyclerView = new RecyclerView(mContext);
        final ManageApplications.ApplicationsAdapter adapter =
                spy(new ManageApplications.ApplicationsAdapter(mState, mFragment,
                        AppFilterRegistry.getInstance().get(FILTER_APPS_ALL), new Bundle()));
        ReflectionHelpers.setField(mFragment, "mApplications", adapter);
        mFragment.createHeader();

        mFragment.onItemSelected(null, null, 1, 0);

        AppFilter filter = ReflectionHelpers.getField(adapter, "mCompositeFilter");
        assertThat(filter.filterApp(createPowerAllowListApp(false))).isTrue();
        assertThat(filter.filterApp(createPowerAllowListApp(true))).isTrue();
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

    private ArrayList<AppEntry> getTestAppList(String[] appNames) {
        final ArrayList<AppEntry> appList = new ArrayList<>();
        for (String name : appNames) {
            final AppEntry appEntry = mock(AppEntry.class);
            appEntry.label = name;
            appList.add(appEntry);
        }
        return appList;
    }

    private AppEntry createPowerAllowListApp(boolean isPowerAllowListed) {
        final ApplicationInfo info = new ApplicationInfo();
        info.sourceDir = "abc";
        final AppEntry entry = new AppEntry(mContext, info, 0);
        entry.extraInfo = isPowerAllowListed ? Boolean.TRUE : Boolean.FALSE;
        return entry;
    }
}
