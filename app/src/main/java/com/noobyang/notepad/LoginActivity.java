package com.noobyang.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.noobyang.database.OrmLiteDao;
import com.noobyang.notepad.dao.User;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    public static final String START_TYPE = "START_TYPE";
    public static final String START_TYPE_LOGIN = "START_TYPE_LOGIN";
    public static final String START_TYPE_REGISTER = "START_TYPE_REGISTER";

    private EditText et_account;
    private EditText et_pwd;
    private Button btn_submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        et_account = findViewById(R.id.et_account);
        et_pwd = findViewById(R.id.et_pwd);
        btn_submit = findViewById(R.id.btn_submit);

        dealIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        dealIntent();
    }

    private void dealIntent() {
        if (START_TYPE_LOGIN.equals(getIntent().getStringExtra(START_TYPE))) {
            btn_submit.setText("登录");
        } else {
            btn_submit.setText("注册");
        }

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = et_account.getText().toString();
                String pwd = et_pwd.getText().toString();
                if (START_TYPE_LOGIN.equals(getIntent().getStringExtra(START_TYPE))) {
                    dealLogin(account, pwd);//登录
                } else {
                    dealRegister(account, pwd);//注册
                }
            }
        });
    }

    private void dealLogin(final String account, final String pwd) {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<String>() {

                @Override
                public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Exception {
                    OrmLiteDao ormLiteDao = new OrmLiteDao(LoginActivity.this, User.class, "notepad");
                    Map<String, Object> map = new HashMap<>();
                    //保存账号
                    map.put("account", account);
                    //利用账号去数据库中查询对应密码
                    User user = (User) ormLiteDao.queryForFirst(map);

                    Log.d(TAG, "subscribe " + account);
                    //密码匹配
                    if (user != null && user.getPwd().equals(pwd)) {
                        SharedPreferencesUtil.getInstance(LoginActivity.this).setAccount(account);
                        emitter.onNext(account);
                    } else {
                        emitter.onError(new Exception("error"));
                    }
                }
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String result) throws Exception {
                    Log.d(TAG, "accept " + result);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    LoginActivity.this.startActivity(intent);
                    LoginActivity.this.finish();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.e(TAG, throwable.toString());
                }
            });
    }

    private void dealRegister(final String account, final String pwd) {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<String>() {

                @Override
                public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Exception {
                    OrmLiteDao ormLiteDao = new OrmLiteDao(LoginActivity.this, User.class, "notepad");
                    User user = new User();
                    user.setAccount(account);
                    user.setPwd(pwd);
                    ormLiteDao.insert(user);
                    Log.d(TAG, "subscribe " + account);

                    SharedPreferencesUtil.getInstance(LoginActivity.this).setAccount(account);
                    emitter.onNext(account);
                }
            }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String result) throws Exception {
                    Log.d(TAG, "accept " + result);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    LoginActivity.this.startActivity(intent);
                    LoginActivity.this.finish();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.e(TAG, throwable.toString());
                }
            });

    }
}
