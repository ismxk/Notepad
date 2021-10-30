package com.noobyang.database;

/**
 * quote
 */

public class DatabaseInfo {
    /**
     * 数据库名
     */
    private String databaseName;
    /**
     * 数据库版本，默认为应用当前版本号，正式版本不用设置，测试版本可设置大于当前应用版本的版本号
     */
    private  int databaseVersion;
    /**
     * 数据库DbHelper
     */
    private DatabaseHelper helper;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(int databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public DatabaseHelper getHelper() {
        return helper;
    }

    public void setHelper(DatabaseHelper helper) {
        this.helper = helper;
    }

    @Override
    public String toString() {
        return "DatabaseInfo{" +
                "databaseName='" + databaseName + '\'' +
                ", databaseVersion=" + databaseVersion +
                '}';
    }
}
