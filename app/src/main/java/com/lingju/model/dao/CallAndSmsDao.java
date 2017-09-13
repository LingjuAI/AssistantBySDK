package com.lingju.model.dao;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.common.repository.SyncDao;
import com.lingju.context.entity.CallLog;
import com.lingju.context.entity.ContactNum;
import com.lingju.context.entity.Contacts;
import com.lingju.context.entity.Message;
import com.lingju.model.CallLogProxy;
import com.lingju.model.CallLogProxyDao;
import com.lingju.model.ContactsProxy;
import com.lingju.model.ContactsProxyDao;
import com.lingju.model.SmsProxy;
import com.lingju.model.SmsProxyDao;
import com.lingju.model.Zipcode;
import com.lingju.model.ZipcodeDao;
import com.lingju.robot.AndroidChatRobotBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/1/3.
 */
public class CallAndSmsDao {

    private static CallAndSmsDao instance;
    private Context mContext;
    private ZipcodeDao mZipcodeDao;
    private ContactsProxyDao mContactDao;
    private CallLogProxyDao mCallLogDao;
    private SmsProxyDao mSmsDao;

    private CallAndSmsDao(Context context) {
        this.mContext = context;
        mZipcodeDao = DaoManager.get().getDaoSession().getZipcodeDao();
        mContactDao = DaoManager.get().getDaoSession().getContactsProxyDao();
        mCallLogDao = DaoManager.get().getDaoSession().getCallLogProxyDao();
        mSmsDao = DaoManager.get().getDaoSession().getSmsProxyDao();
    }

