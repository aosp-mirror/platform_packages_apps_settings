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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;

import com.android.settingslib.RestrictedPreferenceHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class UserPreferenceTest {

    @Mock
    private RestrictedPreferenceHelper mRestrictedPreferenceHelper;
    private Context mContext;
    private UserPreference mUserPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mUserPreference = new UserPreference(mContext, null /* attrs */, UserHandle.USER_CURRENT);
        ReflectionHelpers.setField(mUserPreference, "mHelper", mRestrictedPreferenceHelper);
    }

    @Test
    public void testShouldHideSecondTarget_shouldHide() {
        assertThat(mUserPreference.shouldHideSecondTarget()).isTrue();
    }
}
