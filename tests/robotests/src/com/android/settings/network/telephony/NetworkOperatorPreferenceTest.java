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

package com.android.settings.network.telephony;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.telephony.CellInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class NetworkOperatorPreferenceTest {
    private static final int LEVEL = 2;

    @Mock
    private Resources mResources;
    @Mock
    private CellInfo mCellInfo;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void setIcon_useOldApi_doNothing() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI))
                .thenReturn(false);
        final NetworkOperatorPreference preference = spy(
                new NetworkOperatorPreference(mCellInfo, mContext,
                        new ArrayList<>() /* forbiddenPlmns */, false /* show4GForLTE */));

        preference.setIcon(LEVEL);

        verify(preference, never()).setIcon(any(Drawable.class));
    }
}
