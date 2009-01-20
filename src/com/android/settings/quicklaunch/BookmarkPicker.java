/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.quicklaunch;

import com.android.settings.R;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Activity to pick a bookmark that will be returned to the caller.
 * <p>
 * Currently, bookmarks are either:
 * <li> Activities that are in the launcher
 * <li> Activities that are within an app that is capable of being launched with
 * the {@link Intent#ACTION_CREATE_SHORTCUT}.
 */
public class BookmarkPicker extends ListActivity implements SimpleAdapter.ViewBinder {

    private static final String TAG = "BookmarkPicker";

    /** Extra in the returned intent from this activity. */
    public static final String EXTRA_TITLE = "com.android.settings.quicklaunch.TITLE";
    
    /** Extra that should be provided, and will be returned. */
    public static final String EXTRA_SHORTCUT = "com.android.settings.quicklaunch.SHORTCUT";

    /**
     * The request code for the screen to create a bookmark that is WITHIN an
     * application. For example, Gmail can return a bookmark for the inbox
     * folder.
     */
    private static final int REQUEST_CREATE_SHORTCUT = 1;

    /** Intent used to get all the activities that are launch-able */
    private static Intent sLaunchIntent;
    /** Intent used to get all the activities that are {@link #REQUEST_CREATE_SHORTCUT}-able */
    private static Intent sShortcutIntent;
    
    /**
     * List of ResolveInfo for activities that we can bookmark (either directly
     * to the activity, or by launching the activity and it returning a bookmark
     * WITHIN that application).
     */
    private List<ResolveInfo> mResolveList;
    
    // List adapter stuff
    private static final String KEY_TITLE = "TITLE";
    private static final String KEY_RESOLVE_INFO = "RESOLVE_INFO";
    private static final String sKeys[] = new String[] { KEY_TITLE, KEY_RESOLVE_INFO };
    private static final int sResourceIds[] = new int[] { R.id.title, R.id.icon };
    private SimpleAdapter mMyAdapter;

    /** Display those activities that are launch-able */
    private static final int DISPLAY_MODE_LAUNCH = 0;
    /** Display those activities that are able to have bookmarks WITHIN the application */
    private static final int DISPLAY_MODE_SHORTCUT = 1;
    private int mDisplayMode = DISPLAY_MODE_LAUNCH;
    
    private Handler mUiHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateListAndAdapter();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, DISPLAY_MODE_LAUNCH, 0, R.string.quick_launch_display_mode_applications)
                .setIcon(com.android.internal.R.drawable.ic_menu_archive);
        menu.add(0, DISPLAY_MODE_SHORTCUT, 0, R.string.quick_launch_display_mode_shortcuts)
                .setIcon(com.android.internal.R.drawable.ic_menu_goto);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(DISPLAY_MODE_LAUNCH).setVisible(mDisplayMode != DISPLAY_MODE_LAUNCH);
        menu.findItem(DISPLAY_MODE_SHORTCUT).setVisible(mDisplayMode != DISPLAY_MODE_SHORTCUT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {

            case DISPLAY_MODE_LAUNCH: 
                mDisplayMode = DISPLAY_MODE_LAUNCH;
                break;
            
            case DISPLAY_MODE_SHORTCUT:
                mDisplayMode = DISPLAY_MODE_SHORTCUT;
                break;
            
            default:
                return false;
        }
        
        updateListAndAdapter();
        return true;
    }

    private void ensureIntents() {
        if (sLaunchIntent == null) {
            sLaunchIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            sShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        }
    }

    /**
     * This should be called from the UI thread.
     */
    private void updateListAndAdapter() {
        // Get the activities in a separate thread
        new Thread("data updater") {
            @Override
            public void run() {
                synchronized (BookmarkPicker.this) {
                    /*
                     * Don't touch any of the lists that are being used by the
                     * adapter in this thread!
                     */
                    ArrayList<ResolveInfo> newResolveList = new ArrayList<ResolveInfo>();
                    ArrayList<Map<String, ?>> newAdapterList = new ArrayList<Map<String, ?>>();

                    fillResolveList(newResolveList);
                    Collections.sort(newResolveList,
                            new ResolveInfo.DisplayNameComparator(getPackageManager()));
                    
                    fillAdapterList(newAdapterList, newResolveList);
                    
                    updateAdapterToUseNewLists(newAdapterList, newResolveList);
                }
            }
        }.start();  
    }
    
    private void updateAdapterToUseNewLists(final ArrayList<Map<String, ?>> newAdapterList,
            final ArrayList<ResolveInfo> newResolveList) {
        // Post this back on the UI thread
        mUiHandler.post(new Runnable() {
            public void run() {
                /*
                 * SimpleAdapter does not support changing the lists after it
                 * has been created. We just create a new instance.
                 */
                mMyAdapter = createResolveAdapter(newAdapterList);
                mResolveList = newResolveList;
                setListAdapter(mMyAdapter);
            }
        });
    }
    
    /**
     * Gets all activities matching our current display mode.
     * 
     * @param list The list to fill.
     */
    private void fillResolveList(List<ResolveInfo> list) {
        ensureIntents();
        PackageManager pm = getPackageManager();
        list.clear();
        
        if (mDisplayMode == DISPLAY_MODE_LAUNCH) {
            list.addAll(pm.queryIntentActivities(sLaunchIntent, 0));
        } else if (mDisplayMode == DISPLAY_MODE_SHORTCUT) {
            list.addAll(pm.queryIntentActivities(sShortcutIntent, 0)); 
        }
    }
    
    private SimpleAdapter createResolveAdapter(List<Map<String, ?>> list) {
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.bookmark_picker_item, sKeys, sResourceIds);
        adapter.setViewBinder(this);
        return adapter;
    }

    private void fillAdapterList(List<Map<String, ?>> list,
            List<ResolveInfo> resolveList) {
        list.clear();
        int resolveListSize = resolveList.size();
        for (int i = 0; i < resolveListSize; i++) {
            ResolveInfo info = resolveList.get(i);
            /*
             * Simple adapter craziness. For each item, we need to create a map
             * from a key to its value (the value can be any object--the view
             * binder will take care of filling the View with a representation
             * of that object).
             */
            Map<String, Object> map = new TreeMap<String, Object>();
            map.put(KEY_TITLE, getResolveInfoTitle(info));
            map.put(KEY_RESOLVE_INFO, info);
            list.add(map);
        }
    }

    /** Get the title for a resolve info. */
    private String getResolveInfoTitle(ResolveInfo info) {
        CharSequence label = info.loadLabel(getPackageManager());
        if (label == null) label = info.activityInfo.name;
        return label != null ? label.toString() : null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position >= mResolveList.size()) return;

        ResolveInfo info = mResolveList.get(position);
        
        switch (mDisplayMode) {

            case DISPLAY_MODE_LAUNCH: 
                // We can go ahead and return the clicked info's intent
                Intent intent = getIntentForResolveInfo(info, Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                finish(intent, getResolveInfoTitle(info));
                break;

            case DISPLAY_MODE_SHORTCUT:
                // Start the shortcut activity so the user can pick the actual intent
                // (example: Gmail's shortcut activity shows a list of mailboxes)
                startShortcutActivity(info);
                break;
        }
        
    }
    
    private static Intent getIntentForResolveInfo(ResolveInfo info, String action) {
        Intent intent = new Intent(action);
        ActivityInfo ai = info.activityInfo;
        intent.setClassName(ai.packageName, ai.name);
        return intent;
    }

    /**
     * Starts an activity to get a shortcut.
     * <p>
     * For example, Gmail has an activity that lists the available labels. It
     * returns a shortcut intent for going directly to this label.
     */
    private void startShortcutActivity(ResolveInfo info) {
        Intent intent = getIntentForResolveInfo(info, Intent.ACTION_CREATE_SHORTCUT);
        startActivityForResult(intent, REQUEST_CREATE_SHORTCUT);
        
        // Will get a callback to onActivityResult
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        
        switch (requestCode) {
            
            case REQUEST_CREATE_SHORTCUT:
                if (data != null) {
                    finish((Intent) data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT),
                            data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
                }
                break;
                
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
    
    /**
     * Finishes the activity and returns the given data.
     */
    private void finish(Intent intent, String title) {
        // Give back what was given to us (it will have the shortcut, for example)
        intent.putExtras(getIntent());
        // Put our information
        intent.putExtra(EXTRA_TITLE, title);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * {@inheritDoc}
     */
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        if (view.getId() == R.id.icon) {
            Drawable icon = ((ResolveInfo) data).loadIcon(getPackageManager());
            if (icon != null) {
                ((ImageView) view).setImageDrawable(icon);
            }
            return true;
        } else {
            return false;
        }
    }
    
}
