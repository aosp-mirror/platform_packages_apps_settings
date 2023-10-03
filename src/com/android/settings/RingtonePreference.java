/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

/**
 * A {@link Preference} that allows the user to choose a ringtone from those on the device.
 * The chosen ringtone's URI will be persisted as a string.
 * <p>
 * If the user chooses the "Default" item, the saved string will be one of
 * {@link System#DEFAULT_RINGTONE_URI},
 * {@link System#DEFAULT_NOTIFICATION_URI}, or
 * {@link System#DEFAULT_ALARM_ALERT_URI}. If the user chooses the "Silent"
 * item, the saved string will be an empty string.
 *
 * @attr ref android.R.styleable#RingtonePreference_ringtoneType
 * @attr ref android.R.styleable#RingtonePreference_showDefault
 * @attr ref android.R.styleable#RingtonePreference_showSilent
 *
 * Based of frameworks/base/core/java/android/preference/RingtonePreference.java
 * but extends androidx.preference.Preference instead.
 */
public class RingtonePreference extends Preference {

    private static final String TAG = "RingtonePreference";

    private int mRingtoneType;
    private boolean mShowDefault;
    private boolean mShowSilent;

    private int mRequestCode;
    protected int mUserId;
    protected Context mUserContext;

    public RingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.RingtonePreference, 0, 0);
        mRingtoneType = a.getInt(com.android.internal.R.styleable.RingtonePreference_ringtoneType,
                RingtoneManager.TYPE_RINGTONE);
        mShowDefault = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showDefault,
                true);
        mShowSilent = a.getBoolean(com.android.internal.R.styleable.RingtonePreference_showSilent,
                true);
        setIntent(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER));
        setUserId(UserHandle.myUserId());
        a.recycle();
    }

    public void setUserId(int userId) {
        mUserId = userId;
        mUserContext = Utils.createPackageContextAsUser(getContext(), mUserId);
    }

    public int getUserId() {
        return mUserId;
    }

    /**
     * Returns the sound type(s) that are shown in the picker.
     *
     * @return The sound type(s) that are shown in the picker.
     * @see #setRingtoneType(int)
     */
    public int getRingtoneType() {
        return mRingtoneType;
    }

    /**
     * Sets the sound type(s) that are shown in the picker.
     *
     * @param type The sound type(s) that are shown in the picker.
     * @see RingtoneManager#EXTRA_RINGTONE_TYPE
     */
    public void setRingtoneType(int type) {
        mRingtoneType = type;
    }

    /**
     * Returns whether to a show an item for the default sound/ringtone.
     *
     * @return Whether to show an item for the default sound/ringtone.
     */
    public boolean getShowDefault() {
        return mShowDefault;
    }

    /**
     * Sets whether to show an item for the default sound/ringtone. The default
     * to use will be deduced from the sound type(s) being shown.
     *
     * @param showDefault Whether to show the default or not.
     * @see RingtoneManager#EXTRA_RINGTONE_SHOW_DEFAULT
     */
    public void setShowDefault(boolean showDefault) {
        mShowDefault = showDefault;
    }

    /**
     * Returns whether to a show an item for 'Silent'.
     *
     * @return Whether to show an item for 'Silent'.
     */
    public boolean getShowSilent() {
        return mShowSilent;
    }

    /**
     * Sets whether to show an item for 'Silent'.
     *
     * @param showSilent Whether to show 'Silent'.
     * @see RingtoneManager#EXTRA_RINGTONE_SHOW_SILENT
     */
    public void setShowSilent(boolean showSilent) {
        mShowSilent = showSilent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    /**
     * Prepares the intent to launch the ringtone picker. This can be modified
     * to adjust the parameters of the ringtone picker.
     *
     * @param ringtonePickerIntent The ringtone picker intent that can be
     *            modified by putting extras.
     */
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                onRestoreRingtone());

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, mShowDefault);
        if (mShowDefault) {
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(getRingtoneType()));
        }

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, mShowSilent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, mRingtoneType);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
                AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY);
    }

    /**
     * Called when a ringtone is chosen.
     * <p>
     * By default, this saves the ringtone URI to the persistent storage as a
     * string.
     *
     * @param ringtoneUri The chosen ringtone's {@link Uri}. Can be null.
     */
    protected void onSaveRingtone(Uri ringtoneUri) {
        persistString(ringtoneUri != null ? ringtoneUri.toString() : "");
    }

    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     * <p>
     * By default, this restores the previous ringtone URI from the persistent
     * storage.
     *
     * @return The ringtone to be marked as the current ringtone.
     */
    protected Uri onRestoreRingtone() {
        final String uriString = getPersistedString(null);
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObj) {
        String defaultValue = (String) defaultValueObj;

        /*
         * This method is normally to make sure the internal state and UI
         * matches either the persisted value or the default value. Since we
         * don't show the current value in the UI (until the dialog is opened)
         * and we don't keep local state, if we are restoring the persisted
         * value we don't need to do anything.
         */
        if (restorePersistedValue) {
            return;
        }

        // If we are setting to the default value, we should persist it.
        if (!TextUtils.isEmpty(defaultValue)) {
            onSaveRingtone(Uri.parse(defaultValue));
        }
    }
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if (callChangeListener(uri != null ? uri.toString() : "")) {
                onSaveRingtone(uri);
            }
        }

        return true;
    }

    public boolean isDefaultRingtone(Uri ringtoneUri) {
        // null URIs are valid (None/silence)
        return ringtoneUri == null || RingtoneManager.isDefault(ringtoneUri);
    }

    protected boolean isValidRingtoneUri(Uri ringtoneUri) {
        if (isDefaultRingtone(ringtoneUri)) {
            return true;
        }

        // Return early for android resource URIs
        if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(ringtoneUri.getScheme())) {
            return true;
        }

        String mimeType = mUserContext.getContentResolver().getType(ringtoneUri);
        if (mimeType == null) {
            Log.e(TAG, "isValidRingtoneUri for URI:" + ringtoneUri
                    + " failed: failure to find mimeType (no access from this context?)");
            return false;
        }

        if (!(mimeType.startsWith("audio/") || mimeType.equals("application/ogg")
                || mimeType.equals("application/x-flac"))) {
            Log.e(TAG, "isValidRingtoneUri for URI:" + ringtoneUri
                    + " failed: associated mimeType:" + mimeType + " is not an audio type");
            return false;
        }

        // Validate userId from URIs: content://{userId}@...
        final int userIdFromUri = ContentProvider.getUserIdFromUri(ringtoneUri, mUserId);
        if (userIdFromUri != mUserId) {
            final UserManager userManager = mUserContext.getSystemService(UserManager.class);

            if (!userManager.isSameProfileGroup(mUserId, userIdFromUri)) {
                Log.e(TAG,
                    "isValidRingtoneUri for URI:" + ringtoneUri + " failed: user " + userIdFromUri
                        + " and user " + mUserId + " are not in the same profile group");
                return false;
            }

            final int parentUserId;
            final int profileUserId;
            if (userManager.isProfile()) {
                profileUserId = mUserId;
                parentUserId = userIdFromUri;
            } else {
                parentUserId = mUserId;
                profileUserId = userIdFromUri;
            }

            final UserHandle parent = userManager.getProfileParent(UserHandle.of(profileUserId));
            if (parent == null || parent.getIdentifier() != parentUserId) {
                Log.e(TAG,
                    "isValidRingtoneUri for URI:" + ringtoneUri + " failed: user " + profileUserId
                        + " is not a profile of user " + parentUserId);
                return false;
            }

            // Allow parent <-> managed profile sharing, unless restricted
            if (userManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_SHARE_INTO_MANAGED_PROFILE, UserHandle.of(parentUserId))) {
                Log.e(TAG,
                    "isValidRingtoneUri for URI:" + ringtoneUri + " failed: user " + parentUserId
                        + " has restriction: " + UserManager.DISALLOW_SHARE_INTO_MANAGED_PROFILE);
                return false;
            }

            if (!userManager.isManagedProfile(profileUserId)) {
                Log.e(TAG, "isValidRingtoneUri for URI:" + ringtoneUri
                    + " failed: user " + profileUserId + " is not a managed profile");
                return false;
            }
        }

        return true;
    }

}
