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

package com.android.settings.homepage.contextualcards;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
public class ContextualCardFeatureProviderImplTest {

    private Context mContext;
    private ContextualCardFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mImpl = new ContextualCardFeatureProviderImpl();
    }

    @Test
    public void sendBroadcast_emptyAction_notSendBroadcast() {
        final Intent intent = new Intent();
        mImpl.sendBroadcast(mContext, intent);

        verify(mContext, never()).sendBroadcast(intent);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void sendBroadcast_hasAction_sendBroadcast() {
        final Intent intent = new Intent();
        mImpl.sendBroadcast(mContext, intent);

        verify(mContext).sendBroadcast(intent);
    }
}