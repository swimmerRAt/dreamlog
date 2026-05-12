package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MainActivity extends Activity {
    static final int BLUE = Color.rgb(37, 99, 235);
    static final int DEEP_BLUE = Color.rgb(30, 64, 175);
    static final int BACKGROUND = Color.rgb(248, 250, 252);
    static final int TEXT = Color.rgb(15, 23, 42);
    static final int MUTED = Color.rgb(100, 116, 139);
    static final int LINE = Color.rgb(226, 232, 240);

    enum Page {
        LOGIN,
        HOME,
        MY
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPage(Page.LOGIN);
    }

    void showPage(Page page) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        View pageView;
        if (page == Page.LOGIN) {
            pageView = new Login(this, () -> showPage(Page.HOME)).createView();
        } else if (page == Page.HOME) {
            pageView = new Homepage(this).createView();
        } else {
            pageView = new Mypage(this, () -> showPage(Page.LOGIN)).createView();
        }

        scrollView.addView(pageView, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scrollView);
        root.addView(createBottomNavigation(page));
        setContentView(root);
    }

    private LinearLayout createBottomNavigation(Page selectedPage) {
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(12), dp(10), dp(12), dp(10));
        navigation.setBackgroundColor(Color.WHITE);
        navigation.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button loginTab = createTabButton("로그인", selectedPage == Page.LOGIN);
        Button homeTab = createTabButton("홈", selectedPage == Page.HOME);
        Button myTab = createTabButton("마이", selectedPage == Page.MY);

        loginTab.setOnClickListener(view -> showPage(Page.LOGIN));
        homeTab.setOnClickListener(view -> showPage(Page.HOME));
        myTab.setOnClickListener(view -> showPage(Page.MY));

        navigation.addView(loginTab);
        navigation.addView(homeTab);
        navigation.addView(myTab);
        return navigation;
    }

    private Button createTabButton(String label, boolean selected) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(selected ? Color.WHITE : DEEP_BLUE);
        button.setBackgroundColor(selected ? BLUE : Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        );
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private int dp(int value) {
        return dp(this, value);
    }
}
