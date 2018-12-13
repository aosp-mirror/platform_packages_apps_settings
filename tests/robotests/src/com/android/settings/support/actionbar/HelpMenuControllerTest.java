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

package com.android.settings.support.actionbar;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;

import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HelpMenuControllerTest {

    @Mock
    private Context mContext;
    private TestFragment mHost;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mHost = spy(new TestFragment());
        doReturn(mContext).when(mHost).getContext();
    }

    @Test
    public void onCreateOptionsMenu_withArgumentOverride_shouldPrepareHelpUsingOverride() {
        final Bundle bundle = new Bundle();
        bundle.putInt(HelpResourceProvider.HELP_URI_RESOURCE_KEY, 123);
        mHost.setArguments(bundle);

        HelpMenuController.init(mHost);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(null /* menu */, null /* inflater */);

        verify(mContext).getString(123);
    }

    @Test
    public void onCreateOptionsMenu_noArgumentOverride_shouldPrepareHelpUsingProvider() {
        HelpMenuController.init(mHost);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(null /* menu */, null /* inflater */);

        verify(mContext).getString(mHost.getHelpResource());
    }

    private static class TestFragment extends ObservablePreferenceFragment
        implements HelpResourceProvider {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        }
    }
}
