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
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.settings.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 */
class SettingsInjector {
    static final String TAG = "SettingsInjector";

    private static final long INJECTED_STATUS_UPDATE_TIMEOUT_MILLIS = 1000;

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
     * {@link Message#what} value for starting to load status values
     * in case we aren't already in the process of loading them.
     */
    private static final int WHAT_RELOAD = 1;

    /**
     * {@link Message#what} value sent after receiving a status message.
     */
    private static final int WHAT_RECEIVED_STATUS = 2;

    /**
     * {@link Message#what} value sent after the timeout waiting for a status message.
     */
    private static final int WHAT_TIMEOUT = 3;

    private final Context mContext;

    /**
     * The settings that were injected
     */
    private final Set<Setting> mSettings;

    private final Handler mHandler;

    public SettingsInjector(Context context) {
        mContext = context;
        mSettings = new HashSet<Setting>();
        mHandler = new StatusLoadingHandler();
    }

    /**
     * Returns a list with one {@link InjectedSetting} object for each {@link android.app.Service}
     * that responds to {@link #RECEIVER_INTENT} and provides the expected setting metadata.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     *
     * TODO: unit test
     */
    private List<InjectedSetting> getSettings() {
        PackageManager pm = mContext.getPackageManager();
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

    /**
     * Returns an immutable representation of the static attributes for the setting, or null.
     */
    private static InjectedSetting parseAttributes(
            String packageName, String className, Resources res, AttributeSet attrs) {

        TypedArray sa = res.obtainAttributes(attrs, android.R.styleable.InjectedLocationSetting);
        try {
            // Note that to help guard against malicious string injection, we do not allow dynamic
            // specification of the label (setting title)
            final String label = sa.getString(android.R.styleable.InjectedLocationSetting_label);
            final int iconId = sa.getResourceId(
                    android.R.styleable.InjectedLocationSetting_icon, 0);
            final String settingsActivity =
                    sa.getString(android.R.styleable.InjectedLocationSetting_settingsActivity);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "parsed label: " + label + ", iconId: " + iconId
                        + ", settingsActivity: " + settingsActivity);
            }
            return InjectedSetting.newInstance(packageName, className,
                    label, iconId, settingsActivity);
        } finally {
            sa.recycle();
        }
    }

    /**
     * Gets a list of preferences that other apps have injected.
     *
     * TODO: extract InjectedLocationSettingGetter that returns an iterable over
     * InjectedSetting objects, so that this class can focus on UI
     */
    public List<Preference> getInjectedSettings() {
        Iterable<InjectedSetting> settings = getSettings();
        ArrayList<Preference> prefs = new ArrayList<Preference>();
        for (InjectedSetting setting : settings) {
            Preference pref = addServiceSetting(prefs, setting);
            mSettings.add(new Setting(setting, pref));
        }

        reloadStatusMessages();

        return prefs;
    }

