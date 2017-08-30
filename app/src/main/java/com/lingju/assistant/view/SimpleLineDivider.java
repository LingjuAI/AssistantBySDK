package com.lingju.assistant.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Administrator on 2016/11/23.
 */
public class SimpleLineDivider  extends RecyclerView.ItemDecoration {

    final Paint p;

    public SimpleLineDivider(int colorRid){
        p=new Paint();
        p.setColor(colorRid/*getResources().getColor(R.color.new_line_border)*/);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        View child;
        for (int i = 0, size = parent.getChildCount(); i < size; i++) {
            child = parent.getChildAt(i);
            c.drawLine(child.getLeft(), child.getBottom(), child.getRight(), child.getBottom(), p);
        }
    }

    public void setDivideHeight(int height){
        p.setStrokeWidth(height);
    }
}
