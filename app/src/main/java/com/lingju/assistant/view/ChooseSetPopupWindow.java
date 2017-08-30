package com.lingju.assistant.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.util.XmlyManager;

/**
 * Created by Ken on 2017/6/13.
 */
public class ChooseSetPopupWindow {

    private Context mContext;
    private int totalCount;
    private int totalPage;
    private int currentPage = 1;
    private PopupWindow mChooseWindow;
    private OnItemClickListener mListener;

    public ChooseSetPopupWindow(Context context, int pageNum, int totalPage, int totalCount) {
        this.mContext = context;
        this.currentPage = pageNum;
        this.totalPage = totalPage;
        this.totalCount = totalCount;
        initView();
    }

    private void initView() {
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.popup_choose_set, null);
        contentView.findViewById(R.id.ll_shade).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mChooseWindow = new PopupWindow(contentView, -1, -1);
        //点击空白处的时候PopupWindow会消失
        mChooseWindow.setFocusable(true);
        mChooseWindow.setTouchable(true);
        mChooseWindow.setOutsideTouchable(true);
        mChooseWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // mChooseWindow.setAnimationStyle(R.style.popupAnim);

        //初始化集数列表
        RecyclerView rvChooseSet = (RecyclerView) contentView.findViewById(R.id.rv_choose_set);
        rvChooseSet.setHasFixedSize(true);
        rvChooseSet.setLayoutManager(new GridLayoutManager(mContext, 4));
        rvChooseSet.setAdapter(new ChooseSetAdapter());
    }

    public void show(View anchor) {
        //显示在anchor的下方
        mChooseWindow.showAsDropDown(anchor);
    }

    public void dismiss() {
        mChooseWindow.dismiss();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    class ChooseSetAdapter extends RecyclerView.Adapter<ChooseSetAdapter.SetHolder> {

        @Override
        public ChooseSetAdapter.SetHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_choose_set, parent, false);
            return new SetHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ChooseSetAdapter.SetHolder holder, int position) {
            int page = position + 1;
            LevelListDrawable ld = (LevelListDrawable) holder.mTvSetCount.getBackground();
            if (page == currentPage) {
                ld.setLevel(1);
                holder.mTvSetCount.setTextColor(mContext.getResources().getColor(R.color.white));
            } else {
                ld.setLevel(0);
                holder.mTvSetCount.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
            }
            setText(holder.mTvSetCount, page);
        }

        @Override
        public int getItemCount() {
            return totalPage;
        }

        private void setText(TextView tvSetCount, int page) {
            int tmp = XmlyManager.BASE_COUNT * (page - 1);
            int start = 1 + tmp;
            int end = XmlyManager.BASE_COUNT + tmp;
            end = end > totalCount ? totalCount : end;
            tvSetCount.setText(new StringBuilder().append(start).append("~").append(end).toString());
        }

        class SetHolder extends RecyclerView.ViewHolder {

            private TextView mTvSetCount;

            public SetHolder(View itemView) {
                super(itemView);
                mTvSetCount = (TextView) itemView.findViewById(R.id.tv_set_count);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            dismiss();
                            mListener.onClick(v, getAdapterPosition());
                        }
                    }
                });
            }
        }
    }
}
