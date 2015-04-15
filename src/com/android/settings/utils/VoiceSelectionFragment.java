/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.utils;

import android.app.ListFragment;
import android.app.VoiceInteractor;
import android.app.VoiceInteractor.PickOptionRequest;
import android.app.VoiceInteractor.PickOptionRequest.Option;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * An Activity fragment that presents a set of options as a visual list and also allows
 * items to be selected by the users voice.
 */
public class VoiceSelectionFragment extends ListFragment {
    private static final String EXTRA_SELECTION_PROMPT = "selection_prompt";

    private CharSequence mPrompt = null;
    private VoiceInteractor.Request mRequest = null;
    private VoiceInteractor mVoiceInteractor = null;
    private VoiceSelection.OnItemSelectedListener mOnItemSelectedListener = null;

    /**
     * No-args ctor required for fragment.
     */
    public VoiceSelectionFragment() {}

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        mPrompt = getArguments().getCharSequence(EXTRA_SELECTION_PROMPT);
    }

    /**
     * Set the prompt spoken when the fragment is presented.
     */
    static public Bundle createArguments(CharSequence prompt) {
        Bundle args = new Bundle();
        args.putCharSequence(EXTRA_SELECTION_PROMPT, prompt);
        return args;
    }

    private VoiceSelection getSelectionAt(int position) {
        return ((ArrayAdapter<VoiceSelection>) getListAdapter()).getItem(position);
    }

    @Override
    public void onStart() {
        super.onStart();

        final int numItems = getListAdapter().getCount();
        if (numItems <= 0) {
            return;
        }

        Option[] options = new Option[numItems];
        for (int idx = 0; idx < numItems; idx++) {
            options[idx] = getSelectionAt(idx).toOption(idx);
        }
        mRequest = new PickOptionRequest(mPrompt, options, null) {
            @Override
            public void onPickOptionResult(boolean isComplete, Option[] options, Bundle args) {
                if (!isComplete || options == null) {
                    return;
                }
                if (options.length == 1 && mOnItemSelectedListener != null) {
                    int idx = options[0].getExtras().getInt("index", -1);
                    mOnItemSelectedListener.onItemSelected(idx, getSelectionAt(idx));
                } else {
                    onCancel();
                }
            }
        };
        mVoiceInteractor = getActivity().getVoiceInteractor();
        if (mVoiceInteractor != null) {
            mVoiceInteractor.submitRequest(mRequest);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mVoiceInteractor = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mRequest != null) {
            mRequest.cancel();
            mRequest = null;
        }

        if (mOnItemSelectedListener != null) {
            mOnItemSelectedListener.onItemSelected(position, getSelectionAt(position));
        }
    }


    /**
     * Sets the selection handler for an item either by voice or by touch.
     */
    public void setOnItemSelectedHandler(VoiceSelection.OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /**
     * Called when the user cancels the interaction. The default implementation is to
     * finish the activity.
     */
    public void onCancel() {
        getActivity().finish();
    }
};
