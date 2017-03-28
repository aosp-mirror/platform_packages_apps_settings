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

package com.android.settings.applications.instantapps;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppStoreUtil;
import com.android.settings.overlay.FeatureFactory;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/** Encapsulates a container for buttons relevant to instant apps */
public class InstantAppButtonsController {

    private final Context mContext;
    private final Fragment mFragment;
    private final View mView;
    private String mPackageName;

    public InstantAppButtonsController(Context context, Fragment fragment, View view) {
      mContext = context;
      mFragment = fragment;
      mView = view;
    }

    public InstantAppButtonsController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public void bindButtons() {
        Button installButton = (Button)mView.findViewById(R.id.install);
        Button clearDataButton = (Button)mView.findViewById(R.id.clear_data);
        Intent installIntent = AppStoreUtil.getAppStoreLink(mContext, mPackageName);
        if (installIntent != null) {
            installButton.setEnabled(true);
            installButton.setOnClickListener(v -> mFragment.startActivity(installIntent));
        }
        clearDataButton.setOnClickListener(v -> {
            FeatureFactory.getFactory(mContext).getMetricsFeatureProvider().action(mContext,
                    MetricsEvent.ACTION_SETTINGS_CLEAR_INSTANT_APP, mPackageName);
            PackageManager pm = mContext.getPackageManager();
            pm.clearApplicationUserData(mPackageName, null);
        });
    }

    public InstantAppButtonsController show() {
        bindButtons();
        mView.setVisibility(View.VISIBLE);
        return this;
    }
}
