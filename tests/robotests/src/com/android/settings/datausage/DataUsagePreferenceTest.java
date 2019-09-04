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

package com.android.settings.datausage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;

import com.android.settingslib.net.DataUsageController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DataUsagePreferenceTest {

    @Mock
    private DataUsageController mController;

    private Context mContext;
    private DataUsagePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = spy(new DataUsagePreference(mContext, null /* attrs */));
        doReturn(mController).when(mPreference).getDataUsageController();
    }

    @Test
    public void setTemplate_noDataUsage_shouldDisablePreference() {
        doReturn(0L).when(mController).getHistoricalUsageLevel(any(NetworkTemplate.class));

        mPreference.setTemplate(
                NetworkTemplate.buildTemplateMobileWildcard(), 5 /* subId */, null /* services */);

        verify(mPreference).setEnabled(false);
        verify(mPreference).setIntent(null);
    }

    @Test
    public void setTemplate_hasDataUsage_shouldNotDisablePreference() {
        doReturn(200L).when(mController).getHistoricalUsageLevel(any(NetworkTemplate.class));

        mPreference.setTemplate(
                NetworkTemplate.buildTemplateMobileWildcard(), 5 /* subId */, null /* services */);

        verify(mPreference, never()).setEnabled(false);
        verify(mPreference).setIntent(any(Intent.class));
    }
}
