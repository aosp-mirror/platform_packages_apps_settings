/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_CONTROLLER;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_FOR_WORK;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;

import android.annotation.XmlRes;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;
import com.android.settingslib.core.AbstractPreferenceController;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper to load {@link BasePreferenceController} lists from Xml.
 */
public class PreferenceControllerListHelper {

    private static final String TAG = "PrefCtrlListHelper";

    /**
     * Instantiates a list of controller based on xml definition.
     */
    @NonNull
    public static List<BasePreferenceController> getPreferenceControllersFromXml(Context context,
            @XmlRes int xmlResId) {
        final List<BasePreferenceController> controllers = new ArrayList<>();
        List<Bundle> preferenceMetadata;
        try {
            preferenceMetadata = PreferenceXmlParserUtils.extractMetadata(context, xmlResId,
                    MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER
                            | MetadataFlag.FLAG_INCLUDE_PREF_SCREEN  | MetadataFlag.FLAG_FOR_WORK);
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Failed to parse preference xml for getting controllers", e);
            return controllers;
        }

        for (Bundle metadata : preferenceMetadata) {
            final String controllerName = metadata.getString(METADATA_CONTROLLER);
            if (TextUtils.isEmpty(controllerName)) {
                continue;
            }
            BasePreferenceController controller;
            try {
                controller = BasePreferenceController.createInstance(context, controllerName);
            } catch (IllegalStateException e) {
                Log.d(TAG, "Could not find Context-only controller for pref: " + controllerName);
                final String key = metadata.getString(METADATA_KEY);
                final boolean isWorkProfile = metadata.getBoolean(METADATA_FOR_WORK, false);
                if (TextUtils.isEmpty(key)) {
                    Log.w(TAG, "Controller requires key but it's not defined in xml: "
                            + controllerName);
                    continue;
                }
                try {
                    controller = BasePreferenceController.createInstance(context, controllerName,
                            key, isWorkProfile);
                } catch (IllegalStateException e2) {
                    Log.w(TAG, "Cannot instantiate controller from reflection: " + controllerName);
                    continue;
                }
            }
            controllers.add(controller);
        }
        return controllers;
    }

    /**
     * Checks if the given PreferenceScreen will be empty due to all preferences being unavailable.
     *
     * @param xmlResId resource id of the PreferenceScreen to check
     * @return {@code true} if none of the preferences in the given screen will appear
     */
    public static boolean areAllPreferencesUnavailable(@NonNull Context context,
            @NonNull PreferenceManager preferenceManager, @XmlRes int xmlResId) {
        PreferenceScreen screen = preferenceManager.inflateFromResource(context, xmlResId,
                /* rootPreferences= */ null);
        List<BasePreferenceController> preferenceControllers =
                getPreferenceControllersFromXml(context, xmlResId);
        if (screen.getPreferenceCount() != preferenceControllers.size()) {
            // There are some preferences without controllers, which will show regardless.
            return false;
        }
        return preferenceControllers.stream().noneMatch(BasePreferenceController::isAvailable);
    }

    /**
     * Return a sub list of {@link AbstractPreferenceController} to only contain controller that
     * doesn't exist in filter.
     *
     * @param filter The filter. This list will be unchanged.
     * @param input  This list will be filtered into a sublist and element is kept
     *               IFF the controller key is not used by anything from {@param filter}.
     */
    @NonNull
    public static List<BasePreferenceController> filterControllers(
            @NonNull List<BasePreferenceController> input,
            List<AbstractPreferenceController> filter) {
        if (input == null || filter == null) {
            return input;
        }
        final Set<String> keys = new TreeSet<>();
        final List<BasePreferenceController> filteredList = new ArrayList<>();
        for (AbstractPreferenceController controller : filter) {
            final String key = controller.getPreferenceKey();
            if (key != null) {
                keys.add(key);
            }
        }
        for (BasePreferenceController controller : input) {
            if (keys.contains(controller.getPreferenceKey())) {
                Log.w(TAG, controller.getPreferenceKey() + " already has a controller");
                continue;
            }
            filteredList.add(controller);
        }
        return filteredList;
    }

}
