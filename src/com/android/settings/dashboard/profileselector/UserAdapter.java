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

package com.android.settings.dashboard.profileselector;

import static android.app.admin.DevicePolicyResources.Strings.Settings.PERSONAL_CATEGORY_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.PRIVATE_CATEGORY_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_CATEGORY_HEADER;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.UserIcons;
import com.android.internal.widget.RecyclerView;
import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter for a spinner that shows a list of users.
 */
public class UserAdapter extends BaseAdapter {
    private static final String TAG = "UserAdapter";

    /** Holder for user details */
    public static class UserDetails {
        private final UserHandle mUserHandle;
        private final UserManager mUserManager;
        private final Drawable mIcon;
        private final String mTitle;

        public UserDetails(UserHandle userHandle, UserManager um, Context context) {
            mUserHandle = userHandle;
            mUserManager = um;
            UserInfo userInfo = um.getUserInfo(mUserHandle.getIdentifier());
            int tintColor = Utils.getColorAttrDefaultColor(context,
                    com.android.internal.R.attr.materialColorPrimary);
            if (userInfo.isManagedProfile()) {
                mIcon = context.getPackageManager().getUserBadgeForDensityNoBackground(
                        userHandle, /* density= */ 0);
                mIcon.setTint(tintColor);
            } else {
                mIcon = UserIcons.getDefaultUserIconInColor(context.getResources(), tintColor);
            }
            mTitle = getTitle(context);
        }

        private String getTitle(Context context) {
            DevicePolicyManager devicePolicyManager =
                    Objects.requireNonNull(context.getSystemService(DevicePolicyManager.class));
            DevicePolicyResourcesManager resources = devicePolicyManager.getResources();
            int userId = mUserHandle.getIdentifier();
            if (userId == UserHandle.USER_CURRENT || userId == ActivityManager.getCurrentUser()) {
                return resources.getString(PERSONAL_CATEGORY_HEADER,
                        () -> context.getString(
                                com.android.settingslib.R.string.category_personal));
            } else if (mUserManager.isManagedProfile(userId)) {
                return resources.getString(WORK_CATEGORY_HEADER,
                        () -> context.getString(com.android.settingslib.R.string.category_work));
            } else if (android.os.Flags.allowPrivateProfile()
                    && mUserManager.getUserInfo(userId).isPrivateProfile()) {
                return resources.getString(PRIVATE_CATEGORY_HEADER,
                        () -> context.getString(com.android.settingslib.R.string.category_private));
            }
            Log.w(TAG, "title requested for unexpected user id " + userId);
            return resources.getString(PERSONAL_CATEGORY_HEADER,
                    () -> context.getString(com.android.settingslib.R.string.category_personal));
        }
    }

    private final ArrayList<UserDetails> mUserDetails;
    private final LayoutInflater mInflater;

    public UserAdapter(Context context, ArrayList<UserDetails> users) {
        if (users == null) {
            throw new IllegalArgumentException("A list of user details must be provided");
        }
        mUserDetails = users;
        mInflater = context.getSystemService(LayoutInflater.class);
    }

    public UserHandle getUserHandle(int position) {
        if (position < 0 || position >= mUserDetails.size()) {
            return null;
        }
        return mUserDetails.get(position).mUserHandle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mInflater.inflate(R.layout.user_preference, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        bindViewHolder(holder, position);
        return convertView;
    }

    private void bindViewHolder(ViewHolder holder, int position) {
        UserDetails userDetails = getItem(position);
        holder.getIconView().setImageDrawable(userDetails.mIcon);
        holder.setTitle(userDetails.mTitle);
    }

    @Override
    public int getCount() {
        return mUserDetails.size();
    }

    @Override
    public UserAdapter.UserDetails getItem(int position) {
        return mUserDetails.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mUserDetails.get(position).mUserHandle.getIdentifier();
    }

    private RecyclerView.Adapter<ViewHolder> createRecyclerViewAdapter(
            OnClickListener onClickListener) {
        return new RecyclerView.Adapter<ViewHolder>() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.user_select_item, parent, false);

                return new ViewHolder(view, onClickListener);
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                UserAdapter.this.bindViewHolder(holder, position);
            }

            @Override
            public int getItemCount() {
                return getCount();
            }
        };
    }

    /**
     * Creates a {@link UserAdapter} if there is more than one profile on the device.
     *
     * <p> The adapter can be used to populate a spinner that switches between the different
     * profiles.
     *
     * @return a {@link UserAdapter} or null if there is only one profile.
     */
    public static UserAdapter createUserSpinnerAdapter(UserManager userManager, Context context) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        if (userProfiles.size() < 2) {
            return null;
        }

        UserHandle myUserHandle = new UserHandle(UserHandle.myUserId());
        // The first option should be the current profile
        userProfiles.remove(myUserHandle);
        userProfiles.add(0, myUserHandle);

        return createUserAdapter(userManager, context, userProfiles);
    }

    /**
     * Creates a {@link RecyclerView} adapter which be used to populate a {@link RecyclerView} that
     * select one of the different profiles.
     */
    public static RecyclerView.Adapter<ViewHolder> createUserRecycleViewAdapter(
            Context context, List<UserHandle> userProfiles, OnClickListener onClickListener) {
        UserManager systemService = context.getSystemService(UserManager.class);
        return createUserAdapter(systemService, context, userProfiles)
                .createRecyclerViewAdapter(onClickListener);
    }

    private static UserAdapter createUserAdapter(
            UserManager userManager, Context context, List<UserHandle> userProfiles) {
        ArrayList<UserDetails> userDetails = new ArrayList<>(userProfiles.size());
        for (UserHandle userProfile : userProfiles) {
            userDetails.add(new UserDetails(userProfile, userManager, context));
        }
        return new UserAdapter(context, userDetails);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIconView;
        private final TextView mTitleView;
        private final View mButtonView;

        private ViewHolder(View view) {
            super(view);
            mIconView = view.findViewById(android.R.id.icon);
            mTitleView = view.findViewById(android.R.id.title);
            mButtonView = view.findViewById(R.id.button);
        }

        private ViewHolder(View view, OnClickListener onClickListener) {
            this(view);
            if (mButtonView != null) {
                mButtonView.setOnClickListener(v -> onClickListener.onClick(getAdapterPosition()));
            }
        }

        private ImageView getIconView() {
            return mIconView;
        }

        private void setTitle(CharSequence title) {
            mTitleView.setText(title);
            if (mButtonView != null) {
                mButtonView.setContentDescription(title);
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when a user is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a user has been clicked.
         */
        void onClick(int position);
    }
}
