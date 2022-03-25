/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class CarrierConfigCacheTest {

    static final int ONCE_SUB_ID = 11;
    static final int TWICE_SUB_ID = 12;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    CarrierConfigManager mCarrierConfigManager;

    Context mContext;
    CarrierConfigCache mCarrierConfigCache;
    PersistableBundle mCarrierConfig = new PersistableBundle();

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());

        mCarrierConfigCache = CarrierConfigCache.getInstance(mContext);
        mCarrierConfigCache.sCarrierConfigManager = mCarrierConfigManager;
    }

    @Test
    public void getInstance_diffContext_getSameInstance() {
        Context context = mContext.createContextAsUser(UserHandle.ALL, 0 /* flags */);
        CarrierConfigCache instance = CarrierConfigCache.getInstance(context);

        assertThat(mContext).isNotEqualTo(context);
        assertThat(mCarrierConfigCache).isEqualTo(instance);
    }

    @Test
    public void hasCarrierConfigManager_getSystemService_returnTrue() {
        assertThat(mCarrierConfigCache.hasCarrierConfigManager()).isTrue();
    }

    @Test
    public void getConfigForSubId_getOnce_onlyGetOnceFromManager() {
        when(mCarrierConfigManager.getConfigForSubId(ONCE_SUB_ID)).thenReturn(mCarrierConfig);

        PersistableBundle config = mCarrierConfigCache.getConfigForSubId(ONCE_SUB_ID);

        assertThat(config).isEqualTo(mCarrierConfig);
        verify(mCarrierConfigManager, times(1)).getConfigForSubId(ONCE_SUB_ID);
    }

    @Test
    public void getConfigForSubId_getTwice_onlyGetOnceFromManager() {
        when(mCarrierConfigManager.getConfigForSubId(TWICE_SUB_ID)).thenReturn(mCarrierConfig);

        mCarrierConfigCache.getConfigForSubId(TWICE_SUB_ID);

        verify(mCarrierConfigManager, times(1)).getConfigForSubId(TWICE_SUB_ID);

        mCarrierConfigCache.getConfigForSubId(TWICE_SUB_ID);

        verify(mCarrierConfigManager, times(1)).getConfigForSubId(TWICE_SUB_ID);
    }
}
