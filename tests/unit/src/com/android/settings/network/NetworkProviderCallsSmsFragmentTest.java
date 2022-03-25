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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderCallsSmsFragmentTest {

    private Context mContext;
    private List<String> mPreferenceKeyList;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Test
    @UiThreadTest
    public void isPageSearchEnabled_shouldIncludeFragmentXml() {
        mPreferenceKeyList =
                NetworkProviderCallsSmsFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(mContext);
        assertThat(mPreferenceKeyList).doesNotContain(
                NetworkProviderCallsSmsFragment.KEY_PREFERENCE_CALLS);
        assertThat(mPreferenceKeyList).doesNotContain(
                NetworkProviderCallsSmsFragment.KEY_PREFERENCE_SMS);
    }
}
