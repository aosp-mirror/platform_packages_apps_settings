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

package com.android.settings.wifi.details;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.view.Menu;
import android.view.MenuInflater;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkDetailsFragmentTest {

    @Mock
    Menu mMenu;
    private WifiNetworkDetailsFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new WifiNetworkDetailsFragment();
    }

    @Test
    public void onCreateOptionsMenu_uiRestricted_shouldNotAddEditMenu() {
        mFragment.mIsUiRestricted = true;

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), eq(R.string.wifi_modify));
    }

    @Test
    public void restrictUi_shouldShowRestrictedText() {
        final WifiNetworkDetailsFragmentTest.FakeFragment
                fragment = spy(new WifiNetworkDetailsFragmentTest.FakeFragment());
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final TextView restrictedText = mock(TextView.class);
        doReturn(screen).when(fragment).getPreferenceScreen();
        doReturn(false).when(fragment).isUiRestrictedByOnlyAdmin();
        doReturn(restrictedText).when(fragment).getEmptyTextView();

        fragment.restrictUi();

        verify(restrictedText).setText(anyInt());
    }

    @Test
    public void restrictUi_shouldRemoveAllPreferences() {
        final WifiNetworkDetailsFragmentTest.FakeFragment
                fragment = spy(new WifiNetworkDetailsFragmentTest.FakeFragment());
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        doReturn(screen).when(fragment).getPreferenceScreen();
        doReturn(true).when(fragment).isUiRestrictedByOnlyAdmin();

        fragment.restrictUi();

        verify(screen).removeAll();
    }

    // Fake WifiNetworkDetailsFragment to override the protected method as public.
    public class FakeFragment extends WifiNetworkDetailsFragment {
        @Override
        public boolean isUiRestrictedByOnlyAdmin() {
            return super.isUiRestrictedByOnlyAdmin();
        }
    }
}