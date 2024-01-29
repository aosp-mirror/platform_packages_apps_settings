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

package com.android.settings.accounts;

import android.accounts.Account;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.ThreadUtils;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Avatar related work to the onStart method of registered observable classes
 * in {@link SettingsHomepageActivity}.
 */
public class AvatarViewMixin implements LifecycleObserver {
    private static final String TAG = "AvatarViewMixin";

    @VisibleForTesting
    static final Intent INTENT_GET_ACCOUNT_DATA =
            new Intent("android.content.action.SETTINGS_ACCOUNT_DATA");

    private static final String METHOD_GET_ACCOUNT_AVATAR = "getAccountAvatar";
    private static final String KEY_AVATAR_BITMAP = "account_avatar";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_AVATAR_ICON = "avatar_icon";
    private static final String EXTRA_ACCOUNT_NAME = "extra.accountName";

    private final Context mContext;
    private final ImageView mAvatarView;
    private final MutableLiveData<Bitmap> mAvatarImage;

    @VisibleForTesting
    String mAccountName;

    /**
     * @return true if the avatar icon is supported.
     */
    public static boolean isAvatarSupported(Context context) {
        if (!context.getResources().getBoolean(R.bool.config_show_avatar_in_homepage)) {
            Log.d(TAG, "Feature disabled by config. Skipping");
            return false;
        }
        return true;
    }

    public AvatarViewMixin(SettingsHomepageActivity activity, ImageView avatarView) {
        mContext = activity.getApplicationContext();
        mAvatarView = avatarView;
        mAvatarView.setOnClickListener(v -> {
            Intent intent;
            try {
                final String uri = mContext.getResources().getString(
                        R.string.config_account_intent_uri);
                intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                Log.w(TAG, "Error parsing avatar mixin intent, skipping", e);
                return;
            }

            if (!TextUtils.isEmpty(mAccountName)) {
                intent.putExtra(EXTRA_ACCOUNT_NAME, mAccountName);
            }

            final List<ResolveInfo> matchedIntents =
                    mContext.getPackageManager().queryIntentActivities(intent,
                            PackageManager.MATCH_SYSTEM_ONLY);
            if (matchedIntents.isEmpty()) {
                Log.w(TAG, "Cannot find any matching action VIEW_ACCOUNT intent.");
                return;
            }

            // Set a component name since activity embedding requires a component name for
            // registering a rule.
            intent.setComponent(matchedIntents.get(0).getComponentInfo().getComponentName());
            ActivityEmbeddingRulesController.registerTwoPanePairRuleForSettingsHome(
                    mContext,
                    intent.getComponent(),
                    intent.getAction(),
                    false /* finishPrimaryWithSecondary */,
                    true /* finishSecondaryWithPrimary */,
                    false /* clearTop */);

            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .logSettingsTileClick(KEY_AVATAR_ICON, SettingsEnums.SETTINGS_HOMEPAGE);

            // Here may have two different UI while start the activity.
            // It will display adding account UI when device has no any account.
            // It will display account information page when intent added the specified account.
            activity.startActivity(intent);
        });

        mAvatarImage = new MutableLiveData<>();
        mAvatarImage.observe(activity, bitmap -> {
            avatarView.setImageBitmap(bitmap);
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (hasAccount()) {
            loadAccount();
        } else {
            mAccountName = null;
            mAvatarView.setImageResource(R.drawable.ic_account_circle_24dp);
        }
    }

    @VisibleForTesting
    boolean hasAccount() {
        final Account[] accounts = FeatureFactory.getFeatureFactory().getAccountFeatureProvider()
                .getAccounts(mContext);
        return (accounts != null) && (accounts.length > 0);
    }

    private void loadAccount() {
        final String authority = queryProviderAuthority();
        if (TextUtils.isEmpty(authority)) {
            return;
        }

        ThreadUtils.postOnBackgroundThread(() -> {
            final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .build();
            final Bundle bundle = mContext.getContentResolver().call(uri,
                    METHOD_GET_ACCOUNT_AVATAR, null /* arg */, null /* extras */);
            final Bitmap bitmap = bundle.getParcelable(KEY_AVATAR_BITMAP);
            mAccountName = bundle.getString(KEY_ACCOUNT_NAME, "" /* defaultValue */);
            mAvatarImage.postValue(bitmap);
        });
    }

    @VisibleForTesting
    String queryProviderAuthority() {
        final List<ResolveInfo> providers =
                mContext.getPackageManager().queryIntentContentProviders(INTENT_GET_ACCOUNT_DATA,
                        PackageManager.MATCH_SYSTEM_ONLY);
        if (providers.size() == 1) {
            return providers.get(0).providerInfo.authority;
        } else {
            Log.w(TAG, "The size of the provider is " + providers.size());
            return null;
        }
    }
}
