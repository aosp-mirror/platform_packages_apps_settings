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

package com.android.settings.applications;

import static com.android.settings.widget.EntityHeaderController.ActionType;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.CancellablePreference;
import com.android.settings.CancellablePreference.OnCancelListener;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SummaryPreference;
import com.android.settings.applications.ProcStatsEntry.Service;
import com.android.settings.widget.EntityHeaderController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ProcessStatsDetail extends SettingsPreferenceFragment {

    private static final String TAG = "ProcessStatsDetail";

    public static final int MENU_FORCE_STOP = 1;

    public static final String EXTRA_PACKAGE_ENTRY = "package_entry";
    public static final String EXTRA_WEIGHT_TO_RAM = "weight_to_ram";
    public static final String EXTRA_TOTAL_TIME = "total_time";
    public static final String EXTRA_MAX_MEMORY_USAGE = "max_memory_usage";
    public static final String EXTRA_TOTAL_SCALE = "total_scale";

    private static final String KEY_DETAILS_HEADER = "status_header";

    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_MAX_USAGE = "max_usage";

    private static final String KEY_PROCS = "processes";

    private final ArrayMap<ComponentName, CancellablePreference> mServiceMap = new ArrayMap<>();

    private PackageManager mPm;
    private DevicePolicyManager mDpm;

    private MenuItem mForceStop;

    private ProcStatsPackageEntry mApp;
    private double mWeightToRam;
    private long mTotalTime;
    private long mOnePercentTime;

    private double mMaxMemoryUsage;

    private double mTotalScale;

    private PreferenceCategory mProcGroup;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPm = getActivity().getPackageManager();
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        final Bundle args = getArguments();
        mApp = args.getParcelable(EXTRA_PACKAGE_ENTRY);
        mApp.retrieveUiData(getActivity(), mPm);
        mWeightToRam = args.getDouble(EXTRA_WEIGHT_TO_RAM);
        mTotalTime = args.getLong(EXTRA_TOTAL_TIME);
        mMaxMemoryUsage = args.getDouble(EXTRA_MAX_MEMORY_USAGE);
        mTotalScale = args.getDouble(EXTRA_TOTAL_SCALE);
        mOnePercentTime = mTotalTime/100;

        mServiceMap.clear();
        createDetails();
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mApp.mUiTargetApp == null) {
            finish();
            return;
        }
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* appHeader */)
                .setIcon(mApp.mUiTargetApp != null
                        ? IconDrawableFactory.newInstance(activity).getBadgedIcon(mApp.mUiTargetApp)
                        : new ColorDrawable(0))
                .setLabel(mApp.mUiLabel)
                .setPackageName(mApp.mPackage)
                .setUid(mApp.mUiTargetApp != null
                        ? mApp.mUiTargetApp.uid
                        : UserHandle.USER_NULL)
                .setHasAppInfoLink(true)
                .setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE)
                .done(getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_PROCESS_STATS_DETAIL;
    }

    @Override
    public void onResume() {
        super.onResume();

        checkForceStop();
        updateRunningServices();
    }

    private void updateRunningServices() {
        ActivityManager activityManager = (ActivityManager)
                getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> runningServices =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        // Set all services as not running, then turn back on the ones we find.
        int N = mServiceMap.size();
        for (int i = 0; i < N; i++) {
            mServiceMap.valueAt(i).setCancellable(false);
        }

        N = runningServices.size();
        for (int i = 0; i < N; i++) {
            RunningServiceInfo runningService = runningServices.get(i);
            if (!runningService.started && runningService.clientLabel == 0) {
                continue;
            }
            if ((runningService.flags & RunningServiceInfo.FLAG_PERSISTENT_PROCESS) != 0) {
                continue;
            }
            final ComponentName service = runningService.service;
            CancellablePreference pref = mServiceMap.get(service);
            if (pref != null) {
                pref.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(CancellablePreference preference) {
                        stopService(service.getPackageName(), service.getClassName());
                    }
                });
                pref.setCancellable(true);
            }
        }
    }

    private void createDetails() {
        addPreferencesFromResource(R.xml.app_memory_settings);

        mProcGroup = (PreferenceCategory) findPreference(KEY_PROCS);
        fillProcessesSection();

        SummaryPreference summaryPreference = (SummaryPreference) findPreference(KEY_DETAILS_HEADER);

        // TODO: Find way to share this code with ProcessStatsPreference.
        boolean statsForeground = mApp.mRunWeight > mApp.mBgWeight;
        double avgRam = (statsForeground ? mApp.mRunWeight : mApp.mBgWeight) * mWeightToRam;
        float avgRatio = (float) (avgRam / mMaxMemoryUsage);
        float remainingRatio = 1 - avgRatio;
        Context context = getActivity();
        summaryPreference.setRatios(avgRatio, 0, remainingRatio);
        Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(),
                (long) avgRam, Formatter.FLAG_SHORTER);
        summaryPreference.setAmount(usedResult.value);
        summaryPreference.setUnits(usedResult.units);

        long duration = Math.max(mApp.mRunDuration, mApp.mBgDuration);
        CharSequence frequency = ProcStatsPackageEntry.getFrequency(duration
                / (float) mTotalTime, getActivity());
        findPreference(KEY_FREQUENCY).setSummary(frequency);
        double max = Math.max(mApp.mMaxBgMem, mApp.mMaxRunMem) * mTotalScale * 1024;
        findPreference(KEY_MAX_USAGE).setSummary(
                Formatter.formatShortFileSize(getContext(), (long) max));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mForceStop = menu.add(0, MENU_FORCE_STOP, 0, R.string.force_stop);
        checkForceStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FORCE_STOP:
                killProcesses();
                return true;
        }
        return false;
    }

    final static Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mRunWeight < rhs.mRunWeight) {
                return 1;
            } else if (lhs.mRunWeight > rhs.mRunWeight) {
                return -1;
            }
            return 0;
        }
    };

    private void fillProcessesSection() {
        mProcGroup.removeAll();
        final ArrayList<ProcStatsEntry> entries = new ArrayList<>();
        for (int ie = 0; ie < mApp.mEntries.size(); ie++) {
            ProcStatsEntry entry = mApp.mEntries.get(ie);
            if (entry.mPackage.equals("os")) {
                entry.mLabel = entry.mName;
            } else {
                entry.mLabel = getProcessName(mApp.mUiLabel, entry);
            }
            entries.add(entry);
        }
        Collections.sort(entries, sEntryCompare);
        for (int ie = 0; ie < entries.size(); ie++) {
            ProcStatsEntry entry = entries.get(ie);
            Preference processPref = new Preference(getPrefContext());
            processPref.setTitle(entry.mLabel);
            processPref.setSelectable(false);

            long duration = Math.max(entry.mRunDuration, entry.mBgDuration);
            long memoryUse = Math.max((long) (entry.mRunWeight * mWeightToRam),
                    (long) (entry.mBgWeight * mWeightToRam));
            String memoryString = Formatter.formatShortFileSize(getActivity(), memoryUse);
            CharSequence frequency = ProcStatsPackageEntry.getFrequency(duration
                    / (float) mTotalTime, getActivity());
            processPref.setSummary(
                    getString(R.string.memory_use_running_format, memoryString, frequency));
            mProcGroup.addPreference(processPref);
        }
        if (mProcGroup.getPreferenceCount() < 2) {
            getPreferenceScreen().removePreference(mProcGroup);
        }
    }

    private static String capitalize(String processName) {
        char c = processName.charAt(0);
        if (!Character.isLowerCase(c)) {
            return processName;
        }
        return Character.toUpperCase(c) + processName.substring(1);
    }

    private static String getProcessName(String appLabel, ProcStatsEntry entry) {
        String processName = entry.mName;
        if (processName.contains(":")) {
            return capitalize(processName.substring(processName.lastIndexOf(':') + 1));
        }
        if (processName.startsWith(entry.mPackage)) {
            if (processName.length() == entry.mPackage.length()) {
                return appLabel;
            }
            int start = entry.mPackage.length();
            if (processName.charAt(start) == '.') {
                start++;
            }
            return capitalize(processName.substring(start));
        }
        return processName;
    }

    final static Comparator<ProcStatsEntry.Service> sServiceCompare
            = new Comparator<ProcStatsEntry.Service>() {
        @Override
        public int compare(ProcStatsEntry.Service lhs, ProcStatsEntry.Service rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            } else if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    };

    final static Comparator<PkgService> sServicePkgCompare = new Comparator<PkgService>() {
        @Override
        public int compare(PkgService lhs, PkgService rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            } else if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    };

    static class PkgService {
        final ArrayList<ProcStatsEntry.Service> mServices = new ArrayList<>();
        long mDuration;
    }

    private void fillServicesSection(ProcStatsEntry entry, PreferenceCategory processPref) {
        final HashMap<String, PkgService> pkgServices = new HashMap<>();
        final ArrayList<PkgService> pkgList = new ArrayList<>();
        for (int ip = 0; ip < entry.mServices.size(); ip++) {
            String pkg = entry.mServices.keyAt(ip);
            PkgService psvc = null;
            ArrayList<ProcStatsEntry.Service> services = entry.mServices.valueAt(ip);
            for (int is=services.size()-1; is>=0; is--) {
                ProcStatsEntry.Service pent = services.get(is);
                if (pent.mDuration >= mOnePercentTime) {
                    if (psvc == null) {
                        psvc = pkgServices.get(pkg);
                        if (psvc == null) {
                            psvc = new PkgService();
                            pkgServices.put(pkg, psvc);
                            pkgList.add(psvc);
                        }
                    }
                    psvc.mServices.add(pent);
                    psvc.mDuration += pent.mDuration;
                }
            }
        }
        Collections.sort(pkgList, sServicePkgCompare);
        for (int ip = 0; ip < pkgList.size(); ip++) {
            ArrayList<ProcStatsEntry.Service> services = pkgList.get(ip).mServices;
            Collections.sort(services, sServiceCompare);
            for (int is=0; is<services.size(); is++) {
                final ProcStatsEntry.Service service = services.get(is);
                CharSequence label = getLabel(service);
                CancellablePreference servicePref = new CancellablePreference(getPrefContext());
                servicePref.setSelectable(false);
                servicePref.setTitle(label);
                servicePref.setSummary(ProcStatsPackageEntry.getFrequency(
                        service.mDuration / (float) mTotalTime, getActivity()));
                processPref.addPreference(servicePref);
                mServiceMap.put(new ComponentName(service.mPackage, service.mName), servicePref);
            }
        }
    }

    private CharSequence getLabel(Service service) {
        // Try to get the service label, on the off chance that one exists.
        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(
                    new ComponentName(service.mPackage, service.mName), 0);
            if (serviceInfo.labelRes != 0) {
                return serviceInfo.loadLabel(getPackageManager());
            }
        } catch (NameNotFoundException e) {
        }
        String label = service.mName;
        int tail = label.lastIndexOf('.');
        if (tail >= 0 && tail < (label.length()-1)) {
            label = label.substring(tail+1);
        }
        return label;
    }

    private void stopService(String pkg, String name) {
        try {
            ApplicationInfo appInfo = getActivity().getPackageManager().getApplicationInfo(pkg, 0);
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                showStopServiceDialog(pkg, name);
                return;
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Can't find app " + pkg, e);
            return;
        }
        doStopService(pkg, name);
    }

    private void showStopServiceDialog(final String pkg, final String name) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.runningservicedetails_stop_dlg_title)
                .setMessage(R.string.runningservicedetails_stop_dlg_text)
                .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        doStopService(pkg, name);
                    }
                })
                .setNegativeButton(R.string.dlg_cancel, null)
                .show();
    }

    private void doStopService(String pkg, String name) {
        getActivity().stopService(new Intent().setClassName(pkg, name));
        updateRunningServices();
    }

    private void killProcesses() {
        ActivityManager am = (ActivityManager)getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        for (int i=0; i< mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = mApp.mEntries.get(i);
            for (int j=0; j<ent.mPackages.size(); j++) {
                am.forceStopPackage(ent.mPackages.get(j));
            }
        }
    }

    private void checkForceStop() {
        if (mForceStop == null) {
            return;
        }
        if (mApp.mEntries.get(0).mUid < Process.FIRST_APPLICATION_UID) {
            mForceStop.setVisible(false);
            return;
        }
        boolean isStarted = false;
        for (int i=0; i< mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = mApp.mEntries.get(i);
            for (int j=0; j<ent.mPackages.size(); j++) {
                String pkg = ent.mPackages.get(j);
                if (mDpm.packageHasActiveAdmins(pkg)) {
                    mForceStop.setEnabled(false);
                    return;
                }
                try {
                    ApplicationInfo info = mPm.getApplicationInfo(pkg, 0);
                    if ((info.flags&ApplicationInfo.FLAG_STOPPED) == 0) {
                        isStarted = true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        if (isStarted) {
            mForceStop.setVisible(true);
        }
    }
}
