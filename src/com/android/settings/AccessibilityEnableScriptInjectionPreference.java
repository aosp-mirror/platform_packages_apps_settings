/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Preference for enabling accessibility script injection. It displays a warning
 * dialog before enabling the preference.
 */
public class AccessibilityEnableScriptInjectionPreference extends DialogPreference {

    private boolean mInjectionAllowed;
    private boolean mSendClickAccessibilityEvent;

    public AccessibilityEnableScriptInjectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateSummary();
    }

    public void setInjectionAllowed(boolean injectionAllowed) {
        if (mInjectionAllowed != injectionAllowed) {
            mInjectionAllowed = injectionAllowed;
            persistBoolean(injectionAllowed);
            updateSummary();
        }
    }

    public boolean isInjectionAllowed() {
        return mInjectionAllowed;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View summaryView = view.findViewById(com.android.internal.R.id.summary);
        sendAccessibilityEvent(summaryView);
    }

    private void sendAccessibilityEvent(View view) {
        // Since the view is still not attached we create, populate,
        // and send the event directly since we do not know when it
        // will be attached and posting commands is not as clean.
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getContext());
        if (mSendClickAccessibilityEvent && accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEvent.TYPE_VIEW_CLICKED);
            view.onInitializeAccessibilityEvent(event);
            view.dispatchPopulateAccessibilityEvent(event);
            accessibilityManager.sendAccessibilityEvent(event);
        }
        mSendClickAccessibilityEvent = false;
    }

    @Override
    protected void onClick() {
        if (isInjectionAllowed()) {
            setInjectionAllowed(false);
            // Update the system setting only upon user action.
            setSystemSetting(false);
            mSendClickAccessibilityEvent = true;
        } else {
            super.onClick();
            mSendClickAccessibilityEvent = false;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(index, false);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setInjectionAllowed(restoreValue
                ? getPersistedBoolean(mInjectionAllowed)
                : (Boolean) defaultValue);
    }

    @Override
    protected void onDialogClosed(boolean result) {
        setInjectionAllowed(result);
        if (result) {
            // Update the system setting only upon user action.
            setSystemSetting(true);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        SavedState myState = new SavedState(superState);
        myState.mInjectionAllowed = mInjectionAllowed;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setInjectionAllowed(myState.mInjectionAllowed);
    }

    private void updateSummary() {
        setSummary(mInjectionAllowed
                ? getContext().getString(R.string.accessibility_script_injection_allowed)
                : getContext().getString(R.string.accessibility_script_injection_disallowed));
    }

    private void setSystemSetting(boolean enabled) {
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION, enabled ? 1 : 0);
    }

    private static class SavedState extends BaseSavedState {
        private boolean mInjectionAllowed;

        public SavedState(Parcel source) {
            super(source);
            mInjectionAllowed = (source.readInt() == 1);
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            super.writeToParcel(parcel, flags);
            parcel.writeInt(mInjectionAllowed ? 1 : 0);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("all")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
