package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;

import com.lingju.model.BaiduAddress;

import java.util.List;

/**
 * Created by Administrator on 2015/7/14.
 */
public class LocationFavoriteLimitedListView extends AdaptHeightListView<LocationFavoriteItemView> {
    public LocationFavoriteLimitedListView(Context context) {
        super(context);
    }

    public LocationFavoriteLimitedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocationFavoriteLimitedListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void loadDate(List<BaiduAddress> datas) {
        list.clear();
        if(datas!=null&&datas.size()>0){
//            if(datas.size()>3)
//               list.addAll(datas.subList(0,3));
//            else{
                list.addAll(datas);
//            }
        }
        resetItems(null);
        removeLastItemUnderline();
    }


}
