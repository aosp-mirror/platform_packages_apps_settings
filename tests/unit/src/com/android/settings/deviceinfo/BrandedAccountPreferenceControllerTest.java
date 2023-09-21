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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BrandedAccountPreferenceControllerTest {

    @Mock
    private Resources mResources;
    private Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;

    private PreferenceScreen mScreen;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
    }

    @Test
    public void isAvailable_configOn_noAccount_off() {
        when(mContext.getResources()).thenReturn(mResources);
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_show_branded_account_in_device_info");
        when(mResources.getBoolean(boolId)).thenReturn(true);

        final BrandedAccountPreferenceController controller =
                new BrandedAccountPreferenceController(mContext, "test_key");
        assertThat(controller.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_accountIsAvailable_on() {
        when(mContext.getResources()).thenReturn(mResources);
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_show_branded_account_in_device_info");
        when(mResources.getBoolean(boolId)).thenReturn(true);
        when(mFakeFeatureFactory.mAccountFeatureProvider.getAccounts(any(Context.class)))
                .thenReturn(new Account[]{new Account("fake@account.foo", "fake.reallyfake")});

        final BrandedAccountPreferenceController controller =
                new BrandedAccountPreferenceController(mContext, "test_key");

        assertThat(controller.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configOff_hasAccount_off() {
        when(mContext.getResources()).thenReturn(mResources);
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_show_branded_account_in_device_info");
        when(mResources.getBoolean(boolId)).thenReturn(false);
        when(mFakeFeatureFactory.mAccountFeatureProvider.getAccounts(any(Context.class)))
                .thenReturn(new Account[]{new Account("fake@account.foo", "fake.reallyfake")});

        final BrandedAccountPreferenceController controller =
                new BrandedAccountPreferenceController(mContext, "test_key");

        assertThat(controller.isAvailable()).isFalse();
    }

    /**
     *  Test displayPreference has one account.
     */
    @Test
    public void displayPreference_hasOneAccount_showAccount() {
        when(mFakeFeatureFactory.mAccountFeatureProvider.getAccounts(any(Context.class)))
                .thenReturn(new Account[]{new Account("teresaikeda@gmail.com",
                    "com.google")});

        mPreference.setKey("test_key");
        mScreen.addPreference(mPreference);
        final BrandedAccountPreferenceController controller =
                new BrandedAccountPreferenceController(mContext, "test_key");

        controller.displayPreference(mScreen);

        assertThat(mPreference.getSummary()).isEqualTo("teresaikeda@gmail.com");
    }

    /**
     *  Test displayPreference has two accounts.
     */
    @Test
    public void displayPreference_hasTwoAccounts_showTwoAccountSummary() {
        when(mFakeFeatureFactory.mAccountFeatureProvider.getAccounts(any(Context.class)))
                .thenReturn(new Account[]{new Account("teresa@gmail.com", "com.google"),
                                          new Account("reno@gmail.com", "com.google") });

        mPreference.setKey("test_key");
        mScreen.addPreference(mPreference);
        final BrandedAccountPreferenceController controller =
                new BrandedAccountPreferenceController(mContext, "test_key");

        controller.displayPreference(mScreen);

        assertThat(mPreference.getSummary()).isEqualTo("2 accounts");
    }
}
