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
 * limitations under the License.
 */

package com.android.settings.fingerprint;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;

public class FingerprintStatusPreferenceController extends BasePreferenceController {

    private static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";

    protected final FingerprintManager mFingerprintManager;
    protected final UserManager mUm;
    protected final LockPatternUtils mLockPatternUtils;

    protected final int mUserId = UserHandle.myUserId();
    protected final int mProfileChallengeUserId;

    public FingerprintStatusPreferenceController(Context context) {
        this(context, KEY_FINGERPRINT_SETTINGS);
    }

    public FingerprintStatusPreferenceController(Context context, String key) {
        super(context, key);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (isUserSupported()) {
            return AVAILABLE;
        } else {
            return DISABLED_FOR_USER;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable()) {
            if (preference != null) {
                preference.setVisible(false);
            }
            return;
        } else {
            preference.setVisible(true);
        }
        final int userId = getUserId();
        final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints(userId);
        final int fingerprintCount = items != null ? items.size() : 0;
        final String clazz;
        if (fingerprintCount > 0) {
            preference.setSummary(mContext.getResources().getQuantityString(
                    R.plurals.security_settings_fingerprint_preference_summary,
                    fingerprintCount, fingerprintCount));
            clazz = FingerprintSettings.class.getName();
        } else {
            preference.setSummary(
                    R.string.security_settings_fingerprint_preference_summary_none);
            clazz = FingerprintEnrollIntroduction.class.getName();
        }
        preference.setOnPreferenceClickListener(target -> {
            final Context context = target.getContext();
            final UserManager userManager = UserManager.get(context);
            if (Utils.startQuietModeDialogIfNecessary(context, userManager,
                    userId)) {
                return false;
            }
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", clazz);
            intent.putExtra(Intent.EXTRA_USER_ID, userId);
            context.startActivity(intent);
            return true;
        });
    }

    protected int getUserId() {
        return mUserId;
    }

    protected boolean isUserSupported() {
        return true;
    }
}
