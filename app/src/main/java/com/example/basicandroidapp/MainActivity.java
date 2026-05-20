package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    enum Page {
        LOGIN,
        CONTRACT_INPUT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPage(Page.LOGIN);
    }

    void showPage(Page page) {
        View pageView;
        if (page == Page.LOGIN) {
            pageView = new Login(this, () -> showPage(Page.CONTRACT_INPUT)).createView();
        } else {
            pageView = new ContractInput(this).createView();
        }

        pageView.setForceDarkAllowed(false);
        pageView.setAlpha(1f);
        setContentView(pageView);
    }

    static int dp(Activity activity, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    static int sp(Activity activity, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    static int color(String hex) {
        return Color.parseColor(hex);
    }
}
