package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

class Mypage {
    private static final int PRIMARY   = Color.rgb(63, 81, 181);
    private static final int TEXT      = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int BORDER    = Color.rgb(224, 224, 224);
    private static final int BG        = Color.rgb(245, 246, 250);
    private static final int DANGER    = Color.rgb(211, 47, 47);

    private final Activity activity;
    private final Runnable onLogout;

    Mypage(Activity activity, Runnable onLogout) {
        this.activity = activity;
        this.onLogout = onLogout;
    }

    View createView() {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(BG);

        root.addView(createPageTitle());
        root.addView(createProfileCard());
        root.addView(createStatsCard());
        root.addView(createLogoutButton());

        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createPageTitle() {
        TextView title = new TextView(activity);
        title.setText("마이 페이지");
        title.setTextColor(TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(22));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(4), dp(8), 0, dp(16));
        title.setLayoutParams(params);
        return title;
    }

    private View createProfileCard() {
        LinearLayout card = makeCard(0);

        TextView avatar = new TextView(activity);
        avatar.setText("D");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(22));
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(PRIMARY);
        avatar.setBackground(circle);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(12);
        avatar.setLayoutParams(avatarParams);

        card.addView(sectionTitle("프로필"));
        card.addView(divider());
        card.addView(avatar);
        card.addView(infoRow("사용자 이름", "Dreamer"));
        card.addView(infoRow("이메일", "dreamer@example.com"));
        return card;
    }

    private View createStatsCard() {
        LinearLayout card = makeCard(dp(12));
        card.addView(sectionTitle("기록 통계"));
        card.addView(divider());
        card.addView(infoRow("전체 기록", "0개"));
        card.addView(infoRow("즐겨찾기", "0개"));
        card.addView(infoRow("마지막 작성", "기록 없음"));
        return card;
    }

    private View createLogoutButton() {
        TextView button = new TextView(activity);
        button.setText("로그아웃");
        button.setTextColor(DANGER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15));
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), DANGER);
        button.setBackground(bg);
        button.setOnClickListener(v -> {
            if (onLogout != null) onLogout.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        );
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout makeCard(int topMargin) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = topMargin;
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(TEXT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(16));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private View divider() {
        View line = new View(activity);
        line.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        );
        params.setMargins(0, dp(10), 0, dp(10));
        line.setLayoutParams(params);
        return line;
    }

    private View infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(activity);
        labelView.setText(label);
        labelView.setTextColor(SECONDARY);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new TextView(activity);
        valueView.setText(value);
        valueView.setTextColor(TEXT);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14));
        valueView.setGravity(Gravity.END);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(labelView);
        row.addView(valueView);
        return row;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, activity.getResources().getDisplayMetrics());
    }
}
