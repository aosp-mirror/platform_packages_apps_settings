/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.intentpicker;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A customized {@link InstrumentedDialogFragment} with multiple checkboxes. */
public class SupportedLinksDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "SupportedLinksDialogFrg";
    private static final String DLG_ID = "SupportedLinksDialog";

    private SupportedLinkViewModel mViewModel;
    private List<SupportedLinkWrapper> mSupportedLinkWrapperList;
    private String mPackage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPackage = getArguments().getString(AppLaunchSettings.APP_PACKAGE_KEY);
        mViewModel = ViewModelProviders.of(this.getActivity()).get(SupportedLinkViewModel.class);
        mSupportedLinkWrapperList = mViewModel.getSupportedLinkWrapperList();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final SupportedLinksAdapter adapter = new SupportedLinksAdapter(context,
                mSupportedLinkWrapperList);
        final AlertDialog.Builder builder = new AlertDialog
                .Builder(context)
                .setTitle(IntentPickerUtils.getCentralizedDialogTitle(getSupportedLinksTitle()))
                .setAdapter(adapter, /* listener= */ null)
                .setCancelable(true)
                .setPositiveButton(R.string.app_launch_supported_links_add, (dialog, id) -> {
                    doSelectedAction();
                })
                .setNegativeButton(R.string.app_launch_dialog_cancel, /* listener= */ null);
        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    /** Display the dialog. */
    public void showDialog(FragmentManager manager) {
        show(manager, DLG_ID);
    }

    private String getSupportedLinksTitle() {
        final int supportedLinksNo = mSupportedLinkWrapperList.size();
        return getResources().getQuantityString(
                R.plurals.app_launch_supported_links_title, supportedLinksNo, supportedLinksNo);
    }

    private void doSelectedAction() {
        final DomainVerificationManager manager = getActivity().getSystemService(
                DomainVerificationManager.class);
        final DomainVerificationUserState userState =
                IntentPickerUtils.getDomainVerificationUserState(manager, mPackage);
        if (userState == null || mSupportedLinkWrapperList == null) {
            return;
        }

        updateUserSelection(manager, userState);
        displaySelectedItem();
    }

    private void updateUserSelection(DomainVerificationManager manager,
            DomainVerificationUserState userState) {
        final Set<String> domainSet = new ArraySet<>();
        for (SupportedLinkWrapper wrapper : mSupportedLinkWrapperList) {
            if (wrapper.isChecked()) {
                domainSet.add(wrapper.getHost());
            }
        }
        if (domainSet.size() > 0) {
            setDomainVerificationUserSelection(manager, userState.getIdentifier(),
                    domainSet, /* enabled= */true);
        }
    }

    private void setDomainVerificationUserSelection(DomainVerificationManager manager,
            UUID identifier, Set<String> domainSet, boolean isEnabled) {
        try {
            manager.setDomainVerificationUserSelection(identifier, domainSet, isEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "addSelectedItems : " + e.getMessage());
        }
    }

    private void displaySelectedItem() {
        final List<Fragment> fragments = getActivity().getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof AppLaunchSettings) {
                ((AppLaunchSettings) fragment).addSelectedLinksPreference();
            }
        }
    }
}
