package com.inha.makko;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * Created by user on 2018-04-09.
 */

public class MainViewPagerAdapter extends FragmentStatePagerAdapter {

    private int tabCount;

    public MainViewPagerAdapter(FragmentManager fm, int tabCount) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.tabCount = tabCount;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return MapFragment.newInstance();
            case 1:
                return FriendFragment.newInstance();
        }
        return null;
    }

    @Override
    public int getCount() {
        return tabCount;
    }
}
