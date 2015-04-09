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

package com.android.settings;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.search.Indexable;

import java.util.List;

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SettingsActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {
        private static final int MAX_RETRY_ATTEMPTS = 5;
        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_USAGE_CATEGORY = "fingerprint_usage_category";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_MANAGE_CATEGORY = "fingerprint_manage_category";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_HIGHLIGHT_FINGERPRINT_ITEM = 1001;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        private static final boolean ENABLE_USAGE_CATEGORY = false;
        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private EditText mDialogTextField;
        private PreferenceGroup mManageCategory;
        private CancellationSignal mFingerprintCancel;
        private int mMaxFingerprintAttempts;

        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                mHandler.obtainMessage(MSG_HIGHLIGHT_FINGERPRINT_ITEM,
                        result.getFingerprint().getFingerId(), 0).sendToTarget();
                retryFingerprint(true);
            }

            public void onAuthenticationFailed() {
                retryFingerprint(true);
            };

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT);
                if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    retryFingerprint(false);
                }
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                Toast.makeText(getActivity(), helpString, Toast.LENGTH_SHORT);
            }
        };
        private RemovalCallback mRemoveCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT);
            }
        };
        private final Handler mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                    break;
                    case MSG_HIGHLIGHT_FINGERPRINT_ITEM:
                        highlightFingerprintItem(msg.arg1);
                    break;
                }
            };
        };

        private void stopFingerprint() {
            if (mFingerprintCancel != null) {
                mFingerprintCancel.cancel();
                mFingerprintCancel = null;
            }
        }

        private void retryFingerprint(boolean resetAttempts) {
            if (resetAttempts) {
                mMaxFingerprintAttempts = 0;
            }
            if (mMaxFingerprintAttempts < MAX_RETRY_ATTEMPTS) {
                mFingerprintCancel = new CancellationSignal();
                mFingerprintManager.authenticate(null, mFingerprintCancel, mAuthCallback, 0);
            }
            mMaxFingerprintAttempts++;
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mFingerprintManager = (FingerprintManager) getActivity().getSystemService(
                    Context.FINGERPRINT_SERVICE);
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = mManageCategory.findPreference(name);
            if (prefToRemove != null) {
                if (!mManageCategory.removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            Preference pref = mManageCategory.findPreference(prefName);
            if (pref instanceof FingerprintPreference) {
                final FingerprintPreference fpref = (FingerprintPreference) pref;
                fpref.highlight();
            } else {
                Log.w(TAG, "Wrong pref " + (pref != null ? pref.getKey() : "null"));
            }
        }

        /**
         * Important!
         *
         * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
         * logic or adding/removing preferences here.
         */
        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            root = getPreferenceScreen();

            // Fingerprint items
            mManageCategory = (PreferenceGroup) root.findPreference(KEY_MANAGE_CATEGORY);
            if (mManageCategory != null) {
                addFingerprintItemPreferences(mManageCategory);
            }

            // Fingerprint usage options
            PreferenceGroup usageCategory = (PreferenceGroup) root.findPreference(
                    KEY_USAGE_CATEGORY);
            if (usageCategory != null) {
                Preference toggle = root.findPreference(KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE);
                toggle.setOnPreferenceChangeListener(this);
                if (!ENABLE_USAGE_CATEGORY) {
                    root.removePreference(usageCategory);
                } else {
                    toggle.setOnPreferenceChangeListener(this);
                }
            }

            return root;
        }

        private void addFingerprintItemPreferences(PreferenceGroup manageFingerprintCategory) {
            manageFingerprintCategory.removeAll();
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(
                        manageFingerprintCategory.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                manageFingerprintCategory.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(manageFingerprintCategory.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            addPreference.setTitle(R.string.fingerprint_add_title);
            manageFingerprintCategory.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            createPreferenceHierarchy();
            retryFingerprint(true);
        }

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", FingerprintEnroll.class.getName());
                stopFingerprint();
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp =fpref.getFingerprint();
                showRenameDeleteDialog(pref, fp);
                return super.onPreferenceTreeClick(preferenceScreen, pref);
            }
            return true;
        }

        private void showRenameDeleteDialog(Preference pref, final Fingerprint fp) {
            final Activity activity = getActivity();
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(R.layout.fingerprint_rename_dialog)
                    .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String newName = mDialogTextField.getText().toString();
                                    final CharSequence name = fp.getName();
                                    if (!newName.equals(name)) {
                                        if (DEBUG) Log.v(TAG, "rename " + name + " to " + newName);
                                        mFingerprintManager.rename(fp.getFingerId(), newName);
                                    }
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.security_settings_fingerprint_enroll_dialog_delete,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DEBUG) Log.v(TAG, "Removing fpId=" + fp.getFingerId());
                                    mFingerprintManager.remove(fp, mRemoveCallback);
                                    dialog.dismiss();
                                }
                            })
                    .create();
            dialog.show();
            mDialogTextField = (EditText) dialog.findViewById(R.id.fingerprint_rename_field);
            mDialogTextField.setText(fp.getName());
            mDialogTextField.selectAll();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                // TODO
            } else {
                Log.v(TAG, "Unknown key:" + key);
            }
            return result;
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_security;
        }
    }

    public static class FingerprintPreference extends Preference {
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;
        private Fingerprint mFingerprint;
        private View mView;
        private Drawable mHighlightDrawable;
        private Context mContext;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            mContext = context;
        }
        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            this(context, attrs, com.android.internal.R.attr.preferenceStyle);
        }

        public FingerprintPreference(Context context) {
            this(context, null);
        }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                mHighlightDrawable = mContext.getDrawable(R.drawable.preference_highlight);
            }
            return mHighlightDrawable;
        }

        public void highlight() {
            Drawable highlight = getHighlightDrawable();
            final View view = mView;
            view.setBackground(highlight);
            final int centerX = view.getWidth() / 2;
            final int centerY = view.getHeight() / 2;
            highlight.setHotspot(centerX, centerY);
            view.setPressed(true);
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.setPressed(false);
                    view.setBackground(null);
                }
            }, RESET_HIGHLIGHT_DELAY_MS);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mView = view;
        }
    };
}
