package com.example.ciphererfordigitalbalance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteAbortException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.telephony.TelephonyManager;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.ciphererfordigitalbalance.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.PublicKey;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    LocalBroadcastManager localBroadcastManager;
    MyReceiver myReceiver;
    boolean isLogin;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myReceiver=new MyReceiver(){
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onReceive(Context context, Intent intent) {
                String str=intent.getExtras().getString("Login","");
                if(str.equals("true")) {
                    binding.fab.setImageDrawable(getDrawable(R.drawable.checkmark_outline));
                    binding.fab.setOnClickListener(view -> {
                        sendDone();
                    });
                    isLogin=true;
                } else if(str.equals("false")) {
                    binding.fab.setImageDrawable(getDrawable(R.drawable.mail_outline));
                    isLogin=false;
                }
            }
        };
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(MyReceiver.getStr());
        localBroadcastManager=LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(myReceiver,intentFilter);

//        getSharedPreferences("Cipherer",0).edit().clear().apply();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_SMS
        }, 1);//回调initApp()

        isLogin=false;


//        initApp();
    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(myReceiver);
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.itemAbout) {
            new AlertDialog.Builder(this)
                    .setTitle("Cipherer")
                    .setMessage(String.format(getString(R.string.text_about), BuildConfig.VERSION_NAME))
                    .create().show();
            return true;
        }else if(id==R.id.itemPass)
            pass();//DEBUG
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. The {@link #getOnBackPressedDispatcher() OnBackPressedDispatcher} will be given a
     * chance to handle the back button before the default behavior of
     * {@link Activity#onBackPressed()} is invoked.
     *
     * @see #getOnBackPressedDispatcher()
     */
    @Override
    public void onBackPressed() {
        if(isLogin)
            sendDone();
        else
            finish();
//        super.onBackPressed();
    }
/*
    public boolean checkPermissions(boolean isFirstEntry){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if(!isFirstEntry)
                ;
            return false;
        }
        return true;
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        for(int i :grantResults)
            if(i!=PackageManager.PERMISSION_GRANTED)
            {
                new AlertDialog.Builder(this)
                        .setTitle("权限错误")
                        .setMessage("请转到设置并授予权限或重新打开app。").setOnDismissListener(dialogInterface -> {
                            finish();
                        })
                        .create().show();
                return;
            }
        initApp();
        createView();

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("DefaultLocale")
    private void createView(){
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> {
            Snackbar snackbar= Snackbar.make(view, "正在查询短信...", Snackbar.LENGTH_SHORT);
            snackbar.show();
            String ret=readSms();
            if(!ret.equals("")){
                new AlertDialog.Builder(this)
                        .setTitle("收到请求")
                        .setPositiveButton("解锁", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setMessage(String.format("你的请求为：\n%s\n\n请求%d分钟内有效",ret,Integer.parseInt(getString(R.string.SMS_VALID_INTERVAL))))
                        .setOnDismissListener(dialogInterface -> pass()).create().show();

            }
            else {
                snackbar.dismiss();
                Snackbar.make(view, "无结果", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void initApp() {
        SharedPreferences settings = getSharedPreferences("Cipherer", 0);
        if (settings.contains("Entered"))
            return;
//        if(!checkPermissions(true))
//            return;

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("tel", tm.getLine1Number());

        KeyPair keyPair = RSAUtils.generateRSAKeyPair(1024);
        assert keyPair != null;
        editor.putString("pva",Base64Utils.encode(keyPair.getPrivate().getEncoded()));
        editor.putString("pba",Base64Utils.encode(keyPair.getPublic().getEncoded()));

        editor.putBoolean("Entered", true);
//        editor.clear();
        editor.apply();
        pass();
    }

    public String readSms(){
        final String SMS_URI_INBOX = "content://sms/inbox"; // 收件箱
        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "person",
                    "body", "date", "type",};
            Cursor cur = getContentResolver().query(uri, projection, null,
                    null, "date desc"); // 获取手机内部短信
            // 获取短信中最新的未读短信
            // Cursor cur = getContentResolver().query(uri, projection,
            // "read = ?", new String[]{"0"}, "date desc");
            if (cur.moveToFirst()) {
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                do {
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    if(System.currentTimeMillis()-longDate>Long.parseLong(getString(R.string.SMS_VALID_INTERVAL))*60*1000)
                        break;
                    if(strbody.matches(getString(R.string.SMS_Pattern)))
                        return strbody;


                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                }
            } else {
                Toast.makeText(this, "短信查询失败", Toast.LENGTH_SHORT).show();
            }
        }catch (SQLiteException e) {
            e.printStackTrace();
            Toast.makeText(this, "短信查询失败 (SQLiteException)", Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    /**
     * 首次启动；短信通过<br>
     * To 1st
     */
    private void pass(){
        Intent intent=new Intent(MyReceiver.getStr());
        intent.putExtra("Pass","true");
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * fab点击完成；点击返回<br>
     * To 2nd
     */
    private void sendDone(){
        Intent intent=new Intent(MyReceiver.getStr());
        intent.putExtra("Done","true");
        localBroadcastManager.sendBroadcast(intent);
    }
}