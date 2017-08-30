package com.lingju.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.iflytek.cloud.util.UserWords;
import com.lingju.assistant.R;
import com.lingju.common.log.Log;
import com.lingju.config.Setting;
import com.lingju.context.entity.ContactNum;
import com.lingju.model.Contact;
import com.lingju.model.RawContact;
import com.lingju.model.SmsInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 手机联系人数据管理工具
 *
 * @author ydshu
 */
public class PhoneContactUtils {
    private final static String TAG = "PhoneContactUtils";

    private static PhoneContactUtils mIntance;
    private Context mContext;
    private UserWords userWords = new UserWords();
    public List<Contact> list = new ArrayList<>();
    public List<Contact> last2HourCallContacts = new ArrayList<>();
    public List<Contact> last2HourMsgContacts = new ArrayList<>();

    private long searchInDB = 0;
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    public static final String SMS_URI_ALL = "content://sms/";

    static String[] numbers = new String[]{"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    public final static String UNKOWN_NAME = "陌生号码";

    private PhoneContactUtils(Context context) {
        mContext = context;
    }

    public static PhoneContactUtils getInstance(Context context) {
        if (mIntance == null) {
            mIntance = new PhoneContactUtils(context);
        }
        return mIntance;
    }


    public static String getDateString(int hour, int minute) {
        StringBuilder sb = new StringBuilder();
        if (hour == 10) {
            sb.append("十点");
        } else if (hour > 10) {
            int t = hour / 10 % 10;
            sb.append(t == 1 ? "" : numbers[t]).append("十").append(numbers[hour % 10]).append("点");
        } else if (hour >= 0) {
            sb.append(numbers[hour % 10]).append("点");
        }

        if (minute == 10) {
            sb.append("十分");
        } else if (minute > 10) {
            int t = minute / 10 % 10;
            sb.append(t == 1 ? "" : numbers[t]).append("十").append(numbers[minute % 10]).append("分");
        } else if (minute >= 0) {
            sb.append(numbers[minute % 10]).append("分");
        }

        return sb.toString();
    }

    public List<Contact> getList() {
        return list;
    }

    public void setContactsNameList() {
        //加入自定义关键词充当联系人
        Log.e(TAG, "setContactsNameList");
        if (userWords.getWords() == null || userWords.getWords().size() == 0) {
            formatListName();
            String keywords[] = mContext.getResources().getStringArray(R.array.keywords);
            int l = keywords.length;
            while (--l >= 0) {
                userWords.putWord(keywords[l]);
            }
        }
    }

    public String getContactsSplitByTr() {
        if (userWords.getWords() == null || userWords.getWords().size() == 0) {
            setContactsNameList();
        }
        StringBuffer buff = new StringBuffer();
        ArrayList<String> list = userWords.getWords();
        int l = list.size();
        while (--l > 0) {
            buff.append(list.get(l)).append("\r\n");
        }
        if (l == 0)
            buff.append(list.get(0));
        return buff.toString();
    }

    /**
     * 返回通讯录名字对应的索引
     *
     * @param name
     * @return
     */
    public int nameIndex(String name) {
        if (list == null || list.size() == 0)
            return -1;
        int l = list.size();
        while (--l >= 0) {
            if (list.get(l).getFormatedName().equals(name)) {
                return l;
            }
        }
        return -1;
    }

    public String getNameByNum(StringBuilder number) {
        for (Contact c : list) {
            if (c.getDatas() != null)
                for (RawContact rc : c.getDatas()) {
                    if (isTheSame(rc.getNumber(), number)) {
                        return c.getName();
                    }
                }
        }
        return number.toString();
    }

    public Contact getContactByNum(StringBuilder number) {
        for (Contact c : list) {
            if (c.getDatas() != null)
                for (RawContact rc : c.getDatas()) {
                    if (isTheSame(rc.getNumber(), number)) {
                        return c;
                    }
                }
        }
        return null;
    }

    public static boolean isTheSame(String num1, StringBuilder num2) {
        int l1 = num1.length();
        int l2 = num2.length();
        if (l1 > l2) {
            if (num2.length() < 11) {
                return false;
            }
            if (num2.length() > 11) {
                num2.delete(0, l2 - 11);//num2.substring(l2-11);
            }
            if (num1.endsWith(num2.toString())) {
                num2.setLength(0);
                num2.append(num1);
                return true;
            }
        } else if (l1 == l2) {
            if (num1.equals(num2.toString()))
                return true;
            else if (l1 > 11) {
                if (num1.substring(l1 - 11).equals(num2.substring(l2 - 11))) {
                    num2.setLength(0);
                    num2.append(num1);
                    return true;
                }
            }
        } else {
            if (num1.length() < 11) {
                return false;
            }
            if (num1.length() > 11) {
                num1 = num1.substring(l1 - 11);
            }
            if (num2.toString().endsWith(num1)) {
                num2.setLength(0);
                num2.append(num1);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取经过格式化的通讯录名字列表
     *
     * @return
     */
    public void fillFormatListName() {
        if (list != null && list.size() > 0)
            return;
        formatListName();
    }

    public void formatListName() {
        Log.e(TAG, "formatListName");
        if (list.size() == 0) {
            list = getContactsList();
            new FilledContactsTask().execute();
        }
        if (list.size() > 0) {
            StringBuffer temp = new StringBuffer();
            int l = 0;
            char ct = 0;
            for (Contact c : list) {
                l = c.getName().length();
                while (--l >= 0) {
                    if (((ct = c.getName().charAt(l)) >= 0x4e00 && ct <= 0x9fb0) ||
                            (ct >= 97 && ct <= 122) ||
                            (ct >= 65 && ct <= 90) ||
                            (ct >= 48 && ct < 58)) {
                        temp.insert(0, ct);
                    }
                }
                c.setFormatedName(temp.toString());
                temp.setLength(0);
                userWords.putWord(c.getFormatedName());
            }
        }
    }

    public void insertSMS(String phone, String content) {
        String ADDRESS = "address";
        String DATE = "date";
        String READ = "read";
        String STATUS = "status";
        String TYPE = "type";
        String BODY = "body";
        ContentValues values = new ContentValues();
        /* 手机号 */
        values.put(ADDRESS, phone);
		/* 时间 */
        values.put(DATE, String.valueOf(System.currentTimeMillis()));
        values.put(READ, 1);
        values.put(STATUS, -1);
		/* 类型1为收件箱，2为发件箱 */
        values.put(TYPE, 2);
		/* 短信体内容 */
        values.put(BODY, content);
		/* 插入数据库操作 */
        mContext.getContentResolver().insert(Uri.parse("content://sms"), values);
    }

    public Contact getContactByName(String name) {
        int l = list.size();
        while (--l >= 0) {
            if (list.get(l).getName().equals(name)) {
                return list.get(l);
            }
        }
        return null;
    }

    /**
     * 获取手机联系人
     **/
    private List<Contact> getContactsList() {
        long begin = System.currentTimeMillis();
        List<Contact> contacts = new ArrayList<>();
        String[] selectContact = {
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.DATA_VERSION};
        //ContactsContract.CommonDataKinds.Phone._ID};

        Cursor cursor = null;
        try {
            // 执行查询
            cursor = mContext.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, selectContact, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
            searchInDB = System.currentTimeMillis();
            if (cursor == null || cursor.getCount() <= 0) {
                // TODO: 2017/4/11 读取联系人失败（提醒用户检查是否允许读取联系人权限）
                return contacts;
            }
            Log.d(TAG, "getContactsNameList>>>>Contacts count: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                int numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int lastTimeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED);
                int timesContactedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED);
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
                int versionIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA_VERSION);

                int postion = -1, version;
                String number, contactName;
                RawContact c;
                Contact ct;
                while (!cursor.isAfterLast()) {
                    contactName = cursor.getString(nameIndex);
                    number = cursor.getString(numIndex).replaceAll("(\\+86)|[^\\d]", "");
                    version = cursor.getInt(versionIndex);
                    if (!TextUtils.isEmpty(contactName) && !TextUtils.isEmpty(number)) {
                        if ((postion = searchElmentInList(contacts, contactName)) > -1) {
                            RawContact rc = null;
                            if ((rc = numberExist(number, contacts.get(postion))) == null) {
                                c = new RawContact();
                                c.setDisplayName(contactName);
                                c.setId(cursor.getInt(idIndex));
                                c.setNumber(number);
                                if (cursor.getLong(lastTimeIndex) > 0)
                                    c.setLastContacted(cursor.getLong(lastTimeIndex));
                                c.setTimesContacted(cursor.getInt(timesContactedIndex));
                                c.setVersion(version);
                                contacts.get(postion).getDatas().add(c);
                            } else if (cursor.getInt(timesContactedIndex) > 0)
                                if (cursor.getLong(lastTimeIndex) > rc.getLastContacted()) {
                                    rc.setLastContacted(cursor.getLong(lastTimeIndex));
                                    if (cursor.getInt(timesContactedIndex) > rc.getTimesContacted())
                                        rc.setTimesContacted(cursor.getInt(timesContactedIndex));
                                    if (version > rc.getVersion())
                                        rc.setVersion(version);
                                }
                        } else {
                            c = new RawContact();
                            ct = new Contact();
                            ct.setName(contactName);
                            c.setDisplayName(contactName);
                            c.setId(cursor.getInt(idIndex));
                            c.setNumber(number);
                            if (cursor.getLong(lastTimeIndex) > 0)
                                c.setLastContacted(cursor.getLong(lastTimeIndex));
                            c.setTimesContacted(cursor.getInt(timesContactedIndex));
                            c.setVersion(version);
                            ct.getDatas().add(c);
                            contacts.add(ct);
                        }
                    }
                    cursor.moveToNext();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        //Log.i(TAG, "Phone contacts name list: " + log);
        //this.contactText=log.toString();
        Log.e(TAG, "getContactsList 耗时：" + (System.currentTimeMillis() - begin) + "毫秒");
        return contacts;
    }


    private void fillOrganizeContactList(List<Contact> contacts) {
        String[] selectContact = {
                ContactsContract.CommonDataKinds.Organization.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE};
        Cursor cursor = null;
        try {
            // 执行查询
            cursor = mContext.getContentResolver().query(
                    Data.CONTENT_URI, selectContact,
                    Data.MIMETYPE + "=?",
                    new String[]{ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                    ContactsContract.CommonDataKinds.Organization.RAW_CONTACT_ID + " COLLATE LOCALIZED ASC");
            searchInDB = System.currentTimeMillis();
            if (cursor == null || cursor.getCount() <= 0) {
                return;
            }
            Log.d(TAG, "fillOrganizeContactList>>>>Organizetions count: " + cursor.getCount());
            boolean find = false;
            if (cursor.moveToFirst()) {
                int companyIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY);
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.RAW_CONTACT_ID);
                int jobIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE);
                while (!cursor.isAfterLast()) {
                    find = false;
                    if (TextUtils.isEmpty(cursor.getString(companyIndex)) && TextUtils.isEmpty(cursor.getString(jobIndex))) {
                        Log.e(TAG, "null.................");
                    } else
                        for (Contact c : contacts) {
                            for (RawContact rc : c.getDatas()) {
                                if (rc.getId() == cursor.getInt(idIndex)) {
                                    find = true;
                                    c.setCompany(cursor.getString(companyIndex));
                                    c.setJob(cursor.getString(jobIndex));
                                    //Log.e(TAG, "company="+c.getCompany()+",job="+c.getJod());
                                    break;
                                }
                            }
                            if (find)
                                break;
                        }
                    cursor.moveToNext();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
    }


    public void print() {
        for (Contact c : list) {
            List<ContactNum> ns = c.getCodes();
            StringBuffer sb = new StringBuffer(ns.get(0).getNumber());
            if (ns.size() > 1)
                for (int i = 1; i < ns.size(); i++) {
                    sb.append(",").append(ns.get(i).getNumber());
                }
            Log.i(TAG, "name=" + c.getName() + "[" + sb + "]");
        }
    }

    public Contact getLastContacts() {
        return getLastContacts(0);
    }

    /**
     * @param cType CallLog.Calls.OUTGOING_TYPE/INCOMING_TYPE/MISSED_TYPE/0
     * @return
     */
    public Contact getLastContacts(int cType) {
        Log.i(TAG, "getLastContacts>>>>>>>>>");
        Uri uri = CallLog.Calls.CONTENT_URI;
        Cursor cursor = null;
        Long twoHourBefore = System.currentTimeMillis() - 7200000;
        String[] projection = {CallLog.Calls.DATE, // 日期
                CallLog.Calls.NUMBER, // 号码
                CallLog.Calls.TYPE, // 类型
                CallLog.Calls.CACHED_NAME//, // 名字
                // CallLog.Calls._ID // id
        };
        try {
            if (cType > 0) {
                cursor = mContext.getContentResolver().query(uri, projection,
                        CallLog.Calls.TYPE + "=? and " + CallLog.Calls.DATE + ">?",
                        new String[]{Integer.toString(cType/*CallLog.Calls.OUTGOING_TYPE*/), twoHourBefore.toString()},
                        CallLog.Calls.DATE + " DESC LIMIT 1");
            } else {
                cursor = mContext.getContentResolver().query(uri, projection,
                        CallLog.Calls.DATE + ">?", new String[]{twoHourBefore.toString()},
                        CallLog.Calls.DATE + " DESC LIMIT 1");
            }
            if (cursor == null || cursor.getCount() == 0)
                return null;
            if (cursor.moveToFirst()) {
                long time = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                Log.w(TAG, "name=" + name + ",number=" + number + ",type=" + type + ",time=" + time);
                if (!TextUtils.isEmpty(name)) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(name)) {
                            Log.e("getLastContacts", "list.get(i)=" + list.get(i) + ",list.get(i).getCodes()=" + list.get(i).getCodes());
                            if (list.get(i) != null && list.get(i).getCodes() != null && !list.get(i).getCodes().get(0).getNumber().equals(number)) {
                                int j = 0;
                                for (; j < list.get(i).getCodes().size(); j++) {
                                    if (list.get(i).getCodes().get(j).getNumber().equals(number)) {
                                        break;
                                    }
                                }
                                while (j-- > 0) {
                                    list.get(i).getCodes().get(j + 1).setNumber(list.get(i).getCodes().get(j).getNumber());
                                }
                                list.get(i).getCodes().get(0).setNumber(number);
                            }
                            return list.get(i);
                        }
                    }
                } else {
                    Contact c = new Contact();
                    c.setName(name);
                    List<ContactNum> codes = new ArrayList<>();
                    ContactNum contactNum = new ContactNum();
                    contactNum.setNumber(number);
                    contactNum.setLastTime(time);
                    codes.add(contactNum);
                    c.setCodes(codes);
                    return c;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return null;
    }

    public String getLast2HourMissedCalls() {
        Log.i(TAG, "getLast2hourCalls>>>>>>>>>");
        Uri uri = CallLog.Calls.CONTENT_URI;
        Cursor cursor = null;
        String[] projection = {CallLog.Calls.DATE, // 日期
                CallLog.Calls.NUMBER, // 号码
                CallLog.Calls.TYPE, // 类型
                CallLog.Calls.CACHED_NAME//, // 名字
        };
        try {
            cursor = mContext.getContentResolver().query(uri, projection,
                    CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND "
                            + CallLog.Calls.DATE + ">" + (System.currentTimeMillis() - 3600000),
                    null,
                    CallLog.Calls.DATE + " DESC");
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToLast()) {
                StringBuilder sb = new StringBuilder();
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                long time;
                int type;
                String number, name;
                StringBuilder number_ = new StringBuilder();
                int i = 0;
                Calendar cd = Calendar.getInstance();
                last2HourCallContacts.clear();
                Contact ct;
                while (!cursor.isBeforeFirst() && i++ < 20) {
                    time = cursor.getLong(dateIndex);
                    type = cursor.getInt(typeIndex);
                    number = cursor.getString(numberIndex);
                    if (number.startsWith("+86"))
                        number = number.substring(3);
                    number_.setLength(0);
                    number_.append(number);
                    name = cursor.getString(nameIndex);
                    //Log.w(TAG, "name="+name+",number="+number+",type="+type+",time="+time);
                    ct = getContactByNum(number_);
                    last2HourCallContacts.add(ct);
                    if (ct != null)
                        name = ct.getName();
                    if (TextUtils.isEmpty(name)) {
                        name = number;
                    }
                    cd.setTimeInMillis(time);
                    sb.append('第').append(numbers[i]).append("个,");
                    sb.append(getDateString(cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE))).append(",");
                    sb.append(name).append(",").append(number_.toString()).append("\n如果需要，小灵可以帮您回拨电话。\n");
                    cursor.moveToPrevious();
                }
                if (cursor.getCount() > 20) {
                    sb.append("后面还有").append(cursor.getCount() - 20).append("个,请自行打开通话记录查看。");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return "您最近两小时内没有未接来电！";
    }

    public String getLast2HourMissMsg() {
        Uri uri = Uri.parse(SMS_URI_ALL);
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, new String[]{"address", "person",
                            "body", "date", "type", "read"},
                    "type=1 and read=0 " +
                            " and date<" + System.currentTimeMillis(), null, "date desc");
            Log.e(TAG, "getLastMsgContext>>" + cursor.getCount());
            if (cursor.moveToLast()) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                int numberIndex = cursor.getColumnIndex("address");
                int nameIndex = cursor.getColumnIndex("person");
                int bodyIndex = cursor.getColumnIndex("body");
                int dateIndex = cursor.getColumnIndex("date");
                String number_, body, name;
                StringBuilder number = new StringBuilder();
                last2HourMsgContacts.clear();
                Contact ct;
                Calendar cd = Calendar.getInstance();
                while (!cursor.isBeforeFirst() && i++ < 10) {
                    number_ = cursor.getString(numberIndex);
                    if (number_.startsWith("+86"))
                        number_ = number_.substring(3);
                    number.setLength(0);
                    number.append(number_);
                    name = cursor.getString(nameIndex);
                    body = cursor.getString(bodyIndex);
                    ct = getContactByNum(number);
                    last2HourMsgContacts.add(ct);
                    if (ct != null)
                        name = ct.getName();
                    else
                        name = UNKOWN_NAME;
                    if (TextUtils.isEmpty(name))
                        name = number.toString();
                    cd.setTimeInMillis(cursor.getLong(dateIndex));
                    sb.append('第').append(numbers[i]).append("条,");
                    sb.append(getDateString(cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE))).append(",");
                    sb.append("来自").append(name).append(",内容是：");
                    sb.append(body).append("\n");
                    cursor.moveToPrevious();
                }
                if (cursor.getCount() > 20) {
                    sb.append("后面还有").append(cursor.getCount() - 20).append("个,请自行打开短信收件箱查看。");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return "您最近两小时内没有未读短信";
    }

    public void getCalls() {
        Log.i(TAG, "getCalls>>>>>>>>>");
        Uri uri = CallLog.Calls.CONTENT_URI;
        Cursor cursor = null;
        String[] projection = {CallLog.Calls.DATE, // 日期
                CallLog.Calls.NUMBER, // 号码
                CallLog.Calls.TYPE, // 类型
                CallLog.Calls.CACHED_NAME//, // 名字
                // CallLog.Calls._ID // id
        };
        try {
            cursor = mContext.getContentResolver().query(uri, projection,
                    "",
                    null,
                    CallLog.Calls.DATE + " DESC");
            if (cursor.moveToFirst()) {
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                long time;
                int type;
                String number, name;
                while (!cursor.isAfterLast()) {
                    time = cursor.getLong(dateIndex);
                    type = cursor.getInt(typeIndex);
                    number = cursor.getString(numberIndex);
                    name = cursor.getString(nameIndex);
                    //Log.w(TAG, "name="+name+",number="+number+",type="+type+",time="+time);
                    List<RawContact> rcs = null;
                    if (!TextUtils.isEmpty(name)) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getName().equals(name)) {
                                rcs = list.get(i).getDatas();
                                for (RawContact rc : rcs) {
                                    if (rc.getNumber().equals(number)) {
                                        if (rc.getLastContacted() == 0) {
                                            rc.setLastContacted(time);
                                            // TODO: 2017/8/8
                                            // list.get(i).getNumbers().put(number, time);
                                        }
                                        rc.setTimesContacted(rc.getTimesContacted() + 1);
                                    }
                                }
                            }
                        }
                    }
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public long getLastMessage() {
        return 0;
    }

    public long getLastMessage(long lastTime, SmsInfo sms) {
        Uri uri = Uri.parse(SMS_URI_INBOX);
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, new String[]{"address", "person",
                            "body", "date", "type"},
                    "", null, "date desc limit 1");
            Log.e(TAG, "getLastMessage>>" + cursor.getCount());
            if (cursor.moveToFirst()) {
                String number_ = cursor.getString(cursor.getColumnIndex("address"));
                if (number_.startsWith("+86"))
                    number_ = number_.substring(3);
                StringBuilder number = new StringBuilder(number_);
                String name = cursor.getString(cursor.getColumnIndex("person"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                long time = cursor.getLong(cursor.getColumnIndex("date"));
                if (lastTime >= time)
                    return lastTime;
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                sms.setNumber(number_);
                sms.setContent(body);
                sms.setTime(time);
                sms.setType(type);
                sms.setContact(null);
                Log.e(TAG, "getLastMessage>>name=" + name + ",number=" + number + ",body=" + body + ",time=" + time);
                if (TextUtils.isEmpty(name)) {  //某些手机的name始终是null,不管通讯录是否有该号码
                    Contact c = getContactByNum(number);
                    if (c != null) {
                        name = c.getName();
                        Log.e("getLastMessage111", "getNameByNum(number)" + name + "(" + number + ")");
                        sms.setName(name);
                        reSortList(c, number.toString(), time);
                        sms.setContact(c);
                        return time;
                    } else {
                        sms.setName(name);
                        String n = getNameByNum(number);
                        if (!n.equals(number)) {
                            name = n;
                        }
                    }
                }
                if (!TextUtils.isEmpty(name)) {
                    sms.setName(name);
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(name)) {
                            if (!list.get(i).getCodes().get(0).getNumber().equals(number)) {
                                reSortList(list.get(i), number.toString(), time);
                            }
                            sms.setContact(list.get(i));
                            return time;
                        }
                    }
                    if (sms.getContact() == null) {
                        Contact c = getContactByNum(number);
                        if (c != null) {
                            name = c.getName();
                            Log.e("getLastMessage", "getNameByNum(number)" + name + "(" + number + ")");
                            sms.setName(name);
                            reSortList(c, number.toString(), time);
                            sms.setContact(c);
                            return time;
                        }
                    }
                }
                name = UNKOWN_NAME;
                sms.setName(name);
                Contact c = new Contact();
                c.setName(name);
                List<ContactNum> codes = new ArrayList<>();
                ContactNum contactNum = new ContactNum();
                contactNum.setNumber(c.getName());
                contactNum.setLastTime(time);
                codes.add(contactNum);
                c.setCodes(codes);
                sms.setContact(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return 0;
    }

    public String getLastMsgContext(int count, String defaultText) {
        Uri uri = Uri.parse(SMS_URI_INBOX);
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, new String[]{"address", "person",
                            "body", "date", "type"},
                    "", null, "date desc limit " + count);
            Log.e(TAG, "getLastMsgContext>>" + cursor.getCount());
            if (cursor.getCount() == 0)
                return defaultText;
            count = cursor.getCount();
            StringBuilder sb = new StringBuilder();
            if (cursor.moveToLast()) {
                for (int i = 1; i <= count; i++) {
                    String number_ = cursor.getString(cursor.getColumnIndex("address"));
                    if (number_.startsWith("+86"))
                        number_ = number_.substring(3);
                    StringBuilder number = new StringBuilder(number_);
                    String name = cursor.getString(cursor.getColumnIndex("person"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    name = getNameByNum(number);
                    //if(TextUtils.isEmpty(name))name=number;
                    sb.append('第').append(numbers[i]).append("条：");
                    sb.append("来自").append(name).append(",内容是：");
                    sb.append(body).append("<br/>");
                    cursor.moveToPrevious();
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return defaultText;
    }

    public void reSortList(Contact c, String number, long time) {
        int j = 0;
        for (; j < c.getCodes().size(); j++) {
            if (number.contains(c.getCodes().get(j).getNumber())) {
                c.getCodes().get(j).setNumber(number);
                c.getCodes().get(j).setLastTime(time);
                //((LingjuGlobal)mContext.getApplicationContext()).lastSms.setName(c.getCodes()[j]);
                break;
            }
        }
        while (j-- > 0) {
            c.getCodes().get(j + 1).setNumber(c.getCodes().get(j).getNumber());
        }
        c.getCodes().get(0).setNumber(number);
    }


    private RawContact numberExist(String number, Contact c) {
        for (RawContact rc : c.getDatas()) {
            if (rc.getNumber().equals(number)) {
                return rc;
            }
        }
        return null;
    }

    private int searchElmentInList(List<Contact> list, String subStr) {
        if (list == null || list.size() < 1 || subStr == null) {
            return -1;
        }

        int l = list.size();
        while (--l >= 0) {
            if (subStr.charAt(0) != list.get(l).getName().charAt(0)) {
                return -1;
            }
            if (subStr.equals(list.get(l).getName())) {
                return l;
            }
        }
        return -1;
    }


    public String getContactText() {
        return userWords.toString();
    }

    /**
     * 判断SIM卡是否存在
     *
     * @param context
     * @return
     */
    public static boolean isSimCardExist(Context context) {

        TelephonyManager tm;
        try {
            tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                if (TelephonyManager.SIM_STATE_ABSENT == tm.getCallState()) {
                    return false;
                } else {
                    Log.i(TAG, "SIM state: " + tm.getCallState());
                    if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                        Log.e(TAG, "isSimCardExist | NETWORK_TYPE_UNKNOWN");
                        return false;
                    } else {
                        Log.i(TAG, "SIM net type: " + tm.getNetworkType());
                    }
                }
            } else {
                Log.e(TAG, "cann't get TelephonyManager handler.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        Log.d(TAG, "isSimCardExist | return true");
        return true;
    }

    public boolean updateContacts() {
        return searchInDB > 0;
    }

    private Comparator<RawContact> comparator = new Comparator<RawContact>() {
        public int compare(RawContact l, RawContact r) {
            return r.getNumber().length() - l.getNumber().length();
        }

        public boolean equals(Object object) {
            return false;
        }
    };

    class FilledContactsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.e(TAG, "SortContactsTask!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            long begin = System.currentTimeMillis();
            if (list != null) {
                fillOrganizeContactList(list);
                int l;
                for (Contact c : list) {
                    //Log.w(TAG, "name="+c.getName()+",company="+c.getCompany()+",job="+c.getJod());
                    l = c.getDatas().size();
                    if (l > 0) {
                        if (l > 1) {
                            Collections.sort(c.getDatas(), comparator);
                        }
                        List<ContactNum> codes = new ArrayList<>();
                        for (int i = 0; i < l; i++) {
                            ContactNum contactNum = new ContactNum();
                            contactNum.setNumber(c.getDatas().get(i).getNumber());
                            contactNum.setLastTime(c.getDatas().get(i).getLastContacted());
                            codes.add(contactNum);
                        }
                        c.setCodes(codes);
                    }

                }
                getLastMessage();
                if (Setting.must_visit_calls) {
                    getCalls();
                }
                return true;
            }
            Log.e(TAG, "SortContactsTask 耗时：" + (System.currentTimeMillis() - begin) + "毫秒");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //完成通讯录读取

        }
    }
}
