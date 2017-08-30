package com.lingju.assistant.view.wave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lingju.common.log.Log;

import java.util.List;

public abstract class RenderView extends SurfaceView implements SurfaceHolder.Callback {

    public RenderView(Context context) {
        this(context, null);
    }

    public RenderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        setZOrderOnTop(true);//使surfaceview放到最顶层(会盖住它上面的view)
        getHolder().setFormat(PixelFormat.TRANSPARENT);//使窗口支持透明度
        // setZOrderMediaOverlay(true);//遵从view的层级关系，不盖住上面的view
    }

    private boolean isFirst = true;

    /*回调/线程*/
    private class RenderThread extends Thread {

        private static final long SLEEP_TIME = 16;

        private SurfaceHolder surfaceHolder;
        private boolean running = true;

        public RenderThread(SurfaceHolder holder) {
            super("RenderThread");
            surfaceHolder = holder;
        }

        @Override
        public void run() {
            long startAt = System.currentTimeMillis();
            while (true) {
                synchronized (surfaceLock) {
                    if (!running) {
                        return;
                    }
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        render(canvas, System.currentTimeMillis() - startAt);  //这里做真正绘制的事情
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                if (isFirst) {
                    isFirst = false;
                    running = false;
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRun(boolean isRun) {
            this.running = isRun;
        }
    }

    private final Object surfaceLock = new Object();
    private RenderThread renderThread;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG,"surfaceCreated>>"+getVisibility());
        renderer = onCreateRenderer();
        if (renderer != null && renderer.isEmpty()) {
            throw new IllegalStateException();
        }

        renderThread = new RenderThread(holder);
        renderThread.start();
    }

    private final static String TAG=RenderView.class.getName();

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //这里可以获取SurfaceView的宽高等信息
        Log.i(TAG,"surfaceChanged>>"+getVisibility());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG,"surfaceDestroyed>>"+getVisibility());
        synchronized (surfaceLock) {  //这里需要加锁，否则doDraw中有可能会crash
            if (renderThread != null) {
                renderThread.setRun(false);
                renderThread = null;
            }
        }
    }

    public void startWave() {
        synchronized (surfaceLock) {  //这里需要加锁，否则doDraw中有可能会crash
            if (renderThread == null || !renderThread.running) {
                renderThread = new RenderThread(getHolder());
                renderThread.start();
            }
        }
    }

    /*绘图*/

    public interface IRenderer {

        void onRender(Canvas canvas, long millisPassed);
    }

    private List<IRenderer> renderer;

    protected List<IRenderer> onCreateRenderer() {
        return null;
    }

    private void render(Canvas canvas, long millisPassed) {
        if (renderer != null) {
            for (int i = 0, size = renderer.size(); i < size; i++) {
                renderer.get(i).onRender(canvas, millisPassed);
            }
        } else {
            onRender(canvas, millisPassed);
        }
    }

    /**
     * 渲染surfaceView的回调方法。
     *
     * @param canvas 画布
     */
    protected void onRender(Canvas canvas, long millisPassed) {
    }

}
