/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.settings.R;

import java.util.Locale;

/**
 * A dialog fragment that displays support phone numbers.
 */
public final class SupportPhoneDialogFragment extends DialogFragment implements
        View.OnClickListener {

    public static final String TAG = "SupportPhoneDialog";
    private static final String EXTRA_PHONE = "extra_phone";

    public static SupportPhoneDialogFragment newInstance(SupportPhone phone) {
        final SupportPhoneDialogFragment fragment = new SupportPhoneDialogFragment();
        final Bundle bundle = new Bundle(2);
        bundle.putParcelable(EXTRA_PHONE, phone);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SupportPhone phone = getArguments().getParcelable(EXTRA_PHONE);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.support_international_phone_title);
        final View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.support_phone_dialog_content, null);
        final View phoneNumberContainer = content.findViewById(R.id.phone_number_container);
        final TextView phoneView = (TextView) content.findViewById(R.id.phone_number);
        final String formattedPhoneNumber = getContext().getString(
                R.string.support_phone_international_format,
                new Locale(phone.language).getDisplayLanguage(), phone.number);
        phoneView.setText(formattedPhoneNumber);
        phoneNumberContainer.setOnClickListener(this);
        return builder
                .setView(content)
                .create();
    }

    @Override
    public void onClick(View v) {
        final SupportPhone phone = getArguments().getParcelable(EXTRA_PHONE);
        final Activity activity = getActivity();
        final Intent intent = phone.getDialIntent();
        final boolean canDial = !activity.getPackageManager()
                .queryIntentActivities(intent, 0)
                .isEmpty();
        if (canDial) {
            MetricsLogger.action(getActivity(),
                    MetricsProto.MetricsEvent.ACTION_SUPPORT_DIAL_TOLLED);
            getActivity().startActivity(intent);
        }
        dismiss();
    }
}
