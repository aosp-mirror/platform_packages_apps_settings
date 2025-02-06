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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * An interface for classes wishing to provide the ability to serve surveys to implement.
 */
public interface SurveyFeatureProvider {
    /**
     * Downloads a survey asynchronously to shared preferences to be served at a later date.
     *
     * @param activity A valid context.
     * @param surveyId A unique Id representing a survey to download.
     * @param data     a text blob to be attached to the survey results.
     * @deprecated This is not used after T.
     */
    @Deprecated
    void downloadSurvey(Activity activity, String surveyId, @Nullable String data);

    /**
     * Shows a previously downloaded survey/prompt if possible in the activity provided.
     *
     * @param activity The host activity to show the survey in.
     * @param surveyId A unique Id representing a survey to download.
     * @return A boolean indicating if a survey was shown or not.
     * @deprecated This is not used after T.
     */
    @Deprecated
    boolean showSurveyIfAvailable(Activity activity, String surveyId);

    /**
     * A helper method to get the surveyId. Implementers should create a mapping of
     * keys to surveyIds and provide them via this function.
     *
     * @param context   A valid context.
     * @param simpleKey The simple name of the key to get the surveyId for.
     * @return The unique Id as a string or null on error.
     * @deprecated This is not used after T.
     */
    @Deprecated
    String getSurveyId(Context context, String simpleKey);

    /**
     * Removes the survey for {@code siteId} if it expired, then returns the expiration date (as a
     * unix timestamp) for the remaining survey should it exist and be ready to show. Returns -1 if
     * no valid survey exists after removing the potentially expired one.
     *
     * @param context  the calling context.
     * @param surveyId the site ID.
     * @return the unix timestamp for the available survey for the given {@coe siteId} or -1 if
     * there is none available.
     * @deprecated This is not used after T.
     */
    @Deprecated
    long getSurveyExpirationDate(Context context, String surveyId);

    /**
     * Registers an activity to show surveys/prompts as soon as they are downloaded. The receiver
     * should be unregistered prior to destroying the activity to avoid undefined behavior by
     * calling {@link #unregisterReceiver(Activity, BroadcastReceiver)}.
     *
     * @param activity The activity that should show surveys once they are downloaded.
     * @return the broadcast receiver listening for survey downloads. Must be unregistered before
     * leaving the activity.
     * @deprecated This is not used after T.
     */
    @Deprecated
    BroadcastReceiver createAndRegisterReceiver(Activity activity);

    /**
     * Unregisters the broadcast receiver for this activity. Should only be called once per activity
     * after a call to {@link #createAndRegisterReceiver(Activity)}.
     *
     * @param activity The activity that was used to register the BroadcastReceiver.
     * @deprecated This is not used after T.
     */
    @Deprecated
    static void unregisterReceiver(Activity activity, BroadcastReceiver receiver) {
        if (activity == null) {
            throw new IllegalStateException("Cannot unregister receiver if activity is null");
        }

        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
    }

    /**
     * Send the visited activity to the place where it will trigger a survey if possible.
     *
     * @param simpleKey The simple name of the key to get the surveyId for.
     */
    void sendActivityIfAvailable(String simpleKey);

    /**
     * Checks if a survey is available for the given key by binding to the survey service.
     *
     * @param lifecycleOwner The lifecycle owner to manage the service connection.
     * @param simpleKey The simple name of the key to get the surveyId for.
     * @param listener The callback to be invoked when the survey availability is checked.
     */
    void checkSurveyAvailable(@NonNull LifecycleOwner lifecycleOwner, @NonNull String simpleKey,
            @NonNull Consumer<Boolean> listener);
}
