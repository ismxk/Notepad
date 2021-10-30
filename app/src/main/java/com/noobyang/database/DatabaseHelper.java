package com.noobyang.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.android.AndroidDatabaseConnection;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.LocalLog;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * quote
 */

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private List<TableHandler> tableHandlers;
    /**
     * dao 缓存
     */
    private Map<String, Dao> daoMap;

    private static volatile Map<String, DatabaseInfo> initDbList = new HashMap<String, DatabaseInfo>();

    /**
     * 获取数据库实例
     */
    public static DatabaseHelper getInstance(Context context, String dbName) {
        return getInstance(context, dbName, -1);
    }

    /**
     * 获取数据库实例
     * @param version debug 版本会生效，用于自测
     */
    public static synchronized DatabaseHelper getInstance(Context context, String dbName, int version) {
        if (TextUtils.isEmpty(dbName)) {
            Log.d(TAG, "database name is null");
            throw new IllegalStateException("database name is null");
        }

        if (null == initDbList.get(dbName) || null == initDbList.get(dbName).getHelper()) {
            Log.d(TAG, "current instance is  null now,init here");
            return initDatabase(context, dbName, version);
        }
        return initDbList.get(dbName).getHelper();
    }

    /**
     * 初始化数据库
     * @param dbName   数据库名
     * @param version debug 版本会生效，用于自测
     */
    private static DatabaseHelper initDatabase(Context context, String dbName, int version) {
        Log.i(TAG, "initDatabase dbName = " + dbName + ",version " + version + ",ThreadId = " + Thread.currentThread().getName());
        synchronized (DatabaseHelper.class) {
            DatabaseHelper helper = null;
            DatabaseInfo inst = initDbList.get(dbName);
            if (inst == null) {
                inst = new DatabaseInfo();
            }
            inst.setDatabaseName(dbName);

            int dbVersion = -1;
            boolean isDebug = false;
            try {
                Class clazz = Class.forName(context.getPackageName() + ".BuildConfig");
                Field VERSION_CODE = clazz.getDeclaredField("VERSION_CODE");
                VERSION_CODE.setAccessible(true);
                dbVersion = VERSION_CODE.getInt(null);

                Field DEBUG = clazz.getDeclaredField("DEBUG");
                DEBUG.setAccessible(true);
                isDebug = DEBUG.getBoolean(null);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            if (isDebug && version != -1) {
                dbVersion = version;
            }
            if(dbVersion == -1){
                Log.d(TAG,"something went wrong,need set version = " + 1);
                dbVersion = 1;
            }
            Log.i(TAG, "initDatabase dbVersion " + dbVersion);
            inst.setDatabaseVersion(dbVersion);
            helper = new DatabaseHelper(context, dbName, null, dbVersion);
            inst.setHelper(helper);
            initDbList.put(dbName, inst);
            return helper;
        }
    }

    public DatabaseHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
        System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, "FATAL");
        daoMap = new HashMap<String, Dao>();
    }

    /**
     * 注册数据表
     */
    public <T> void registerTable(Class<T> clazz) {
        if (tableHandlers == null) {
            tableHandlers = new ArrayList<TableHandler>();
        }
        TableHandler<T> handler = new TableHandler<T>(clazz);
        if (isValidTable(handler)) {
            tableHandlers.add(handler);
        }
    }

    public boolean isValidTable(TableHandler handler) {
        if (tableHandlers == null || handler == null) {
            return false;
        }
        String tableName = handler.getTableName();
        for (TableHandler element : tableHandlers) {
            if (tableName.equals(element.getTableName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            for (TableHandler handler : tableHandlers) {
                handler.create(connectionSource);
            }
        } catch (SQLException e) {
            Log.e("database create fail", e.toString());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) {
        Log.i(TAG,"数据库升级了" + " oldVersion = " + oldVersion + " newVersion = " + newVersion);
        try {
            Log.i(TAG, "onUpgrade: 开始升级数据表");
            for (TableHandler handler : tableHandlers) {
                handler.onUpgrade(db, cs);
            }
            Log.i(TAG, "onUpgrade: 升级数据表完成，tableHandlers.size = " + (tableHandlers==null ? -1 : tableHandlers.size()));
        } catch (SQLException e) {
            Log.e("database upgrade fail", e.toString());
        }
    }

    /**
     * 数据库降级
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ConnectionSource cs = getConnectionSource();
        Object conn = cs.getSpecialConnection();
        boolean clearSpecial = false;
        if (conn == null) {
            conn = new AndroidDatabaseConnection(db, true, this.cancelQueriesEnabled);
            try {
                cs.saveSpecialConnection((DatabaseConnection) conn);
                clearSpecial = true;
            } catch (SQLException var11) {
                throw new IllegalStateException("Could not save special connection", var11);
            }
        }

        try {
            this.onDowngrade(cs, oldVersion, newVersion);
        } finally {
            if (clearSpecial) {
                cs.clearSpecialConnection((DatabaseConnection) conn);
            }
        }
    }

    public void onDowngrade(ConnectionSource cs, int oldVersion, int newVersion) {
        Log.i(TAG,"数据库降级了" + " oldVersion = " + oldVersion + " newVersion = " + newVersion);
        try {
            for (TableHandler handler : tableHandlers) {
                handler.onDowngrade(cs, oldVersion, newVersion);
            }
        } catch (SQLException e) {
            Log.e("database downgrade fail", e.toString());
        }
    }

    /**
     * 清空所有表的数据
     */
    public void clearAllTable() {
        try {
            for (TableHandler handler : tableHandlers) {
                handler.clear(connectionSource);
            }
        } catch (SQLException e) {
            Log.e("clear all table fail", e.toString());
        }
    }

    /**
     * 获取dao
     */
    public synchronized Dao getDao(Class cls) {
        Dao dao;
        String clsName = cls.getSimpleName();
        if (daoMap.containsKey(clsName)) {
            dao = daoMap.get(clsName);
        } else {
            try {
                dao = super.getDao(cls);
            } catch (SQLException e) {
                Log.e("database operate fail", e.toString());
                return null;
            }
            daoMap.put(clsName, dao);
        }
        return dao;
    }

    /**
     * 释放dao
     */
    @Override
    public void close() {
        super.close();
        synchronized (this) {
            Iterator<Map.Entry<String, Dao>> it = daoMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Dao> entry = it.next();
                Dao dao = entry.getValue();
                dao = null;
                it.remove();
            }
        }
    }

    public synchronized void closeDbHelper(String dbName) {
        if (null == initDbList.get(dbName) || null == initDbList.get(dbName).getHelper()) {
            Log.i(TAG, "current database helper is null");
            return;
        }
        initDbList.get(dbName).getHelper().close();
    }

}
