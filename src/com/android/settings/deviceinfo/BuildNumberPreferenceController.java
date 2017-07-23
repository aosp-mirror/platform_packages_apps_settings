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

package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.development.DevelopmentSettings;
import com.android.settings.development.DevelopmentSettingsEnabler;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class BuildNumberPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnResume {

    static final int TAPS_TO_BE_A_DEVELOPER = 7;
    static final int REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF = 100;

    private static final String KEY_BUILD_NUMBER = "build_number";

    private final Activity mActivity;
    private final Fragment mFragment;
    private final UserManager mUm;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Toast mDevHitToast;
    private RestrictedLockUtils.EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;
    private int mDevHitCountdown;
    private boolean mProcessingLastDevHit;

    public BuildNumberPreferenceController(Context context, Activity activity, Fragment fragment,
            Lifecycle lifecycle) {
        super(context);
        mActivity = activity;
        mFragment = fragment;
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(KEY_BUILD_NUMBER);
        if (preference != null) {
            try {
                preference.setSummary(BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY));
                preference.setEnabled(true);
            } catch (Exception e) {
                preference.setSummary(R.string.device_info_default);
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUILD_NUMBER;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onResume() {
        mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDevHitCountdown = mContext.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_BUILD_NUMBER)) {
            return false;
        }
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        // Don't enable developer options for secondary users.
        if (!mUm.isAdminUser()) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF);
            return false;
        }

        // Don't enable developer options until device has been provisioned
        if (!Utils.isDeviceProvisioned(mContext)) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF);
            return false;
        }

        if (mUm.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
            if (mDebuggingFeaturesDisallowedAdmin != null &&
                    !mDebuggingFeaturesDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                        mDebuggingFeaturesDisallowedAdmin);
            }
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF);
            return false;
        }

        if (mDevHitCountdown > 0) {
            mDevHitCountdown--;
            if (mDevHitCountdown == 0 && !mProcessingLastDevHit) {
                // Add 1 count back, then start password confirmation flow.
                mDevHitCountdown++;
                final ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(mActivity, mFragment);
                mProcessingLastDevHit = helper.launchConfirmationActivity(
                        REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF,
                        mContext.getString(R.string.unlock_set_unlock_launch_picker_title));
                if (!mProcessingLastDevHit) {
                    enableDevelopmentSettings();
                }
                mMetricsFeatureProvider.action(
                        mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF,
                        Pair.create(MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                                mProcessingLastDevHit ? 0 : 1));
            } else if (mDevHitCountdown > 0
                    && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER - 2)) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(mContext,
                        mContext.getResources().getQuantityString(
                                R.plurals.show_dev_countdown, mDevHitCountdown,
                                mDevHitCountdown),
                        Toast.LENGTH_SHORT);
                mDevHitToast.show();
            }
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF,
                    Pair.create(MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                            0));
        } else if (mDevHitCountdown < 0) {
            if (mDevHitToast != null) {
                mDevHitToast.cancel();
            }
            mDevHitToast = Toast.makeText(mContext, R.string.show_dev_already,
                    Toast.LENGTH_LONG);
            mDevHitToast.show();
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF,
                    Pair.create(MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                            1));
        }
        return true;
    }

    /**
     * Handles password confirmation result.
     *
     * @return if activity result is handled.
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            enableDevelopmentSettings();
        }
        mProcessingLastDevHit = false;
        return true;
    }

    /**
     * Enables development settings. Only call this after confirming password.
     */
    private void enableDevelopmentSettings() {
        mDevHitCountdown = 0;
        mProcessingLastDevHit = false;
        DevelopmentSettingsEnabler.enableDevelopmentSettings(mContext,
                mContext.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                        Context.MODE_PRIVATE));
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mContext, R.string.show_dev_on,
                Toast.LENGTH_LONG);
        mDevHitToast.show();
        // This is good time to index the Developer Options
        FeatureFactory.getFactory(mContext).getSearchFeatureProvider().getIndexingManager(mContext)
                .updateFromClassNameResource(DevelopmentSettings.class.getName(),
                        true /* includeInSearchResults */);
    }
}
