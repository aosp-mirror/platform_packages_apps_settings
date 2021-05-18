/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings;
import com.android.settings.testutils.AdbUtils;
import com.android.settings.testutils.UiUtils;
import com.android.settingslib.utils.ThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserSettingsComponentTest {
    public static final int TIMEOUT = 2000;
    private static final int USER_TYPE_RESTRICTED_PROFILE = 2;
    public final String TAG = this.getClass().getName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final ArrayList<Integer> mOriginUserIds = new ArrayList<>();
    private final UserManager mUserManager =
            (UserManager) mInstrumentation.getTargetContext().getSystemService("user");
    @Rule
    public ActivityScenarioRule<Settings.UserSettingsActivity>
            rule = new ActivityScenarioRule<>(
            new Intent(android.provider.Settings.ACTION_USER_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

    @Before
    public void setUp() {
        for (UserInfo info : mUserManager.getUsers()) {
            mOriginUserIds.add(info.id);
        }

        // Enable multiple user switch.
        if (!mUserManager.isUserSwitcherEnabled()) {
            android.provider.Settings.Global.putInt(
                    mInstrumentation.getTargetContext().getContentResolver(),
                    android.provider.Settings.Global.USER_SWITCHER_ENABLED, 1);
        }
    }

    @Test
    public void test_new_user_on_multiple_setting_page() throws IOException {
        String randomUserName = gendrate_random_name(10);
        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            Fragment f =
                    ((FragmentActivity) activity).getSupportFragmentManager().getFragments().get(0);
            UserSettings us = (UserSettings) f;
            Log.d(TAG, "Start to add user :" + randomUserName);
            ThreadUtils.postOnBackgroundThread(
                    us.new AddUserNowImpl(USER_TYPE_RESTRICTED_PROFILE, randomUserName));
        });

        assertThat(
                UiUtils.waitUntilCondition(5000, () -> mUserManager.getAliveUsers().stream().filter(
                        (user) -> user.name.equals(
                                randomUserName)).findFirst().isPresent())).isTrue();
    }

    @After
    public void tearDown() {
        int retryNumber = 5;
        for (int i = 0; i < retryNumber; ++i) {
            int currentUsersCount = mUserManager.getUserCount();
            if (currentUsersCount == mOriginUserIds.size()) {
                break;
            } else if (i != 0) {
                Log.d(TAG, "[tearDown] User not fully removed. Retry #" + (i = 1) + " of total "
                        + mOriginUserIds.size());
            }

            for (UserInfo info : mUserManager.getUsers()) {
                if (mOriginUserIds.contains(info.id)) {
                    continue;
                }
                Log.d(TAG, "[tearDown] Clean up user {" + info.id + "}:" + info.name);
                try {
                    AdbUtils.shell("pm remove-user " + info.id);
                } catch (Exception e) {
                    Log.w(TAG, "[tearDown] Error occurs while removing user. " + e.toString());
                }
            }
        }
    }

    private String gendrate_random_name(int length) {
        String seed = "abcdefghijklmnopqrstuvwxyABCDEFGHIJKLMNOPQSTUVWXYZ";
        Random r1 = new Random();
        String result = "";
        for (int i = 0; i < length; ++i) {
            result = result + seed.charAt(r1.nextInt(seed.length() - 1));
        }
        if (mUserManager.getAliveUsers().stream().map(user -> user.name).collect(
                Collectors.toList()).contains(result)) {
            Log.d(TAG, "Name repeated! add padding 'rpt' in the end of name.");
            result += "rpt";
        }
        return result;
    }

}
