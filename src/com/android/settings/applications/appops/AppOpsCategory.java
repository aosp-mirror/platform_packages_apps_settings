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

package com.android.settings.applications.appops;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.applications.appops.AppOpsState.AppOpEntry;

import java.util.List;

public class AppOpsCategory extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<AppOpEntry>> {

    AppOpsState mState;

    // This is the Adapter being used to display the list's data.
    AppListAdapter mAdapter;

    public AppOpsCategory() {
    }

    public AppOpsCategory(AppOpsState.OpsTemplate template) {
        Bundle args = new Bundle();
        args.putParcelable("template", template);
        setArguments(args);
    }

    /**
     * Helper for determining if the configuration has changed in an interesting
     * way so we need to rebuild the app list.
     */
    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged || (configChanges&(ActivityInfo.CONFIG_LOCALE
                    |ActivityInfo.CONFIG_UI_MODE|ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                return true;
            }
            return false;
        }
    }

    /**
     * Helper class to look for interesting changes to the installed apps
     * so that the loader can be updated.
     */
    public static class PackageIntentReceiver extends BroadcastReceiver {
        final AppListLoader mLoader;

        public PackageIntentReceiver(AppListLoader loader) {
            mLoader = loader;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mLoader.getContext().registerReceiver(this, filter);
            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            mLoader.getContext().registerReceiver(this, sdFilter);
        }

        @Override public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            mLoader.onContentChanged();
        }
    }

    /**
     * A custom Loader that loads all of the installed applications.
     */
    public static class AppListLoader extends AsyncTaskLoader<List<AppOpEntry>> {
        final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
        final AppOpsState mState;
        final AppOpsState.OpsTemplate mTemplate;

        List<AppOpEntry> mApps;
        PackageIntentReceiver mPackageObserver;

        public AppListLoader(Context context, AppOpsState state, AppOpsState.OpsTemplate template) {
            super(context);
            mState = state;
            mTemplate = template;
        }

        @Override public List<AppOpEntry> loadInBackground() {
            return mState.buildState(mTemplate, 0, null, AppOpsState.LABEL_COMPARATOR);
        }

        /**
         * Called when there is new data to deliver to the client.  The
         * super class will take care of delivering it; the implementation
         * here just adds a little more logic.
         */
        @Override public void deliverResult(List<AppOpEntry> apps) {
            if (isReset()) {
                // An async query came in while the loader is stopped.  We
                // don't need the result.
                if (apps != null) {
                    onReleaseResources(apps);
                }
            }
            List<AppOpEntry> oldApps = apps;
            mApps = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override protected void onStartLoading() {
            // We don't monitor changed when loading is stopped, so need
            // to always reload at this point.
            onContentChanged();

            if (mApps != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mApps);
            }

            // Start watching for changes in the app data.
            if (mPackageObserver == null) {
                mPackageObserver = new PackageIntentReceiver(this);
            }

            // Has something interesting in the configuration changed since we
            // last built the app list?
            boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

            if (takeContentChanged() || mApps == null || configChange) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override public void onCanceled(List<AppOpEntry> apps) {
            super.onCanceled(apps);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mApps != null) {
                onReleaseResources(mApps);
                mApps = null;
            }

            // Stop monitoring for changes.
            if (mPackageObserver != null) {
                getContext().unregisterReceiver(mPackageObserver);
                mPackageObserver = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated
         * with an actively loaded data set.
         */
        protected void onReleaseResources(List<AppOpEntry> apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }

    public static class AppListAdapter extends BaseAdapter {
        private final Resources mResources;
        private final LayoutInflater mInflater;

        List<AppOpEntry> mList;

        public AppListAdapter(Context context) {
            mResources = context.getResources();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<AppOpEntry> data) {
            mList = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList != null ? mList.size() : 0;
        }

        @Override
        public AppOpEntry getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Populate new items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.app_ops_item, parent, false);
            } else {
                view = convertView;
            }

            AppOpEntry item = getItem(position);
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(
                    item.getAppEntry().getIcon());
            ((TextView) view.findViewById(R.id.app_name)).setText(item.getAppEntry().getLabel());
            ((TextView) view.findViewById(R.id.op_name)).setText(
                    item.getTimeText(mResources, false));
            view.findViewById(R.id.op_time).setVisibility(View.GONE);
            ((CompoundButton) view.findViewById(R.id.op_switch)).setChecked(
                    item.getPrimaryOpMode() == AppOpsManager.MODE_ALLOWED);

            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mState = new AppOpsState(getActivity());
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data.  In a real
        // application this would come from a resource.
        setEmptyText("No applications");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new AppListAdapter(getActivity());
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        AppOpEntry entry = mAdapter.getItem(position);
        if (entry != null) {
            // We treat this as tapping on the check box, toggling the app op state.
            CompoundButton sw = v.findViewById(R.id.op_switch);
            boolean checked = !sw.isChecked();
            sw.setChecked(checked);
            AppOpsManager.OpEntry op = entry.getOpEntry(0);
            int mode = checked ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED;
            mState.getAppOpsManager().setMode(op.getOp(),
                    entry.getAppEntry().getApplicationInfo().uid,
                    entry.getAppEntry().getApplicationInfo().packageName,
                    mode);
            entry.overridePrimaryOpMode(mode);
        }
    }

    @Override public Loader<List<AppOpEntry>> onCreateLoader(int id, Bundle args) {
        Bundle fargs = getArguments();
        AppOpsState.OpsTemplate template = null;
        if (fargs != null) {
            template = fargs.getParcelable("template");
        }
        return new AppListLoader(getActivity(), mState, template);
    }

    @Override public void onLoadFinished(Loader<List<AppOpEntry>> loader, List<AppOpEntry> data) {
        // Set the new data in the adapter.
        mAdapter.setData(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override public void onLoaderReset(Loader<List<AppOpEntry>> loader) {
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }
}
