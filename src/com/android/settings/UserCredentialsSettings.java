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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.EnumSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class UserCredentialsSettings extends InstrumentedFragment implements OnItemClickListener {
    private static final String TAG = "UserCredentialsSettings";

    private View mRootView;
    private ListView mListView;

    @Override
    protected int getMetricsCategory() {
        // TODO (rgl): Declare a metrics category for user credentials.
        return UNDECLARED;
    }

    @Override
    public void onResume() {
        super.onResume();
        new AliasLoader().execute();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.user_credentials, parent, false);

        // Set up an OnItemClickListener for the credential list.
        mListView = (ListView) mRootView.findViewById(R.id.credential_list);
        mListView.setOnItemClickListener(this);

        return mRootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Credential item = (Credential) parent.getItemAtPosition(position);

        View root = getActivity().getLayoutInflater()
                .inflate(R.layout.user_credential_dialog, null);
        ViewGroup infoContainer = (ViewGroup) root.findViewById(R.id.credential_container);
        infoContainer.addView(parent.getAdapter().getView(position, null, null));

        new AlertDialog.Builder(getActivity())
                .setView(root)
                .setTitle(R.string.user_credential_title)
                .setPositiveButton(R.string.done, null)
                .setNegativeButton(R.string.trusted_credentials_remove_label,
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int id) {
                                final KeyStore ks = KeyStore.getInstance();
                                Credentials.deleteAllTypesForAlias(ks, item.alias);
                                new AliasLoader().execute();
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    /**
     * Opens a background connection to KeyStore to list user credentials.
     * The credentials are stored in a {@link CredentialAdapter} attached to the main
     * {@link ListView} in the fragment.
     */
    private class AliasLoader extends AsyncTask<Void, Void, ListAdapter> {
        @Override
        protected ListAdapter doInBackground(Void... params) {
            // Create a list of names for credential sets, ordered by name.
            SortedMap<String, Credential> credentials = new TreeMap<>();
            KeyStore keyStore = KeyStore.getInstance();
            for (final Credential.Type type : Credential.Type.values()) {
                for (final String alias : keyStore.list(type.prefix)) {
                    Credential c = credentials.get(alias);
                    if (c == null) {
                        credentials.put(alias, (c = new Credential(alias)));
                    }
                    c.storedTypes.add(type);
                }
            }

            // Flatten to array so that the list can be presented via ArrayAdapter.
            Credential[] results = credentials.values().toArray(new Credential[0]);
            return new CredentialAdapter(getActivity(), R.layout.user_credential, results);
        }

        @Override
        protected void onPostExecute(ListAdapter credentials) {
            mListView.setAdapter(credentials);
        }
    }

    /**
     * Helper class to display {@link Credential}s in a list.
     */
    private static class CredentialAdapter extends ArrayAdapter<Credential> {
        public CredentialAdapter(Context context, int resource,  Credential[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.user_credential, parent, false);
            }
            Credential item = getItem(position);
            ((TextView) view.findViewById(R.id.alias)).setText(item.alias);
            view.findViewById(R.id.contents_userkey).setVisibility(
                    item.storedTypes.contains(Credential.Type.USER_PRIVATE_KEY) ? VISIBLE : GONE);
            view.findViewById(R.id.contents_usercrt).setVisibility(
                    item.storedTypes.contains(Credential.Type.USER_CERTIFICATE) ? VISIBLE : GONE);
            view.findViewById(R.id.contents_cacrt).setVisibility(
                    item.storedTypes.contains(Credential.Type.CA_CERTIFICATE) ? VISIBLE : GONE);
            return view;
        }
    }

    private static class Credential {
        private static enum Type {
            CA_CERTIFICATE (Credentials.CA_CERTIFICATE),
            USER_CERTIFICATE (Credentials.USER_CERTIFICATE),
            USER_PRIVATE_KEY (Credentials.USER_PRIVATE_KEY),
            USER_SECRET_KEY (Credentials.USER_SECRET_KEY);

            final String prefix;

            Type(String prefix) {
                this.prefix = prefix;
            }
        }

        /**
         * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
         * prefixes from {@link CredentialItem.storedTypes}.
         */
        final String alias;

        /**
         * Should contain some non-empty subset of:
         * <ul>
         *   <li>{@link Credentials.CA_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_PRIVATE_KEY}</li>
         *   <li>{@link Credentials.USER_SECRET_KEY}</li>
         * </ul>
         */
        final Set<Type> storedTypes = EnumSet.noneOf(Type.class);

        Credential(final String alias) {
            this.alias = alias;
        }
    }
}
