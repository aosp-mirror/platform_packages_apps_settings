/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.SwitchWidgetController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class MultiUserSwitchBarControllerTest {

    private Context mContext;
    private ShadowUserManager mUserManager;
    private SwitchWidgetController mSwitchWidgetController;


    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mUserManager = ShadowUserManager.getShadow();
        mSwitchWidgetController = mock(SwitchWidgetController.class);
        mUserManager.setSupportsMultipleUsers(true);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    public void onStart_disallowUserSwitch_shouldSetDisabledByAdmin() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, true);

        final MultiUserSwitchBarController controller = new MultiUserSwitchBarController(mContext,
                mSwitchWidgetController, null);

        verify(mSwitchWidgetController).setDisabledByAdmin(any());
    }

    @Test
    public void onStart_allowUserSwitch_shouldNotSetDisabledByAdmin() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);

        final MultiUserSwitchBarController controller = new MultiUserSwitchBarController(mContext,
                mSwitchWidgetController, null);

        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any());
    }
}
