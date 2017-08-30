package com.lingju.model.dao;

import com.lingju.model.User;
import com.lingju.model.UserDao;

/**
 * Created by Ken on 2017/1/5.
 */
public class UserManagerDao {

    private static UserManagerDao instance;
    private final UserDao mUserDao;

    private UserManagerDao() {
        mUserDao = DaoManager.get().getDaoSession().getUserDao();
    }

    public static UserManagerDao getInstance() {
        if (instance == null) {
            synchronized (UserManagerDao.class) {
                if (instance == null) {
                    instance = new UserManagerDao();
                }
            }
        }
        return instance;
    }

    /** 插入一条用户记录 **/
    public void insertUser(User user){
        mUserDao.deleteAll();
        mUserDao.insert(user);
    }

    /**
     * 查询一条最新用户记录
     **/
    public User findNewCreated() {
        User user = mUserDao.queryBuilder().orderDesc(UserDao.Properties.Created).limit(1).unique();
        return user == null ? new User() : user;
    }
}
