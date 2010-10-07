package com.android.settings.applications;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;

import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Keeps track of information about all installed applications, lazy-loading
 * as needed.
 */
public class ApplicationsState {
    static final String TAG = "ApplicationsState";
    static final boolean DEBUG = false;

    public static interface Callbacks {
        public void onRunningStateChanged(boolean running);
        public void onPackageListChanged();
        public void onRebuildComplete(ArrayList<AppEntry> apps);
        public void onPackageIconChanged();
        public void onPackageSizeChanged(String packageName);
        public void onAllSizesComputed();
    }

    public static interface AppFilter {
        public void init();
        public boolean filterApp(ApplicationInfo info);
    }

    static final int SIZE_UNKNOWN = -1;
    static final int SIZE_INVALID = -2;

    static final Pattern REMOVE_DIACRITICALS_PATTERN
            = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalize(String str) {
        String tmp = Normalizer.normalize(str, Form.NFD);
        return REMOVE_DIACRITICALS_PATTERN.matcher(tmp)
                .replaceAll("").toLowerCase();
    }

    public static class AppEntry {
        final String label;
        final long id;
        long size;
        
        long cacheSize;
        long codeSize;
        long dataSize;

        String getNormalizedLabel() {
            if (normalizedLabel != null) {
                return normalizedLabel;
            }
            normalizedLabel = normalize(label);
            return normalizedLabel;
        }

        // Need to synchronize on 'this' for the following.
        ApplicationInfo info;
        Drawable icon;
        String sizeStr;
        boolean sizeStale;
        long sizeLoadStart;

        String normalizedLabel;

