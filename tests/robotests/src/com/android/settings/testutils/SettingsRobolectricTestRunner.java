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
 * limitations under the License.
 */
package com.android.settings.testutils;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Custom test runner for the testing of BluetoothPairingDialogs. This is needed because the
 * default behavior for robolectric is just to grab the resource directory in the target package.
 * We want to override this to add several spanning different projects.
 */
public class SettingsRobolectricTestRunner extends RobolectricTestRunner {

    /**
     * We don't actually want to change this behavior, so we just call super.
     */
    public SettingsRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    /**
     * We are going to create our own custom manifest so that we can add multiple resource
     * paths to it. This lets us access resources in both Settings and SettingsLib in our tests.
     */
    @Override
    protected AndroidManifest getAppManifest(Config config) {
        try {
            // Using the manifest file's relative path, we can figure out the application directory.
            final URL appRoot = new URL("file:packages/apps/Settings/");
            final URL manifestPath = new URL(appRoot, "AndroidManifest.xml");
            final URL resDir = new URL(appRoot, "tests/robotests/res");
            final URL assetsDir = new URL(appRoot, "tests/robotests/assets");

            // By adding any resources from libraries we need the AndroidManifest, we can access
            // them from within the parallel universe's resource loader.
            return new AndroidManifest(Fs.fromURL(manifestPath), Fs.fromURL(resDir),
                Fs.fromURL(assetsDir), "com.android.settings") {
                @Override
                public List<ResourcePath> getIncludedResourcePaths() {
                    final List<ResourcePath> paths = super.getIncludedResourcePaths();
                    addIncludedResourcePaths(paths);
                    return paths;
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException("SettingsRobolectricTestRunner failure", e);
        }
    }

    public static void addIncludedResourcePaths(List<ResourcePath> paths) {
        try {
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:packages/apps/Settings/res")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:frameworks/base/packages/SettingsLib/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/base/packages/SettingsLib/AppPreference/res/")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/base/packages/SettingsLib/HelpUtils/res/")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/base/packages/SettingsLib/RestrictedLockUtils/res/")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:frameworks/base/core/res/res")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:frameworks/opt/setupwizard/library/main/res")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:frameworks/opt/setupwizard/library/gingerbread/res")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:frameworks/opt/setupwizard/library/recyclerview/res")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:out/soong/.intermediates/prebuilts/sdk/current/androidx/androidx.appcompat_appcompat-nodeps/android_common/aar/res/")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:out/soong/.intermediates/prebuilts/sdk/current/extras/material-design-x/com.google.android.material_material-nodeps/android_common/aar/res/")), null));
            paths.add(new ResourcePath(null,
                Fs.fromURL(new URL("file:out/soong/.intermediates/prebuilts/sdk/current/androidx/androidx.cardview_cardview-nodeps/android_common/aar/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:out/soong/.intermediates/prebuilts/sdk/current/androidx/androidx.slice_slice-view-nodeps/android_common/aar/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:out/soong/.intermediates/prebuilts/sdk/current/androidx/androidx.preference_preference-nodeps/android_common/aar/res")), null));
        } catch (MalformedURLException e) {
            throw new RuntimeException("SettingsRobolectricTestRunner failure", e);
        }
    }
}
