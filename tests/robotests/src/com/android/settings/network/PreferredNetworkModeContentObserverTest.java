/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreferredNetworkModeContentObserverTest {

    private static final int SUB_ID = 1;

    @Mock
    private ContentResolver mResolver;
    @Mock
    private Context mContext;
    @Mock
    private PreferredNetworkModeContentObserver.OnPreferredNetworkModeChangedListener mListener;

    private PreferredNetworkModeContentObserver mPreferredNetworkModeContentObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
		when(mContext.getContentResolver()).thenReturn(mResolver);
        mPreferredNetworkModeContentObserver =
                spy(new PreferredNetworkModeContentObserver(null));
    }

    @Test
    public void onChange_shouldCallListener() {
        mPreferredNetworkModeContentObserver.mListener = mListener;
        mPreferredNetworkModeContentObserver.onChange(true);

        verify(mListener).onPreferredNetworkModeChanged();
    }

    @Test
    public void register_shouldRegisterContentObserver() {
        mPreferredNetworkModeContentObserver.register(mContext, SUB_ID);

        verify(mResolver).registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID), false,
            mPreferredNetworkModeContentObserver);
    }

    @Test
    public void unregister_shouldUnregisterContentObserver() {
        mPreferredNetworkModeContentObserver.unregister(mContext);

        verify(mResolver).unregisterContentObserver(mPreferredNetworkModeContentObserver);
    }

}
