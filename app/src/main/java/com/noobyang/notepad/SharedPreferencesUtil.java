package com.noobyang.notepad;

/**
 * quote
 */

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {

    private static SharedPreferences sharedPreferences;
    private static SharedPreferencesUtil sharedPreferencesUtil;

    private SharedPreferencesUtil(Context context, String name) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static SharedPreferencesUtil getInstance(Context context) {
        if (sharedPreferencesUtil == null) {
            synchronized (SharedPreferencesUtil.class) {
                if (sharedPreferencesUtil == null) {
                    sharedPreferencesUtil = new SharedPreferencesUtil(context, "sp_notepad");
                }
            }
        }
        return sharedPreferencesUtil;
    }

    private SharedPreferences.Editor getEditor() {
        return sharedPreferences.edit();
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }


    public boolean putString(String key, String value){
        return getEditor().putString(key, value).commit();
    }

    public boolean putBoolean(String key, boolean value){
        return getEditor().putBoolean(key, value).commit();
    }

    public String getString(String key, String defValue){
        return sharedPreferences.getString(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue){
        return sharedPreferences.getBoolean(key, defValue);
    }

    public static int[] string2IntArray(String str){
        if(null == str || str.isEmpty()){
            return null;
        }
        int len = str.length();
        int[] array = new int[len];
        for(int i=0; i<len; i++){
            array[i] = Integer.valueOf(str.substring(i,i+1));
        }
        return array;
    }

    public static String intArray2String(int[] array){
        if(null == array || 0 == array.length){
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int anArray : array) {
            stringBuilder.append(anArray);
        }
        return stringBuilder.toString();
    }

    public boolean setAccount(String account) {
        return sharedPreferencesUtil.putString("user_account", account);
    }
    public String getAccount() {
        return sharedPreferencesUtil.getString("user_account", "");
    }
}
