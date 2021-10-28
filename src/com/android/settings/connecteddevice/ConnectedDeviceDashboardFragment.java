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
package com.android.settings.connecteddevice;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "ConnectedDeviceFrag";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    static final String KEY_CONNECTED_DEVICES = "connected_device_list";
    @VisibleForTesting
    static final String KEY_AVAILABLE_DEVICES = "available_device_list";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final DiscoverableFooterPreferenceController discoverableFooterPreferenceController =
                new DiscoverableFooterPreferenceController(context);
        controllers.add(discoverableFooterPreferenceController);

        if (lifecycle != null) {
            lifecycle.addObserver(discoverableFooterPreferenceController);
        }

        return controllers;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        String callingAppPackageName = getCallingAppPackageName(getActivity().getActivityToken());
        if (DEBUG) {
            Log.d(TAG, "onAttach() calling package name is : " + callingAppPackageName);
        }
        use(AvailableMediaDeviceGroupController.class).init(this);
        use(ConnectedDeviceGroupController.class).init(this);
        use(PreviouslyConnectedDevicePreferenceController.class).init(this);
        use(DiscoverableFooterPreferenceController.class).init(this,
                TextUtils.equals(SETTINGS_PACKAGE_NAME, callingAppPackageName)
                        || TextUtils.equals(SYSTEMUI_PACKAGE_NAME, callingAppPackageName));
    }

    private String getCallingAppPackageName(IBinder activityToken) {
        String pkg = null;
        try {
            pkg = ActivityManager.getService().getLaunchedFromPackage(activityToken);
        } catch (RemoteException e) {
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
        return pkg;
    }

    @VisibleForTesting
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
                mSummaryLoader.setSummary(this, mContext.getText(AdvancedConnectedDeviceController.
                        getConnectedDevicesSummaryResourceId(mContext)));
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

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.connected_devices;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Disable because they show dynamic data
                    keys.add(KEY_AVAILABLE_DEVICES);
                    keys.add(KEY_CONNECTED_DEVICES);
                    return keys;
                }
            };
}
