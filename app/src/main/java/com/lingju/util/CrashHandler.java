package com.lingju.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.lingju.assistant.player.audio.LingjuAudioPlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 全局异常捕获处理类
 **/
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final static String ERROR_DIR = FileUtil.DEFAULT_DIR + "/log";
    private static CrashHandler instance; // 单例模式
    private Context context; // 程序Context对象
    private Thread.UncaughtExceptionHandler defalutHandler; // 系统默认的UncaughtException处理类
    // 用来存储设备信息
    private Map<String, String> infos = new HashMap<>();

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
        deleteErrorLog(7);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 自定义错误处理
        if (handleException(ex) && defalutHandler != null) {
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
        try {
            if (ex == null)
                return false;
            //出现异常关闭音乐播放通知栏
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(LingjuAudioPlayer.NOTIFICATION_ID);
            // 收集设备参数信息
            collectDeviceInfo();
            // 保存日志文件
            saveCrashInfoFile(ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 收集设备信息
     **/
    private void collectDeviceInfo() throws Exception {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
        infos.put("versionCode", String.valueOf(packageInfo.versionCode));
        infos.put("versionName", String.valueOf(packageInfo.versionName));
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            infos.put(field.getName(), field.get(null).toString());
        }
    }

    /**
     * 保存设备信息和错误日志
     **/
    private void saveCrashInfoFile(Throwable ex) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n------  ").append(TimeUtils.time2String(new Date())).append("  ------\r\n");
        Set<Map.Entry<String, String>> entries = infos.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            sb.append(entry.getKey()).append("：").append(entry.getValue()).append("\r\n");
        }
        File errorLogFile = new File(ERROR_DIR, "crash-" + TimeUtils.getDateString(new Date()) + ".log");
        PrintWriter pw = new PrintWriter(new FileOutputStream(errorLogFile, true));
        BufferedWriter bw = new BufferedWriter(pw);
        pw.append(sb.toString());
        ex.printStackTrace(pw);
        /*Throwable cause = ex.getCause();
      while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }*/
        bw.write("\r\n");
        bw.flush();
        bw.close();
        pw.close();
    }

    /**
     * 删除文件
     *
     * @param day 删除文件的周期，单位：天
     **/
    private void deleteErrorLog(int day) {
        Single.just(day)
                .doOnSuccess(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer delDay) throws Exception {
                        File logDir = new File(ERROR_DIR);
                        if (logDir.exists()) {
                            File[] files = logDir.listFiles();
                            long currentTimeMillis = System.currentTimeMillis();
                            for (File errorLog : files) {
                                //超过day天则删除文件
                                String fileName = errorLog.getName();
                                String date = fileName.substring(fileName.indexOf("-"), fileName.indexOf("."));
                                if ((currentTimeMillis - TimeUtils.getDate(date).getTime()) >= delDay * TimeUtils.DAY)
                                    errorLog.delete();
                            }
                        } else {
                            logDir.mkdirs();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

}