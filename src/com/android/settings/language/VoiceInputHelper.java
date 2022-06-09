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

package com.android.settings.language;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.provider.Settings;
import android.speech.RecognitionService;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Helper class of the Voice Input setting. */
public final class VoiceInputHelper {
    static final String TAG = "VoiceInputHelper";
    final Context mContext;

    /**
     * Base info of the Voice Input provider.
     *
     * TODO: Remove this superclass as we only have 1 class now (RecognizerInfo).
     * TODO: Group recognition service xml meta-data attributes in a single class.
     */
    public static class BaseInfo implements Comparable<BaseInfo> {
        public final ServiceInfo mService;
        public final ComponentName mComponentName;
        public final String mKey;
        public final ComponentName mSettings;
        public final CharSequence mLabel;
        public final String mLabelStr;
        public final CharSequence mAppLabel;

        public BaseInfo(PackageManager pm, ServiceInfo service, String settings) {
            mService = service;
            mComponentName = new ComponentName(service.packageName, service.name);
            mKey = mComponentName.flattenToShortString();
            mSettings = settings != null
                    ? new ComponentName(service.packageName, settings) : null;
            mLabel = service.loadLabel(pm);
            mLabelStr = mLabel.toString();
            mAppLabel = service.applicationInfo.loadLabel(pm);
        }

        @Override
        public int compareTo(BaseInfo another) {
            return mLabelStr.compareTo(another.mLabelStr);
        }
    }

    /** Info of the speech recognizer (i.e. recognition service). */
    public static class RecognizerInfo extends BaseInfo {
        public final boolean mSelectableAsDefault;

        public RecognizerInfo(PackageManager pm,
                ServiceInfo serviceInfo,
                String settings,
                boolean selectableAsDefault) {
            super(pm, serviceInfo, settings);
            this.mSelectableAsDefault = selectableAsDefault;
        }
    }

    ArrayList<RecognizerInfo> mAvailableRecognizerInfos = new ArrayList<>();

    ComponentName mCurrentRecognizer;

    public VoiceInputHelper(Context context) {
        mContext = context;
    }

    /** Draws the UI of the Voice Input picker page. */
    public void buildUi() {
        // Get the currently selected recognizer from the secure setting.
        String currentSetting = Settings.Secure.getString(
                mContext.getContentResolver(), Settings.Secure.VOICE_RECOGNITION_SERVICE);
        if (currentSetting != null && !currentSetting.isEmpty()) {
            mCurrentRecognizer = ComponentName.unflattenFromString(currentSetting);
        } else {
            mCurrentRecognizer = null;
        }

        final ArrayList<RecognizerInfo> validRecognitionServices =
                validRecognitionServices(mContext);

        // Filter all recognizers which can be selected as default or are the current recognizer.
        mAvailableRecognizerInfos = new ArrayList<>();
        for (RecognizerInfo recognizerInfo: validRecognitionServices) {
            if (recognizerInfo.mSelectableAsDefault || new ComponentName(
                    recognizerInfo.mService.packageName, recognizerInfo.mService.name)
                    .equals(mCurrentRecognizer)) {
                mAvailableRecognizerInfos.add(recognizerInfo);
            }
        }

        Collections.sort(mAvailableRecognizerInfos);
    }

    /**
     * Query all services with {@link RecognitionService#SERVICE_INTERFACE} intent. Filter only
     * those which have proper xml meta-data which start with a `recognition-service` tag.
     * Filtered services are sorted by their labels in the ascending order.
     *
     * @param context {@link Context} inside which the settings app is run.
     *
     * @return {@link ArrayList}&lt;{@link RecognizerInfo}&gt;
     * containing info about the filtered speech recognition services.
     */
    static ArrayList<RecognizerInfo> validRecognitionServices(Context context) {
        final List<ResolveInfo> resolvedRecognitionServices =
                context.getPackageManager().queryIntentServices(
                        new Intent(RecognitionService.SERVICE_INTERFACE),
                        PackageManager.GET_META_DATA);

        final ArrayList<RecognizerInfo> validRecognitionServices = new ArrayList<>();

        for (ResolveInfo resolveInfo: resolvedRecognitionServices) {
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;

            final Pair<String, Boolean> recognitionServiceAttributes =
                    parseRecognitionServiceXmlMetadata(context, serviceInfo);

            if (recognitionServiceAttributes != null) {
                validRecognitionServices.add(new RecognizerInfo(
                        context.getPackageManager(),
                        serviceInfo,
                        recognitionServiceAttributes.first      /* settingsActivity */,
                        recognitionServiceAttributes.second     /* selectableAsDefault */));
            }
        }

        return validRecognitionServices;
    }

    /**
     * Load recognition service's xml meta-data and parse it. Return the meta-data attributes,
     * namely, `settingsActivity` {@link String} and `selectableAsDefault` {@link Boolean}.
     *
     * <p>Parsing fails if the meta-data for the given service is not found
     * or the found meta-data does not start with a `recognition-service`.</p>
     *
     * @param context {@link Context} inside which the settings app is run.
     * @param serviceInfo {@link ServiceInfo} containing info
     * about the speech recognition service in question.
     *
     * @return {@link Pair}&lt;{@link String}, {@link Boolean}&gt;  containing `settingsActivity`
     * and `selectableAsDefault` attributes if the parsing was successful, {@code null} otherwise.
     */
    private static Pair<String, Boolean> parseRecognitionServiceXmlMetadata(
            Context context, ServiceInfo serviceInfo) {
        // Default recognition service attribute values.
        // Every recognizer can be selected unless specified otherwise.
        String settingsActivity;
        boolean selectableAsDefault = true;

        // Parse xml meta-data.
        try (XmlResourceParser parser = serviceInfo.loadXmlMetaData(
                context.getPackageManager(), RecognitionService.SERVICE_META_DATA)) {
            if (parser == null) {
                throw new XmlPullParserException(String.format("No %s meta-data for %s package",
                        RecognitionService.SERVICE_META_DATA, serviceInfo.packageName));
            }

            final Resources res = context.getPackageManager().getResourcesForApplication(
                    serviceInfo.applicationInfo);
            final AttributeSet attrs = Xml.asAttributeSet(parser);

            // Xml meta-data must start with a `recognition-service tag`.
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Intentionally do nothing.
            }

            final String nodeName = parser.getName();
            if (!"recognition-service".equals(nodeName)) {
                throw new XmlPullParserException(String.format(
                        "%s package meta-data does not start with a `recognition-service` tag",
                        serviceInfo.packageName));
            }

            final TypedArray array = res.obtainAttributes(attrs,
                    com.android.internal.R.styleable.RecognitionService);
            settingsActivity = array.getString(
                    com.android.internal.R.styleable.RecognitionService_settingsActivity);
            selectableAsDefault = array.getBoolean(
                    com.android.internal.R.styleable.RecognitionService_selectableAsDefault,
                    selectableAsDefault);
            array.recycle();
        } catch (XmlPullParserException | IOException
                | PackageManager.NameNotFoundException e) {
            Log.e(TAG, String.format("Error parsing %s package recognition service meta-data",
                    serviceInfo.packageName), e);
            return null;
        }

        return Pair.create(settingsActivity, selectableAsDefault);
    }
}
