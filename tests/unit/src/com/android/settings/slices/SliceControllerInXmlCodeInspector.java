/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.slices;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_CONTROLLER;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableData;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SliceControllerInXmlCodeInspector extends CodeInspector {

    private static final List<Class> sSliceControllerClasses = Arrays.asList(
            TogglePreferenceController.class,
            SliderPreferenceController.class
    );

    private final List<String> mXmlDeclaredControllers = new ArrayList<>();
    private final List<String> mExemptedClasses = new ArrayList<>();

    private static final String ERROR_MISSING_CONTROLLER =
            "The following controllers were expected to be declared by "
                    + "'settings:controller=Controller_Class_Name' in their corresponding Xml. "
                    + "If it should not appear in XML, add the controller's classname to "
                    + "exempt_slice_controller_not_in_xml. Controllers:\n";

    private final Context mContext;
    private final SearchFeatureProvider mSearchProvider;
    private final FakeFeatureFactory mFakeFeatureFactory;

    public SliceControllerInXmlCodeInspector(List<Class<?>> classes) throws Exception {
        super(classes);
        mContext = ApplicationProvider.getApplicationContext();
        mSearchProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchProvider;

        CodeInspector.initializeExemptList(mExemptedClasses,
                "exempt_slice_controller_not_in_xml");
        initDeclaredControllers();
    }

    private void initDeclaredControllers() throws IOException, XmlPullParserException {
        final List<Integer> xmlResources = getIndexableXml();
        for (int xmlResId : xmlResources) {
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                    xmlResId, PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
            for (Bundle bundle : metadata) {
                final String controllerClassName = bundle.getString(METADATA_CONTROLLER);
                if (TextUtils.isEmpty(controllerClassName)) {
                    continue;
                }
                mXmlDeclaredControllers.add(controllerClassName);
            }
        }
        // We definitely have some controllers in xml, so assert not-empty here as a proxy to
        // make sure the parser didn't fail
        assertThat(mXmlDeclaredControllers).isNotEmpty();
    }

    @Override
    public void run() {
        final List<String> missingControllersInXml = new ArrayList<>();

        for (Class<?> clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                // Only care about non-abstract classes.
                continue;
            }
            if (!isInlineSliceClass(clazz)) {
                // Only care about inline-slice controller classes.
                continue;
            }

            if (!mXmlDeclaredControllers.contains(clazz.getName())) {
                // Class clazz should have been declared in XML (unless allowlisted).
                missingControllersInXml.add(clazz.getName());
            }
        }

        // Removed allowlisted classes
        missingControllersInXml.removeAll(mExemptedClasses);

        final String missingControllerError =
                buildErrorMessage(ERROR_MISSING_CONTROLLER, missingControllersInXml);

        assertWithMessage(missingControllerError).that(missingControllersInXml).isEmpty();
    }

    private boolean isInlineSliceClass(Class clazz) {
        while (clazz != null) {
            clazz = clazz.getSuperclass();
            if (sSliceControllerClasses.contains(clazz)) {
                return true;
            }
        }
        return false;
    }

    private String buildErrorMessage(String errorSummary, List<String> errorClasses) {
        final StringBuilder error = new StringBuilder(errorSummary);
        for (String c : errorClasses) {
            error.append(c).append("\n");
        }
        return error.toString();
    }

    private List<Integer> getIndexableXml() {
        final List<Integer> xmlResSet = new ArrayList<>();

        final Collection<SearchIndexableData> bundles = FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider().getSearchIndexableResources()
                .getProviderValues();

        for (SearchIndexableData bundle : bundles) {
            Indexable.SearchIndexProvider provider = bundle.getSearchIndexProvider();

            if (provider == null) {
                continue;
            }

            List<SearchIndexableResource> resources = provider.getXmlResourcesToIndex(mContext,
                    true);

            if (resources == null) {
                continue;
            }

            for (SearchIndexableResource resource : resources) {
                // Add '0's anyway. It won't break the test.
                xmlResSet.add(resource.xmlResId);
            }
        }
        return xmlResSet;
    }
}
