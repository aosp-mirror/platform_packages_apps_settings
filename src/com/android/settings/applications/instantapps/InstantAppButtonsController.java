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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppStoreUtil;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.overlay.FeatureFactory;

/** Encapsulates a container for buttons relevant to instant apps */
public class InstantAppButtonsController implements DialogInterface.OnClickListener {

    public interface ShowDialogDelegate {
        /**
         * Delegate that should be called when this controller wants to show a dialog.
         */
        void showDialog(int id);
    }

    private final Context mContext;
    private final Fragment mFragment;
    private final View mView;
    private final PackageManagerWrapper mPackageManagerWrapper;
    private final ShowDialogDelegate mShowDialogDelegate;
    private String mPackageName;

    public static final int DLG_BASE = 0x5032;
    public static final int DLG_CLEAR_APP = DLG_BASE + 1;

    public InstantAppButtonsController(
            Context context,
            Fragment fragment,
            View view,
            ShowDialogDelegate showDialogDelegate) {
      mContext = context;
      mFragment = fragment;
      mView = view;
      mShowDialogDelegate = showDialogDelegate;
      mPackageManagerWrapper = new PackageManagerWrapperImpl(context.getPackageManager());
    }

    public InstantAppButtonsController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public void bindButtons() {
        Button installButton = (Button)mView.findViewById(R.id.install);
        Button clearDataButton = (Button)mView.findViewById(R.id.clear_data);
        Intent appStoreIntent = AppStoreUtil.getAppStoreLink(mContext, mPackageName);
        if (appStoreIntent != null) {
            installButton.setEnabled(true);
            installButton.setOnClickListener(v -> mFragment.startActivity(appStoreIntent));
        }

        clearDataButton.setOnClickListener(v -> mShowDialogDelegate.showDialog(DLG_CLEAR_APP));
    }

    public AlertDialog createDialog(int id) {
        if (id == DLG_CLEAR_APP) {
            AlertDialog dialog = new AlertDialog.Builder(mFragment.getActivity())
                    .setPositiveButton(R.string.clear_instant_app_data, this)
                    .setNegativeButton(R.string.cancel, null)
                    .setTitle(R.string.clear_instant_app_data)
                    .setMessage(mContext.getString(R.string.clear_instant_app_confirmation))
                    .create();
            return dialog;
        }
        return null;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            FeatureFactory.getFactory(mContext)
                    .getMetricsFeatureProvider()
                    .action(mContext,
                            MetricsEvent.ACTION_SETTINGS_CLEAR_INSTANT_APP,
                            mPackageName);
            mPackageManagerWrapper.deletePackageAsUser(
                    mPackageName, null, 0, UserHandle.myUserId());
        }
    }

    public InstantAppButtonsController show() {
        bindButtons();
        mView.setVisibility(View.VISIBLE);
        return this;
    }
}
