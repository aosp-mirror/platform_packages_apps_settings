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
 * limitations under the License
 */
package com.android.settings.system;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class FactoryResetPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {
    /** Key of the "Factory reset" preference in {@link R.xml.reset_dashboard_fragment}. */
    private static final String KEY_FACTORY_RESET = "factory_reset";

    private final UserManager mUm;

    public FactoryResetPreferenceController(Context context) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    /** Hide "Factory reset" settings for secondary users, except demo users. */
    @Override
    public boolean isAvailable() {
        return mUm.isAdminUser() || Utils.isDemoUser(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_FACTORY_RESET;
    }
}
