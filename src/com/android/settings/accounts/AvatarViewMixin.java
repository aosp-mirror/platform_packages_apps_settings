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
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.overlay.FeatureFactory;

/**
 * Avatar related work to the onStart method of registered observable classes
 * in {@link SettingsHomepageActivity}.
 */
public class AvatarViewMixin implements LifecycleObserver {
    private static final String TAG = "AvatarViewMixin";

    private final Context mContext;
    private final ImageView mAvatarView;

    public AvatarViewMixin(Context context, ImageView avatarView) {
        mContext = context.getApplicationContext();
        mAvatarView = avatarView;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_avatar_in_homepage)) {
            Log.d(TAG, "Feature disabled. Skipping");
            return;
        }
        if (hasAccount()) {
            //TODO(b/117509285): To migrate account icon on search bar
        } else {
            mAvatarView.setImageResource(R.drawable.ic_account_circle_24dp);
        }
    }

    @VisibleForTesting
    boolean hasAccount() {
        final Account accounts[] = FeatureFactory.getFactory(
                mContext).getAccountFeatureProvider().getAccounts(mContext);
        return (accounts != null) && (accounts.length > 0);
    }
}
