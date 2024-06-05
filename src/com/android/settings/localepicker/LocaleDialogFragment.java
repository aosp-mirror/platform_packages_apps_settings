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

package com.android.settings.localepicker;

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Create a dialog for system locale events.
 */
public class LocaleDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = LocaleDialogFragment.class.getSimpleName();

    static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 1;
    static final int DIALOG_NOT_AVAILABLE_LOCALE = 2;
    static final int DIALOG_ADD_SYSTEM_LOCALE = 3;

    static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    static final String ARG_TARGET_LOCALE = "arg_target_locale";
    static final String ARG_SHOW_DIALOG = "arg_show_dialog";

    private boolean mShouldKeepDialog;
    private AlertDialog mAlertDialog;
    private OnBackInvokedDispatcher mBackDispatcher;

    private OnBackInvokedCallback mBackCallback = () -> {
        Log.d(TAG, "Do not back to previous page if the dialog is displaying.");
    };

    public static LocaleDialogFragment newInstance() {
        return new LocaleDialogFragment();
    }

    @Override
    public int getMetricsCategory() {
        int dialogType = getArguments().getInt(ARG_DIALOG_TYPE);
        switch (dialogType) {
            case DIALOG_CONFIRM_SYSTEM_DEFAULT:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE;
            case DIALOG_NOT_AVAILABLE_LOCALE:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_UNAVAILABLE;
            default:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_SHOW_DIALOG, mShouldKeepDialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Bundle arguments = getArguments();
            int type = arguments.getInt(ARG_DIALOG_TYPE);
            mShouldKeepDialog = savedInstanceState.getBoolean(ARG_SHOW_DIALOG, false);
            // Keep the dialog if user rotates the device, otherwise close the confirm system
            // default dialog only when user changes the locale.
            if ((type == DIALOG_CONFIRM_SYSTEM_DEFAULT || type == DIALOG_ADD_SYSTEM_LOCALE)
                    && !mShouldKeepDialog) {
                dismiss();
            }
        }

        mShouldKeepDialog = true;
        LocaleListEditor parentFragment = (LocaleListEditor) getParentFragment();
        LocaleDialogController controller = getLocaleDialogController(getContext(), this,
                parentFragment);
        LocaleDialogController.DialogContent dialogContent = controller.getDialogContent();
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getContext()).inflate(
                R.layout.locale_dialog, null);
        setDialogTitle(viewGroup, dialogContent.mTitle);
        setDialogMessage(viewGroup, dialogContent.mMessage);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(viewGroup);
        if (!dialogContent.mPositiveButton.isEmpty()) {
            builder.setPositiveButton(dialogContent.mPositiveButton, controller);
        }
        if (!dialogContent.mNegativeButton.isEmpty()) {
            builder.setNegativeButton(dialogContent.mNegativeButton, controller);
        }
        mAlertDialog = builder.create();
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(PRIORITY_DEFAULT, mBackCallback);
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.setOnDismissListener(dialogInterface -> {
            mAlertDialog.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                            mBackCallback);
        });

        return mAlertDialog;
    }

    private static void setDialogTitle(View root, String content) {
        TextView titleView = root.findViewById(R.id.dialog_title);
        if (titleView == null) {
            return;
        }
        titleView.setText(content);
    }

    private static void setDialogMessage(View root, String content) {
        TextView textView = root.findViewById(R.id.dialog_msg);
        if (textView == null) {
            return;
        }
        textView.setText(content);
    }

    @VisibleForTesting
    public OnBackInvokedCallback getBackInvokedCallback() {
        return mBackCallback;
    }

    @VisibleForTesting
    public void setBackDispatcher(OnBackInvokedDispatcher dispatcher) {
        mBackDispatcher = dispatcher;
    }

    @VisibleForTesting
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        if (mBackDispatcher != null) {
            return mBackDispatcher;
        } else {
            return mAlertDialog.getOnBackInvokedDispatcher();
        }
    }

    @VisibleForTesting
    LocaleDialogController getLocaleDialogController(Context context,
            LocaleDialogFragment dialogFragment, LocaleListEditor parentFragment) {
        return new LocaleDialogController(context, dialogFragment, parentFragment);
    }

    class LocaleDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final MetricsFeatureProvider mMetricsFeatureProvider;

        private LocaleListEditor mParent;

        LocaleDialogController(
                @NonNull Context context, @NonNull LocaleDialogFragment dialogFragment,
                LocaleListEditor parentFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);
            mMetricsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
            mParent = parentFragment;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mDialogType == DIALOG_CONFIRM_SYSTEM_DEFAULT
                    || mDialogType == DIALOG_ADD_SYSTEM_LOCALE) {
                int result = Activity.RESULT_CANCELED;
                boolean changed = false;
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    result = Activity.RESULT_OK;
                    changed = true;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt(ARG_DIALOG_TYPE, mDialogType);
                bundle.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, mLocaleInfo);
                intent.putExtras(bundle);
                mParent.onActivityResult(mDialogType, result, intent);
                mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_CHANGE_LANGUAGE,
                        changed);
            }
            mShouldKeepDialog = false;
        }

        @VisibleForTesting
        DialogContent getDialogContent() {
            DialogContent dialogContent = new DialogContent();
            switch (mDialogType) {
                case DIALOG_CONFIRM_SYSTEM_DEFAULT:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_change_system_locale), mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                            R.string.desc_notice_device_locale_settings_change);
                    dialogContent.mPositiveButton = mContext.getString(
                            R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_NOT_AVAILABLE_LOCALE:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_unavailable_locale), mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(R.string.desc_unavailable_locale);
                    dialogContent.mPositiveButton = mContext.getString(R.string.okay);
                    break;
                case DIALOG_ADD_SYSTEM_LOCALE:
                    dialogContent.mTitle = String.format(mContext.getString(
                                    R.string.title_system_locale_addition),
                            mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                            R.string.desc_system_locale_addition);
                    dialogContent.mPositiveButton = mContext.getString(R.string.add);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                default:
                    break;
            }
            return dialogContent;
        }

        @VisibleForTesting
        static class DialogContent {
            String mTitle = "";
            String mMessage = "";
            String mPositiveButton = "";
            String mNegativeButton = "";
        }
    }
}
