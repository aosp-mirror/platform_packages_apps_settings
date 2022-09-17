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

import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Used for parsing application-metadata.xml and return relevant fields
 */
public class ApplicationMetadataUtils {
    private static final String TAG = ApplicationMetadataUtils.class.getSimpleName();

    private static final ApplicationMetadataUtils DEFAULT_INSTANCE = new ApplicationMetadataUtils();
    private static final String TRANSPARENCY_XML_DIR = "APP-INF/application-metadata.xml";
    private static final String DESCRIPTION_TAG = "description";
    private static final String CONTAINS_ADS_TAG = "contains-ads";
    private static final String PRIVACY_POLICY_TAG = "privacy-policy";
    private static final String CONTACT_TAG = "contact";
    private static final String CATEGORY_TAG = "category";
    private static final String DEVELOPER_TAG = "developer";
    private static final String URL_TAG = "url";
    private static final String EMAIL_TAG = "email";
    private static final String NAME_TAG = "name";
    private static final String RELATIONSHIP_TAG = "relationship";
    private static final String COUNTRY_TAG = "country";

    private final PackageManager mPackageManager;

    private Document mXmlDoc;

    @VisibleForTesting
    ApplicationMetadataUtils() {
        mPackageManager = null;
    }

    //Need to create singleton factory as Android is unable to mock static for testing.
    public static ApplicationMetadataUtils getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Generates a new instance that also provisions and reads the XML file.
     */
    public static ApplicationMetadataUtils newInstance(final PackageManager packageManager,
            String packageName) {
        return new ApplicationMetadataUtils(packageManager, packageName);
    }
    private ApplicationMetadataUtils(final PackageManager packageManager,
            final String packageName) {
        mPackageManager = packageManager;
        try (ZipFile apk = new ZipFile(getApkDirectory(packageName, mPackageManager))) {
            mXmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(apk.getInputStream(apk.getEntry(TRANSPARENCY_XML_DIR)));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void setXmlDoc(final Document xmlDoc) {
        mXmlDoc = xmlDoc;
    }

    private static String getApkDirectory(final String packageName,
            final PackageManager packageManager)
            throws PackageManager.NameNotFoundException {
        return packageManager
                .getApplicationInfo(packageName,
                        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA))
                .sourceDir;
    }
    public boolean getContainsAds() {
        return mXmlDoc != null
                && mXmlDoc.getElementsByTagName(CONTAINS_ADS_TAG) != null
                && mXmlDoc.getElementsByTagName(CONTAINS_ADS_TAG).getLength() > 0;
    }

    public String getPrivacyPolicyUrl() {
        return retrieveElementAttributeValue(PRIVACY_POLICY_TAG, URL_TAG);
    }

    private String retrieveElementAttributeValue(final String elementTag, final String attribute) {
        try {
            return mXmlDoc.getElementsByTagName(elementTag).item(0)
                    .getAttributes().getNamedItem(attribute).getNodeValue();
        } catch (Exception e) {
            return null;
        }
    }

    public String getDescription() {
        return retrieveElementValue(DESCRIPTION_TAG);
    }

    private String retrieveElementValue(final String elementTag) {
        try {
            return mXmlDoc.getElementsByTagName(elementTag).item(0).getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    public String getCategoryName() {
        return retrieveElementAttributeValue(CATEGORY_TAG, NAME_TAG);
    }

    public String getContactUrl() {
        return retrieveElementAttributeValue(CONTACT_TAG, URL_TAG);
    }

    public String getContactEmail() {
        return retrieveElementAttributeValue(CONTACT_TAG, EMAIL_TAG);
    }

    public String getPlayStoreUrl() {
        return retrieveElementValue(DESCRIPTION_TAG);
    }

    /**
     * Retrieves the list of relevant major parties involved with this MBA
     */
    public List<MbaDeveloper> getDevelopers() {
        final List<MbaDeveloper> developersDetails = new ArrayList();
        try {
            final NodeList developers = mXmlDoc.getElementsByTagName(DEVELOPER_TAG);
            if (developers == null) return developersDetails;
            for (int i = 0; i < developers.getLength(); ++i) {
                final NamedNodeMap developerAttributes = developers.item(i).getAttributes();
                developersDetails.add(new MbaDeveloper(
                        developerAttributes.getNamedItem(NAME_TAG).getNodeValue(),
                        developerAttributes.getNamedItem(RELATIONSHIP_TAG).getNodeValue(),
                        developerAttributes.getNamedItem(EMAIL_TAG).getNodeValue(),
                        developerAttributes.getNamedItem(COUNTRY_TAG).getNodeValue()
                ));
            }
        } catch (final Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return developersDetails;
    }

    /**
     * Determines if the a package can be parsed and extrapolate metadata from.
     */
    public boolean packageContainsXmlFile(final PackageManager packageManager,
            final String packageName) {
        try (ZipFile apk = new ZipFile(getApkDirectory(packageName, packageManager))) {
            return apk.getEntry(TRANSPARENCY_XML_DIR) != null;
        } catch (final Exception e) {
            Log.d(TAG, e.getMessage());
            return false;
        }
    }

    /**
     * Used to return developer details
     */
    public static class MbaDeveloper {
        public final String name;
        public final String relationship;
        public final String email;
        public final String country;

        public MbaDeveloper(final String name,
                final String relationship,
                final String email,
                final String country) {
            this.name = name;
            this.relationship = relationship;
            this.email = email;
            this.country = country;
        }
    }
}
