package com.lingju.assistant.download.impl;

import com.lingju.model.FileDownLog;
import com.lingju.model.FileDownLogDao;
import com.lingju.model.dao.DaoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileService {
    private static FileService instance;
    private FileDownLogDao mDownLogDao;

    private FileService() {
        mDownLogDao = DaoManager.get().getDaoSession().getFileDownLogDao();
    }

    public synchronized static FileService getInstance() {
        if (instance == null)
            instance = new FileService();
        return instance;
    }

    /**
     * 获取每条线程已经下载的文件长度
     *
     * @param path 下载路径
     * @return
     */
    public Map<Integer, Integer> getData(String path) {
        Map<Integer, Integer> data = new HashMap<>();

        List<FileDownLog> list = mDownLogDao.queryBuilder().where(FileDownLogDao.Properties.Downpath.eq(path)).list();
        for (FileDownLog downlog : list) {
            data.put(downlog.getThreadid(), downlog.getDownlength());
        }
        return data;

    }

    public long getDataSize(String path) {
        Map<Integer, Integer> data = getData(path);
        if (data.size() == 0)
            return 0;
        long size = 0;
        for (Integer key : data.keySet()) {
            size += data.get(key);
        }
        return size;
    }

    /**
     * 保存每条线程已经下载的文件长度
     *
     * @param path
     * @param map
     */
    public void save(String path, Map<Integer, Integer> map) {
        List<FileDownLog> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            FileDownLog downLog = new FileDownLog();
            downLog.setDownpath(path);
            downLog.setThreadid(entry.getKey());
            downLog.setDownlength(entry.getValue());
            list.add(downLog);
        }
        mDownLogDao.insertInTx(list);
    }

    /**
     * 实时更新每条线程已经下载的文件长度
     *
     * @param path
     * @param map
     */
    public void update(String path, Map<Integer, Integer> map) {
        List<FileDownLog> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            FileDownLog downLog = new FileDownLog();
            downLog.setDownpath(path);
            downLog.setThreadid(entry.getKey());
            downLog.setDownlength(entry.getValue());
            list.add(downLog);
        }
        mDownLogDao.updateInTx(list);
    }

    /**
     * 当文件下载完成后，删除对应的下载记录
     *
     * @param path
     */
    public void delete(String path) {
        mDownLogDao.deleteInTx(mDownLogDao.queryBuilder().where(FileDownLogDao.Properties.Downpath.eq(path)).list());
    }
}
