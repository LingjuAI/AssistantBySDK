package com.lingju.assistant.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.BaseAssistActivity;
import com.lingju.assistant.activity.index.view.ChatListFragment;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.MemoEditDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.VoiceInputComponent;
import com.lingju.model.Memo;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/17.
 */
public class MemoActivity extends BaseAssistActivity {

    private List<TaskCard<Memo>> mDatas = new ArrayList<>();
    private MemoAdapter mMemoAdapter;
    private String content = "";
    private int memoCount;
    private AssistEntityDao.MemoEntityDao mMemoEntityDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        loadData();
        mMemoEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
    }

    private void initView() {
        mIvHeader.setImageResource(R.drawable.pic_memo_bar);
        android.support.design.widget.CollapsingToolbarLayout.LayoutParams params = (CollapsingToolbarLayout.LayoutParams) mIvHeader.getLayoutParams();
        params.rightMargin = 0;
        params.topMargin = 16;
        mIvHeader.setLayoutParams(params);
        mRlDatetime.setVisibility(View.GONE);
        mLlAccount.setVisibility(View.GONE);
        mTbAssist.setTitle("备忘");

        mMemoAdapter = new MemoAdapter();
        mAssistRecyclerview.setAdapter(mMemoAdapter);
    }

    private void loadData() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                List<Memo> memos = mAssistDao.findAllMemoDesc(false);
                for (Memo memo : memos) {
                    mDatas.add(new TaskCard<>(memo, TaskCard.TaskState.ACTIVE));
                }
                memoCount = memos.size();
                e.onNext(0);
            }
        })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        mCpbLoad.setVisibility(View.VISIBLE);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        mCpbLoad.setVisibility(View.GONE);
                        mMemoAdapter.notifyDataSetChanged();

                    }
                });
    }

    @Override
    protected void onAssistCreate() {
        moveToPosition(1);
        mDatas.add(0, new TaskCard<>(new Memo(), TaskCard.TaskState.ACTIVE));
        mMemoAdapter.notifyItemInserted(1);
        isEditPosition = 1;
    }

    @Override
    public boolean hadEdit() {
        if (isEditPosition != 0) {
            Memo memo = mDatas.get(isEditPosition - 1).t;
            return !content.equals(memo.getContent() == null ? "" : memo.getContent());
        }
        return false;
    }

    @Override
    public void collapseCard(int position) {
        if (position != 0) {
            content = "";
            Memo memo = mDatas.get(position - 1).t;
            if (memo.getId() == null) {      //新建未保存，直接移除视图
                mDatas.remove(position - 1);
                mMemoAdapter.notifyItemRemoved(position);
                //取消新建，添加图标重新显示
                mFabAdd.show();
            } else {     //已保存，收起视图
                View itemView = mLayoutManager.findViewByPosition(position);
                if (itemView != null) {
                    VoiceInputComponent mVicMemoInput = (VoiceInputComponent) itemView.findViewById(R.id.vic_memo_input);
                    View editMemoItem = itemView.findViewById(R.id.edit_memo_item);
                    View delItem = itemView.findViewById(R.id.del_item);
                    editMemoItem.setVisibility(View.GONE);
                    delItem.setVisibility(View.VISIBLE);
                    mVicMemoInput.stopRecord();
                }
            }
        }
    }


    class MemoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder;
            View itemView;
            if (viewType == HEADER_VIEW) {
                itemView = mInflater.inflate(R.layout.task_list_header, parent, false);
                holder = new HeaderHolder(itemView);
            } else {
                itemView = mInflater.inflate(R.layout.item_memo_view, parent, false);
                holder = new MemoHolder(itemView);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                ((HeaderHolder) holder).mTvHeader.setText("您共有" + memoCount + "条备忘");
            } else {
                MemoHolder memoHolder = (MemoHolder) holder;
                TaskCard<Memo> taskCard = mDatas.get(position - 1);
                if (position == isEditPosition) {
                    memoHolder.mDelItem.setVisibility(View.GONE);
                    memoHolder.mEditMemoItem.setVisibility(View.VISIBLE);
                    memoHolder.mTvCancel.setVisibility(View.VISIBLE);
                    memoHolder.mTvSave.setVisibility(View.VISIBLE);
                    memoHolder.mEditMemoTime.setText(taskCard.t.getModified() == null ? new SimpleDate().toString() : TimeUtils.getTime(taskCard.t.getModified()));
                    memoHolder.mEditMemoCount.setText("字数" + content.length());
                    memoHolder.mEditMemoContent.setText(content);
                    memoHolder.mTvSave.setText(taskCard.t.getId() == null ? "创建" : "保存");
                } else {
                    memoHolder.mDelItem.setVisibility(View.VISIBLE);
                    memoHolder.mTvCancel.setVisibility(View.GONE);
                    memoHolder.mTvSave.setVisibility(View.GONE);
                    memoHolder.mEditMemoItem.setVisibility(View.GONE);
                    memoHolder.mMemoContent.setText(taskCard.t.getContent());
                    refreshCard(taskCard.taskState, memoHolder.mTvState, null, memoHolder.mDelItem, null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return 1 + mDatas.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEADER_VIEW : CONTENT_VIEW;
        }

        /**
         * 头部视图
         **/
        class HeaderHolder extends RecyclerView.ViewHolder {
            TextView mTvHeader;

            public HeaderHolder(View itemView) {
                super(itemView);
                mTvHeader = (TextView) itemView.findViewById(R.id.tv_header);
            }
        }

        class MemoHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, VoiceInputComponent.OnResultListener, MemoEditDialog.OnMemoEditListener {

            @BindView(R.id.memo_content)
            TextView mMemoContent;
            @BindView(R.id.del_item)
            SlidingItem mDelItem;
            @BindView(R.id.edit_memo_time)
            TextView mEditMemoTime;
            @BindView(R.id.edit_memo_count)
            TextView mEditMemoCount;
            @BindView(R.id.edit_memo_content)
            EditText mEditMemoContent;
            @BindView(R.id.tv_save)
            TextView mTvSave;
            @BindView(R.id.tv_cancel)
            TextView mTvCancel;
            @BindView(R.id.vic_memo_input)
            VoiceInputComponent mVicMemoInput;
            @BindView(R.id.edit_memo_item)
            LinearLayout mEditMemoItem;
            @BindView(R.id.tv_state)
            TextView mTvState;

            public MemoHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mDelItem.setOnSlidingItemListener(this);
                mVicMemoInput.setOnResultListener(this);
                mEditMemoContent.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        // TODO Auto-generated method stub
                        if (view.getId() == R.id.edit_memo_content) {
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                                case MotionEvent.ACTION_UP:
                                    view.getParent().requestDisallowInterceptTouchEvent(false);
                                    break;
                            }
                        }
                        return false;
                    }
                });
                mEditMemoContent.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        mEditMemoCount.setText("字数" + s.length());
                        mEditMemoContent.setSelection(mEditMemoContent.getSelectionStart());
                        content = mEditMemoContent.getText().toString();
                        if (TextUtils.isEmpty(content.trim())) {
                            mTvSave.setTextColor(getResources().getColor(R.color.forbid_click_color));
                        } else {
                            mTvSave.setTextColor(getResources().getColor(R.color.base_blue));
                        }
                    }
                });
            }

            @OnClick({R.id.edit_memo_full_screen, R.id.tv_save, R.id.tv_cancel, R.id.tv_state})
            public void onClick(View view) {
                mVicMemoInput.stopRecord();
                TaskCard<Memo> taskCard = mDatas.get(getAdapterPosition() - 1);
                switch (view.getId()) {
                    case R.id.edit_memo_full_screen:
                        MemoEditDialog memoEditDialog = new MemoEditDialog(MemoActivity.this,
                                mEditMemoContent.getText().toString(),
                                mEditMemoTime.getText().toString(),
                                taskCard.t.getId() == null,
                                this);
                        memoEditDialog.show();
                        break;
                    case R.id.tv_save:
                        saveHandle(taskCard.t);
                        break;
                    case R.id.tv_cancel:
                        cancelHandle(taskCard.t);
                        break;
                    case R.id.tv_state:
                        if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                            memoCount++;
                            notifyItemChanged(0);
                            taskCard.taskState = TaskCard.TaskState.ACTIVE;
                            taskCard.t.setSynced(false);
                            mAssistDao.insertMemo(taskCard.t);
                            notifyItemChanged(getAdapterPosition());
                            //与服务器同步
                            AssistEntityDao.create().sync(mMemoEntityDao);
                        }
                        break;
                }
            }

            private void saveHandle(Memo memo) {
                mVicMemoInput.stopRecord();
                String content = mEditMemoContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    new CommonDialog(MemoActivity.this, "编辑备忘", "您还没输入备忘内容哦", "知道了").show();
                    return;
                }
                isEditPosition = 0;
                MemoActivity.this.content = "";
                //创建完成，添加图标重新显示
                mFabAdd.show();
                mTvSave.setVisibility(View.GONE);
                mTvCancel.setVisibility(View.GONE);
                mEditMemoItem.setVisibility(View.GONE);
                mDelItem.setVisibility(View.VISIBLE);
                mMemoContent.setText(content);
                //保存记录
                memo.setContent(content);
                memo.setSynced(false);
                if (memo.getId() == null) {
                    memoCount++;
                    notifyItemChanged(0);
                    memo.setCreated(new Date());
                    mAssistDao.insertMemo(memo);
                } else {
                    memo.setModified(new Date());
                    mAssistDao.updateMemo(memo);
                }
                //与服务器同步
                AssistEntityDao.create().sync(mMemoEntityDao);
            }

            private void cancelHandle(Memo memo) {
                mVicMemoInput.stopRecord();
                isEditPosition = 0;
                content = "";
                //取消新建，添加图标重新显示
                mFabAdd.show();
                if (memo.getId() == null) {
                    mDatas.remove(getAdapterPosition() - 1);
                    notifyItemRemoved(getAdapterPosition());
                } else {
                    mTvCancel.setVisibility(View.GONE);
                    mTvSave.setVisibility(View.GONE);
                    mEditMemoItem.setVisibility(View.GONE);
                    mDelItem.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSliding(SlidingItem item) {
                if (lastItem != null && lastItem != item)
                    lastItem.hide();
            }

            @Override
            public void onBtnClick(View v) {
                //移除可撤销状态记录
                for (int i = 0; i < mDatas.size(); i++) {
                    if (mDatas.get(i).taskState == TaskCard.TaskState.REVOCABLE) {
                        if (isEditPosition > i + 1)
                            isEditPosition--;
                        mDatas.remove(i);
                        notifyItemRemoved(i + 1);
                        break;      //结束循环
                    }
                }
                memoCount--;
                notifyItemChanged(0);
                TaskCard<Memo> taskCard = mDatas.get(getAdapterPosition() - 1);
                taskCard.taskState = TaskCard.TaskState.REVOCABLE;
                taskCard.t.setSynced(false);
                mAssistDao.deleteMemo(taskCard.t);
                notifyItemChanged(getAdapterPosition());
                //与服务器同步
                AssistEntityDao.create().sync(mMemoEntityDao);
            }

            @Override
            public void onContentClick(View v) {
                if (isEditPosition != 0 && hadEdit()) {
                    showEditTips();
                    return;
                }
                collapseCard(isEditPosition);
                isEditPosition = getAdapterPosition();
                mDelItem.setVisibility(View.GONE);
                mEditMemoItem.setVisibility(View.VISIBLE);
                mTvCancel.setVisibility(View.VISIBLE);
                mTvSave.setVisibility(View.VISIBLE);
                Memo memo = mDatas.get(getAdapterPosition() - 1).t;
                mEditMemoContent.setText(memo.getContent());
                mEditMemoContent.setSelection(mEditMemoContent.length());
                mEditMemoTime.setText(memo.getModified() == null ? new SimpleDate().toString() : TimeUtils.getTime(memo.getModified()));
                mEditMemoCount.setText("字数" + memo.getContent().length());
                mTvSave.setText("保存");
                content = memo.getContent();
            }

            @Override
            public void onExpanded(SlidingItem item) {
                lastItem = item;
            }

            @Override
            public void onResult(String text) {
                Memo memo = mDatas.get(getAdapterPosition() - 1).t;
                if (text.length() < 6 && (text.startsWith(ChatListFragment.SAVE_KEYWORDS[0])
                        || text.startsWith(ChatListFragment.SAVE_KEYWORDS[1])
                        || text.startsWith(ChatListFragment.SAVE_KEYWORDS[2]))) {     //保存
                    saveHandle(memo);
                } else if (text.length() < 6 && (text.startsWith(ChatListFragment.QUIT_KEYWORDS[0])
                        || text.startsWith(ChatListFragment.QUIT_KEYWORDS[1]))) {     //取消
                    cancelHandle(memo);
                } else
                    mEditMemoContent.getText().insert(mEditMemoContent.getSelectionStart(), text);
            }

            @Override
            public void onError(int errorCode, String description) {
                new CommonDialog(MemoActivity.this, "错误提示", description, "确定").show();
            }

            @Override
            public void onCancel() {
                Memo memo = mDatas.get(getAdapterPosition() - 1).t;
                cancelHandle(memo);
            }

            @Override
            public void onBack(String content) {
                mEditMemoContent.setText(content);
                mEditMemoContent.setSelection(content.length());
            }

            @Override
            public void onSave(String content) {
                mEditMemoContent.setText(content);
                Memo memo = mDatas.get(getAdapterPosition() - 1).t;
                saveHandle(memo);
            }
        }
    }
}