    /**
     * Reloads the status messages for all the preference items.
     */
    public void reloadStatusMessages() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "reloadingStatusMessages: " + mSettings);
        }
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_RELOAD));
    }

    /**
     * Adds an injected setting to the root with status "Loading...".
     */
    private Preference addServiceSetting(List<Preference> prefs, InjectedSetting info) {
        Preference pref = new Preference(mContext);
        pref.setTitle(info.title);
        pref.setSummary(R.string.location_loading_injected_setting);
        PackageManager pm = mContext.getPackageManager();
        Drawable icon = pm.getDrawable(info.packageName, info.iconId, null);
        pref.setIcon(icon);

        Intent settingIntent = new Intent();
        settingIntent.setClassName(info.packageName, info.settingsActivity);
        pref.setIntent(settingIntent);

        prefs.add(pref);
        return pref;
    }

    /**
     * Loads the setting status values one at a time. Each load starts a subclass of {@link
     * SettingInjectorService}, so to reduce memory pressure we don't want to load too many at
     * once.
     */
    private final class StatusLoadingHandler extends Handler {

        /**
         * Settings whose status values need to be loaded. A set is used to prevent redundant loads
         * even if {@link #reloadStatusMessages()} is called many times in rapid succession (for
         * example, if we receive a lot of {@link
         * android.location.SettingInjectorService#ACTION_INJECTED_SETTING_CHANGED} broadcasts).
         * <p/>
         * We use a linked hash set to ensure that when {@link #reloadStatusMessages()} is called,
         * any settings that haven't been loaded yet will finish loading before any already-loaded
         * messages are loaded again.
         */
        private LinkedHashSet<Setting> mSettingsToLoad = new LinkedHashSet<Setting>();

        /**
         * Whether we're in the middle of loading settings.
         */
        private boolean mLoading;

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage start: " + msg + ", mSettingsToLoad: " + mSettingsToLoad);
            }

            switch (msg.what) {
                case WHAT_RELOAD:
                    mSettingsToLoad.addAll(mSettings);
                    if (mLoading) {
                        // Already waiting for a service to return its status, don't ask a new one
                        return;
                    }
                    mLoading = true;
                    break;
                case WHAT_TIMEOUT:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        final Setting setting = (Setting) msg.obj;
                        setting.timedOut = true;
                        Log.w(TAG, "Timed out trying to get status for: " + setting);
                    }
                    break;
                case WHAT_RECEIVED_STATUS:
                    final Setting setting = (Setting) msg.obj;
                    if (setting.timedOut) {
                        // We've already restarted retrieving the next setting, don't start another
                        return;
                    }

                    // Received the setting without timeout, clear any previous timed out status
                    setting.timedOut = false;
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected what: " + msg);
            }

            // Remove the next setting to load from the queue, if any
            Iterator<Setting> iter = mSettingsToLoad.iterator();
            if (!iter.hasNext()) {
                mLoading = false;
                return;
            }
            Setting setting = iter.next();
            iter.remove();

            // Request the status value
            Intent intent = setting.createUpdatingIntent();
            mContext.startService(intent);

            // Ensure that if receiving the status value takes too long, we start loading the
            // next value anyway
            Message timeoutMsg = obtainMessage(WHAT_TIMEOUT, setting);
            removeMessages(WHAT_TIMEOUT);
            sendMessageDelayed(timeoutMsg, INJECTED_STATUS_UPDATE_TIMEOUT_MILLIS);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage end: " + msg + ", mSettingsToLoad: " + mSettingsToLoad);
            }
        }
    }

    /**
     * Represents an injected setting and the corresponding preference.
     */
    private final class Setting {

        public final InjectedSetting setting;
        public final Preference preference;
        public boolean timedOut = false;

        private Setting(InjectedSetting setting, Preference preference) {
            this.setting = setting;
            this.preference = preference;
        }

        @Override
        public String toString() {
            return "Setting{" +
                    "setting=" + setting +
                    ", preference=" + preference +
                    ", timedOut=" + timedOut +
                    '}';
        }

        /**
         * Creates an Intent to ask the receiver for the current status for the setting, and display
         * it when it replies.
         */
        public Intent createUpdatingIntent() {
            final Intent receiverIntent = setting.getServiceIntent();
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    String summary = bundle.getString(SettingInjectorService.SUMMARY_KEY);
                    boolean enabled = bundle.getBoolean(SettingInjectorService.ENABLED_KEY, true);
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, setting + ": received " + msg + ", bundle: " + bundle);
                    }
                    preference.setSummary(summary);
                    preference.setEnabled(enabled);
                    mHandler.sendMessage(
                            mHandler.obtainMessage(WHAT_RECEIVED_STATUS, Setting.this));
                }
            };
            Messenger messenger = new Messenger(handler);
            receiverIntent.putExtra(SettingInjectorService.MESSENGER_KEY, messenger);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, setting + ": sending rcv-intent: " + receiverIntent
                        + ", handler: " + handler);
            }
            return receiverIntent;
        }
    }
}
