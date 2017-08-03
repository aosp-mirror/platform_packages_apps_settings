/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

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
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Search loader for installed apps.
 */
public class InstalledAppResultLoader extends AsyncLoader<Set<? extends SearchResult>> {

    private static final int NAME_NO_MATCH = -1;
    private static final Intent LAUNCHER_PROBE = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER);

    private List<String> mBreadcrumb;
    private SiteMapManager mSiteMapManager;
    @VisibleForTesting
    final String mQuery;
    private final UserManager mUserManager;
    private final PackageManagerWrapper mPackageManager;
    private final List<ResolveInfo> mHomeActivities = new ArrayList<>();

    public InstalledAppResultLoader(Context context, PackageManagerWrapper pmWrapper,
            String query, SiteMapManager mapManager) {
        super(context);
        mSiteMapManager = mapManager;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = pmWrapper;
        mQuery = query;
    }

    @Override
    public Set<? extends SearchResult> loadInBackground() {
        final Set<AppSearchResult> results = new HashSet<>();
        final PackageManager pm = mPackageManager.getPackageManager();

        mHomeActivities.clear();
        mPackageManager.getHomeActivities(mHomeActivities);

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
                        .setData(Uri.fromParts("package", info.packageName, null))
                        .putExtra(SettingsActivity.EXTRA_SOURCE_METRICS_CATEGORY,
                                MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS);

                final AppSearchResult.Builder builder = new AppSearchResult.Builder();
                builder.setAppInfo(info)
                        .setStableId(Objects.hash(info.packageName, user.id))
                        .setTitle(info.loadLabel(pm))
                        .setRank(getRank(wordDiff))
                        .addBreadcrumbs(getBreadCrumb())
                        .setPayload(new ResultPayload(intent));
                results.add(builder.build());
            }
        }
        return results;
    }

    /**
     * Returns true if the candidate should be included in candidate list
     * <p/>
     * This method matches logic in {@code ApplicationState#FILTER_DOWNLOADED_AND_LAUNCHER}.
     */
    private boolean shouldIncludeAsCandidate(ApplicationInfo info, UserInfo user) {
        // Not system app
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                || (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
        // Shows up in launcher
        final Intent launchIntent = new Intent(LAUNCHER_PROBE)
                .setPackage(info.packageName);
        final List<ResolveInfo> intents = mPackageManager.queryIntentActivitiesAsUser(
                launchIntent,
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE,
                user.id);
        if (intents != null && intents.size() != 0) {
            return true;
        }
        // Is launcher app itself
        return isPackageInList(mHomeActivities, info.packageName);
    }

    @Override
    protected void onDiscardResult(Set<? extends SearchResult> result) {

    }

    private List<UserInfo> getUsersToCount() {
        return mUserManager.getProfiles(UserHandle.myUserId());
    }

    /**
     * Returns "difference" between appName and query string. appName must contain all
     * characters from query as a prefix to a word, in the same order.
     * If not, returns NAME_NO_MATCH.
     * If they do match, returns an int value representing  how different they are,
     * and larger values means they are less similar.
     * <p/>
     * Example:
     * appName: Abcde, query: Abcde, Returns 0
     * appName: Abcde, query: abc, Returns 2
     * appName: Abcde, query: ab, Returns 3
     * appName: Abcde, query: bc, Returns NAME_NO_MATCH
     * appName: Abcde, query: xyz, Returns NAME_NO_MATCH
     * appName: Abc de, query: de, Returns 4
     * TODO: Move this to a common util class.
     */
    static int getWordDifference(String appName, String query) {
        if (TextUtils.isEmpty(appName) || TextUtils.isEmpty(query)) {
            return NAME_NO_MATCH;
        }

        final char[] queryTokens = query.toLowerCase().toCharArray();
        final char[] appTokens = appName.toLowerCase().toCharArray();
        final int appLength = appTokens.length;
        if (queryTokens.length > appLength) {
            return NAME_NO_MATCH;
        }

        int i = 0;
        int j;

        while (i < appLength) {
            j = 0;
            // Currently matching a prefix
            while ((i + j < appLength) && (queryTokens[j] == appTokens[i + j])) {
                // Matched the entire query
                if (++j >= queryTokens.length) {
                    // Use the diff in length as a proxy of how close the 2 words match.
                    // Value range from 0 to infinity.
                    return appLength - queryTokens.length;
                }
            }

            i += j;

            // Remaining string is longer that the query or we have search the whole app name.
            if (queryTokens.length > appLength - i) {
                return NAME_NO_MATCH;
            }

            // This is the first index where app name and query name are different
            // Find the next space in the app name or the end of the app name.
            while ((i < appLength) && (!Character.isWhitespace(appTokens[i++]))) ;

            // Find the start of the next word
            while ((i < appLength) && !(Character.isLetter(appTokens[i])
                    || Character.isDigit(appTokens[i]))) {
                // Increment in body because we cannot guarantee which condition was true
                i++;
            }
        }
        return NAME_NO_MATCH;
    }

    private boolean isPackageInList(List<ResolveInfo> resolveInfos, String pkg) {
        for (ResolveInfo info : resolveInfos) {
            if (TextUtils.equals(info.activityInfo.packageName, pkg)) {
                return true;
            }
        }
        return false;
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

    /**
     * A temporary ranking scheme for installed apps.
     *
     * @param wordDiff difference between query length and app name length.
     * @return the ranking.
     */
    private int getRank(int wordDiff) {
        if (wordDiff < 6) {
            return 2;
        }
        return 3;
    }
}
