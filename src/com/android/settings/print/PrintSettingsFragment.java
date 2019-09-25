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

import static com.android.settings.print.PrintSettingPreferenceController.shouldShowToUser;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment with the top level print settings.
 */
@SearchIndexable
public class PrintSettingsFragment extends ProfileSettingsPreferenceFragment
        implements Indexable, OnClickListener {
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
    public int getMetricsCategory() {
        return SettingsEnums.PRINT_SETTINGS;
    }

    @Override
    public int getHelpResource() {
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
    private final class PrintServicesController implements LoaderCallbacks<List<PrintServiceInfo>> {
        @Override
        public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
            PrintManager printManager =
                    (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
            if (printManager != null) {
                return new SettingsPrintServicesLoader(printManager, getContext(),
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
            final Context context = getPrefContext();
            if (context == null) {
                Log.w(TAG, "No preference context, skip adding print services");
                return;
            }

            for (PrintServiceInfo service : services) {
                AppPreference preference = new AppPreference(context);

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
        preference.setIcon(R.drawable.ic_add_24dp);
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
                final Context context = getPrefContext();
                if (context == null) {
                    Log.w(TAG, "No preference context, skip adding print jobs");
                    return;
                }

                for (PrintJobInfo printJob : printJobs) {
                    Preference preference = new Preference(context);

                    preference.setPersistent(false);
                    preference.setFragment(PrintJobSettingsFragment.class.getName());
                    preference.setKey(printJob.getId().flattenToString());

                    switch (printJob.getState()) {
                        case PrintJobInfo.STATE_QUEUED:
                        case PrintJobInfo.STATE_STARTED:
                            if (!printJob.isCancelling()) {
                                preference.setTitle(getString(
                                        R.string.print_printing_state_title_template,
                                        printJob.getLabel()));
                            } else {
                                preference.setTitle(getString(
                                        R.string.print_cancelling_state_title_template,
                                        printJob.getLabel()));
                            }
                            break;
                        case PrintJobInfo.STATE_FAILED:
                            preference.setTitle(getString(
                                    R.string.print_failed_state_title_template,
                                    printJob.getLabel()));
                            break;
                        case PrintJobInfo.STATE_BLOCKED:
                            if (!printJob.isCancelling()) {
                                preference.setTitle(getString(
                                        R.string.print_blocked_state_title_template,
                                        printJob.getLabel()));
                            } else {
                                preference.setTitle(getString(
                                        R.string.print_cancelling_state_title_template,
                                        printJob.getLabel()));
                            }
                            break;
                    }

                    preference.setSummary(getString(R.string.print_job_summary,
                            printJob.getPrinterName(), DateUtils.formatSameDayTime(
                                    printJob.getCreationTime(), printJob.getCreationTime(),
                                    DateFormat.SHORT, DateFormat.SHORT)));

                    TypedArray a = getActivity().obtainStyledAttributes(new int[]{
                            android.R.attr.colorControlNormal});
                    int tintColor = a.getColor(0, 0);
                    a.recycle();

                    switch (printJob.getState()) {
                        case PrintJobInfo.STATE_QUEUED:
                        case PrintJobInfo.STATE_STARTED: {
                            Drawable icon = getActivity().getDrawable(
                                    com.android.internal.R.drawable.ic_print);
                            icon.setTint(tintColor);
                            preference.setIcon(icon);
                            break;
                        }

                        case PrintJobInfo.STATE_FAILED:
                        case PrintJobInfo.STATE_BLOCKED: {
                            Drawable icon = getActivity().getDrawable(
                                    com.android.internal.R.drawable.ic_print_error);
                            icon.setTint(tintColor);
                            preference.setIcon(icon);
                            break;
                        }
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
                        printJobInfos = new ArrayList<>();
                    }
                    printJobInfos.add(printJob);
                }
            }
            return printJobInfos;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.print_settings);
}
