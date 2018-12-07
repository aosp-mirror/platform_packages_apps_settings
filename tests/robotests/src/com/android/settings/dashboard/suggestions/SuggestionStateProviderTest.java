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

package com.android.settings.dashboard.suggestions;

import static android.content.Intent.EXTRA_COMPONENT_NAME;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SuggestionStateProviderTest {

    private SuggestionStateProvider mProvider;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        mProvider = Robolectric.setupContentProvider(SuggestionStateProvider.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void query_shouldCrash() {
        mProvider.query(null, null, null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getType_shouldCrash() {
        mProvider.getType(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void insert_shouldCrash() {
        mProvider.insert(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void delete_shouldCrash() {
        mProvider.delete(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void update_shouldCrash() {
        mProvider.update(null, null, null, null);
    }

    @Test
    public void getSuggestionState_shouldQueryFeatureProvider() {
        final Bundle extras = new Bundle();
        extras.putString(SuggestionStateProvider.EXTRA_CANDIDATE_ID, "ID");
        extras.putParcelable(EXTRA_COMPONENT_NAME, new ComponentName("pkg", "cls"));

        mProvider.call(SuggestionStateProvider.METHOD_GET_SUGGESTION_STATE, "foobar", extras);

        verify(mFeatureFactory.suggestionsFeatureProvider)
                .isSuggestionComplete(any(Context.class), any(ComponentName.class));
    }
}