        AppEntry(Context context, ApplicationInfo info, long id) {
            CharSequence label = info.loadLabel(context.getPackageManager());
            this.label = label != null ? label.toString() : info.packageName;
            this.id = id;
            this.info = info;
            this.size = SIZE_UNKNOWN;
            this.sizeStale = true;
        }
    }

    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            return sCollator.compare(object1.label, object2.label);
        }
    };

    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.size < object2.size) return 1;
            if (object1.size > object2.size) return -1;
            return sCollator.compare(object1.label, object2.label);
        }
    };

    public static final AppFilter THIRD_PARTY_FILTER = new AppFilter() {
        public void init() {
        }
        
        @Override
        public boolean filterApp(ApplicationInfo info) {
            if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return true;
            } else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return true;
            }
            return false;
        }
    };

    public static final AppFilter ON_SD_CARD_FILTER = new AppFilter() {
        final CanBeOnSdCardChecker mCanBeOnSdCardChecker
                = new CanBeOnSdCardChecker();
        
        public void init() {
            mCanBeOnSdCardChecker.init();
        }
        
        @Override
        public boolean filterApp(ApplicationInfo info) {
            return mCanBeOnSdCardChecker.check(info);
        }
    };

    final Context mContext;
    final PackageManager mPm;
    PackageIntentReceiver mPackageIntentReceiver;

    boolean mResumed;
    Callbacks mCurCallbacks;

    // Information about all applications.  Synchronize on mAppEntries
    // to protect access to these.
    final HashMap<String, AppEntry> mEntriesMap = new HashMap<String, AppEntry>();
    final ArrayList<AppEntry> mAppEntries = new ArrayList<AppEntry>();
    List<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
    long mCurId = 1;
    String mCurComputingSizePkg;

    // Rebuilding of app list.  Synchronized on mRebuildSync.
    final Object mRebuildSync = new Object();
    boolean mRebuildRequested;
    boolean mRebuildAsync;
    AppFilter mRebuildFilter;
    Comparator<AppEntry> mRebuildComparator;
    ArrayList<AppEntry> mRebuildResult;

    /**
     * Receives notifications when applications are added/removed.
     */
    private class PackageIntentReceiver extends BroadcastReceiver {
         void registerReceiver() {
             IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
             filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
             filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
             filter.addDataScheme("package");
             mContext.registerReceiver(this, filter);
             // Register for events related to sdcard installation.
             IntentFilter sdFilter = new IntentFilter();
             sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
             sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
             mContext.registerReceiver(this, sdFilter);
         }
         @Override
         public void onReceive(Context context, Intent intent) {
             String actionStr = intent.getAction();
             if (Intent.ACTION_PACKAGE_ADDED.equals(actionStr)) {
                 Uri data = intent.getData();
                 String pkgName = data.getEncodedSchemeSpecificPart();
                 addPackage(pkgName);
             } else if (Intent.ACTION_PACKAGE_REMOVED.equals(actionStr)) {
                 Uri data = intent.getData();
                 String pkgName = data.getEncodedSchemeSpecificPart();
                 removePackage(pkgName);
             } else if (Intent.ACTION_PACKAGE_CHANGED.equals(actionStr)) {
                 Uri data = intent.getData();
                 String pkgName = data.getEncodedSchemeSpecificPart();
                 removePackage(pkgName);
                 addPackage(pkgName);
             } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(actionStr) ||
                     Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(actionStr)) {
                 // When applications become available or unavailable (perhaps because
                 // the SD card was inserted or ejected) we need to refresh the
                 // AppInfo with new label, icon and size information as appropriate
                 // given the newfound (un)availability of the application.
                 // A simple way to do that is to treat the refresh as a package
                 // removal followed by a package addition.
                 String pkgList[] = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                 if (pkgList == null || pkgList.length == 0) {
                     // Ignore
                     return;
                 }
                 boolean avail = Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(actionStr);
                 for (String pkgName : pkgList) {
                     if (avail) addPackage(pkgName);
                     else removePackage(pkgName);
                 }
             }
         }
    }

    class MainHandler extends Handler {
        static final int MSG_REBUILD_COMPLETE = 1;
        static final int MSG_PACKAGE_LIST_CHANGED = 2;
        static final int MSG_PACKAGE_ICON_CHANGED = 3;
        static final int MSG_PACKAGE_SIZE_CHANGED = 4;
        static final int MSG_ALL_SIZES_COMPUTED = 5;
        static final int MSG_RUNNING_STATE_CHANGED = 6;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_COMPLETE: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onRebuildComplete((ArrayList<AppEntry>)msg.obj);
                    }
                } break;
                case MSG_PACKAGE_LIST_CHANGED: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onPackageListChanged();
                    }
                } break;
                case MSG_PACKAGE_ICON_CHANGED: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onPackageIconChanged();
                    }
                } break;
                case MSG_PACKAGE_SIZE_CHANGED: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onPackageSizeChanged((String)msg.obj);
                    }
                } break;
                case MSG_ALL_SIZES_COMPUTED: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onAllSizesComputed();
                    }
                } break;
                case MSG_RUNNING_STATE_CHANGED: {
                    if (mCurCallbacks != null) {
                        mCurCallbacks.onRunningStateChanged(msg.arg1 != 0);
                    }
                } break;
            }
        }
    }

    final MainHandler mMainHandler = new MainHandler();

    // --------------------------------------------------------------

    static final Object sLock = new Object();
    static ApplicationsState sInstance;

    static ApplicationsState getInstance(Application app) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ApplicationsState(app);
            }
            return sInstance;
        }
    }

    private ApplicationsState(Application app) {
        mContext = app;
        mPm = mContext.getPackageManager();
        mThread = new HandlerThread("ApplicationsState.Loader",
                Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mBackgroundHandler = new BackgroundHandler(mThread.getLooper());
    }

    void resume(Callbacks callbacks) {
        synchronized (mEntriesMap) {
            mCurCallbacks = callbacks;
            mResumed = true;
            if (mPackageIntentReceiver == null) {
                mPackageIntentReceiver = new PackageIntentReceiver();
                mPackageIntentReceiver.registerReceiver();
            }
            mApplications = mPm.getInstalledApplications(
                    PackageManager.GET_UNINSTALLED_PACKAGES |
                    PackageManager.GET_DISABLED_COMPONENTS);
            if (mApplications == null) {
                mApplications = new ArrayList<ApplicationInfo>();
            }
            for (int i=0; i<mAppEntries.size(); i++) {
                mAppEntries.get(i).sizeStale = true;
            }
            mCurComputingSizePkg = null;
            if (!mBackgroundHandler.hasMessages(BackgroundHandler.MSG_LOAD_ENTRIES)) {
                mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ENTRIES);
            }
        }
    }

    void pause() {
        synchronized (mEntriesMap) {
            mCurCallbacks = null;
            mResumed = false;
        }
    }

    // Creates a new list of app entries with the given filter and comparator.
    ArrayList<AppEntry> rebuild(AppFilter filter, Comparator<AppEntry> comparator) {
        synchronized (mRebuildSync) {
            mRebuildRequested = true;
            mRebuildAsync = false;
            mRebuildFilter = filter;
            mRebuildComparator = comparator;
            mRebuildResult = null;
            if (!mBackgroundHandler.hasMessages(BackgroundHandler.MSG_REBUILD_LIST)) {
                mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_REBUILD_LIST);
            }

            // We will wait for .25s for the list to be built.
            long waitend = SystemClock.uptimeMillis()+250;

            while (mRebuildResult == null) {
                long now = SystemClock.uptimeMillis();
                if (now >= waitend) {
                    break;
                }
                try {
                    mRebuildSync.wait(waitend - now);
                } catch (InterruptedException e) {
                }
            }

            mRebuildAsync = true;

            return mRebuildResult;
        }
    }

    void handleRebuildList() {
        AppFilter filter;
        Comparator<AppEntry> comparator;
        synchronized (mRebuildSync) {
            if (!mRebuildRequested) {
                return;
            }

            filter = mRebuildFilter;
            comparator = mRebuildComparator;
            mRebuildRequested = false;
            mRebuildFilter = null;
            mRebuildComparator = null;
        }

        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

        if (filter != null) {
            filter.init();
        }
        
        List<ApplicationInfo> apps;
        synchronized (mEntriesMap) {
            apps = new ArrayList<ApplicationInfo>(mApplications);
        }

        ArrayList<AppEntry> filteredApps = new ArrayList<AppEntry>();
        if (DEBUG) Log.i(TAG, "Rebuilding...");
        for (int i=0; i<apps.size(); i++) {
            ApplicationInfo info = apps.get(i);
            if (filter == null || filter.filterApp(info)) {
                synchronized (mEntriesMap) {
                    AppEntry entry = getEntryLocked(info);
                    if (DEBUG) Log.i(TAG, "Using " + info.packageName + ": " + entry);
                    filteredApps.add(entry);
                }
            }
        }

        Collections.sort(filteredApps, comparator);

        synchronized (mRebuildSync) {
            if (!mRebuildRequested) {
                if (!mRebuildAsync) {
                    mRebuildResult = filteredApps;
                    mRebuildSync.notifyAll();
                } else {
                    if (!mMainHandler.hasMessages(MainHandler.MSG_REBUILD_COMPLETE)) {
                        Message msg = mMainHandler.obtainMessage(
                                MainHandler.MSG_REBUILD_COMPLETE, filteredApps);
                        mMainHandler.sendMessage(msg);
                    }
                }
            }
        }

        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    AppEntry getEntry(String packageName) {
        synchronized (mEntriesMap) {
            AppEntry entry = mEntriesMap.get(packageName);
            if (entry == null) {
                for (int i=0; i<mApplications.size(); i++) {
                    ApplicationInfo info = mApplications.get(i);
                    if (packageName.equals(info.packageName)) {
                        entry = getEntryLocked(info);
                        break;
                    }
                }
            }
            return entry;
        }
    }
    
    void ensureIcon(AppEntry entry) {
        if (entry.icon != null) {
            return;
        }
        synchronized (entry) {
            if (entry.icon == null) {
                entry.icon = entry.info.loadIcon(mPm);
            }
        }
    }

    void requestSize(String packageName) {
        synchronized (mEntriesMap) {
            AppEntry entry = mEntriesMap.get(packageName);
            if (entry != null) {
                mPm.getPackageSizeInfo(packageName, mBackgroundHandler.mStatsObserver);
            }
        }
    }

    int indexOfApplicationInfoLocked(String pkgName) {
        for (int i=mApplications.size()-1; i>=0; i--) {
            if (mApplications.get(i).packageName.equals(pkgName)) {
                return i;
            }
        }
        return -1;
    }

    void addPackage(String pkgName) {
        try {
            synchronized (mEntriesMap) {
                if (DEBUG) Log.i(TAG, "Adding package " + pkgName);
                if (!mResumed) {
                    // If we are not resumed, we will do a full query the
                    // next time we resume, so there is no reason to do work
                    // here.
                    return;
                }
                if (indexOfApplicationInfoLocked(pkgName) >= 0) {
                    if (DEBUG) Log.i(TAG, "Package already exists!");
                    return;
                }
                ApplicationInfo info = mPm.getApplicationInfo(pkgName,
                        PackageManager.GET_UNINSTALLED_PACKAGES |
                        PackageManager.GET_DISABLED_COMPONENTS);
                mApplications.add(info);
                if (!mBackgroundHandler.hasMessages(BackgroundHandler.MSG_LOAD_ENTRIES)) {
                    mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ENTRIES);
                }
                if (!mMainHandler.hasMessages(MainHandler.MSG_PACKAGE_LIST_CHANGED)) {
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_PACKAGE_LIST_CHANGED);
                }
            }
        } catch (NameNotFoundException e) {
        }
    }

    void removePackage(String pkgName) {
        synchronized (mEntriesMap) {
            int idx = indexOfApplicationInfoLocked(pkgName);
            if (DEBUG) Log.i(TAG, "removePackage: " + pkgName + " @ " + idx);
            if (idx >= 0) {
                AppEntry entry = mEntriesMap.get(pkgName);
                if (DEBUG) Log.i(TAG, "removePackage: " + entry);
                if (entry != null) {
                    mEntriesMap.remove(pkgName);
                    mAppEntries.remove(entry);
                }
                mApplications.remove(idx);
                if (!mMainHandler.hasMessages(MainHandler.MSG_PACKAGE_LIST_CHANGED)) {
                    mMainHandler.sendEmptyMessage(MainHandler.MSG_PACKAGE_LIST_CHANGED);
                }
            }
        }
    }

    AppEntry getEntryLocked(ApplicationInfo info) {
        AppEntry entry = mEntriesMap.get(info.packageName);
        if (DEBUG) Log.i(TAG, "Looking up entry of pkg " + info.packageName + ": " + entry);
        if (entry == null) {
            if (DEBUG) Log.i(TAG, "Creating AppEntry for " + info.packageName);
            entry = new AppEntry(mContext, info, mCurId++);
            mEntriesMap.put(info.packageName, entry);
            mAppEntries.add(entry);
        } else if (entry.info != info) {
            entry.info = info;
        }
        return entry;
    }

    // --------------------------------------------------------------

    private long getTotalSize(PackageStats ps) {
        if (ps != null) {
            return ps.cacheSize+ps.codeSize+ps.dataSize;
        }
        return SIZE_INVALID;
    }

    private String getSizeStr(long size) {
        if (size >= 0) {
            return Formatter.formatFileSize(mContext, size);
        }
        return null;
    }

    final HandlerThread mThread;
    final BackgroundHandler mBackgroundHandler;
    class BackgroundHandler extends Handler {
        static final int MSG_REBUILD_LIST = 1;
        static final int MSG_LOAD_ENTRIES = 2;
        static final int MSG_LOAD_ICONS = 3;
        static final int MSG_LOAD_SIZES = 4;

        boolean mRunning;

        final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                boolean sizeChanged = false;
                synchronized (mEntriesMap) {
                    AppEntry entry = mEntriesMap.get(stats.packageName);
                    if (entry != null) {
                        synchronized (entry) {
                            entry.sizeStale = false;
                            entry.sizeLoadStart = 0;
                            long newSize = getTotalSize(stats);
                            if (entry.size != newSize ||
                                    entry.cacheSize != stats.cacheSize ||
                                    entry.codeSize != stats.codeSize ||
                                    entry.dataSize != stats.dataSize) {
                                entry.size = newSize;
                                entry.cacheSize = stats.cacheSize;
                                entry.codeSize = stats.codeSize;
                                entry.dataSize = stats.dataSize;
                                entry.sizeStr = getSizeStr(entry.size);
                                if (DEBUG) Log.i(TAG, "Set size of " + entry.label + " " + entry
                                        + ": " + entry.sizeStr);
                                sizeChanged = true;
                            }
                        }
                        if (sizeChanged) {
                            Message msg = mMainHandler.obtainMessage(
                                    MainHandler.MSG_PACKAGE_SIZE_CHANGED, stats.packageName);
                            mMainHandler.sendMessage(msg);
                        }
                    }
                    if (mCurComputingSizePkg == null
                            || mCurComputingSizePkg.equals(stats.packageName)) {
                        mCurComputingSizePkg = null;
                        sendEmptyMessage(MSG_LOAD_SIZES);
                    }
                }
            }
        };

        BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Always try rebuilding list first thing, if needed.
            handleRebuildList();

            switch (msg.what) {
                case MSG_REBUILD_LIST: {
                } break;
                case MSG_LOAD_ENTRIES: {
                    int numDone = 0;
                    synchronized (mEntriesMap) {
                        for (int i=0; i<mApplications.size() && numDone<6; i++) {
                            if (!mRunning) {
                                mRunning = true;
                                Message m = mMainHandler.obtainMessage(
                                        MainHandler.MSG_RUNNING_STATE_CHANGED, 1);
                                mMainHandler.sendMessage(m);
                            }
                            ApplicationInfo info = mApplications.get(i);
                            if (mEntriesMap.get(info.packageName) == null) {
                                numDone++;
                                getEntryLocked(info);
                            }
                        }
                    }

                    if (numDone >= 6) {
                        sendEmptyMessage(MSG_LOAD_ENTRIES);
                    } else {
                        sendEmptyMessage(MSG_LOAD_ICONS);
                    }
                } break;
                case MSG_LOAD_ICONS: {
                    int numDone = 0;
                    synchronized (mEntriesMap) {
                        for (int i=0; i<mAppEntries.size() && numDone<2; i++) {
                            AppEntry entry = mAppEntries.get(i);
                            if (entry.icon == null) {
                                if (!mRunning) {
                                    mRunning = true;
                                    Message m = mMainHandler.obtainMessage(
                                            MainHandler.MSG_RUNNING_STATE_CHANGED, 1);
                                    mMainHandler.sendMessage(m);
                                }
                                numDone++;
                                synchronized (entry) {
                                    entry.icon = entry.info.loadIcon(mPm);
                                }
                            }
                        }
                    }
                    if (numDone > 0) {
                        if (!mMainHandler.hasMessages(MainHandler.MSG_PACKAGE_ICON_CHANGED)) {
                            mMainHandler.sendEmptyMessage(MainHandler.MSG_PACKAGE_ICON_CHANGED);
                        }
                    }
                    if (numDone >= 2) {
                        sendEmptyMessage(MSG_LOAD_ICONS);
                    } else {
                        sendEmptyMessage(MSG_LOAD_SIZES);
                    }
                } break;
                case MSG_LOAD_SIZES: {
                    synchronized (mEntriesMap) {
                        if (mCurComputingSizePkg != null) {
                            return;
                        }

                        long now = SystemClock.uptimeMillis();
                        for (int i=0; i<mAppEntries.size(); i++) {
                            AppEntry entry = mAppEntries.get(i);
                            if (entry.size == SIZE_UNKNOWN || entry.sizeStale) {
                                if (entry.sizeLoadStart == 0 ||
                                        (entry.sizeLoadStart < (now-20*1000))) {
                                    if (!mRunning) {
                                        mRunning = true;
                                        Message m = mMainHandler.obtainMessage(
                                                MainHandler.MSG_RUNNING_STATE_CHANGED, 1);
                                        mMainHandler.sendMessage(m);
                                    }
                                    entry.sizeLoadStart = now;
                                    mCurComputingSizePkg = entry.info.packageName;
                                    mPm.getPackageSizeInfo(mCurComputingSizePkg, mStatsObserver);
                                }
                                return;
                            }
                        }
                        if (!mMainHandler.hasMessages(MainHandler.MSG_ALL_SIZES_COMPUTED)) {
                            mMainHandler.sendEmptyMessage(MainHandler.MSG_ALL_SIZES_COMPUTED);
                            mRunning = false;
                            Message m = mMainHandler.obtainMessage(
                                    MainHandler.MSG_RUNNING_STATE_CHANGED, 0);
                            mMainHandler.sendMessage(m);
                        }
                    }
                } break;
            }
        }

    }
}
