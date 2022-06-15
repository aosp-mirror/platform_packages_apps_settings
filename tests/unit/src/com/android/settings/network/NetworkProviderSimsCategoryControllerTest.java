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

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import com.android.settingslib.core.lifecycle.Lifecycle;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderSimsCategoryControllerTest {

    private static final String KEY_PREFERENCE_CATEGORY_SIM = "provider_model_sim_category";

    @Mock
    private NetworkProviderSimListController mNetworkProviderSimListController;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private Lifecycle mLifecycle;

    private Context mContext;
    private NetworkProviderSimsCategoryController mCategoryController;

    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mCategoryController = new NetworkProviderSimsCategoryController(
                mContext, KEY_PREFERENCE_CATEGORY_SIM) {
            @Override
            protected NetworkProviderSimListController createSimListController(
                    Lifecycle lifecycle) {
                return mNetworkProviderSimListController;
            }
        };

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        when(mPreferenceCategory.getKey()).thenReturn(KEY_PREFERENCE_CATEGORY_SIM);
        when(mPreferenceCategory.getPreferenceCount()).thenReturn(1);
        mPreferenceScreen.addPreference(mPreferenceCategory);
    }

    @Test
    public void getAvailabilityStatus_returnUnavailable() {
        mNetworkProviderSimListController = null;

        assertThat(mCategoryController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_isVisible() {
        when(mNetworkProviderSimListController.isAvailable()).thenReturn(true);
        mCategoryController.init(mLifecycle);
        mCategoryController.displayPreference(mPreferenceScreen);

        assertEquals(mPreferenceCategory.isVisible(), true);
    }
}
