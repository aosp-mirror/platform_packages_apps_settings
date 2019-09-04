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
 * limitations under the License.
 */

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NetworkResetRestrictionCheckerTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private Context mContext;
    private NetworkResetRestrictionChecker mRestrictionChecker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mRestrictionChecker = spy(new NetworkResetRestrictionChecker(mContext));
    }

    @Test
    public void testHasRestriction_notAdmin_shouldReturnTrue() {
        final Context context = mock(Context.class);
        when(context.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mRestrictionChecker.hasRestriction()).isTrue();
    }

    @Test
    public void testHasRestriction_hasUserRestriction_shouldReturnTrue() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        doReturn(true).when(mRestrictionChecker).hasUserBaseRestriction();
        doReturn(false).when(mRestrictionChecker).isRestrictionEnforcedByAdmin();

        assertThat(mRestrictionChecker.hasRestriction()).isTrue();
    }

    @Test
    public void testHasRestriction_hasAdminRestriction_shouldReturnTrue() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        doReturn(false).when(mRestrictionChecker).hasUserBaseRestriction();
        doReturn(true).when(mRestrictionChecker).isRestrictionEnforcedByAdmin();

        assertThat(mRestrictionChecker.hasRestriction()).isTrue();
    }

    @Test
    public void testHasRestriction_noRestriction_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        doReturn(false).when(mRestrictionChecker).hasUserBaseRestriction();
        doReturn(false).when(mRestrictionChecker).isRestrictionEnforcedByAdmin();

        assertThat(mRestrictionChecker.hasRestriction()).isFalse();
    }
}
