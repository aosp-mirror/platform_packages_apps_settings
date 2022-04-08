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

package com.android.settings.network.telephony;

import android.util.Log;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Session for controlling the status of TelephonyPreferenceController(s).
 *
 * Within this session, result of {@link BasePreferenceController#availabilityStatus()}
 * would be under control.
 */
public class TelephonyStatusControlSession implements AutoCloseable {

    private static final String LOG_TAG = "TelephonyStatusControlSS";

    private Collection<AbstractPreferenceController> mControllers;
    private Future<Boolean> mResult;

    /**
     * Buider of session
     */
    public static class Builder {
        private Collection<AbstractPreferenceController> mControllers;

        /**
         * Constructor
         *
         * @param controllers is a collection of {@link AbstractPreferenceController}
         *        which would have {@link BasePreferenceController#availabilityStatus()}
         *        under control within this session.
         */
        public Builder(Collection<AbstractPreferenceController> controllers) {
            mControllers = controllers;
        }

        /**
         * Method to build this session.
         * @return {@link TelephonyStatusControlSession} session been setup.
         */
        public TelephonyStatusControlSession build() {
            return new TelephonyStatusControlSession(mControllers);
        }
    }

    private TelephonyStatusControlSession(Collection<AbstractPreferenceController> controllers) {
        mControllers = controllers;
        mResult = ThreadUtils.postOnBackgroundThread(() ->
            setupAvailabilityStatus(controllers)
        );
    }

    /**
     * Close the session.
     *
     * No longer control the status.
     */
    public void close() {
        //check the background thread is finished then unset the status of availability.
        try {
            mResult.get();
        } catch (ExecutionException | InterruptedException exception) {
            Log.e(LOG_TAG, "setup availability status failed!", exception);
        }
        unsetAvailabilityStatus(mControllers);
    }

    private Boolean setupAvailabilityStatus(
            Collection<AbstractPreferenceController> controllerLists) {
        try {
            controllerLists.stream()
                    .filter(controller -> controller instanceof TelephonyAvailabilityHandler)
                    .map(TelephonyAvailabilityHandler.class::cast)
                    .forEach(controller -> {
                        int status = ((BasePreferenceController) controller)
                                .getAvailabilityStatus();
                        controller.setAvailabilityStatus(status);
                    });
            return true;
        } catch (Exception exception) {
            Log.e(LOG_TAG, "Setup availability status failed!", exception);
            return false;
        }
    }

    private void unsetAvailabilityStatus(
            Collection<AbstractPreferenceController> controllerLists) {
        controllerLists.stream()
                .filter(controller -> controller instanceof TelephonyAvailabilityHandler)
                .map(TelephonyAvailabilityHandler.class::cast)
                .forEach(controller -> {
                    controller.unsetAvailabilityStatus();
                });
    }
}
