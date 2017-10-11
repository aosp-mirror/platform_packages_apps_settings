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
 * limitations under the License
 */

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Application;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;
import android.provider.Settings;
import android.provider.UserDictionary;
import android.view.inputmethod.InputMethodInfo;

import com.android.internal.content.PackageMonitor;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.VirtualKeyboardFragment;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.search2.DatabaseIndexingManager;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.shadow.ShadowActivityWithLoadManager;
import com.android.settings.testutils.shadow.ShadowContextImplWithRegisterReceiver;
import com.android.settings.testutils.shadow.ShadowInputManager;
import com.android.settings.testutils.shadow.ShadowInputMethodManagerWithMethodList;
import com.android.settings.testutils.shadow.ShadowPackageMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowActivityWithLoadManager.class,
                ShadowContextImplWithRegisterReceiver.class,
                ShadowInputManager.class,
                ShadowInputMethodManagerWithMethodList.class,
                ShadowPackageMonitor.class,
        }
)
public class DynamicIndexableContentMonitorTest {

    private static final int LOADER_ID = 1234;
    private static final String A11Y_PACKAGE_1 = "a11y-1";
    private static final String A11Y_PACKAGE_2 = "a11y-2";
    private static final String IME_PACKAGE_1 = "ime-1";
    private static final String IME_PACKAGE_2 = "ime-2";

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private DatabaseIndexingManager mIndexManager;

    private Activity mActivity;
    private InputManager mInputManager;

    private ShadowContextImplWithRegisterReceiver mShadowContextImpl;
    private ShadowActivityWithLoadManager mShadowActivity;
    private ShadowAccessibilityManager mShadowAccessibilityManager;
    private ShadowInputMethodManagerWithMethodList mShadowInputMethodManager;
    private RobolectricPackageManager mRobolectricPackageManager;

    private final DynamicIndexableContentMonitor mMonitor = new DynamicIndexableContentMonitor();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mInputManager = InputManager.getInstance();

        // Robolectric shadows.
        mShadowContextImpl = (ShadowContextImplWithRegisterReceiver) ShadowExtractor.extract(
                ((Application) ShadowApplication.getInstance().getApplicationContext())
                .getBaseContext());
        mShadowActivity = (ShadowActivityWithLoadManager) ShadowExtractor.extract(mActivity);
        mShadowAccessibilityManager = (ShadowAccessibilityManager) ShadowExtractor.extract(
                mActivity.getSystemService(Context.ACCESSIBILITY_SERVICE));
        mShadowInputMethodManager = (ShadowInputMethodManagerWithMethodList) ShadowExtractor
                .extract(mActivity.getSystemService(Context.INPUT_METHOD_SERVICE));
        mRobolectricPackageManager = RuntimeEnvironment.getRobolectricPackageManager();

