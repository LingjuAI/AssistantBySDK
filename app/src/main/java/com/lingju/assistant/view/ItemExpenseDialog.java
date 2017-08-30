package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.ItemEditActivity;
import com.lingju.assistant.activity.event.AccountItemEvent;
import com.lingju.model.Item;
import com.lingju.model.SubItem;
import com.lingju.model.dao.AccountItemDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2017/2/27.
 */
public class ItemExpenseDialog extends Dialog {

    private static final String TAG = "LingJu";
    private Context mContext;
    private String item;
    private AccountItemDao dao;
    private ListView itemListView;
    private ListView subItemListView;
    private LayoutInflater inflater;
    private List<Item> items = new ArrayList<>();
    private List<SubItem> subItems = new ArrayList<>();
    private List<SubItem> showSubItems = new ArrayList<>();

    private long selectedSubItemId = 0;
    private int seletectedItemPosition = 0;
    private OnItemExpenseListener mItemListener;

    public ItemExpenseDialog(Context context, String item, OnItemExpenseListener listener) {
        super(context, R.style.full_dialog);
        this.mContext = context;
        this.item = item;
        this.mItemListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_expense);
        dao = AccountItemDao.getInstance();
        EventBus.getDefault().register(this);
        itemListView = (ListView) findViewById(R.id.aie_item_list);
        subItemListView = (ListView) findViewById(R.id.aie_subitem_list);
        items = dao.findAllByExpense(1);
        subItems = dao.findAllSubItem();
        inflater = LayoutInflater.from(mContext);
        Log.w(TAG, "items.size=" + items.size() + ",subItems.size=" + subItems.size());
        itemListView.setAdapter(itemAdapter);
        itemListView.setOnItemClickListener(itemItemListener);
        subItemListView.setAdapter(subAdapter);
      //  subItemListView.setOnItemClickListener(subItemItemListener);

        findViewById(R.id.aie_back).setOnClickListener(clickListener);
        findViewById(R.id.aie_edit).setOnClickListener(clickListener);

        if (!TextUtils.isEmpty(item)) {
            String is[] = item.split("，");
            if (is.length == 2) {
                boolean r = false;
                for (int i = 0; i < subItems.size(); i++) {
                    if (subItems.get(i).getName().equals(is[1])) {
                        for (int j = 0; j < items.size(); j++) {
                            if (items.get(j).getId() == subItems.get(i).getItemid()) {
                                if (items.get(j).getItem().equals(is[0])) {
                                    selectedSubItemId = subItems.get(i).getId();
                                    seletectedItemPosition = j;
                                    r = true;
                                    break;
                                }
                            }
                        }
                        if (r)
                            break;
                    }
                }
            }
        }
        setShowSubItems(items.get(seletectedItemPosition).getId());
        itemListView.setSelection(seletectedItemPosition);
    }

    private int dp2px(int dp) {
        return (int) (mContext.getResources().getDisplayMetrics().density * dp + 0.5f);
    }


    private SubItem getSubItemById(long id) {
        for (SubItem si : subItems) {
            if (si.getId() == id) {
                return si;
            }
        }
        return null;
    }


    private void setShowSubItems(long itemId) {
        showSubItems.clear();
        for (SubItem si : subItems) {
            if (si.getItemid() == itemId) {
                showSubItems.add(si);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onItemEvent(AccountItemEvent e) {
        items = dao.findAllByExpense(1);
        subItems = dao.findAllSubItem();
        setShowSubItems(items.get(seletectedItemPosition).getId());
        itemAdapter.notifyDataSetChanged();
        subAdapter.notifyDataSetChanged();
    }

    @Override
    public void cancel() {
        super.cancel();
        EventBus.getDefault().unregister(this);
    }

    private AdapterView.OnItemClickListener itemItemListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            setShowSubItems(items.get(position).getId());
            subAdapter.notifyDataSetChanged();
            seletectedItemPosition = position;
            itemAdapter.notifyDataSetChanged();
        }

    };

//    private AdapterView.OnItemClickListener subItemItemListener = new AdapterView.OnItemClickListener() {
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position,
//                                long id) {
//            selectedSubItemId = showSubItems.get(position).getId();
//            Log.i(TAG, "item selected：" + seletectedItemPosition);
//            subAdapter.notifyDataSetChanged();
//            String item = items.get(seletectedItemPosition).getItem() + (getSubItemById(selectedSubItemId) != null ? "," + getSubItemById(selectedSubItemId).getName() : "");
//            if (mItemListener != null) {
//                mItemListener.onItemSelected(item);
//            }
//            cancel();
//        }
//    };

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.aie_back:
                    String item = items.get(seletectedItemPosition).getItem() + (getSubItemById(selectedSubItemId) != null ? "，" + getSubItemById(selectedSubItemId).getName() : "");
                    if (mItemListener != null) {
                        mItemListener.onItemSelected(item);
                    }
                    cancel();
                    break;
                case R.id.aie_edit:
                    Intent it = new Intent(mContext, ItemEditActivity.class);
                    it.putExtra(ItemEditActivity.TYPE, ItemEditActivity.EXPENSE);
                    mContext.startActivity(it);
                    ((Activity) mContext).overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                    break;
            }

        }
    };

    private BaseAdapter itemAdapter = new BaseAdapter() {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(mContext);
                AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, dp2px(64));
                convertView.setLayoutParams(lp);
                ((TextView) convertView).setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                ((TextView) convertView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                ((TextView) convertView).setTextColor(mContext.getResources().getColorStateList(R.color.new_text_color_first));
                //((TextView)convertView).setPadding(dp2px(16), 0, 0, 0);
            }
            ((TextView) convertView).setText(items.get(position).getItem());
            if (position == seletectedItemPosition) {
                convertView.setBackgroundResource(R.color.white);
                ((TextView) convertView).setTextColor(mContext.getResources().getColorStateList(R.color.green_style));
            } else {
                convertView.setBackgroundResource(0);
                ((TextView) convertView).setTextColor(mContext.getResources().getColorStateList(R.color.new_text_color_first));
            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return items.size();
        }
    };


    private BaseAdapter subAdapter = new BaseAdapter() {



        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.account_sub_item, null);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.account_sub_item_text);
            tv.setText(showSubItems.get(position).getName());
            AppCompatRadioButton acrb = (AppCompatRadioButton) convertView.findViewById(R.id.aitt_button);

            convertView.findViewById(R.id.account_sub_item).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedSubItemId = showSubItems.get(position).getId();
                    Log.i(TAG, "selectedSubItemId：>>>>>>" + selectedSubItemId);
                    Log.i(TAG, "seletectedItemPosition：" + seletectedItemPosition);
                    subAdapter.notifyDataSetChanged();
                    String item = items.get(seletectedItemPosition).getItem() + (getSubItemById(selectedSubItemId) != null ? "，" + getSubItemById(selectedSubItemId).getName() : "");
                    if (mItemListener != null) {
                        mItemListener.onItemSelected(item);
                         }
                    cancel();
                }
            });
           // ((TextView) ((LinearLayout) convertView).getChildAt(0)).setText(showSubItems.get(position).getName());
            Log.i(TAG, "selectedSubItemId：" + selectedSubItemId);
            Log.i(TAG, "showSubItems.get(position).getId()：" + showSubItems.get(position).getId());
            if (showSubItems.get(position).getId() == selectedSubItemId) {
                acrb.setVisibility(View.VISIBLE);
            } else {
                acrb.setVisibility(View.GONE);
            }
            return convertView;
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return showSubItems.size();
        }


    };


    public interface OnItemExpenseListener {
        void onItemSelected(String item);
    }
}
