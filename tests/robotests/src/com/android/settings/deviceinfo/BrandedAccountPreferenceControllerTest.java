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
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.content.Context;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class BrandedAccountPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private BrandedAccountPreferenceController mController;
    private FakeFeatureFactory fakeFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new BrandedAccountPreferenceController(mContext);
    }

    @Test
    public void isAvailable_defaultOff() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_onWhenAccountIsAvailable() {
        when(fakeFeatureFactory.mAccountFeatureProvider.getAccounts(any(Context.class)))
            .thenReturn(new Account[] {new Account("fake@account.foo", "fake.reallyfake")});
        mController = new BrandedAccountPreferenceController(mContext);
        assertThat(mController.isAvailable()).isTrue();
    }
}
