package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.R;

public class CaculatorDialog extends Dialog implements View.OnClickListener {
    private Activity context;
    private StringBuffer sb = new StringBuffer();
    private double result = 0;

    private EditText resultText;
    private OnResultListener onResultListener;


    public CaculatorDialog(Activity context, double amount, OnResultListener onResultListener) {
        super(context, R.style.lingju_dialog2);
        this.context = context;
        this.result = amount;
        this.onResultListener = onResultListener;
        sb.setLength(0);
        if (amount > 0)
            // sb.append(Double.toString(amount/100)).append(".").append(amount%100);
            sb.append(amount);
    }

    public CaculatorDialog(Context context, boolean cancelable,
                           OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculator__dialog);
        resultText = (EditText) findViewById(R.id.cd_result);
        resultText.setInputType(InputType.TYPE_NULL);
        resultText.setFocusable(true);
        resultText.setText(sb);

        findViewById(R.id.cd0).setOnClickListener(this);
        findViewById(R.id.cd1).setOnClickListener(this);
        findViewById(R.id.cd2).setOnClickListener(this);
        findViewById(R.id.cd3).setOnClickListener(this);
        findViewById(R.id.cd4).setOnClickListener(this);
        findViewById(R.id.cd5).setOnClickListener(this);
        findViewById(R.id.cd6).setOnClickListener(this);
        findViewById(R.id.cd7).setOnClickListener(this);
        findViewById(R.id.cd8).setOnClickListener(this);
        findViewById(R.id.cd9).setOnClickListener(this);
        findViewById(R.id.cd_delete).setOnClickListener(this);
        findViewById(R.id.cd_dot).setOnClickListener(this);
        findViewById(R.id.cd_confirm).setOnClickListener(this);
        findViewById(R.id.cd_close).setOnClickListener(this);
        findViewById(R.id.cd_add).setOnClickListener(this);
    }

    private boolean lastNumIsDecimals() {
        int l = sb.length();
        char t;
        while (--l >= 0) {
            t = sb.charAt(l);
            if (Character.isDigit(t)) {
                continue;
            } else if (t == '.') {
                return true;
            } else if (t == '+') {
                return false;
            }
        }
        return false;
    }

    private double getResult() {
        String[] rs = sb.toString().split("\\+");
        int l = rs.length;
        float r = 0;
        try {
            while (--l >= 0) {
                r += Float.parseFloat(rs[l]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result = Math.round(r * 100);
        return result;
    }

    @Override
    public void onClick(View v) {
        int l = sb.length();
        switch (v.getId()) {
            case R.id.cd_close:
                cancel();
                break;
            case R.id.cd_confirm:
                getResult();
                resultText.setText(((int) (result / 100)) + "." + ((int) (result % 100)));
                if (onResultListener != null) {
                    onResultListener.onResult(Double.valueOf(resultText.getText().toString()));
                }
                sb.setLength(0);
                break;
            case R.id.cd_delete:
                if (l > 0) {
                    sb.delete(l - 1, l);
                    resultText.setText(sb);
                }
                break;
            case R.id.cd_dot:
                if (l > 0 && Character.isDigit(sb.charAt(l - 1)) && !lastNumIsDecimals()) {
                    sb.append('.');
                    resultText.setText(sb);
                }
                break;
            case R.id.cd_add:
                if (l > 0 && Character.isDigit(sb.charAt(l - 1))) {
                    sb.append('+');
                    resultText.setText(sb);
                }
                break;
            case R.id.cd0:
            case R.id.cd1:
            case R.id.cd2:
            case R.id.cd3:
            case R.id.cd4:
            case R.id.cd5:
            case R.id.cd6:
            case R.id.cd7:
            case R.id.cd8:
            case R.id.cd9:
                sb.append(((TextView) v).getText());
                resultText.setText(sb);
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (context.getWindow().getAttributes().screenBrightness == 0.01f) {
            LayoutParams params = context.getWindow().getAttributes();
            params.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            context.getWindow().setAttributes(params);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.LEFT | Gravity.BOTTOM);
        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = d.getWidth();
        lp.x = 0;
        lp.y = 0;
        dialogWindow.setAttributes(lp);
        super.show();
    }

    public interface OnResultListener {
        void onResult(double result);
    }

}
