/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.AppHeader;

public abstract class AppInfoWithHeader extends AppInfoBase {

    public static final String EXTRA_HIDE_INFO_BUTTON = "hideInfoButton";

    private boolean mCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) return;
        AppHeader.createAppHeader(this, mPackageInfo.applicationInfo.loadIcon(mPm),
                mPackageInfo.applicationInfo.loadLabel(mPm), getInfoIntent(this, mPackageName), 0);
    }

    public static Intent getInfoIntent(Fragment fragment, String packageName) {
        Bundle args = fragment.getArguments();
        Intent intent = fragment.getActivity().getIntent();
        boolean showInfo = true;
        if (args != null && args.getBoolean(EXTRA_HIDE_INFO_BUTTON, false)) {
            showInfo = false;
        }
        if (intent != null && intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)) {
            showInfo = false;
        }
        Intent infoIntent = null;
        if (showInfo) {
            infoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            infoIntent.setData(Uri.fromParts("package", packageName, null));
        }
        return infoIntent;
    }
}
