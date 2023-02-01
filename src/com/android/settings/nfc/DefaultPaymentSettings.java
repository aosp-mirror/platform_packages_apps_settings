/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.nfc;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DefaultPaymentSettings handles the NFC default payment app selection.
 */
public class DefaultPaymentSettings extends DefaultAppPickerFragment {
    public static final String TAG = "DefaultPaymentSettings";

    private PaymentBackend mPaymentBackend;
    private List<PaymentAppInfo> mAppInfos;
    private FooterPreference mFooterPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NFC_DEFAULT_PAYMENT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.nfc_default_payment_settings;
    }

    @Override
    protected String getDefaultKey() {
        PaymentAppInfo defaultAppInfo = mPaymentBackend.getDefaultApp();
        if (defaultAppInfo != null) {
            return defaultAppInfo.componentName.flattenToString() + " "
                    + defaultAppInfo.userHandle.getIdentifier();
        }
        return null;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        String[] keys = key.split(" ");
        if (keys.length >= 2) {
            mPaymentBackend.setDefaultPaymentApp(ComponentName.unflattenFromString(keys[0]),
                    Integer.parseInt(keys[1]));
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPaymentBackend = new PaymentBackend(getActivity());
        mAppInfos = mPaymentBackend.getPaymentAppInfos();
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        if (mFooterPreference == null) {
            setupFooterPreference();
        }
        screen.addPreference(mFooterPreference);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaymentBackend.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaymentBackend.onPause();
    }

    /**
     * Comparator for NfcPaymentCandidateInfo.
     */
    public class NfcPaymentCandidateInfoComparator implements Comparator<NfcPaymentCandidateInfo> {
        /**
         * Compare the NfcPaymentCandidateInfo by the label string.
         */
        public int compare(NfcPaymentCandidateInfo obj1, NfcPaymentCandidateInfo obj2) {
            if (obj1.loadLabel() == obj2.loadLabel()) {
                return 0;
            }
            if (obj1.loadLabel() == null) {
                return -1;
            }
            if (obj2.loadLabel() == null) {
                return 1;
            }
            return obj1.loadLabel().toString().compareTo(obj2.loadLabel().toString());
        }
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref, String key,
            CandidateInfo info, String defaultKey, String systemDefaultKey) {
        final NfcPaymentCandidateInfo candidateInfo = (NfcPaymentCandidateInfo) info;
        if (candidateInfo.isManagedProfile()) {
            final String textWork = getContext().getString(R.string.nfc_work_text);
            pref.setSummary(textWork);
        }
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<NfcPaymentCandidateInfo> candidates = new ArrayList<>();
        for (PaymentAppInfo appInfo: mAppInfos) {
            UserManager um = getContext().createContextAsUser(
                    appInfo.userHandle, /*flags=*/0).getSystemService(UserManager.class);
            boolean isManagedProfile = um.isManagedProfile(appInfo.userHandle.getIdentifier());

            CharSequence label;
            label = appInfo.label;
            candidates.add(new NfcPaymentCandidateInfo(
                    appInfo.componentName.flattenToString(),
                    label,
                    appInfo.icon,
                    appInfo.userHandle.getIdentifier(),
                    isManagedProfile));
        }
        Collections.sort(candidates, new NfcPaymentCandidateInfoComparator());
        return candidates;
    }

    @VisibleForTesting
    class NfcPaymentCandidateInfo extends CandidateInfo {
        private final String mKey;
        private final CharSequence mLabel;
        private final Drawable mDrawable;
        private final int mUserId;
        private final boolean mIsManagedProfile;

        NfcPaymentCandidateInfo(String key, CharSequence label, Drawable drawable, int userId,
                boolean isManagedProfile) {
            super(true /* enabled */);
            mKey = key;
            mLabel = label;
            mDrawable = drawable;
            mUserId = userId;
            mIsManagedProfile = isManagedProfile;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return mDrawable;
        }

        @Override
        public String getKey() {
            return mKey + " " + mUserId;
        }

        public boolean isManagedProfile() {
            return mIsManagedProfile;
        }
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        NfcPaymentCandidateInfo paymentInfo = (NfcPaymentCandidateInfo) appInfo;
        UserManager um = getContext().createContextAsUser(UserHandle.of(paymentInfo.mUserId),
                /*flags=*/0).getSystemService(UserManager.class);
        boolean isManagedProfile = um.isManagedProfile(paymentInfo.mUserId);
        if (!isManagedProfile) {
            return null;
        }

        final String title = getContext().getString(
                R.string.nfc_default_payment_workapp_confirmation_title);
        final String messageTitle = getContext().getString(
                R.string.nfc_default_payment_workapp_confirmation_message_title);
        final String messageOne = getContext().getString(
                R.string.nfc_default_payment_workapp_confirmation_message_1);
        final String messageTwo = getContext().getString(
                R.string.nfc_default_payment_workapp_confirmation_message_2);
        final SpannableString titleString = new SpannableString(title);
        final SpannableString messageString = new SpannableString(messageTitle);
        final SpannableString oneString = new SpannableString(messageOne);
        final SpannableString twoString = new SpannableString(messageTwo);

        titleString.setSpan(new RelativeSizeSpan(1.5f), 0, title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleString.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0,
                title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageString.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0,
                messageTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        oneString.setSpan(new BulletSpan(20), 0, messageOne.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        twoString.setSpan(new BulletSpan(20), 0, messageTwo.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return TextUtils.concat(titleString, "\n\n", messageString, "\n\n", oneString, "\n",
                twoString);
    }

    private void setupFooterPreference() {
        mFooterPreference = new FooterPreference(getContext());
        mFooterPreference.setTitle(getResources().getString(R.string.nfc_default_payment_footer));
        mFooterPreference.setIcon(R.drawable.ic_info_outline_24dp);
        mFooterPreference.setLearnMoreAction(v -> {
            final Intent howItWorksIntent = new Intent(getActivity(), HowItWorks.class);
            getContext().startActivity(howItWorksIntent);
        });
    }
}
