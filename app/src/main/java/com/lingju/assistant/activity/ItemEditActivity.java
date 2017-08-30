package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.AccountItemEvent;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.CommonEditDialog;
import com.lingju.model.Item;
import com.lingju.model.SubItem;
import com.lingju.model.dao.AccountItemDao;
import com.lingju.util.ScreenUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by Ken on 2016/12/16.
 */
public class ItemEditActivity extends GoBackActivity {

    public final static int FOR_ITEM_EDIT = 9;
    public final static int EXPENSE = 0;
    public final static int SUB_EXPENSE = 1;
    public final static int INCOME = 2;
    public final static String TYPE = "type";
    public final static String UPDATE = "update";

    private Intent intent;
    private int type = 0;
    private boolean update = false;
    private ListView listView;

    private Item parent;
    private AccountItemDao dao;
    private List<Item> items;
    private List<SubItem> subItems;
    private LayoutInflater inflater;
    private boolean isSubAdd = false;       //子项修改标记

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_expense_edit);

        View statusBar = findViewById(R.id.status_bar);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = statusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        statusBar.setLayoutParams(layoutParams);

        listView = (ListView) findViewById(R.id.aiee_list);
        dao = AccountItemDao.getInstance();
        intent = getIntent();
        if (intent != null) {
            type = intent.getIntExtra(TYPE, 0);
            if (type == SUB_EXPENSE) {
                long itemId = intent.getLongExtra("id", 0);
                if (itemId > 0)
                    parent = dao.findItemById(itemId);
            }
        }

        String title = "";
        switch (type) {
            case EXPENSE:
                title = "支出项目类别-编辑";
                items = dao.findAllByExpense(1);
                break;
            case SUB_EXPENSE:
                title = parent.getItem() + "子类别-编辑";
                subItems = dao.findAllByItemid(parent.getId());
                break;
            case INCOME:
                title = "收入项目类别-编辑";
                items = dao.findAllByExpense(0);
                break;
        }
        ((TextView) findViewById(R.id.aiee_title)).setText(title);
        inflater = LayoutInflater.from(this);
        listView.setAdapter(adapter);
        findViewById(R.id.aiee_back).setOnClickListener(clickListener);
        findViewById(R.id.aiee_finish).setOnClickListener(clickListener);
        findViewById(R.id.aiee_add).setOnClickListener(clickListener);
        if (type == 0) {
            listView.setOnItemClickListener(itemListener);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (resultCode == FOR_ITEM_EDIT) {
                if (!update)
                    update = data.getBooleanExtra(UPDATE, false);
                isSubAdd = false;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.aiee_back:
                    if (update) {
                        if (isSubAdd) {
                            intent.putExtra(UPDATE, true);
                            setResult(FOR_ITEM_EDIT, intent);
                        } else
                            EventBus.getDefault().post(new AccountItemEvent());
                    }
                    goBack();
                    break;
                case R.id.aiee_finish:
                    if (update) {
                        if (isSubAdd) {
                            intent.putExtra(UPDATE, true);
                            setResult(FOR_ITEM_EDIT, intent);
                        } else
                            EventBus.getDefault().post(new AccountItemEvent());
                    }
                    goBack();
                    break;
                case R.id.aiee_add:
                    final CommonEditDialog ced = new CommonEditDialog(ItemEditActivity.this, "添加类别名称", "", "取消", "确定");
                    ced.setHint("最多5个汉字");
                    ced.setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {

                        @Override
                        public void onConfirm(String text) {
                            if (!TextUtils.isEmpty(text)) {
                                ced.cancel();
                                if (type == 1) {
                                    SubItem si = new SubItem(parent, text);
                                    update = true;
                                    dao.insertSubItem(si);
                                    subItems = dao.findAllByItemid(parent.getId());
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Item it = new Item(text);
                                    if (type == EXPENSE)
                                        it.setExpense(1);
                                    update = true;
                                    dao.inserItem(it);
                                    items = dao.findAllByExpense(type == INCOME ? 0 : 1);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }).show();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            Intent intent = new Intent(ItemEditActivity.this, ItemEditActivity.class);
            intent.putExtra(TYPE, SUB_EXPENSE);
            intent.putExtra("id", items.get(position).getId());
            startActivityForResult(intent, FOR_ITEM_EDIT);
            goInto();
            isSubAdd = true;
        }
    };

    private BaseAdapter adapter = new BaseAdapter() {

        View.OnClickListener listener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v.getTag() instanceof Integer) {
                    final int p = (Integer) v.getTag();
                    switch (v.getId()) {
                        case R.id.iel_edit:
                            //CommonEditDialog ced=new CommonEditDialog(ItemEditActivity.this, "添加类别名称", "最多四个汉字", "取消", "确定");
                            if (type == 1) {
                                final CommonEditDialog commonEditDialog = new CommonEditDialog(ItemEditActivity.this, "修改类别名称", subItems.get(p).getName(), "取消", "确定");
                                commonEditDialog.setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {

                                    @Override
                                    public void onConfirm(String text) {
                                        if (!TextUtils.isEmpty(text)) {
                                            subItems.get(p).setName(text);
                                            update = true;
                                            dao.updateSubItem(subItems.get(p));
                                            adapter.notifyDataSetChanged();
                                            commonEditDialog.cancel();
                                        }
                                    }
                                }).show();
                            } else {
                                final CommonEditDialog commonEditDialog = new CommonEditDialog(ItemEditActivity.this, "修改类别名称", items.get(p).getItem(), "取消", "确定");
                                commonEditDialog.setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {

                                    @Override
                                    public void onConfirm(String text) {
                                        if (!TextUtils.isEmpty(text)) {
                                            items.get(p).setItem(text);
                                            update = true;
                                            dao.updateItem(items.get(p));
                                            adapter.notifyDataSetChanged();
                                            commonEditDialog.cancel();
                                        }
                                    }
                                }).show();
                            }
                            break;
                        case R.id.iel_del:
                            if (type == 1) {
                                final CommonDialog commonDialog = new CommonDialog(ItemEditActivity.this, "请选择是否删除", "删除“" + subItems.get(p).getName() + "”这条记录吗", "取消", "确定");
                                commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {

                                    @Override
                                    public void onConfirm() {
                                        dao.deleteSubItem(subItems.get(p));
                                        update = true;
                                        subItems.remove(p);
                                        adapter.notifyDataSetChanged();
                                        commonDialog.cancel();
                                    }
                                }).show();
                            } else {
                                final CommonDialog commonDialog = new CommonDialog(ItemEditActivity.this, "请选择是否删除", "删除“" + items.get(p).getItem() + "”这条记录吗", "取消", "确定");
                                commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {

                                    @Override
                                    public void onConfirm() {
                                        dao.deleteItem(items.get(p));
                                        update = true;
                                        items.remove(p);
                                        adapter.notifyDataSetChanged();
                                        commonDialog.cancel();
                                    }
                                }).show();
                            }
                            break;
                    }
                }

            }
        };


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // LinearLayout main;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_edit_line, null);
                convertView.findViewById(R.id.iel_edit).setOnClickListener(listener);
                convertView.findViewById(R.id.iel_del).setOnClickListener(listener);
            }
            // main = (LinearLayout) convertView;
            if (type == 1) {
                ((TextView) convertView.findViewById(R.id.edit_line_text)).setText(subItems.get(position).getName());
                if (subItems.get(position).getId() > 121) {
                    convertView.findViewById(R.id.item_edit_line_box).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.iel_edit).setTag(position);
                    convertView.findViewById(R.id.iel_del).setTag(position);
                } else {
                    convertView.findViewById(R.id.item_edit_line_box).setVisibility(View.GONE);
                }
            } else {
                ((TextView) convertView.findViewById(R.id.edit_line_text)).setText(items.get(position).getItem());
                if (items.get(position).getId() > 16) {
                    convertView.findViewById(R.id.item_edit_line_box).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.iel_edit).setTag(position);
                    convertView.findViewById(R.id.iel_del).setTag(position);
                } else {
                    convertView.findViewById(R.id.item_edit_line_box).setVisibility(View.GONE);
                }
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return type == 1 ? false : type == 2 ? false : true;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getCount() {
            return type == 1 ? subItems.size() : items.size();
        }
    };

}
