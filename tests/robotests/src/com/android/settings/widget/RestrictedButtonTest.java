/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManager.EnforcingUser;
import android.view.View.OnClickListener;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Ignore("b/315133235")
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class RestrictedButtonTest {

    private RestrictedButton mButton;
    private OnClickListener mOnClickListener;
    private UserHandle mUser;

    @Before
    public void setUp() {
        mButton = new RestrictedButton(Robolectric.setupActivity(FragmentActivity.class));
        mOnClickListener = mock(OnClickListener.class);
        mButton.setOnClickListener(mOnClickListener);

        int userId = UserHandle.myUserId();
        mUser = UserHandle.of(userId);
        List<EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new EnforcingUser(userId, UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_MODIFY_ACCOUNTS,
                mUser,
                enforcingUsers);
    }

    @Test
    public void performClick_buttonIsNotInited_shouldCallListener() {
        mButton.performClick();

        verify(mOnClickListener).onClick(eq(mButton));
    }

    @Test
    public void performClick_noRestriction_shouldCallListener() {
        mButton.init(mUser, UserManager.DISALLOW_ADJUST_VOLUME);

        mButton.performClick();

        verify(mOnClickListener).onClick(eq(mButton));
    }

    @Test
    public void performClick_hasRestriction_shouldNotCallListener() {
        mButton.init(mUser, UserManager.DISALLOW_MODIFY_ACCOUNTS);

        mButton.performClick();

        verify(mOnClickListener, never()).onClick(eq(mButton));
    }

    @Test
    public void updateState_noRestriction_shouldEnableButton() {
        mButton.init(mUser, UserManager.DISALLOW_ADJUST_VOLUME);

        mButton.updateState();

        assertThat(mButton.isEnabled()).isTrue();
    }

    @Test
    public void updateState_noRestriction_shoulddisableButton() {
        mButton.init(mUser, UserManager.DISALLOW_MODIFY_ACCOUNTS);

        mButton.updateState();

        assertThat(mButton.isEnabled()).isFalse();
    }
}
