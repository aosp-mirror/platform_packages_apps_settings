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

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static com.android.settings.Utils.prepareCustomPreferencesList;

public class ProcessStatsDetail extends InstrumentedFragment implements Button.OnClickListener {
    private static final String TAG = "ProcessStatsDetail";

    public static final int ACTION_FORCE_STOP = 1;

    public static final String EXTRA_PACKAGE_ENTRY = "package_entry";
    public static final String EXTRA_USE_USS = "use_uss";
    public static final String EXTRA_MAX_WEIGHT = "max_weight";
    public static final String EXTRA_WEIGHT_TO_RAM = "weight_to_ram";
    public static final String EXTRA_TOTAL_TIME = "total_time";

    private PackageManager mPm;
    private DevicePolicyManager mDpm;

    private ProcStatsPackageEntry mApp;
    private boolean mUseUss;
    private double mMaxWeight;
    private double mWeightToRam;
    private long mTotalTime;
    private long mOnePercentTime;

    private View mRootView;
    private TextView mTitleView;
    private ViewGroup mTwoButtonsPanel;
    private Button mForceStopButton;
    private Button mReportButton;
    private ViewGroup mProcessesParent;
    private ViewGroup mServicesParent;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPm = getActivity().getPackageManager();
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        final Bundle args = getArguments();
        mApp = args.getParcelable(EXTRA_PACKAGE_ENTRY);
        mApp.retrieveUiData(getActivity(), mPm);
        mUseUss = args.getBoolean(EXTRA_USE_USS);
        mMaxWeight = args.getDouble(EXTRA_MAX_WEIGHT);
        mWeightToRam = args.getDouble(EXTRA_WEIGHT_TO_RAM);
        mTotalTime = args.getLong(EXTRA_TOTAL_TIME);
        mOnePercentTime = mTotalTime/100;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.process_stats_details, container, false);
        prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        createDetails();
        return view;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_PROCESS_STATS_DETAIL;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForceStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void createDetails() {
        final double percentOfWeight = (mApp.mBgWeight / mMaxWeight) * 100;

        int appLevel = (int) Math.ceil(percentOfWeight);
        String appLevelText = Formatter.formatShortFileSize(getActivity(),
                (long)(mApp.mRunWeight * mWeightToRam));

        // Set all values in the header.
        mTitleView = (TextView) mRootView.findViewById(android.R.id.title);
        mTitleView.setText(mApp.mUiLabel);
        final TextView text1 = (TextView)mRootView.findViewById(android.R.id.text1);
        text1.setText(appLevelText);
        final ProgressBar progress = (ProgressBar) mRootView.findViewById(android.R.id.progress);
        progress.setProgress(appLevel);
        final ImageView icon = (ImageView) mRootView.findViewById(android.R.id.icon);
        if (mApp.mUiTargetApp != null) {
            icon.setImageDrawable(mApp.mUiTargetApp.loadIcon(mPm));
        }

        mTwoButtonsPanel = (ViewGroup)mRootView.findViewById(R.id.two_buttons_panel);
        mForceStopButton = (Button)mRootView.findViewById(R.id.right_button);
        mReportButton = (Button)mRootView.findViewById(R.id.left_button);
        mForceStopButton.setEnabled(false);
        mReportButton.setVisibility(View.INVISIBLE);

        mProcessesParent = (ViewGroup)mRootView.findViewById(R.id.processes);
        mServicesParent = (ViewGroup)mRootView.findViewById(R.id.services);

        fillProcessesSection();
        fillServicesSection();
        if (mServicesParent.getChildCount() <= 0) {
            mServicesParent.setVisibility(View.GONE);
            mRootView.findViewById(R.id.services_label).setVisibility(View.GONE);
        }

        if (mApp.mEntries.get(0).mUid >= android.os.Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setText(R.string.force_stop);
            mForceStopButton.setTag(ACTION_FORCE_STOP);
            mForceStopButton.setOnClickListener(this);
            mTwoButtonsPanel.setVisibility(View.VISIBLE);
        } else {
            mTwoButtonsPanel.setVisibility(View.GONE);
        }
    }

    public void onClick(View v) {
        doAction((Integer) v.getTag());
    }

    private void doAction(int action) {
        switch (action) {
            case ACTION_FORCE_STOP:
                killProcesses();
                break;
        }
    }

    private void addPackageHeaderItem(ViewGroup parent, String packageName) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.running_processes_item,
                null);
        parent.addView(item);
        final ImageView icon = (ImageView) item.findViewById(R.id.icon);
        TextView nameView = (TextView) item.findViewById(R.id.name);
        TextView descriptionView = (TextView) item.findViewById(R.id.description);
        try {
            ApplicationInfo ai = mPm.getApplicationInfo(packageName, 0);
            icon.setImageDrawable(ai.loadIcon(mPm));
            nameView.setText(ai.loadLabel(mPm));
        } catch (PackageManager.NameNotFoundException e) {
        }
        descriptionView.setText(packageName);
    }

    private void addDetailsItem(ViewGroup parent, CharSequence label, CharSequence value) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.power_usage_detail_item_text,
                null);
        parent.addView(item);
        TextView labelView = (TextView) item.findViewById(R.id.label);
        TextView valueView = (TextView) item.findViewById(R.id.value);
        labelView.setText(label);
        valueView.setText(value);
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
        final ArrayList<ProcStatsEntry> entries = new ArrayList<>();
        for (int ie=0; ie<mApp.mEntries.size(); ie++) {
            ProcStatsEntry entry = mApp.mEntries.get(ie);
            entries.add(entry);
        }
        Collections.sort(entries, sEntryCompare);
        for (int ie=0; ie<entries.size(); ie++) {
            ProcStatsEntry entry = entries.get(ie);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            ViewGroup item = (ViewGroup) inflater.inflate(R.layout.process_stats_proc_details,
                    null);
            mProcessesParent.addView(item);
            ((TextView)item.findViewById(R.id.processes_name)).setText(entry.mName);
            addDetailsItem(item, getResources().getText(R.string.process_stats_ram_use),
                    Formatter.formatShortFileSize(getActivity(),
                            (long)(entry.mRunWeight * mWeightToRam)));
            if (entry.mBgWeight > 0) {
                addDetailsItem(item, getResources().getText(R.string.process_stats_bg_ram_use),
                        Formatter.formatShortFileSize(getActivity(),
                                (long)(entry.mBgWeight * mWeightToRam)));
            }
            addDetailsItem(item, getResources().getText(R.string.process_stats_run_time),
                    Utils.formatPercentage(entry.mRunDuration, mTotalTime));
        }
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

    private void fillServicesSection() {
        final HashMap<String, PkgService> pkgServices = new HashMap<>();
        final ArrayList<PkgService> pkgList = new ArrayList<>();
        for (int ie=0; ie< mApp.mEntries.size(); ie++) {
            ProcStatsEntry ent = mApp.mEntries.get(ie);
            for (int ip=0; ip<ent.mServices.size(); ip++) {
                String pkg = ent.mServices.keyAt(ip);
                PkgService psvc = null;
                ArrayList<ProcStatsEntry.Service> services = ent.mServices.valueAt(ip);
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
        }
        Collections.sort(pkgList, sServicePkgCompare);
        for (int ip=0; ip<pkgList.size(); ip++) {
            ArrayList<ProcStatsEntry.Service> services = pkgList.get(ip).mServices;
            Collections.sort(services, sServiceCompare);
            if (pkgList.size() > 1) {
                addPackageHeaderItem(mServicesParent, services.get(0).mPackage);
            }
            for (int is=0; is<services.size(); is++) {
                ProcStatsEntry.Service service = services.get(is);
                String label = service.mName;
                int tail = label.lastIndexOf('.');
                if (tail >= 0 && tail < (label.length()-1)) {
                    label = label.substring(tail+1);
                }
                String percentage = Utils.formatPercentage(service.mDuration, mTotalTime);
                addDetailsItem(mServicesParent, label, percentage);
            }
        }
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
        checkForceStop();
    }

    private void checkForceStop() {
        if (mApp.mEntries.get(0).mUid < Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setEnabled(false);
            return;
        }
        boolean isStarted = false;
        for (int i=0; i< mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = mApp.mEntries.get(i);
            for (int j=0; j<ent.mPackages.size(); j++) {
                String pkg = ent.mPackages.get(j);
                if (mDpm.packageHasActiveAdmins(pkg)) {
                    mForceStopButton.setEnabled(false);
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
            mForceStopButton.setEnabled(true);
        }
    }
}
