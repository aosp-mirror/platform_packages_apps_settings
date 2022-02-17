/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settingslib.HelpUtils;

final class ActionDisabledByAppOpsHelper {

    private final ViewGroup mDialogView;
    private final Activity mActivity;

    ActionDisabledByAppOpsHelper(Activity activity) {
        mActivity = activity;
        mDialogView = (ViewGroup) LayoutInflater.from(mActivity).inflate(
                R.layout.support_details_dialog, null);
    }

    public AlertDialog.Builder prepareDialogBuilder() {
        final String helpUrl = mActivity.getString(
                R.string.help_url_action_disabled_by_restricted_settings);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setPositiveButton(R.string.okay, null)
                .setView(mDialogView);
        if (!TextUtils.isEmpty(helpUrl)) {
            builder.setNeutralButton(R.string.learn_more,
                    (DialogInterface.OnClickListener) (dialog, which) -> {
                        final Intent intent = HelpUtils.getHelpIntent(mActivity,
                                helpUrl, mActivity.getClass().getName());
                        if (intent != null) {
                            mActivity.startActivity(intent);
                        }
                    });
        }
        initializeDialogViews(mDialogView);
        return builder;
    }

    public void updateDialog() {
        initializeDialogViews(mDialogView);
    }

    private void initializeDialogViews(View root) {
        setSupportTitle(root);
        setSupportDetails(root);
    }

    @VisibleForTesting
    void setSupportTitle(View root) {
        final TextView titleView = root.findViewById(R.id.admin_support_dialog_title);
        if (titleView == null) {
            return;
        }
        titleView.setText(R.string.blocked_by_restricted_settings_title);
    }

    void setSupportDetails(final View root) {
        final TextView textView = root.findViewById(R.id.admin_support_msg);
        textView.setText(R.string.blocked_by_restricted_settings_content);
    }
}
