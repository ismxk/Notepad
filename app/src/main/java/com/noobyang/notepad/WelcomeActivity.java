package com.noobyang.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                //将要传递的值附加到Intent对象(字典型)
                intent.putExtra(LoginActivity.START_TYPE, LoginActivity.START_TYPE_LOGIN);
                WelcomeActivity.this.startActivity(intent);//启动该Intent对象，实现跳转
                WelcomeActivity.this.finish();
            }
        });

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                intent.putExtra(LoginActivity.START_TYPE, LoginActivity.START_TYPE_REGISTER);
                WelcomeActivity.this.startActivity(intent);
                WelcomeActivity.this.finish();
            }
        });

        if (!TextUtils.isEmpty(SharedPreferencesUtil.getInstance(this).getAccount())) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.putExtra(LoginActivity.START_TYPE, LoginActivity.START_TYPE_LOGIN);
            WelcomeActivity.this.startActivity(intent);
            WelcomeActivity.this.finish();
        }
    }

}
