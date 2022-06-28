package com.example.ciphererfordigitalbalance;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.ciphererfordigitalbalance.databinding.ActivityMainBinding;
import com.example.ciphererfordigitalbalance.databinding.FragmentSecondBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    Context mContext;
    SharedPreferences.Editor editor;
    MyReceiver myReceiver;
    LocalBroadcastManager localBroadcastManager;
    PrivateKey PVA;
    PublicKey PBA;
    String curpw,newpw;
    SharedPreferences settings;
    boolean isInitFrag;
    /**
     * 代表进入模式：是否显示密码模式
     * 是：显示密码，不能修改
     * 否：不显示密码，可以修改
     */
    boolean isShowPw;
    
    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext=context;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        isInitFrag=false;

        assert getArguments() != null;
        isShowPw=getArguments().getBoolean("Show",false);

        localBroadcastManager=LocalBroadcastManager.getInstance(mContext);
        myReceiver=new MyReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getExtras().getString("Done","").equals("true"))
                    done();
            }
        };
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(MyReceiver.getStr());
        localBroadcastManager.registerReceiver(myReceiver,intentFilter);
        return binding.getRoot();
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to  of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStart() {
        try {
            initFrag();
        } catch (Exception e) {
            requireActivity().finish();//TODO 异常
            e.printStackTrace();
        }
        super.onStart();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        curpw=newpw="";
        binding.btUpdate.setOnClickListener(view1 ->{
            updatePw();
        });
        binding.btGenPwProt.setOnClickListener(view1 -> {
            new AlertDialog.Builder(mContext)
                    .setTitle("注意")
                    .setMessage("密保是找回密码的重要途径。请不要随意修改密保，并注意备份到其他设备。点击确定更新密保。")
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        updatePwProt();
                    })
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .create().show();

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        localBroadcastManager.unregisterReceiver(myReceiver);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    public void initFrag() throws Exception {
        if(isInitFrag)
            return;
        isInitFrag=true;
        
        settings= mContext.getSharedPreferences("Cipherer",0);
        editor=settings.edit();
        PVA=RSAUtils.loadPrivateKey(settings.getString("pva",""));
        PBA=RSAUtils.loadPublicKey(settings.getString("pba",""));
        assert PVA!=null&&PBA!=null;
        int updateInterval=Integer.parseInt(getString(R.string.UPDATE_INTERVAL));
        binding.tvUpdateInterval.setText(String.format("%d 小时（ %.2g 天）",updateInterval,((float)updateInterval)/24.0));
        if(!settings.contains("LastUpdate")){
            //首次启动
            binding.tvLastUpdate.setText("无");
            binding.tvCurrPw.setText("无");
            binding.tvNewPw.setText("请点击更新");
            binding.etPwProt.setText("");
            binding.tvTip.setText("请更新密保");
            setEditable(true);
        }else{
            long lastUpdateTimeStamp=settings.getLong("LastUpdate",0L);
            long UPDATE_INTERVAL=Long.parseLong(getString(R.string.UPDATE_INTERVAL))*3600*1000;
            assert lastUpdateTimeStamp!=0;
            if(System.currentTimeMillis()-lastUpdateTimeStamp>=UPDATE_INTERVAL||!isShowPw)
            {
                setEditable(true);
                binding.tvCurrPw.setText("******");
            }
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            binding.tvLastUpdate.setText(formatter.format(lastUpdateTimeStamp));
            if (isShowPw) {
                binding.tvCurrPw.setText(new String(RSAUtils.decryptData(Base64Utils.decode(settings.getString("pw", "")), PVA)));
                setEditable(false);
            }
            binding.tvNewPw.setText("");
            binding.etPwProt.setText(new String(RSAUtils.decryptData(Base64Utils.decode(settings.getString("PwProt","")),PVA)));
        }

        binding.etPwProt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Snackbar.make((View) binding.getRoot().getRootView(),"密保已更改，请立即前往设置更新。",Snackbar.LENGTH_SHORT)
                        /*.setAction("复制", view12 -> {
                            ClipboardManager cbm= (ClipboardManager) mContext
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            cbm.setText(binding.etPwProt.getText());
                            Toast.makeText(mContext, "已复制密保", Toast.LENGTH_SHORT).show();
                        })*/
                        .show();
                binding.tvTip.setText("密保已更改");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void updatePw(){
        if (curpw.equals("")&&settings.contains("pw")) {
            curpw = new String(RSAUtils.decryptData(Base64Utils.decode(settings.getString("pw", "")), PVA));
            binding.tvCurrPw.setText(curpw);
        }
        newpw=String.valueOf((new Random().nextInt(900000))+100000);
        binding.tvNewPw.setText(newpw);
        Toast.makeText(mContext, "密码已更新", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("DefaultLocale")
    public void updatePwProt(){
/*        int PW_PROT_LEN=32;
        binding.etPwProt.setText(generatePwProt(PW_PROT_LEN));
        binding.etPwProt.requestFocus();
        binding.etPwProt.selectAll();*/
        binding.etPwProt.setText(String.format("%d %d",(new Random().nextInt(900000))+100000,
                (new Random().nextInt(900000))+100000));
    }

    public void done(){
        if(isShowPw) {
            requireActivity().finish();
            return;
        }
        try {
            assert !(newpw.equals("") || binding.tvTip.getText().equals(""));
            editor.putString("pw", Base64Utils.encode(
                    Objects.requireNonNull(RSAUtils.encryptData(newpw.getBytes(), PBA))));
            editor.putString("PwProt",Base64Utils.encode(
                    Objects.requireNonNull(RSAUtils.encryptData(binding.etPwProt.getText().toString().getBytes(), PBA))));
            editor.putLong("LastUpdate", System.currentTimeMillis());
            editor.apply();
//        NavHostFragment.findNavController(SecondFragment.this)
//                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            Toast.makeText(mContext, "已保存", Toast.LENGTH_SHORT).show();
        }catch (AssertionError assertionError){
            Toast.makeText(mContext, "保存失败", Toast.LENGTH_LONG).show();
        }
        requireActivity().finish();
    }
/*
    public String generatePwProt(int len){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random1=new Random();
        StringBuilder sb=new StringBuilder();
        for (int i = 0; i < len; i++)
            sb.append(str.charAt(random1.nextInt(str.length())));
        return sb.toString();
    }*/

    private void setEditable(boolean isEditable){
        if(isEditable){
            binding.btUpdate.setEnabled(true);
            binding.btGenPwProt.setEnabled(true);
        }else {
            binding.btGenPwProt.setEnabled(false);
            binding.etPwProt.setKeyListener(null);
            binding.etPwProt.setTextIsSelectable(true);
        }
    }

}