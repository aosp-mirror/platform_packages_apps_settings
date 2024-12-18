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

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_SEARCHABLE;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_INCLUDE_PREF_SCREEN;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_SEARCHABLE;
import static com.android.settings.search.SettingsSearchIndexablesProvider.SYSPROP_CRASH_ON_ERROR;

import android.annotation.XmlRes;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableRaw;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A basic SearchIndexProvider that returns no data to index.
 */
public class BaseSearchIndexProvider implements Indexable.SearchIndexProvider {

    private static final String TAG = "BaseSearchIndex";
    private int mXmlRes = 0;

    public BaseSearchIndexProvider() {
    }

    public BaseSearchIndexProvider(int xmlRes) {
        mXmlRes = xmlRes;
    }

    @Override
    public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
        if (mXmlRes != 0) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = mXmlRes;
            return Arrays.asList(sir);
        }
        return null;
    }

    @Override
    public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
        final List<SearchIndexableRaw> raws = new ArrayList<>();
        final List<AbstractPreferenceController> controllers = getPreferenceControllers(context);
        if (controllers == null || controllers.isEmpty()) {
            return raws;
        }
        for (AbstractPreferenceController controller : controllers) {
            if (controller instanceof PreferenceControllerMixin) {
                ((PreferenceControllerMixin) controller).updateRawDataToIndex(raws);
            } else if (controller instanceof BasePreferenceController) {
                ((BasePreferenceController) controller).updateRawDataToIndex(raws);
            }
        }
        return raws;
    }

    @Override
    @CallSuper
    public List<SearchIndexableRaw> getDynamicRawDataToIndex(Context context, boolean enabled) {
        final List<SearchIndexableRaw> dynamicRaws = new ArrayList<>();
        if (!isPageSearchEnabled(context)) {
            // Entire page should be suppressed, do not add dynamic raw data.
            return dynamicRaws;
        }
        final List<AbstractPreferenceController> controllers = getPreferenceControllers(context);
        if (controllers == null || controllers.isEmpty()) {
            return dynamicRaws;
        }
        for (AbstractPreferenceController controller : controllers) {
            if (controller instanceof PreferenceControllerMixin) {
                ((PreferenceControllerMixin) controller).updateDynamicRawDataToIndex(dynamicRaws);
            } else if (controller instanceof BasePreferenceController) {
                ((BasePreferenceController) controller).updateDynamicRawDataToIndex(dynamicRaws);
            } else {
                Log.e(TAG, controller.getClass().getName()
                        + " must implement " + PreferenceControllerMixin.class.getName()
                        + " treating the dynamic indexable");
            }
        }
        return dynamicRaws;
    }

    @Override
    @CallSuper
    public List<String> getNonIndexableKeys(Context context) {
        final List<String> nonIndexableKeys = new ArrayList<>();
        if (!isPageSearchEnabled(context)) {
            // Entire page should be suppressed, mark all keys from this page as non-indexable.
            nonIndexableKeys.addAll(
                    getNonIndexableKeysFromXml(context, true /* suppressAllPage */));
            nonIndexableKeys.addAll(
                    getRawDataToIndex(context, true /* enabled */)
                            .stream()
                            .map(data -> data.key)
                            .collect(Collectors.toList()));
            return nonIndexableKeys;
        }
        nonIndexableKeys.addAll(getNonIndexableKeysFromXml(context, false /* suppressAllPage */));
        updateNonIndexableKeysFromControllers(context, nonIndexableKeys);
        return nonIndexableKeys;
    }

    private void updateNonIndexableKeysFromControllers(
            Context context, List<String> nonIndexableKeys) {
        final List<AbstractPreferenceController> controllers = getPreferenceControllers(context);
        if (controllers != null) {
            for (AbstractPreferenceController controller : controllers) {
                updateNonIndexableKeysFromController(nonIndexableKeys, controller);
            }
        }
    }

    private static void updateNonIndexableKeysFromController(
            List<String> nonIndexableKeys, AbstractPreferenceController controller) {
        try {
            if (controller instanceof PreferenceControllerMixin controllerMixin) {
                controllerMixin.updateNonIndexableKeys(nonIndexableKeys);
            } else if (controller instanceof BasePreferenceController basePreferenceController) {
                basePreferenceController.updateNonIndexableKeys(nonIndexableKeys);
            } else {
                Log.e(TAG, controller.getClass().getName()
                        + " must implement " + PreferenceControllerMixin.class.getName()
                        + " treating the key non-indexable");
                nonIndexableKeys.add(controller.getPreferenceKey());
            }
        } catch (Exception e) {
            String msg = "Error trying to get non-indexable keys from: " + controller;
            // Catch a generic crash. In the absence of the catch, the background thread will
            // silently fail anyway, so we aren't losing information by catching the exception.
            // We crash on debuggable build or when the system property exists, so that we can test
            // if crashes need to be fixed.
            // The gain is that if there is a crash in a specific controller, we don't lose all
            // non-indexable keys, but we can still find specific crashes in development.
            if (Build.IS_DEBUGGABLE || System.getProperty(SYSPROP_CRASH_ON_ERROR) != null) {
                throw new RuntimeException(msg, e);
            }
            Log.e(TAG, msg, e);
            // When there is an error, treat the key as non-indexable.
            nonIndexableKeys.add(controller.getPreferenceKey());
        }
    }

    public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllersFromCode = new ArrayList<>();
        try {
            controllersFromCode = createPreferenceControllers(context);
        } catch (Exception e) {
            Log.w(TAG, "Error initializing controller in fragment: " + this + ", e: " + e);
        }

        final List<SearchIndexableResource> res = getXmlResourcesToIndex(context, true);
        if (res == null || res.isEmpty()) {
            return controllersFromCode;
        }
        List<BasePreferenceController> controllersFromXml = new ArrayList<>();
        for (SearchIndexableResource sir : res) {
            controllersFromXml.addAll(PreferenceControllerListHelper
                    .getPreferenceControllersFromXml(context, sir.xmlResId));
        }
        controllersFromXml = PreferenceControllerListHelper.filterControllers(controllersFromXml,
                controllersFromCode);
        final List<AbstractPreferenceController> allControllers = new ArrayList<>();
        if (controllersFromCode != null) {
            allControllers.addAll(controllersFromCode);
        }
        allControllers.addAll(controllersFromXml);
        return allControllers;
    }

    /**
     * Creates a list of {@link AbstractPreferenceController} programatically.
     * <p/>
     * This list should create controllers that are not defined in xml as a Slice controller.
     */
    public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    /**
     * Returns true if the page should be considered in search query. If return false, entire page
     * will be suppressed during search query.
     */
    protected boolean isPageSearchEnabled(Context context) {
        return true;
    }

    /**
     * Get all non-indexable keys from xml. If {@param suppressAllPage} is set, all keys are
     * considered non-indexable. Otherwise, only keys with searchable="false" are included.
     */
    private List<String> getNonIndexableKeysFromXml(Context context, boolean suppressAllPage) {
        final List<SearchIndexableResource> resources = getXmlResourcesToIndex(
                context, true /* not used*/);
        if (resources == null || resources.isEmpty()) {
            return new ArrayList<>();
        }
        final List<String> nonIndexableKeys = new ArrayList<>();
        for (SearchIndexableResource res : resources) {
            nonIndexableKeys.addAll(
                    getNonIndexableKeysFromXml(context, res.xmlResId, suppressAllPage));
        }
        return nonIndexableKeys;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public List<String> getNonIndexableKeysFromXml(Context context, @XmlRes int xmlResId,
            boolean suppressAllPage) {
        return getKeysFromXml(context, xmlResId, suppressAllPage);
    }

    private List<String> getKeysFromXml(Context context, @XmlRes int xmlResId,
            boolean suppressAllPage) {
        final List<String> keys = new ArrayList<>();
        try {
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(context,
                    xmlResId, FLAG_NEED_KEY | FLAG_INCLUDE_PREF_SCREEN | FLAG_NEED_SEARCHABLE);
            for (Bundle bundle : metadata) {
                if (suppressAllPage || !bundle.getBoolean(METADATA_SEARCHABLE, true)) {
                    keys.add(bundle.getString(METADATA_KEY));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Log.w(TAG, "Error parsing non-indexable from xml " + xmlResId);
        }
        return keys;
    }
}
