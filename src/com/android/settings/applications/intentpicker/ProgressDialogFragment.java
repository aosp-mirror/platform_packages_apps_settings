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

import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.verify.domain.DomainOwner;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/** A customized {@link InstrumentedDialogFragment} with a progress bar. */
public class ProgressDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "ProgressDialogFragment";
    private static final String DLG_ID = "ProgressDialog";
    private static final int PROGRESS_BAR_STEPPING_TIME = 20;

    private ProgressAlertDialog mProgressAlertDialog;
    private DomainVerificationManager mDomainVerificationManager;
    private List<SupportedLinkWrapper> mSupportedLinkWrapperList;
    private SupportedLinkViewModel mViewModel;
    private Handler mHandle;
    private String mPackage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this.getActivity()).get(SupportedLinkViewModel.class);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mPackage = getArguments().getString(AppLaunchSettings.APP_PACKAGE_KEY);
        mDomainVerificationManager = getActivity().getSystemService(
                DomainVerificationManager.class);
        mHandle = new Handler(Looper.getMainLooper());
        mProgressAlertDialog = createProgressAlertDialog();
        return mProgressAlertDialog;
    }

    private ProgressAlertDialog createProgressAlertDialog() {
        final Context context = getActivity();
        final ProgressAlertDialog progressDialog = new ProgressAlertDialog(context);
        final String title = context.getResources().getString(
                R.string.app_launch_checking_links_title);
        progressDialog.setTitle(IntentPickerUtils.getCentralizedDialogTitle(title));
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getText(R.string.app_launch_dialog_cancel),
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        dialog.cancel();
                    }
                });
        progressDialog.setCanceledOnTouchOutside(true);
        return progressDialog;
    }

    /** Display the {@link ProgressAlertDialog}. */
    public void showDialog(FragmentManager manager) {
        show(manager, DLG_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        generateProgressAlertDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProgressAlertDialog != null && mProgressAlertDialog.isShowing()) {
            mProgressAlertDialog.cancel();
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    /**
     * To generate a progress alter dialog and invoke the supported links dialog.
     */
    private void generateProgressAlertDialog() {
        ThreadUtils.postOnBackgroundThread(() -> {
            final long start = SystemClock.elapsedRealtime();
            queryLinksInBackground();
            IntentPickerUtils.logd(
                    "queryLinksInBackground take time: " + (SystemClock.elapsedRealtime() - start));
            if (mProgressAlertDialog.isShowing()) {
                mHandle.post(() -> {
                    synchronized (mHandle) {
                        if (mProgressAlertDialog.isShowing()) {
                            mProgressAlertDialog.dismiss();
                            IntentPickerUtils.logd("mProgressAlertDialog.dismiss() and isShowing: "
                                    + mProgressAlertDialog.isShowing());
                            launchSupportedLinksDialogFragment();
                        }
                    }
                });
            }
        });
    }

    private void queryLinksInBackground() {
        final List<String> links = IntentPickerUtils.getLinksList(mDomainVerificationManager,
                mPackage, DOMAIN_STATE_NONE);
        final int linksNo = links.size();
        int index = 0;
        mSupportedLinkWrapperList = new ArrayList<>();
        for (String host : links) {
            final SortedSet<DomainOwner> ownerSet =
                    mDomainVerificationManager.getOwnersForDomain(host);
            mSupportedLinkWrapperList.add(new SupportedLinkWrapper(getActivity(), host, ownerSet));
            index++;
            // The cancel was clicked while progressing to collect data.
            if (!mProgressAlertDialog.isShowing()) {
                Log.w(TAG, "Exit the background thread!!!");
                // clear buffer
                mSupportedLinkWrapperList.clear();
                break;
            }
            int progress = (int) (index * 100) / linksNo;
            mHandle.post(() -> {
                synchronized (mHandle) {
                    if (!mProgressAlertDialog.isShowing()) {
                        Log.w(TAG, "Exit the UI thread");
                        return;
                    }
                    mProgressAlertDialog.getProgressBar().setProgress(progress);
                }
            });
            if (ownerSet.size() == 0) {
                SystemClock.sleep(PROGRESS_BAR_STEPPING_TIME);
            }
        }
        IntentPickerUtils.logd("queryLinksInBackground : SupportedLinkWrapperList size="
                + mSupportedLinkWrapperList.size());
        Collections.sort(mSupportedLinkWrapperList);
    }

    private void launchSupportedLinksDialogFragment() {
        if (mSupportedLinkWrapperList.size() > 0) {
            mViewModel.setSupportedLinkWrapperList(mSupportedLinkWrapperList);
            final Bundle args = new Bundle();
            args.putString(AppLaunchSettings.APP_PACKAGE_KEY, mPackage);
            final SupportedLinksDialogFragment dialogFragment = new SupportedLinksDialogFragment();
            dialogFragment.setArguments(args);
            dialogFragment.showDialog(getActivity().getSupportFragmentManager());
        }
    }

    /** Create a custom {@link AlertDialog} with a {@link ProgressBar}. */
    static class ProgressAlertDialog extends AlertDialog {
        private ProgressBar mProgressBar;

        protected ProgressAlertDialog(@NonNull Context context) {
            this(context, 0);
        }

        protected ProgressAlertDialog(@NonNull Context context, int themeResId) {
            super(context, themeResId);
            init(context);
        }

        private void init(Context context) {
            final View view = LayoutInflater.from(context).inflate(
                    R.layout.app_launch_progress, /* root= */ null);
            mProgressBar = view.findViewById(R.id.scan_links_progressbar);
            mProgressBar.setProgress(0);
            mProgressBar.setMax(100);
            setView(view);
        }

        ProgressBar getProgressBar() {
            return mProgressBar;
        }
    }
}
