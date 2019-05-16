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

package com.android.settings.biometrics.face;

import static android.provider.Settings.Secure.FACE_UNLOCK_EDUCATION_INFO_DISPLAYED;
import static android.security.KeyStore.getApplicationContext;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.view.IllustrationVideoView;

public class FaceEnrollEducation extends BiometricEnrollBase {

    private static final String TAG = "FaceEducation";
    private static final int ON = 1;
    private static final int OFF = 0;
    // 8 seconds.
    private static final long FACE_ENROLL_EDUCATION_DELAY = 8000;

    private FaceManager mFaceManager;
    private FaceEnrollAccessibilityToggle mSwitchDiversity;

    private IllustrationVideoView mIllustrationNormal;
    private View mIllustrationAccessibility;
    private Handler mHandler;
    private Intent mResultIntent;
    private TextView mDescriptionText;

    private CompoundButton.OnCheckedChangeListener mSwitchDiversityListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int titleRes = isChecked ?
                            R.string.security_settings_face_enroll_education_title_accessibility
                            : R.string.security_settings_face_enroll_education_title;
                    getLayout().setHeaderText(titleRes);
                    setTitle(titleRes);

                    if (isChecked) {
                        mIllustrationNormal.stop();
                        mIllustrationNormal.setVisibility(View.INVISIBLE);
                        mIllustrationAccessibility.setVisibility(View.VISIBLE);
                        mDescriptionText.setVisibility(View.INVISIBLE);
                    } else {
                        mIllustrationNormal.setVisibility(View.VISIBLE);
                        mIllustrationNormal.start();
                        mIllustrationAccessibility.setVisibility(View.INVISIBLE);
                        mDescriptionText.setVisibility(View.VISIBLE);
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_enroll_education);
        getLayout().setHeaderText(R.string.security_settings_face_enroll_education_title);
        setTitle(R.string.security_settings_face_enroll_education_title);
        mHandler = new Handler();

        mFaceManager = Utils.getFaceManagerOrNull(this);

        mIllustrationNormal = findViewById(R.id.illustration_normal);
        mIllustrationAccessibility = findViewById(R.id.illustration_accessibility);
        mDescriptionText = findViewById(R.id.sud_layout_description);

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_face_enroll_enrolling_skip)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        final FooterButton footerButton = new FooterButton.Builder(this)
                .setText(R.string.security_settings_face_enroll_education_start)
                .setListener(this::onNextButtonClick)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(R.style.SudGlifButton_Primary)
                .build();

        boolean accessibilityEnabled = false;
        final AccessibilityManager accessibilityManager = getApplicationContext().getSystemService(
                AccessibilityManager.class);
        if (accessibilityManager != null) {
            accessibilityEnabled = accessibilityManager.isEnabled();
        }
        mFooterBarMixin.setPrimaryButton(footerButton);
        final Context context = getApplicationContext();
        final boolean didDisplayEdu = Settings.Secure.getIntForUser(context.getContentResolver(),
                FACE_UNLOCK_EDUCATION_INFO_DISPLAYED, OFF, mUserId) == ON;
        if (!didDisplayEdu && !accessibilityEnabled) {
            Settings.Secure.putIntForUser(context.getContentResolver(),
                    FACE_UNLOCK_EDUCATION_INFO_DISPLAYED, ON, mUserId);
            footerButton.setEnabled(false);
            mHandler.postDelayed(() -> {
                footerButton.setEnabled(true);
            }, FACE_ENROLL_EDUCATION_DELAY);
        }

        final Button accessibilityButton = findViewById(R.id.accessibility_button);
        accessibilityButton.setOnClickListener(view -> {
            footerButton.setEnabled(true);
            mSwitchDiversity.setChecked(true);
            accessibilityButton.setVisibility(View.GONE);
            mSwitchDiversity.setVisibility(View.VISIBLE);
        });

        mSwitchDiversity = findViewById(R.id.toggle_diversity);
        mSwitchDiversity.setListener(mSwitchDiversityListener);

        if (accessibilityEnabled) {
            accessibilityButton.callOnClick();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchDiversityListener.onCheckedChanged(mSwitchDiversity.getSwitch(),
                mSwitchDiversity.isChecked());
    }

    @Override
    protected void onNextButtonClick(View view) {
        final Intent intent = new Intent();
        if (mToken != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        final String flattenedString = getString(R.string.config_face_enroll);
        if (!TextUtils.isEmpty(flattenedString)) {
            ComponentName componentName = ComponentName.unflattenFromString(flattenedString);
            intent.setComponent(componentName);
        } else {
            intent.setClass(this, FaceEnrollEnrolling.class);
        }
        intent.putExtra(EXTRA_KEY_REQUIRE_DIVERSITY, !mSwitchDiversity.isChecked());
        if (mResultIntent != null) {
            intent.putExtras(mResultIntent);
        }
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        startActivityForResult(intent, BIOMETRIC_FIND_SENSOR_REQUEST);
    }

    protected void onSkipButtonClick(View view) {
        setResult(RESULT_SKIP);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mResultIntent = data;
        if (requestCode == BIOMETRIC_FIND_SENSOR_REQUEST) {
            // If the user finished or skipped enrollment, finish this activity
            if (resultCode == RESULT_SKIP || resultCode == RESULT_FINISHED) {
                setResult(resultCode);
                finish();
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }
}
