/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.PERSONAL_CATEGORY_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_CATEGORY_HEADER;
import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;

import android.annotation.UiThread;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsSettings.Tab;
import com.android.settingslib.core.lifecycle.ObservableFragment;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Fragment to display trusted credentials settings for one tab.
 */
public class TrustedCredentialsFragment extends ObservableFragment
        implements TrustedCredentialsDialogBuilder.DelegateInterface {

    public static final String ARG_POSITION = "tab";
    public static final String ARG_SHOW_NEW_FOR_USER = "ARG_SHOW_NEW_FOR_USER";

    private static final String TAG = "TrustedCredentialsFragment";

    private DevicePolicyManager mDevicePolicyManager;
    private UserManager mUserManager;
    private KeyguardManager mKeyguardManager;
    private int mTrustAllCaUserId;

    private static final String SAVED_CONFIRMED_CREDENTIAL_USERS = "ConfirmedCredentialUsers";
    private static final String SAVED_CONFIRMING_CREDENTIAL_USER = "ConfirmingCredentialUser";
    private static final int REQUEST_CONFIRM_CREDENTIALS = 1;

    private GroupAdapter mGroupAdapter;
    private AliasOperation mAliasOperation;
    private ArraySet<Integer> mConfirmedCredentialUsers;
    private int mConfirmingCredentialUser;
    private IntConsumer mConfirmingCredentialListener;
    private final Set<AdapterData.AliasLoader> mAliasLoaders = new ArraySet<>(2);
    @GuardedBy("mKeyChainConnectionByProfileId")
    private final SparseArray<KeyChainConnection>
            mKeyChainConnectionByProfileId = new SparseArray<>();
    private ViewGroup mFragmentView;

    private final BroadcastReceiver mWorkProfileChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNLOCKED.equals(action)) {
                mGroupAdapter.load();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        mDevicePolicyManager = activity.getSystemService(DevicePolicyManager.class);
        mUserManager = activity.getSystemService(UserManager.class);
        mKeyguardManager = activity.getSystemService(KeyguardManager.class);
        mTrustAllCaUserId = activity.getIntent().getIntExtra(ARG_SHOW_NEW_FOR_USER,
                UserHandle.USER_NULL);
        mConfirmedCredentialUsers = new ArraySet<>(2);
        mConfirmingCredentialUser = UserHandle.USER_NULL;
        if (savedInstanceState != null) {
            mConfirmingCredentialUser = savedInstanceState.getInt(SAVED_CONFIRMING_CREDENTIAL_USER,
                    UserHandle.USER_NULL);
            ArrayList<Integer> users = savedInstanceState.getIntegerArrayList(
                    SAVED_CONFIRMED_CREDENTIAL_USERS);
            if (users != null) {
                mConfirmedCredentialUsers.addAll(users);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
        activity.registerReceiver(mWorkProfileChangedReceiver, filter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(SAVED_CONFIRMED_CREDENTIAL_USERS, new ArrayList<>(
                mConfirmedCredentialUsers));
        outState.putInt(SAVED_CONFIRMING_CREDENTIAL_USER, mConfirmingCredentialUser);
        mGroupAdapter.saveState(outState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mFragmentView = (ViewGroup) inflater.inflate(R.layout.trusted_credentials, parent, false);

        ViewGroup contentView = mFragmentView.findViewById(R.id.content);

        mGroupAdapter = new GroupAdapter(
                requireArguments().getInt(ARG_POSITION) == 0 ? Tab.SYSTEM : Tab.USER);
        int profilesSize = mGroupAdapter.getGroupCount();
        for (int i = 0; i < profilesSize; i++) {
            Bundle childState = savedInstanceState == null ? null
                    : savedInstanceState.getBundle(mGroupAdapter.getKey(i));
            createChildView(inflater, contentView, childState, i);
        }
        return mFragmentView;
    }

    private void createChildView(
            LayoutInflater inflater, ViewGroup parent, Bundle childState, int i) {
        boolean isWork = mGroupAdapter.getUserInfoByGroup(i).isManagedProfile();
        ChildAdapter adapter = mGroupAdapter.createChildAdapter(i);

        LinearLayout containerView = (LinearLayout) inflater.inflate(
                R.layout.trusted_credential_list_container, parent, false);
        adapter.setContainerView(containerView, childState);

        int profilesSize = mGroupAdapter.getGroupCount();
        adapter.showHeader(profilesSize > 1);
        adapter.showDivider(isWork);
        adapter.setExpandIfAvailable(profilesSize <= 2 || !isWork, childState);
        if (isWork) {
            parent.addView(containerView);
        } else {
            parent.addView(containerView, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFragmentView.requestLayout();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mWorkProfileChangedReceiver);
        for (AdapterData.AliasLoader aliasLoader : mAliasLoaders) {
            aliasLoader.cancel(true);
        }
        mAliasLoaders.clear();
        if (mAliasOperation != null) {
            mAliasOperation.cancel(true);
            mAliasOperation = null;
        }
        closeKeyChainConnections();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIRM_CREDENTIALS) {
            int userId = mConfirmingCredentialUser;
            IntConsumer listener = mConfirmingCredentialListener;
            // reset them before calling the listener because the listener may call back to start
            // activity again. (though it should never happen.)
            mConfirmingCredentialUser = UserHandle.USER_NULL;
            mConfirmingCredentialListener = null;
            if (resultCode == Activity.RESULT_OK) {
                mConfirmedCredentialUsers.add(userId);
                if (listener != null) {
                    listener.accept(userId);
                }
            }
        }
    }

    private void closeKeyChainConnections() {
        synchronized (mKeyChainConnectionByProfileId) {
            int n = mKeyChainConnectionByProfileId.size();
            for (int i = 0; i < n; ++i) {
                mKeyChainConnectionByProfileId.valueAt(i).close();
            }
            mKeyChainConnectionByProfileId.clear();
        }
    }

    /**
     * Start work challenge activity.
     *
     * @return true if screenlock exists
     */
    private boolean startConfirmCredential(int userId) {
        Intent newIntent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, userId);
        if (newIntent == null) {
            return false;
        }
        mConfirmingCredentialUser = userId;
        startActivityForResult(newIntent, REQUEST_CONFIRM_CREDENTIALS);
        return true;
    }

    /**
     * Adapter for expandable list view of certificates. Groups in the view correspond to profiles
     * whereas children correspond to certificates.
     */
    private class GroupAdapter extends BaseExpandableListAdapter implements
            ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener {
        private final AdapterData mData;
        private final ArrayList<ChildAdapter> mChildAdapters = new ArrayList<>();

        private GroupAdapter(Tab tab) {
            mData = new AdapterData(tab, this);
            load();
        }

        @Override
        public int getGroupCount() {
            return mData.mCertHoldersByUserId.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<CertHolder> certHolders = mData.mCertHoldersByUserId.valueAt(groupPosition);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }

        @Override
        public UserHandle getGroup(int groupPosition) {
            return new UserHandle(mData.mCertHoldersByUserId.keyAt(groupPosition));
        }

        @Override
        public CertHolder getChild(int groupPosition, int childPosition) {
            return mData.mCertHoldersByUserId.get(getUserIdByGroup(groupPosition)).get(
                    childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return getUserIdByGroup(groupPosition);
        }

        private int getUserIdByGroup(int groupPosition) {
            return mData.mCertHoldersByUserId.keyAt(groupPosition);
        }

        public UserInfo getUserInfoByGroup(int groupPosition) {
            return mUserManager.getUserInfo(getUserIdByGroup(groupPosition));
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = Utils.inflateCategoryHeader(inflater, parent);
            }

            TextView title = convertView.findViewById(android.R.id.title);
            if (getUserInfoByGroup(groupPosition).isManagedProfile()) {
                title.setText(mDevicePolicyManager.getResources().getString(WORK_CATEGORY_HEADER,
                        () -> getString(com.android.settingslib.R.string.category_work)));
            } else {
                title.setText(mDevicePolicyManager.getResources().getString(
                        PERSONAL_CATEGORY_HEADER,
                        () -> getString(com.android.settingslib.R.string.category_personal)));

            }
            title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            return getViewForCertificate(getChild(groupPosition, childPosition), mData.mTab,
                    convertView, parent);
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public boolean onChildClick(ExpandableListView expandableListView, View view,
                int groupPosition, int childPosition, long id) {
            showCertDialog(getChild(groupPosition, childPosition));
            return true;
        }

        @Override
        public boolean onGroupClick(ExpandableListView expandableListView, View view,
                int groupPosition, long id) {
            return !checkGroupExpandableAndStartWarningActivity(groupPosition);
        }

        public void load() {
            mData.new AliasLoader().execute();
        }

        public void remove(CertHolder certHolder) {
            mData.remove(certHolder);
        }

        ChildAdapter createChildAdapter(int groupPosition) {
            ChildAdapter childAdapter = new ChildAdapter(this, groupPosition);
            mChildAdapters.add(childAdapter);
            return childAdapter;
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition) {
            return checkGroupExpandableAndStartWarningActivity(groupPosition, true);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition,
                boolean startActivity) {
            UserHandle groupUser = getGroup(groupPosition);
            int groupUserId = groupUser.getIdentifier();
            if (mUserManager.isQuietModeEnabled(groupUser)) {
                if (startActivity) {
                    Intent intent =
                            UnlaunchableAppActivity.createInQuietModeDialogIntent(groupUserId);
                    getActivity().startActivity(intent);
                }
                return false;
            } else if (!mUserManager.isUserUnlocked(groupUser)) {
                LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
                if (lockPatternUtils.isSeparateProfileChallengeEnabled(groupUserId)) {
                    if (startActivity) {
                        startConfirmCredential(groupUserId);
                    }
                    return false;
                }
            }
            return true;
        }

        private View getViewForCertificate(CertHolder certHolder, Tab mTab, View convertView,
                ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.trusted_credential, parent, false);
                convertView.setTag(holder);
                holder.mSubjectPrimaryView =
                        convertView.findViewById(R.id.trusted_credential_subject_primary);
                holder.mSubjectSecondaryView =
                        convertView.findViewById(R.id.trusted_credential_subject_secondary);
                holder.mSwitch = convertView.findViewById(R.id.trusted_credential_status);
                holder.mSwitch.setOnClickListener(view -> {
                    removeOrInstallCert((CertHolder) view.getTag());
                });
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
            holder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
            if (mTab.mSwitch) {
                holder.mSwitch.setChecked(!certHolder.mDeleted);
                holder.mSwitch.setEnabled(!mUserManager.hasUserRestriction(
                        UserManager.DISALLOW_CONFIG_CREDENTIALS,
                        new UserHandle(certHolder.mProfileId)));
                holder.mSwitch.setVisibility(View.VISIBLE);
                holder.mSwitch.setTag(certHolder);
            }
            return convertView;
        }

        private void saveState(Bundle outState) {
            for (int groupPosition = 0, mChildAdaptersSize = mChildAdapters.size();
                    groupPosition < mChildAdaptersSize; groupPosition++) {
                ChildAdapter childAdapter = mChildAdapters.get(groupPosition);
                outState.putBundle(getKey(groupPosition), childAdapter.saveState());
            }
        }

        @NonNull
        private String getKey(int groupPosition) {
            return "Group" + getUserIdByGroup(groupPosition);
        }

        private static class ViewHolder {
            private TextView mSubjectPrimaryView;
            private TextView mSubjectSecondaryView;
            private CompoundButton mSwitch;
        }
    }

    private class ChildAdapter extends BaseAdapter implements View.OnClickListener,
            AdapterView.OnItemClickListener {
        private static final String KEY_CONTAINER = "Container";
        private static final String KEY_IS_LIST_EXPANDED = "IsListExpanded";
        private final int[] mGroupExpandedStateSet = {com.android.internal.R.attr.state_expanded};
        private final int[] mEmptyStateSet = {};
        private final LinearLayout.LayoutParams mHideContainerLayoutParams =
                new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 0f);
        private final LinearLayout.LayoutParams mHideListLayoutParams =
                new LinearLayout.LayoutParams(MATCH_PARENT, 0);
        private final LinearLayout.LayoutParams mShowLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, MATCH_PARENT, 1f);
        private final GroupAdapter mParent;
        private final int mGroupPosition;
        /*
         * This class doesn't hold the actual data. Events should notify parent.
         * When notifying DataSet events in this class, events should be forwarded to mParent.
         * i.e. this.notifyDataSetChanged -> mParent.notifyDataSetChanged -> mObserver.onChanged
         * -> outsideObservers.onChanged() (e.g. ListView)
         */
        private final DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                TrustedCredentialsFragment.ChildAdapter.super.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                TrustedCredentialsFragment.ChildAdapter.super.notifyDataSetInvalidated();
            }
        };

        private boolean mIsListExpanded = true;
        private LinearLayout mContainerView;
        private ViewGroup mHeaderView;
        private ListView mListView;
        private ImageView mIndicatorView;

        private ChildAdapter(GroupAdapter parent, int groupPosition) {
            mParent = parent;
            mGroupPosition = groupPosition;
            mParent.registerDataSetObserver(mObserver);
        }

        @Override
        public int getCount() {
            return mParent.getChildrenCount(mGroupPosition);
        }

        @Override
        public CertHolder getItem(int position) {
            return mParent.getChild(mGroupPosition, position);
        }

        @Override
        public long getItemId(int position) {
            return mParent.getChildId(mGroupPosition, position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mParent.getChildView(mGroupPosition, position, false, convertView, parent);
        }

        // DataSet events
        @Override
        public void notifyDataSetChanged() {
            // Don't call super as the parent will propagate this event back later in mObserver
            mParent.notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetInvalidated() {
            // Don't call super as the parent will propagate this event back later in mObserver
            mParent.notifyDataSetInvalidated();
        }

        // View related codes
        @Override
        public void onClick(View view) {
            mIsListExpanded = checkGroupExpandableAndStartWarningActivity() && !mIsListExpanded;
            refreshViews();
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            showCertDialog(getItem(pos));
        }

        public void setContainerView(LinearLayout containerView, Bundle savedState) {
            mContainerView = containerView;
            // Handle manually because multiple groups with same id elements.
            mContainerView.setSaveFromParentEnabled(false);

            mListView = mContainerView.findViewById(R.id.cert_list);
            mListView.setAdapter(this);
            mListView.setOnItemClickListener(this);
            mListView.setItemsCanFocus(true);

            mHeaderView = mContainerView.findViewById(R.id.header_view);
            mHeaderView.setOnClickListener(this);

            mIndicatorView = mHeaderView.findViewById(R.id.group_indicator);
            mIndicatorView.setImageDrawable(getGroupIndicator());

            FrameLayout headerContentContainer =
                    mHeaderView.findViewById(R.id.header_content_container);
            headerContentContainer.addView(
                    mParent.getGroupView(mGroupPosition, true /* parent ignores it */, null,
                            headerContentContainer));

            if (savedState != null) {
                SparseArray<Parcelable> containerStates =
                        savedState.getSparseParcelableArray(KEY_CONTAINER, Parcelable.class);
                if (containerStates != null) {
                    mContainerView.restoreHierarchyState(containerStates);
                }
            }
        }

        public void showHeader(boolean showHeader) {
            mHeaderView.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        }

        public void showDivider(boolean showDivider) {
            View dividerView = mHeaderView.findViewById(R.id.header_divider);
            dividerView.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }

        public void setExpandIfAvailable(boolean expanded, Bundle savedState) {
            if (savedState != null) {
                expanded = savedState.getBoolean(KEY_IS_LIST_EXPANDED);
            }
            mIsListExpanded = expanded && mParent.checkGroupExpandableAndStartWarningActivity(
                    mGroupPosition, false /* startActivity */);
            refreshViews();
        }

        private boolean checkGroupExpandableAndStartWarningActivity() {
            return mParent.checkGroupExpandableAndStartWarningActivity(mGroupPosition);
        }

        private void refreshViews() {
            mIndicatorView.setImageState(mIsListExpanded ? mGroupExpandedStateSet
                    : mEmptyStateSet, false);
            mListView.setLayoutParams(mIsListExpanded ? mShowLayoutParams
                    : mHideListLayoutParams);
            mContainerView.setLayoutParams(mIsListExpanded ? mShowLayoutParams
                    : mHideContainerLayoutParams);
        }

        // Get group indicator from styles of ExpandableListView
        private Drawable getGroupIndicator() {
            TypedArray a = getActivity().obtainStyledAttributes(null,
                    com.android.internal.R.styleable.ExpandableListView,
                    com.android.internal.R.attr.expandableListViewStyle, 0);
            Drawable groupIndicator = a.getDrawable(
                    com.android.internal.R.styleable.ExpandableListView_groupIndicator);
            a.recycle();
            return groupIndicator;
        }

        private Bundle saveState() {
            Bundle bundle = new Bundle();
            SparseArray<Parcelable> states = new SparseArray<>();
            mContainerView.saveHierarchyState(states);
            bundle.putSparseParcelableArray(KEY_CONTAINER, states);
            bundle.putBoolean(KEY_IS_LIST_EXPANDED, mIsListExpanded);
            return bundle;
        }
    }

    private class AdapterData {
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId =
                new SparseArray<>();
        private final Tab mTab;
        private final GroupAdapter mAdapter;

        private AdapterData(Tab tab, GroupAdapter adapter) {
            mAdapter = adapter;
            mTab = tab;
        }

        private class AliasLoader extends AsyncTask<Void, Integer, SparseArray<List<CertHolder>>> {
            private ProgressBar mProgressBar;
            private View mContentView;
            private Context mContext;

            AliasLoader() {
                mContext = getActivity();
                mAliasLoaders.add(this);
                List<UserHandle> profiles = mUserManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                    mCertHoldersByUserId.put(profile.getIdentifier(), new ArrayList<>());
                }
            }

            private boolean shouldSkipProfile(UserHandle userHandle) {
                return mUserManager.isQuietModeEnabled(userHandle)
                        || !mUserManager.isUserUnlocked(userHandle.getIdentifier());
            }

            @Override
            protected void onPreExecute() {
                mProgressBar = mFragmentView.findViewById(R.id.progress);
                mContentView = mFragmentView.findViewById(R.id.content);
                mProgressBar.setVisibility(View.VISIBLE);
                mContentView.setVisibility(View.GONE);
            }

            @Override
            protected SparseArray<List<CertHolder>> doInBackground(Void... params) {
                SparseArray<List<CertHolder>> certHoldersByProfile =
                        new SparseArray<>();
                try {
                    synchronized (mKeyChainConnectionByProfileId) {
                        List<UserHandle> profiles = mUserManager.getUserProfiles();
                        // First we get all aliases for all profiles in order to show progress
                        // correctly. Otherwise this could all be in a single loop.
                        SparseArray<List<String>> aliasesByProfileId =
                                new SparseArray<>(profiles.size());
                        int max = 0;
                        int progress = 0;
                        for (UserHandle profile : profiles) {
                            int profileId = profile.getIdentifier();
                            if (shouldSkipProfile(profile)) {
                                continue;
                            }
                            KeyChainConnection keyChainConnection = KeyChain.bindAsUser(mContext,
                                    profile);
                            // Saving the connection for later use on the certificate dialog.
                            mKeyChainConnectionByProfileId.put(profileId, keyChainConnection);
                            IKeyChainService service = keyChainConnection.getService();
                            List<String> aliases = mTab.getAliases(service);
                            if (isCancelled()) {
                                return new SparseArray<>();
                            }
                            max += aliases.size();
                            aliasesByProfileId.put(profileId, aliases);
                        }
                        for (UserHandle profile : profiles) {
                            int profileId = profile.getIdentifier();
                            List<String> aliases = aliasesByProfileId.get(profileId);
                            if (isCancelled()) {
                                return new SparseArray<>();
                            }
                            KeyChainConnection keyChainConnection =
                                    mKeyChainConnectionByProfileId.get(
                                            profileId);
                            if (shouldSkipProfile(profile) || aliases == null
                                    || keyChainConnection == null) {
                                certHoldersByProfile.put(profileId, new ArrayList<>(0));
                                continue;
                            }
                            IKeyChainService service = keyChainConnection.getService();
                            List<CertHolder> certHolders = new ArrayList<>(max);
                            for (String alias : aliases) {
                                byte[] encodedCertificate = service.getEncodedCaCertificate(alias,
                                        true);
                                X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                                certHolders.add(new CertHolder(service, mAdapter,
                                        mTab, alias, cert, profileId));
                                publishProgress(++progress, max);
                            }
                            Collections.sort(certHolders);
                            certHoldersByProfile.put(profileId, certHolders);
                        }
                        return certHoldersByProfile;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote exception while loading aliases.", e);
                    return new SparseArray<>();
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException while loading aliases.", e);
                    return new SparseArray<>();
                }
            }

            @Override
            protected void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0];
                int max = progressAndMax[1];
                if (max != mProgressBar.getMax()) {
                    mProgressBar.setMax(max);
                }
                mProgressBar.setProgress(progress);
            }

            @Override
            protected void onPostExecute(SparseArray<List<CertHolder>> certHolders) {
                mCertHoldersByUserId.clear();
                int n = certHolders.size();
                for (int i = 0; i < n; ++i) {
                    mCertHoldersByUserId.put(certHolders.keyAt(i), certHolders.valueAt(i));
                }
                mAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
                mContentView.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(0);
                mAliasLoaders.remove(this);
                showTrustAllCaDialogIfNeeded();
            }

            private boolean isUserTabAndTrustAllCertMode() {
                return isTrustAllCaCertModeInProgress() && mTab == Tab.USER;
            }

            @UiThread
            private void showTrustAllCaDialogIfNeeded() {
                if (!isUserTabAndTrustAllCertMode()) {
                    return;
                }
                List<CertHolder> certHolders = mCertHoldersByUserId.get(mTrustAllCaUserId);
                if (certHolders == null) {
                    return;
                }

                List<CertHolder> unapprovedUserCertHolders = new ArrayList<>();
                DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
                for (CertHolder cert : certHolders) {
                    if (cert != null && !dpm.isCaCertApproved(cert.mAlias, mTrustAllCaUserId)) {
                        unapprovedUserCertHolders.add(cert);
                    }
                }

                if (unapprovedUserCertHolders.size() == 0) {
                    Log.w(TAG, "no cert is pending approval for user " + mTrustAllCaUserId);
                    return;
                }
                showTrustAllCaDialog(unapprovedUserCertHolders);
            }
        }

        public void remove(CertHolder certHolder) {
            if (mCertHoldersByUserId != null) {
                List<CertHolder> certs = mCertHoldersByUserId.get(certHolder.mProfileId);
                if (certs != null) {
                    certs.remove(certHolder);
                }
            }
        }
    }

    /* package */ static class CertHolder implements Comparable<CertHolder> {
        public int mProfileId;
        private final IKeyChainService mService;
        private final GroupAdapter mAdapter;
        private final Tab mTab;
        private final String mAlias;
        private final X509Certificate mX509Cert;

        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private boolean mDeleted;

        private CertHolder(IKeyChainService service,
                GroupAdapter adapter,
                Tab tab,
                String alias,
                X509Certificate x509Cert,
                int profileId) {
            mProfileId = profileId;
            mService = service;
            mAdapter = adapter;
            mTab = tab;
            mAlias = alias;
            mX509Cert = x509Cert;

            mSslCert = new SslCertificate(x509Cert);

            String cn = mSslCert.getIssuedTo().getCName();
            String o = mSslCert.getIssuedTo().getOName();
            String ou = mSslCert.getIssuedTo().getUName();
            // if we have a O, use O as primary subject, secondary prefer CN over OU
            // if we don't have an O, use CN as primary, empty secondary
            // if we don't have O or CN, use DName as primary, empty secondary
            if (!o.isEmpty()) {
                if (!cn.isEmpty()) {
                    mSubjectPrimary = o;
                    mSubjectSecondary = cn;
                } else {
                    mSubjectPrimary = o;
                    mSubjectSecondary = ou;
                }
            } else {
                if (!cn.isEmpty()) {
                    mSubjectPrimary = cn;
                    mSubjectSecondary = "";
                } else {
                    mSubjectPrimary = mSslCert.getIssuedTo().getDName();
                    mSubjectSecondary = "";
                }
            }
            try {
                mDeleted = mTab.deleted(mService, mAlias);
            } catch (RemoteException e) {
                Log.e(TAG, "Remote exception while checking if alias " + mAlias + " is deleted.",
                        e);
                mDeleted = false;
            }
        }

        @Override
        public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            if (primary != 0) {
                return primary;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return mAlias.equals(other.mAlias);
        }

        @Override
        public int hashCode() {
            return mAlias.hashCode();
        }

        public int getUserId() {
            return mProfileId;
        }

        public String getAlias() {
            return mAlias;
        }

        public boolean isSystemCert() {
            return mTab == Tab.SYSTEM;
        }

        public boolean isDeleted() {
            return mDeleted;
        }
    }


    private boolean isTrustAllCaCertModeInProgress() {
        return mTrustAllCaUserId != UserHandle.USER_NULL;
    }

    private void showTrustAllCaDialog(List<CertHolder> unapprovedCertHolders) {
        CertHolder[] arr =
                unapprovedCertHolders.toArray(new CertHolder[unapprovedCertHolders.size()]);
        new TrustedCredentialsDialogBuilder(getActivity(), this)
                .setCertHolders(arr)
                .setOnDismissListener(dialogInterface -> {
                    // Avoid starting dialog again after Activity restart.
                    getActivity().getIntent().removeExtra(ARG_SHOW_NEW_FOR_USER);
                    mTrustAllCaUserId = UserHandle.USER_NULL;
                })
                .show();
    }

    private void showCertDialog(final CertHolder certHolder) {
        new TrustedCredentialsDialogBuilder(getActivity(), this)
                .setCertHolder(certHolder)
                .show();
    }

    @Override
    public List<X509Certificate> getX509CertsFromCertHolder(CertHolder certHolder) {
        List<X509Certificate> certificates = null;
        try {
            synchronized (mKeyChainConnectionByProfileId) {
                KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                        certHolder.mProfileId);
                IKeyChainService service = keyChainConnection.getService();
                List<String> chain = service.getCaCertificateChainAliases(certHolder.mAlias, true);
                certificates = new ArrayList<>(chain.size());
                for (String s : chain) {
                    byte[] encodedCertificate = service.getEncodedCaCertificate(s, true);
                    X509Certificate certificate = KeyChain.toCertificate(encodedCertificate);
                    certificates.add(certificate);
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while retrieving certificate chain for root "
                    + certHolder.mAlias, ex);
        }
        return certificates;
    }

    @Override
    public void removeOrInstallCert(CertHolder certHolder) {
        new AliasOperation(certHolder).execute();
    }

    @Override
    public boolean startConfirmCredentialIfNotConfirmed(int userId,
            IntConsumer onCredentialConfirmedListener) {
        if (mConfirmedCredentialUsers.contains(userId)) {
            // Credential has been confirmed. Don't start activity.
            return false;
        }

        boolean result = startConfirmCredential(userId);
        if (result) {
            mConfirmingCredentialListener = onCredentialConfirmedListener;
        }
        return result;
    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        private AliasOperation(CertHolder certHolder) {
            mCertHolder = certHolder;
            mAliasOperation = this;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                synchronized (mKeyChainConnectionByProfileId) {
                    KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                            mCertHolder.mProfileId);
                    IKeyChainService service = keyChainConnection.getService();
                    if (mCertHolder.mDeleted) {
                        byte[] bytes = mCertHolder.mX509Cert.getEncoded();
                        service.installCaCertificate(bytes);
                        return true;
                    } else {
                        return service.deleteCaCertificate(mCertHolder.mAlias);
                    }
                }
            } catch (CertificateEncodingException | SecurityException | IllegalStateException
                    | RemoteException e) {
                Log.w(TAG, "Error while toggling alias " + mCertHolder.mAlias, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            if (ok) {
                if (mCertHolder.mTab.mSwitch) {
                    mCertHolder.mDeleted = !mCertHolder.mDeleted;
                } else {
                    mCertHolder.mAdapter.remove(mCertHolder);
                }
                mCertHolder.mAdapter.notifyDataSetChanged();
            } else {
                // bail, reload to reset to known state
                mCertHolder.mAdapter.load();
            }
            mAliasOperation = null;
        }
    }
}
