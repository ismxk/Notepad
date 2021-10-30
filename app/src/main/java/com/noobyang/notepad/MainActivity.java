package com.noobyang.notepad;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noobyang.database.OrmLiteDao;
import com.noobyang.notepad.dao.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private RelativeLayout rl_content;
    private RecyclerView recycler_view;
    private MessageAdapter messageAdapter;
    private RelativeLayout rl_left_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer_layout);

        rl_content = findViewById(R.id.rl_content);
        findViewById(R.id.iv_person_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(rl_left_menu, true);
            }
        });
        //初始化RecyclerView
        recycler_view = findViewById(R.id.recycler_view);
        recycler_view.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this);
        recycler_view.setAdapter(messageAdapter);
        findViewById(R.id.iv_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dealAdd();
            }
        });

        rl_left_menu = findViewById(R.id.rl_left_menu);
        TextView tv_account = findViewById(R.id.tv_account);
        //显示账户名
        tv_account.setText(SharedPreferencesUtil.getInstance(MainActivity.this).getAccount());
        findViewById(R.id.btn_clear_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(rl_left_menu, true);
                clearData();
            }
        });
        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(rl_left_menu, true);
                //将当前信息置空，下次进入时需要登录
                SharedPreferencesUtil.getInstance(MainActivity.this).setAccount("");
                MainActivity.this.startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                MainActivity.this.finish();
            }
        });

        initData();
    }

    private void initData() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<List<Message>>() {

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<List<Message>> emitter) throws Exception {
                String account = SharedPreferencesUtil.getInstance(MainActivity.this).getAccount();
                Log.d(TAG, "subscribe " + account);
                if (TextUtils.isEmpty(account)) {
                    emitter.onNext(new ArrayList<Message>());
                } else {
                    OrmLiteDao ormLiteDao = new OrmLiteDao(MainActivity.this, Message.class, "notepad");
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("account", account);
                    List<Message> data = ormLiteDao.queryByColumnName(map);
                    emitter.onNext(data);
                }
            }
            }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Message>>() {
                    @Override
                    public void accept(List<Message> result) throws Exception {
                        Log.d(TAG, "accept " + result);
                        messageAdapter.setData(result);
                        messageAdapter.notifyDataSetChanged();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.toString());
                    }
                });
    }

    private void dealAdd() {
        final Dialog dialog = new Dialog(this, R.style.DialogTheme);
        //将布局放入dialog
        dialog.setContentView(R.layout.dialog_add_message);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = displayMetrics.widthPixels;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(layoutParams);
        dialog.show();

        final EditText et_content = dialog.findViewById(R.id.et_content);
        dialog.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   dialog.dismiss();
               }
           }
        );
        dialog.findViewById(R.id.tv_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = et_content.getText().toString();
                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(MainActivity.this, "输入内容为空", Toast.LENGTH_SHORT).show();
                } else {
                    dialog.dismiss();
                    Message message = new Message();
                    message.setContent(content);
                    message.setTimestamp(System.currentTimeMillis());
                    addMessage(message);
                }
            }
        });
    }

    private void addMessage(final Message message) {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Message>() {

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Message> emitter) throws Exception {
                String account = SharedPreferencesUtil.getInstance(MainActivity.this).getAccount();
                Log.d(TAG, "subscribe " + account);
                if (TextUtils.isEmpty(account)) {
                    emitter.onNext(null);
                } else {
                    OrmLiteDao ormLiteDao = new OrmLiteDao(MainActivity.this, Message.class, "notepad");
                    message.setAccount(account);
                    ormLiteDao.insert(message);
                    emitter.onNext(message);
                }
            }
            }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Message>() {
                    @Override
                    public void accept(Message result) throws Exception {
                        Log.d(TAG, "accept " + result);
                        if (result != null) {
                            messageAdapter.addData(result);
                            messageAdapter.notifyDataSetChanged();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.toString());
                    }
                });
    }

    private void clearData() {
        Disposable disposable = Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<String> emitter) throws Exception {
                String account = SharedPreferencesUtil.getInstance(MainActivity.this).getAccount();
                Log.d(TAG, "subscribe " + account);
                OrmLiteDao ormLiteDao = new OrmLiteDao(MainActivity.this, Message.class, "notepad");
                HashMap<String, Object> map = new HashMap<>();
                map.put("account", account);
                ormLiteDao.deleteByColumnName(map);
                emitter.onNext("");
            }
            }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String result) throws Exception {
                        Log.d(TAG, "accept " + result);
                        messageAdapter.addData(null);
                        messageAdapter.notifyDataSetChanged();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.toString());
                    }
                });
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder> {

        private Context context;
        private List<Message> data;

        public MessageAdapter(Context context) {
            this.context = context;
        }

        public void setData(List<Message> data) {
            this.data = data;
        }

        public void addData(Message message) {
            if (data == null) {
                data = new ArrayList<>();
                data.add(message);
            } else {
                data.add(0, message);
            }
        }

        @NonNull
        @Override
        public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageHolder(LayoutInflater.from(context).inflate(R.layout.item_message, null));
        }

        @Override
        public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
            final Message bean = data.get(position);
            holder.tv_content.setText(bean.getContent());
            if (bean.getStatus().equals("1")) {
                holder.checkbox.setChecked(false);
            } else {
                holder.checkbox.setChecked(true);
            }
            holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && bean.getStatus().equals("1")) {
                        bean.setStatus("2");
                    } else if (!isChecked && bean.getStatus().equals("2")) {
                        bean.setStatus("1");
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            OrmLiteDao ormLiteDao = new OrmLiteDao(context, Message.class, "notepad");
                            ormLiteDao.update(bean);
                        }
                    }.start();
                }
            });
        }

        @Override
        public int getItemCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }

        private class MessageHolder extends RecyclerView.ViewHolder {

            CheckBox checkbox;
            TextView tv_content;

            public MessageHolder(@NonNull View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.checkbox);
                tv_content = itemView.findViewById(R.id.tv_content);
            }
        }

    }

}
