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

import static com.android.settings.security.EncryptionStatusPreferenceController
        .PREF_KEY_ENCRYPTION_SECURITY_PAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.enterprise.EnterprisePrivacyPreferenceController;
import com.android.settings.enterprise.ManageDeviceAdminPreferenceController;
import com.android.settings.fingerprint.FingerprintProfileStatusPreferenceController;
import com.android.settings.fingerprint.FingerprintStatusPreferenceController;
import com.android.settings.location.LocationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.screenlock.LockScreenPreferenceController;
import com.android.settings.security.trustagent.ManageTrustAgentsPreferenceController;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class SecuritySettings extends DashboardFragment {

    private static final String TAG = "SecuritySettings";
    private static final String SECURITY_CATEGORY = "security_category";
    private static final String WORK_PROFILE_SECURITY_CATEGORY = "security_category_profile";

    public static final int CHANGE_TRUST_AGENT_SETTINGS = 126;
    public static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    public static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    public static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SECURITY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_dashboard_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_security;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this /* host*/);
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (use(TrustAgentListPreferenceController.class)
                .handleActivityResult(requestCode, resultCode)) {
            return;
        }
        if (use(LockUnificationPreferenceController.class)
                .handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void launchConfirmDeviceLockForUnification() {
        use(LockUnificationPreferenceController.class)
                .launchConfirmDeviceLockForUnification();
    }

    void unifyUncompliantLocks() {
        use(LockUnificationPreferenceController.class).unifyUncompliantLocks();
    }

    void updateUnificationPreference() {
        use(LockUnificationPreferenceController.class).updateState(null);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, SecuritySettings host) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new LocationPreferenceController(context, lifecycle));
        controllers.add(new ManageDeviceAdminPreferenceController(context));
        controllers.add(new EnterprisePrivacyPreferenceController(context));
        controllers.add(new ManageTrustAgentsPreferenceController(context));
        controllers.add(new ScreenPinningPreferenceController(context));
        controllers.add(new SimLockPreferenceController(context));
        controllers.add(new ShowPasswordPreferenceController(context));
        controllers.add(new EncryptionStatusPreferenceController(context,
                PREF_KEY_ENCRYPTION_SECURITY_PAGE));
        controllers.add(new TrustAgentListPreferenceController(context, host, lifecycle));

        final List<AbstractPreferenceController> securityPreferenceControllers = new ArrayList<>();
        securityPreferenceControllers.add(new FingerprintStatusPreferenceController(context));
        securityPreferenceControllers.add(new LockScreenPreferenceController(context, lifecycle));
        securityPreferenceControllers.add(new ChangeScreenLockPreferenceController(context, host));
        controllers.add(new PreferenceCategoryController(context, SECURITY_CATEGORY)
                .setChildren(securityPreferenceControllers));
        controllers.addAll(securityPreferenceControllers);

        final List<AbstractPreferenceController> profileSecurityControllers = new ArrayList<>();
        profileSecurityControllers.add(new ChangeProfileScreenLockPreferenceController(
                context, host));
        profileSecurityControllers.add(new LockUnificationPreferenceController(context, host));
        profileSecurityControllers.add(new VisiblePatternProfilePreferenceController(
                context, lifecycle));
        profileSecurityControllers.add(new FingerprintProfileStatusPreferenceController(context));
        controllers.add(new PreferenceCategoryController(context, WORK_PROFILE_SECURITY_CATEGORY)
                .setChildren(profileSecurityControllers));
        controllers.addAll(profileSecurityControllers);

        return controllers;
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final List<SearchIndexableResource> index = new ArrayList<>();
                    // Append the rest of the settings
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.security_dashboard_settings;
                    index.add(sir);
                    return index;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* host*/);
                }
            };

    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                final FingerprintManager fpm =
                        Utils.getFingerprintManagerOrNull(mContext);
                if (fpm != null && fpm.isHardwareDetected()) {
                    mSummaryLoader.setSummary(this,
                            mContext.getString(R.string.security_dashboard_summary));
                } else {
                    mSummaryLoader.setSummary(this, mContext.getString(
                            R.string.security_dashboard_summary_no_fingerprint));
                }
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY =
            new SummaryLoader.SummaryProviderFactory() {
                @Override
                public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                        SummaryLoader summaryLoader) {
                    return new SummaryProvider(activity, summaryLoader);
                }
            };
}
