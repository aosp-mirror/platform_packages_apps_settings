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

package com.android.settings.mobilenetwork;

import static com.android.settings.mobilenetwork.MobileSettingsActivity.MOBILE_SETTINGS_TAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Menu;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class MobileSettingsActivityTest {

    private static final int CURRENT_SUB_ID = 3;
    private static final int PREV_SUB_ID = 1;

    private Context mContext;
    private MobileSettingsActivity mMobileSettingsActivity;
    private List<SubscriptionInfo> mSubscriptionInfos;
    private Fragment mShowFragment;
    private Fragment mHideFragment;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private BottomNavigationView mBottomNavigationView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mMobileSettingsActivity = spy(new MobileSettingsActivity());
        mSubscriptionInfos = new ArrayList<>();
        mShowFragment = new Fragment();
        mHideFragment = new Fragment();
        mMobileSettingsActivity.mSubscriptionInfos = mSubscriptionInfos;

        doReturn(mSubscriptionManager).when(mMobileSettingsActivity).getSystemService(
                SubscriptionManager.class);
        doReturn(mBottomNavigationView).when(mMobileSettingsActivity).findViewById(R.id.bottom_nav);
        doReturn(mFragmentManager).when(mMobileSettingsActivity).getSupportFragmentManager();
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();
        doReturn(mHideFragment).when(mFragmentManager).findFragmentByTag(
                MOBILE_SETTINGS_TAG + PREV_SUB_ID);
        doReturn(mShowFragment).when(mFragmentManager).findFragmentByTag(
                MOBILE_SETTINGS_TAG + CURRENT_SUB_ID);
    }

    @Test
    public void updateBottomNavigationView_oneSubscription_shouldBeGone() {
        mSubscriptionInfos.add(mSubscriptionInfo);
        doReturn(mSubscriptionInfos).when(mSubscriptionManager).getActiveSubscriptionInfoList();

        mMobileSettingsActivity.updateBottomNavigationView();

        verify(mBottomNavigationView).setVisibility(View.GONE);
    }

    @Test
    public void updateBottomNavigationView_twoSubscription_updateMenu() {
        final Menu menu = new ContextMenuBuilder(mContext);
        mSubscriptionInfos.add(mSubscriptionInfo);
        mSubscriptionInfos.add(mSubscriptionInfo);
        doReturn(mSubscriptionInfos).when(mSubscriptionManager).getActiveSubscriptionInfoList();
        doReturn(menu).when(mBottomNavigationView).getMenu();

        mMobileSettingsActivity.updateBottomNavigationView();

        assertThat(menu.size()).isEqualTo(2);
    }

    @Test
    public void switchFragment_hidePreviousFragment() {
        mMobileSettingsActivity.mPrevSubscriptionId = PREV_SUB_ID;

        mMobileSettingsActivity.switchFragment(mShowFragment, CURRENT_SUB_ID);

        verify(mFragmentTransaction).hide(mHideFragment);
    }

    @Test
    public void switchFragment_fragmentExist_showItWithArguments() {
        mMobileSettingsActivity.mPrevSubscriptionId = PREV_SUB_ID;

        mMobileSettingsActivity.switchFragment(mShowFragment, CURRENT_SUB_ID);

        assertThat(mShowFragment.getArguments().getInt(
                MobileSettingsActivity.KEY_SUBSCRIPTION_ID)).isEqualTo(CURRENT_SUB_ID);
        verify(mFragmentTransaction).show(mShowFragment);
    }
}
