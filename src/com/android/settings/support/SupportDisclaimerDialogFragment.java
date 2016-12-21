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
import android.text.Annotation;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

/**
 * {@link DialogFragment} for support disclaimer.
 */
public final class SupportDisclaimerDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "SupportDisclaimerDialog";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_ACCOUNT = "extra_account";

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
        final Activity activity = getActivity();
        final SupportFeatureProvider supportFeatureProvider =
                FeatureFactory.getFactory(activity).getSupportFeatureProvider(activity);

        // sets the two links that go to privacy policy and terms of service
        disclaimer.setText(supportFeatureProvider.getDisclaimerStringResId());
        Spannable viewText = (Spannable) disclaimer.getText();
        stripUnderlines(viewText);
        SystemInformationSpan.linkify(viewText, this);
        // sets the link that launches a dialog to expose the signals we are sending
        return builder
                .setView(content)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_NEGATIVE) {
            mMetricsFeatureProvider.action(getContext(),
                    MetricsProto.MetricsEvent.ACTION_SUPPORT_DISCLAIMER_CANCEL);
            return;
        }
        final Activity activity = getActivity();
        final CheckBox doNotShow =
                (CheckBox) getDialog().findViewById(R.id.support_disclaimer_do_not_show_again);
        final boolean isChecked = doNotShow.isChecked();
        final SupportFeatureProvider supportFeatureProvider =
                FeatureFactory.getFactory(activity).getSupportFeatureProvider(activity);
        supportFeatureProvider.setShouldShowDisclaimerDialog(getContext(), !isChecked);
        final Bundle bundle = getArguments();
        if (isChecked) {
            mMetricsFeatureProvider.action(activity,
                    MetricsProto.MetricsEvent.ACTION_SKIP_DISCLAIMER_SELECTED);
        }
        mMetricsFeatureProvider.action(activity,
                MetricsProto.MetricsEvent.ACTION_SUPPORT_DISCLAIMER_OK);
        supportFeatureProvider.startSupport(getActivity(),
                bundle.getParcelable(EXTRA_ACCOUNT), bundle.getInt(EXTRA_TYPE));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mMetricsFeatureProvider.action(getContext(),
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
            if (!TextUtils.isEmpty(span.getURL())) {
                input.removeSpan(span);
                input.setSpan(new NoUnderlineUrlSpan(span.getURL()), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_SUPPORT_DISCLAIMER;
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

    /**
     * A {@link URLSpan} that opens a dialog when clicked
     */
    public static class SystemInformationSpan extends URLSpan {

        private static final String ANNOTATION_URL = "url";
        private final DialogFragment mDialog;
        private SupportFeatureProvider mSupport;

        public SystemInformationSpan(DialogFragment parent) {
            // sets the url to empty string so we can prevent the NoUnderlineUrlSpan from stripping
            // this one
            super("");
            mSupport  = FeatureFactory.getFactory(parent.getContext())
                    .getSupportFeatureProvider(parent.getContext());
            mDialog = parent;
        }

        @Override
        public void onClick(View widget) {
            Activity activity =  mDialog.getActivity();
            if (mSupport != null && activity != null) {
                // launch the system info fragment
                mSupport.launchSystemInfoFragment(mDialog.getArguments(),
                        activity.getFragmentManager());

                // dismiss this fragment
                mDialog.dismiss();
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            // remove underline
            ds.setUnderlineText(false);
        }

        /**
         * This method takes a string and turns it into a url span that will launch a
         * SupportSystemInformationDialogFragment
         * @param msg The text to turn into a link
         * @param parent The dialog the text is in
         * @return A CharSequence containing the original text content as a url
         */
        public static CharSequence linkify(Spannable msg, DialogFragment parent) {
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                if (ANNOTATION_URL.equals(annotation.getValue())) {
                    SystemInformationSpan link = new SystemInformationSpan(parent);
                    msg.removeSpan(annotation);
                    msg.setSpan(link, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return msg;
        }

        @VisibleForTesting
        public void setSupportProvider(SupportFeatureProvider prov) {
            mSupport = prov;
        }
    }
}
