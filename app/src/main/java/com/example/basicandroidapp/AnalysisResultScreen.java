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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AnalysisResultScreen extends Activity {
    private static final int BACKGROUND = Color.rgb(248, 249, 250);
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int SAFE_BG = Color.rgb(232, 245, 233);
    private static final int SAFE = Color.rgb(46, 125, 50);
    private static final int WARN_BG = Color.rgb(255, 248, 225);
    private static final int WARN = Color.rgb(245, 124, 0);
    private static final int DANGER_BG = Color.rgb(255, 235, 238);
    private static final int DANGER = Color.rgb(198, 40, 40);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createView());
    }

    private View createView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        root.addView(createTopBar());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(content);

        content.addView(createScoreCard());
        content.addView(createFindingCard("주의", "보증금 보호 확인 필요", "등기부등본의 선순위 권리와 보증금 규모를 함께 확인하세요.", WARN_BG, WARN));
        content.addView(createFindingCard("안전", "수리비 부담 조항 명확", "주요 설비 하자는 임대인이 부담하도록 작성되어 있습니다.", SAFE_BG, SAFE));
        content.addView(createFindingCard("위험", "근저당권 설정 제한 문구 보완", "계약 후 추가 담보 설정을 제한하는 특약을 더 구체화하는 것이 좋습니다.", DANGER_BG, DANGER));

        root.addView(scrollView);
        return root;
    }

    private View createTopBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(TEXT);
        back.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(32));
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setOnClickListener(view -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        TextView title = new TextView(this);
        title.setText("분석 결과");
        title.setTextColor(TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(17));
        title.setTypeface(typeface(Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return bar;
    }

    private View createScoreCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackground(roundRect(Color.WHITE, dp(12), 0, 0));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        TextView label = text("계약 안전 점수", SECONDARY, 13, Typeface.NORMAL);
        label.setGravity(Gravity.CENTER);
        card.addView(label);

        TextView score = text("82점", PRIMARY, 40, Typeface.BOLD);
        score.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scoreParams.setMargins(0, dp(6), 0, 0);
        score.setLayoutParams(scoreParams);
        card.addView(score);

        TextView summary = text("대체로 안전하지만 보증금 보호와 특약 문구를 한 번 더 확인하세요.", TEXT, 14, Typeface.NORMAL);
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(dp(5), 1.0f);
        card.addView(summary);
        return card;
    }

    private View createFindingCard(String badge, String title, String body, int badgeBg, int badgeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundRect(Color.WHITE, dp(12), BORDER, dp(1)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView badgeView = text(badge, badgeColor, 12, Typeface.BOLD);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(10), dp(4), dp(10), dp(4));
        badgeView.setBackground(roundRect(badgeBg, dp(12), 0, 0));
        card.addView(badgeView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView titleView = text(title, TEXT, 16, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(10), 0, 0);
        titleView.setLayoutParams(titleParams);
        card.addView(titleView);

        TextView bodyView = text(body, SECONDARY, 13, Typeface.NORMAL);
        bodyView.setLineSpacing(dp(5), 1.0f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.setMargins(0, dp(6), 0, 0);
        bodyView.setLayoutParams(bodyParams);
        card.addView(bodyView);
        return card;
    }

    private TextView text(String value, int color, int size, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        textView.setTypeface(typeface(style));
        return textView;
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

    private Typeface typeface(int style) {
        return Typeface.create("sans-serif", style);
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
