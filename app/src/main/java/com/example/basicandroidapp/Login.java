package com.example.basicandroidapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Login extends Activity {
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int HINT = Color.rgb(158, 158, 158);
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int INPUT_BG = Color.rgb(248, 249, 250);
    private static final int GOOGLE_BLUE = Color.rgb(66, 133, 244);

    private Activity hostActivity;
    private Runnable onLogin;

    public Login() {
        hostActivity = this;
    }

    Login(Activity activity, Runnable onLogin) {
        hostActivity = activity;
        this.onLogin = onLogin;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostActivity = this;
        setContentView(createScrollContent());
    }

    View createView() {
        return createScrollContent();
    }

    private View createScrollContent() {
        ScrollView scrollView = new ScrollView(hostActivity);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(245, 246, 250));

        LinearLayout root = new LinearLayout(hostActivity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 246, 250));

        root.addView(createHero());
        root.addView(createLoginCard());
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createHero() {
        LinearLayout hero = new LinearLayout(hostActivity);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(32), dp(40), dp(32), dp(24));
        hero.setBackground(heroBackground());
        hero.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(260)
        ));

        hero.addView(createLogo());
        hero.addView(text("계약서 안심이", 24, Color.WHITE, Typeface.BOLD, Gravity.CENTER, dp(10)));
        hero.addView(text("전월세 계약, 안전하게 시작하세요", 14, Color.argb(191, 255, 255, 255),
                Typeface.NORMAL, Gravity.CENTER, dp(8)));
        return hero;
    }

    private View createLogo() {
        FrameLayout outer = new FrameLayout(hostActivity);
        outer.setBackground(oval(Color.argb(38, 255, 255, 255)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(72), dp(72));
        outer.setLayoutParams(params);

        TextView inner = new TextView(hostActivity);
        inner.setText("✓");
        inner.setTextColor(Color.WHITE);
        inner.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(28));
        inner.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        inner.setGravity(Gravity.CENTER);
        inner.setBackground(oval(Color.argb(64, 255, 255, 255)));
        outer.addView(inner, new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER));
        return outer;
    }

    private View createLoginCard() {
        LinearLayout card = new LinearLayout(hostActivity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(28), dp(20), dp(28), dp(20));
        card.setBackground(roundRect(Color.WHITE, dp(16), 0, 0));
        card.setElevation(dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), -dp(20), dp(16), dp(24));
        card.setLayoutParams(params);

        card.addView(text("간편하게 시작하기", 18, TEXT, Typeface.BOLD, Gravity.CENTER, 0));
        card.addView(text("소셜 계정으로 3초만에 로그인", 13, SECONDARY, Typeface.NORMAL, Gravity.CENTER, dp(6)));
        card.addView(socialButton("G", GOOGLE_BLUE, "Google로 계속하기", Color.WHITE, TEXT, dp(24)));
        card.addView(socialButton("", Color.WHITE, "Apple로 계속하기", Color.BLACK, Color.WHITE, dp(12)));
        card.addView(divider());
        card.addView(input("이메일 주소", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, dp(16)));
        card.addView(input("비밀번호", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, dp(10)));
        card.addView(loginButton());
        card.addView(signupLink());
        card.addView(text("로그인 시 서비스 이용약관 및 개인정보처리방침에 동의합니다",
                11, HINT, Typeface.NORMAL, Gravity.CENTER, dp(32)));
        return card;
    }

    private View socialButton(String icon, int iconColor, String label, int bgColor, int textColor, int topMargin) {
        TextView button = new TextView(hostActivity);
        button.setText(icon + "   " + label);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15));
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(roundRect(bgColor, dp(12), bgColor == Color.WHITE ? BORDER : 0,
                bgColor == Color.WHITE ? Math.max(1, dp(1.5f)) : 0));

        SpannableString spannable = new SpannableString(button.getText());
        spannable.setSpan(new android.text.style.ForegroundColorSpan(iconColor), 0, icon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, icon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.AbsoluteSizeSpan((int) sp(18)), 0, icon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(spannable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, topMargin, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private View divider() {
        LinearLayout row = new LinearLayout(hostActivity);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(20), 0, 0);
        row.setLayoutParams(params);

        row.addView(line());
        TextView label = text("또는", 12, HINT, Typeface.NORMAL, Gravity.CENTER, 0);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(dp(12), 0, dp(12), 0);
        label.setLayoutParams(labelParams);
        row.addView(label);
        row.addView(line());
        return row;
    }

    private View line() {
        View line = new View(hostActivity);
        line.setBackgroundColor(BORDER);
        line.setLayoutParams(new LinearLayout.LayoutParams(0, dp(1), 1));
        return line;
    }

    private View input(String hint, int inputType, int topMargin) {
        EditText input = new EditText(hostActivity);
        input.setHint(hint);
        input.setHintTextColor(HINT);
        input.setTextColor(TEXT);
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15));
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(roundRect(INPUT_BG, dp(10), BORDER, Math.max(1, dp(1.5f))));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        params.setMargins(0, topMargin, 0, 0);
        input.setLayoutParams(params);
        return input;
    }

    private View loginButton() {
        TextView button = new TextView(hostActivity);
        button.setText("로그인");
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(16));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(roundRect(PRIMARY, dp(12), 0, 0));
        button.setOnClickListener(view -> {
            if (onLogin != null) {
                onLogin.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(16), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private View signupLink() {
        TextView link = text("계정이 없으신가요? 회원가입", 13, SECONDARY, Typeface.NORMAL, Gravity.CENTER, dp(16));
        SpannableString spannable = new SpannableString("계정이 없으신가요? 회원가입");
        int start = spannable.toString().indexOf("회원가입");
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(PRIMARY);
                ds.setTypeface(Typeface.DEFAULT_BOLD);
                ds.setUnderlineText(true);
            }
        }, start, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        link.setText(spannable);
        link.setMovementMethod(LinkMovementMethod.getInstance());
        link.setHighlightColor(Color.TRANSPARENT);
        return link;
    }

    private TextView text(String value, int size, int color, int style, int gravity, int topMargin) {
        TextView textView = new TextView(hostActivity);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setGravity(gravity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, topMargin, 0, 0);
        textView.setLayoutParams(params);
        return textView;
    }

    private GradientDrawable heroBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(PRIMARY);
        float radius = dp(40);
        drawable.setCornerRadii(new float[]{0, 0, 0, 0, radius, radius, radius, radius});
        return drawable;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                hostActivity.getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                hostActivity.getResources().getDisplayMetrics()
        );
    }
}
