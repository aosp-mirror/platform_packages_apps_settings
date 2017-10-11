/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.search2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SavedQueryViewHolderTest {

    @Mock
    private SearchFragment mSearchFragment;
    private Context mContext;
    private SavedQueryViewHolder mHolder;
    private View mView;
    private View mTitleView;
    private View mRemoveButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mView = LayoutInflater.from(mContext)
                .inflate(R.layout.search_saved_query_item, null);
        mTitleView = mView.findViewById(android.R.id.title);
        mRemoveButton = mView.findViewById(android.R.id.icon);
        mHolder = new SavedQueryViewHolder(mView);
    }

    @Test
    public void onBind_shouldBindClickCallback() {
        final SearchResult result = mock(SearchResult.class);
        mHolder.onBind(mSearchFragment, result);

        mTitleView.performClick();
        mRemoveButton.performClick();

        verify(mSearchFragment).onSavedQueryClicked(any(CharSequence.class));
        verify(mSearchFragment).onRemoveSavedQueryClicked(any(CharSequence.class));
    }
}
