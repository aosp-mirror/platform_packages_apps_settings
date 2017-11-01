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

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.ResetNetwork;
import com.android.settings.Utils;

public class NetworkResetActionMenuController {

    private static final int MENU_NETWORK_RESET = Menu.FIRST + 200;
    private final Context mContext;
    private final NetworkResetRestrictionChecker mRestrictionChecker;

    public NetworkResetActionMenuController(Context context) {
        mContext = context;
        mRestrictionChecker = new NetworkResetRestrictionChecker(context);
    }

    public void buildMenuItem(Menu menu) {
        MenuItem item = null;
        if (isAvailable() && menu != null) {
            item = menu.add(0, MENU_NETWORK_RESET, 0, R.string.reset_network_title);
        }
        if (item != null) {
            item.setOnMenuItemClickListener(target -> {
                Utils.startWithFragment(mContext, ResetNetwork.class.getName(), null, null,
                        0, R.string.reset_network_title, null,
                        MetricsProto.MetricsEvent.SETTINGS_NETWORK_CATEGORY);
                return true;
            });
        }
    }


    boolean isAvailable() {
        return !mRestrictionChecker.hasRestriction();
    }
}
