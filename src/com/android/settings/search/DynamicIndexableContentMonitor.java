/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.provider.UserDictionary;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.content.PackageMonitor;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.VirtualKeyboardFragment;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.print.PrintSettingsFragment;

import java.util.ArrayList;
import java.util.List;

public final class DynamicIndexableContentMonitor implements
        LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
    // Shorten the class name because log TAG can be at most 23 chars.
    private static final String TAG = "DynamicContentMonitor";

    private static final long DELAY_PROCESS_PACKAGE_CHANGE = 2000;
    // A PackageMonitor shared among Settings activities.
    private static final PackageChangeMonitor PACKAGE_CHANGE_MONITOR = new PackageChangeMonitor();

    // Null if not initialized.
    @Nullable private DatabaseIndexingManager mIndexManager;
    private Context mContext;
    private boolean mHasFeaturePrinting;

    @VisibleForTesting
    static Intent getAccessibilityServiceIntent(String packageName) {
        final Intent intent = new Intent(AccessibilityService.SERVICE_INTERFACE);
        intent.setPackage(packageName);
        return intent;
    }

    @VisibleForTesting
    static Intent getIMEServiceIntent(String packageName) {
        final Intent intent = new Intent(InputMethod.SERVICE_INTERFACE);
        intent.setPackage(packageName);
        return intent;
    }

    @VisibleForTesting
    static void resetForTesting() {
        InputDevicesMonitor.getInstance().resetForTesting();
        AccessibilityServicesMonitor.getInstance().resetForTesting();
        InputMethodServicesMonitor.getInstance().resetForTesting();
    }

    /**
     * This instance holds a set of content monitor singleton objects.
     *
     * This object is created every time a sub-settings that extends {@code SettingsActivity}
     * is created.
     */
    public DynamicIndexableContentMonitor() {}

    /**
     * Creates and initializes a set of content monitor singleton objects if not yet exist.
     * Also starts loading the list of print services.
     * <code>mIndex</code> has non-null value after successfully initialized.
     *
     * @param activity used to get {@link LoaderManager}.
     * @param loaderId id for loading print services.
     */
    public void register(Activity activity, int loaderId) {
        final boolean isUserUnlocked = activity
                .getSystemService(UserManager.class)
                .isUserUnlocked();
        register(activity, loaderId, FeatureFactory.getFactory(activity)
                        .getSearchFeatureProvider().getIndexingManager(activity), isUserUnlocked);
    }

    /**
     * For testing to inject {@link DatabaseIndexingManager} object.
     * Also because currently Robolectric doesn't support API 24, we can not test code that calls
     * {@link UserManager#isUserUnlocked()}.
     */
    @VisibleForTesting
    void register(Activity activity, int loaderId, DatabaseIndexingManager indexManager,
                  boolean isUserUnlocked) {
        if (!isUserUnlocked) {
            Log.w(TAG, "Skipping content monitoring because user is locked");
            return;
        }
        final Context context = activity.getApplicationContext();
        mContext = context;
        mIndexManager = indexManager;

        PACKAGE_CHANGE_MONITOR.registerMonitor(context);
        mHasFeaturePrinting = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_PRINTING);
        if (mHasFeaturePrinting) {
            activity.getLoaderManager().initLoader(loaderId, null /* args */, this /* callbacks */);
        }

        // Watch for input device changes.
        InputDevicesMonitor.getInstance().initialize(context, mIndexManager);

        // Start tracking packages.
        AccessibilityServicesMonitor.getInstance().initialize(context, mIndexManager);
        InputMethodServicesMonitor.getInstance().initialize(context, mIndexManager);
    }

    /**
     * Aborts loading the list of print services.
     * Note that a set of content monitor singletons keep alive while Settings app is running.
     *
     * @param activity user to get {@link LoaderManager}.
     * @param loaderId id for loading print services.
     */
    public void unregister(Activity activity, int loaderId) {
        if (mIndexManager == null) return;

        PACKAGE_CHANGE_MONITOR.unregisterMonitor();
        if (mHasFeaturePrinting) {
            activity.getLoaderManager().destroyLoader(loaderId);
        }
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
        return new PrintServicesLoader(
                (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE), mContext,
                PrintManager.ALL_SERVICES);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader,
            List<PrintServiceInfo> services) {
        mIndexManager.updateFromClassNameResource(PrintSettingsFragment.class.getName(),
                true /* includeInSearchResults */);
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        // nothing to do
    }

    // A singleton that monitors input devices changes and updates indexes of physical keyboards.
    private static class InputDevicesMonitor implements InputManager.InputDeviceListener {

        // Null if not initialized.
        @Nullable private DatabaseIndexingManager mIndexManager;
        private InputManager mInputManager;

        private InputDevicesMonitor() {}

        private static class SingletonHolder {
            private static final InputDevicesMonitor INSTANCE = new InputDevicesMonitor();
        }

        static InputDevicesMonitor getInstance() {
            return SingletonHolder.INSTANCE;
        }

        @VisibleForTesting
        synchronized void resetForTesting() {
            if (mIndexManager != null) {
                mInputManager.unregisterInputDeviceListener(this /* listener */);
            }
            mIndexManager = null;
        }

        synchronized void initialize(Context context, DatabaseIndexingManager indexManager) {
            if (mIndexManager != null) return;
            mIndexManager = indexManager;
            mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            buildIndex();

            // Watch for input device changes.
            mInputManager.registerInputDeviceListener(this /* listener */, null /* handler */);
        }

        private void buildIndex() {
            mIndexManager.updateFromClassNameResource(PhysicalKeyboardFragment.class.getName(),
                    true /* includeInSearchResults */);
        }

        @Override
        public void onInputDeviceAdded(int deviceId) {
            buildIndex();
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            buildIndex();
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            buildIndex();
        }
    }

    // A singleton that monitors package installing, uninstalling, enabling, and disabling.
    // Then updates indexes of accessibility services and input methods.
    private static class PackageChangeMonitor extends PackageMonitor {
        private static final String TAG = PackageChangeMonitor.class.getSimpleName();

        // Null if not initialized. Guarded by {@link #mLock}.
        @Nullable private PackageManager mPackageManager;
        private final Object mLock = new Object();

        public void registerMonitor(Context context) {
            synchronized (mLock) {
                if (mPackageManager != null) {
                    return;
                }
                mPackageManager = context.getPackageManager();

                // Start tracking packages. Use background thread for monitoring. Note that no need
                // to unregister this monitor. This should be alive while Settings app is running.
                super.register(context, null /* thread */, UserHandle.CURRENT, false);
            }
        }

        public void unregisterMonitor() {
            synchronized (mLock) {
                if (mPackageManager == null) {
                    return;
                }
                super.unregister();
                mPackageManager = null;
            }
        }

        // Covers installed, appeared external storage with the package, upgraded.
        @Override
        public void onPackageAppeared(String packageName, int reason) {
            postPackageAvailable(packageName);
        }

        // Covers uninstalled, removed external storage with the package.
        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            postPackageUnavailable(packageName);
        }

        // Covers enabled, disabled.
        @Override
        public void onPackageModified(String packageName) {
            try {
                final int state = mPackageManager.getApplicationEnabledSetting(packageName);
                if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    postPackageAvailable(packageName);
                } else {
                    postPackageUnavailable(packageName);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Package does not exist: " + packageName, e);
            }
        }

        private void postPackageAvailable(final String packageName) {
            getRegisteredHandler().postDelayed(() -> {
                AccessibilityServicesMonitor.getInstance().onPackageAvailable(packageName);
                InputMethodServicesMonitor.getInstance().onPackageAvailable(packageName);
            }, DELAY_PROCESS_PACKAGE_CHANGE);
        }

        private void postPackageUnavailable(final String packageName) {
            getRegisteredHandler().postDelayed(() -> {
                AccessibilityServicesMonitor.getInstance().onPackageUnavailable(packageName);
                InputMethodServicesMonitor.getInstance().onPackageUnavailable(packageName);
            }, DELAY_PROCESS_PACKAGE_CHANGE);
        }
    }

    // A singleton that holds list of available accessibility services and updates search index.
    private static class AccessibilityServicesMonitor {

        // Null if not initialized.
        @Nullable private DatabaseIndexingManager mIndexManager;
        private PackageManager mPackageManager;
        private final List<String> mAccessibilityServices = new ArrayList<>();

        private AccessibilityServicesMonitor() {}

        private static class SingletonHolder {
            private static final AccessibilityServicesMonitor INSTANCE =
                    new AccessibilityServicesMonitor();
        }

        static AccessibilityServicesMonitor getInstance() {
            return SingletonHolder.INSTANCE;
        }

        @VisibleForTesting
        synchronized void resetForTesting() {
            mIndexManager = null;
        }

        synchronized void initialize(Context context, DatabaseIndexingManager index) {
            if (mIndexManager != null) return;
            mIndexManager = index;
            mPackageManager = context.getPackageManager();
            mAccessibilityServices.clear();
            buildIndex();

            // Cache accessibility service packages to know when they go away.
            AccessibilityManager accessibilityManager = (AccessibilityManager) context
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);
            for (final AccessibilityServiceInfo accessibilityService
                    : accessibilityManager.getInstalledAccessibilityServiceList()) {
                ResolveInfo resolveInfo = accessibilityService.getResolveInfo();
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    mAccessibilityServices.add(resolveInfo.serviceInfo.packageName);
                }
            }
        }

        private void buildIndex() {
            mIndexManager.updateFromClassNameResource(AccessibilitySettings.class.getName(),
                    true /* includeInSearchResults */);
        }

        synchronized void onPackageAvailable(String packageName) {
            if (mIndexManager == null) return;
            if (mAccessibilityServices.contains(packageName)) return;

            final Intent intent = getAccessibilityServiceIntent(packageName);
            final List<ResolveInfo> services = mPackageManager
                    .queryIntentServices(intent, 0 /* flags */);
            if (services == null || services.isEmpty()) return;
            mAccessibilityServices.add(packageName);
            buildIndex();
        }

        synchronized void onPackageUnavailable(String packageName) {
            if (mIndexManager == null) return;
            if (!mAccessibilityServices.remove(packageName)) return;
            buildIndex();
        }
    }

    // A singleton that holds list of available input methods and updates search index.
    // Also it monitors user dictionary changes and updates search index.
    private static class InputMethodServicesMonitor extends ContentObserver {

        private static final Uri ENABLED_INPUT_METHODS_CONTENT_URI =
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS);

        // Null if not initialized.
        @Nullable private DatabaseIndexingManager mIndexManager;
        private PackageManager mPackageManager;
        private ContentResolver mContentResolver;
        private final List<String> mInputMethodServices = new ArrayList<>();

        private InputMethodServicesMonitor() {
            // No need for handler because {@link #onChange(boolean,Uri)} is short and quick.
            super(null /* handler */);
        }

        private static class SingletonHolder {
            private static final InputMethodServicesMonitor INSTANCE =
                    new InputMethodServicesMonitor();
        }

        static InputMethodServicesMonitor getInstance() {
            return SingletonHolder.INSTANCE;
        }

        @VisibleForTesting
        synchronized void resetForTesting() {
            if (mIndexManager != null) {
                mContentResolver.unregisterContentObserver(this /* observer */);
            }
            mIndexManager = null;
        }

        synchronized void initialize(Context context, DatabaseIndexingManager indexManager) {
            final boolean hasFeatureIme = context.getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS);
            if (!hasFeatureIme) return;

            if (mIndexManager != null) return;
            mIndexManager = indexManager;
            mPackageManager = context.getPackageManager();
            mContentResolver = context.getContentResolver();
            mInputMethodServices.clear();
            // Build index of {@link UserDictionary}.
            buildIndex(LanguageAndInputSettings.class);
            // Build index of IMEs.
            buildIndex(VirtualKeyboardFragment.class);
            buildIndex(AvailableVirtualKeyboardFragment.class);

            // Cache IME service packages to know when they go away.
            final InputMethodManager inputMethodManager = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            for (final InputMethodInfo inputMethod : inputMethodManager.getInputMethodList()) {
                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                if (serviceInfo != null) {
                    mInputMethodServices.add(serviceInfo.packageName);
                }
            }

            // TODO: Implements by JobScheduler with TriggerContentUri parameters.
            // Watch for related content URIs.
            mContentResolver.registerContentObserver(UserDictionary.Words.CONTENT_URI,
                    true /* notifyForDescendants */, this /* observer */);
            // Watch for changing enabled IMEs.
            mContentResolver.registerContentObserver(ENABLED_INPUT_METHODS_CONTENT_URI,
                    false /* notifyForDescendants */, this /* observer */);
        }

        private void buildIndex(Class<?> indexClass) {
            mIndexManager.updateFromClassNameResource(indexClass.getName(),
                    true /* includeInSearchResults */);
        }

        synchronized void onPackageAvailable(String packageName) {
            if (mIndexManager == null) return;
            if (mInputMethodServices.contains(packageName)) return;

            final Intent intent = getIMEServiceIntent(packageName);
            final List<ResolveInfo> services = mPackageManager
                    .queryIntentServices(intent, 0 /* flags */);
            if (services == null || services.isEmpty()) return;
            mInputMethodServices.add(packageName);
            buildIndex(VirtualKeyboardFragment.class);
            buildIndex(AvailableVirtualKeyboardFragment.class);
        }

        synchronized void onPackageUnavailable(String packageName) {
            if (mIndexManager == null) return;
            if (!mInputMethodServices.remove(packageName)) return;
            buildIndex(VirtualKeyboardFragment.class);
            buildIndex(AvailableVirtualKeyboardFragment.class);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (ENABLED_INPUT_METHODS_CONTENT_URI.equals(uri)) {
                buildIndex(VirtualKeyboardFragment.class);
                buildIndex(AvailableVirtualKeyboardFragment.class);
            } else if (UserDictionary.Words.CONTENT_URI.equals(uri)) {
                buildIndex(LanguageAndInputSettings.class);
            }
        }
    }
}
