/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Locale;
import java.util.Set;

/**
 * Create a dialog for system region events.
 */
public class RegionDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "RegionDialogFragment";
    static final int DIALOG_CHANGE_LOCALE_REGION = 1;
    static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    static final String ARG_TARGET_LOCALE = "arg_target_locale";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RegionDialogFragment.
     */
    @NonNull
    public static RegionDialogFragment newInstance() {
        return new RegionDialogFragment();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CHANGE_REGION_DIALOG;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // TODO(385834414): Migrate to use MaterialAlertDialogBuilder
        RegionDialogController controller = getRegionDialogController(getContext(), this);
        RegionDialogController.DialogContent dialogContent = controller.getDialogContent();
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getContext()).inflate(
                R.layout.locale_dialog, null);
        setDialogTitle(viewGroup, dialogContent.mTitle);
        setDialogMessage(viewGroup, dialogContent.mMessage);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setView(viewGroup);
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

    @VisibleForTesting
    RegionDialogController getRegionDialogController(Context context,
            RegionDialogFragment dialogFragment) {
        return new RegionDialogController(context, dialogFragment);
    }

    class RegionDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final MetricsFeatureProvider mMetricsFeatureProvider;

        RegionDialogController(
                @NonNull Context context, @NonNull RegionDialogFragment dialogFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);
            mMetricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        }

        @Override
        public void onClick(@NonNull DialogInterface dialog, int which) {
            if (mDialogType == DIALOG_CHANGE_LOCALE_REGION) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    updateRegion(mLocaleInfo.getLocale().toLanguageTag());
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_CHANGE_REGION_DIALOG_POSITIVE_BTN_CLICKED);
                }
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_CHANGE_REGION_DIALOG_NEGATIVE_BTN_CLICKED);
                }
                dismiss();
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        }

        @VisibleForTesting
        DialogContent getDialogContent() {
            DialogContent dialogContent = new DialogContent();
            switch (mDialogType) {
                case DIALOG_CHANGE_LOCALE_REGION:
                    dialogContent.mTitle = String.format(mContext.getString(
                        R.string.title_change_system_region),
                        mLocaleInfo.getLocale().getDisplayCountry());
                    dialogContent.mMessage = mContext.getString(
                        R.string.desc_notice_device_region_change,
                        Locale.getDefault().getDisplayLanguage());
                    dialogContent.mPositiveButton = mContext.getString(
                        R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                default:
                    break;
            }
            return dialogContent;
        }

        private void updateRegion(String selectedLanguageTag) {
            LocaleList localeList = LocaleList.getDefault();
            Locale systemLocale = Locale.getDefault();
            Set<Character> extensionKeys = systemLocale.getExtensionKeys();
            Locale selectedLocale = Locale.forLanguageTag(selectedLanguageTag);
            Locale.Builder builder = new Locale.Builder();
            builder.setLocale(selectedLocale);
            if (!extensionKeys.isEmpty()) {
                for (Character extKey : extensionKeys) {
                    builder.setExtension(extKey, systemLocale.getExtension(extKey));
                }
            }
            Locale newLocale = builder.build();
            Locale[] resultLocales = new Locale[localeList.size()];
            resultLocales[0] = newLocale;
            for (int i = 1; i < localeList.size(); i++) {
                resultLocales[i] = localeList.get(i);
            }
            LocalePicker.updateLocales(new LocaleList(resultLocales));
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
