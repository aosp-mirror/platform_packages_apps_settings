/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardItemsLayout;
import com.android.setupwizardlib.items.Item;
import com.android.setupwizardlib.items.ItemAdapter;

/**
 * Onboarding activity for fingerprint enrollment.
 */
public class FingerprintEnrollIntroduction extends FingerprintEnrollBase
        implements AdapterView.OnItemClickListener {

    protected static final int CHOOSE_LOCK_GENERIC_REQUEST = 1;
    protected static final int FINGERPRINT_FIND_SENSOR_REQUEST = 2;

    private boolean mHasPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_introduction);
        setHeaderText(R.string.security_settings_fingerprint_enroll_introduction_title);
        final SetupWizardItemsLayout layout =
                (SetupWizardItemsLayout) findViewById(R.id.setup_wizard_layout);
        layout.getListView().setOnItemClickListener(this);
        final ItemAdapter adapter = (ItemAdapter) layout.getAdapter();
        Item item = (Item) adapter.findItemById(R.id.fingerprint_introduction_message_warning);
        item.setTitle(LearnMoreSpan.linkify(
                getText(R.string.security_settings_fingerprint_enroll_introduction_message_warning),
                getString(R.string.help_url_fingerprint)));
        updatePasswordQuality();
    }

    private void updatePasswordQuality() {
        final int passwordQuality = new ChooseLockSettingsHelper(this).utils()
                .getActivePasswordQuality(mUserId);
        mHasPassword = passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Override
    protected void onNextButtonClick() {
        if (!mHasPassword) {
            // No fingerprints registered, launch into enrollment wizard.
            launchChooseLock();
        } else {
            // Lock thingy is already set up, launch directly into find sensor step from wizard.
            launchFindSensor(null);
        }
    }

    private void launchChooseLock() {
        Intent intent = getChooseLockIntent();
        long challenge = getSystemService(FingerprintManager.class).preEnroll();
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
    }

    private void launchFindSensor(byte[] token) {
        Intent intent = getFindSensorIntent();
        if (token != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, FINGERPRINT_FIND_SENSOR_REQUEST);
    }

    protected Intent getChooseLockIntent() {
        return new Intent(this, ChooseLockGeneric.class);
    }

    protected Intent getFindSensorIntent() {
        Intent intent = new Intent(this, FingerprintEnrollFindSensor.class);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final boolean isResultFinished = resultCode == RESULT_FINISHED;
        if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST) {
            if (isResultFinished || resultCode == RESULT_SKIP) {
                final int result = isResultFinished ? RESULT_OK : RESULT_SKIP;
                setResult(result, data);
                finish();
                return;
            }
        } else if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
            if (isResultFinished) {
                updatePasswordQuality();
                byte[] token = data.getByteArrayExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                launchFindSensor(token);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item item = (Item) parent.getItemAtPosition(position);
        switch (item.getId()) {
            case R.id.next_button:
                onNextButtonClick();
                break;
            case R.id.cancel_button:
                onCancelButtonClick();
                break;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_INTRO;
    }

    protected void onCancelButtonClick() {
        finish();
    }

    private static class LearnMoreSpan extends URLSpan {
        private static final String TAG = "LearnMoreSpan";
        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private LearnMoreSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            try {
                ((Activity) ctx).startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w(LearnMoreSpan.TAG,
                        "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
            }
            return builder;
        }
    }
}
