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

import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivitySettingsManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.common.net.InternetDomainName;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to set the Private DNS
 */
public class PrivateDnsModeDialogPreference extends CustomDialogPreferenceCompat implements
        DialogInterface.OnClickListener, RadioGroup.OnCheckedChangeListener, TextWatcher {

    public static final String ANNOTATION_URL = "url";

    private static final String TAG = "PrivateDnsModeDialog";
    // DNS_MODE -> RadioButton id
    private static final Map<Integer, Integer> PRIVATE_DNS_MAP;

    // Must match ConnectivitySettingsUtils
    private static final int PRIVATE_DNS_MODE_CLOUDFLARE = 4;

    static {
        PRIVATE_DNS_MAP = new HashMap<>();
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OFF, R.id.private_dns_mode_off);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_CLOUDFLARE, R.id.private_dns_mode_cloudflare);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPPORTUNISTIC, R.id.private_dns_mode_opportunistic);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, R.id.private_dns_mode_provider);
    }

    @VisibleForTesting
    static final String MODE_KEY = Settings.Global.PRIVATE_DNS_MODE;
    @VisibleForTesting
    static final String HOSTNAME_KEY = Settings.Global.PRIVATE_DNS_SPECIFIER;

    public static String getHostnameFromSettings(ContentResolver cr) {
        return Settings.Global.getString(cr, HOSTNAME_KEY);
    }

    @VisibleForTesting
    EditText mEditText;
    @VisibleForTesting
    RadioGroup mRadioGroup;
    @VisibleForTesting
    int mMode;

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private final AnnotationSpan.LinkInfo mUrlLinkInfo = new AnnotationSpan.LinkInfo(
            ANNOTATION_URL, (widget) -> {
        final Context context = widget.getContext();
        final Intent intent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        if (intent != null) {
            try {
                widget.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    });

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (isDisabledByAdmin()) {
            // If the preference is disabled by the admin, set the inner item as enabled so
            // it could act as a click target. The preference itself will have been disabled
            // by the controller.
            holder.itemView.setEnabled(true);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        final Context context = getContext();
        final ContentResolver contentResolver = context.getContentResolver();

        mMode = ConnectivitySettingsManager.getPrivateDnsMode(context);

        mEditText = view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.addTextChangedListener(this);
        mEditText.setText(getHostnameFromSettings(contentResolver));

        mRadioGroup = view.findViewById(R.id.private_dns_radio_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(mMode, R.id.private_dns_mode_opportunistic));

        // Initial radio button text
        final RadioButton offRadioButton = view.findViewById(R.id.private_dns_mode_off);
        offRadioButton.setText(R.string.private_dns_mode_off);
        final RadioButton cloudflareRadioButton =
                view.findViewById(R.id.private_dns_mode_cloudflare);
        cloudflareRadioButton.setText(R.string.private_dns_mode_cloudflare);
        final RadioButton opportunisticRadioButton =
                view.findViewById(R.id.private_dns_mode_opportunistic);
        opportunisticRadioButton.setText(R.string.private_dns_mode_opportunistic);
        final RadioButton providerRadioButton = view.findViewById(R.id.private_dns_mode_provider);
        providerRadioButton.setText(R.string.private_dns_mode_provider);

        final TextView helpTextView = view.findViewById(R.id.private_dns_help_info);
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        final Intent helpIntent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(context,
                ANNOTATION_URL, helpIntent);
        if (linkInfo.isActionable()) {
            helpTextView.setText(AnnotationSpan.linkify(
                    context.getText(R.string.private_dns_help_message), linkInfo));
        } else {
            helpTextView.setText("");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final Context context = getContext();
            if (mMode == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME) {
                // Only clickable if hostname is valid, so we could save it safely
                ConnectivitySettingsManager.setPrivateDnsHostname(context,
                        mEditText.getText().toString());
            }

            FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context,
                    SettingsEnums.ACTION_PRIVATE_DNS_MODE, mMode);
            ConnectivitySettingsManager.setPrivateDnsMode(context, mMode);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.private_dns_mode_off) {
            mMode = PRIVATE_DNS_MODE_OFF;
        } else if (checkedId == R.id.private_dns_mode_cloudflare) {
            mMode = PRIVATE_DNS_MODE_CLOUDFLARE;
        } else if (checkedId == R.id.private_dns_mode_opportunistic) {
            mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        } else if (checkedId == R.id.private_dns_mode_provider) {
            mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
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
        updateDialogInfo();
    }

    @Override
    public void performClick() {
        EnforcedAdmin enforcedAdmin = getEnforcedAdmin();

        if (enforcedAdmin == null) {
            // If the restriction is not restricted by admin, continue as usual.
            super.performClick();
        } else {
            // Show a dialog explaining to the user why they cannot change the preference.
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), enforcedAdmin);
        }
    }

    private EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getContext(), UserManager.DISALLOW_CONFIG_PRIVATE_DNS, UserHandle.myUserId());
    }

    private boolean isDisabledByAdmin() {
        return getEnforcedAdmin() != null;
    }

    private Button getSaveButton() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return null;
        }
        return dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    }

    private void updateDialogInfo() {
        final boolean modeProvider = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME == mMode;
        if (mEditText != null) {
            mEditText.setEnabled(modeProvider);
        }
        final Button saveButton = getSaveButton();
        if (saveButton != null) {
            saveButton.setEnabled(modeProvider
                    ? InternetDomainName.isValid(mEditText.getText().toString())
                    : true);
        }
    }
}
