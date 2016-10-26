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
package com.android.settings;

import java.util.List;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

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
        // Using the manifest file's relative path, we can figure out the application directory.
        final String appRoot = "packages/apps/Settings";
        final String manifestPath = appRoot + "/AndroidManifest.xml";
        final String resDir = appRoot + "/res";
        final String assetsDir = appRoot + "/assets";

        // By adding any resources from libraries we need to the AndroidManifest, we can access
        // them from within the parallel universe's resource loader.
        final AndroidManifest manifest = new AndroidManifest(Fs.fileFromPath(manifestPath),
                Fs.fileFromPath(resDir), Fs.fileFromPath(assetsDir)) {
            @Override
            public List<ResourcePath> getIncludedResourcePaths() {
                List<ResourcePath> paths = super.getIncludedResourcePaths();
                paths.add(new ResourcePath(
                        getPackageName(),
                        Fs.fileFromPath("./packages/apps/Settings/res"),
                        null));
                paths.add(new ResourcePath(
                        getPackageName(),
                        Fs.fileFromPath("./frameworks/base/packages/SettingsLib/res"),
                        null));
                return paths;
            }
        };

        // Set the package name to the renamed one
        manifest.setPackageName("com.android.settings");
        return manifest;
    }
}