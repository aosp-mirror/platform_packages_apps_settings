/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;

import org.robolectric.RuntimeEnvironment;

import java.util.List;

public class DashboardFragmentSearchIndexProviderInspector {

    public static boolean isSharingPreferenceControllers(Class clazz) {
        final Context context = RuntimeEnvironment.application;
        final Fragment fragment;
        try {
            fragment = Fragment.instantiate(context, clazz.getName());
        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }
        if (!(fragment instanceof DashboardFragment)) {
            return true;
        }

        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) DatabaseIndexingUtils.getSearchIndexProvider(clazz);
        if (provider == null) {
            return true;
        }
        final List<AbstractPreferenceController> controllersFromSearchIndexProvider;
        final List<AbstractPreferenceController> controllersFromFragment;
        try {
            controllersFromSearchIndexProvider = provider.getPreferenceControllers(context);
        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }
        try {
            controllersFromFragment =
                    ((DashboardFragment) fragment).createPreferenceControllers(context);
            List<BasePreferenceController> controllersFromXml = PreferenceControllerListHelper
                    .getPreferenceControllersFromXml(context,
                            ((DashboardFragment) fragment).getPreferenceScreenResId());
            final List<BasePreferenceController> uniqueControllerFromXml =
                    PreferenceControllerListHelper.filterControllers(
                            controllersFromXml, controllersFromFragment);
            controllersFromFragment.addAll(uniqueControllerFromXml);

        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }

        if (controllersFromFragment == controllersFromSearchIndexProvider) {
            return true;
        } else if (controllersFromFragment != null && controllersFromSearchIndexProvider != null) {
            return controllersFromFragment.size() == controllersFromSearchIndexProvider.size();
        } else {
            return false;
        }
    }
}
