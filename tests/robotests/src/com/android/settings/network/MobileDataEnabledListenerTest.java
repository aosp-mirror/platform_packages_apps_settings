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
 * limitations under the License
 */

package com.android.settings.network;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MobileDataEnabledListenerTest {
    private static final int SUB_ID_ONE = 111;
    private static final int SUB_ID_TWO = 222;

    @Mock
    private MobileDataEnabledListener.Client mClient;

    private Context mContext;
    private MobileDataEnabledListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListener = new MobileDataEnabledListener(mContext, mClient);
    }

    @Test
    public void onMobileDataEnabledChange_firesCorrectly() {
        mListener.start(SUB_ID_ONE);
        final Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + SUB_ID_ONE);
        mContext.getContentResolver().notifyChange(uri, null);
        verify(mClient).onMobileDataEnabledChange();
    }

    @Test
    public void onMobileDataEnabledChange_doesNotFireAfterStop() {
        mListener.start(SUB_ID_ONE);
        mListener.stop();
        final Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + SUB_ID_ONE);
        mContext.getContentResolver().notifyChange(uri, null);
        verify(mClient, never()).onMobileDataEnabledChange();
    }

    @Test
    public void onMobileDataEnabledChange_changedToDifferentId_firesCorrectly() {
        mListener.start(SUB_ID_ONE);
        mListener.stop();
        mListener.start(SUB_ID_TWO);
        final Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + SUB_ID_TWO);
        mContext.getContentResolver().notifyChange(uri, null);
        verify(mClient).onMobileDataEnabledChange();
    }
}
