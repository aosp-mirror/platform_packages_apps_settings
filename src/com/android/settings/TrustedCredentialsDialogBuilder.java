/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.NonNull;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.net.http.SslCertificate;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsSettings.CertHolder;
import com.android.settingslib.RestrictedLockUtils;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

class TrustedCredentialsDialogBuilder extends AlertDialog.Builder {
    public interface DelegateInterface {
        List<X509Certificate> getX509CertsFromCertHolder(CertHolder certHolder);
        void removeOrInstallCert(CertHolder certHolder);
        boolean startConfirmCredentialIfNotConfirmed(int userId,
                IntConsumer onCredentialConfirmedListener);
    }

    private final DialogEventHandler mDialogEventHandler;

    public TrustedCredentialsDialogBuilder(Activity activity, DelegateInterface delegate) {
        super(activity);
        mDialogEventHandler = new DialogEventHandler(activity, delegate);

        initDefaultBuilderParams();
    }

    public TrustedCredentialsDialogBuilder setCertHolder(CertHolder certHolder) {
        return setCertHolders(certHolder == null ? new CertHolder[0]
                : new CertHolder[]{certHolder});
    }

    public TrustedCredentialsDialogBuilder setCertHolders(@NonNull CertHolder[] certHolders) {
        mDialogEventHandler.setCertHolders(certHolders);
        return this;
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(mDialogEventHandler);
        mDialogEventHandler.setDialog(dialog);
        return dialog;
    }

    private void initDefaultBuilderParams() {
        setTitle(com.android.internal.R.string.ssl_certificate);
        setView(mDialogEventHandler.mRootContainer);

        // Enable buttons here. The actual labels and listeners are configured in nextOrDismiss
        setPositiveButton(R.string.trusted_credentials_trust_label, null);
        setNegativeButton(android.R.string.ok, null);
    }

