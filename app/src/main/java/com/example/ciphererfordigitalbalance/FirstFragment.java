package com.example.ciphererfordigitalbalance;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.ciphererfordigitalbalance.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    LocalBroadcastManager localBroadcastManager;
    MyReceiver myReceiver;
    private Context mContext;
    QuizUtils quizUtils;
    boolean isStart;
    int right,count,total,timeLimit;
    String countFormat;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        mContext= binding.getRoot().getContext();
        localBroadcastManager=LocalBroadcastManager.getInstance(mContext);
        quizUtils=new QuizUtils();
        isStart=false;
        right= 0;
        countFormat="%d/%d  正确率: %.1f";
        total=5;//20道题
        timeLimit=1;//15分钟

        SharedPreferences settings=mContext.getSharedPreferences("Cipherer", 0);
        long lastUpdateTimeStamp=settings.getLong("LastUpdate",0L);
        long UPDATE_INTERVAL=Long.parseLong(getString(R.string.UPDATE_INTERVAL))*3600*1000;
        if(System.currentTimeMillis()-lastUpdateTimeStamp>=UPDATE_INTERVAL)
            login(false);
        myReceiver=new MyReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getExtras().getString("Pass","").equals("true"))
                    login(true);
            }
        };
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(MyReceiver.getStr());
        localBroadcastManager.registerReceiver(myReceiver,intentFilter);

        return binding.getRoot();

    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to  of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        Intent intent=new Intent(MyReceiver.getStr());
        intent.putExtra("Login","false");
        localBroadcastManager.sendBroadcast(intent);
        super.onResume();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cmTimer.setCountDown(true);
        binding.cmTimer.setBase(SystemClock.elapsedRealtime()+timeLimit*60*1000);
        binding.tvQuestion.setText(quizUtils.nextQuestion());
        binding.etAnswer.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        final int total = FirstFragment.this.total;
        binding.tvCount.setText(String.format(countFormat,count, total,0f));
        binding.cmTimer.setOnChronometerTickListener(chronometer -> {
            String str= (String) chronometer.getText();
            if(str.equals("00:00")||str.contains("−")){
                chronometer.setText("00:00");
                chronometer.stop();
                float rate=(float)right/total;
                final float passRate=0.9f;//正确率0.9
                boolean isPass=(rate>=passRate)&&(count==total);
                new AlertDialog.Builder(mContext).setTitle("时间到")
                        .setMessage(String.format("完成了 %d 道题，正确率为 %.1f 。%s",count,rate,
                                isPass?"满足解锁条件，点击确定解锁":"不满足解锁条件，请重新打开应用重试。"))
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            if(isPass)
                                login(true);
                            else
                                requireActivity().finish();
                        })
                        .create().show();
            }
        });
        binding.btNext.setOnClickListener(view1 -> next());
        binding.etAnswer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!isStart){
                    isStart=true;
                    binding.cmTimer.setBase(SystemClock.elapsedRealtime()+timeLimit*60*1000);
                    binding.cmTimer.start();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        binding.etAnswer.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i==EditorInfo.IME_ACTION_NEXT)
                next();
            return false;
        });

        binding.tvSmsTip.setOnClickListener(view12 -> {
            String tel =mContext.getSharedPreferences("Cipherer", 0).getString("tel","");
            new AlertDialog.Builder(mContext).setTitle("提示")
                    .setMessage(String.format("发送以下短信到本机(%s)：\n\n" +
                            "%s<此处写你的你解锁的意图，可不带括号>\n\n" +
                            "意图必填，否则无法解锁。点击右下角的信息按钮检查收件箱。",tel,getString(R.string.SMS_TEMPLATE)))
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    }).create().show();
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        localBroadcastManager.unregisterReceiver(myReceiver);
    }

    private void next(){
        if(count>=total)
            return;
        if(binding.etAnswer.getText().toString().equals("")){
            Toast.makeText(mContext, "请输入答案", Toast.LENGTH_SHORT).show();
            return;
        }
        if(binding.etAnswer.getText().toString().equals(quizUtils.getAnswer()))
            binding.tvRight.setText(String.valueOf(++right));
        else
            binding.tvWrong.setText(String.valueOf(count-right+1));
        binding.tvCount.setText(String.format(countFormat,++count, total,((float)right)/ total));
        if(count>=total){
            binding.btNext.setEnabled(false);
            Toast.makeText(mContext,"您已全部完成，请耐心等待计时结束。",Toast.LENGTH_LONG).show();
        }
        binding.etAnswer.setText("");
        binding.tvQuestion.setText(quizUtils.nextQuestion());
    }

    /**
     * 过期更新，收到Pass信号，show调用<br>
     * 发送登录信号：调整fab<br>
     * 允许登录<br>
     * To Main
     * @param isShow
     */
    private void login(boolean isShow){
        Bundle bundle=new Bundle();
        bundle.putBoolean("Show",isShow);
        Intent intent=new Intent(MyReceiver.getStr());
        intent.putExtra("Login","true");
        localBroadcastManager.sendBroadcast(intent);
        NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);
    }
}