/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog.Builder;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreferenceCompat;

public class BugreportPreference extends CustomDialogPreferenceCompat {

    private static final String TAG = "BugreportPreference";

    private CheckedTextView mInteractiveTitle;
    private TextView mInteractiveSummary;
    private CheckedTextView mFullTitle;
    private TextView mFullSummary;

    public BugreportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        final View dialogView = View.inflate(getContext(), R.layout.bugreport_options_dialog, null);
        mInteractiveTitle = (CheckedTextView) dialogView
                .findViewById(R.id.bugreport_option_interactive_title);
        mInteractiveSummary = (TextView) dialogView
                .findViewById(R.id.bugreport_option_interactive_summary);
        mFullTitle = (CheckedTextView) dialogView.findViewById(R.id.bugreport_option_full_title);
        mFullSummary = (TextView) dialogView.findViewById(R.id.bugreport_option_full_summary);
        final View.OnClickListener l = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v == mFullTitle || v == mFullSummary) {
                    mInteractiveTitle.setChecked(false);
                    mFullTitle.setChecked(true);
                }
                if (v == mInteractiveTitle || v == mInteractiveSummary) {
                    mInteractiveTitle.setChecked(true);
                    mFullTitle.setChecked(false);
                }
            }
        };
        mInteractiveTitle.setOnClickListener(l);
        mFullTitle.setOnClickListener(l);
        mInteractiveSummary.setOnClickListener(l);
        mFullSummary.setOnClickListener(l);

        builder.setPositiveButton(com.android.internal.R.string.report, listener);
        builder.setView(dialogView);
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {

            final Context context = getContext();
            if (mFullTitle.isChecked()) {
                Log.v(TAG, "Taking full bugreport right away");
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(context,
                        SettingsEnums.ACTION_BUGREPORT_FROM_SETTINGS_FULL);
                try {
                    ActivityManager.getService().requestFullBugReport();
                } catch (RemoteException e) {
                    Log.e(TAG, "error taking bugreport (bugreportType=Full)", e);
                }
            } else {
                Log.v(TAG, "Taking interactive bugreport right away");
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(context,
                        SettingsEnums.ACTION_BUGREPORT_FROM_SETTINGS_INTERACTIVE);
                try {
                    ActivityManager.getService().requestInteractiveBugReport();
                } catch (RemoteException e) {
                    Log.e(TAG, "error taking bugreport (bugreportType=Interactive)", e);
                }
            }
        }
    }
}
