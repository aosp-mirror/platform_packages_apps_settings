/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.security.trustagent.TrustAgentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SecurityFeatureProviderImplTest {

    private Context mContext;
    private SecurityFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mImpl = new SecurityFeatureProviderImpl();
    }

    @Test
    public void getTrustAgentManager_shouldReturnCache() {
        final TrustAgentManager m1 = mImpl.getTrustAgentManager();
        final TrustAgentManager m2 = mImpl.getTrustAgentManager();

        assertThat(m1).isSameAs(m2);
    }

    @Test
    public void getLockPatternUtils_shouldReturnCache() {
        final LockPatternUtils l1 = mImpl.getLockPatternUtils(mContext);
        final LockPatternUtils l2 = mImpl.getLockPatternUtils(mContext);

        assertThat(l1).isSameAs(l2);
    }
}
