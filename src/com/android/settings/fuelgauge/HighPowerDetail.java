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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class HighPowerDetail extends DialogFragment implements OnClickListener {

    private final PowerWhitelistBackend mBackend = PowerWhitelistBackend.getInstance();

    private String mPackageName;
    private CharSequence mLabel;
    private Adapter mAdapter;
    private int mSelectedIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageName = getArguments().getString(AppInfoBase.ARG_PACKAGE_NAME);
        PackageManager pm = getContext().getPackageManager();
        try {
            mLabel = pm.getApplicationInfo(mPackageName, 0).loadLabel(pm);
        } catch (NameNotFoundException e) {
            mLabel = mPackageName;
        }
        mAdapter = new Adapter(getContext(), R.layout.radio_with_summary);
        mAdapter.add(new Pair<String, String>(getString(R.string.ignore_optimizations_on),
                getString(R.string.ignore_optimizations_on_desc)));
        mAdapter.add(new Pair<String, String>(getString(R.string.ignore_optimizations_off),
                getString(R.string.ignore_optimizations_off_desc)));
        mSelectedIndex = mBackend.isWhitelisted(mPackageName) ? 0 : 1;
        if (mBackend.isSysWhitelisted(mPackageName)) {
            mAdapter.setEnabled(1, false);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.ignore_optimizations_title, mLabel))
                .setNegativeButton(R.string.cancel, null)
                .setSingleChoiceItems(mAdapter, mSelectedIndex, this);
        if (!mBackend.isSysWhitelisted(mPackageName)) {
            b.setPositiveButton(R.string.done, this);
        }
        return b.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            boolean newValue = mSelectedIndex == 0;
            boolean oldValue = mBackend.isWhitelisted(mPackageName);
            if (newValue != oldValue) {
                if (newValue) {
                    mBackend.addApp(mPackageName);
                } else {
                    mBackend.removeApp(mPackageName);
                }
            }
        } else {
            mSelectedIndex = which;
        }
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        return context.getString(PowerWhitelistBackend.getInstance().isWhitelisted(pkg)
                ? R.string.high_power_on : R.string.high_power_off);
    }

    public static void show(Activity activity, String packageName) {
        HighPowerDetail fragment = new HighPowerDetail();
        Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, packageName);
        fragment.setArguments(args);
        fragment.show(activity.getFragmentManager(), HighPowerDetail.class.getSimpleName());
    }

    private class Adapter extends ArrayAdapter<Pair<String, String>> {
        private final SparseBooleanArray mEnabled = new SparseBooleanArray();

        public Adapter(Context context, int resource) {
            super(context, resource, android.R.id.title);
        }

        public void setEnabled(int index, boolean enabled) {
            mEnabled.put(index, enabled);
        }

        public boolean isEnabled(int position) {
            return mEnabled.get(position, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ((TextView) view.findViewById(android.R.id.title)).setText(getItem(position).first);
            ((TextView) view.findViewById(android.R.id.summary)).setText(getItem(position).second);
            view.setEnabled(isEnabled(position));
            return view;
        }
    }
}
