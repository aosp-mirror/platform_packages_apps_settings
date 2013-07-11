/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.inputmethod;

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.inputmethod.InputMethodUtils.InputMethodSettings;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is a wrapper for InputMethodSettings. You need to refresh internal states
 * manually on some events when "InputMethodInfo"s and "InputMethodSubtype"s can be
 * changed.
 */
public class InputMethodSettingValuesWrapper {
    private static final String TAG = InputMethodSettingValuesWrapper.class.getSimpleName();
    private static volatile InputMethodSettingValuesWrapper sInstance;
    private final ArrayList<InputMethodInfo> mMethodList = new ArrayList<InputMethodInfo>();
    private final HashMap<String, InputMethodInfo> mMethodMap =
            new HashMap<String, InputMethodInfo>();
    private final InputMethodSettings mSettings;
    private final InputMethodManager mImm;

    public static InputMethodSettingValuesWrapper getInstance(Context context) {
        if (sInstance == null) {
            synchronized(TAG) {
                if (sInstance == null) {
                    sInstance = new InputMethodSettingValuesWrapper(context);
                }
            }
        }
        return sInstance;
    }

    private static int getDefaultCurrentUserId() {
        try {
            return ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        return 0;
    }

    // Ensure singleton
    private InputMethodSettingValuesWrapper(Context context) {
        mSettings =
                new InputMethodSettings(context.getResources(), context.getContentResolver(),
                        mMethodMap, mMethodList, getDefaultCurrentUserId());
        mImm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        refreshAllInputMethodAndSubtypes();
    }

    public void refreshAllInputMethodAndSubtypes() {
        synchronized (mMethodMap) {
            mMethodList.clear();
            mMethodMap.clear();
            final List<InputMethodInfo> imms = mImm.getInputMethodList();
            mMethodList.addAll(imms);
            for (InputMethodInfo imi : imms) {
                mMethodMap.put(imi.getId(), imi);
            }
        }
    }

    public List<InputMethodInfo> getInputMethodList() {
        synchronized (mMethodMap) {
            return mMethodList;
        }
    }

    public CharSequence getCurrentInputMethodName(Context context) {
        synchronized (mMethodMap) {
            final InputMethodInfo imi = mMethodMap.get(mSettings.getSelectedInputMethod());
            if (imi == null) {
                Log.w(TAG, "Invalid selected imi: " + mSettings.getSelectedInputMethod());
                return "";
            }
            final InputMethodSubtype subtype = mImm.getCurrentInputMethodSubtype();
            return InputMethodUtils.getImeAndSubtypeDisplayName(context, imi, subtype);
        }
    }
}
