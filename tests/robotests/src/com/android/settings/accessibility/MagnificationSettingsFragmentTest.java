/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link MagnificationSettingsFragment} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class MagnificationSettingsFragmentTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private MagnificationSettingsFragment mFragment;
    private PreferenceScreen mScreen;

    @Before
    public void setup() {
        mContext.setTheme(R.style.Theme_AppCompat);
        mFragment = spy(new MagnificationSettingsFragment());
        mScreen = new PreferenceScreen(mContext, null);

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS)).when(
                mFragment).getChildFragmentManager();

    }

    @Test
    public void showPreferenceOnTheScreen_setDialogHelper() {
        showPreferenceOnTheScreen(null);

        verify(mFragment).setDialogDelegate(any(MagnificationModePreferenceController.class));
    }

    @Test
    public void onCreateDialog_setDialogDelegate_invokeDialogDelegate() {
        final DialogCreatable dialogDelegate = mock(DialogCreatable.class, RETURNS_DEEP_STUBS);
        when(dialogDelegate.getDialogMetricsCategory(anyInt())).thenReturn(1);

        mFragment.setDialogDelegate(dialogDelegate);

        mFragment.onCreateDialog(1);
        mFragment.getDialogMetricsCategory(1);

        verify(dialogDelegate).onCreateDialog(1);
        verify(dialogDelegate).getDialogMetricsCategory(1);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks =
                ShortcutsSettingsFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_magnification_service_settings);
        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    private void showPreferenceOnTheScreen(Bundle savedInstanceState) {
        mFragment.onAttach(mContext);
        mFragment.onCreate(savedInstanceState);
        mFragment.onResume();
    }
}
