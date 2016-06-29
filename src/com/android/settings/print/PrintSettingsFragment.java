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

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.print.PrintManager.PrintJobStateChangeListener;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.utils.ProfileSettingsPreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment with the top level print settings.
 */
public class PrintSettingsFragment extends ProfileSettingsPreferenceFragment
        implements DialogCreatable, Indexable, OnClickListener {
    public static final String TAG = "PrintSettingsFragment";
    private static final int LOADER_ID_PRINT_JOBS_LOADER = 1;
    private static final int LOADER_ID_PRINT_SERVICES = 2;

    private static final String PRINT_JOBS_CATEGORY = "print_jobs_category";
    private static final String PRINT_SERVICES_CATEGORY = "print_services_category";

    static final String EXTRA_CHECKED = "EXTRA_CHECKED";
    static final String EXTRA_TITLE = "EXTRA_TITLE";
    static final String EXTRA_SERVICE_COMPONENT_NAME = "EXTRA_SERVICE_COMPONENT_NAME";

    static final String EXTRA_PRINT_JOB_ID = "EXTRA_PRINT_JOB_ID";

    private static final String EXTRA_PRINT_SERVICE_COMPONENT_NAME =
            "EXTRA_PRINT_SERVICE_COMPONENT_NAME";

    private static final int ORDER_LAST = Preference.DEFAULT_ORDER - 1;

    private PreferenceCategory mActivePrintJobsCategory;
    private PreferenceCategory mPrintServicesCategory;

    private PrintJobsController mPrintJobsController;
    private PrintServicesController mPrintServicesController;

    private Button mAddNewServiceButton;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.PRINT_SETTINGS;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_printing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        addPreferencesFromResource(R.xml.print_settings);

        mActivePrintJobsCategory = (PreferenceCategory) findPreference(
                PRINT_JOBS_CATEGORY);
        mPrintServicesCategory = (PreferenceCategory) findPreference(
                PRINT_SERVICES_CATEGORY);
        getPreferenceScreen().removePreference(mActivePrintJobsCategory);

        mPrintJobsController = new PrintJobsController();
        getLoaderManager().initLoader(LOADER_ID_PRINT_JOBS_LOADER, null, mPrintJobsController);

        mPrintServicesController = new PrintServicesController();
        getLoaderManager().initLoader(LOADER_ID_PRINT_SERVICES, null, mPrintServicesController);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        startSubSettingsIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_print_state, contentRoot, false);
        TextView textView = (TextView) emptyView.findViewById(R.id.message);
        textView.setText(R.string.print_no_services_installed);

        final Intent addNewServiceIntent = createAddNewServiceIntentOrNull();
        if (addNewServiceIntent != null) {
            mAddNewServiceButton = (Button) emptyView.findViewById(R.id.add_new_service);
            mAddNewServiceButton.setOnClickListener(this);
            // The empty is used elsewhere too so it's hidden by default.
            mAddNewServiceButton.setVisibility(View.VISIBLE);
        }

        contentRoot.addView(emptyView);
        setEmptyView(emptyView);
    }

    @Override
    protected String getIntentActionString() {
        return Settings.ACTION_PRINT_SETTINGS;
    }

    /**
     * Adds preferences for all print services to the {@value PRINT_SERVICES_CATEGORY} cathegory.
     */
    private final class PrintServicesController implements
           LoaderCallbacks<List<PrintServiceInfo>> {
        @Override
        public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
            PrintManager printManager =
                    (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
            if (printManager != null) {
                return new PrintServicesLoader(printManager, getContext(),
                        PrintManager.ALL_SERVICES);
            } else {
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<List<PrintServiceInfo>> loader,
                List<PrintServiceInfo> services) {
            if (services.isEmpty()) {
                getPreferenceScreen().removePreference(mPrintServicesCategory);
                return;
            } else if (getPreferenceScreen().findPreference(PRINT_SERVICES_CATEGORY) == null) {
                getPreferenceScreen().addPreference(mPrintServicesCategory);
            }

            mPrintServicesCategory.removeAll();
            PackageManager pm = getActivity().getPackageManager();

            final int numServices = services.size();
            for (int i = 0; i < numServices; i++) {
                PrintServiceInfo service = services.get(i);
                PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(
                        getActivity());

                String title = service.getResolveInfo().loadLabel(pm).toString();
                preference.setTitle(title);

                ComponentName componentName = service.getComponentName();
                preference.setKey(componentName.flattenToString());

                preference.setFragment(PrintServiceSettingsFragment.class.getName());
                preference.setPersistent(false);

                if (service.isEnabled()) {
                    preference.setSummary(getString(R.string.print_feature_state_on));
                } else {
                    preference.setSummary(getString(R.string.print_feature_state_off));
                }

                Drawable drawable = service.getResolveInfo().loadIcon(pm);
                if (drawable != null) {
                    preference.setIcon(drawable);
                }

                Bundle extras = preference.getExtras();
                extras.putBoolean(EXTRA_CHECKED, service.isEnabled());
                extras.putString(EXTRA_TITLE, title);
                extras.putString(EXTRA_SERVICE_COMPONENT_NAME, componentName.flattenToString());

                mPrintServicesCategory.addPreference(preference);
            }

            Preference addNewServicePreference = newAddServicePreferenceOrNull();
            if (addNewServicePreference != null) {
                mPrintServicesCategory.addPreference(addNewServicePreference);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
            getPreferenceScreen().removePreference(mPrintServicesCategory);
        }
    }

    private Preference newAddServicePreferenceOrNull() {
        final Intent addNewServiceIntent = createAddNewServiceIntentOrNull();
        if (addNewServiceIntent == null) {
            return null;
        }
        Preference preference = new Preference(getPrefContext());
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_menu_add);
        preference.setOrder(ORDER_LAST);
        preference.setIntent(addNewServiceIntent);
        preference.setPersistent(false);
        return preference;
    }

    private Intent createAddNewServiceIntentOrNull() {
        final String searchUri = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.PRINT_SERVICE_SEARCH_URI);
        if (TextUtils.isEmpty(searchUri)) {
            return null;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
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
                prereference.performClick();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mAddNewServiceButton == v) {
            final Intent addNewServiceIntent = createAddNewServiceIntentOrNull();
            if (addNewServiceIntent != null) { // check again just in case.
                try {
                    startActivity(addNewServiceIntent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to start activity", e);
                }
            }
        }
    }

     private final class PrintJobsController implements LoaderCallbacks<List<PrintJobInfo>> {

        @Override
        public Loader<List<PrintJobInfo>> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ID_PRINT_JOBS_LOADER) {
                return new PrintJobsLoader(getContext());
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
    }

    /**
     * Should the print job the shown to the user in the settings app.
     *
     * @param printJob The print job in question.
     * @return true iff the print job should be shown.
     */
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

    /**
     * Provider for the print settings summary
     */
    private static class PrintSummaryProvider
            implements SummaryLoader.SummaryProvider, PrintJobStateChangeListener {
        private final Context mContext;
        private final PrintManager mPrintManager;
        private final SummaryLoader mSummaryLoader;

        /**
         * Create a new {@link PrintSummaryProvider}.
         *
         * @param context The context this provider is for
         * @param summaryLoader The summary load using this provider
         */
        public PrintSummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mPrintManager = ((PrintManager) context.getSystemService(Context.PRINT_SERVICE))
                    .getGlobalPrintManagerForUser(context.getUserId());
        }

        @Override
        public void setListening(boolean isListening) {
            if (mPrintManager != null) {
                if (isListening) {
                    mPrintManager.addPrintJobStateChangeListener(this);
                    onPrintJobStateChanged(null);
                } else {
                    mPrintManager.removePrintJobStateChangeListener(this);
                }
            }
        }

        @Override
        public void onPrintJobStateChanged(PrintJobId printJobId) {
            List<PrintJob> printJobs = mPrintManager.getPrintJobs();

            int numActivePrintJobs = 0;
            final int numPrintJobs = printJobs.size();
            for (int i = 0; i < numPrintJobs; i++) {
                if (shouldShowToUser(printJobs.get(i).getInfo())) {
                    numActivePrintJobs++;
                }
            }

            mSummaryLoader.setSummary(this, mContext.getResources().getQuantityString(
                    R.plurals.print_settings_title, numActivePrintJobs, numActivePrintJobs));
        }
    }

    /**
     * A factory for {@link PrintSummaryProvider providers} the settings app can use to read the
     * print summary.
     */
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new PrintSummaryProvider(activity, summaryLoader);
        }
    };

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

            // Indexing all services, regardless if enabled. Please note that the index will not be
            // updated until this function is called again
            List<PrintServiceInfo> services =
                    printManager.getPrintServices(PrintManager.ALL_SERVICES);

            if (services != null) {
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
