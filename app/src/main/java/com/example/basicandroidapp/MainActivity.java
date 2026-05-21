package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int MUTED   = Color.rgb(158, 158, 158);
    private static final int BORDER  = Color.rgb(224, 224, 224);

    enum Page {
        LOGIN,
        SIGNUP,
        CONTRACT_INPUT,
        MYPAGE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPage(Page.LOGIN);
    }

    void showPage(Page page) {
        View pageView;
        if (page == Page.LOGIN) {
            pageView = new Login(this,
                    () -> showPage(Page.CONTRACT_INPUT),
                    () -> showPage(Page.SIGNUP)
            ).createView();
        } else if (page == Page.SIGNUP) {
            pageView = new SignUp(this, () -> showPage(Page.LOGIN)).createView();
        } else if (page == Page.CONTRACT_INPUT) {
            pageView = wrapWithBottomNav(new ContractInput(this).createView(), page);
        } else {
            pageView = wrapWithBottomNav(new Mypage(this, () -> showPage(Page.LOGIN)).createView(), page);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            pageView.setForceDarkAllowed(false);
        }
        setContentView(pageView);
    }

    private View wrapWithBottomNav(View content, Page activePage) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        wrapper.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        View separator = new View(this);
        separator.setBackgroundColor(BORDER);
        wrapper.addView(separator, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this, 1)
        ));

        wrapper.addView(createBottomNav(activePage));
        return wrapper;
    }

    private View createBottomNav(Page activePage) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this, 60)
        ));

        nav.addView(navItem("계약서 입력", activePage == Page.CONTRACT_INPUT,
                () -> showPage(Page.CONTRACT_INPUT)));
        nav.addView(navItem("마이 페이지", activePage == Page.MYPAGE,
                () -> showPage(Page.MYPAGE)));
        return nav;
    }

    private View navItem(String label, boolean active, Runnable onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> onClick.run());
        item.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f
        ));

        if (active) {
            View indicator = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setColor(PRIMARY);
            d.setCornerRadius(dp(this, 2));
            indicator.setBackground(d);
            LinearLayout.LayoutParams indParams = new LinearLayout.LayoutParams(dp(this, 24), dp(this, 3));
            indParams.bottomMargin = dp(this, 2);
            indicator.setLayoutParams(indParams);
            item.addView(indicator);
        }

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(active ? PRIMARY : MUTED);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(this, 12));
        labelView.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelView.setGravity(Gravity.CENTER);
        item.addView(labelView);

        return item;
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
