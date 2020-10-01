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

package com.android.settings.core;

import static junit.framework.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableData;
import com.android.settingslib.search.SearchIndexableResources;

import com.google.android.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class UserRestrictionTest {

    private static final String TAG = "UserRestrictionTest";

    private Context mContext;

    private static final Set<String> USER_RESTRICTIONS = Sets.newHashSet(
            UserManager.DISALLOW_CONFIG_DATE_TIME,
            UserManager.DISALLOW_CONFIG_CREDENTIALS,
            UserManager.DISALLOW_NETWORK_RESET,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_CONFIG_TETHERING,
            UserManager.DISALLOW_CONFIG_VPN,
            UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
            UserManager.DISALLOW_AIRPLANE_MODE,
            UserManager.DISALLOW_CONFIG_BRIGHTNESS,
            UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT
    );

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    /**
     * Verity that userRestriction attributes are entered and parsed successfully.
     */
    @Test
    public void userRestrictionAttributeShouldBeValid()
            throws IOException, XmlPullParserException, Resources.NotFoundException {
        final SearchIndexableResources resources =
                FeatureFactory.getFactory(mContext).getSearchFeatureProvider()
                        .getSearchIndexableResources();
        for (SearchIndexableData bundle : resources.getProviderValues()) {
            verifyUserRestriction(bundle);
        }
    }

    private void verifyUserRestriction(SearchIndexableData searchIndexableData)
            throws IOException, XmlPullParserException, Resources.NotFoundException {

        final Indexable.SearchIndexProvider provider =
                searchIndexableData.getSearchIndexProvider();
        final List<SearchIndexableResource> resourcesToIndex =
                provider.getXmlResourcesToIndex(mContext, true);

        final String className = searchIndexableData.getTargetClass().getName();

        if (resourcesToIndex == null) {
            Log.d(TAG, className + "is not providing SearchIndexableResource, skipping");
            return;
        }

        for (SearchIndexableResource sir : resourcesToIndex) {
            if (sir.xmlResId <= 0) {
                Log.d(TAG, className + " doesn't have a valid xml to index.");
                continue;
            }
            final XmlResourceParser parser = mContext.getResources().getXml(sir.xmlResId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            final int outerDepth = parser.getDepth();

            do {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                final String nodeName = parser.getName();
                if (!nodeName.endsWith("Preference")) {
                    continue;
                }
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                final String userRestriction = getDataUserRestrictions(mContext, attrs);
                if (userRestriction != null) {
                    if(!isValidRestriction(userRestriction)) {
                        fail("userRestriction in " + className + " not valid.");
                    }
                }
            } while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth));
        }
    }

    boolean isValidRestriction(String userRestriction) {
        return USER_RESTRICTIONS.contains(userRestriction);
    }

    private String getDataUserRestrictions(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.settingslib.R.styleable.RestrictedPreference,
                com.android.settingslib.R.styleable.RestrictedPreference_userRestriction);
    }

    private String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray ta = context.obtainStyledAttributes(set, attrs);
        String data = ta.getString(resId);
        ta.recycle();
        return data;
    }
}
