package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

class Mypage {
    private final Activity activity;
    private final Runnable onLogout;

    Mypage(Activity activity, Runnable onLogout) {
        this.activity = activity;
        this.onLogout = onLogout;
    }

    LinearLayout createView() {
        LinearLayout content = createContentLayout();

        addTitle(content, "마이 페이지");
        addSubtitle(content, "내 기록 설정과 프로필 정보를 관리합니다.");
        addInfoPanel(content, "프로필", "사용자 이름: Dreamer\n이메일: dreamer@example.com");
        addInfoPanel(content, "기록 통계", "전체 기록 0개\n즐겨찾기 0개\n마지막 작성 기록 없음");

        Button logoutButton = createSecondaryButton("로그인 페이지로 이동");
        logoutButton.setOnClickListener(view -> onLogout.run());
        content.addView(logoutButton);
        return content;
    }

    private LinearLayout createContentLayout() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(28), dp(24), dp(28));
        return content;
    }

    private void addTitle(LinearLayout content, String title) {
        TextView titleView = new TextView(activity);
        titleView.setText(title);
        titleView.setTextColor(MainActivity.TEXT);
        titleView.setTextSize(32);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(null, 1);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.addView(titleView);
    }

    private void addSubtitle(LinearLayout content, String subtitle) {
        TextView subtitleView = createBodyText(subtitle);
        subtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, dp(28));
        subtitleView.setLayoutParams(params);
        content.addView(subtitleView);
    }

    private void addInfoPanel(LinearLayout content, String title, String body) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        panel.setLayoutParams(params);

        TextView titleView = new TextView(activity);
        titleView.setText(title);
        titleView.setTextColor(MainActivity.TEXT);
        titleView.setTextSize(18);
        titleView.setTypeface(null, 1);

        View divider = new View(activity);
        divider.setBackgroundColor(MainActivity.LINE);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.setMargins(0, dp(10), 0, dp(10));
        divider.setLayoutParams(dividerParams);

        panel.addView(titleView);
        panel.addView(divider);
        panel.addView(createBodyText(body));
        content.addView(panel);
    }

    private Button createSecondaryButton(String label) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(MainActivity.DEEP_BLUE);
        button.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(4), 0, dp(16));
        button.setLayoutParams(params);
        return button;
    }

    private TextView createBodyText(String text) {
        TextView textView = new TextView(activity);
        textView.setText(text);
        textView.setTextColor(MainActivity.MUTED);
        textView.setTextSize(15);
        textView.setLineSpacing(dp(3), 1.0f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return textView;
    }

    private int dp(int value) {
        return MainActivity.dp(activity, value);
    }
}
