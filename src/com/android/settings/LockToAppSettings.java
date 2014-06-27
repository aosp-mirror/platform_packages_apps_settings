/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Location access settings.
 */
public class LockToAppSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Indexable {
    private static final String TAG = "LockToAppSettings";

    private static final CharSequence KEY_USE_SCREEN_LOCK = "use_screen_lock";
    private static final int CHANGE_LOCK_METHOD_REQUEST = 123;

    private SwitchBar mSwitchBar;
    private SwitchPreference mUseScreenLock;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLockToAppEnabled()) {
            mSwitchBar.setChecked(true);
            createPreferenceHierarchy();
        } else {
            mSwitchBar.setChecked(false);
            createSetupInstructions();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lock_to_app_instructions, null);
    }

    private boolean isLockToAppEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED)
                != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private boolean isLockScreenEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCK_TO_APP_EXIT_LOCKED) != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void setLockToAppEnabled(boolean isEnabled) {
        Settings.System.putInt(getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED,
                isEnabled ? 1 : 0);
    }

    private void setUseLockScreen(boolean useLockScreen) {
        if (useLockScreen) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            if (lockPatternUtils.getKeyguardStoredPasswordQuality()
                    == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                chooseLockIntent.putExtra(
                        ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                startActivityForResult(chooseLockIntent, CHANGE_LOCK_METHOD_REQUEST);
                return;
            }
        }
        setUseLockScreenSetting(useLockScreen);
    }

    private void setUseLockScreenSetting(boolean useLockScreen) {
        mUseScreenLock.setChecked(useLockScreen);
        Settings.System.putInt(getContentResolver(), Settings.System.LOCK_TO_APP_EXIT_LOCKED,
                useLockScreen ? 1 : 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_LOCK_METHOD_REQUEST) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            setUseLockScreenSetting(lockPatternUtils.getKeyguardStoredPasswordQuality()
                    != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        getView().findViewById(R.id.instructions_area).setVisibility(View.INVISIBLE);
        addPreferencesFromResource(R.xml.lock_to_app_settings);
        root = getPreferenceScreen();

        mUseScreenLock = (SwitchPreference) root.findPreference(KEY_USE_SCREEN_LOCK);
        mUseScreenLock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setUseLockScreen((boolean) newValue);
                return true;
            }
        });
        mUseScreenLock.setChecked(isLockScreenEnabled());

        return root;
    }

    private void createSetupInstructions() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        final View view = getView();
        view.findViewById(R.id.instructions_area).setVisibility(View.VISIBLE);

        final Resources r = Resources.getSystem();
        TextView tv = (TextView) view.findViewById(R.id.lock_to_app_start_step3);
        ImageSpan imageSpan = new ImageSpan(getActivity(),
                BitmapFactory.decodeResource(r, com.android.internal.R.drawable.ic_recent),
                DynamicDrawableSpan.ALIGN_BOTTOM);

        String descriptionString = getResources().getString(R.string.lock_to_app_start_step3);
        SpannableString description =
                new SpannableString(descriptionString.replace('$', ' '));
        int index = descriptionString.indexOf('$');
        if (index >= 0) {
            description.setSpan(imageSpan, index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Make icon fit.
        float width = imageSpan.getDrawable().getIntrinsicWidth();
        float height = imageSpan.getDrawable().getIntrinsicHeight();
        int lineHeight = tv.getLineHeight();
        imageSpan.getDrawable().setBounds(0, 0, (int) (lineHeight * width / height), lineHeight);

        tv.setText(description);
    }

    /**
     * Listens to the state change of the lock-to-app master switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setLockToAppEnabled(isChecked);
        if (isChecked) {
            createPreferenceHierarchy();
        } else {
            createSetupInstructions();
        }
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.lock_to_app_title);
                data.screenTitle = res.getString(R.string.lock_to_app_title);
                result.add(data);

                // Lock-to-app description.
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.lock_to_app_description);
                data.screenTitle = res.getString(R.string.lock_to_app_title);
                result.add(data);

                // Lock-to-app use screen lock.
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.lock_to_app_screen_lock);
                data.screenTitle = res.getString(R.string.lock_to_app_title);
                result.add(data);

                return result;
            }
        };
}
