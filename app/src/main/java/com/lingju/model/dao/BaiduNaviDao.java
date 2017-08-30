package com.lingju.model.dao;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.common.repository.SyncDao;
import com.lingju.lbsmodule.location.Address;
import com.lingju.model.BaiduAddress;
import com.lingju.model.BaiduAddressDao;
import com.lingju.robot.AndroidChatRobotBuilder;

import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/21.
 */
public class BaiduNaviDao implements SyncDao<com.lingju.context.entity.Address> {

    private static BaiduNaviDao instance;
    private BaiduAddressDao mAddressDao;

    private BaiduNaviDao() {
        if (DaoManager.get() != null && DaoManager.get().getDaoSession() != null) {

        }
        mAddressDao = DaoManager.get().getDaoSession().getBaiduAddressDao();
        try {
            mAddressDao = DaoManager.get().getDaoSession().getBaiduAddressDao();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static BaiduNaviDao getInstance() {
        if (instance == null) {
            instance = new BaiduNaviDao();
        }
        return instance;
    }

    /**
     * 获取指定数量的常用（已收藏）地址
     *
     * @param list
     * @param number 指定数量 <=0则搜索全部
     **/
    public void getFavorList(List<BaiduAddress> list, int number) {
        if (list == null) {
            return;
        }
        list.clear();
        QueryBuilder<BaiduAddress> queryBuilder = mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Recyle.eq(0), BaiduAddressDao.Properties.FavoritedTime.isNotNull())
                .orderDesc(BaiduAddressDao.Properties.FavoritedTime);
        if (number > 0) {
            queryBuilder = queryBuilder.limit(number);
        }
        list.addAll(queryBuilder.list());
    }

    /**
     * 获取指定数量搜索历史记录
     *
     * @param list   待填充的数据集合
     * @param number 指定数量 <=0则搜索全部
     **/
    public void getHistoryList(List<BaiduAddress> list, int number) {
        if (list == null) {
            return;
        }
        list.clear();
        QueryBuilder<BaiduAddress> queryBuilder = mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Disable.eq(0),
                BaiduAddressDao.Properties.SearchKeyWord.isNotNull())
                .orderDesc(BaiduAddressDao.Properties.Created);
        if (number > 0) {
            queryBuilder = queryBuilder.limit(number);
        }
        list.addAll(queryBuilder.list());
    }