    public static CallAndSmsDao getInstance(Context context) {
        if (instance == null) {
            synchronized (CallAndSmsDao.class) {
                if (instance == null) {
                    instance = new CallAndSmsDao(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 批量插入区号记录
     **/
    public void insertZipCodes(List<Zipcode> zipcodes) {
        mZipcodeDao.insertInTx(zipcodes);
    }

    /**
     * 根据城市查询对应区号值
     **/
    public String getZipCode(String city) {
        Zipcode zipcode = mZipcodeDao.queryBuilder().where(ZipcodeDao.Properties.City.like("%" + city + "%")).limit(1).unique();
        return zipcode == null ? "" : zipcode.getCode();
    }

    /**
     * 每次开启应用时新增手机联系人代理信息
     **/
    public void insertRawContacts() {
        Cursor cursor = mContext.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, new String[]{"name_raw_contact_id"}, null, null, null);
        if (cursor == null || cursor.getCount() <= 0)
            return;
        while (cursor.moveToNext()) {
            String raw_contact_id = cursor.getString(cursor.getColumnIndex("name_raw_contact_id"));
            ContactsProxy contact = findContactsById(raw_contact_id);
            if (contact == null) {
                contact = new ContactsProxy();
                contact.setRawContactId(raw_contact_id);
                mContactDao.insert(contact);
            }
        }
        cursor.close();
    }

    /**
     * 修改指定联系人昵称
     **/
    public void updateNickName(ContactsProxy proxy) {
        ContactsProxy contact = findContactsById(proxy.getRawContactId());
        if (contact == null) {
            contact = new ContactsProxy();
            contact.setRawContactId(proxy.getRawContactId());
        }
        contact.setName(proxy.getName());
        contact.setNickName(proxy.getNickName());
        contact.setSynced(false);
        mContactDao.insertOrReplace(contact);
    }

    /**
     * 删除联系人昵称
     **/
    public void deleteNickName(String id) {
        ContactsProxy contact = findContactsById(id);
        contact.setNickName(null);
        contact.setSynced(false);
        mContactDao.update(contact);
    }

    /**
     * 新增通话记录代理信息
     **/
    public void insertCallLog() {
        Cursor cursor = mContext.getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI, new String[]{android.provider.CallLog.Calls._ID}, null, null, android.provider.CallLog.Calls.DATE + " DESC");
        if (cursor == null || cursor.getCount() <= 0)
            return;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(android.provider.CallLog.Calls._ID));
            CallLogProxy callLog = findCallLogById(id);
            if (callLog == null) {
                callLog = new CallLogProxy();
                callLog.setId(id);
                mCallLogDao.insert(callLog);
            }
        }
        cursor.close();
    }

    /**
     * 新增短信记录代理信息
     **/
    public void insertSmsLog() {
        Cursor cursor = mContext.getContentResolver().query(
                Uri.parse("content://sms"), new String[]{Telephony.Sms._ID, "read"}, null, null, "date DESC");
        if (cursor == null || cursor.getCount() <= 0)
            return;
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(Telephony.Sms._ID));
            SmsProxy msg = findSmsById(id);
            if (msg == null) {
                msg = new SmsProxy();
                msg.setId(id);
                mSmsDao.insert(msg);
            }
        }
        cursor.close();
    }

    /**
     * 获取所有联系人
     **/
    public List<ContactsProxy> findAllContacts() {
        return mContactDao.queryBuilder().where(ContactsProxyDao.Properties.Recyle.eq(0)).list();
    }

    /**
     * 获取所有带有昵称的联系人
     **/
    public List<ContactsProxy> findAllNickContacts() {
        return mContactDao.queryBuilder().where(ContactsProxyDao.Properties.NickName.isNotNull(), ContactsProxyDao.Properties.Recyle.eq(0)).list();
    }

    /**
     * 获取指定ID的联系人记录
     **/
    public ContactsProxy findContactsById(String id) {
        return mContactDao.queryBuilder().where(ContactsProxyDao.Properties.RawContactId.eq(id)).unique();
    }

    public List<CallLogProxy> findAllCallLogs() {
        return mCallLogDao.queryBuilder().where(CallLogProxyDao.Properties.Recyle.eq(0)).list();
    }

    public CallLogProxy findCallLogById(long id) {
        return mCallLogDao.queryBuilder().where(CallLogProxyDao.Properties.Id.eq(id)).unique();
    }

    public List<SmsProxy> findAllSms() {
        return mSmsDao.queryBuilder().where(SmsProxyDao.Properties.Recyle.eq(0)).list();
    }

    public SmsProxy findSmsById(long id) {
        return mSmsDao.queryBuilder().where(SmsProxyDao.Properties.Id.eq(id)).unique();
    }

    /**
     * 获取指定类型对象实例
     **/
    public <T> T getSyncDao(Class<T> clazz) {
        T t = null;
        try {
            //内部类编译时会在构造方法中加入外部类作为参数，所以通过反射无参构造无效
            t = clazz.getDeclaredConstructor(CallAndSmsDao.class).newInstance(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return t;
    }

    /**
     * 同步数据
     **/
    public void sync(SyncDao syncDao) {
        Single.just(syncDao)
                .observeOn(Schedulers.io())
                .doOnSuccess(new Consumer<SyncDao>() {
                    @Override
                    public void accept(SyncDao syncDao) throws Exception {
                        AndroidChatRobotBuilder.get().robot().actionTargetAccessor().sync(syncDao);
                    }
                })
                .subscribe();
    }

    public void clearRecyleData() {
        List<ContactsProxy> contacts = mContactDao.queryBuilder().where(ContactsProxyDao.Properties.Recyle.eq(1)).list();
        mContactDao.deleteInTx(contacts);

        List<CallLogProxy> callLogs = mCallLogDao.queryBuilder().where(CallLogProxyDao.Properties.Recyle.eq(1)).list();
        mCallLogDao.deleteInTx(callLogs);

        List<SmsProxy> list = mSmsDao.queryBuilder().where(SmsProxyDao.Properties.Recyle.eq(1)).list();
        mSmsDao.deleteInTx(list);
    }

    /**
     * 通讯录同步管理器
     **/
    public class ContactsDao implements SyncDao<Contacts> {

        public void convertEntity(List<Contacts> syncList) {
            insertRawContacts();
            List<ContactsProxy> contactList = findAllContacts();
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = ContactsContract.Data.CONTENT_URI;
            for (ContactsProxy proxy : contactList) {
                if (syncList.size() == 500)
                    break;
                Contacts contact = new Contacts();
                contact.setLid(Integer.valueOf(proxy.getRawContactId()));
                contact.setCid(Integer.valueOf(proxy.getRawContactId()));
                contact.setTimestamp(proxy.getTimestamp());
                contact.setSid(proxy.getSid());
                contact.setNickname(proxy.getNickName());
                Cursor cursor = resolver.query(uri, null, "raw_contact_id=?", new String[]{proxy.getRawContactId()}, null);
                if (cursor == null || cursor.getCount() <= 0) {
                    contact.setRecyle(1);
                    contact.setSynced(false);
                    syncList.add(contact);
                    continue;
                }
                contact.setRecyle(proxy.getRecyle());
                contact.setSynced(proxy.getSynced());
                List<ContactNum> codes = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                    String mimetype = cursor.getString(cursor.getColumnIndex("mimetype"));
                    if ("vnd.android.cursor.item/name".equals(mimetype)) {
                        contact.setName(data1);
                        if (!TextUtils.isEmpty(proxy.getName()) && !proxy.getName().equals(data1))
                            contact.setSynced(false);
                    } else if ("vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                        ContactNum contactNum = new ContactNum();
                        contactNum.setNumber(data1);
                        codes.add(contactNum);
                    } else if ("vnd.android.cursor.item/organization".equals(mimetype)) {
                        contact.setCompany(data1);
                        String job = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA4));
                        contact.setJob(job);
                        if ((TextUtils.isEmpty(proxy.getCompany()) && !TextUtils.isEmpty(data1))
                                || (!TextUtils.isEmpty(proxy.getCompany()) && TextUtils.isEmpty(data1))
                                || (!TextUtils.isEmpty(proxy.getCompany()) && !proxy.getCompany().equals(data1))) {
                            contact.setSynced(false);
                        }
                        if ((TextUtils.isEmpty(proxy.getJob()) && !TextUtils.isEmpty(job))
                                || (!TextUtils.isEmpty(proxy.getJob()) && TextUtils.isEmpty(job))
                                || (!TextUtils.isEmpty(proxy.getJob()) && !proxy.getJob().equals(job))) {
                            contact.setSynced(false);
                        }
                    }

                }
                //比对联系人号码。。。
                if (contact.isSynced() && !TextUtils.isEmpty(proxy.getCodes())) {
                    contact.setSynced(false);
                    String[] numbers = proxy.getCodes().split("\\|");
                    if (numbers.length == codes.size()) {
                        for (ContactNum num : codes) {
                            contact.setSynced(false);
                            for (String number : numbers) {
                                if (number.equals(num.getNumber())) {
                                    contact.setSynced(true);
                                    break;
                                }
                            }
                        }
                    }
                }
                contact.setCodes(codes);
                if (!contact.isSynced())
                    syncList.add(contact);
                cursor.close();
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_CONTACT;
        }

        @Override
        public Class<Contacts> getTargetClass() {
            return Contacts.class;
        }

        @Override
        public long getLastTimestamp() {
            long t = 0;
            ContactsProxy proxy = mContactDao.queryBuilder().orderDesc(ContactsProxyDao.Properties.Timestamp).limit(1).unique();
            if (proxy != null)
                t = proxy.getTimestamp();
            return t;
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<Contacts> list) {
            if (list != null && list.size() > 0) {
                mergeContactData(list);
                //由于一次最多只能合并100条数据，合并数据后再递归请求数据刷新，保证所有云端数据刷新完毕
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<Contacts> getUnSyncLocalData(int i) {
            List<Contacts> list = new ArrayList<>();
            convertEntity(list);
            Log.i("LingJu", "ContactsDao getUnSyncLocalData() " + list.size());
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return (int) mContactDao.queryBuilder().where(ContactsProxyDao.Properties.Synced.eq(false)).count();
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }

    /**
     * 合并云端联系人数据
     **/
    private void mergeContactData(List<Contacts> list) {
        List<ContactsProxy> proxies = new ArrayList<>();
        for (Contacts entity : list) {
            Log.i("LingJu", "CallAndSmsDao mergeContactData()>>> id:" + entity.getCid() + " " + entity.getName() + " 时间戳：" + entity.getTimestamp());
            ContactsProxy proxy = findContactsById(String.valueOf(entity.getCid()));
            if (proxy == null)
                proxy = new ContactsProxy();
            proxy.setRawContactId(String.valueOf(entity.getCid()));
            proxy.setRecyle(entity.getRecyle());
            proxy.setSid(entity.getSid());
            proxy.setTimestamp(entity.getTimestamp());
            proxy.setName(entity.getName());
            proxy.setNickName(entity.getNickname());
            proxy.setCompany(entity.getCompany());
            proxy.setJob(entity.getJob());
            List<ContactNum> codes = entity.getCodes();
            StringBuilder numbers = new StringBuilder();
            for (ContactNum num : codes) {
                numbers.append(num.getNumber()).append("|");
            }
            if (numbers.length() > 1) {
                numbers.deleteCharAt(numbers.length() - 1);
                proxy.setCodes(numbers.toString());
            }
            proxy.setSynced(true);
            proxies.add(proxy);
        }
        mContactDao.insertOrReplaceInTx(proxies);
    }

    /**
     * 通话记录同步管理
     **/
    public class CallLogDao implements SyncDao<CallLog> {

        private void convertEntity(List<CallLog> syncList) {
            insertCallLog();
            List<CallLogProxy> proxies = findAllCallLogs();
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = android.provider.CallLog.Calls.CONTENT_URI;
            for (CallLogProxy proxy : proxies) {
                if (syncList.size() == 500)
                    break;
                CallLog callLog = new CallLog();
                callLog.setCid((int) proxy.getId());
                callLog.setLid((int) proxy.getId());
                callLog.setTimestamp(proxy.getTimestamp());
                callLog.setSid(proxy.getSid());
                Cursor cursor = resolver.query(uri, null, android.provider.CallLog.Calls._ID + "=?", new String[]{String.valueOf(proxy.getId())}, null);
                if (cursor == null || cursor.getCount() <= 0) {
                    callLog.setRecyle(1);
                    callLog.setSynced(false);
                    syncList.add(callLog);
                    continue;
                }
                callLog.setRecyle(proxy.getRecyle());
                callLog.setSynced(proxy.getSynced());
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME));
                    ContactNum num = new ContactNum();
                    num.setName(name);
                    num.setNumber(number);
                    callLog.setCode(num);
                    long date = cursor.getLong(cursor.getColumnIndex(android.provider.CallLog.Calls.DATE));
                    int duration = cursor.getInt(cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION));
                    callLog.setItime(date);
                    callLog.setCtime(date);
                    callLog.setHtime(date + duration * 1000);
                    int type = cursor.getInt(cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE));
                    callLog.setStatus(type == 3 ? 0 : type);
                }
                if (!callLog.isSynced())
                    syncList.add(callLog);
                cursor.close();
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_CALL_LOG;
        }

        @Override
        public Class<CallLog> getTargetClass() {
            return CallLog.class;
        }

        @Override
        public long getLastTimestamp() {
            long t = 0;
            CallLogProxy proxy = mCallLogDao.queryBuilder().orderDesc(CallLogProxyDao.Properties.Timestamp).limit(1).unique();
            if (proxy != null)
                t = proxy.getTimestamp();
            return t;
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<CallLog> list) {
            if (list != null && list.size() > 0) {
                mergeCallLogData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<CallLog> getUnSyncLocalData(int i) {
            List<CallLog> list = new ArrayList<>();
            convertEntity(list);
            Log.i("LingJu", "CallLogDao getUnSyncLocalData() " + list.size());
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return (int) mCallLogDao.queryBuilder().where(CallLogProxyDao.Properties.Synced.eq(false)).count();
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }

    /**
     * 合并云端通话记录数据
     **/
    private void mergeCallLogData(List<CallLog> list) {
        List<CallLogProxy> proxies = new ArrayList<>();
        for (CallLog callLog : list) {
            Log.i("LingJu", "CallAndSmsDao mergeCallLogData()>>> " + callLog.getCid() + " " + callLog.getTimestamp());
            CallLogProxy proxy = findCallLogById(callLog.getCid());
            if (proxy == null)
                proxy = new CallLogProxy();
            proxy.setId(callLog.getCid());
            proxy.setSynced(true);
            proxy.setRecyle(callLog.getRecyle());
            proxy.setSid(callLog.getSid());
            proxy.setTimestamp(callLog.getTimestamp());
            proxies.add(proxy);
        }
        mCallLogDao.insertOrReplaceInTx(proxies);
    }

    /**
     * 短信记录同步管理
     **/
    public class MessageDao implements SyncDao<Message> {
        /**
         * 填充待同步数据
         **/
        private void convertEntity(List<Message> syncList) {
            insertSmsLog();
            List<SmsProxy> smsList = findAllSms();
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            for (SmsProxy sms : smsList) {
                if (syncList.size() == 500)
                    break;
                Message message = new Message();
                message.setCid((int) sms.getId());
                message.setLid((int) sms.getId());
                message.setTimestamp(sms.getTimestamp());
                message.setSid(sms.getSid());
                Cursor cursor = resolver.query(uri, null, Telephony.Sms._ID + "=?", new String[]{String.valueOf(sms.getId())}, null);
                if (cursor == null || cursor.getCount() <= 0) {
                    message.setRecyle(1);
                    message.setSynced(false);
                    syncList.add(message);
                    continue;
                }
                message.setRecyle(sms.getRecyle());
                while (cursor.moveToNext()) {
                    String phoneNum = cursor.getString(cursor.getColumnIndex("address"));
                    int read = cursor.getInt(cursor.getColumnIndex("read"));
                    int type = cursor.getInt(cursor.getColumnIndex("type"));
                    String content = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    read = read == 0 ? 1 : 0;
                    ContactNum num = new ContactNum();
                    num.setNumber(phoneNum);
                    message.setCode(num);
                    message.setStatus(read);
                    message.setSynced(sms.getStatus() == read);
                    message.setType(type == 1 ? type : 0);
                    message.setContent(content);
                    message.setCreated(new Date(date));
                }
                if (!message.isSynced())
                    syncList.add(message);
                cursor.close();
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_SMS_LOG;
        }

        @Override
        public Class<Message> getTargetClass() {
            return Message.class;
        }

        @Override
        public long getLastTimestamp() {
            long t = 0;
            SmsProxy proxy = mSmsDao.queryBuilder().orderDesc(SmsProxyDao.Properties.Timestamp).limit(1).unique();
            if (proxy != null)
                t = proxy.getTimestamp();
            Log.i("LingJu", "MessageDao getLastTimestamp()>>> " + t);
            return t;
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<Message> list) {
            if (list != null && list.size() > 0) {
                mergeMessageData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        /**
         * 合并云端短信记录
         **/
        private void mergeMessageData(List<Message> list) {
            List<SmsProxy> smsList = new ArrayList<>();
            for (Message msg : list) {
                Log.i("LingJu", "MessageDao mergeMessageData()>>> " + msg.getCid() + " " + msg.getContent() + " " + msg.getTimestamp());
                SmsProxy sms = findSmsById(msg.getCid());
                if (sms == null)
                    sms = new SmsProxy();
                sms.setId(msg.getCid());
                sms.setStatus(msg.getStatus());
                sms.setSid(msg.getSid());
                sms.setSynced(true);
                sms.setRecyle(msg.getRecyle());
                sms.setTimestamp(msg.getTimestamp());
                smsList.add(sms);
            }
            mSmsDao.insertOrReplaceInTx(smsList);
        }

        @Override
        public List<Message> getUnSyncLocalData(int i) {
            List<Message> list = new ArrayList<>();
            convertEntity(list);
            Log.i("LingJu", "MessageDao getUnSyncLocalData() " + list.size());
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return (int) mSmsDao.queryBuilder().where(SmsProxyDao.Properties.Synced.eq(false)).count();
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }
}
