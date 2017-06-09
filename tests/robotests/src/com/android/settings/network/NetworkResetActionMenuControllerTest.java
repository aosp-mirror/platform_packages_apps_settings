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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NetworkResetActionMenuControllerTest {

    private Context mContext;
    private NetworkResetActionMenuController mController;
    @Mock
    private Menu mMenu;
    @Mock
    private MenuItem mMenuItem;
    @Mock
    private NetworkResetRestrictionChecker mRestrictionChecker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NetworkResetActionMenuController(mContext);
        ReflectionHelpers.setField(mController, "mRestrictionChecker", mRestrictionChecker);
        when(mMenu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mMenuItem);
    }

    @Test
    public void buildMenuItem_available_shouldAddToMenu() {
        when(mRestrictionChecker.hasRestriction()).thenReturn(false);
        mController.buildMenuItem(mMenu);

        verify(mMenu).add(anyInt(), anyInt(), anyInt(), anyInt());
        verify(mMenuItem).setOnMenuItemClickListener(any(MenuItem.OnMenuItemClickListener.class));
    }

    @Test
    public void buildMenuItem_notAvailable_shouldNotAddToMenu() {
        when(mRestrictionChecker.hasRestriction()).thenReturn(true);

        mController.buildMenuItem(mMenu);

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }
}
