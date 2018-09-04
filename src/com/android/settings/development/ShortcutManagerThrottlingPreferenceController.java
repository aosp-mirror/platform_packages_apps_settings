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

package com.android.settings.development;

import android.content.Context;
import android.content.pm.IShortcutService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShortcutManagerThrottlingPreferenceController extends
        DeveloperOptionsPreferenceController implements PreferenceControllerMixin {

    private static final String TAG = "ShortcutMgrPrefCtrl";

    private static final String SHORTCUT_MANAGER_RESET_KEY = "reset_shortcut_manager_throttling";

    private final IShortcutService mShortcutService;

    public ShortcutManagerThrottlingPreferenceController(Context context) {
        super(context);

        mShortcutService = getShortCutService();
    }

    @Override
    public String getPreferenceKey() {
        return SHORTCUT_MANAGER_RESET_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(SHORTCUT_MANAGER_RESET_KEY, preference.getKey())) {
            return false;
        }
        resetShortcutManagerThrottling();
        return true;
    }

    private void resetShortcutManagerThrottling() {
        if (mShortcutService == null) {
            return;
        }
        try {
            mShortcutService.resetThrottling();
            Toast.makeText(mContext, R.string.reset_shortcut_manager_throttling_complete,
                    Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to reset rate limiting", e);
        }
    }

    private IShortcutService getShortCutService() {
        try {
            return IShortcutService.Stub.asInterface(
                    ServiceManager.getService(Context.SHORTCUT_SERVICE));
        } catch (VerifyError e) {
            // Used for tests since Robolectric cannot initialize this class.
            return null;
        }
    }
}
