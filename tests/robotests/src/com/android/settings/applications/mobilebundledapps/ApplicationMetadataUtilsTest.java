/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.mobilebundledapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.google.common.io.CharSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@RunWith(RobolectricTestRunner.class)
public class ApplicationMetadataUtilsTest {
    private static final String TEST_PACKAGE_NAME = "test";
    private static final String TEST_SOURCE_DIR = "sourcedir";

    private static final String TEST_XML_SCHEMA = "<transparency-info>\n"
            + "  <template/>\n"
            + "  <contains-ads/>\n"
            + "  <developers>\n"
            + "    <developer name=\"Example ODM\" relationship=\"ODM\" email=\"odm@example.com\""
            + " \n"
            + "               website=\"http://odm.example.com\" country=\"US\"/>\n"
            + "    <developer name=\"Example carrier\" relationship=\"CARRIER\" "
            + "email=\"carrier@example.com\" \n"
            + "               country=\"US\"/>\n"
            + "  </developers>\n"
            + "  <contact url=\"http://example.com/contact-us\" email=\"contact@example.com\"/>\n"
            + "  <privacy-policy url=\"https://www.example.com/privacy-policy.html\"/>\n"
            + "  <description>This application provides the user with news "
            + "headlines</description>\n"
            + "  <category name=\"News and magazines\"/>\n"
            + "</transparency-info>";
    @Mock
    private PackageManager mPackageManager;

    private Document mDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(CharSource.wrap(TEST_XML_SCHEMA).asByteSource(StandardCharsets.UTF_8)
                    .openStream());


    public ApplicationMetadataUtilsTest()
            throws IOException, ParserConfigurationException, SAXException {
    }

    @Before
    public void setup()
            throws PackageManager.NameNotFoundException, IOException, ParserConfigurationException,
            SAXException {
        MockitoAnnotations.initMocks(this);
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.sourceDir = TEST_SOURCE_DIR;
        when(mPackageManager.getApplicationInfo(eq(TEST_PACKAGE_NAME),
                any(PackageManager.ApplicationInfoFlags.class))).thenReturn(appInfo);
    }

    @Test
    public void getDefaultInstance_alwaysReturnSameInstance() {
        final ApplicationMetadataUtils firstInstance =
                ApplicationMetadataUtils.getDefaultInstance();

        assertThat(firstInstance).isEqualTo(ApplicationMetadataUtils.getDefaultInstance());
    }

    @Test(expected = RuntimeException.class)
    public void createInstance_bubblesUpException() throws PackageManager.NameNotFoundException {
        final String testErrorMsg = "test";
        when(mPackageManager.getApplicationInfo(eq(TEST_PACKAGE_NAME),
                any(PackageManager.ApplicationInfoFlags.class)))
                .thenThrow(new Exception(testErrorMsg));

        ApplicationMetadataUtils.newInstance(mPackageManager, TEST_PACKAGE_NAME);
    }

    @Test
    public void fieldGetters_toReturnNull_whenEmptyOrError() {
        final ApplicationMetadataUtils appUtils = new ApplicationMetadataUtils();
        assertThat(appUtils.getContainsAds()).isEqualTo(false);
        assertThat(appUtils.getCategoryName()).isNull();
        assertThat(appUtils.getPrivacyPolicyUrl()).isNull();
        assertThat(appUtils.getDescription()).isNull();
        assertThat(appUtils.getDevelopers()).isEmpty();
    }

    @Test
    public void fieldGetters_toReturnCorrectValues_whenExists() {
        final ApplicationMetadataUtils appUtils = new ApplicationMetadataUtils();
        appUtils.setXmlDoc(mDocument);
        assertThat(appUtils.getContainsAds()).isEqualTo(true);
        assertThat(appUtils.getCategoryName()).isEqualTo("News and magazines");
        assertThat(appUtils.getPrivacyPolicyUrl())
                .isEqualTo("https://www.example.com/privacy-policy.html");
        assertThat(appUtils.getDescription())
                .isEqualTo("This application provides the user with news headlines");
    }

    @Test
    public void getDevelopers_returnsCorrectValues() {
        final ApplicationMetadataUtils appUtils = new ApplicationMetadataUtils();
        appUtils.setXmlDoc(mDocument);
        final List<ApplicationMetadataUtils.MbaDeveloper> developers = appUtils.getDevelopers();

        assertThat(developers.size()).isEqualTo(2);
        assertThat(developers.get(0).country).isEqualTo("US");
        assertThat(developers.get(0).email).isEqualTo("odm@example.com");
        assertThat(developers.get(0).name).isEqualTo("Example ODM");
        assertThat(developers.get(1).relationship).isEqualTo("CARRIER");
        assertThat(developers.get(1).country).isEqualTo("US");
        assertThat(developers.get(1).email).isEqualTo("carrier@example.com");
        assertThat(developers.get(1).name).isEqualTo("Example carrier");
        assertThat(developers.get(1).relationship).isEqualTo("CARRIER");
    }
}