        // Setup shadows.
        mShadowContextImpl.setSystemService(Context.PRINT_SERVICE, mock(PrintManager.class));
        mShadowContextImpl.setSystemService(Context.INPUT_SERVICE, mInputManager);
        mShadowActivity.setLoaderManager(mLoaderManager);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(Collections.emptyList());
        mShadowInputMethodManager.setInputMethodList(Collections.emptyList());
        mRobolectricPackageManager.setSystemFeature(PackageManager.FEATURE_PRINTING, true);
        mRobolectricPackageManager.setSystemFeature(PackageManager.FEATURE_INPUT_METHODS, true);
    }

    @After
    public void shutDown() {
        mMonitor.unregister(mActivity, LOADER_ID);
        // BroadcastReceiver must be unregistered.
        assertThat(extractPackageMonitor()).isNull();

        DynamicIndexableContentMonitor.resetForTesting();
        mRobolectricPackageManager.reset();

        DatabaseTestUtils.clearDb();
    }

    @Test
    public void testLockedUser() {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, false /* isUserUnlocked */);

        // No loader procedure happens.
        verify(mLoaderManager, never()).initLoader(
                anyInt(), any(Bundle.class), any(LoaderManager.LoaderCallbacks.class));
        // No indexing happens.
        verify(mIndexManager, never()).updateFromClassNameResource(
                anyString(), anyBoolean());

        mMonitor.unregister(mActivity, LOADER_ID);

        // No destroy loader should happen.
        verify(mLoaderManager, never()).destroyLoader(anyInt());
    }

    @Test
    public void testWithNoPrintingFeature() {
        mRobolectricPackageManager.setSystemFeature(PackageManager.FEATURE_PRINTING, false);

        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        // No loader procedure happens.
        verify(mLoaderManager, never()).initLoader(
                anyInt(), any(Bundle.class), any(LoaderManager.LoaderCallbacks.class));
        verifyNoIndexing(PrintSettingsFragment.class);

        mMonitor.unregister(mActivity, LOADER_ID);

        // No destroy loader should happen.
        verify(mLoaderManager, never()).destroyLoader(anyInt());
        // BroadcastReceiver must be unregistered.
        assertThat(extractPackageMonitor()).isNull();

        // To suppress spurious test fail in {@link #shutDown()}.
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);
    }

    @Test
    public void testPrinterServiceIndex() {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        // Loader procedure happens.
        verify(mLoaderManager, only()).initLoader(LOADER_ID, null, mMonitor);

        // Loading print services happens.
        final Loader<List<PrintServiceInfo>> loader =
                mMonitor.onCreateLoader(LOADER_ID, null /* args */);
        assertThat(loader).isInstanceOf(PrintServicesLoader.class);
        verifyNoIndexing(PrintSettingsFragment.class);

        mMonitor.onLoadFinished(loader, Collections.emptyList());

        verifyIncrementalIndexing(PrintSettingsFragment.class);
    }

    @Test
    public void testInputDevicesMonitor() {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        // Rebuild indexing should happen.
        verifyIncrementalIndexing(PhysicalKeyboardFragment.class);
        // Input monitor should be registered to InputManager.
        final InputManager.InputDeviceListener listener = extactInputDeviceListener();
        assertThat(listener).isNotNull();

        /*
         * Nothing happens on successive register calls.
         */
        mMonitor.unregister(mActivity, LOADER_ID);
        reset(mIndexManager);

        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        verifyNoIndexing(PhysicalKeyboardFragment.class);
        assertThat(extactInputDeviceListener()).isEqualTo(listener);

        /*
         * A device is added.
         */
        reset(mIndexManager);

        listener.onInputDeviceAdded(1 /* deviceId */);

        verifyIncrementalIndexing(PhysicalKeyboardFragment.class);

        /*
         * A device is removed.
         */
        reset(mIndexManager);

        listener.onInputDeviceRemoved(2 /* deviceId */);

        verifyIncrementalIndexing(PhysicalKeyboardFragment.class);

        /*
         * A device is changed.
         */
        reset(mIndexManager);

        listener.onInputDeviceChanged(3 /* deviceId */);

        verifyIncrementalIndexing(PhysicalKeyboardFragment.class);
    }

    @Test
    public void testAccessibilityServicesMonitor() throws Exception {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When an accessibility service package is installed, incremental indexing happen.
         */
        reset(mIndexManager);

        installAccessibilityService(A11Y_PACKAGE_1);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When another accessibility service package is installed, incremental indexing happens.
         */
        reset(mIndexManager);

        installAccessibilityService(A11Y_PACKAGE_2);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When an accessibility service is disabled, rebuild indexing happens.
         */
        reset(mIndexManager);

        disableInstalledPackage(A11Y_PACKAGE_1);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When an accessibility service is enabled, incremental indexing happens.
         */
        reset(mIndexManager);

        enableInstalledPackage(A11Y_PACKAGE_1);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When an accessibility service package is uninstalled, rebuild indexing happens.
         */
        reset(mIndexManager);

        uninstallAccessibilityService(A11Y_PACKAGE_1);

        verifyIncrementalIndexing(AccessibilitySettings.class);

        /*
         * When an input method service package is installed, nothing happens.
         */
        reset(mIndexManager);

        installInputMethodService(IME_PACKAGE_1);

        verifyNoIndexing(AccessibilitySettings.class);
    }

    @Test
    public void testInputMethodServicesMonitor() throws Exception {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        final Uri enabledInputMethodsContentUri = Settings.Secure.getUriFor(
                Settings.Secure.ENABLED_INPUT_METHODS);
        // Content observer should be registered.
        final ContentObserver observer = extractContentObserver(enabledInputMethodsContentUri);
        assertThat(observer).isNotNull();

        /*
         * When an input method service package is installed, incremental indexing happen.
         */
        reset(mIndexManager);

        installInputMethodService(IME_PACKAGE_1);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When another input method service package is installed, incremental indexing happens.
         */
        reset(mIndexManager);

        installInputMethodService(IME_PACKAGE_2);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When an input method service is disabled, rebuild indexing happens.
         */
        reset(mIndexManager);

        disableInstalledPackage(IME_PACKAGE_1);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When an input method service is enabled, incremental indexing happens.
         */
        reset(mIndexManager);

        enableInstalledPackage(IME_PACKAGE_1);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When an input method service package is uninstalled, rebuild indexing happens.
         */
        reset(mIndexManager);

        uninstallInputMethodService(IME_PACKAGE_1);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When an accessibility service package is installed, nothing happens.
         */
        reset(mIndexManager);

        installAccessibilityService(A11Y_PACKAGE_1);

        verifyNoIndexing(VirtualKeyboardFragment.class);
        verifyNoIndexing(AvailableVirtualKeyboardFragment.class);

        /*
         * When enabled IMEs list is changed, rebuild indexing happens.
         */
        reset(mIndexManager);

        observer.onChange(false /* selfChange */, enabledInputMethodsContentUri);

        verifyIncrementalIndexing(VirtualKeyboardFragment.class);
        verifyIncrementalIndexing(AvailableVirtualKeyboardFragment.class);
    }

    @Test
    public void testUserDictionaryChangeMonitor() throws Exception {
        mMonitor.register(mActivity, LOADER_ID, mIndexManager, true /* isUserUnlocked */);

        // Content observer should be registered.
        final ContentObserver observer = extractContentObserver(UserDictionary.Words.CONTENT_URI);
        assertThat(observer).isNotNull();

        verifyIncrementalIndexing(LanguageAndInputSettings.class);

        /*
         * When user dictionary content is changed, rebuild indexing happens.
         */
        reset(mIndexManager);

        observer.onChange(false /* selfChange */, UserDictionary.Words.CONTENT_URI);

        verifyIncrementalIndexing(LanguageAndInputSettings.class);
    }

    /*
     * Verification helpers.
     */

    private void verifyNoIndexing(Class<?> indexingClass) {
        verify(mIndexManager, never()).updateFromClassNameResource(eq(indexingClass.getName()),
                anyBoolean());
    }

    private void verifyIncrementalIndexing(Class<?> indexingClass) {
        verify(mIndexManager, times(1)).updateFromClassNameResource(indexingClass.getName(),
                true /* includeInSearchResults */);
        verify(mIndexManager, never()).updateFromClassNameResource(indexingClass.getName(),
                false /* includeInSearchResults */);
    }

    /*
     * Testing helper methods.
     */

    private InputManager.InputDeviceListener extactInputDeviceListener() {
        List<InputManager.InputDeviceListener> listeners = ((ShadowInputManager) ShadowExtractor
                .extract(mInputManager))
                .getRegisteredInputDeviceListeners();
        InputManager.InputDeviceListener inputDeviceListener = null;
        for (InputManager.InputDeviceListener listener : listeners) {
            if (isUnderTest(listener)) {
                if (inputDeviceListener != null) {
                    assertThat(listener).isEqualTo(inputDeviceListener);
                } else {
                    inputDeviceListener = listener;
                }
            }
        }
        return inputDeviceListener;
    }

    private PackageMonitor extractPackageMonitor() {
        List<ShadowApplication.Wrapper> receivers = ShadowApplication.getInstance()
                .getRegisteredReceivers();
        PackageMonitor packageMonitor = null;
        for (ShadowApplication.Wrapper wrapper : receivers) {
            BroadcastReceiver receiver = wrapper.getBroadcastReceiver();
            if (isUnderTest(receiver) && receiver instanceof PackageMonitor) {
                if (packageMonitor != null) {
                    assertThat(receiver).isEqualTo(packageMonitor);
                } else {
                    packageMonitor = (PackageMonitor) receiver;
                }
            }
        }
        return packageMonitor;
    }

    private ContentObserver extractContentObserver(Uri uri) {
        ShadowContentResolver contentResolver = (ShadowContentResolver) ShadowExtractor
                .extract(mActivity.getContentResolver());
        Collection<ContentObserver> observers = contentResolver.getContentObservers(uri);
        ContentObserver contentObserver = null;
        for (ContentObserver observer : observers) {
            if (isUnderTest(observer)) {
                if (contentObserver != null) {
                    assertThat(observer).isEqualTo(contentObserver);
                } else {
                    contentObserver = observer;
                }
            }
        }
        return contentObserver;
    }

    private void enableInstalledPackage(String packageName) {
        ((PackageManager) mRobolectricPackageManager).setApplicationEnabledSetting(
                packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
        extractPackageMonitor().onPackageModified(packageName);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private void disableInstalledPackage(String packageName) {
        ((PackageManager) mRobolectricPackageManager).setApplicationEnabledSetting(
                packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0 /* flags */);
        extractPackageMonitor().onPackageModified(packageName);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private void installAccessibilityService(String packageName) throws Exception {
        final AccessibilityServiceInfo serviceToAdd = buildAccessibilityServiceInfo(packageName);

        final List<AccessibilityServiceInfo> services = new ArrayList<>();
        services.addAll(mShadowAccessibilityManager.getInstalledAccessibilityServiceList());
        services.add(serviceToAdd);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(services);

        final Intent intent = DynamicIndexableContentMonitor
                .getAccessibilityServiceIntent(packageName);
        mRobolectricPackageManager.addResolveInfoForIntent(intent, serviceToAdd.getResolveInfo());
        mRobolectricPackageManager.addPackage(packageName);

        extractPackageMonitor()
                .onPackageAppeared(packageName, PackageMonitor.PACKAGE_PERMANENT_CHANGE);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private void uninstallAccessibilityService(String packageName) throws Exception {
        final AccessibilityServiceInfo serviceToRemove = buildAccessibilityServiceInfo(packageName);

        final List<AccessibilityServiceInfo> services = new ArrayList<>();
        services.addAll(mShadowAccessibilityManager.getInstalledAccessibilityServiceList());
        services.remove(serviceToRemove);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(services);

        final Intent intent = DynamicIndexableContentMonitor
                .getAccessibilityServiceIntent(packageName);
        mRobolectricPackageManager.removeResolveInfosForIntent(intent, packageName);
        mRobolectricPackageManager.removePackage(packageName);

        extractPackageMonitor()
                .onPackageDisappeared(packageName, PackageMonitor.PACKAGE_PERMANENT_CHANGE);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private void installInputMethodService(String packageName) throws Exception {
        final ResolveInfo resolveInfoToAdd = buildResolveInfo(packageName, "imeService");
        final InputMethodInfo serviceToAdd = buildInputMethodInfo(resolveInfoToAdd);

        final List<InputMethodInfo> services = new ArrayList<>();
        services.addAll(mShadowInputMethodManager.getInputMethodList());
        services.add(serviceToAdd);
        mShadowInputMethodManager.setInputMethodList(services);

        final Intent intent = DynamicIndexableContentMonitor.getIMEServiceIntent(packageName);
        mRobolectricPackageManager.addResolveInfoForIntent(intent, resolveInfoToAdd);
        mRobolectricPackageManager.addPackage(packageName);

        extractPackageMonitor()
                .onPackageAppeared(packageName, PackageMonitor.PACKAGE_PERMANENT_CHANGE);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private void uninstallInputMethodService(String packageName) throws Exception {
        final ResolveInfo resolveInfoToRemove = buildResolveInfo(packageName, "imeService");
        final InputMethodInfo serviceToRemove = buildInputMethodInfo(resolveInfoToRemove);

        final List<InputMethodInfo> services = new ArrayList<>();
        services.addAll(mShadowInputMethodManager.getInputMethodList());
        services.remove(serviceToRemove);
        mShadowInputMethodManager.setInputMethodList(services);

        final Intent intent = DynamicIndexableContentMonitor.getIMEServiceIntent(packageName);
        mRobolectricPackageManager.removeResolveInfosForIntent(intent, packageName);
        mRobolectricPackageManager.removePackage(packageName);

        extractPackageMonitor()
                .onPackageDisappeared(packageName, PackageMonitor.PACKAGE_PERMANENT_CHANGE);
        Robolectric.flushBackgroundThreadScheduler();
    }

    private AccessibilityServiceInfo buildAccessibilityServiceInfo(String packageName)
            throws IOException, XmlPullParserException {
        return new AccessibilityServiceInfo(
                buildResolveInfo(packageName, "A11yService"), mActivity);
    }

    private static InputMethodInfo buildInputMethodInfo(ResolveInfo resolveInfo) {
        return new InputMethodInfo(resolveInfo, false /* isAuxIme */, "SettingsActivity",
                null /* subtypes */,  0 /* defaultResId */, false /* forceDefault */);
    }

    private static ResolveInfo buildResolveInfo(String packageName, String className) {
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.serviceInfo.name = className;
        // To workaround that RobolectricPackageManager.removeResolveInfosForIntent() only works
        // for activity/broadcast resolver.
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = className;

        return resolveInfo;
    }

    private static boolean isUnderTest(Object object) {
        return object.getClass().getName().startsWith(
                DynamicIndexableContentMonitor.class.getName());
    }
}
