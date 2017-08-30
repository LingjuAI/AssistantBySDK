package com.lingju.model.dao;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.common.repository.SyncDao;
import com.lingju.context.entity.TapeEntity;
import com.lingju.model.Tape;
import com.lingju.model.TapeDao;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/2.
 */
public class TapeEntityDao implements SyncDao<TapeEntity> {

    private static TapeEntityDao instance;
    private TapeDao mTapeDao;
    private boolean isSynced;

    private TapeEntityDao() {
        mTapeDao = DaoManager.get().getDaoSession().getTapeDao();
    }

    public static TapeEntityDao getInstance() {
        if (instance == null) {
            synchronized (TapeEntityDao.class) {
                if (instance == null)
                    instance = new TapeEntityDao();
            }
        }
        return instance;
    }

    public void insertTape(Tape tape) {
        tape.setRecyle(0);
        mTapeDao.save(tape);
    }

    public void deleteTape(Tape tape) {
        tape.setRecyle(1);
        mTapeDao.update(tape);
    }

    public Tape findTapeById(long id) {
        return mTapeDao.queryBuilder().where(TapeDao.Properties.Id.eq(id)).unique();
    }

    public Tape findTapeSid(String sid) {
        return mTapeDao.queryBuilder().where(TapeDao.Properties.Sid.eq(sid)).unique();
    }

    public List<Tape> findAllTape() {
        return mTapeDao.queryBuilder().list();
    }

    /**
     * 清空数据库中的无效记录
     **/
    public void clearRecyleData() {
        List<Tape> list = mTapeDao.queryBuilder().where(TapeDao.Properties.Recyle.eq(1)).list();
        for (Tape tape : list) {
            if (!TextUtils.isEmpty(tape.getUrl())) {
                boolean delete = FileUtil.deleteFile(tape.getUrl());
                Log.i("LingJu", tape.getText() + " " + tape.getUrl() + " 删除: " + delete);
            }
        }
        mTapeDao.deleteInTx(list);
    }

    /**
     * 合并服务器的录音记录
     **/
    private void mergeTapeData(List<TapeEntity> list) {
        for (TapeEntity entity : list) {
            Log.i("LingJu", "TapeEntityDao mergeTapeData()>>" + entity.getLid() + " " + entity.getTimestamp());
            Tape tape = findTapeById(entity.getLid());
            if (tape == null)
                tape = findTapeSid(entity.getSid());
            if (tape == null)
                tape = new Tape();
            tape.setText(entity.getText());
            tape.setUrl(entity.getUrl());
            tape.setModified(entity.getModified());
            tape.setCreated(entity.getCreated());
            tape.setSid(entity.getSid());
            tape.setRecyle(entity.getRecyle());
            tape.setTimestamp(entity.getTimestamp());
            tape.setSynced(true);
            mTapeDao.save(tape);
        }
    }

    /**
     * 同步数据
     **/
    public void sync() {
        Single.just(0)
                .observeOn(Schedulers.io())
                .doOnSuccess(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        AndroidChatRobotBuilder.get().robot().actionTargetAccessor().sync(instance);
                    }
                })
                .subscribe();
    }

    /**
     * 填充mTapes集合
     **/
    private void convertEntity(List<TapeEntity> unSyncList) {
        List<Tape> tapeList = findAllTape();
        for (Tape tape : tapeList) {
            if (!tape.getSynced()) {
                isSynced = false;
                TapeEntity entity = new TapeEntity();
                entity.setText(tape.getText());
                entity.setUrl(tape.getUrl());
                entity.setCreated(tape.getCreated());
                entity.setModified(tape.getModified());
                entity.setLid(tape.getId().intValue());
                entity.setSid(tape.getSid());
                entity.setRecyle(tape.getRecyle());
                entity.setSynced(tape.getSynced());
                entity.setTimestamp(tape.getTimestamp());
                unSyncList.add(entity);
            }
        }
    }

    @Override
    public int getTargetId() {
        return RobotConstant.ACTION_TAPE;
    }

    @Override
    public Class<TapeEntity> getTargetClass() {
        return TapeEntity.class;
    }

    @Override
    public long getLastTimestamp() {
        long t = 0;
        Tape tape = mTapeDao.queryBuilder().orderDesc(TapeDao.Properties.Timestamp).limit(1).unique();
        if (tape != null)
            t = tape.getTimestamp();
        return t;
    }

    @Override
    public boolean mergeServerData(JsonArray jsonArray) {
        return false;
    }

    @Override
    public boolean mergeServerData(List<TapeEntity> list) {
        if (list != null && list.size() > 0) {
            mergeTapeData(list);
            AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
        }
        return true;
    }

    @Override
    public List<TapeEntity> getUnSyncLocalData(int i) {
        List<TapeEntity> list = new ArrayList<>();
        convertEntity(list);
        return list;
    }

    @Override
    public int getUnsyncLocalDataCount() {
        if (!isSynced && mListener != null) {
            isSynced = true;
            mListener.onSyncComplete();
            mListener = null;
        }
        return (int) mTapeDao.queryBuilder().where(TapeDao.Properties.Synced.eq(false)).count();
    }

    @Override
    public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
        return null;
    }


    private OnSyncListener mListener;

    public void setOnSyncListener(OnSyncListener listener) {
        this.mListener = listener;
    }

    public interface OnSyncListener {
        void onSyncComplete();
    }
}
