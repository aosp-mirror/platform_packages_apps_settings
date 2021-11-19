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

package com.android.settings.network.telephony;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.SettingsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowSubscriptionManager.SubscriptionInfoBuilder;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkActivityTest {

    private static final int SUB_ID = 1;
    private static final String DISPLAY_NAME = "SUB_ID";

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;
    private Intent mTestIntent;
    @Mock
    private UserManager mUserManager;
    private ShadowSubscriptionManager mSubscriptionManager;
    private SubscriptionInfo mSubscriptionInfo;
    private ActivityScenario<MobileNetworkActivity> mMobileNetworkActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mShadowContextImpl = Shadow.extract(RuntimeEnvironment.application.getBaseContext());
        mSubscriptionManager = shadowOf(mContext.getSystemService(SubscriptionManager.class));
        mShadowContextImpl.setSystemService(Context.USER_SERVICE, mUserManager);
        doReturn(true).when(mUserManager).isAdminUser();

        mTestIntent = new Intent(mContext, MobileNetworkActivity.class);
        mSubscriptionInfo = SubscriptionInfoBuilder.newBuilder()
                .setId(SUB_ID).setDisplayName(DISPLAY_NAME).buildSubscriptionInfo();
        mTestIntent.putExtra(Settings.EXTRA_SUB_ID, SUB_ID);
        mTestIntent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                mSubscriptionInfo.getDisplayName());
    }

    @After
    public void cleanUp() {
        if (mMobileNetworkActivity != null) {
            mMobileNetworkActivity.close();
        }
    }

    private ActivityScenario<MobileNetworkActivity> createTargetActivity(Intent activityIntent) {
        return ActivityScenario.launch(activityIntent);
    }

    @Test
    public void onCreate_getExtraFromIntent() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo);
        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.onActivity(activity -> {
            final Bundle bundle = new Bundle();
            activity.onCreate(bundle);
            assertThat(bundle.getInt(Settings.EXTRA_SUB_ID)).isEqualTo(SUB_ID);
            assertThat(bundle.getString(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE)).isEqualTo(
                    DISPLAY_NAME);
        });
    }
}
