/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.addappnetworks;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;

/**
 * The Fragment list those networks, which is proposed by other app, to user, and handle user's
 * choose on either saving those networks or rejecting the request.
 */
public class AddAppNetworksFragment extends InstrumentedFragment {
    public static final String TAG = "AddAppNetworksFragment";

    private TextView mTitleView;
    private TextView mSummaryView;
    private ImageView mIconView;

    @VisibleForTesting
    Button mCancelButton;
    @VisibleForTesting
    Button mSaveButton;
    @VisibleForTesting
    String mCallingPackageName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_add_app_networks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // init local variable.
        mTitleView = view.findViewById(R.id.app_title);
        mIconView = view.findViewById(R.id.app_icon);
        mCancelButton = view.findViewById(R.id.cancel);
        mSaveButton = view.findViewById(R.id.save);
        mSummaryView = view.findViewById(R.id.app_summary);

        // Assigns button listeners.
        mCancelButton.setOnClickListener(getCancelListener());
        mSaveButton.setOnClickListener(getSaveListener());

        final Bundle bundle = getArguments();
        createContent(bundle);
    }

    private void createContent(Bundle bundle) {
        final FragmentActivity activity = getActivity();

        // Assigns caller app icon and summary.
        mCallingPackageName =
                bundle.getString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME);
        assignAppIcon(activity, mCallingPackageName);
        assignTitleAndSummary(activity, mCallingPackageName);
    }

    private void assignAppIcon(Context context, String callingPackageName) {
        final Drawable drawable = loadPackageIconDrawable(context, callingPackageName);
        mIconView.setImageDrawable(drawable);
    }

    private Drawable loadPackageIconDrawable(Context context, String callingPackageName) {
        Drawable icon = null;
        try {
            icon = context.getPackageManager().getApplicationIcon(callingPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Cannot get application icon", e);
        }

        return icon;
    }

    private void assignTitleAndSummary(Context context, String callingPackageName) {
        // Assigns caller app name to title
        mTitleView.setText(getTitle());

        // Set summary
        mSummaryView.setText(getAddNetworkRequesterSummary(
                Utils.getApplicationLabel(context, callingPackageName)));
    }

    private CharSequence getAddNetworkRequesterSummary(CharSequence appName) {
        return getString(R.string.wifi_add_app_single_network_summary, appName);
    }

    private CharSequence getTitle() {
        return getString(R.string.wifi_add_app_single_network_title);
    }

    View.OnClickListener getCancelListener() {
        return (v) -> {
            Log.d(TAG, "User rejected to add network");
        };
    }

    View.OnClickListener getSaveListener() {
        return (v) -> {
            Log.d(TAG, "User agree to add networks");
        };
    }

    private void finishWithResult(int resultCode, ArrayList<Integer> resultArrayList) {
        if (resultArrayList != null) {
            Intent intent = new Intent();
            intent.putIntegerArrayListExtra(Settings.EXTRA_WIFI_CONFIGURATION_RESULT_LIST,
                    resultArrayList);
            getActivity().setResult(resultCode, intent);
        }
        getActivity().finish();
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/144891278): Need to define a new metric for this page, use the WIFI item first.
        return SettingsEnums.WIFI;
    }
}
