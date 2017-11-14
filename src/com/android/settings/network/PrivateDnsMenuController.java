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

import android.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;

public class PrivateDnsMenuController {
    private final FragmentManager mFragmentManager;
    private final int mMenuId;

    public PrivateDnsMenuController(FragmentManager fragmentManager, int menuId) {
        mFragmentManager = fragmentManager;
        mMenuId = menuId;
    }

    public void buildMenuItem(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.add(0 /* groupId */, mMenuId, 0 /* order */,
                    R.string.select_private_dns_configuration_title);
            item.setOnMenuItemClickListener(target -> {
                PrivateDnsModeDialogFragment.show(mFragmentManager);
                return true;
            });
        }
    }
}
