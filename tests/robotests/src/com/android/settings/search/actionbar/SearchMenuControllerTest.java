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

import static com.android.settings.search.actionbar.SearchMenuController.MENU_SEARCH;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class SearchMenuControllerTest {

    @Mock
    private Menu mMenu;
    private InstrumentedFragment mHost;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).get();
        mHost = spy(new InstrumentedFragment() {

            @Override
            public int getMetricsCategory() {
                return SettingsEnums.TESTING;
            }
        });
        Global.putInt(mActivity.getContentResolver(), Global.DEVICE_PROVISIONED, 1);

        when(mHost.getActivity()).thenReturn(mActivity);
        when(mMenu.add(Menu.NONE, MENU_SEARCH, 0 /* order */, R.string.search_menu))
                .thenReturn(mock(MenuItem.class));
    }

    @Test
    public void init_observableFragment_shouldAddMenu() {
        SearchMenuController.init(mHost);
        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verify(mMenu).add(Menu.NONE, MENU_SEARCH, 0 /* order */, R.string.search_menu);
    }

    @Test
    public void init_doNotNeedSearchIcon_shouldNotAddMenu() {
        final Bundle args = new Bundle();
        args.putBoolean(SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR, false);
        mHost.setArguments(args);

        SearchMenuController.init(mHost);
        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);
        verifyZeroInteractions(mMenu);
    }

    @Test
    public void init_deviceNotProvisioned_shouldNotAddMenu() {
        Global.putInt(mActivity.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        SearchMenuController.init(mHost);
        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verifyZeroInteractions(mMenu);
    }

    @Test
    public void init_startFromSetupWizard_shouldNotAddMenu() {
        final Intent intent = new Intent();
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        mActivity.setIntent(intent);
        SearchMenuController.init(mHost);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, null /* inflater */);

        verifyZeroInteractions(mMenu);
    }
}
