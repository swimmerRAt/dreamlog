package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

class Login {
    private final Activity activity;
    private final Runnable onLogin;

    Login(Activity activity, Runnable onLogin) {
        this.activity = activity;
        this.onLogin = onLogin;
    }

    LinearLayout createView() {
        LinearLayout content = createContentLayout();

        addTitle(content, "Dreamlog");
        addSubtitle(content, "오늘의 꿈과 감정을 안전하게 기록하세요.");

        EditText email = createInput("이메일");
        EditText password = createInput("비밀번호");
        password.setInputType(0x00000081);

        Button loginButton = createPrimaryButton("로그인");
        loginButton.setOnClickListener(view -> onLogin.run());

        TextView helper = createBodyText("계정이 없어도 둘러볼 수 있습니다. 로그인 버튼을 누르면 홈으로 이동합니다.");
        helper.setGravity(Gravity.CENTER);

        content.addView(email);
        content.addView(password);
        content.addView(loginButton);
        content.addView(helper);
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

    private EditText createInput(String hint) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        params.setMargins(0, 0, 0, dp(12));
        input.setLayoutParams(params);
        return input;
    }

    private Button createPrimaryButton(String label) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(MainActivity.BLUE);
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
