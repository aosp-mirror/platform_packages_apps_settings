/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to set the private dns
 */
public class PrivateDnsModeDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, RadioGroup.OnCheckedChangeListener, TextWatcher {

    public static final String ANNOTATION_URL = "url";

    private static final String TAG = "PrivateDnsModeDialogFragment";
    // DNS_MODE -> RadioButton id
    private static final Map<String, Integer> PRIVATE_DNS_MAP;

    static {
        PRIVATE_DNS_MAP = new HashMap<>();
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OFF, R.id.private_dns_mode_off);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPPORTUNISTIC, R.id.private_dns_mode_opportunistic);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, R.id.private_dns_mode_provider);
    }

    @VisibleForTesting
    static final String MODE_KEY = Settings.Global.PRIVATE_DNS_MODE;
    @VisibleForTesting
    static final String HOSTNAME_KEY = Settings.Global.PRIVATE_DNS_SPECIFIER;

    @VisibleForTesting
    EditText mEditText;
    @VisibleForTesting
    RadioGroup mRadioGroup;
    @VisibleForTesting
    Button mSaveButton;
    @VisibleForTesting
    String mMode;

    private final AnnotationSpan.LinkInfo mUrlLinkInfo = new AnnotationSpan.LinkInfo(
            ANNOTATION_URL, (widget) -> {
        final Context context = widget.getContext();
        final Intent intent = HelpUtils.getHelpIntent(context,
                getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        if (intent != null) {
            try {
                widget.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    });

    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(TAG) == null) {
            final PrivateDnsModeDialogFragment fragment = new PrivateDnsModeDialogFragment();
            fragment.show(fragmentManager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.select_private_dns_configuration_title)
                .setView(buildPrivateDnsView(context))
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(R.string.dlg_cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            mSaveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            updateDialogInfo();
        });
        return dialog;
    }

    private View buildPrivateDnsView(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        mMode = Settings.Global.getString(contentResolver, MODE_KEY);
        final View view = LayoutInflater.from(context).inflate(R.layout.private_dns_mode_dialog,
                null);

        mEditText = view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.addTextChangedListener(this);
        mEditText.setText(Settings.Global.getString(contentResolver, HOSTNAME_KEY));

        mRadioGroup = view.findViewById(R.id.private_dns_radio_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(mMode, R.id.private_dns_mode_opportunistic));

        final TextView helpTextView = view.findViewById(R.id.private_dns_help_info);
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        helpTextView.setText(AnnotationSpan.linkify(
                context.getText(R.string.private_dns_help_message), mUrlLinkInfo));

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mMode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
            // Only clickable if hostname is valid, so we could save it safely
            Settings.Global.putString(getContext().getContentResolver(), HOSTNAME_KEY,
                    mEditText.getText().toString());
        }

        mMetricsFeatureProvider.action(getContext(),
                MetricsProto.MetricsEvent.ACTION_PRIVATE_DNS_MODE, mMode);
        Settings.Global.putString(getContext().getContentResolver(), MODE_KEY, mMode);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_PRIVATE_DNS;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.private_dns_mode_off:
                mMode = PRIVATE_DNS_MODE_OFF;
                break;
            case R.id.private_dns_mode_opportunistic:
                mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
                break;
            case R.id.private_dns_mode_provider:
                mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
                break;
        }
        updateDialogInfo();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mSaveButton != null) {
            mSaveButton.setEnabled(isWeaklyValidatedHostname(mEditText.getText().toString()));
        }
    }

    private boolean isWeaklyValidatedHostname(String hostname) {
        // TODO(b/34953048): Find and use a better validation method.  Specifically:
        //     [1] this should reject IP string literals, and
        //     [2] do the best, simplest, future-proof verification that
        //         the input approximates a DNS hostname.
        final String WEAK_HOSTNAME_REGEX = "^[a-zA-Z0-9_.-]+$";
        return hostname.matches(WEAK_HOSTNAME_REGEX);
    }

    private void updateDialogInfo() {
        final boolean modeProvider = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME.equals(mMode);
        if (mEditText != null) {
            mEditText.setEnabled(modeProvider);
        }
        if (mSaveButton != null) {
            mSaveButton.setEnabled(
                    modeProvider
                            ? isWeaklyValidatedHostname(mEditText.getText().toString())
                            : true);
        }
    }

}
