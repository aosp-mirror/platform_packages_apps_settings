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

package com.android.settings.accessibility;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class ToggleFeaturePreferenceFragmentTest {

    private ToggleFeaturePreferenceFragmentTestable mFragment;

    @Test
    public void createFragment_shouldOnlyAddPreferencesOnce() {
        mFragment = spy(new ToggleFeaturePreferenceFragmentTestable());
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId*/,
                null /* bundle */);

        // execute exactly once
        verify(mFragment).addPreferencesFromResource(R.xml.placeholder_prefs);
    }

    public static class ToggleFeaturePreferenceFragmentTestable
            extends ToggleFeaturePreferenceFragment {

        @Override
        protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return mock(View.class);
        }

        @Override
        public void addPreferencesFromResource(@XmlRes int preferencesResId) {
            // do nothing
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // do nothing
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            mSwitchBar = mock(SwitchBar.class);
            super.onActivityCreated(savedInstanceState);
        }
    }
}
