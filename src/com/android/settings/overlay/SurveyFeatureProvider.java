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
package com.android.settings.overlay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

/**
 * An interface for classes wishing to provide the ability to serve surveys to implement.
 */
public interface SurveyFeatureProvider {

    /**
     * Downloads a survey asynchronously to shared preferences to be served at a later date.
     *
     * @param activity A valid context.
     * @param surveyId A unique Id representing a survey to download.
     * @param data a text blob to be attached to the survey results.
     */
    void downloadSurvey(Activity activity, String surveyId, @Nullable String data);

    /**
     * Shows a previously downloaded survey/prompt if possible in the activity provided.
     *
     * @param activity The host activity to show the survey in.
     * @param surveyId A unique Id representing a survey to download.
     */
    void showSurveyIfAvailable(Activity activity, String surveyId);

    /**
     * A helper method to get the surveyId. Implementers should create a mapping of
     * keys to surveyIds and provide them via this function.
     *
     * @param context A valid context.
     * @param key The key to get the surveyId for.
     * @return The unique Id as a string or null on error.
     */
    String getSurveyId(Context context, String key);
}
