package com.lingju.assistant.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.NickEditDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.model.ContactsProxy;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.util.ScreenUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/1/9.
 */
public class NickSettingActivity extends GoBackActivity {

    private final static int ADD = 110;
    private final static int UPDATE = 112;

    @BindView(R.id.ans_listView)
    RecyclerView mAnsListView;
    @BindView(R.id.status_bar)
    View mStatusBar;
    private NickAdapter mAdapter;
    private List<ContactsProxy> nicks;
    private CallAndSmsDao mDao;
    private ContactsProxy newNick;
    private SlidingItem lastItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nick_setting);
        ButterKnife.bind(this);

        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        /* 获取所有昵称记录 */
        mDao = CallAndSmsDao.getInstance(this);
        nicks = mDao.findAllNickContacts();

        /* 初始化昵称列表 */
        mAdapter = new NickAdapter();
        mAnsListView.setHasFixedSize(true);
        mAnsListView.setItemAnimator(new DefaultItemAnimator());
        mAnsListView.setLayoutManager(new LinearLayoutManager(this));
        mAnsListView.setAdapter(mAdapter);
    }

    @OnClick(R.id.tv_back)
    public void back() {
        goBack();
    }

    @OnClick(R.id.tv_add)
    public void addNick() {
        Intent i = new Intent();
        /* 打开系统选择联系人页面 */
        i.setAction(Intent.ACTION_PICK);
        i.setData(ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(i, ADD);
    }

    /**
     * 编辑选中昵称item
     **/
    private void edit(final int position) {
        final NickEditDialog nickEditDialog = new NickEditDialog(this, newNick.getName(), newNick.getNickName());
        nickEditDialog.setHint("最多输入10个字")
                .setInputType(NickEditDialog.INPUT_ZH)
                .setOnNickEditListener(new NickEditDialog.OnNickEditListener() {
                    @Override
                    public void onContactEdit() {
                        // 进入联系人页面重新选择
                        Intent i = new Intent();
                        if (nicks != null && nicks.size() > 0) {
                            newNick = nicks.get(position);
                        }
                        i.putExtra("position", position);
                        i.setAction(Intent.ACTION_PICK);
                        i.setData(ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(i, UPDATE);
                        nickEditDialog.cancel();
                    }

                    @Override
                    public void onConfirm(String nick) {
                        if (TextUtils.isEmpty(nick)) {
                            nickEditDialog.getTextInputLayout().setErrorEnabled(true);
                            nickEditDialog.getTextInputLayout().setError("请输入备注");
                            Drawable background = nickEditDialog.getTextInputLayout().getEditText().getBackground();
                            if (background != null)
                                background.clearColorFilter();
                        } else if (isContactsExist(nick, position, "EDIT")) {
                            nickEditDialog.getTextInputLayout().setErrorEnabled(true);
                            nickEditDialog.getTextInputLayout().setError("存在同名备注或联系人");
                            Drawable background = nickEditDialog.getTextInputLayout().getEditText().getBackground();
                            if (background != null)
                                background.clearColorFilter();
                        } else if (newNick != null) {
                            nickEditDialog.getTextInputLayout().setError("");
                            nickEditDialog.getTextInputLayout().setErrorEnabled(false);
                            newNick.setNickName(nick);
                            //更新联系人昵称
                            CallAndSmsDao.getInstance(NickSettingActivity.this).updateNickName(newNick);
                            mAdapter.notifyDataSetChanged();
                            nickEditDialog.cancel();
                            //同步更新
                            CallAndSmsDao.getInstance(NickSettingActivity.this).sync(CallAndSmsDao.getInstance(NickSettingActivity.this).getSyncDao(CallAndSmsDao.ContactsDao.class));
                        }
                    }
                })
                .show();

    }

    /**
     * 判断备注名称是否已在联系人或备注中存在
     *
     * @param text
     * @return
     */
    private boolean isContactsExist(String text, int position, String type) {
        Log.i("新增联系人的位置", "" + position);
        boolean isExist = false;
        for (int i = 0; i < nicks.size(); i++) {
            ContactsProxy n = nicks.get(i);
            if (text.equals(n.getNickName())) {
                //如果是点击的item位置的备注名不需要弹出错误提示
                if (i == position && type.equals("EDIT")) {
                    isExist = false;
                } else {
                    isExist = true;
                }
            }

        }
        //判断联系人名中是否存在
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null,
                null);
        for (cursor.moveToFirst(); (!cursor.isAfterLast()); cursor.moveToNext()) { //遍历Cursor，提取数据
            if (text.equals(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)))) {
                isExist = true;
            }
        }
        cursor.close();
        return isExist;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null)
            return;
        Uri uri = data.getData();
        String name = null;
        final int position = data.getIntExtra("position", 0);
        String id = null;
        if (uri != null) {
            id = uri.toString();
            id = id.substring(id.lastIndexOf("/") + 1, id.length());
            try {
                Cursor cursor = getContentResolver().query(uri, null, null, null,
                        null);

                if (cursor.moveToFirst()) {
                    /* 获取选中联系人名称 */
                    name = cursor.getString(cursor
                            .getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                }
                cursor.close();
            } catch (Exception e) {
            }
        }
        if (name != null) {
            if (requestCode == ADD) {
                newNick = new ContactsProxy();
                newNick.setRawContactId(id);
                newNick.setName(name);
                final NickEditDialog nickEditDialog = new NickEditDialog(NickSettingActivity.this, name, "");
                nickEditDialog.setHint("最多输入10个字")
                        .setInputType(NickEditDialog.INPUT_ZH)
                        .setOnNickEditListener(new NickEditDialog.OnNickEditListener() {
                            @Override
                            public void onContactEdit() {
                                // 进入联系人页面重新选择
                                Intent i = new Intent();
                                i.putExtra("position", position);
                                i.setAction(Intent.ACTION_PICK);
                                i.setData(ContactsContract.Contacts.CONTENT_URI);
                                startActivityForResult(i, ADD);
                                nickEditDialog.cancel();
                            }

                            @Override
                            public void onConfirm(String nick) {
                                if (TextUtils.isEmpty(nick)) {
                                    nickEditDialog.getTextInputLayout().setErrorEnabled(true);
                                    nickEditDialog.getTextInputLayout().setError("请输入备注");
                                    Drawable background = nickEditDialog.getTextInputLayout().getEditText().getBackground();
                                    if (background != null)
                                        background.clearColorFilter();
                                } else if (isContactsExist(nick, 0, "ADD")) {
                                    nickEditDialog.getTextInputLayout().setErrorEnabled(true);
                                    nickEditDialog.getTextInputLayout().setError("存在同名备注或联系人");
                                    Drawable background = nickEditDialog.getTextInputLayout().getEditText().getBackground();
                                    if (background != null)
                                        background.clearColorFilter();
                                } else {
                                    nickEditDialog.getTextInputLayout().setError("");
                                    nickEditDialog.getTextInputLayout().setErrorEnabled(false);
                                    newNick.setNickName(nick);
                                    nicks.add(newNick);
                                    // AppConfig.mContactUtils.addNickContacts(newNick);
                                    mAdapter.notifyDataSetChanged();
                                    nickEditDialog.cancel();
                                    //更新联系人昵称
                                    CallAndSmsDao.getInstance(NickSettingActivity.this).updateNickName(newNick);
                                    //同步更新
                                    CallAndSmsDao.getInstance(NickSettingActivity.this).sync(CallAndSmsDao.getInstance(NickSettingActivity.this).getSyncDao(CallAndSmsDao.ContactsDao.class));
                                }
                            }
                        })
                        .show();
            } else if (requestCode == UPDATE) {
                edit(position);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class NickAdapter extends RecyclerView.Adapter<NickAdapter.NickHolder> {
        @Override
        public NickHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(NickSettingActivity.this).inflate(R.layout.nick_line, parent, false);
            return new NickHolder(item);
        }

        @Override
        public void onBindViewHolder(NickHolder holder, int position) {
            Log.i("nick", "position is " + position);
            ContactsProxy proxy = nicks.get(position);
            holder.mNickNameText.setText(proxy.getNickName());
            holder.mNickNameContactText.setText(proxy.getName());
        }

        @Override
        public int getItemCount() {
            return nicks == null ? 0 : nicks.size();
        }

        class NickHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.nickName_text)
            TextView mNickNameText;
            @BindView(R.id.nickName_contact_text)
            TextView mNickNameContactText;
            @BindView(R.id.sd_slidingItem)
            SlidingItem slidingItem;

            public NickHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                slidingItem.setOnSlidingItemListener(new SlidingItem.OnSlidingItemListener() {
                    @Override
                    public void onSliding(SlidingItem item) {
                        if (lastItem != null && lastItem != item)
                            lastItem.hide();
                    }

                    @Override
                    public void onBtnClick(View v) {
                        slidingItem.hide();
                        newNick = nicks.get(NickHolder.this.getAdapterPosition());
                        mDao.deleteNickName(newNick.getRawContactId());
                        nicks.remove(NickHolder.this.getAdapterPosition());
                        mAdapter.notifyItemRemoved(NickHolder.this.getAdapterPosition());
                        mDao.sync(mDao.getSyncDao(CallAndSmsDao.ContactsDao.class));
                    }

                    @Override
                    public void onContentClick(View v) {
                        newNick = nicks.get(NickHolder.this.getAdapterPosition());
                        edit(NickHolder.this.getAdapterPosition());
                    }

                    @Override
                    public void onExpanded(SlidingItem item) {
                        lastItem = item;
                    }
                });
            }
        }
    }
}
