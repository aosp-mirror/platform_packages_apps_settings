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

package com.android.settings.network;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class PrivateDnsMenuControllerTest {
    private static final int MENU_ID = 0;

    private PrivateDnsMenuController mController;
    @Mock
    private Menu mMenu;
    @Mock
    private MenuItem mMenuItem;
    @Mock
    private FragmentManager mFragmentManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new PrivateDnsMenuController(mFragmentManager, MENU_ID);
        when(mMenu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mMenuItem);
    }

    @Test
    public void buildMenuItem_available_shouldAddToMenu() {
        mController.buildMenuItem(mMenu);

        verify(mMenu).add(0 /* groupId */, MENU_ID, 0 /* order */,
                R.string.select_private_dns_configuration_title);
        verify(mMenuItem).setOnMenuItemClickListener(any(MenuItem.OnMenuItemClickListener.class));
    }
}
