package com.example.ciphererfordigitalbalance;

import android.annotation.SuppressLint;

import java.util.Random;

public class QuizUtils {
    int answer;
    @SuppressLint("DefaultLocale")
    public String nextQuestion(){
        Random random=new Random();
        int a,b;
        a=random.nextInt(899)+101;
        b= random.nextInt(89)+11;
        answer=a*b;
        return String.format("%d × %d = " ,a,b);
    }

    public String getAnswer() {
        return String.valueOf(answer);
    }
}
