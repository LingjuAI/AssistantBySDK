package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.LevelListDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO: document your custom view class.
 */
public class RealTimeUpdateSearchBox extends LinearLayout {

    private EditText edit;
    private ImageButton stateBt;
    private OnSearchListener sListener;
    private Animation animate;
    private LevelListDrawable drawable;
    private volatile int disableUpdateNum = 0;
    private View mLlRoot;

    public RealTimeUpdateSearchBox(Context context) {
        super(context);
        init(null, 0);
    }

    public RealTimeUpdateSearchBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public RealTimeUpdateSearchBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.lingju, defStyle, 0);
        LayoutInflater.from(getContext()).inflate(R.layout.search_online_box, this);
        mLlRoot = findViewById(R.id.ll_root);
        edit = (EditText) findViewById(R.id.sob_search_edit);
        stateBt = (ImageButton) findViewById(R.id.sob_state_bt);
        animate = AnimationUtils.loadAnimation(getContext(), R.anim.start_up_loading);
        animate.setInterpolator(new LinearInterpolator());
        drawable = (LevelListDrawable) stateBt.getDrawable();
        edit.addTextChangedListener(searhWatcher);
        edit.setOnEditorActionListener(editorActionListener);
        edit.setOnClickListener(editorClickListener);
        stateBt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (stateBt.getVisibility() == View.VISIBLE && drawable.getLevel() == 1) {
                    edit.setText("");
                    stateBt.setVisibility(View.INVISIBLE);
                }
            }
        });
        edit.setHint(a.getString(R.styleable.lingju_hint));
        // edit.setHintTextColor(getResources().getColor(R.color.navi_search_box_color));
        edit.setHintTextColor(a.getColor(R.styleable.lingju_hintColor, getResources().getColor(R.color.navi_search_box_color)));
        edit.setTextColor(a.getColor(R.styleable.lingju_textColor, getResources().getColor(R.color.ksw_md_solid_disable)));
        mLlRoot.setBackgroundColor(a.getColor(R.styleable.lingju_search_background, getResources().getColor(R.color.green_style)));
        //edit.setTextSize(a.getFloat(com.android.internal.R.styleable.TextView_textSize,12));

        a.recycle();
    }

    /**
     * 弹出键盘（可能存在视图尚未加载完成，需要延迟一段时间弹出）
     **/
    public void showKeyboard() {
        Single.just(0)
                .delay(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        edit.requestFocus();
                        imm.showSoftInput(edit, 0);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();

    }

    public void setSearchListener(OnSearchListener listener) {
        this.sListener = listener;
    }

    public void setSearchCompletedState() {
        if (stateBt.getVisibility() != View.VISIBLE) {
            stateBt.setVisibility(View.VISIBLE);
        }
        stateBt.clearAnimation();
        drawable.setLevel(1);
        if (sListener != null) {
            sListener.onSearchSuggestCompleted();
        }
    }

    public void setSearchIdleState() {
        stateBt.clearAnimation();
        stateBt.setVisibility(View.INVISIBLE);
    }

    /**
     * 仅设置文本，不刷新输入框状态
     **/
    public void setTextNoUpdate(String text) {
        disableUpdateNum = 1;
        edit.setText(text);
    }

    public void setEditHint(int hint) {
        this.edit.setHint(hint);
    }

    public void setEditHint(String hint) {
        this.edit.setHint(hint);
    }

    public String getEditHint() {
        return this.edit.getHint().toString();
    }

    public void setText(String text) {
        disableUpdateNum = 0;
        edit.setText(text);
    }

    public String getText() {
        return edit.getText().toString();
    }


    TextWatcher searhWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (disableUpdateNum-- > 0) {
                return;
            }
            String keyword = s.toString();
            if (TextUtils.isEmpty(keyword)) {
                stateBt.setVisibility(View.INVISIBLE);
            } else {
                drawable.setLevel(0);
                stateBt.setVisibility(View.VISIBLE);
                stateBt.startAnimation(animate);
            }
            if (sListener != null) {
                sListener.onSearchTextUpdate(keyword);
            }
        }
    };
    /**
     * 输入栏点击事件监听
     */
    View.OnClickListener editorClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (sListener != null)
                sListener.editClick();
        }
    };

    /**
     * 手机软键盘按钮点击事件监听器
     **/
    TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            /* 点击搜索按钮 */
            if (actionId == EditorInfo.IME_ACTION_SEARCH /*||(event!=null&&event.getKeyCode()== KeyEvent.KEYCODE_ENTER)*/ && !TextUtils.isEmpty(v.getText())) {
                if (sListener != null) {
                    sListener.onSearch(v.getText().toString());
                }
                ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        }
    };


    public interface OnSearchListener {

        public void editClick();

        public void onSearchTextUpdate(String text);

        public void onSearchSuggestCompleted();

        public void onSearch(String text);

    }
}
