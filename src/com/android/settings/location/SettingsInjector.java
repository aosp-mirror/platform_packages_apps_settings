/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.location;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds the preferences specified by the {@link InjectedSetting} objects to a preference group.
 *
 * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}. We do not use that
 * class directly because it is not a good match for our use case: we do not need the caching, and
 * so do not want the additional resource hit at app install/upgrade time; and we would have to
 * suppress the tie-breaking between multiple services reporting settings with the same name.
 * Code-sharing would require extracting {@link
 * android.content.pm.RegisteredServicesCache#parseServiceAttributes(android.content.res.Resources,
 * String, android.util.AttributeSet)} into an interface, which didn't seem worth it.
 *
 * TODO: register a broadcast receiver that calls updateUI() when it receives
 * {@link SettingInjectorService#UPDATE_INTENT}.
 */
class SettingsInjector {

    private static final String TAG = "SettingsInjector";

    /**
     * Intent action marking the receiver as injecting a setting
     */
    public static final String RECEIVER_INTENT = "com.android.settings.InjectedLocationSetting";

    /**
     * Name of the meta-data tag used to specify the resource file that includes the settings
     * attributes.
     */
    public static final String META_DATA_NAME = "com.android.settings.InjectedLocationSetting";

    /**
     * Name of the XML tag that includes the attributes for the setting.
     */
    public static final String ATTRIBUTES_NAME = "injected-location-setting";

    /**
     * Returns a list with one {@link InjectedSetting} object for each {@link android.app.Service}
     * that responds to {@link #RECEIVER_INTENT} and provides the expected setting metadata.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     *
     * TODO: sort alphabetically
     *
     * TODO: unit test
     */
    public static List<InjectedSetting> getSettings(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent receiverIntent = new Intent(RECEIVER_INTENT);

        List<ResolveInfo> resolveInfos =
                pm.queryIntentServices(receiverIntent, PackageManager.GET_META_DATA);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Found services: " + resolveInfos);
        }
        List<InjectedSetting> settings = new ArrayList<InjectedSetting>(resolveInfos.size());
        for (ResolveInfo receiver : resolveInfos) {
            try {
                InjectedSetting info = parseServiceInfo(receiver, pm);
                if (info == null) {
                    Log.w(TAG, "Unable to load service info " + receiver);
                } else {
                    if (Log.isLoggable(TAG, Log.INFO)) {
                        Log.i(TAG, "Loaded service info: " + info);
                    }
                    settings.add(info);
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Unable to load service info " + receiver, e);
            } catch (IOException e) {
                Log.w(TAG, "Unable to load service info " + receiver, e);
            }
        }

        return settings;
    }

    /**
     * Parses {@link InjectedSetting} from the attributes of the {@link #META_DATA_NAME} tag.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     */
    private static InjectedSetting parseServiceInfo(ResolveInfo service, PackageManager pm)
            throws XmlPullParserException, IOException {

        ServiceInfo si = service.serviceInfo;

        XmlResourceParser parser = null;
        try {
            parser = si.loadXmlMetaData(pm, META_DATA_NAME);
            if (parser == null) {
                throw new XmlPullParserException("No " + META_DATA_NAME
                        + " meta-data for " + service + ": " + si);
            }

            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }

            String nodeName = parser.getName();
            if (!ATTRIBUTES_NAME.equals(nodeName)) {
                throw new XmlPullParserException("Meta-data does not start with "
                        + ATTRIBUTES_NAME + " tag");
            }

            Resources res = pm.getResourcesForApplication(si.applicationInfo);
            return parseAttributes(si.packageName, si.name, res, attrs);
        } catch (PackageManager.NameNotFoundException e) {
            throw new XmlPullParserException(
                    "Unable to load resources for package " + si.packageName);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private static InjectedSetting parseAttributes(
            String packageName, String className, Resources res, AttributeSet attrs) {

        TypedArray sa = res.obtainAttributes(attrs, R.styleable.InjectedLocationSetting);
        try {
            // Note that to help guard against malicious string injection, we do not allow dynamic
            // specification of the label (setting title)
            final int labelId = sa.getResourceId(R.styleable.InjectedLocationSetting_label, 0);
            final String label = sa.getString(R.styleable.InjectedLocationSetting_label);
            final int iconId = sa.getResourceId(R.styleable.InjectedLocationSetting_icon, 0);
            final String settingsActivity =
                    sa.getString(R.styleable.InjectedLocationSetting_settingsActivity);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "parsed labelId: " + labelId + ", label: " + label
                        + ", iconId: " + iconId);
            }
            if (labelId == 0 || TextUtils.isEmpty(label) || TextUtils.isEmpty(settingsActivity)) {
                return null;
            }
            return new InjectedSetting(packageName, className,
                    label, iconId, settingsActivity);
        } finally {
            sa.recycle();
        }
    }

    /**
     * Add settings that other apps have injected.
     */
    public static void addInjectedSettings(PreferenceGroup group, Context context,
            PreferenceManager preferenceManager) {

        Iterable<InjectedSetting> settings = getSettings(context);
        for (InjectedSetting setting : settings) {
            Preference pref = addServiceSetting(context, group, setting, preferenceManager);

            // TODO: to prevent churn from multiple live broadcast receivers, don't trigger
            // the next update until the sooner of: the current update completes or 1-2 seconds
            // after the current update was started.
            updateSetting(context, pref, setting);
        }
    }

    /**
     * Adds an injected setting to the root with status "Loading...".
     */
    private static PreferenceScreen addServiceSetting(Context context,
            PreferenceGroup group, InjectedSetting info, PreferenceManager preferenceManager) {

        PreferenceScreen screen = preferenceManager.createPreferenceScreen(context);
        screen.setTitle(info.title);
        screen.setSummary("Loading...");
        PackageManager pm = context.getPackageManager();
        Drawable icon = pm.getDrawable(info.packageName, info.iconId, null);
        screen.setIcon(icon);

        Intent settingIntent = new Intent();
        settingIntent.setClassName(info.packageName, info.settingsActivity);
        screen.setIntent(settingIntent);

        group.addPreference(screen);
        return screen;
    }

    /**
     * Ask the receiver for the current status for the setting, and display it when it replies.
     */
    private static void updateSetting(Context context,
            final Preference pref, final InjectedSetting info) {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String status = bundle.getString(SettingInjectorService.STATUS_KEY);
                boolean enabled = bundle.getBoolean(SettingInjectorService.ENABLED_KEY, true);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, info + ": received " + msg + ", bundle: " + bundle);
                }
                pref.setSummary(status);
                pref.setEnabled(enabled);
            }
        };
        Messenger messenger = new Messenger(handler);
        Intent receiverIntent = info.getServiceIntent();
        receiverIntent.putExtra(SettingInjectorService.MESSENGER_KEY, messenger);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, info + ": sending rcv-intent: " + receiverIntent + ", handler: " + handler);
        }
        context.startService(receiverIntent);
    }
}
