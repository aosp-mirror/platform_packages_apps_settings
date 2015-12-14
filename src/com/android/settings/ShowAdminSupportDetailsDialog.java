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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ShowAdminSupportDetailsDialog extends Activity
        implements DialogInterface.OnDismissListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View rootView = LayoutInflater.from(this).inflate(
                R.layout.admin_support_details_dialog, null);
        setAdminSupportDetails(rootView);

        new AlertDialog.Builder(this)
                .setView(rootView)
                .setPositiveButton(R.string.okay, null)
                .setOnDismissListener(this)
                .show();
    }

    private void setAdminSupportDetails(View root) {
        CharSequence adminDisabledMsg = getString(R.string.disabled_by_admin_msg,
                getString(R.string.default_organisation_name));
        TextView textView = (TextView) root.findViewById(R.id.disabled_by_admin_msg);
        textView.setText(adminDisabledMsg);

        CharSequence adminSupportDetails = getString(R.string.default_admin_support_msg);
        textView = (TextView) root.findViewById(R.id.admin_support_msg);
        textView.setText(adminSupportDetails);

        root.findViewById(R.id.admins_policies_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setClass(ShowAdminSupportDetailsDialog.this,
                                Settings.DeviceAdminSettingsActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}