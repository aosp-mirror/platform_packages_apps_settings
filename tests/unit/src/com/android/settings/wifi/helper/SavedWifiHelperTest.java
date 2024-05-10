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

package com.android.settings.wifi.helper;

import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.wifitrackerlib.SavedNetworkTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SavedWifiHelperTest {
    static final String TEST_ALIAS = "test_alias";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    Lifecycle mLifecycle;
    @Mock
    SavedNetworkTracker mSaveNetworkTracker;

    SavedWifiHelper mSavedWifiHelper;

    @Before
    public void setUp() {
        mSavedWifiHelper = new SavedWifiHelper(mContext, mLifecycle, mSaveNetworkTracker);
    }

    @Test
    public void isCertificateInUse_redirectToSavedNetworkTracker() {
        mSavedWifiHelper.isCertificateInUse(TEST_ALIAS);

        verify(mSaveNetworkTracker).isCertificateRequired(TEST_ALIAS);
    }

    @Test
    public void getCertificateNetworkNames_redirectToSavedNetworkTracker() {
        mSavedWifiHelper.getCertificateNetworkNames(TEST_ALIAS);

        verify(mSaveNetworkTracker).getCertificateRequesterNames(TEST_ALIAS);
    }
}
