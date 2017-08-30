package com.lingju.assistant.activity.index.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.index.view.TingAlbumFragment;
import com.ximalaya.ting.android.opensdk.model.category.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingPagerAdapter extends FragmentPagerAdapter {

    private List<Category> categorys = new ArrayList<>();

    public TingPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        categorys.clear();
        Category cate = new Category();
        cate.setCategoryName("我的收藏");
        categorys.add(cate);
        //初始化类别
        String[] temps = context.getResources().getStringArray(R.array.ting_category);
        for (String category : temps) {
            String[] cateParams = category.split("-");
            cate = new Category();
            cate.setId(Long.valueOf(cateParams[0]));
            cate.setCategoryName(cateParams[1]);
            categorys.add(cate);
        }
    }

    @Override
    public Fragment getItem(int position) {

        Category category = categorys.get(position);
        TingAlbumFragment fragment = new TingAlbumFragment();
        Bundle args = new Bundle();
        switch (position) {
            case 0:
                args.putInt(TingAlbumFragment.FRAG_TYPE, TingAlbumFragment.FRAG_SUBSCRIBE);
                break;
            default:
                args.putInt(TingAlbumFragment.FRAG_TYPE, TingAlbumFragment.FRAG_CATEGORY);
                args.putLong(TingAlbumFragment.CATEGORY_ID, category.getId());
                break;
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return categorys.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return categorys.get(position).getCategoryName();
    }
}
