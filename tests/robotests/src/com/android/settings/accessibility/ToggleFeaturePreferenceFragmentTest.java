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

import static com.google.common.truth.Truth.assertThat;

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
import com.android.settings.accessibility.ToggleFeaturePreferenceFragment.AccessibilityUserShortcutType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ToggleFeaturePreferenceFragmentTest {

    private ToggleFeaturePreferenceFragmentTestable mFragment;

    private static final String TEST_SERVICE_KEY_1 = "abc:111";
    private static final String TEST_SERVICE_KEY_2 = "mno:222";
    private static final String TEST_SERVICE_KEY_3 = "xyz:333";

    private static final String TEST_SERVICE_NAME_1 = "abc";
    private static final int TEST_SERVICE_VALUE_1 = 111;

    @Test
    public void a11yUserShortcutType_setConcatString_shouldReturnTargetValue() {
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                TEST_SERVICE_KEY_1);

        assertThat(shortcut.getComponentName()).isEqualTo(TEST_SERVICE_NAME_1);
        assertThat(shortcut.getType()).isEqualTo(TEST_SERVICE_VALUE_1);
    }

    @Test
    public void a11yUserShortcutType_shouldUpdateConcatString() {
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                TEST_SERVICE_KEY_2);

        shortcut.setComponentName(TEST_SERVICE_NAME_1);
        shortcut.setType(TEST_SERVICE_VALUE_1);

        assertThat(shortcut.flattenToString()).isEqualTo(TEST_SERVICE_KEY_1);
    }

    @Test
    public void stringSet_convertA11yPreferredShortcut_shouldRemoveTarget() {
        Set<String> mySet = new HashSet<>();
        mySet.add(TEST_SERVICE_KEY_1);
        mySet.add(TEST_SERVICE_KEY_2);
        mySet.add(TEST_SERVICE_KEY_3);

        final Set<String> filtered = mySet.stream()
                .filter(str -> str.contains(TEST_SERVICE_NAME_1))
                .collect(Collectors.toSet());
        mySet.removeAll(filtered);

        assertThat(mySet).doesNotContain(TEST_SERVICE_KEY_1);
        assertThat(mySet).hasSize(/* expectedSize= */2);
    }

    @Test
    public void stringSet_convertA11yUserShortcutType_shouldReturnPreferredShortcut() {
        Set<String> mySet = new HashSet<>();
        mySet.add(TEST_SERVICE_KEY_1);
        mySet.add(TEST_SERVICE_KEY_2);
        mySet.add(TEST_SERVICE_KEY_3);

        final Set<String> filtered = mySet.stream()
                .filter(str -> str.contains(TEST_SERVICE_NAME_1))
                .collect(Collectors.toSet());

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        final int type = shortcut.getType();
        assertThat(type).isEqualTo(TEST_SERVICE_VALUE_1);
    }

    @Test
    public void createFragment_shouldOnlyAddPreferencesOnce() {
        mFragment = spy(new ToggleFeaturePreferenceFragmentTestable());
        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                /* containerViewId= */ 0, /* bundle= */null);

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
        int getUserShortcutTypes() {
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
    }
}
