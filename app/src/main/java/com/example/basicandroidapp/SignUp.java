package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUp {
    @SuppressWarnings("java:S1313")
    private static final String BASE_URL = "http://10.0.2.2:8000";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();

    private static final int PRIMARY  = Color.rgb(63, 81, 181);
    private static final int TEXT     = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int HINT     = Color.rgb(158, 158, 158);
    private static final int BORDER   = Color.rgb(224, 224, 224);
    private static final int INPUT_BG = Color.rgb(248, 249, 250);

    private final Activity activity;
    private final Runnable onBackToLogin;

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private EditText editPasswordConfirm;

    SignUp(Activity activity, Runnable onBackToLogin) {
        this.activity = activity;
        this.onBackToLogin = onBackToLogin;
    }

    View createView() {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(245, 246, 250));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 246, 250));

        root.addView(createHeader());
        root.addView(createFormCard());

        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(32), dp(40), dp(32), dp(24));
        header.setBackground(heroBackground());
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(200)
        ));

        header.addView(text("회원가입", 24, Color.WHITE, Typeface.BOLD, Gravity.CENTER, 0));
        header.addView(text("계약서 안심이와 함께 시작하세요", 14,
                Color.argb(191, 255, 255, 255), Typeface.NORMAL, Gravity.CENTER, dp(8)));
        return header;
    }

    private View createFormCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(28), dp(20), dp(28), dp(32));
        card.setBackground(roundRect(Color.WHITE, dp(16)));
        card.setElevation(dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), -dp(20), dp(16), dp(24));
        card.setLayoutParams(params);

        card.addView(text("정보를 입력해주세요", 18, TEXT, Typeface.BOLD, Gravity.CENTER, 0));

        editName = input("이름", InputType.TYPE_CLASS_TEXT, dp(20));
        editEmail = input("이메일 주소", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, dp(10));
        editPassword = input("비밀번호 (6자 이상)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, dp(10));
        editPasswordConfirm = input("비밀번호 확인", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, dp(10));

        card.addView(editName);
        card.addView(editEmail);
        card.addView(editPassword);
        card.addView(editPasswordConfirm);
        card.addView(signupButton());
        card.addView(loginLink());
        return card;
    }

    private View signupButton() {
        TextView button = new TextView(activity);
        button.setText("가입하기");
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(16));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(roundRect(PRIMARY, dp(12)));
        button.setOnClickListener(v -> {
            String name     = editName.getText().toString().trim();
            String email    = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString();
            String confirm  = editPasswordConfirm.getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(activity, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                editName.requestFocus();
                return;
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(activity, "올바른 이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
                editEmail.requestFocus();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(activity, "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show();
                editPassword.requestFocus();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(activity, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                editPasswordConfirm.requestFocus();
                return;
            }
            performSignup(name, email, password);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        );
        params.setMargins(0, dp(20), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void performSignup(String name, String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", name);
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/auth/register")
                    .post(RequestBody.create(body.toString(), JSON_TYPE))
                    .build();

            new Thread(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "가입이 완료됐습니다. 로그인해주세요", Toast.LENGTH_SHORT).show();
                            if (onBackToLogin != null) onBackToLogin.run();
                        });
                    } else {
                        String message = parseError(responseBody);
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
                    }
                } catch (IOException e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "서버에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(activity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String parseError(String responseBody) {
        try {
            JSONObject err = new JSONObject(responseBody);
            if (err.has("detail")) return err.getString("detail");
        } catch (Exception ignored) { /* JSON 파싱 실패 시 기본 메시지 사용 */ }
        return "가입에 실패했습니다.";
    }

    private View loginLink() {
        TextView link = new TextView(activity);
        link.setText("이미 계정이 있으신가요? 로그인");
        link.setTextColor(SECONDARY);
        link.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13));
        link.setGravity(Gravity.CENTER);
        link.setClickable(true);
        link.setFocusable(true);
        link.setOnClickListener(v -> {
            if (onBackToLogin != null) onBackToLogin.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(16), 0, 0);
        link.setLayoutParams(params);
        return link;
    }

    private EditText input(String hint, int inputType, int topMargin) {
        EditText et = new EditText(activity);
        et.setHint(hint);
        et.setHintTextColor(HINT);
        et.setTextColor(TEXT);
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15));
        et.setSingleLine(true);
        et.setInputType(inputType);
        et.setPadding(dp(14), 0, dp(14), 0);
        et.setBackground(roundRect2(INPUT_BG, dp(10), BORDER, Math.max(1, dp(1.5f))));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        );
        params.setMargins(0, topMargin, 0, 0);
        et.setLayoutParams(params);
        return et;
    }

    private TextView text(String value, int size, int color, int style, int gravity, int topMargin) {
        TextView tv = new TextView(activity);
        tv.setText(value);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setGravity(gravity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, topMargin, 0, 0);
        tv.setLayoutParams(params);
        return tv;
    }

    private GradientDrawable heroBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(PRIMARY);
        float r = dp(40);
        d.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        return d;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable roundRect2(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
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
