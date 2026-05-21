package com.example.basicandroidapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class Mypage {
    @SuppressWarnings("java:S1313")
    private static final String BASE_URL = "http://10.0.2.2:8000";

    private static final int PRIMARY   = Color.rgb(63, 81, 181);
    private static final int TEXT      = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int BORDER    = Color.rgb(224, 224, 224);
    private static final int BG        = Color.rgb(245, 246, 250);
    private static final int DANGER    = Color.rgb(211, 47, 47);

    private final Activity activity;
    private final Runnable onLogout;
    private final OkHttpClient httpClient = new OkHttpClient();

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

        // profile: [0]=initial, [1]=name, [2]=email
        TextView[] profile = new TextView[3];
        // stats:   [0]=total,   [1]=last
        TextView[] stats = new TextView[2];

        root.addView(createPageTitle());
        root.addView(createProfileCard(profile));
        root.addView(createStatsCard(stats));
        root.addView(createLogoutButton());

        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        loadUserData(profile, stats);
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

    private View createProfileCard(TextView[] out) {
        LinearLayout card = makeCard(0);

        TextView tvInitial = new TextView(activity);
        tvInitial.setText("?");
        tvInitial.setTextColor(Color.WHITE);
        tvInitial.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(22));
        tvInitial.setTypeface(Typeface.DEFAULT_BOLD);
        tvInitial.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(PRIMARY);
        tvInitial.setBackground(circle);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(12);
        tvInitial.setLayoutParams(avatarParams);

        card.addView(sectionTitle("프로필"));
        card.addView(divider());
        card.addView(tvInitial);
        out[0] = tvInitial;
        out[1] = mutableInfoRow(card, "사용자 이름", "로딩 중...");
        out[2] = mutableInfoRow(card, "이메일", "로딩 중...");
        return card;
    }

    private View createStatsCard(TextView[] out) {
        LinearLayout card = makeCard(dp(12));
        card.addView(sectionTitle("기록 통계"));
        card.addView(divider());
        out[0] = mutableInfoRow(card, "전체 분석", "0개");
        card.addView(infoRow("즐겨찾기", "0개"));
        out[1] = mutableInfoRow(card, "마지막 분석", "기록 없음");
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

    private void loadUserData(TextView[] profile, TextView[] stats) {
        String token = getToken();
        if (token == null) return;
        new Thread(() -> loadUserInfo(token, profile)).start();
        new Thread(() -> loadHistory(token, stats)).start();
    }

    private void loadUserInfo(String token, TextView[] profile) {
        try {
            Request req = new Request.Builder()
                    .url(BASE_URL + "/auth/me")
                    .addHeader("Authorization", "Bearer " + token)
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (resp.isSuccessful()) {
                    JSONObject obj = new JSONObject(body);
                    String name = obj.optString("username", "");
                    String email = obj.optString("email", "");
                    activity.runOnUiThread(() -> {
                        profile[1].setText(name.isEmpty() ? "알 수 없음" : name);
                        profile[2].setText(email.isEmpty() ? "알 수 없음" : email);
                        if (!name.isEmpty()) {
                            profile[0].setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }
                    });
                }
            }
        } catch (IOException | org.json.JSONException ignored) { /* no-op */ }
    }

    private void loadHistory(String token, TextView[] stats) {
        try {
            Request req = new Request.Builder()
                    .url(BASE_URL + "/contract/history")
                    .addHeader("Authorization", "Bearer " + token)
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (resp.isSuccessful()) {
                    JSONArray arr = new JSONArray(body);
                    int total = arr.length();
                    String last = "기록 없음";
                    if (total > 0) {
                        String date = arr.getJSONObject(0).optString("created_at", "");
                        if (date.length() >= 10) last = date.substring(0, 10);
                    }
                    String finalLast = last;
                    activity.runOnUiThread(() -> {
                        stats[0].setText(total + "개");
                        stats[1].setText(finalLast);
                    });
                }
            }
        } catch (IOException | org.json.JSONException ignored) { /* no-op */ }
    }

    private String getToken() {
        SharedPreferences prefs = activity.getSharedPreferences(Login.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Login.KEY_TOKEN, null);
    }

    private TextView mutableInfoRow(LinearLayout parent, String label, String initial) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(activity);
        labelView.setText(label);
        labelView.setTextColor(SECONDARY);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new TextView(activity);
        valueView.setText(initial);
        valueView.setTextColor(TEXT);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14));
        valueView.setGravity(Gravity.END);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(labelView);
        row.addView(valueView);
        parent.addView(row);
        return valueView;
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
