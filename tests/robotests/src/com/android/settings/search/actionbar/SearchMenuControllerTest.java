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

package com.android.settings.search.actionbar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchMenuControllerTest {

    @Mock
    private Menu mMenu;
    private TestFragment mHost;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mHost = new TestFragment();
    }

    @Test
    public void init_shouldAddMenu() {
        when(mMenu.add(Menu.NONE, Menu.NONE, 0 /* order */, R.string.search_menu))
                .thenReturn(mock(MenuItem.class));

        SearchMenuController.init(mHost);
        mHost.getLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verify(mMenu).add(Menu.NONE, Menu.NONE, 0 /* order */, R.string.search_menu);
    }

    public static class TestFragment extends ObservablePreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }
    }
}
