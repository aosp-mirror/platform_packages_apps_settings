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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShortcutServicePickerFragmentTest {

    private static final String TEST_SERVICE_KEY_1 = "abc/123";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;

    private ShortcutServicePickerFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mFragment = spy(new ShortcutServicePickerFragment());
        mFragment.onAttach(mActivity);

        doReturn(RuntimeEnvironment.application).when(mFragment).getContext();
    }

    @Test
    public void setAndGetDefaultAppKey_shouldUpdateDefaultAppKey() {
        assertThat(mFragment.setDefaultKey(TEST_SERVICE_KEY_1)).isTrue();
        assertThat(mFragment.getDefaultKey()).isEqualTo(TEST_SERVICE_KEY_1);
    }
}

