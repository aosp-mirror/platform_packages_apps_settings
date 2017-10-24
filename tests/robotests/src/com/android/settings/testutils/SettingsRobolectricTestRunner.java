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

import android.app.Fragment;
import android.content.Intent;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;
import org.robolectric.util.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static org.robolectric.Robolectric.getShadowsAdapter;

import com.android.settings.SettingsActivity;

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
        final String resDir = appRoot + "/tests/robotests/res";
        final String assetsDir = appRoot + config.assetDir();

        // By adding any resources from libraries we need the AndroidManifest, we can access
        // them from within the parallel universe's resource loader.
        final AndroidManifest manifest = new AndroidManifest(Fs.fileFromPath(manifestPath),
                Fs.fileFromPath(resDir), Fs.fileFromPath(assetsDir)) {
            @Override
            public List<ResourcePath> getIncludedResourcePaths() {
                List<ResourcePath> paths = super.getIncludedResourcePaths();
                SettingsRobolectricTestRunner.getIncludedResourcePaths(getPackageName(), paths);
                return paths;
            }
        };

        // Set the package name to the renamed one
        manifest.setPackageName("com.android.settings");
        return manifest;
    }

    public static void getIncludedResourcePaths(String packageName, List<ResourcePath> paths) {
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./packages/apps/Settings/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/base/packages/SettingsLib/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/base/core/res/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/opt/setupwizard/library/main/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/opt/setupwizard/library/gingerbread/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/opt/setupwizard/library/recyclerview/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/support/v7/appcompat/res"),
                null));
        paths.add(new ResourcePath(
                packageName,
                Fs.fileFromPath("./frameworks/support/v7/cardview/res"),
                null));
    }

    // A simple utility class to start a Settings fragment with an intent. The code here is almost
    // the same as FragmentTestUtil.startFragment except that it starts an activity with an intent.
    public static void startSettingsFragment(
            Fragment fragment, Class<? extends SettingsActivity> activityClass) {
        Intent intent = new Intent().putExtra(EXTRA_SHOW_FRAGMENT, fragment.getClass().getName());
        SettingsActivity activity = ActivityController.of(
                getShadowsAdapter(), ReflectionHelpers.callConstructor(activityClass), intent)
                .setup().get();
        activity.getFragmentManager().beginTransaction().add(fragment, null).commit();
    }
}
