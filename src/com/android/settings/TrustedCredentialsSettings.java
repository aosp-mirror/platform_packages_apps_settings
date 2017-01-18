/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.annotation.UiThread;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.util.Log;
import android.util.SparseArray;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.ParcelableString;
import com.android.internal.widget.LockPatternUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

public class TrustedCredentialsSettings extends OptionsMenuFragment
        implements TrustedCredentialsDialogBuilder.DelegateInterface {

    public static final String ARG_SHOW_NEW_FOR_USER = "ARG_SHOW_NEW_FOR_USER";

    private static final String TAG = "TrustedCredentialsSettings";

    private UserManager mUserManager;
    private KeyguardManager mKeyguardManager;
    private int mTrustAllCaUserId;

    private static final String SAVED_CONFIRMED_CREDENTIAL_USERS = "ConfirmedCredentialUsers";
    private static final String SAVED_CONFIRMING_CREDENTIAL_USER = "ConfirmingCredentialUser";
    private static final String USER_ACTION = "com.android.settings.TRUSTED_CREDENTIALS_USER";
    private static final int REQUEST_CONFIRM_CREDENTIALS = 1;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.TRUSTED_CREDENTIALS;
    }

    private enum Tab {
        SYSTEM("system",
                R.string.trusted_credentials_system_tab,
                R.id.system_tab,
                R.id.system_progress,
                R.id.system_personal_container,
                R.id.system_work_container,
                R.id.system_expandable_list,
                R.id.system_content,
               true),
        USER("user",
                R.string.trusted_credentials_user_tab,
                R.id.user_tab,
                R.id.user_progress,
                R.id.user_personal_container,
                R.id.user_work_container,
                R.id.user_expandable_list,
                R.id.user_content,
                false);

        private final String mTag;
        private final int mLabel;
        private final int mView;
        private final int mProgress;
        private final int mPersonalList;
        private final int mWorkList;
        private final int mExpandableList;
        private final int mContentView;
        private final boolean mSwitch;

        private Tab(String tag, int label, int view, int progress, int personalList, int workList,
                int expandableList, int contentView, boolean withSwitch) {
            mTag = tag;
            mLabel = label;
            mView = view;
            mProgress = progress;
            mPersonalList = personalList;
            mWorkList = workList;
            mExpandableList = expandableList;
            mContentView = contentView;
            mSwitch = withSwitch;
        }

        private List<ParcelableString> getAliases(IKeyChainService service) throws RemoteException {
            switch (this) {
                case SYSTEM: {
                    return service.getSystemCaAliases().getList();
                }
                case USER:
                    return service.getUserCaAliases().getList();
            }
            throw new AssertionError();
        }
        private boolean deleted(IKeyChainService service, String alias) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return !service.containsCaAlias(alias);
                case USER:
                    return false;
            }
            throw new AssertionError();
        }
    }

    private TabHost mTabHost;
    private ArrayList<GroupAdapter> mGroupAdapters = new ArrayList<>(2);
    private AliasOperation mAliasOperation;
    private ArraySet<Integer> mConfirmedCredentialUsers;
    private int mConfirmingCredentialUser;
    private IntConsumer mConfirmingCredentialListener;
    private Set<AdapterData.AliasLoader> mAliasLoaders = new ArraySet<AdapterData.AliasLoader>(2);
    private final SparseArray<KeyChainConnection>
            mKeyChainConnectionByProfileId = new SparseArray<KeyChainConnection>();

    private BroadcastReceiver mWorkProfileChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action) ||
                    Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action) ||
                    Intent.ACTION_MANAGED_PROFILE_UNLOCKED.equals(action)) {
                for (GroupAdapter adapter : mGroupAdapters) {
                    adapter.load();
                }
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mKeyguardManager = (KeyguardManager) getActivity()
                .getSystemService(Context.KEYGUARD_SERVICE);
        mTrustAllCaUserId = getActivity().getIntent().getIntExtra(ARG_SHOW_NEW_FOR_USER,
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

        mConfirmingCredentialListener = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
        getActivity().registerReceiver(mWorkProfileChangedReceiver, filter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(SAVED_CONFIRMED_CREDENTIAL_USERS, new ArrayList<>(
                mConfirmedCredentialUsers));
        outState.putInt(SAVED_CONFIRMING_CREDENTIAL_USER, mConfirmingCredentialUser);
    }

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mTabHost = (TabHost) inflater.inflate(R.layout.trusted_credentials, parent, false);
        mTabHost.setup();
        addTab(Tab.SYSTEM);
        // TODO add Install button on Tab.USER to go to CertInstaller like KeyChainActivity
        addTab(Tab.USER);
        if (getActivity().getIntent() != null &&
                USER_ACTION.equals(getActivity().getIntent().getAction())) {
            mTabHost.setCurrentTabByTag(Tab.USER.mTag);
        }
        return mTabHost;
    }
    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mWorkProfileChangedReceiver);
        for (AdapterData.AliasLoader aliasLoader : mAliasLoaders) {
            aliasLoader.cancel(true);
        }
        mAliasLoaders.clear();
        mGroupAdapters.clear();
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
        final int n = mKeyChainConnectionByProfileId.size();
        for (int i = 0; i < n; ++i) {
            mKeyChainConnectionByProfileId.valueAt(i).close();
        }
        mKeyChainConnectionByProfileId.clear();
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = mTabHost.newTabSpec(tab.mTag)
                .setIndicator(getActivity().getString(tab.mLabel))
                .setContent(tab.mView);
        mTabHost.addTab(systemSpec);

        final int profilesSize = mUserManager.getUserProfiles().size();
        final GroupAdapter groupAdapter = new GroupAdapter(tab);
        mGroupAdapters.add(groupAdapter);

        if (profilesSize == 1) {
            final ChildAdapter adapter = groupAdapter.getChildAdapter(0);
            adapter.setContainerViewId(tab.mPersonalList);
            adapter.prepare();
        } else if (profilesSize == 2) {
            final int workIndex = groupAdapter.getUserInfoByGroup(1).isManagedProfile() ? 1 : 0;
            final int personalIndex = workIndex == 1 ? 0 : 1;

            final ChildAdapter personalAdapter = groupAdapter.getChildAdapter(personalIndex);
            personalAdapter.setContainerViewId(tab.mPersonalList);
            personalAdapter.showHeader(true);
            personalAdapter.prepare();

            final ChildAdapter workAdapter = groupAdapter.getChildAdapter(workIndex);
            workAdapter.setContainerViewId(tab.mWorkList);
            workAdapter.showHeader(true);
            workAdapter.showDivider(true);
            workAdapter.prepare();
        } else if (profilesSize >= 3) {
            groupAdapter.setExpandableListView(
                    (ExpandableListView) mTabHost.findViewById(tab.mExpandableList));
        }
    }

    /**
     * Start work challenge activity.
     * @return true if screenlock exists
     */
    private boolean startConfirmCredential(int userId) {
        final Intent newIntent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null,
                userId);
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

            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            if (getUserInfoByGroup(groupPosition).isManagedProfile()) {
                title.setText(R.string.category_work);
            } else {
                title.setText(R.string.category_personal);
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

        public void setExpandableListView(ExpandableListView lv) {
            lv.setAdapter(this);
            lv.setOnGroupClickListener(this);
            lv.setOnChildClickListener(this);
            lv.setVisibility(View.VISIBLE);
        }

        public ChildAdapter getChildAdapter(int groupPosition) {
            return new ChildAdapter(this, groupPosition);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition) {
            return checkGroupExpandableAndStartWarningActivity(groupPosition, true);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition,
                boolean startActivity) {
            final UserHandle groupUser = getGroup(groupPosition);
            final int groupUserId = groupUser.getIdentifier();
            if (mUserManager.isQuietModeEnabled(groupUser)) {
                final Intent intent = UnlaunchableAppActivity.createInQuietModeDialogIntent(
                        groupUserId);
                if (startActivity) {
                    getActivity().startActivity(intent);
                }
                return false;
            } else if (!mUserManager.isUserUnlocked(groupUser)) {
                final LockPatternUtils lockPatternUtils = new LockPatternUtils(
                        getActivity());
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
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.trusted_credential, parent, false);
                holder = new ViewHolder();
                holder.mSubjectPrimaryView = (TextView)
                        convertView.findViewById(R.id.trusted_credential_subject_primary);
                holder.mSubjectSecondaryView = (TextView)
                        convertView.findViewById(R.id.trusted_credential_subject_secondary);
                holder.mSwitch = (Switch) convertView.findViewById(
                        R.id.trusted_credential_status);
                convertView.setTag(holder);
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
            }
            return convertView;
        }

        private class ViewHolder {
            private TextView mSubjectPrimaryView;
            private TextView mSubjectSecondaryView;
            private Switch mSwitch;
        }
    }

    private class ChildAdapter extends BaseAdapter implements View.OnClickListener,
            AdapterView.OnItemClickListener {
        private final int[] GROUP_EXPANDED_STATE_SET = {com.android.internal.R.attr.state_expanded};
        private final int[] EMPTY_STATE_SET = {};
        private final LinearLayout.LayoutParams HIDE_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        private final LinearLayout.LayoutParams SHOW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
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
                ChildAdapter.super.notifyDataSetChanged();
            }
            @Override
            public void onInvalidated() {
                super.onInvalidated();
                ChildAdapter.super.notifyDataSetInvalidated();
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

        @Override public int getCount() {
            return mParent.getChildrenCount(mGroupPosition);
        }
        @Override public CertHolder getItem(int position) {
            return mParent.getChild(mGroupPosition, position);
        }
        @Override public long getItemId(int position) {
            return mParent.getChildId(mGroupPosition, position);
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
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

        public void setContainerViewId(int viewId) {
            mContainerView = (LinearLayout) mTabHost.findViewById(viewId);
            mContainerView.setVisibility(View.VISIBLE);

            mListView = (ListView) mContainerView.findViewById(R.id.cert_list);
            mListView.setAdapter(this);
            mListView.setOnItemClickListener(this);

            mHeaderView = (ViewGroup) mContainerView.findViewById(R.id.header_view);
            mHeaderView.setOnClickListener(this);

            mIndicatorView = (ImageView) mHeaderView.findViewById(R.id.group_indicator);
            mIndicatorView.setImageDrawable(getGroupIndicator());

            FrameLayout headerContentContainer = (FrameLayout)
                    mHeaderView.findViewById(R.id.header_content_container);
            headerContentContainer.addView(
                    mParent.getGroupView(mGroupPosition, true /* parent ignores it */, null,
                            headerContentContainer));
        }

        public void showHeader(boolean showHeader) {
            mHeaderView.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        }

        public void showDivider(boolean showDivider) {
            View dividerView = mHeaderView.findViewById(R.id.header_divider);
            dividerView.setVisibility(showDivider ? View.VISIBLE : View.GONE );
        }

        public void prepare() {
            mIsListExpanded = mParent.checkGroupExpandableAndStartWarningActivity(mGroupPosition,
                    false /* startActivity */);
            refreshViews();
        }

        private boolean checkGroupExpandableAndStartWarningActivity() {
            return mParent.checkGroupExpandableAndStartWarningActivity(mGroupPosition);
        }

        private void refreshViews() {
            mIndicatorView.setImageState(mIsListExpanded ? GROUP_EXPANDED_STATE_SET
                    : EMPTY_STATE_SET, false);
            mListView.setVisibility(mIsListExpanded ? View.VISIBLE : View.GONE);
            mContainerView.setLayoutParams(mIsListExpanded ? SHOW_LAYOUT_PARAMS
                    : HIDE_LAYOUT_PARAMS);
        }

        // Get group indicator from styles of ExpandableListView
        private Drawable getGroupIndicator() {
            final TypedArray a = getActivity().obtainStyledAttributes(null,
                    com.android.internal.R.styleable.ExpandableListView,
                    com.android.internal.R.attr.expandableListViewStyle, 0);
            Drawable groupIndicator = a.getDrawable(
                    com.android.internal.R.styleable.ExpandableListView_groupIndicator);
            a.recycle();
            return groupIndicator;
        }
    }

    private class AdapterData {
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId =
                new SparseArray<List<CertHolder>>();
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

            public AliasLoader() {
                mContext = getActivity();
                mAliasLoaders.add(this);
                List<UserHandle> profiles = mUserManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                    mCertHoldersByUserId.put(profile.getIdentifier(), new ArrayList<CertHolder>());
                }
            }

            private boolean shouldSkipProfile(UserHandle userHandle) {
                return mUserManager.isQuietModeEnabled(userHandle)
                        || !mUserManager.isUserUnlocked(userHandle.getIdentifier());
            }

            @Override protected void onPreExecute() {
                View content = mTabHost.getTabContentView();
                mProgressBar = (ProgressBar) content.findViewById(mTab.mProgress);
                mContentView = content.findViewById(mTab.mContentView);
                mProgressBar.setVisibility(View.VISIBLE);
                mContentView.setVisibility(View.GONE);
            }
            @Override protected SparseArray<List<CertHolder>> doInBackground(Void... params) {
                SparseArray<List<CertHolder>> certHoldersByProfile =
                        new SparseArray<List<CertHolder>>();
                try {
                    List<UserHandle> profiles = mUserManager.getUserProfiles();
                    final int n = profiles.size();
                    // First we get all aliases for all profiles in order to show progress
                    // correctly. Otherwise this could all be in a single loop.
                    SparseArray<List<ParcelableString>> aliasesByProfileId = new SparseArray<
                            List<ParcelableString>>(n);
                    int max = 0;
                    int progress = 0;
                    for (int i = 0; i < n; ++i) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        if (shouldSkipProfile(profile)) {
                            continue;
                        }
                        KeyChainConnection keyChainConnection = KeyChain.bindAsUser(mContext,
                                profile);
                        // Saving the connection for later use on the certificate dialog.
                        mKeyChainConnectionByProfileId.put(profileId, keyChainConnection);
                        IKeyChainService service = keyChainConnection.getService();
                        List<ParcelableString> aliases = mTab.getAliases(service);
                        if (isCancelled()) {
                            return new SparseArray<List<CertHolder>>();
                        }
                        max += aliases.size();
                        aliasesByProfileId.put(profileId, aliases);
                    }
                    for (int i = 0; i < n; ++i) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        List<ParcelableString> aliases = aliasesByProfileId.get(profileId);
                        if (isCancelled()) {
                            return new SparseArray<List<CertHolder>>();
                        }
                        KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                                profileId);
                        if (shouldSkipProfile(profile) || aliases == null
                                || keyChainConnection == null) {
                            certHoldersByProfile.put(profileId, new ArrayList<CertHolder>(0));
                            continue;
                        }
                        IKeyChainService service = keyChainConnection.getService();
                        List<CertHolder> certHolders = new ArrayList<CertHolder>(max);
                        final int aliasMax = aliases.size();
                        for (int j = 0; j < aliasMax; ++j) {
                            String alias = aliases.get(j).string;
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
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote exception while loading aliases.", e);
                    return new SparseArray<List<CertHolder>>();
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException while loading aliases.", e);
                    return new SparseArray<List<CertHolder>>();
                }
            }
            @Override protected void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0];
                int max = progressAndMax[1];
                if (max != mProgressBar.getMax()) {
                    mProgressBar.setMax(max);
                }
                mProgressBar.setProgress(progress);
            }
            @Override protected void onPostExecute(SparseArray<List<CertHolder>> certHolders) {
                mCertHoldersByUserId.clear();
                final int n = certHolders.size();
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
                final DevicePolicyManager dpm = mContext.getSystemService(
                        DevicePolicyManager.class);
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
                final List<CertHolder> certs = mCertHoldersByUserId.get(certHolder.mProfileId);
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
        @Override public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            if (primary != 0) {
                return primary;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return mAlias.equals(other.mAlias);
        }
        @Override public int hashCode() {
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
        final CertHolder[] arr = unapprovedCertHolders.toArray(
                new CertHolder[unapprovedCertHolders.size()]);
        new TrustedCredentialsDialogBuilder(getActivity(), this)
                .setCertHolders(arr)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        // Avoid starting dialog again after Activity restart.
                        getActivity().getIntent().removeExtra(ARG_SHOW_NEW_FOR_USER);
                        mTrustAllCaUserId = UserHandle.USER_NULL;
                    }
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
            KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                    certHolder.mProfileId);
            IKeyChainService service = keyChainConnection.getService();
            List<String> chain = service.getCaCertificateChainAliases(certHolder.mAlias, true);
            final int n = chain.size();
            certificates = new ArrayList<X509Certificate>(n);
            for (int i = 0; i < n; ++i) {
                byte[] encodedCertificate = service.getEncodedCaCertificate(chain.get(i), true);
                X509Certificate certificate = KeyChain.toCertificate(encodedCertificate);
                certificates.add(certificate);
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
