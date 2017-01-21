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

package com.android.settings.search2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Search loader for installed apps.
 */
public class InstalledAppResultLoader extends AsyncLoader<List<SearchResult>> {

    private static final int NAME_NO_MATCH = -1;
    private static final int NAME_EXACT_MATCH = 0;
    private static final Intent LAUNCHER_PROBE = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER);

    private List<String> mBreadcrumb;
    private SiteMapManager mSiteMapManager;
    private final String mQuery;
    private final UserManager mUserManager;
    private final PackageManagerWrapper mPackageManager;


    public InstalledAppResultLoader(Context context, PackageManagerWrapper pmWrapper,
            String query) {
        super(context);
        mSiteMapManager = FeatureFactory.getFactory(context)
                .getSearchFeatureProvider().getSiteMapManager();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = pmWrapper;
        mQuery = query;
    }

    @Override
    public List<SearchResult> loadInBackground() {
        final List<SearchResult> results = new ArrayList<>();
        final PackageManager pm = mPackageManager.getPackageManager();

        for (UserInfo user : getUsersToCount()) {
            final List<ApplicationInfo> apps =
                    mPackageManager.getInstalledApplicationsAsUser(
                            PackageManager.MATCH_DISABLED_COMPONENTS
                                    | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                                    | (user.isAdmin() ? PackageManager.MATCH_ANY_USER : 0),
                            user.id);
            for (ApplicationInfo info : apps) {
                if (!shouldIncludeAsCandidate(info, user)) {
                    continue;
                }
                final CharSequence label = info.loadLabel(pm);
                final int wordDiff = getWordDifference(label.toString(), mQuery);
                if (wordDiff == NAME_NO_MATCH) {
                    continue;
                }
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", info.packageName, null));

                final SearchResult.Builder builder = new SearchResult.Builder();
                builder.addIcon(info.loadIcon(pm))
                        .addTitle(info.loadLabel(pm))
                        .addRank(wordDiff)
                        .addBreadcrumbs(getBreadCrumb())
                        .addPayload(new IntentPayload(intent));
                results.add(builder.build());
            }
        }
        Collections.sort(results);
        return results;
    }

    private boolean shouldIncludeAsCandidate(ApplicationInfo info, UserInfo user) {
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                || (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
        final Intent launchIntent = new Intent(LAUNCHER_PROBE)
                .setPackage(info.packageName);
        final List<ResolveInfo> intents = mPackageManager.queryIntentActivitiesAsUser(
                launchIntent,
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE,
                user.id);
        return intents != null && intents.size() != 0;
    }

    @Override
    protected void onDiscardResult(List<SearchResult> result) {

    }

    private List<UserInfo> getUsersToCount() {
        return mUserManager.getProfiles(UserHandle.myUserId());
    }

    /**
     * Returns "difference" between appName and query string. appName must contain all
     * characters from query, in the same order. If not, returns NAME_NO_MATCH. If they do match,
     * returns an int value representing how different they are, NAME_EXACT_MATCH means they match
     * perfectly, and larger values means they are less similar.
     * <p/>
     * Example:
     * appName: Abcde, query: Abcde, Returns {@link #NAME_EXACT_MATCH}
     * appName: Abcde, query: ade, Returns 2
     * appName: Abcde, query: ae, Returns 3
     * appName: Abcde, query: ea, Returns NAME_NO_MATCH
     * appName: Abcde, query: xyz, Returns NAME_NO_MATCH
     */
    private int getWordDifference(String appName, String query) {
        if (TextUtils.isEmpty(appName) || TextUtils.isEmpty(query)) {
            return NAME_NO_MATCH;
        }
        final char[] queryTokens = query.toString().toLowerCase().toCharArray();
        final char[] valueText = appName.toLowerCase().toCharArray();
        if (queryTokens.length > valueText.length) {
            return NAME_NO_MATCH;
        }
        int i = 0;
        int j = 0;
        while (i < valueText.length && j < queryTokens.length) {
            if (valueText[i++] == queryTokens[j]) {
                j++;
            }
        }
        if (j != queryTokens.length) {
            return NAME_NO_MATCH;
        }
        // Use the diff in length as a proxy of how close the 2 words match. Value range from 0
        // to infinity.
        return valueText.length - queryTokens.length;
    }

    private List<String> getBreadCrumb() {
        if (mBreadcrumb == null || mBreadcrumb.isEmpty()) {
            final Context context = getContext();
            mBreadcrumb = mSiteMapManager.buildBreadCrumb(
                    context, ManageApplications.class.getName(),
                    context.getString(R.string.applications_settings));
        }
        return mBreadcrumb;
    }
}
