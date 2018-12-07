/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class UserFeatureProviderImplTest {

    private static final int FIRST_USER_ID = 0;
    private static final int SECOND_USER_ID = 4;

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;

    private UserFeatureProviderImpl mFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mFeatureProvider = new UserFeatureProviderImpl(mContext);
    }

    @Test
    public void getUserProfiles() {
        final List<UserHandle> expected =
                Arrays.asList(new UserHandle(FIRST_USER_ID), new UserHandle(SECOND_USER_ID));
        when(mUserManager.getUserProfiles()).thenReturn(expected);
        final List<UserHandle> userProfiles = mFeatureProvider.getUserProfiles();
        assertThat(userProfiles).isEqualTo(expected);
    }
}
