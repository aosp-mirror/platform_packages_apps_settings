package com.android.settings.applications;

import android.app.AppOpsManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.AppOpsManager.OpEntry;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.android.settings.R;

public class AppOpsCategory extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<AppOpsCategory.AppOpEntry>> {

    // This is the Adapter being used to display the list's data.
    AppListAdapter mAdapter;

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final AppListLoader mLoader;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(AppListLoader loader, ApplicationInfo info) {
            mLoader = loader;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mLoader.getContext().getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }

        @Override public String toString() {
            return mLabel;
        }

        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label != null ? label.toString() : mInfo.packageName;
                }
            }
        }
    }

    public AppOpsCategory() {
    }

    public AppOpsCategory(int[] ops, String[] perms) {
        Bundle args = new Bundle();
        args.putIntArray("ops", ops);
        args.putStringArray("perms", perms);
        setArguments(args);
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppOpEntry {
        private final AppOpsManager.PackageOps mPkgOps;
        private final AppOpsManager.OpEntry mOp;
        private final AppEntry mApp;

        public AppOpEntry(AppOpsManager.PackageOps pkg, AppOpsManager.OpEntry op, AppEntry app) {
            mPkgOps = pkg;
            mOp = op;
            mApp = app;
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public AppOpsManager.PackageOps getPackageOps() {
            return mPkgOps;
        }

        public AppOpsManager.OpEntry getOpEntry() {
            return mOp;
        }

        public long getTime() {
            return mOp.getTime();
        }

        @Override public String toString() {
            return mApp.getLabel();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.getOpEntry().isRunning() != object2.getOpEntry().isRunning()) {
                // Currently running ops go first.
                return object1.getOpEntry().isRunning() ? -1 : 1;
            }
            if (object1.getTime() != object2.getTime()) {
                // More recent times go first.
                return object1.getTime() > object2.getTime() ? -1 : 1;
            }
            return sCollator.compare(object1.getAppEntry().getLabel(),
                    object2.getAppEntry().getLabel());
        }
    };

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
        final AppOpsManager mAppOps;
        final PackageManager mPm;
        final int[] mOps;
        final String[] mPerms;

        final HashMap<String, AppEntry> mAppEntries = new HashMap<String, AppEntry>();

        List<AppOpEntry> mApps;
        PackageIntentReceiver mPackageObserver;

        public AppListLoader(Context context, int[] ops, String[] perms) {
            super(context);
            mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
            mPm = context.getPackageManager();
            mOps = ops;
            mPerms = perms;
        }

        @Override public List<AppOpEntry> loadInBackground() {
            final Context context = getContext();

            List<AppOpsManager.PackageOps> pkgs = mAppOps.getPackagesForOps(mOps);
            List<AppOpEntry> entries = new ArrayList<AppOpEntry>(pkgs.size());
            for (int i=0; i<pkgs.size(); i++) {
                AppOpsManager.PackageOps pkgOps = pkgs.get(i);
                AppEntry appEntry = mAppEntries.get(pkgOps.getPackageName());
                if (appEntry == null) {
                    ApplicationInfo appInfo = null;
                    try {
                        appInfo = mPm.getApplicationInfo(pkgOps.getPackageName(),
                                PackageManager.GET_DISABLED_COMPONENTS
                                | PackageManager.GET_UNINSTALLED_PACKAGES);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    appEntry = new AppEntry(this, appInfo);
                    appEntry.loadLabel(context);
                    mAppEntries.put(pkgOps.getPackageName(), appEntry);
                }
                for (int j=0; j<pkgOps.getOps().size(); j++) {
                    AppOpsManager.OpEntry opEntry = pkgOps.getOps().get(j);
                    AppOpEntry entry = new AppOpEntry(pkgOps, opEntry, appEntry);
                    entries.add(entry);
                }
            }

            if (mPerms != null) {
                List<PackageInfo> apps = mPm.getPackagesHoldingPermissions(mPerms, 0);
                for (int i=0; i<apps.size(); i++) {
                    PackageInfo appInfo = apps.get(i);
                    AppEntry appEntry = mAppEntries.get(appInfo.packageName);
                    if (appEntry == null) {
                        appEntry = new AppEntry(this, appInfo.applicationInfo);
                        appEntry.loadLabel(context);
                        mAppEntries.put(appInfo.packageName, appEntry);
                        List<AppOpsManager.OpEntry> dummyOps = new ArrayList<AppOpsManager.OpEntry>();
                        AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(0, 0, 0);
                        dummyOps.add(opEntry);
                        AppOpsManager.PackageOps pkgOps = new AppOpsManager.PackageOps(
                                appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);
                        AppOpEntry entry = new AppOpEntry(pkgOps, opEntry, appEntry);
                        entries.add(entry);
                    }
                }
            }

            // Sort the list.
            Collections.sort(entries, APP_OP_COMPARATOR);

            // Done!
            return entries;
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

    public static class AppListAdapter extends ArrayAdapter<AppOpEntry> {
        private final LayoutInflater mInflater;
        private final CharSequence[] mOpNames;
        private final CharSequence mRunningStr;

        public AppListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mOpNames = context.getResources().getTextArray(R.array.app_ops_names);
            mRunningStr = context.getResources().getText(R.string.app_ops_running);
        }

        public void setData(List<AppOpEntry> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        CharSequence opTimeToString(AppOpsManager.OpEntry op) {
            if (op.isRunning()) {
                return mRunningStr;
            }
            return DateUtils.getRelativeTimeSpanString(op.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
        }

        /**
         * Populate new items in the list.
         */
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.app_ops_item, parent, false);
            } else {
                view = convertView;
            }

            AppOpEntry item = getItem(position);
            ((ImageView)view.findViewById(R.id.app_icon)).setImageDrawable(
                    item.getAppEntry().getIcon());
            ((TextView)view.findViewById(R.id.app_name)).setText(item.getAppEntry().getLabel());
            if (item.getOpEntry().getTime() != 0) {
                ((TextView)view.findViewById(R.id.op_name)).setText(
                        mOpNames[item.getOpEntry().getOp()]);
                ((TextView)view.findViewById(R.id.op_time)).setText(opTimeToString(item.getOpEntry()));
            } else {
                ((TextView)view.findViewById(R.id.op_name)).setText("");
                ((TextView)view.findViewById(R.id.op_time)).setText("");
            }

            return view;
        }
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

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("LoaderCustom", "Item clicked: " + id);
    }

    @Override public Loader<List<AppOpEntry>> onCreateLoader(int id, Bundle args) {
        Bundle fargs = getArguments();
        int[] ops = null;
        String[] perms = null;
        if (fargs != null) {
            ops = fargs.getIntArray("ops");
            perms = fargs.getStringArray("perms");
        }
        return new AppListLoader(getActivity(), ops, perms);
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
