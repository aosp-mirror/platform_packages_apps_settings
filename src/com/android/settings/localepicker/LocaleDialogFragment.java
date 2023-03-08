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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Create a dialog for system locale events.
 */
public class LocaleDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = LocaleDialogFragment.class.getSimpleName();

    static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 0;
    static final int DIALOG_NOT_AVAILABLE_LOCALE = 1;

    static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    static final String ARG_TARGET_LOCALE = "arg_target_locale";
    static final String ARG_RESULT_RECEIVER = "arg_result_receiver";

    /**
     * Show dialog
     */
    public static void show(
            @NonNull RestrictedSettingsFragment fragment,
            int dialogType,
            LocaleStore.LocaleInfo localeInfo) {
        show(fragment, dialogType, localeInfo, null);
    }

    /**
     * Show dialog
     */
    public static void show(
            @NonNull RestrictedSettingsFragment fragment,
            int dialogType,
            LocaleStore.LocaleInfo localeInfo,
            ResultReceiver resultReceiver) {
        FragmentManager manager = fragment.getChildFragmentManager();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, dialogType);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        args.putParcelable(ARG_RESULT_RECEIVER, resultReceiver);

        LocaleDialogFragment localeDialogFragment = new LocaleDialogFragment();
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(manager, TAG);
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LocaleDialogController controller = new LocaleDialogController(this);
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
        return builder.create();
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

    static class LocaleDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final ResultReceiver mResultReceiver;

        LocaleDialogController(
                @NonNull Context context, @NonNull LocaleDialogFragment dialogFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(
                    ARG_TARGET_LOCALE);
            mResultReceiver = (ResultReceiver) arguments.getParcelable(ARG_RESULT_RECEIVER);
        }

        LocaleDialogController(@NonNull LocaleDialogFragment dialogFragment) {
            this(dialogFragment.getContext(), dialogFragment);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mResultReceiver != null && mDialogType == DIALOG_CONFIRM_SYSTEM_DEFAULT) {
                Bundle bundle = new Bundle();
                bundle.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    mResultReceiver.send(Activity.RESULT_OK, bundle);
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    mResultReceiver.send(Activity.RESULT_CANCELED, bundle);
                }
            }
        }

        @VisibleForTesting
        DialogContent getDialogContent() {
            DialogContent
                    dialogContent = new DialogContent();
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
