package com.lingju.model.dao;

import com.lingju.model.NaviRecord;
import com.lingju.model.NaviRecordDao;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

/**
 * Created by Dyy on 2017/3/1.
 */
public class RecordDao {
    private static RecordDao instance;
    private NaviRecordDao mNaviRecordDao;

    private RecordDao() {
        if (DaoManager.get() != null && DaoManager.get().getDaoSession() != null) {
            mNaviRecordDao = DaoManager.get().getDaoSession().getNaviRecordDao();
        }
    }

    public synchronized static RecordDao getInstance() {
        if (instance == null) {
            instance = new RecordDao();
        }
        return instance;
    }

    /**
     * 获取指定数量的导航记录
     *
     * @param list   待填充的数据集合
     * @param number 指定数量 <=0则搜索全部
     **/
    public void getRecordList(List<NaviRecord> list, int number) {
        if (list == null) {
            return;
        }
        list.clear();
        QueryBuilder<NaviRecord> queryBuilder = mNaviRecordDao.queryBuilder().where(NaviRecordDao.Properties.StartName.isNotNull(), NaviRecordDao.Properties.EndName.isNotNull())
                .orderDesc(NaviRecordDao.Properties.Created);
        if (number > 0) {
            queryBuilder = queryBuilder.limit(number);
        }
        list.addAll(queryBuilder.list());
    }

    /**
     * 获取更多的导航记录，如之前已获取了N页记录，则当前获取N+1页的记录，
     * 如果之前获取数量不满N页但大于(N-1)页，则当前只尝试获取N页记录。保证
     * 获取到数据是新的
     *
     * @param list    之前已获取的记录
     * @param perPage 每页的记录数量
     * @return <0 list没有更新，数据库没有新的记录，=0 list有更新，已获取所有符合条件的记录， >0 数据库仍有未读的记录
     */
    public int getMoreRecordList(List<NaviRecord> list, int perPage, int type) {
        if (list == null || perPage <= 0) {
            throw new NullPointerException("list is null or perPage<=0");
        }
        int limit = 0;
        if (type == 0) {
            limit = list.size();
        }
        int total = (int) getRecordCount();
        if (total == limit)
            return -1;
        list.clear();
        limit = perPage * (limit / perPage + 1);
        if (limit > total) {
            limit = total;
        }
        List<NaviRecord> records = mNaviRecordDao.queryBuilder().where(NaviRecordDao.Properties.StartName.isNotNull(),
                NaviRecordDao.Properties.EndName.isNotNull())
                .orderDesc(NaviRecordDao.Properties.Created)
                .limit(limit).list();
        list.addAll(records);
        return total - limit;
    }

    /**
     * 获取导航记录的总数
     **/
    public long getRecordCount() {
        return mNaviRecordDao.queryBuilder().where(NaviRecordDao.Properties.StartName.isNotNull(), NaviRecordDao.Properties.EndName.isNotNull()).count();
    }

    /**
     * 清除搜索历史记录
     **/
    public void removeHistoryRecord() {
        List<NaviRecord> list = mNaviRecordDao.queryBuilder().list();
        for (NaviRecord record : list) {
            mNaviRecordDao.delete(record);
        }
    }

    /**
     * 插入导航记录
     */
    public void insertRecord(NaviRecord naviRecord) {
        NaviRecord record = mNaviRecordDao.queryBuilder().where(NaviRecordDao.Properties.StartName.eq(naviRecord.getStartName()), NaviRecordDao.Properties.EndName.eq(naviRecord.getEndName())).limit(1).unique();
        if (record == null)
            mNaviRecordDao.insert(naviRecord);
    }
}
