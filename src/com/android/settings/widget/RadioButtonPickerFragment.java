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

package com.android.settings.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.RadioButtonPreference;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class RadioButtonPickerFragment extends InstrumentedPreferenceFragment implements
        RadioButtonPreference.OnClickListener {

    @VisibleForTesting
    static final String EXTRA_FOR_WORK = "for_work";
    private static final String TAG = "RadioButtonPckrFrgmt";
    @VisibleForTesting
    boolean mAppendStaticPreferences = false;

    private final Map<String, CandidateInfo> mCandidates = new ArrayMap<>();

    protected UserManager mUserManager;
    protected int mUserId;
    private int mIllustrationId;
    private int mIllustrationPreviewId;
    private VideoPreference mVideoPreference;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        final Bundle arguments = getArguments();

        boolean mForWork = false;
        if (arguments != null) {
            mForWork = arguments.getBoolean(EXTRA_FOR_WORK);
        }
        final UserHandle managedProfile = Utils.getManagedProfile(mUserManager);
        mUserId = mForWork && managedProfile != null
                ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        try {
            // Check if the xml specifies if static preferences should go on the top or bottom
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(getContext(),
                getPreferenceScreenResId(),
                MetadataFlag.FLAG_INCLUDE_PREF_SCREEN |
                MetadataFlag.FLAG_NEED_PREF_APPEND);
            mAppendStaticPreferences = metadata.get(0)
                    .getBoolean(PreferenceXmlParserUtils.METADATA_APPEND);
        } catch (IOException e) {
            Log.e(TAG, "Error trying to open xml file", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing xml", e);
        }
        updateCandidates();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    protected abstract int getPreferenceScreenResId();

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();
        onRadioButtonConfirmed(selectedKey);
    }

    /**
     * Called after the user tries to select an item.
     */
    protected void onSelectionPerformed(boolean success) {
    }

    /**
     * Whether the UI should show a "None" item selection.
     */
    protected boolean shouldShowItemNone() {
        return false;
    }

    /**
     * Populate any static preferences, independent of the radio buttons.
     * These might be used to provide extra information about the choices.
     **/
    protected void addStaticPreferences(PreferenceScreen screen) {
    }

    protected CandidateInfo getCandidate(String key) {
        return mCandidates.get(key);
    }

    protected void onRadioButtonConfirmed(String selectedKey) {
        final boolean success = setDefaultKey(selectedKey);
        if (success) {
            updateCheckedState(selectedKey);
        }
        onSelectionPerformed(success);
    }

    /**
     * A chance for subclasses to bind additional things to the preference.
     */
    public void bindPreferenceExtra(RadioButtonPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
    }

    public void updateCandidates() {
        mCandidates.clear();
        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList != null) {
            for (CandidateInfo info : candidateList) {
                mCandidates.put(info.getKey(), info);
            }
        }
        final String defaultKey = getDefaultKey();
        final String systemDefaultKey = getSystemDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        if (mIllustrationId != 0) {
            addIllustration(screen);
        }
        if (!mAppendStaticPreferences) {
            addStaticPreferences(screen);
        }

        final int customLayoutResId = getRadioButtonPreferenceCustomLayoutResId();
        if (shouldShowItemNone()) {
            final RadioButtonPreference nonePref = new RadioButtonPreference(getPrefContext());
            if (customLayoutResId > 0) {
                nonePref.setLayoutResource(customLayoutResId);
            }
            nonePref.setIcon(R.drawable.ic_remove_circle);
            nonePref.setTitle(R.string.app_list_preference_none);
            nonePref.setChecked(TextUtils.isEmpty(defaultKey));
            nonePref.setOnClickListener(this);
            screen.addPreference(nonePref);
        }
        if (candidateList != null) {
            for (CandidateInfo info : candidateList) {
                RadioButtonPreference pref = new RadioButtonPreference(getPrefContext());
                if (customLayoutResId > 0) {
                    pref.setLayoutResource(customLayoutResId);
                }
                bindPreference(pref, info.getKey(), info, defaultKey);
                bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
                screen.addPreference(pref);
            }
        }
        mayCheckOnlyRadioButton();
        if (mAppendStaticPreferences) {
            addStaticPreferences(screen);
        }
    }

    public RadioButtonPreference bindPreference(RadioButtonPreference pref,
            String key, CandidateInfo info, String defaultKey) {
        pref.setTitle(info.loadLabel());
        Utils.setSafeIcon(pref, info.loadIcon());
        pref.setKey(key);
        if (TextUtils.equals(defaultKey, key)) {
            pref.setChecked(true);
        }
        pref.setEnabled(info.enabled);
        pref.setOnClickListener(this);
        return pref;
    }

    public void updateCheckedState(String selectedKey) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            final int count = screen.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                final Preference pref = screen.getPreference(i);
                if (pref instanceof RadioButtonPreference) {
                    final RadioButtonPreference radioPref = (RadioButtonPreference) pref;
                    final boolean newCheckedState = TextUtils.equals(pref.getKey(), selectedKey);
                    if (radioPref.isChecked() != newCheckedState) {
                        radioPref.setChecked(TextUtils.equals(pref.getKey(), selectedKey));
                    }
                }
            }
        }
    }

    public void mayCheckOnlyRadioButton() {
        final PreferenceScreen screen = getPreferenceScreen();
        // If there is only 1 thing on screen, select it.
        if (screen != null && screen.getPreferenceCount() == 1) {
            final Preference onlyPref = screen.getPreference(0);
            if (onlyPref instanceof RadioButtonPreference) {
                ((RadioButtonPreference) onlyPref).setChecked(true);
            }
        }
    }

    /**
     * Allows you to set an illustration at the top of this screen. Set the illustration id to 0
     * if you want to remove the illustration.
     * @param illustrationId The res id for the raw of the illustration.
     * @param previewId The res id for the drawable of the illustration
     */
    protected void setIllustration(int illustrationId, int previewId) {
        mIllustrationId = illustrationId;
        mIllustrationPreviewId = previewId;
    }

    private void addIllustration(PreferenceScreen screen) {
        mVideoPreference = new VideoPreference(getContext());
        mVideoPreference.setVideo(mIllustrationId, mIllustrationPreviewId);
        screen.addPreference(mVideoPreference);
    }

    protected abstract List<? extends CandidateInfo> getCandidates();

    protected abstract String getDefaultKey();

    protected abstract boolean setDefaultKey(String key);

    protected String getSystemDefaultKey() {
        return null;
    }

    /**
     * Provides a custom layout for each candidate row.
     */
    @LayoutRes
    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return 0;
    }

}
