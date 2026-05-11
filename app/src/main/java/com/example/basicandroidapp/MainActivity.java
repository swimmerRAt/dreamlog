package com.example.basicandroidapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setText(R.string.welcome_message);
        textView.setTextSize(24);

        setContentView(textView);
    }
}
