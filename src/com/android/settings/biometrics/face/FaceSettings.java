/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static android.app.Activity.RESULT_OK;

import static com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST;
import static com.android.settings.biometrics.BiometricEnrollBase.ENROLL_REQUEST;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings screen for face authentication.
 */
@SearchIndexable
public class FaceSettings extends DashboardFragment {

    private static final String TAG = "FaceSettings";
    private static final String KEY_TOKEN = "hw_auth_token";

    private static final String PREF_KEY_DELETE_FACE_DATA =
            "security_settings_face_delete_faces_container";
    private static final String PREF_KEY_ENROLL_FACE_UNLOCK =
            "security_settings_face_enroll_faces_container";

    private UserManager mUserManager;
    private FaceManager mFaceManager;
    private int mUserId;
    private byte[] mToken;
    private FaceSettingsAttentionPreferenceController mAttentionController;
    private FaceSettingsRemoveButtonPreferenceController mRemoveController;
    private FaceSettingsEnrollButtonPreferenceController mEnrollController;
    private FaceSettingsLockscreenBypassPreferenceController mLockscreenController;
    private List<AbstractPreferenceController> mControllers;

    private List<Preference> mTogglePreferences;
    private Preference mRemoveButton;
    private Preference mEnrollButton;
    private FaceFeatureProvider mFaceFeatureProvider;

    private boolean mConfirmingPassword;

    private final FaceSettingsRemoveButtonPreferenceController.Listener mRemovalListener = () -> {

        // Disable the toggles until the user re-enrolls
        for (Preference preference : mTogglePreferences) {
            preference.setEnabled(false);
        }

        // Hide the "remove" button and show the "set up face authentication" button.
        mRemoveButton.setVisible(false);
        mEnrollButton.setVisible(true);
    };

    private final FaceSettingsEnrollButtonPreferenceController.Listener mEnrollListener = intent ->
            startActivityForResult(intent, ENROLL_REQUEST);

