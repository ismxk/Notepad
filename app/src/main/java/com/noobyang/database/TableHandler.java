package com.noobyang.database;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * partial quote
 */

public class TableHandler<T> {

    private static final String TAG = "TableHandler";

    private Class<T> clazz;
    private final String tableName;

    public TableHandler(Class<T> clazz) {
        this.clazz = clazz;
        tableName = Utils.extractTableName(clazz);
    }

    public String getTableName() {
        return tableName;
    }

    protected void onUpgrade(SQLiteDatabase db, ConnectionSource cs) throws SQLException {
        List<ColumnStruct> oldStruct = Utils.getOldTableStruct(db, tableName);
        List<ColumnStruct> newStruct = Utils.getNewTableStruct(cs, clazz);

        if (oldStruct.isEmpty() && newStruct.isEmpty()) {
            Log.d(TAG, "数据表结构都为空！不是合法的数据库bean！！！");
        } else if (oldStruct.isEmpty()) {
            Log.d(TAG, "新增表");
            create(cs);
        } else if (newStruct.isEmpty()) {
            // 永远不会执行
            Log.e(TAG, "删除表");
            drop(cs);
        } else {
            dealColumnChange(db, cs, oldStruct, newStruct);
        }
    }

    /**
     * 处理表有变化的情况
     */
    private void dealColumnChange(SQLiteDatabase db, ConnectionSource cs, List<ColumnStruct> oldStruct,
                                  List<ColumnStruct> newStruct) throws SQLException {
        if (Utils.hasChangeColumnLimit(oldStruct, newStruct)) {
            Log.d(TAG, "数据表已有字段的描述改变");
            // 已有字段描述改变了，删除旧表，重建新表
            reset(cs);
        } else {
            // 数据表已有字段的描述没有改变
            // 判断列是否有增减
            List<String> oldColumns = Utils.getColumnNames(oldStruct);
            List<String> newColumns = Utils.getColumnNames(newStruct);
            if (!oldColumns.equals(newColumns)) {
                Log.d(TAG, "表发生了变化 tableName =" + tableName + ",oldColumns = " + oldColumns + ",newColumns =" + newColumns);
                // 判断列的变化情况：增加、减少、增减
                List<String> deleteList = Utils.getDeleteColumns(oldColumns, newColumns);
                upgradeByCopy(db, cs, Utils.getCopyColumns(oldColumns, deleteList));
            } else {
                Log.i(TAG, "表没有发生变化,不需要更新数据表");
            }
        }
    }

    /**
     * 拷贝数据的方式更新
     *
     * @param columns 原始列减去删除的列
     */
    private void upgradeByCopy(SQLiteDatabase db, ConnectionSource cs, String columns) throws SQLException {
        if(TextUtils.isEmpty(columns)){
            Log.d(TAG, "upgradeByCopy columns is null");
            return;
        }
        db.beginTransaction();
        String tempTableName = tableName + "_temp";
        String sql = "ALTER TABLE " + tableName + " RENAME TO " + tempTableName;
        try {
            //rename table
            db.execSQL(sql);

            //create table
            try {
                sql = TableUtils.getCreateTableStatements(cs, clazz).get(0);
                db.execSQL(sql);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                TableUtils.createTable(cs, clazz);
            }
            sql = "INSERT INTO " + tableName + " (" + columns + ") " +
                    " SELECT " + columns + " FROM " + tempTableName;
            db.execSQL(sql);

            //drop temp table
            sql = "DROP TABLE IF EXISTS " + tempTableName;
            db.execSQL(sql);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            throw new SQLException("upgrade database table struct fail");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 数据库升级
     */
    public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) throws SQLException {
        try {
            onUpgrade(db, cs);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
            reset(cs);
        }
    }

    /**
     * 数据库降级
     */
    public void onDowngrade(ConnectionSource connectionSource, int oldVersion, int newVersion) throws SQLException {
        reset(connectionSource);
    }

    /**
     * 删除重新创建数据表
     *
     * @throws SQLException
     */
    private void reset(ConnectionSource connectionSource) throws SQLException {
        drop(connectionSource);
        create(connectionSource);
    }

    /**
     * 清除表数据
     *
     * @throws SQLException
     */
    public void clear(ConnectionSource connectionSource) throws SQLException {
        TableUtils.clearTable(connectionSource, clazz);
    }

    /**
     * 创建表
     *
     * @throws SQLException
     */
    public void create(ConnectionSource connectionSource) throws SQLException {
        TableUtils.createTable(connectionSource, clazz);
    }

    /**
     * 删除表
     *
     * @throws SQLException
     */
    public void drop(ConnectionSource connectionSource) throws SQLException {
        TableUtils.dropTable(connectionSource, clazz, true);
    }

}
