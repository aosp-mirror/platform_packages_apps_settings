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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.core.codeinspection.ClassScanner;
import com.android.settings.core.codeinspection.CodeInspector;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceControllerInXmlTest {

    private static final List<Class> mSliceControllerClasses = Collections.singletonList(
        TogglePreferenceController.class
    );

    private final List<String> mXmlDeclaredControllers = new ArrayList<>();
    private final List<String> mGrandfatheredClasses = new ArrayList<>();

    private final String ERROR_MISSING_CONTROLLER =
            "The following controllers were expected to be declared by "
                    + "'settings:controller=Controller_Class_Name' in their corresponding Xml. "
                    + "If it should not appear in XML, add the controller's classname to "
                    + "grandfather_slice_controller_not_in_xml. Controllers:\n";

    private Context mContext;

    private SearchFeatureProvider mSearchProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        mSearchProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchProvider;

        CodeInspector.initializeGrandfatherList(mGrandfatheredClasses,
                "grandfather_slice_controller_not_in_xml");
        initDeclaredControllers();
    }

    private void initDeclaredControllers() {
        final List<Integer> xmlResources = getIndexableXml();
        XmlResourceParser parser;

        for (int xmlResId : xmlResources) {
            try {
                parser = mContext.getResources().getXml(xmlResId);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                    // Parse next until start tag is found
                }

                final int outerDepth = parser.getDepth();
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                String controllerClassName;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    controllerClassName = PreferenceXmlParserUtils.getController(mContext, attrs);

                    if (!TextUtils.isEmpty(controllerClassName)) {
                        mXmlDeclaredControllers.add(controllerClassName);
                    }
                }
            } catch (Exception e) {
                // Assume an issue with robolectric resources
            }
        }
    }

    @Test
    public void testAllControllersDeclaredInXml() throws Exception {
        final List<Class<?>> classes =
            new ClassScanner().getClassesForPackage(mContext.getPackageName());
        final List<String> missingControllersInXml = new ArrayList<>();

        for (Class<?> clazz : classes) {
            if (!isInlineSliceClass(clazz)) {
                // Only care about inline-slice controller classes.
                continue;
            }

            if (!mXmlDeclaredControllers.contains(clazz.getName())) {
                // Class clazz should have been declared in XML (unless whitelisted).
                missingControllersInXml.add(clazz.getName());
            }
        }

        // Removed whitelisted classes
        missingControllersInXml.removeAll(mGrandfatheredClasses);

        final String missingControllerError =
            buildErrorMessage(ERROR_MISSING_CONTROLLER, missingControllersInXml);

        assertWithMessage(missingControllerError).that(missingControllersInXml).isEmpty();
    }

    private boolean isInlineSliceClass(Class clazz) {
        while (clazz != null) {
            clazz = clazz.getSuperclass();
            if (mSliceControllerClasses.contains(clazz)) {
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

        final Collection<Class> indexableClasses = FeatureFactory.getFactory(
                mContext).getSearchFeatureProvider().getSearchIndexableResources()
                .getProviderValues();

        for (Class clazz : indexableClasses) {
            Indexable.SearchIndexProvider provider = DatabaseIndexingUtils.getSearchIndexProvider(
                    clazz);

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
