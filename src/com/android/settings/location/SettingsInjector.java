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
import android.content.pm.ApplicationInfo;
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
import android.os.SystemClock;
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

    /**
     * If reading the status of a setting takes longer than this, we go ahead and start reading
     * the next setting.
     */
    private static final long INJECTED_STATUS_UPDATE_TIMEOUT_MILLIS = 1000;

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
     * that responds to {@link SettingInjectorService#ACTION_SERVICE_INTENT} and provides the
     * expected setting metadata.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     *
     * TODO: unit test
     */
    private List<InjectedSetting> getSettings() {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(SettingInjectorService.ACTION_SERVICE_INTENT);

        List<ResolveInfo> resolveInfos =
                pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Found services: " + resolveInfos);
        }
        List<InjectedSetting> settings = new ArrayList<InjectedSetting>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            try {
                InjectedSetting setting = parseServiceInfo(resolveInfo, pm);
                if (setting == null) {
                    Log.w(TAG, "Unable to load service info " + resolveInfo);
                } else {
                    settings.add(setting);
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Unable to load service info " + resolveInfo, e);
            } catch (IOException e) {
                Log.w(TAG, "Unable to load service info " + resolveInfo, e);
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Loaded settings: " + settings);
        }

        return settings;
    }

    /**
     * Returns the settings parsed from the attributes of the
     * {@link SettingInjectorService#META_DATA_NAME} tag, or null.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     */
    private static InjectedSetting parseServiceInfo(ResolveInfo service, PackageManager pm)
            throws XmlPullParserException, IOException {

        ServiceInfo si = service.serviceInfo;
        ApplicationInfo ai = si.applicationInfo;

        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Ignoring attempt to inject setting from app not in system image: "
                        + service);
                return null;
            }
        }

        XmlResourceParser parser = null;
        try {
            parser = si.loadXmlMetaData(pm, SettingInjectorService.META_DATA_NAME);
            if (parser == null) {
                throw new XmlPullParserException("No " + SettingInjectorService.META_DATA_NAME
                        + " meta-data for " + service + ": " + si);
            }

            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }

            String nodeName = parser.getName();
            if (!SettingInjectorService.ATTRIBUTES_NAME.equals(nodeName)) {
                throw new XmlPullParserException("Meta-data does not start with "
                        + SettingInjectorService.ATTRIBUTES_NAME + " tag");
            }

            Resources res = pm.getResourcesForApplication(ai);
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

        TypedArray sa = res.obtainAttributes(attrs, android.R.styleable.SettingInjectorService);
        try {
            // Note that to help guard against malicious string injection, we do not allow dynamic
            // specification of the label (setting title)
            final String title = sa.getString(android.R.styleable.SettingInjectorService_title);
            final int iconId =
                    sa.getResourceId(android.R.styleable.SettingInjectorService_icon, 0);
            final String settingsActivity =
                    sa.getString(android.R.styleable.SettingInjectorService_settingsActivity);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "parsed title: " + title + ", iconId: " + iconId
                        + ", settingsActivity: " + settingsActivity);
            }
            return InjectedSetting.newInstance(packageName, className,
                    title, iconId, settingsActivity);
        } finally {
            sa.recycle();
        }
    }

    /**
     * Gets a list of preferences that other apps have injected.
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
         * Settings whose status values need to be loaded. A set is used to prevent redundant loads.
         */
        private Set<Setting> mSettingsToLoad = new HashSet<Setting>();

        /**
         * Settings that are being loaded now and haven't timed out. In practice this should have
         * zero or one elements.
         */
        private Set<Setting> mSettingsBeingLoaded = new HashSet<Setting>();

        /**
         * Settings that are being loaded but have timed out. If only one setting has timed out, we
         * will go ahead and start loading the next setting so that one slow load won't delay the
         * load of the other settings.
         */
        private Set<Setting> mTimedOutSettings = new HashSet<Setting>();

        private boolean mReloadRequested;

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage start: " + msg + ", " + this);
            }

            // Update state in response to message
            switch (msg.what) {
                case WHAT_RELOAD:
                    mReloadRequested = true;
                    break;
                case WHAT_RECEIVED_STATUS:
                    final Setting receivedSetting = (Setting) msg.obj;
                    receivedSetting.maybeLogElapsedTime();
                    mSettingsBeingLoaded.remove(receivedSetting);
                    mTimedOutSettings.remove(receivedSetting);
                    removeMessages(WHAT_TIMEOUT, receivedSetting);
                    break;
                case WHAT_TIMEOUT:
                    final Setting timedOutSetting = (Setting) msg.obj;
                    mSettingsBeingLoaded.remove(timedOutSetting);
                    mTimedOutSettings.add(timedOutSetting);
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Timed out after " + timedOutSetting.getElapsedTime()
                                + " millis trying to get status for: " + timedOutSetting);
                    }
                    break;
                default:
                    Log.wtf(TAG, "Unexpected what: " + msg);
            }

            // Decide whether to load additional settings based on the new state. Start by seeing
            // if we have headroom to load another setting.
            if (mSettingsBeingLoaded.size() > 0 || mTimedOutSettings.size() > 1) {
                // Don't load any more settings until one of the pending settings has completed.
                // To reduce memory pressure, we want to be loading at most one setting (plus at
                // most one timed-out setting) at a time. This means we'll be responsible for
                // bringing in at most two services.
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "too many services already live for " + msg + ", " + this);
                }
                return;
            }

            if (mReloadRequested && mSettingsToLoad.isEmpty() && mSettingsBeingLoaded.isEmpty()
                    && mTimedOutSettings.isEmpty()) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "reloading because idle and reload requesteed " + msg + ", " + this);
                }
                // Reload requested, so must reload all settings
                mSettingsToLoad.addAll(mSettings);
                mReloadRequested = false;
            }

            // Remove the next setting to load from the queue, if any
            Iterator<Setting> iter = mSettingsToLoad.iterator();
            if (!iter.hasNext()) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "nothing left to do for " + msg + ", " + this);
                }
                return;
            }
            Setting setting = iter.next();
            iter.remove();

            // Request the status value
            setting.startService();
            mSettingsBeingLoaded.add(setting);

            // Ensure that if receiving the status value takes too long, we start loading the
            // next value anyway
            Message timeoutMsg = obtainMessage(WHAT_TIMEOUT, setting);
            sendMessageDelayed(timeoutMsg, INJECTED_STATUS_UPDATE_TIMEOUT_MILLIS);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage end " + msg + ", " + this
                        + ", started loading " + setting);
            }
        }

        @Override
        public String toString() {
            return "StatusLoadingHandler{" +
                    "mSettingsToLoad=" + mSettingsToLoad +
                    ", mSettingsBeingLoaded=" + mSettingsBeingLoaded +
                    ", mTimedOutSettings=" + mTimedOutSettings +
                    ", mReloadRequested=" + mReloadRequested +
                    '}';
        }
    }

    /**
     * Represents an injected setting and the corresponding preference.
     */
    private final class Setting {

        public final InjectedSetting setting;
        public final Preference preference;
        public long startMillis;

        private Setting(InjectedSetting setting, Preference preference) {
            this.setting = setting;
            this.preference = preference;
        }

        @Override
        public String toString() {
            return "Setting{" +
                    "setting=" + setting +
                    ", preference=" + preference +
                    '}';
        }

        /**
         * Returns true if they both have the same {@link #setting} value. Ignores mutable
         * {@link #preference} and {@link #startMillis} so that it's safe to use in sets.
         */
        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Setting && setting.equals(((Setting) o).setting);
        }

        @Override
        public int hashCode() {
            return setting.hashCode();
        }

        /**
         * Starts the service to fetch for the current status for the setting, and updates the
         * preference when the service replies.
         */
        public void startService() {
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

            Intent intent = setting.getServiceIntent();
            intent.putExtra(SettingInjectorService.MESSENGER_KEY, messenger);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, setting + ": sending update intent: " + intent
                        + ", handler: " + handler);
                startMillis = SystemClock.elapsedRealtime();
            } else {
                startMillis = 0;
            }

            // Start the service, making sure that this is attributed to the current user rather
            // than the system user.
            mContext.startServiceAsUser(intent, android.os.Process.myUserHandle());
        }

        public long getElapsedTime() {
            long end = SystemClock.elapsedRealtime();
            return end - startMillis;
        }

        public void maybeLogElapsedTime() {
            if (Log.isLoggable(TAG, Log.DEBUG) && startMillis != 0) {
                long elapsed = getElapsedTime();
                Log.d(TAG, this + " update took " + elapsed + " millis");
            }
        }
    }
}
