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

package com.android.settings.security.trustagent;

import static android.service.trust.TrustAgentService.TRUST_AGENT_META_DATA;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.UserHandle;
import android.service.trust.TrustAgentService;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import androidx.annotation.VisibleForTesting;

import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/** A manager for trust agent state. */
public class TrustAgentManager {

    // Only allow one trust agent on the platform.
    private static final boolean ONLY_ONE_TRUST_AGENT = false;

    public static class TrustAgentComponentInfo {
        public ComponentName componentName;
        public String title;
        public String summary;
        public RestrictedLockUtils.EnforcedAdmin admin = null;
    }

    private static final String TAG = "TrustAgentManager";
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    @VisibleForTesting
    static final String PERMISSION_PROVIDE_AGENT =
            android.Manifest.permission.PROVIDE_TRUST_AGENT;

    /**
     * Determines if the service associated with a resolved trust agent intent is allowed to provide
     * trust on this device.
     *
     * @param resolveInfo The entry corresponding to the matched trust agent intent.
     * @param pm          The package manager to be used to check for permissions.
     * @return {@code true} if the associated service is allowed to provide a trust agent, and
     * {@code false} if otherwise.
     */
    public boolean shouldProvideTrust(ResolveInfo resolveInfo, PackageManager pm) {
        final String packageName = resolveInfo.serviceInfo.packageName;
        if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Skipping agent because package " + packageName
                    + " does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
            return false;
        }
        return true;
    }

    /**
     * Return the display label for active trust agent.
     */
    public CharSequence getActiveTrustAgentLabel(Context context, LockPatternUtils utils) {
        final List<TrustAgentComponentInfo> agents = getActiveTrustAgents(context, utils);
        return agents.isEmpty() ? null : agents.get(0).title;
    }

    /**
     * Returns a list of trust agents.
     *
     * If {@link #ONLY_ONE_TRUST_AGENT} is set, the list will contain up to 1 agent instead of all
     * available agents on device.
     */
    public List<TrustAgentComponentInfo> getActiveTrustAgents(Context context,
            LockPatternUtils utils) {
        final int myUserId = UserHandle.myUserId();
        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        final PackageManager pm = context.getPackageManager();
        final List<TrustAgentComponentInfo> result = new ArrayList<>();

        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        final List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(myUserId);
        final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtilsInternal
                .checkIfKeyguardFeaturesDisabled(
                        context, DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS, myUserId);

        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.serviceInfo == null || !shouldProvideTrust(resolveInfo, pm)) {
                    continue;
                }
                final TrustAgentComponentInfo trustAgentComponentInfo =
                        getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) {
                    continue;
                }
                if (admin != null && dpm.getTrustAgentConfiguration(
                        null, getComponentName(resolveInfo)) == null) {
                    trustAgentComponentInfo.admin = admin;
                }
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) {
                    break;
                }
            }
        }
        return result;
    }

    public ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private TrustAgentComponentInfo getSettingsComponent(PackageManager pm,
            ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        TrustAgentComponentInfo trustAgentComponentInfo = new TrustAgentComponentInfo();
        XmlResourceParser parser = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, TRUST_AGENT_META_DATA);
            if (parser == null) {
                Slog.w(TAG, "Can't find " + TRUST_AGENT_META_DATA + " meta-data");
                return null;
            }
            Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"trust-agent".equals(nodeName)) {
                Slog.w(TAG, "Meta-data does not start with trust-agent tag");
                return null;
            }
            TypedArray sa =
                    res.obtainAttributes(attrs, com.android.internal.R.styleable.TrustAgent);
            trustAgentComponentInfo.summary =
                    sa.getString(com.android.internal.R.styleable.TrustAgent_summary);
            trustAgentComponentInfo.title =
                    sa.getString(com.android.internal.R.styleable.TrustAgent_title);
            cn = sa.getString(com.android.internal.R.styleable.TrustAgent_settingsActivity);
            sa.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e) {
            caughtException = e;
        } catch (XmlPullParserException e) {
            caughtException = e;
        } finally {
            if (parser != null) parser.close();
        }
        if (caughtException != null) {
            Slog.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        if (cn != null && cn.indexOf('/') < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        trustAgentComponentInfo.componentName =
                (cn == null) ? null : ComponentName.unflattenFromString(cn);
        return trustAgentComponentInfo;
    }
}
