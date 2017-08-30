package com.lingju.assistant.view;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.model.BaiduAddress;

/**
 * Created by Administrator on 2015/7/14.
 */
public class LocationFavoriteItemView extends LocationItemView{
    private final static String TAG = "LocationFavoriteItemView";

    protected TextView nameText;
    protected TextView addressText;
    protected BaiduAddress address;


    public LocationFavoriteItemView(Context context, BaiduAddress address, String keyword) {
        super(context,address,keyword);
    }

    public LocationFavoriteItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocationFavoriteItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init(String keyword){
        LayoutInflater.from(getContext()).inflate(R.layout.location_search_item_favorite, this);
        nameText= (TextView) findViewById(R.id.loc_item_favorite_name_text);
        addressText=(TextView)findViewById(R.id.loc_item_favorite_address_text);
        if(this.address!=null){
            setAddress(address,keyword);
        }
    }

    public void setAddress(BaiduAddress address) {
        setAddress(address, null);
    }

    public void setAddress(BaiduAddress address,String keyword) {
        this.address = address;
        addressText.setText(address.getAddress());
        if(TextUtils.isEmpty(address.getAddress())){
            addressText.setVisibility(View.GONE);
        }
        else{
            addressText.setVisibility(View.VISIBLE);
        }
        if(!TextUtils.isEmpty(keyword)){
            StringBuffer nameBuffer=new StringBuffer(address.getName());
            int s;
            if((s=address.getName().indexOf(keyword))!=-1){
                int e=s+keyword.length();
                nameBuffer.replace(s,e,"<font color='#32c0c4'>"+keyword+"</font>");
                nameText.setText(Html.fromHtml(nameBuffer.toString()));
                return;
            }
        }
        nameText.setText(getAddressShowName(address));
    }


    protected String getAddressShowName(BaiduAddress ba){
        if(TextUtils.isEmpty(ba.getRemark())){
            return ba.getName();
        }
        else if(ba.getRemark().equals("家")||ba.getRemark().equals("单位")){
            return ba.getName();
        }
        else{
            return  ba.getRemark();
        }
    }

    public void setUnderline(){
        getChildAt(0).setBackgroundResource(R.drawable.gray_underline_bg);
    }

    public void removeUnderLine(){
        getChildAt(0).setBackgroundResource(R.drawable.gray_no_underline_bg);
    }

    public BaiduAddress getAddress() {
        return address;
    }
    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
        // searchBt.setOnClickListener(l);
    }

}
