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

package com.android.settings.fingerprint;


import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Browser;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.search.Indexable;

import java.util.List;

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings {
    /**
     * Used by the FP settings wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;

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
        private static final int MAX_RETRY_ATTEMPTS = 20;
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private EditText mDialogTextField;
        private CancellationSignal mFingerprintCancel;
        private int mMaxFingerprintAttempts;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private Drawable mHighlightDrawable;

        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
            }

            public void onAuthenticationFailed() {
                mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
            };

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                        .sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                        .sendToTarget();
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
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };
        private final Handler mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                    break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        mFingerprintCancel = null;
                        highlightFingerprintItem(msg.arg1);
                        retryFingerprint(true);
                    break;
                    case MSG_FINGER_AUTH_FAIL:
                        mFingerprintCancel = null;
                        retryFingerprint(true);
                    break;
                    case MSG_FINGER_AUTH_ERROR: {
                        mFingerprintCancel = null;
                        // get activity will be null on a screen rotation
                        final Activity activity = getActivity();
                        if (activity != null) {
                            CharSequence errString = (CharSequence) msg.obj;
                            Toast.makeText(activity, errString , Toast.LENGTH_SHORT);
                        }
                        final int errMsgId = msg.arg1;
                        if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                            retryFingerprint(false);
                        }
                    }
                    break;
                    case MSG_FINGER_AUTH_HELP: {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            CharSequence helpString = (CharSequence) msg.obj;
                            Toast.makeText(activity, helpString , Toast.LENGTH_SHORT);
                        }
                    }
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
            if (mMaxFingerprintAttempts < MAX_RETRY_ATTEMPTS && mFingerprintCancel == null) {
                mFingerprintCancel = new CancellationSignal();
                mFingerprintManager.authenticate(null, mFingerprintCancel, 0 /* flags */,
                        mAuthCallback, null);
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
            if (savedInstanceState != null) {
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
            }

            Activity activity = getActivity();
            mFingerprintManager = (FingerprintManager) activity.getSystemService(
                    Context.FINGERPRINT_SERVICE);

            // Need to authenticate a session token if none
            if (mToken == null && mLaunchedConfirm == false) {
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(
                    R.layout.fingerprint_settings_footer, null);
            v.setText(LearnMoreSpan.linkify(getText(isFingerprintDisabled()
                            ? R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled
                            : R.string.security_settings_fingerprint_enroll_disclaimer),
                    getString(getHelpResource())));
            v.setMovementMethod(new LinkMovementMethod());
            getListView().addFooterView(v);
            getListView().setFooterDividersEnabled(false);
        }

        private boolean isFingerprintDisabled() {
            final DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                if (!getPreferenceScreen().removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
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
            addFingerprintItemPreferences(root);
            return root;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
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
            updatePreferences();
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint(true);
        }

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        FingerprintEnrollEnrolling.class.getName());
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
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
            final AlertDialog dialog = new AlertDialog.Builder(activity)
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
                                        updatePreferences();
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
            // show the IME
            mDialogTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dialog.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
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
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    if (data != null) {
                        mToken = data.getByteArrayExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    }
                }
            }

            if (mToken == null) {
                // Didn't get an authentication, finishing
                getActivity().finish();
            }
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            final Drawable highlight = getHighlightDrawable();
            if (highlight != null) {
                final View view = fpref.getView();
                final int centerX = view.getWidth() / 2;
                final int centerY = view.getHeight() / 2;
                highlight.setHotspot(centerX, centerY);
                view.setBackground(highlight);
                view.setPressed(true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setPressed(false);
                        view.setBackground(null);
                    }
                }, RESET_HIGHLIGHT_DELAY_MS);
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge)) {
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }
    }

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
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

        public View getView() { return mView; }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mView = view;
        }
    };

    private static class LearnMoreSpan extends URLSpan {

        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private LearnMoreSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            try {
                ctx.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w(FingerprintSettingsFragment.TAG,
                        "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
            }
            return builder;
        }
    }
}
