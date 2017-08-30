package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.ItemEditActivity;
import com.lingju.assistant.activity.event.AccountItemEvent;
import com.lingju.model.Item;
import com.lingju.model.dao.AccountItemDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2017/2/27.
 */
public class ItemIncomeDialog extends Dialog {

    private Context mContext;
    private String item;
    private OnItemIncomeListener mItemListener;
    private AccountItemDao dao;
    private ListView itemListView;
    private LayoutInflater inflater;
    private List<Item> items = new ArrayList<>();
    private int seletectedItemPosition = -1;

    public ItemIncomeDialog(Context context, String item, OnItemIncomeListener listener) {
        super(context, R.style.full_dialog);
        this.mContext = context;
        this.item = item;
        this.mItemListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_income);
        dao = AccountItemDao.getInstance();
        EventBus.getDefault().register(this);
        itemListView = (ListView) findViewById(R.id.aii_item_list);
        items = dao.findAllByExpense(0);
        inflater = LayoutInflater.from(mContext);
        itemListView.setAdapter(itemAdapter);
     //   itemListView.setOnItemClickListener(itemItemListener);

        findViewById(R.id.aii_back).setOnClickListener(clickListener);
        findViewById(R.id.aii_edit).setOnClickListener(clickListener);

        if (!TextUtils.isEmpty(item)) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getItem().equals(item)) {
                    seletectedItemPosition = i;
                    break;
                }
            }
        }
        itemListView.setSelection(seletectedItemPosition);
    }

    private int dp2px(int dp) {
        return (int) (mContext.getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onItemEvent(AccountItemEvent e) {
        items = dao.findAllByExpense(0);
        itemAdapter.notifyDataSetChanged();
    }

    @Override
    public void cancel() {
        super.cancel();
        EventBus.getDefault().unregister(this);
    }

   /* @Override
    public void show() {
        Window dialogWindow =getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.LEFT|Gravity.TOP);
        WindowManager m = ((Activity)mContext).getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = d.getWidth();
        lp.x = 0;
        lp.y = 0;
        dialogWindow.setAttributes(lp);
        super.show();
    }*/


//    private AdapterView.OnItemClickListener itemItemListener = new AdapterView.OnItemClickListener() {
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position,
//                                long id) {
//            seletectedItemPosition = position;
//            itemAdapter.notifyDataSetChanged();
//            if (seletectedItemPosition > -1) {
//                if (mItemListener != null) {
//                    mItemListener.onIncomeSelected(items.get(seletectedItemPosition).getItem());
//                }
//            }
//            cancel();
//        }
//    };

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.aii_back:
                    if (seletectedItemPosition > -1) {
                        if (mItemListener != null) {
                            mItemListener.onIncomeSelected(items.get(seletectedItemPosition).getItem());
                        }
                    }
                    cancel();
                    break;
                case R.id.aii_edit:
                    Intent it = new Intent(mContext, ItemEditActivity.class);
                    it.putExtra(ItemEditActivity.TYPE, ItemEditActivity.INCOME);
                    mContext.startActivity(it);
                    ((Activity) mContext).overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                    break;
            }

        }
    };

    private BaseAdapter itemAdapter = new BaseAdapter() {

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.account_sub_item, null);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.account_sub_item_text);
            tv.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
            tv.setText(items.get(position).getItem());
            AppCompatRadioButton acrb = (AppCompatRadioButton) convertView.findViewById(R.id.aitt_button);

            convertView.findViewById(R.id.account_sub_item).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seletectedItemPosition = position;
                    itemAdapter.notifyDataSetChanged();
                    if (seletectedItemPosition > -1) {
                        if (mItemListener != null) {
                            mItemListener.onIncomeSelected(items.get(seletectedItemPosition).getItem());
                        }
                    }
                    cancel();
                }
            });
            // ((TextView) ((LinearLayout) convertView).getChildAt(0)).setText(showSubItems.get(position).getName());
            if (position == seletectedItemPosition) {
                acrb.setVisibility(View.VISIBLE);
            } else {
                acrb.setVisibility(View.GONE);
            }
            return convertView;
//            if (convertView == null) {
//                convertView = new LinearLayout(mContext);
//
//                AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, dp2px(64));
//                convertView.setLayoutParams(lp);
//                ((LinearLayout) convertView).setOrientation(LinearLayout.HORIZONTAL);
//                TextView tv = new TextView(mContext);
//                tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//                tv.setTextColor(mContext.getResources().getColorStateList(R.color.new_text_color_first));
//                LinearLayout.LayoutParams llpt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
//                llpt.weight = 1.0f;
//
//                LinearLayout.LayoutParams llpi = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                ImageView iv = new ImageView(mContext);
//                iv.setBackgroundResource(R.drawable.ic_green_checkbox);
//                iv.setVisibility(View.GONE);
//                llpi.gravity = Gravity.CENTER_VERTICAL;
//
//                ((LinearLayout) convertView).addView(tv, 0, llpt);
//                ((LinearLayout) convertView).addView(iv, 1, llpi);
//            }
//            ((TextView) ((LinearLayout) convertView).getChildAt(0)).setText(items.get(position).getItem());
//            if (position == seletectedItemPosition) {
//                ((LinearLayout) convertView).getChildAt(1).setVisibility(View.VISIBLE);
//            } else {
//                ((LinearLayout) convertView).getChildAt(1).setVisibility(View.GONE);
//            }
//            return convertView;
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

    public interface OnItemIncomeListener {
        void onIncomeSelected(String item);
    }
}
