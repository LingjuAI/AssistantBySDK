package com.lingju.model.dao;

import com.lingju.model.Item;
import com.lingju.model.ItemDao;
import com.lingju.model.SubItem;
import com.lingju.model.SubItemDao;

import java.util.List;

/**
 * Created by Ken on 2016/12/16.
 */
public class AccountItemDao {

    private static AccountItemDao instance;
    private final ItemDao mItemDao;
    private final SubItemDao mSubItemDao;

    private AccountItemDao() {
        mItemDao = DaoManager.get().getDaoSession().getItemDao();
        mSubItemDao = DaoManager.get().getDaoSession().getSubItemDao();
    }

    public static AccountItemDao getInstance() {
        if (instance == null) {
            synchronized (AccountItemDao.class) {
                if (instance == null) {
                    instance = new AccountItemDao();
                }
            }
        }
        return instance;
    }

    /**
     * 根据字段“expense”的值查找所有Item记录
     **/
    public List<Item> findAllByExpense(int value) {
        return mItemDao.queryBuilder().where(ItemDao.Properties.Expense.eq(value)).list();
    }

    /** 查询所有SubItem记录 **/
    public List<SubItem> findAllSubItem(){
        return mSubItemDao.queryBuilder().list();
    }

    /** 插入多条Item记录 **/
    public void insertItems(List<Item> items){
        mItemDao.insertInTx(items);
    }

    /** 插入一条item记录 **/
    public void inserItem(Item item){
        mItemDao.insert(item);
    }

    /** 插入一条subItem记录 **/
    public void insertSubItem(SubItem subItem){
        mSubItemDao.insert(subItem);
    }

    public Item findItemById(long id){
        return mItemDao.queryBuilder().where(ItemDao.Properties.Id.eq(id)).unique();
    }

    public List<SubItem> findAllByItemid(long id){
        return mSubItemDao.queryBuilder().where(SubItemDao.Properties.Itemid.eq(id)).list();
    }

    public void updateItem(Item item){
        mItemDao.update(item);
    }

    public void updateSubItem(SubItem subItem){
        mSubItemDao.update(subItem);
    }

    public void deleteItem(Item item){
        mItemDao.delete(item);
    }

    public void deleteSubItem(SubItem subItem){
        mSubItemDao.delete(subItem);
    }
}
