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
package com.android.settings.accounts;

import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.app.Activity;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserAndAccountDashboardFragment extends DashboardFragment {

    private static final String TAG = "UserAndAccountDashboard";
    private static final String METADATA_IA_ACCOUNT = "com.android.settings.ia.account";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.user_and_accounts_settings;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new EmergencyInfoPreferenceController(context));
        AddUserWhenLockedPreferenceController addUserWhenLockedPrefController =
                new AddUserWhenLockedPreferenceController(context);
        controllers.add(addUserWhenLockedPrefController);
        getLifecycle().addObserver(addUserWhenLockedPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, this));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, this));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, this));
        String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(context, this, authorities);
        getLifecycle().addObserver(accountPrefController);
        controllers.add(accountPrefController);
        return controllers;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                UserInfo info = mContext.getSystemService(UserManager.class).getUserInfo(
                        UserHandle.myUserId());
                mSummaryLoader.setSummary(this,
                    mContext.getString(R.string.users_and_accounts_summary, info.name));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.user_and_accounts_settings;
                    return Arrays.asList(sir);
                }
            };
}