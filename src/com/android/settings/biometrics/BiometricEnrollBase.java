/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.core.InstrumentedActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.activityembedding.ActivityEmbeddingUtils;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Base activity for all biometric enrollment steps.
 */
public abstract class BiometricEnrollBase extends InstrumentedActivity {

    private static final String TAG = "BiometricEnrollBase";

    public static final String EXTRA_FROM_SETTINGS_SUMMARY = "from_settings_summary";
    public static final String EXTRA_KEY_LAUNCHED_CONFIRM = "launched_confirm_lock";
    public static final String EXTRA_KEY_REQUIRE_VISION = "accessibility_vision";
    public static final String EXTRA_KEY_REQUIRE_DIVERSITY = "accessibility_diversity";
    public static final String EXTRA_KEY_SENSOR_ID = "sensor_id";
    public static final String EXTRA_KEY_CHALLENGE = "challenge";
    public static final String EXTRA_KEY_MODALITY = "sensor_modality";
    public static final String EXTRA_KEY_NEXT_LAUNCHED = "next_launched";
    public static final String EXTRA_FINISHED_ENROLL_FACE = "finished_enrolling_face";
    public static final String EXTRA_FINISHED_ENROLL_FINGERPRINT = "finished_enrolling_fingerprint";
    public static final String EXTRA_LAUNCHED_POSTURE_GUIDANCE = "launched_posture_guidance";

    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     *
     * This must be the same as
     * {@link com.android.settings.password.ChooseLockPattern#RESULT_FINISHED}
     */
    public static final int RESULT_FINISHED = RESULT_FIRST_USER;

    /**
     * Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
     * will be useful if the user accidentally entered this flow.
     */
    public static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

    /**
     * Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
     * device was left idle. This is used to clear the credential token to require the user to
     * re-enter their pin/pattern/password before continuing.
     */
    public static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;

    /**
     * Used by consent screens to indicate that consent was granted. Extras, such as
     * EXTRA_KEY_MODALITY, will be included in the result to provide details about the
     * consent that was granted.
     */
    public static final int RESULT_CONSENT_GRANTED = RESULT_FIRST_USER + 3;

    /**
     * Used by consent screens to indicate that consent was denied. Extras, such as
     * EXTRA_KEY_MODALITY, will be included in the result to provide details about the
     * consent that was not granted.
     */
    public static final int RESULT_CONSENT_DENIED = RESULT_FIRST_USER + 4;

    public static final int CHOOSE_LOCK_GENERIC_REQUEST = 1;
    public static final int BIOMETRIC_FIND_SENSOR_REQUEST = 2;
    public static final int LEARN_MORE_REQUEST = 3;
    public static final int CONFIRM_REQUEST = 4;
    public static final int ENROLL_REQUEST = 5;

    /**
     * Request code when starting another biometric enrollment from within a biometric flow. For
     * example, when starting fingerprint enroll after face enroll.
     */
    public static final int ENROLL_NEXT_BIOMETRIC_REQUEST = 6;
    public static final int REQUEST_POSTURE_GUIDANCE = 7;

    protected boolean mLaunchedConfirmLock;
    protected boolean mLaunchedPostureGuidance;
    protected boolean mNextLaunched;
    protected byte[] mToken;
    protected int mUserId;
    protected int mSensorId;
    @BiometricUtils.DevicePostureInt
    protected int mDevicePostureState;
    protected long mChallenge;
    protected boolean mFromSettingsSummary;
    protected FooterBarMixin mFooterBarMixin;
    protected boolean mShouldSetFooterBarBackground = true;
    @Nullable
    protected ScreenSizeFoldProvider mScreenSizeFoldProvider;
    @Nullable
    protected Intent mPostureGuidanceIntent = null;
    @Nullable
    protected FoldProvider.FoldCallback mFoldCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        mChallenge = getIntent().getLongExtra(EXTRA_KEY_CHALLENGE, -1L);
        mSensorId = getIntent().getIntExtra(EXTRA_KEY_SENSOR_ID, -1);
        // Don't need to retrieve the HAT if it already exists. In some cases, the extras do not
        // contain EXTRA_KEY_CHALLENGE_TOKEN but contain EXTRA_KEY_GK_PW, in which case enrollment
        // classes may request a HAT to be created (as opposed to being passed in)
        if (mToken == null) {
            mToken = getIntent().getByteArrayExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        mFromSettingsSummary = getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS_SUMMARY, false);
        if (savedInstanceState != null) {
            if (mToken == null) {
                mLaunchedConfirmLock = savedInstanceState.getBoolean(EXTRA_KEY_LAUNCHED_CONFIRM);
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mFromSettingsSummary =
                        savedInstanceState.getBoolean(EXTRA_FROM_SETTINGS_SUMMARY, false);
                mChallenge = savedInstanceState.getLong(EXTRA_KEY_CHALLENGE);
                mSensorId = savedInstanceState.getInt(EXTRA_KEY_SENSOR_ID);
            }
            mLaunchedPostureGuidance = savedInstanceState.getBoolean(
                    EXTRA_LAUNCHED_POSTURE_GUIDANCE);
            mNextLaunched = savedInstanceState.getBoolean(EXTRA_KEY_NEXT_LAUNCHED);
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        mPostureGuidanceIntent = FeatureFactory.getFeatureFactory()
                .getFaceFeatureProvider().getPostureGuidanceIntent(getApplicationContext());

        // Remove the existing split screen dialog.
        BiometricsSplitScreenDialog dialog =
                (BiometricsSplitScreenDialog) getSupportFragmentManager()
                        .findFragmentByTag(BiometricsSplitScreenDialog.class.getName());
        if (dialog != null) {
            getSupportFragmentManager().beginTransaction().remove(dialog).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        outState.putBoolean(EXTRA_FROM_SETTINGS_SUMMARY, mFromSettingsSummary);
        outState.putLong(EXTRA_KEY_CHALLENGE, mChallenge);
        outState.putInt(EXTRA_KEY_SENSOR_ID, mSensorId);
        outState.putBoolean(EXTRA_LAUNCHED_POSTURE_GUIDANCE, mLaunchedPostureGuidance);
        outState.putBoolean(EXTRA_KEY_NEXT_LAUNCHED, mNextLaunched);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initViews();

        if (mShouldSetFooterBarBackground) {
            @SuppressLint("VisibleForTests")
            final LinearLayout buttonContainer = mFooterBarMixin != null
                    ? mFooterBarMixin.getButtonContainer()
                    : null;
            if (buttonContainer != null) {
                buttonContainer.setBackgroundColor(getBackgroundColor());
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setStatusBarColor(getBackgroundColor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mScreenSizeFoldProvider != null && mFoldCallback != null) {
            mScreenSizeFoldProvider.unregisterCallback(mFoldCallback);
        }
        mScreenSizeFoldProvider = null;
        mFoldCallback = null;

        if (!isChangingConfigurations() && shouldFinishWhenBackgrounded()
                && !BiometricUtils.isAnyMultiBiometricFlow(this)) {
            setResult(RESULT_TIMEOUT);
            finish();
        }
    }

    protected boolean launchPostureGuidance() {
        if (mPostureGuidanceIntent == null || mLaunchedPostureGuidance) {
            return false;
        }
        BiometricUtils.copyMultiBiometricExtras(getIntent(), mPostureGuidanceIntent);
        startActivityForResult(mPostureGuidanceIntent, REQUEST_POSTURE_GUIDANCE);
        mLaunchedPostureGuidance = true;
        overridePendingTransition(0 /* no enter anim */, 0 /* no exit anim */);
        return mLaunchedPostureGuidance;
    }

    protected boolean shouldFinishWhenBackgrounded() {
        return !WizardManagerHelper.isAnySetupWizard(getIntent());
    }

    protected void initViews() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    protected GlifLayout getLayout() {
        return (GlifLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected void setHeaderText(int resId, boolean force) {
        TextView layoutTitle = getLayout().getHeaderTextView();
        CharSequence previousTitle = layoutTitle.getText();
        CharSequence title = getText(resId);
        if (previousTitle != title || force) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            getLayout().setHeaderText(title);
            getLayout().getHeaderTextView().setContentDescription(title);
            setTitle(title);
        }
    }

    protected void setHeaderText(int resId) {
        setHeaderText(resId, false /* force */);
        getLayout().getHeaderTextView().setContentDescription(getText(resId));
    }

    protected void setHeaderText(CharSequence title) {
        getLayout().setHeaderText(title);
        getLayout().getHeaderTextView().setContentDescription(title);
    }

    protected void setDescriptionText(int resId) {
        CharSequence previousDescription = getLayout().getDescriptionText();
        CharSequence description = getString(resId);
        // Prevent a11y for re-reading the same string
        if (!TextUtils.equals(previousDescription, description)) {
            getLayout().setDescriptionText(resId);
        }
    }

    protected void setDescriptionText(CharSequence descriptionText) {
        getLayout().setDescriptionText(descriptionText);
    }

    protected FooterButton getNextButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getPrimaryButton();
        }
        return null;
    }

    protected void onNextButtonClick(View view) {
    }

    protected Intent getFingerprintEnrollingIntent() {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, FingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, mFromSettingsSummary);
        intent.putExtra(EXTRA_KEY_CHALLENGE, mChallenge);
        intent.putExtra(EXTRA_KEY_SENSOR_ID, mSensorId);
        BiometricUtils.copyMultiBiometricExtras(getIntent(), intent);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        return intent;
    }

    protected void launchConfirmLock(int titleResId) {
        Log.d(TAG, "launchConfirmLock");

        final ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(this);
        builder.setRequestCode(CONFIRM_REQUEST)
                .setTitle(getString(titleResId))
                .setRequestGatekeeperPasswordHandle(true)
                .setForegroundOnly(true)
                .setReturnCredentials(true);

        if (mUserId != UserHandle.USER_NULL) {
            builder.setUserId(mUserId);
        }

        final boolean launched = builder.show();
        if (!launched) {
            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        } else {
            mLaunchedConfirmLock = true;
        }
    }

    @ColorInt
    public int getBackgroundColor() {
        final ColorStateList stateList = Utils.getColorAttr(this, android.R.attr.windowBackground);
        return stateList != null ? stateList.getDefaultColor() : Color.TRANSPARENT;
    }

    protected boolean shouldShowSplitScreenDialog() {
        return isInMultiWindowMode() && !ActivityEmbeddingUtils.isActivityEmbedded(this);
    }
}
