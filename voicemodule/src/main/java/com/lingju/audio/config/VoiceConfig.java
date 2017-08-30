package com.lingju.audio.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Administrator on 2016/11/1.
 */
public class VoiceConfig {
    private static final String VOL_NAME="speaker_key";
    private static VoiceConfig instance;
    private SharedPreferences mPreferences;

    private VoiceConfig(Context context){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static synchronized VoiceConfig create(Context context){
        if (instance == null)   instance = new VoiceConfig(context);
        return instance;
    }

    public static VoiceConfig get(){return instance;}

    public String getVolName(){
        return mPreferences.getString(VOL_NAME, "vixq");
    }

    public int getVolume(){
        return 100;
    }

    public int getVolSpeed(){
        return 60;
    }

}