    private static class DialogEventHandler implements DialogInterface.OnShowListener,
            View.OnClickListener  {
        private static final long OUT_DURATION_MS = 300;
        private static final long IN_DURATION_MS = 200;

        private final Activity mActivity;
        private final DevicePolicyManager mDpm;
        private final UserManager mUserManager;
        private final DelegateInterface mDelegate;
        private final LinearLayout mRootContainer;

        private int mCurrentCertIndex = -1;
        private AlertDialog mDialog;
        private Button mPositiveButton;
        private Button mNegativeButton;
        private boolean mNeedsApproval;
        private CertHolder[] mCertHolders = new CertHolder[0];
        private View mCurrentCertLayout = null;

        public DialogEventHandler(Activity activity, DelegateInterface delegate) {
            mActivity = activity;
            mDpm = activity.getSystemService(DevicePolicyManager.class);
            mUserManager = activity.getSystemService(UserManager.class);
            mDelegate = delegate;

            mRootContainer = new LinearLayout(mActivity);
            mRootContainer.setOrientation(LinearLayout.VERTICAL);
        }

        public void setDialog(AlertDialog dialog) {
            mDialog = dialog;
        }

        public void setCertHolders(CertHolder[] certHolder) {
            mCertHolders = certHolder;
        }

        @Override
        public void onShow(DialogInterface dialogInterface) {
            // Config the display content only when the dialog is shown because the
            // positive/negative buttons don't exist until the dialog is shown
            nextOrDismiss();
        }

        @Override
        public void onClick(View view) {
            if (view == mPositiveButton) {
                if (mNeedsApproval) {
                    onClickTrust();
                } else {
                    onClickOk();
                }
            } else if (view == mNegativeButton) {
                onClickEnableOrDisable();
            }
        }

        private void onClickOk() {
            nextOrDismiss();
        }

        private void onClickTrust() {
            CertHolder certHolder = getCurrentCertInfo();
            if (!mDelegate.startConfirmCredentialIfNotConfirmed(certHolder.getUserId(),
                    this::onCredentialConfirmed)) {
                mDpm.approveCaCert(certHolder.getAlias(), certHolder.getUserId(), true);
                nextOrDismiss();
            }
        }

        private void onClickEnableOrDisable() {
            final CertHolder certHolder = getCurrentCertInfo();
            DialogInterface.OnClickListener onConfirm = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    mDelegate.removeOrInstallCert(certHolder);
                    nextOrDismiss();
                }
            };
            if (certHolder.isSystemCert()) {
                // Removing system certs is reversible, so skip confirmation.
                onConfirm.onClick(null, -1);
            } else {
                new AlertDialog.Builder(mActivity)
                        .setMessage(R.string.trusted_credentials_remove_confirmation)
                        .setPositiveButton(android.R.string.yes, onConfirm)
                        .setNegativeButton(android.R.string.no, null)
                        .show();

            }
        }

        private void onCredentialConfirmed(int userId) {
            if (mDialog.isShowing() && mNeedsApproval && getCurrentCertInfo() != null
                    && getCurrentCertInfo().getUserId() == userId) {
                // Treat it as user just clicks "trust" for this cert
                onClickTrust();
            }
        }

        private CertHolder getCurrentCertInfo() {
            return mCurrentCertIndex < mCertHolders.length ? mCertHolders[mCurrentCertIndex] : null;
        }

        private void nextOrDismiss() {
            mCurrentCertIndex++;
            // find next non-null cert or dismiss
            while (mCurrentCertIndex < mCertHolders.length && getCurrentCertInfo() == null) {
                mCurrentCertIndex++;
            }

            if (mCurrentCertIndex >= mCertHolders.length) {
                mDialog.dismiss();
                return;
            }

            updateViewContainer();
            updatePositiveButton();
            updateNegativeButton();
        }

        /**
         * @return true if current user or parent user is guarded by screenlock
         */
        private boolean isUserSecure(int userId) {
            final LockPatternUtils lockPatternUtils = new LockPatternUtils(mActivity);
            if (lockPatternUtils.isSecure(userId)) {
                return true;
            }
            UserInfo parentUser = mUserManager.getProfileParent(userId);
            if (parentUser == null) {
                return false;
            }
            return lockPatternUtils.isSecure(parentUser.id);
        }

        private void updatePositiveButton() {
            final CertHolder certHolder = getCurrentCertInfo();
            mNeedsApproval = !certHolder.isSystemCert()
                    && isUserSecure(certHolder.getUserId())
                    && !mDpm.isCaCertApproved(certHolder.getAlias(), certHolder.getUserId());

            final boolean isProfileOrDeviceOwner = RestrictedLockUtils.getProfileOrDeviceOwner(
                    mActivity, UserHandle.of(certHolder.getUserId())) != null;

            // Show trust button only when it requires consumer user (non-PO/DO) to approve
            CharSequence displayText = mActivity.getText(!isProfileOrDeviceOwner && mNeedsApproval
                    ? R.string.trusted_credentials_trust_label
                    : android.R.string.ok);
            mPositiveButton = updateButton(DialogInterface.BUTTON_POSITIVE, displayText);
        }

        private void updateNegativeButton() {
            final CertHolder certHolder = getCurrentCertInfo();
            final boolean showRemoveButton = !mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS,
                    new UserHandle(certHolder.getUserId()));
            CharSequence displayText = mActivity.getText(getButtonLabel(certHolder));
            mNegativeButton = updateButton(DialogInterface.BUTTON_NEGATIVE, displayText);
            mNegativeButton.setVisibility(showRemoveButton ? View.VISIBLE : View.GONE);
        }

        /**
         * mDialog.setButton doesn't trigger text refresh since mDialog has been shown.
         * It's invoked only in case mDialog is refreshed.
         * setOnClickListener is invoked to avoid dismiss dialog onClick
         */
        private Button updateButton(int buttonType, CharSequence displayText) {
            mDialog.setButton(buttonType, displayText, (DialogInterface.OnClickListener) null);
            Button button = mDialog.getButton(buttonType);
            button.setText(displayText);
            button.setOnClickListener(this);
            return button;
        }


        private void updateViewContainer() {
            CertHolder certHolder = getCurrentCertInfo();
            LinearLayout nextCertLayout = getCertLayout(certHolder);

            // Displaying first cert doesn't require animation
            if (mCurrentCertLayout == null) {
                mCurrentCertLayout = nextCertLayout;
                mRootContainer.addView(mCurrentCertLayout);
            } else {
                animateViewTransition(nextCertLayout);
            }
        }

        private LinearLayout getCertLayout(final CertHolder certHolder) {
            final ArrayList<View> views =  new ArrayList<View>();
            final ArrayList<String> titles = new ArrayList<String>();
            List<X509Certificate> certificates = mDelegate.getX509CertsFromCertHolder(certHolder);
            if (certificates != null) {
                for (X509Certificate certificate : certificates) {
                    SslCertificate sslCert = new SslCertificate(certificate);
                    views.add(sslCert.inflateCertificateView(mActivity));
                    titles.add(sslCert.getIssuedTo().getCName());
                }
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mActivity,
                    android.R.layout.simple_spinner_item,
                    titles);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = new Spinner(mActivity);
            spinner.setAdapter(arrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    for (int i = 0; i < views.size(); i++) {
                        views.get(i).setVisibility(i == position ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            LinearLayout certLayout = new LinearLayout(mActivity);
            certLayout.setOrientation(LinearLayout.VERTICAL);
            certLayout.addView(spinner);
            for (int i = 0; i < views.size(); ++i) {
                View certificateView = views.get(i);
                // Show first cert by default
                certificateView.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
                certLayout.addView(certificateView);
            }

            return certLayout;
        }

        private static int getButtonLabel(CertHolder certHolder) {
            return certHolder.isSystemCert() ? ( certHolder.isDeleted()
                        ? R.string.trusted_credentials_enable_label
                        : R.string.trusted_credentials_disable_label )
                    : R.string.trusted_credentials_remove_label;
        }

        /* Animation code */
        private void animateViewTransition(final View nextCertView) {
            animateOldContent(new Runnable() {
                @Override
                public void run() {
                    addAndAnimateNewContent(nextCertView);
                }
            });
        }

        private void animateOldContent(Runnable callback) {
            // Fade out
            mCurrentCertLayout.animate()
                    .alpha(0)
                    .setDuration(OUT_DURATION_MS)
                    .setInterpolator(AnimationUtils.loadInterpolator(mActivity,
                            android.R.interpolator.fast_out_linear_in))
                    .withEndAction(callback)
                    .start();
        }

        private void addAndAnimateNewContent(View nextCertLayout) {
            mCurrentCertLayout = nextCertLayout;
            mRootContainer.removeAllViews();
            mRootContainer.addView(nextCertLayout);

            mRootContainer.addOnLayoutChangeListener( new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mRootContainer.removeOnLayoutChangeListener(this);

                    // Animate slide in from the right
                    final int containerWidth = mRootContainer.getWidth();
                    mCurrentCertLayout.setTranslationX(containerWidth);
                    mCurrentCertLayout.animate()
                            .translationX(0)
                            .setInterpolator(AnimationUtils.loadInterpolator(mActivity,
                                    android.R.interpolator.linear_out_slow_in))
                            .setDuration(IN_DURATION_MS)
                            .start();
                }
            });
        }
    }
}
