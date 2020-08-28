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
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.slices.Sliceable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import com.google.android.setupcompat.util.WizardManagerHelper;

public class BuildNumberPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart {

    static final int TAPS_TO_BE_A_DEVELOPER = 7;
    static final int REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF = 100;

    private Activity mActivity;
    private InstrumentedPreferenceFragment mFragment;
    private final UserManager mUm;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Toast mDevHitToast;
    private RestrictedLockUtils.EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;
    private int mDevHitCountdown;
    private boolean mProcessingLastDevHit;

    public BuildNumberPreferenceController(Context context, String key) {
        super(context, key);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    public void setHost(InstrumentedPreferenceFragment fragment) {
        mFragment = fragment;
        mActivity = fragment.getActivity();
    }

    @Override
    public CharSequence getSummary() {
        return BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY);
    }

    @Override
    public void onStart() {
        mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDevHitCountdown = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public void copy() {
        Sliceable.setCopyContent(mContext, getSummary(), mContext.getText(R.string.build_number));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        // Don't enable developer options for secondary non-demo users.
        if (!(mUm.isAdminUser() || mUm.isDemoUser())) {
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_SETTINGS_BUILD_NUMBER_PREF);
            return false;
        }

        // Don't enable developer options until device has been provisioned
        if (!WizardManagerHelper.isDeviceProvisioned(mContext)) {
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_SETTINGS_BUILD_NUMBER_PREF);
            return false;
        }

        if (mUm.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
            if (mUm.isDemoUser()) {
                // Route to demo device owner to lift the debugging restriction.
                final ComponentName componentName = Utils.getDeviceOwnerComponent(mContext);
                if (componentName != null) {
                    final Intent requestDebugFeatures = new Intent()
                            .setPackage(componentName.getPackageName())
                            .setAction("com.android.settings.action.REQUEST_DEBUG_FEATURES");
                    final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(
                            requestDebugFeatures, 0);
                    if (resolveInfo != null) {
                        mContext.startActivity(requestDebugFeatures);
                        return false;
                    }
                }
            }
            if (mDebuggingFeaturesDisallowedAdmin != null &&
                    !mDebuggingFeaturesDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                        mDebuggingFeaturesDisallowedAdmin);
            }
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_SETTINGS_BUILD_NUMBER_PREF);
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
                        mMetricsFeatureProvider.getAttribution(mActivity),
                        MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                        mFragment.getMetricsCategory(),
                        null,
                        mProcessingLastDevHit ? 0 : 1);
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
                    mMetricsFeatureProvider.getAttribution(mActivity),
                    MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                    mFragment.getMetricsCategory(),
                    null,
                    0);
        } else if (mDevHitCountdown < 0) {
            if (mDevHitToast != null) {
                mDevHitToast.cancel();
            }
            mDevHitToast = Toast.makeText(mContext, R.string.show_dev_already,
                    Toast.LENGTH_LONG);
            mDevHitToast.show();
            mMetricsFeatureProvider.action(
                    mMetricsFeatureProvider.getAttribution(mActivity),
                    MetricsEvent.FIELD_SETTINGS_BUILD_NUMBER_DEVELOPER_MODE_ENABLED,
                    mFragment.getMetricsCategory(),
                    null,
                    1);
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
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(mContext, true);
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mContext, R.string.show_dev_on,
                Toast.LENGTH_LONG);
        mDevHitToast.show();
    }
}
