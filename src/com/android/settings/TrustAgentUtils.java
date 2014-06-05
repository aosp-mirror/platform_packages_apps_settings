/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.service.trust.TrustAgentService;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class TrustAgentUtils {
    static final String TAG = "TrustAgentUtils";

    private static final String TRUST_AGENT_META_DATA = TrustAgentService.TRUST_AGENT_META_DATA;
    private static final String PERMISSION_PROVIDE_AGENT = android.Manifest.permission.PROVIDE_TRUST_AGENT;

    /**
     * @return true, if the service in resolveInfo has the permission to provide a trust agent.
     */
    public static boolean checkProvidePermission(ResolveInfo resolveInfo, PackageManager pm) {
        String packageName = resolveInfo.serviceInfo.packageName;
        if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Skipping agent because package " + packageName
                    + " does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
            return false;
        }
        return true;
    }

    public static class TrustAgentComponentInfo {
        ComponentName componentName;
        String title;
        String summary;
    }

    public static ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    public static TrustAgentComponentInfo getSettingsComponent(
            PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) return null;
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
        trustAgentComponentInfo.componentName = (cn == null) ? null : ComponentName.unflattenFromString(cn);
        return trustAgentComponentInfo;
    }
}
