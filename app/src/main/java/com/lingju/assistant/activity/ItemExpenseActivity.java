package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.model.Item;
import com.lingju.model.SubItem;
import com.lingju.model.dao.AccountItemDao;

import java.util.ArrayList;
import java.util.List;

public class ItemExpenseActivity extends GoBackActivity {
    private final static String TAG = "ItemExpenseActivity";

    public final static int FOR_SELECT_ITEM = 8;
    public final static String ITEM = "ITEMS";

    private AccountItemDao dao;
    private ListView itemListView;
    private ListView subItemListView;
    private List<Item> items = new ArrayList<>();
    private List<SubItem> subItems = new ArrayList<>();
    private List<SubItem> showSubItems = new ArrayList<>();

    private long selectedSubItemId = 0;
    private int seletectedItemPosition = 0;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_expense);
        dao = AccountItemDao.getInstance();
        itemListView = (ListView) findViewById(R.id.aie_item_list);
        subItemListView = (ListView) findViewById(R.id.aie_subitem_list);
        items = dao.findAllByExpense(1);
        subItems = dao.findAllSubItem();
        Log.w(TAG, "items.size=" + items.size() + ",subItems.size=" + subItems.size());
        itemListView.setAdapter(itemAdapter);
        itemListView.setOnItemClickListener(itemItemListener);
        subItemListView.setAdapter(subAdapter);
        subItemListView.setOnItemClickListener(subItemItemListener);

        findViewById(R.id.aie_back).setOnClickListener(clickListener);
        findViewById(R.id.aie_edit).setOnClickListener(clickListener);

        intent = getIntent();
        if (intent != null) {
            String item = intent.getStringExtra(ITEM);
            Log.w(TAG, "item=" + item);
            if (!TextUtils.isEmpty(item)) {
                String is[] = item.split(",");
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
        }
        setShowSubItems(items.get(seletectedItemPosition).getId());
        itemListView.setSelection(seletectedItemPosition);
    }

    private int dp2px(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
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

    private OnItemClickListener itemItemListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            setShowSubItems(items.get(position).getId());
            subAdapter.notifyDataSetChanged();
            seletectedItemPosition = position;
            itemAdapter.notifyDataSetChanged();
        }

    };

    private OnItemClickListener subItemItemListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            selectedSubItemId = showSubItems.get(position).getId();
            Log.i(TAG, "item selected：" + seletectedItemPosition);
            subAdapter.notifyDataSetChanged();
            if (intent != null) {
                String item = items.get(seletectedItemPosition).getItem() + (getSubItemById(selectedSubItemId) != null ? "," + getSubItemById(selectedSubItemId).getName() : "");
                if (intent.getIntExtra(MainActivity.RESULT_CODE, 0) == 0) {
                    intent.putExtra(ITEM, item);
                    setResult(FOR_SELECT_ITEM, intent);
                } else {
                    /*intent.setClass(ItemExpenseActivity.this, MainActivity.class);
                    startActivity(intent);*/

                }
                goBack();
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (resultCode == ItemEditActivity.FOR_ITEM_EDIT) {
                if (data.getBooleanExtra(ItemEditActivity.UPDATE, false)) {
                    items = dao.findAllByExpense(1);
                    subItems = dao.findAllSubItem();
                    setShowSubItems(items.get(seletectedItemPosition).getId());
                    itemAdapter.notifyDataSetChanged();
                    subAdapter.notifyDataSetChanged();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.aie_back:
                    if (intent != null) {
                        String item = items.get(seletectedItemPosition).getItem() + (getSubItemById(selectedSubItemId) != null ? "," + getSubItemById(selectedSubItemId).getName() : "");
                        if (intent.getIntExtra(MainActivity.RESULT_CODE, 0) == 0) {
                            intent.putExtra(ITEM, item);
                            setResult(FOR_SELECT_ITEM, intent);
                        } else {
                            /*intent.setClass(ItemExpenseActivity.this, MainActivity.class);
                            startActivity(intent);*/
                        }
                    }
                    goBack();
                    break;
                case R.id.aie_edit:
                    Intent it = new Intent(ItemExpenseActivity.this, ItemEditActivity.class);
                    it.putExtra(ItemEditActivity.TYPE, ItemEditActivity.EXPENSE);
                    startActivityForResult(it, ItemEditActivity.FOR_ITEM_EDIT);
                    goInto();
                    break;
            }

        }
    };


    private BaseAdapter itemAdapter = new BaseAdapter() {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(ItemExpenseActivity.this);
                AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, dp2px(64));
                convertView.setLayoutParams(lp);
                ((TextView) convertView).setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                ((TextView) convertView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                ((TextView) convertView).setTextColor(getResources().getColorStateList(R.color.new_text_color_first));
                //((TextView)convertView).setPadding(dp2px(16), 0, 0, 0);
            }
            ((TextView) convertView).setText(items.get(position).getItem());
            if (position == seletectedItemPosition) {
                convertView.setBackgroundResource(R.color.new_main_bgcolor);
                ((TextView) convertView).setTextColor(getResources().getColorStateList(R.color.green_style));
            } else {
                convertView.setBackgroundResource(0);
                ((TextView) convertView).setTextColor(getResources().getColorStateList(R.color.new_text_color_first));
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
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new LinearLayout(ItemExpenseActivity.this);

                AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, dp2px(48));
                convertView.setLayoutParams(lp);
                ((LinearLayout) convertView).setOrientation(LinearLayout.HORIZONTAL);
                TextView tv = new TextView(ItemExpenseActivity.this);
                tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setTextColor(getResources().getColorStateList(R.color.new_text_color_first));
                LinearLayout.LayoutParams llpt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
                llpt.weight = 1.0f;

                LinearLayout.LayoutParams llpi = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ImageView iv = new ImageView(ItemExpenseActivity.this);
                iv.setBackgroundResource(R.drawable.ic_green_checkbox);
                iv.setVisibility(View.GONE);
                llpi.gravity = Gravity.CENTER_VERTICAL;

                ((LinearLayout) convertView).addView(tv, 0, llpt);
                ((LinearLayout) convertView).addView(iv, 1, llpi);
            }
            ((TextView) ((LinearLayout) convertView).getChildAt(0)).setText(showSubItems.get(position).getName());
            if (showSubItems.get(position).getId() == selectedSubItemId) {
                ((LinearLayout) convertView).getChildAt(1).setVisibility(View.VISIBLE);
            } else {
                ((LinearLayout) convertView).getChildAt(1).setVisibility(View.GONE);
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

}
