package com.android.settings.applications;

import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;

public class AppOpsSummary extends Fragment {
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    
    private ViewGroup mContentContainer;
    private View mRootView;
    private ViewPager mViewPager;

    CharSequence[] mPageNames;
    static int[][] sPageOps = new int[][] {
        // "Location" page.
        new int[] { AppOpsManager.OP_COARSE_LOCATION, AppOpsManager.OP_FINE_LOCATION,
                AppOpsManager.OP_GPS },

        // "Personal" page.
        new int[] { AppOpsManager.OP_READ_CONTACTS, AppOpsManager.OP_WRITE_CONTACTS,
                AppOpsManager.OP_READ_CALL_LOG, AppOpsManager.OP_WRITE_CALL_LOG },

        // "Device" page.
        new int[] { AppOpsManager.OP_VIBRATE },
    };
    static String[][] sPagePerms = new String[][] {
        // "Location" page.
        new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION },

        // "Personal" page.
        new String[] { android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.WRITE_CONTACTS,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.WRITE_CALL_LOG },

        // "Device" page.
        new String[] { android.Manifest.permission.VIBRATE },
    };

    int mCurPos;

    class MyPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new AppOpsCategory(sPageOps[position], sPagePerms[position]);
        }

        @Override
        public int getCount() {
            return sPageOps.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageNames[position];
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurPos = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                //updateCurrentTab(mCurPos);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.app_ops_summary,
                container, false);
        mContentContainer = container;
        mRootView = rootView;

        mPageNames = getResources().getTextArray(R.array.app_ops_categories);

        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(adapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        return rootView;
    }
}
