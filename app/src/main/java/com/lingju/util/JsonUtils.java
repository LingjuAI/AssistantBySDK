package com.lingju.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lingju.assistant.entity.action.PlayerEntity;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2016/12/14.
 */
public class JsonUtils {

    public static final String DATE_FROMAT = "EEE MMM dd HH:mm:ss z yyyy";
    /**
     * 解析单个json
     **/
    public static <T> T getObj(String json, Class<T> clazz) {
        Gson gson = new GsonBuilder()
                .setDateFormat(DATE_FROMAT)
                .create();
        T t = gson.fromJson(json, clazz);
        return t;
    }

    public static <T> PlayerEntity<T> getPlayerEntity(String json, Class<T> clazz) {
        Gson gson = new GsonBuilder()
                .setDateFormat(DATE_FROMAT)
                .create();
        Type type = new ParameterizedTypeImpl(PlayerEntity.class, new Type[]{clazz});
        return gson.fromJson(json, type);
    }

    /**
     * 解析json数组
     **/
    public static <T> List<T> getList(String jsonArray, Class<T> clazz) {
        Gson gson = new GsonBuilder()
                .setDateFormat(DATE_FROMAT)
                .create();
        List<T> list = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(jsonArray);
            for (int i = 0; i < json.length(); i++) {
                T t = gson.fromJson(json.getString(i), clazz);
                list.add(t);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 将对象封装成JsonArray对象
     **/
    public static <T> JSONArray bean2JsonArray(List<T> list) {
        try {
            Gson gson = new GsonBuilder()
                    .setDateFormat(DATE_FROMAT)
                    .create();
            String jsonArray = gson.toJson(list);
            JSONArray json = new JSONArray(jsonArray);
            Log.i("LingJu", "json字符串：" + json.toString());
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
