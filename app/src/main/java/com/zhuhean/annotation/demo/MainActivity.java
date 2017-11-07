package com.zhuhean.annotation.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zhuhean.annotation.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        User me = new UserBuilder()
                .firstName("HeAn")
                .lastName("Zhu")
                .nickName("violet")
                .age(22)
                .build();
    }

}