    /**
     * @param name
     * @param lat
     * @param lng
     * @return
     */
    public BaiduAddress find(String name, Double lat, Double lng) {

        return mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Name.eq(name),
                BaiduAddressDao.Properties.Latitude.eq(lat), BaiduAddressDao.Properties.Longitude.eq(lng)).limit(1).unique();

    }

    /**
     * 获取更多的历史记录，如之前已获取了N页记录，则当前获取N+1页的记录，
     * 如果之前获取数量不满N页但大于(N-navi)页，则当前只尝试获取N页记录。保证
     * 获取到数据是新的
     *
     * @param list    之前已获取的记录
     * @param perPage 每页的记录数量
     * @return <0 list没有更新，数据库没有新的记录，=0 list有更新，已获取所有符合条件的记录， >0 数据库仍有未读的记录
     */
    public int getMoreHistoryList(List<BaiduAddress> list, int perPage) {
        if (list == null || perPage <= 0) {
            throw new NullPointerException("list is null or perPage<=0");
        }
        int limit = list.size();
        int total = (int) getHistoryCount();
        if (total == limit)
            return -1;
        list.clear();
        System.out.println("limit" + limit + "perPage" + perPage + "total" + total);
        limit = perPage * (limit / perPage + 1);
        if (limit > total)
            limit = total;
        System.out.println("limit" + limit + "perPage" + perPage + "total" + total);
        List<BaiduAddress> historys = mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Disable.eq(0),
                BaiduAddressDao.Properties.SearchKeyWord.isNotNull())
                .orderDesc(BaiduAddressDao.Properties.Created)
                .limit(limit).list();
        list.addAll(historys);
        return total - limit;
    }

    /**
     * 获取历史搜索地址的记录的总数
     **/
    public long getHistoryCount() {
        return mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Disable.eq(0),
                BaiduAddressDao.Properties.SearchKeyWord.isNotNull()).count();
    }

    /**
     * 获取家或单位地址
     **/
    public BaiduAddress getHomeOrCompanyAddress(String remark) {
        List<BaiduAddress> addressList = getRemarkAddress(remark);
        if (addressList.size() > 0) {
            return addressList.get(0);
        }
        return null;
    }

    /**
     * 获取指定标记的地址记录
     **/
    public List<BaiduAddress> getRemarkAddress(String remark) {
        return mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Remark.eq(remark)).list();
    }

    /**
     * 插入一条地址记录
     **/
    public void insertAddress(BaiduAddress address) {
        if (address.getFavoritedTime() != null)
            address.setSynced(false);
        if (address.getId() != null) {
            updateAddress(address);
        } else {
            if (!TextUtils.isEmpty(address.getName())) {
                BaiduAddress ba = findBySKAndName(address.getSearchKeyWord(), address.getName());
                if (ba != null) {
                    address.setCreated(new Date());
                    address.setUpdate(ba);
                    updateAddress(address);
                    return;
                }
            }
            mAddressDao.insert(address);
        }
    }

    /**
     * 修改一条地址记录
     **/
    public void updateAddress(BaiduAddress address) {
        mAddressDao.update(address);
    }

    /**
     * 查询最新插入的地址记录
     **/
    public BaiduAddress findNewCreated() {
        return mAddressDao.queryBuilder().orderDesc(BaiduAddressDao.Properties.Id).limit(1).unique();
    }

    /**
     * 根据指定搜索关键词和地址描述搜索地址
     **/
    public BaiduAddress findBySKAndName(String searchkeyWord, String name) {
        QueryBuilder<BaiduAddress> queryBuilder = mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Name.eq(name));
        if (!TextUtils.isEmpty(searchkeyWord)) {
            queryBuilder = queryBuilder.where(BaiduAddressDao.Properties.SearchKeyWord.eq(searchkeyWord));
        }
        return queryBuilder.limit(1).unique();
    }

    /**
     * 清除搜索历史记录
     **/
    public void removeHistoryAddress() {
        List<BaiduAddress> list = mAddressDao.queryBuilder().list();
        for (BaiduAddress address : list) {
            if (address.getFavoritedTime() != null) {    //若已收藏，则只去除搜索标记，保留记录
                address.setSearchKeyWord(null);
                mAddressDao.update(address);
            } else {
                mAddressDao.delete(address);    //若未收藏则直接删除记录
            }
        }
    }

    /**
     * 将Address对象信息填充到BaiduAddress对象中
     **/
    public BaiduAddress get(Address address) {
        BaiduAddress temp = new BaiduAddress();
        temp.setName(address.getAddressDetail());
        temp.setAddress(address.getAddressDetail());
        temp.setLatitude(address.getLatitude());
        temp.setLongitude(address.getLongitude());
        temp.setCreated(new Date());
        BaiduAddress baiduAddress = get(temp);
        return baiduAddress == null ? temp : baiduAddress;
    }

    /**
     * 根据指定地址名称或经纬度查询地址
     **/
    public BaiduAddress get(BaiduAddress baiduAddress) {
        DecimalFormat df = new DecimalFormat("0.0000");
        WhereCondition andCondition = mAddressDao.queryBuilder().and(BaiduAddressDao.Properties.Latitude.like(df.format(baiduAddress.getLatitude()) + "%"),
                BaiduAddressDao.Properties.Longitude.like(df.format(baiduAddress.getLongitude()) + "%"));
        return mAddressDao.queryBuilder().whereOr(BaiduAddressDao.Properties.Address.eq(baiduAddress.getAddress())
                , andCondition).orderDesc(BaiduAddressDao.Properties.FavoritedTime).limit(1).unique();
    }

    /**
     * 查询最近一次搜索的地址
     **/
    public BaiduAddress getLastSearchedAddress() {
        return mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.SearchKeyWord.isNotNull())
                .orderDesc(BaiduAddressDao.Properties.Created).limit(1).unique();
    }

    /**
     * 移除家或单位标记
     **/
    public void removeHomeOrCompany(String remark) {
        if (TextUtils.isEmpty(remark)) {
            return;
        }
        List<BaiduAddress> addressList = getRemarkAddress(remark);
        for (BaiduAddress address : addressList) {
            if (remark.equals("家") || remark.equals("单位"))
                address.setSynced(false);
            address.setRemark(null);
            insertAddress(address);
        }
    }

    /**
     * 移除地址
     */
    public void removeAddress(String remark) {
        if (TextUtils.isEmpty(remark)) {
            return;
        }
        BaiduAddress remarkAddress = getHomeOrCompanyAddress(remark);
        mAddressDao.delete(remarkAddress);
    }

    /**
     * 同步收藏地址
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

    @Override
    public int getTargetId() {
        return RobotConstant.ACTION_ADDRESS;
    }

    @Override
    public Class<com.lingju.context.entity.Address> getTargetClass() {
        return com.lingju.context.entity.Address.class;
    }

    @Override
    public long getLastTimestamp() {
        long t = 0;
        BaiduAddress address = mAddressDao.queryBuilder().orderDesc(BaiduAddressDao.Properties.Timestamp).limit(1).unique();
        if (address != null)
            t = address.getTimestamp();
        return t;
    }

    @Override
    public boolean mergeServerData(JsonArray jsonArray) {
        return false;
    }

    @Override
    public boolean mergeServerData(List<com.lingju.context.entity.Address> list) {
        if (list != null && list.size() > 0) {
            mergeAddressData(list);
            AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
        }
        return true;
    }

    @Override
    public List<com.lingju.context.entity.Address> getUnSyncLocalData(int i) {
        List<com.lingju.context.entity.Address> syncList = new ArrayList<>();
        List<BaiduAddress> list = new ArrayList<>();
        getFavorList(list, 0);
        for (BaiduAddress ba : list) {
            if (!ba.getSynced()) {
                com.lingju.context.entity.Address address = new com.lingju.context.entity.Address();
                address.setName(ba.getName());
                address.setDetailedaddress(ba.getAddress());
                address.setLatitude(ba.getLatitude());
                address.setLongitude(ba.getLongitude());
                address.setAlias(ba.getRemark());
                address.setCity(ba.getCity());
                address.setTelephone(ba.getPhoneNum());
                address.setUid(ba.getUid());
                address.setSynced(ba.getSynced());
                address.setRecyle(ba.getRecyle());
                address.setTimestamp(ba.getTimestamp());
                address.setLid(ba.getId().intValue());
                address.setSid(ba.getSid());
                syncList.add(address);
            }
        }
        return syncList;
    }

    @Override
    public int getUnsyncLocalDataCount() {
        return (int) mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Synced.eq(false)).count();
    }

    @Override
    public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
        return null;
    }

    private void mergeAddressData(List<com.lingju.context.entity.Address> list) {
        List<BaiduAddress> addresses = new ArrayList<>();
        for (com.lingju.context.entity.Address address : list) {
            Log.i("LingJu", "BaiduNaviDao mergeAddressData()>>> " + address.getLid() + " " + address.getName() + " " + address.getTimestamp());
            BaiduAddress ba = mAddressDao.queryBuilder().where(BaiduAddressDao.Properties.Id.eq(address.getLid())).limit(1).unique();
            if (ba == null)
                ba = new BaiduAddress();
            ba.setName(address.getName());
            ba.setAddress(address.getDetailedaddress());
            ba.setUid(address.getUid());
            ba.setPhoneNum(address.getTelephone());
            ba.setRemark(address.getAlias());
            ba.setLatitude(address.getLatitude());
            ba.setLongitude(address.getLongitude());
            ba.setCity(ba.getCity());
            ba.setSynced(true);
            ba.setFavoritedTime(new Date(address.getTimestamp()));
            ba.setTimestamp(address.getTimestamp());
            ba.setRecyle(address.getRecyle());
            ba.setSid(address.getSid());
            addresses.add(ba);
        }
        mAddressDao.insertOrReplaceInTx(addresses);
    }
}
