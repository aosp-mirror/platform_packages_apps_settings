/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.role.RoleManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.List;

/** Fragment for the final screen shown on successful completion of private space setup. */
public class SetupSuccessFragment extends InstrumentedFragment {
    private static final String TAG = "SetupSuccessFragment";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()
                || !android.multiuser.Flags.enablePrivateSpaceFeatures()) {
            return null;
        }
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.private_space_setup_success, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_done_label)
                        .setListener(onClickNext())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        LottieAnimationView lottieAnimationView = rootView.findViewById(R.id.lottie_animation);
        LottieColorUtils.applyDynamicColors(getContext(), lottieAnimationView);

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_FINISH;
    }

    private View.OnClickListener onClickNext() {
        return v -> {
            Activity activity = getActivity();
            if (activity != null) {
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_DONE);
                Intent allAppsIntent = new Intent(Intent.ACTION_ALL_APPS);
                ResolveInfo resolveInfo =
                        activity.getPackageManager()
                                .resolveActivityAsUser(
                                        new Intent(Intent.ACTION_MAIN)
                                                .addCategory(Intent.CATEGORY_HOME),
                                        PackageManager.MATCH_SYSTEM_ONLY,
                                        activity.getUserId());
                if (resolveInfo != null) {
                    RoleManager mRoleManager = getContext().getSystemService(RoleManager.class);
                    final List<String> packageNames = mRoleManager
                            .getRoleHolders(RoleManager.ROLE_HOME);
                    if (packageNames.contains(resolveInfo.activityInfo.packageName)) {
                        allAppsIntent.setPackage(resolveInfo.activityInfo.packageName);
                        allAppsIntent.setComponent(resolveInfo.activityInfo.getComponentName());
                    }
                }
                activity.setTheme(R.style.Theme_SubSettings);
                if (allAppsIntent.getPackage() != null) {
                    accessPrivateSpaceToast();
                    startActivity(allAppsIntent);
                }
                Log.i(TAG, "Private space setup complete");
                deleteAllTaskAndFinish(activity);
            }
        };
    }

    private void accessPrivateSpaceToast() {
        Drawable drawable = getContext().getDrawable(R.drawable.ic_private_space_icon);
        Toast.makeCustomToastWithIcon(
                        getContext(),
                        null /* looper */ ,
                        getContext().getString(R.string.private_space_scrolldown_to_access),
                        Toast.LENGTH_SHORT,
                        drawable)
                .show();
    }

    private void deleteAllTaskAndFinish(Activity activity) {
        ActivityManager activityManager = activity.getSystemService(ActivityManager.class);
        List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
        for (var task : tasks) {
            task.finishAndRemoveTask();
        }
    }
}
