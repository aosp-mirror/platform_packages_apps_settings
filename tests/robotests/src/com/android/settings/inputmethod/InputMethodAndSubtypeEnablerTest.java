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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class InputMethodAndSubtypeEnablerTest {

    private FragmentActivity mActivity;
    private InputMethodAndSubtypeEnabler mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = spy(new InputMethodAndSubtypeEnabler());
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void onActivityCreated_shouldUpdateTitleFromArgument() {
        final String test = "title1";
        final Bundle args = new Bundle();
        args.putString(Intent.EXTRA_TITLE, test);
        mFragment.setArguments(args);

        mFragment.onActivityCreated(null);

        assertThat(mFragment.getActivity().getTitle()).isEqualTo(test);
    }

    @Test
    public void onActivityCreated_shouldUpdateTitleFromIntent() {
        final String test = "title1";
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_TITLE, test);
        mActivity.setIntent(intent);

        mFragment.onActivityCreated(null);

        assertThat(mFragment.getActivity().getTitle()).isEqualTo(test);
    }

    @Test
    public void getPreferenceScreenResId_shouldReturnXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.input_methods_subtype);
    }
}