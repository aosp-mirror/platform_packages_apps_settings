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

package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class ChangeScreenLockPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, GearPreference.OnGearClickListener {

    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";

    protected final DevicePolicyManager mDPM;
    protected final SecuritySettings mHost;
    protected final UserManager mUm;
    protected final LockPatternUtils mLockPatternUtils;

    protected final int mUserId = UserHandle.myUserId();
    protected final int mProfileChallengeUserId;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    protected RestrictedPreference mPreference;

    public ChangeScreenLockPreferenceController(Context context, SecuritySettings host) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mHost = host;
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_unlock_set_or_change);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UNLOCK_SET_OR_CHANGE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference != null && mPreference instanceof GearPreference) {
            if (mLockPatternUtils.isSecure(mUserId)) {
                ((GearPreference) mPreference).setOnGearClickListener(this);
            } else {
                ((GearPreference) mPreference).setOnGearClickListener(null);
            }
        }

        updateSummary(preference, mUserId);
        disableIfPasswordQualityManaged(mUserId);
        if (!mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)) {
            // PO may disallow to change password for the profile, but screen lock and managed
            // profile's lock is the same. Disable main "Screen lock" menu.
            disableIfPasswordQualityManaged(mProfileChallengeUserId);
        }
    }

    @Override
    public void onGearClick(GearPreference p) {
        if (TextUtils.equals(p.getKey(), getPreferenceKey())) {
            mMetricsFeatureProvider.logClickedPreference(p,
                    p.getExtras().getInt(DashboardFragment.CATEGORY));
            new SubSettingLauncher(mContext)
                    .setDestination(ScreenLockSettings.class.getName())
                    .setSourceMetricsCategory(mHost.getMetricsCategory())
                    .launch();
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        // TODO(b/35930129): Remove once existing password can be passed into vold directly.
        // Currently we need this logic to ensure that the QUIET_MODE is off for any work
        // profile with unified challenge on FBE-enabled devices. Otherwise, vold would not be
        // able to complete the operation due to the lack of (old) encryption key.
        if (mProfileChallengeUserId != UserHandle.USER_NULL
                && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)
                && StorageManager.isFileEncryptedNativeOnly()) {
            if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileChallengeUserId)) {
                return false;
            }
        }

        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setTitleRes(R.string.lock_settings_picker_title)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .launch();
        return true;
    }

    protected void updateSummary(Preference preference, int userId) {
        if (!mLockPatternUtils.isSecure(userId)) {
            if (userId == mProfileChallengeUserId
                    || mLockPatternUtils.isLockScreenDisabled(userId)) {
                preference.setSummary(R.string.unlock_set_unlock_mode_off);
            } else {
                preference.setSummary(R.string.unlock_set_unlock_mode_none);
            }
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(userId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    preference.setSummary(R.string.unlock_set_unlock_mode_pattern);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    preference.setSummary(R.string.unlock_set_unlock_mode_pin);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    preference.setSummary(R.string.unlock_set_unlock_mode_password);
                    break;
            }
        }
        mPreference.setEnabled(true);
    }

    /**
     * Sets the preference as disabled by admin if PASSWORD_QUALITY_MANAGED is set.
     * The preference must be a RestrictedPreference.
     * <p/>
     * DO or PO installed in the user may disallow to change password.
     */
    void disableIfPasswordQualityManaged(int userId) {
        final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtilsInternal
                .checkIfPasswordQualityIsSet(mContext, userId);
        final DevicePolicyManager dpm = (DevicePolicyManager) mContext
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (admin != null && dpm.getPasswordQuality(admin.component, userId)
                == DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
            mPreference.setDisabledByAdmin(admin);
        }
    }
}
