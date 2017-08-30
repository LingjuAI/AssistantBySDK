package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.model.BaiduAddress;

/**
 * Created by Administrator on 2015/7/14.
 */
public class LocationSuggestItemView extends LocationItemView {
    private final static String TAG="LocationFavoriteItemView";

    private ImageButton searchBt;

    public LocationSuggestItemView(Context context, BaiduAddress address, String keyword) {
        super(context,address,keyword);
    }

    public LocationSuggestItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocationSuggestItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(String keyword) {
        LayoutInflater.from(getContext()).inflate(R.layout.location_item_search_suggest, this);
        nameText= (TextView) findViewById(R.id.loc_item_favorite_name_text);
        addressText=(TextView)findViewById(R.id.loc_item_favorite_address_text);
       // searchBt=(ImageButton)findViewById(R.id.loc_item_search_bt);
        if(this.address!=null){
            setAddress(address,keyword);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
       // searchBt.setOnClickListener(l);
    }
}
