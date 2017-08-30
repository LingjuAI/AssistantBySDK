package com.lingju.assistant.view;

/**
 * Created by Administrator on 2015/7/14.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lingju.model.BaiduAddress;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public abstract class AdaptHeightListView<T extends LocationItemView> extends LinearLayout {
    private final static String TAG="AdaptHeightListView";

    protected List<BaiduAddress> list= new ArrayList<>();
    protected Constructor<T> constructor;
    protected Class<T> itemClass;
    protected OnItemClickListener itemClickListener;

    public AdaptHeightListView(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setItemClass();
    }

    public AdaptHeightListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        setItemClass();
    }

    public AdaptHeightListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(LinearLayout.VERTICAL);
        setItemClass();
    }

    private void setItemClass(){
        Type genType = getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        itemClass = (Class) params[0];
        try {
            constructor = itemClass.getConstructor(Context.class,BaiduAddress.class,String.class);
            if(constructor==null)return;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void loadDate(List<BaiduAddress> datas){
        loadDate(datas,null);
    }

    public void loadDate(List<BaiduAddress> datas,String keyword){
        if(itemClass==null||constructor==null)return;
        list.clear();
        if(datas!=null&&datas.size()>0){
            list.addAll(datas);
        }
        resetItems(keyword);
    }

    @Override
    public T getChildAt(int index) {
        return (T) super.getChildAt(index);
    }

    protected void resetItems(String keyword){
        int l=this.getChildCount();
        int length=list.size();
        try {
            for (int i = 0; i < length; i++) {
                if (i < l) {
                    getChildAt(i).setAddress(list.get(i),keyword);
                } else {
                    this.addView(constructor.newInstance(getContext(), list.get(i),keyword), i);
                    getChildAt(i).setOnClickListener(clickListener);
                    //getChildAt(i).setUnderline();
                }
            }
            if (l > length) {
                while((l=getChildCount())>length){
                    removeViewAt(l-1);
                }
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void removeLastItemUnderline(){
        getChildAt(getChildCount()-1).removeUnderLine();
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    private OnClickListener clickListener=new OnClickListener(){

        @Override
        public void onClick(View v) {
            if(v instanceof LocationItemView){
                if(itemClickListener!=null){
                    itemClickListener.onClick(((LocationItemView)v).getAddress());
                }
            }
//            else if(v instanceof ImageButton&&v.getId()== R.id.loc_item_search_bt){
//                if(itemClickListener!=null){
//                    itemClickListener.onSelect(getLocationItemView(v).getAddress());
//                }
//            }
        }
    };


    private LocationItemView getLocationItemView(View v){
        while(!(v.getParent() instanceof LocationItemView)){
            v=(ViewGroup)v.getParent();
        }
        return (LocationItemView)v.getParent();

    }



    public interface  OnItemClickListener{
        void onClick(BaiduAddress address);
        void onSelect(BaiduAddress address);
    }

}
