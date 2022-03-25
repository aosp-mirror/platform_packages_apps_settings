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

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * Base dialog fragment class with the functionality to make a fragment or an activity as a listener
 * which can survive through the activity restarts.
 */
public abstract class BaseDialogFragment extends DialogFragment {
    // Tags for the listener which receives event callbacks.
    private static final String ARG_LISTENER_TAG = "listener_tag";
    private static final String ARG_IN_CALLER_TAG = "in_caller_tag";

    /**
     * @param activity The caller activity or the activity attached with the fragment.
     * @param listener The original caller, that is, the listener. The listener can be the fragment
     *     to receive event callbacks. If it is null, will use the activity to handle the event
     *     callback instead.
     * @param callbackInterfaceClass The interface that the listener should implements.
     * @param arguments The arguments bundle of the dispatcher fragment used to store the listener's
     *     info.
     * @param tagInCaller An integer given by the listener to distinguish the action when calling
     *     the listener's callback function.
     */
    protected static <T> void setListener(
            Activity activity,
            @Nullable Fragment listener,
            Class<T> callbackInterfaceClass,
            int tagInCaller,
            Bundle arguments) {
        checkValidity(activity, listener, callbackInterfaceClass);

        if (listener != null && listener.getParentFragment() != null) {
            throw new IllegalArgumentException("The listener must be attached to an activity.");
        }
        arguments.putInt(ARG_IN_CALLER_TAG, tagInCaller);
        if (listener != null) {
            arguments.putString(ARG_LISTENER_TAG, listener.getTag());
        }
    }

    /**
     * @param callbackInterfaceClass The interface that the listener should implements.
     * @param <T> Template type.
     * @return The listener class.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getListener(Class<T> callbackInterfaceClass) {
        Object listener;
        String listenerTag = getArguments().getString(ARG_LISTENER_TAG);
        if (listenerTag == null) {
            listener = getActivity();
        } else {
            listener = getActivity().getFragmentManager().findFragmentByTag(listenerTag);
        }
        if (callbackInterfaceClass.isInstance(listener)) {
            return (T) listener;
        }
        throw new IllegalArgumentException("The caller should implement the callback function.");
    }

    /** @return The tag set in the listener. */
    protected int getTagInCaller() {
        return getArguments().getInt(ARG_IN_CALLER_TAG);
    }

    private static <T> void checkValidity(
            Activity activity, @Nullable Fragment listener, Class<T> callbackInterfaceClass) {
        if (listener != null) {
            if (!callbackInterfaceClass.isInstance(listener)) {
                throw new IllegalArgumentException(
                        "The listener fragment should implement the callback function.");
            }
        } else {
            if (!callbackInterfaceClass.isInstance(activity)) {
                throw new IllegalArgumentException(
                        "The caller activity should implement the callback function.");
            }
        }
    }
}