    /**
     * @param context
     * @return true if the Face hardware is detected.
     */
    public static boolean isFaceHardwareDetected(Context context) {
        FaceManager manager = Utils.getFaceManagerOrNull(context);
        boolean isHardwareDetected = false;
        if (manager == null) {
            Log.d(TAG, "FaceManager is null");
        } else {
            isHardwareDetected = manager.isHardwareDetected();
            Log.d(TAG, "FaceManager is not null. Hardware detected: " + isHardwareDetected);
        }
        return manager != null && isHardwareDetected;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_face;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(KEY_TOKEN, mToken);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getPrefContext();
        if (!isFaceHardwareDetected(context)) {
            Log.w(TAG, "no faceManager, finish this");
            finish();
            return;
        }

        mUserManager = context.getSystemService(UserManager.class);
        mFaceManager = context.getSystemService(FaceManager.class);
        mToken = getIntent().getByteArrayExtra(KEY_TOKEN);

        mUserId = getActivity().getIntent().getIntExtra(
                Intent.EXTRA_USER_ID, UserHandle.myUserId());
        mFaceFeatureProvider = FeatureFactory.getFactory(getContext()).getFaceFeatureProvider();

        if (mUserManager.getUserInfo(mUserId).isManagedProfile()) {
            getActivity().setTitle(getActivity().getResources().getString(
                    R.string.security_settings_face_profile_preference_title));
        }

        Preference keyguardPref = findPreference(FaceSettingsKeyguardPreferenceController.KEY);
        Preference appPref = findPreference(FaceSettingsAppPreferenceController.KEY);
        Preference attentionPref = findPreference(FaceSettingsAttentionPreferenceController.KEY);
        Preference confirmPref = findPreference(FaceSettingsConfirmPreferenceController.KEY);
        Preference bypassPref =
                findPreference(mLockscreenController.getPreferenceKey());
        mTogglePreferences = new ArrayList<>(
                Arrays.asList(keyguardPref, appPref, attentionPref, confirmPref, bypassPref));

        mRemoveButton = findPreference(FaceSettingsRemoveButtonPreferenceController.KEY);
        mEnrollButton = findPreference(FaceSettingsEnrollButtonPreferenceController.KEY);

        // There is no better way to do this :/
        for (AbstractPreferenceController controller : mControllers) {
            if (controller instanceof FaceSettingsPreferenceController) {
                ((FaceSettingsPreferenceController) controller).setUserId(mUserId);
            } else if (controller instanceof FaceSettingsEnrollButtonPreferenceController) {
                ((FaceSettingsEnrollButtonPreferenceController) controller).setUserId(mUserId);
            }
        }
        mRemoveController.setUserId(mUserId);

        // Don't show keyguard controller for work profile settings.
        if (mUserManager.isManagedProfile(mUserId)) {
            removePreference(FaceSettingsKeyguardPreferenceController.KEY);
            removePreference(mLockscreenController.getPreferenceKey());
        }

        if (savedInstanceState != null) {
            mToken = savedInstanceState.getByteArray(KEY_TOKEN);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mLockscreenController = use(FaceSettingsLockscreenBypassPreferenceController.class);
        mLockscreenController.setUserId(mUserId);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mToken == null && !mConfirmingPassword) {
            // Generate challenge in onResume instead of onCreate, since FaceSettings can be
            // created while Keyguard is showing, in which case the resetLockout revokeChallenge
            // will invalidate the too-early created challenge here.
            final long challenge = mFaceManager.generateChallenge();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);

            mConfirmingPassword = true;
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_face_preference_title),
                    null, null, challenge, mUserId, true /* foregroundOnly */)) {
                Log.e(TAG, "Password not set");
                finish();
            }
        } else {
            mAttentionController.setToken(mToken);
            mEnrollController.setToken(mToken);
        }

        final boolean hasEnrolled = mFaceManager.hasEnrolledTemplates(mUserId);
        mEnrollButton.setVisible(!hasEnrolled);
        mRemoveButton.setVisible(hasEnrolled);

        if (!mFaceFeatureProvider.isAttentionSupported(getContext())) {
            removePreference(FaceSettingsAttentionPreferenceController.KEY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_REQUEST) {
            mConfirmingPassword = false;
            if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                mFaceManager.setActiveUser(mUserId);
                // The pin/pattern/password was set.
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    if (mToken != null) {
                        mAttentionController.setToken(mToken);
                        mEnrollController.setToken(mToken);
                    }
                }
            }
        } else if (requestCode == ENROLL_REQUEST) {
            if (resultCode == RESULT_TIMEOUT) {
                setResult(resultCode, data);
                finish();
            }
        }

        if (mToken == null) {
            // Didn't get an authentication, finishing
            finish();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!mEnrollController.isClicked() && !getActivity().isChangingConfigurations()
                && !mConfirmingPassword) {
            // Revoke challenge and finish
            if (mToken != null) {
                final int result = mFaceManager.revokeChallenge();
                if (result < 0) {
                    Log.w(TAG, "revokeChallenge failed, result: " + result);
                }
                mToken = null;
            }
            finish();
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_face;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (!isFaceHardwareDetected(context)) {
            return null;
        }
        mControllers = buildPreferenceControllers(context, getSettingsLifecycle());
        // There's no great way of doing this right now :/
        for (AbstractPreferenceController controller : mControllers) {
            if (controller instanceof FaceSettingsAttentionPreferenceController) {
                mAttentionController = (FaceSettingsAttentionPreferenceController) controller;
            } else if (controller instanceof FaceSettingsRemoveButtonPreferenceController) {
                mRemoveController = (FaceSettingsRemoveButtonPreferenceController) controller;
                mRemoveController.setListener(mRemovalListener);
                mRemoveController.setActivity((SettingsActivity) getActivity());
            } else if (controller instanceof FaceSettingsEnrollButtonPreferenceController) {
                mEnrollController = (FaceSettingsEnrollButtonPreferenceController) controller;
                mEnrollController.setListener(mEnrollListener);
                mEnrollController.setActivity((SettingsActivity) getActivity());
            }
        }

        return mControllers;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new FaceSettingsKeyguardPreferenceController(context));
        controllers.add(new FaceSettingsAppPreferenceController(context));
        controllers.add(new FaceSettingsAttentionPreferenceController(context));
        controllers.add(new FaceSettingsRemoveButtonPreferenceController(context));
        controllers.add(new FaceSettingsFooterPreferenceController(context));
        controllers.add(new FaceSettingsConfirmPreferenceController(context));
        controllers.add(new FaceSettingsEnrollButtonPreferenceController(context));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.security_settings_face) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    if (isFaceHardwareDetected(context)) {
                        return buildPreferenceControllers(context, null /* lifecycle */);
                    } else {
                        return null;
                    }
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (isFaceHardwareDetected(context)) {
                        return hasEnrolledBiometrics(context);
                    }

                    return false;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    final boolean isFaceHardwareDetected = isFaceHardwareDetected(context);
                    Log.d(TAG, "Get non indexable keys. isFaceHardwareDetected: "
                            + isFaceHardwareDetected + ", size:" + keys.size());
                    if (isFaceHardwareDetected) {
                        final boolean hasEnrolled = hasEnrolledBiometrics(context);
                        keys.add(hasEnrolled ? PREF_KEY_ENROLL_FACE_UNLOCK
                                : PREF_KEY_DELETE_FACE_DATA);
                    }

                    if (!isAttentionSupported(context)) {
                        keys.add(FaceSettingsAttentionPreferenceController.KEY);
                    }

                    return keys;
                }

                private boolean isAttentionSupported(Context context) {
                    FaceFeatureProvider featureProvider = FeatureFactory.getFactory(
                            context).getFaceFeatureProvider();
                    boolean isAttentionSupported = false;
                    if (featureProvider != null) {
                        isAttentionSupported = featureProvider.isAttentionSupported(context);
                    }
                    return isAttentionSupported;
                }

                private boolean hasEnrolledBiometrics(Context context) {
                    final FaceManager faceManager = Utils.getFaceManagerOrNull(context);
                    if (faceManager != null) {
                        return faceManager.hasEnrolledTemplates(UserHandle.myUserId());
                    }
                    return false;
                }
            };
}
