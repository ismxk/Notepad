package com.noobyang.notepad;

import android.app.Application;
import android.content.Context;

import com.noobyang.database.DatabaseHelper;
import com.noobyang.notepad.dao.Message;
import com.noobyang.notepad.dao.User;

public class MainApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDatabase();
    }

    private void initDatabase() {
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getApplicationContext(), "notepad");
        databaseHelper.registerTable(User.class);
        databaseHelper.registerTable(Message.class);
    }

}
