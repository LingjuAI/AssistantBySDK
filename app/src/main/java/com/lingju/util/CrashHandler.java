package com.lingju.util;

import android.app.NotificationManager;
import android.content.Context;

import com.lingju.assistant.player.audio.LingjuAudioPlayer;

/**
 * 全局异常捕获处理类
 **/
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance; // 单例模式
    private Context context; // 程序Context对象
    private Thread.UncaughtExceptionHandler defalutHandler; // 系统默认的UncaughtException处理类


    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例
     *
     * @return CrashHandler
     */
    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 异常处理初始化
     *
     * @param context
     */
    public void init(Context context) {
        this.context = context;
        // 获取系统默认的UncaughtException处理器
        defalutHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 自定义错误处理
        if (!handleException(ex) && defalutHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            defalutHandler.uncaughtException(thread, ex);

        } else {
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            // 关闭虚拟机，彻底释放内存空间
            //  System.exit(0);
            /*//重启主界面
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);*/
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return false;
        }
        //出现异常关闭音乐播放通知栏
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(LingjuAudioPlayer.NOTIFICATION_ID);
        /*Intent intent = new Intent(context, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.CLOSE_PLAY_NOTIFICATION);
        context.startService(intent);*/
        ex.printStackTrace();
        // TODO: 2017/5/3  收集设备参数信息、日志信息
        return true;
    }
}