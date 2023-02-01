/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privacy;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.TestUtils;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

public class PrivacyDashboardFragmentTest {

    private Context mContext;
    private PrivacyDashboardFragment mPrivacyDashboardFragment;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = ApplicationProvider.getApplicationContext();
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        mPrivacyDashboardFragment = spy(new PrivacyDashboardFragment());
        when(mPrivacyDashboardFragment.getContext()).thenReturn(mContext);
    }

    @Test
    public void whenSafetyCenterIsEnabled_pageIndexExcluded() throws Exception {
        when(mSafetyCenterManagerWrapper.isEnabled(any())).thenReturn(true);
        BaseSearchIndexProvider indexProvider = PrivacyDashboardFragment.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isEmpty();
    }
}
