package com.lingju.model.dao;

import android.content.Context;

import com.lingju.model.DaoMaster;
import com.lingju.model.DaoSession;

import org.greenrobot.greendao.database.Database;

/**
 * Created by Administrator on 2016/11/1.
 */
public class DaoManager {

    public static final boolean ENCRYPTED = false;

    private DaoSession daoSession;
    private static DaoManager instance;
    private Database mDb;

    private DaoManager(Context application) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(application, ENCRYPTED ? "notes-db-encrypted" : "notes-db");
        mDb = ENCRYPTED ? helper.getEncryptedWritableDb("super-secret") : helper.getWritableDb();
        daoSession = new DaoMaster(mDb).newSession();
    }

    public static synchronized DaoManager create(Context application){
        if(instance==null)instance=new DaoManager(application.getApplicationContext());
        return instance;
    }

    public static DaoManager get(){
        return instance;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public Database getDb(){
        return mDb;
    }

}
