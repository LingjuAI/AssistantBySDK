package com.lingju.model.dao;

import android.content.Context;

import com.lingju.common.log.Log;
import com.lingju.model.AccountingDao;
import com.lingju.model.AlarmClockDao;
import com.lingju.model.BaiduAddressDao;
import com.lingju.model.CallLogProxyDao;
import com.lingju.model.ContactsProxyDao;
import com.lingju.model.DaoMaster;
import com.lingju.model.FileDownLogDao;
import com.lingju.model.ItemDao;
import com.lingju.model.MemoDao;
import com.lingju.model.MetaDataDao;
import com.lingju.model.NaviRecordDao;
import com.lingju.model.PlayMusicDao;
import com.lingju.model.RemindDao;
import com.lingju.model.SmsProxyDao;
import com.lingju.model.SubItemDao;
import com.lingju.model.TapeDao;
import com.lingju.model.TrackAlbumDao;
import com.lingju.model.UserDao;
import com.lingju.model.ZipcodeDao;

import org.greenrobot.greendao.database.Database;

/**
 * Created by Ken on 2017/9/1.<br />
 */
public class SQLiteOpenHelper extends DaoMaster.OpenHelper {

    public SQLiteOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onCreate(Database db) {
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
        MigrationHelper.getInstance().migrate(db
                , AccountingDao.class
                , AlarmClockDao.class
                , BaiduAddressDao.class
                , CallLogProxyDao.class
                , ContactsProxyDao.class
                , ItemDao.class
                , MemoDao.class
                , MetaDataDao.class
                , NaviRecordDao.class
                , PlayMusicDao.class
                , RemindDao.class
                , SmsProxyDao.class
                , SubItemDao.class
                , TapeDao.class
                , TrackAlbumDao.class
                , UserDao.class
                , ZipcodeDao.class
                , FileDownLogDao.class);
    }
}



