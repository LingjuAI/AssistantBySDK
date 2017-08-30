package com.lingju.assistant.activity.event;

import android.view.View;

/**
 * Created by Ken on 2016/12/2.
 */
public interface OnItemClickListener {
    void onClick(View itemView, int position);
    void onLongClick(View intemView, int position);
}
