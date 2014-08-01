/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

/**
 * This activity is displayed when an app launches the BIND_APPWIDGET intent. This allows apps
 * that don't have the BIND_APPWIDGET permission to bind specific widgets.
 */
public class AllowBindAppWidgetActivity extends AlertActivity implements
        DialogInterface.OnClickListener {

    private CheckBox mAlwaysUse;
    private int mAppWidgetId;
    private UserHandle mProfile;
    private ComponentName mComponentName;
    private String mCallingPackage;
    private AppWidgetManager mAppWidgetManager;

    // Indicates whether this activity was closed because of a click
    private boolean mClicked;

    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            // By default, set the result to cancelled
            setResult(RESULT_CANCELED);
            if (mAppWidgetId != -1 && mComponentName != null && mCallingPackage != null) {
                try {
                    final boolean bound = mAppWidgetManager.bindAppWidgetIdIfAllowed(mAppWidgetId,
                            mProfile, mComponentName, null);
                    if (bound) {
                        Intent result = new Intent();
                        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        setResult(RESULT_OK, result);
                    }
                } catch (Exception e) {
                    Log.v("BIND_APPWIDGET", "Error binding widget with id "
                            + mAppWidgetId + " and component " + mComponentName);
                }

                final boolean alwaysAllowBind = mAlwaysUse.isChecked();
                if (alwaysAllowBind != mAppWidgetManager.hasBindAppWidgetPermission(
                        mCallingPackage)) {
                    mAppWidgetManager.setBindAppWidgetPermission(mCallingPackage,
                            alwaysAllowBind);
                }
            }
        }
        finish();
    }

    protected void onPause() {
        if (isDestroyed() && !mClicked) {
            setResult(RESULT_CANCELED);
        }
        super.onDestroy();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        CharSequence label = "";
        if (intent != null) {
            try {
                mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                mProfile = intent.getParcelableExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                if (mProfile == null) {
                    mProfile = android.os.Process.myUserHandle();
                }
                mComponentName =
                        intent.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
                mCallingPackage = getCallingPackage();
                PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(mCallingPackage, 0);
                label = pm.getApplicationLabel(ai);
            } catch (Exception e) {
                mAppWidgetId = -1;
                mComponentName = null;
                mCallingPackage = null;
                Log.v("BIND_APPWIDGET", "Error getting parameters");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        AlertController.AlertParams ap = mAlertParams;
        ap.mTitle = getString(R.string.allow_bind_app_widget_activity_allow_bind_title);
        ap.mMessage = getString(R.string.allow_bind_app_widget_activity_allow_bind, label);
        ap.mPositiveButtonText = getString(R.string.create);
        ap.mNegativeButtonText = getString(android.R.string.cancel);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;
        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ap.mView = inflater.inflate(com.android.internal.R.layout.always_use_checkbox, null);
        mAlwaysUse = (CheckBox) ap.mView.findViewById(com.android.internal.R.id.alwaysUse);
        mAlwaysUse.setText(getString(R.string.allow_bind_app_widget_activity_always_allow_bind, label));

        mAlwaysUse.setPadding(mAlwaysUse.getPaddingLeft(),
                mAlwaysUse.getPaddingTop(),
                mAlwaysUse.getPaddingRight(),
                (int) (mAlwaysUse.getPaddingBottom() +
                        getResources().getDimension(R.dimen.bind_app_widget_dialog_checkbox_bottom_padding)));

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAlwaysUse.setChecked(mAppWidgetManager.hasBindAppWidgetPermission(mCallingPackage,
                mProfile.getIdentifier()));

        setupAlert();
    }
}
