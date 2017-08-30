package com.lingju.util;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

public class FileUtil {

    private static final String TAG = "FileUtil";
    private static final String SD_LOG_FILE = "speech.log";


    public final static String SDCARD_PATH = "/mnt/sdcard";
    public final static String STORAGE_PATH = "/LingjuAssistant";
    public static String DEFAULT_DIR;

    static {
        DEFAULT_DIR = getExtSdcardPath() + STORAGE_PATH;
    }

    /**
     * @param stat 文件StatFs对象
     * @return 剩余存储空间的MB数
     */
    private static int calculateSizeInMB(StatFs stat) {
        if (stat != null) {
            return (int) ((((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize())) / (long) 0x100000);
        }
        return 0;
    }

    public static String getExtSdcardPath() {
        File dir = new File("/mnt");
        String name = null;
        String status = Environment.getExternalStorageState();
        // 是否只读
        if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            status = Environment.MEDIA_MOUNTED;
        }
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File path = Environment.getExternalStorageDirectory();
                //System.out.println("Environment.getExternalStorageDirectory()="+Environment.getExternalStorageDirectory());
                StatFs stat = new StatFs(path.getPath());
                int sdc = calculateSizeInMB(stat);
                //System.out.println("path.getAbsolutePath() capacity:"+sdc);
                if (sdc > 20 || sdc == 0) {
                    return path.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
                status = Environment.MEDIA_REMOVED;
            }
        }
        /*File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
		Log.i(TAG, "path.getAbsolutePath() capacity:"+calculateSizeInMB(stat));
		if(calculateSizeInMB(stat)>50){
			return "/mnt";
		}*/

        if (dir.exists() && dir.isDirectory()) {
            File[] fs = dir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.toLowerCase().contains("sdcard"))
                        return true;
                    return false;
                }
            });
            int sdc = 0;
            int esdc = 0;
            String sdcard = null;
            for (File f : fs) {
                if (f.getName().equals("sdcard")) {
                    sdcard = SDCARD_PATH;
                    sdc = calculateSizeInMB(new StatFs(SDCARD_PATH));
                    //System.out.println("getExtSdcardPath:/mnt/sdcard,size="+sdc);
                    if (sdc > 0 && sdc < 20)
                        sdcard = null;
                } else {
                    name = "/mnt/" + f.getName();
                    int c = calculateSizeInMB(new StatFs(name));
                    //System.out.println("getExtSdcardPath:"+name+",size="+esdc);
                    if (c > 20 && c >= esdc) {
                        esdc = c;
                    } else {
                        name = null;
                    }
                }
            }
            //Log.i(TAG, "sdc:"+sdc+",esdc"+esdc);
            if (sdcard != null)
                return sdcard;
            if (name != null && (esdc > 20 || esdc == 0)) {
                return name;
            }
        }
        return null;
    }


    /**
     * @param path 文件路径
     * @return 文件路径的StatFs对象
     * @throws Exception 路径为空或非法异常抛出
     */
    private static StatFs getStatFs(String path) {
        try {
            return new StatFs(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 写SD卡文�?
     *
     * @param text
     * @param fileName
     */
    public static void writeSDFile(String text, String fileName) {
        try {
            String sdPath = getSDPath();
            if (sdPath == null) {
                Log.e(TAG, "SD path not found!");
                return;
            }
            FileOutputStream fout = new FileOutputStream(sdPath + '/' + fileName);
            byte[] bytes = text.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean deleteFile(String path) {
        File delFile = new File(path);
        return delFile.exists() && delFile.delete();
    }

    /**
     * 写Log日志
     *
     * @param text
     */
    public static void logFile(String text) {
        writeSDFile(text, SD_LOG_FILE);
    }

    private static String getSDPath() {

        String sdPath = null;
        //判断sd卡是否存�?
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            //获取根目�?
            File sdDir = Environment.getExternalStorageDirectory();
            if (sdDir != null)
                sdPath = sdDir.toString();
        }
        return sdPath;
    }
}
