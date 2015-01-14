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

package com.android.settings.print;

import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.print.PrintManager.PrintJobStateChangeListener;
import android.printservice.PrintServiceInfo;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.UserSpinnerAdapter;
import com.android.settings.UserSpinnerAdapter.UserDetails;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

/**
 * Fragment with the top level print settings.
 */
public class PrintSettingsFragment extends SettingsPreferenceFragment
        implements DialogCreatable, Indexable, OnItemSelectedListener {

    private static final int LOADER_ID_PRINT_JOBS_LOADER = 1;

    private static final String PRINT_JOBS_CATEGORY = "print_jobs_category";
    private static final String PRINT_SERVICES_CATEGORY = "print_services_category";

    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "EXTRA_PREFERENCE_KEY";
    static final String EXTRA_CHECKED = "EXTRA_CHECKED";
    static final String EXTRA_TITLE = "EXTRA_TITLE";
    static final String EXTRA_ENABLE_WARNING_TITLE = "EXTRA_ENABLE_WARNING_TITLE";
    static final String EXTRA_ENABLE_WARNING_MESSAGE = "EXTRA_ENABLE_WARNING_MESSAGE";
    static final String EXTRA_SETTINGS_TITLE = "EXTRA_SETTINGS_TITLE";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "EXTRA_SETTINGS_COMPONENT_NAME";
    static final String EXTRA_ADD_PRINTERS_TITLE = "EXTRA_ADD_PRINTERS_TITLE";
    static final String EXTRA_ADD_PRINTERS_COMPONENT_NAME = "EXTRA_ADD_PRINTERS_COMPONENT_NAME";
    static final String EXTRA_SERVICE_COMPONENT_NAME = "EXTRA_SERVICE_COMPONENT_NAME";

    static final String EXTRA_PRINT_JOB_ID = "EXTRA_PRINT_JOB_ID";

    private static final String EXTRA_PRINT_SERVICE_COMPONENT_NAME =
            "EXTRA_PRINT_SERVICE_COMPONENT_NAME";

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            updateServicesPreferences();
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateServicesPreferences();
        }
    };

    private PreferenceCategory mActivePrintJobsCategory;
    private PreferenceCategory mPrintServicesCategory;

    private PrintJobsController mPrintJobsController;
    private UserSpinnerAdapter mProfileSpinnerAdapter;
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.print_settings);

        mActivePrintJobsCategory = (PreferenceCategory) findPreference(
                PRINT_JOBS_CATEGORY);
        mPrintServicesCategory = (PreferenceCategory) findPreference(
                PRINT_SERVICES_CATEGORY);
        getPreferenceScreen().removePreference(mActivePrintJobsCategory);

        mPrintJobsController = new PrintJobsController();
        getActivity().getLoaderManager().initLoader(LOADER_ID_PRINT_JOBS_LOADER,
                null, mPrintJobsController);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        updateServicesPreferences();
        setHasOptionsMenu(true);
        startSubSettingsIfNeeded();
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        String searchUri = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.PRINT_SERVICE_SEARCH_URI);
        if (!TextUtils.isEmpty(searchUri)) {
            MenuItem menuItem = menu.add(R.string.print_menu_item_add_service);
            menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri)));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_print_state, contentRoot, false);
        TextView textView = (TextView) emptyView.findViewById(R.id.message);
        textView.setText(R.string.print_no_services_installed);
        contentRoot.addView(emptyView);
        getListView().setEmptyView(emptyView);

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        mProfileSpinnerAdapter = Utils.createUserSpinnerAdapter(um, getActivity());
        if (mProfileSpinnerAdapter != null) {
            mSpinner = (Spinner) getActivity().getLayoutInflater().inflate(
                    R.layout.spinner_view, null);
            mSpinner.setAdapter(mProfileSpinnerAdapter);
            mSpinner.setOnItemSelectedListener(this);
            setPinnedHeaderView(mSpinner);
        }
    }

    private void updateServicesPreferences() {
        if (getPreferenceScreen().findPreference(PRINT_SERVICES_CATEGORY) == null) {
            getPreferenceScreen().addPreference(mPrintServicesCategory);
        } else {
            // Since services category is auto generated we have to do a pass
            // to generate it since services can come and go.
            mPrintServicesCategory.removeAll();
        }

        List<ComponentName> enabledServices = PrintSettingsUtils
                .readEnabledPrintServices(getActivity());

        List<ResolveInfo> installedServices = getActivity().getPackageManager()
                .queryIntentServices(
                        new Intent(android.printservice.PrintService.SERVICE_INTERFACE),
                        PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

        final int installedServiceCount = installedServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            ResolveInfo installedService = installedServices.get(i);

            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                    getActivity());

            String title = installedService.loadLabel(getPackageManager()).toString();
            preference.setTitle(title);

            ComponentName componentName = new ComponentName(
                    installedService.serviceInfo.packageName,
                    installedService.serviceInfo.name);
            preference.setKey(componentName.flattenToString());

            preference.setOrder(i);
            preference.setFragment(PrintServiceSettingsFragment.class.getName());
            preference.setPersistent(false);

            final boolean serviceEnabled = enabledServices.contains(componentName);
            if (serviceEnabled) {
                preference.setSummary(getString(R.string.print_feature_state_on));
            } else {
                preference.setSummary(getString(R.string.print_feature_state_off));
            }

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            PrintServiceInfo printServiceInfo = PrintServiceInfo.create(
                    installedService, getActivity());

            CharSequence applicationLabel = installedService.loadLabel(getPackageManager());

            extras.putString(EXTRA_ENABLE_WARNING_TITLE, getString(
                    R.string.print_service_security_warning_title, applicationLabel));
            extras.putString(EXTRA_ENABLE_WARNING_MESSAGE, getString(
                    R.string.print_service_security_warning_summary, applicationLabel));

            String settingsClassName = printServiceInfo.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.print_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(installedService.serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            String addPrinterClassName = printServiceInfo.getAddPrintersActivityName();
            if (!TextUtils.isEmpty(addPrinterClassName)) {
                extras.putString(EXTRA_ADD_PRINTERS_TITLE,
                        getString(R.string.print_menu_item_add_printers));
                extras.putString(EXTRA_ADD_PRINTERS_COMPONENT_NAME,
                        new ComponentName(installedService.serviceInfo.packageName,
                                addPrinterClassName).flattenToString());
            }

            extras.putString(EXTRA_SERVICE_COMPONENT_NAME, componentName.flattenToString());

            mPrintServicesCategory.addPreference(preference);
        }

        if (mPrintServicesCategory.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mPrintServicesCategory);
        }
    }

    private void startSubSettingsIfNeeded() {
        if (getArguments() == null) {
            return;
        }
        String componentName = getArguments().getString(EXTRA_PRINT_SERVICE_COMPONENT_NAME);
        if (componentName != null) {
            getArguments().remove(EXTRA_PRINT_SERVICE_COMPONENT_NAME);
            Preference prereference = findPreference(componentName);
            if (prereference != null) {
                prereference.performClick(getPreferenceScreen());
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        UserHandle selectedUser = mProfileSpinnerAdapter.getUserHandle(position);
        if (selectedUser.getIdentifier() != UserHandle.myUserId()) {
            Intent intent = new Intent(Settings.ACTION_PRINT_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getActivity().startActivityAsUser(intent, selectedUser);
            // Go back to default selection, which is the first one
            mSpinner.setSelection(0);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to do
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }
    }

    private static abstract class SettingsContentObserver extends ContentObserver {

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.ENABLED_PRINT_SERVICES), false, this);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override
        public abstract void onChange(boolean selfChange, Uri uri);
    }

    private final class PrintJobsController implements LoaderCallbacks<List<PrintJobInfo>> {

        @Override
        public Loader<List<PrintJobInfo>> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ID_PRINT_JOBS_LOADER) {
                return new PrintJobsLoader(getActivity());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrintJobInfo>> loader,
                List<PrintJobInfo> printJobs) {
            if (printJobs == null || printJobs.isEmpty()) {
                getPreferenceScreen().removePreference(mActivePrintJobsCategory);
            } else {
                if (getPreferenceScreen().findPreference(PRINT_JOBS_CATEGORY) == null) {
                    getPreferenceScreen().addPreference(mActivePrintJobsCategory);
                }

                mActivePrintJobsCategory.removeAll();

                final int printJobCount = printJobs.size();
                for (int i = 0; i < printJobCount; i++) {
                    PrintJobInfo printJob = printJobs.get(i);

                    PreferenceScreen preference = getPreferenceManager()
                            .createPreferenceScreen(getActivity());

                    preference.setPersistent(false);
                    preference.setFragment(PrintJobSettingsFragment.class.getName());
                    preference.setKey(printJob.getId().flattenToString());

                    switch (printJob.getState()) {
                        case PrintJobInfo.STATE_QUEUED:
                        case PrintJobInfo.STATE_STARTED: {
                            if (!printJob.isCancelling()) {
                                preference.setTitle(getString(
                                        R.string.print_printing_state_title_template,
                                        printJob.getLabel()));
                            } else {
                                preference.setTitle(getString(
                                        R.string.print_cancelling_state_title_template,
                                        printJob.getLabel()));
                            }
                        } break;

                        case PrintJobInfo.STATE_FAILED: {
                            preference.setTitle(getString(
                                    R.string.print_failed_state_title_template,
                                    printJob.getLabel()));
                        } break;

                        case PrintJobInfo.STATE_BLOCKED: {
                            if (!printJob.isCancelling()) {
                                preference.setTitle(getString(
                                        R.string.print_blocked_state_title_template,
                                        printJob.getLabel()));
                            } else {
                                preference.setTitle(getString(
                                        R.string.print_cancelling_state_title_template,
                                        printJob.getLabel()));
                            }
                        } break;
                    }

                    preference.setSummary(getString(R.string.print_job_summary,
                            printJob.getPrinterName(), DateUtils.formatSameDayTime(
                                    printJob.getCreationTime(), printJob.getCreationTime(),
                                    DateFormat.SHORT, DateFormat.SHORT)));

                    switch (printJob.getState()) {
                        case PrintJobInfo.STATE_QUEUED:
                        case PrintJobInfo.STATE_STARTED: {
                            preference.setIcon(R.drawable.ic_print);
                        } break;

                        case PrintJobInfo.STATE_FAILED:
                        case PrintJobInfo.STATE_BLOCKED: {
                            preference.setIcon(R.drawable.ic_print_error);
                        } break;
                    }

                    Bundle extras = preference.getExtras();
                    extras.putString(EXTRA_PRINT_JOB_ID, printJob.getId().flattenToString());

                    mActivePrintJobsCategory.addPreference(preference);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<PrintJobInfo>> loader) {
            getPreferenceScreen().removePreference(mActivePrintJobsCategory);
        }
    }

    private static final class PrintJobsLoader extends AsyncTaskLoader<List<PrintJobInfo>> {

        private static final String LOG_TAG = "PrintJobsLoader";

        private static final boolean DEBUG = false;

        private List<PrintJobInfo> mPrintJobs = new ArrayList<PrintJobInfo>();

        private final PrintManager mPrintManager;

        private PrintJobStateChangeListener mPrintJobStateChangeListener;

        public PrintJobsLoader(Context context) {
            super(context);
            mPrintManager = ((PrintManager) context.getSystemService(
                    Context.PRINT_SERVICE)).getGlobalPrintManagerForUser(
                    context.getUserId());
        }

        @Override
        public void deliverResult(List<PrintJobInfo> printJobs) {
            if (isStarted()) {
                super.deliverResult(printJobs);
            }
        }

        @Override
        protected void onStartLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStartLoading()");
            }
            // If we already have a result, deliver it immediately.
            if (!mPrintJobs.isEmpty()) {
                deliverResult(new ArrayList<PrintJobInfo>(mPrintJobs));
            }
            // Start watching for changes.
            if (mPrintJobStateChangeListener == null) {
                mPrintJobStateChangeListener = new PrintJobStateChangeListener() {
                    @Override
                    public void onPrintJobStateChanged(PrintJobId printJobId) {
                        onForceLoad();
                    }
                };
                mPrintManager.addPrintJobStateChangeListener(
                        mPrintJobStateChangeListener);
            }
            // If the data changed or we have no data - load it now.
            if (mPrintJobs.isEmpty()) {
                onForceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStopLoading()");
            }
            // Cancel the load in progress if possible.
            onCancelLoad();
        }

        @Override
        protected void onReset() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onReset()");
            }
            // Stop loading.
            onStopLoading();
            // Clear the cached result.
            mPrintJobs.clear();
            // Stop watching for changes.
            if (mPrintJobStateChangeListener != null) {
                mPrintManager.removePrintJobStateChangeListener(
                        mPrintJobStateChangeListener);
                mPrintJobStateChangeListener = null;
            }
        }

        @Override
        public List<PrintJobInfo> loadInBackground() {
            List<PrintJobInfo> printJobInfos = null;
            List<PrintJob> printJobs = mPrintManager.getPrintJobs();
            final int printJobCount = printJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo printJob = printJobs.get(i).getInfo();
                if (shouldShowToUser(printJob)) {
                    if (printJobInfos == null) {
                        printJobInfos = new ArrayList<PrintJobInfo>();
                    }
                    printJobInfos.add(printJob);
                }
            }
            return printJobInfos;
        }

        private static boolean shouldShowToUser(PrintJobInfo printJob) {
            switch (printJob.getState()) {
                case PrintJobInfo.STATE_QUEUED:
                case PrintJobInfo.STATE_STARTED:
                case PrintJobInfo.STATE_BLOCKED:
                case PrintJobInfo.STATE_FAILED: {
                    return true;
                }
            }
            return false;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            PackageManager packageManager = context.getPackageManager();
            PrintManager printManager = (PrintManager) context.getSystemService(
                    Context.PRINT_SERVICE);

            String screenTitle = context.getResources().getString(R.string.print_settings);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            indexables.add(data);

            // Indexing all services, regardless if enabled.
            List<PrintServiceInfo> services = printManager.getInstalledPrintServices();
            final int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                PrintServiceInfo service = services.get(i);

                ComponentName componentName = new ComponentName(
                        service.getResolveInfo().serviceInfo.packageName,
                        service.getResolveInfo().serviceInfo.name);

                data = new SearchIndexableRaw(context);
                data.key = componentName.flattenToString();
                data.title = service.getResolveInfo().loadLabel(packageManager).toString();
                data.summaryOn = context.getString(R.string.print_feature_state_on);
                data.summaryOff = context.getString(R.string.print_feature_state_off);
                data.screenTitle = screenTitle;
                indexables.add(data);
            }

            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList<SearchIndexableResource>();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.print_settings;
            indexables.add(indexable);
            return indexables;
        }
    };
}
