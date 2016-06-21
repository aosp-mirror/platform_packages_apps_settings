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

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

/**
 * {@link DialogFragment} for support disclaimer.
 */
public final class SupportDisclaimerDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    public static final String TAG = "SupportDisclaimerDialog";
    private static final String EXTRA_TYPE = "extra_type";
    private static final String EXTRA_ACCOUNT = "extra_account";

    public static SupportDisclaimerDialogFragment newInstance(Account account,
            @SupportFeatureProvider.SupportType int type) {
        final SupportDisclaimerDialogFragment fragment = new SupportDisclaimerDialogFragment();
        final Bundle bundle = new Bundle(2);
        bundle.putParcelable(SupportDisclaimerDialogFragment.EXTRA_ACCOUNT, account);
        bundle.putInt(SupportDisclaimerDialogFragment.EXTRA_TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.support_disclaimer_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this);
        final View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.support_disclaimer_content, null);
        final TextView disclaimer = (TextView) content.findViewById(R.id.support_disclaimer_text);
        disclaimer.setMovementMethod(LinkMovementMethod.getInstance());
        stripUnderlines((Spannable) disclaimer.getText());
        return builder
                .setView(content)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_NEGATIVE) {
            MetricsLogger.action(getContext(),
                    MetricsProto.MetricsEvent.ACTION_SUPPORT_DISCLAIMER_CANCEL);
            return;
        }
        final Activity activity = getActivity();
        final CheckBox doNotShow =
                (CheckBox) getDialog().findViewById(R.id.support_disclaimer_do_not_show_again);
        final SupportFeatureProvider supportFeatureProvider =
                FeatureFactory.getFactory(activity).getSupportFeatureProvider(activity);
        supportFeatureProvider.setShouldShowDisclaimerDialog(getContext(), !doNotShow.isChecked());
        final Bundle bundle = getArguments();
        MetricsLogger.action(activity, MetricsProto.MetricsEvent.ACTION_SUPPORT_DISCLAIMER_OK);
        supportFeatureProvider.startSupport(getActivity(),
                (Account) bundle.getParcelable(EXTRA_ACCOUNT), bundle.getInt(EXTRA_TYPE));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        MetricsLogger.action(getContext(),
                MetricsProto.MetricsEvent.ACTION_SUPPORT_DISCLAIMER_CANCEL);
    }

    /**
     * Removes the underlines of {@link android.text.style.URLSpan}s.
     */
    private static void stripUnderlines(Spannable input) {
        final URLSpan[] urls = input.getSpans(0, input.length(), URLSpan.class);

        for (URLSpan span : urls) {
            final int start = input.getSpanStart(span);
            final int end = input.getSpanEnd(span);
            input.removeSpan(span);
            input.setSpan(new NoUnderlineUrlSpan(span.getURL()), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * A {@link URLSpan} that doesn't decorate the link with underline.
     */
    public static class NoUnderlineUrlSpan extends URLSpan {

        public NoUnderlineUrlSpan(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}
