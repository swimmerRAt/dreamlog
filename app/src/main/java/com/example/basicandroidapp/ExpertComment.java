package com.example.basicandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Toast;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ExpertComment extends Activity {
    static final String EXTRA_DANGER_COUNT  = "extra_danger_count";
    static final String EXTRA_CAUTION_COUNT = "extra_caution_count";

    private static final int BACKGROUND = Color.rgb(248, 249, 250);
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int CARD = Color.WHITE;
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int INDIGO_TINT = Color.rgb(232, 234, 246);
    private static final int MUTED_BG = Color.rgb(243, 244, 246);
    private static final int SAFE = Color.rgb(76, 175, 80);
    private static final int SAFE_TINT = Color.rgb(232, 245, 233);
    private static final int STAR = Color.rgb(255, 193, 7);
    private static final int DOT = Color.rgb(158, 158, 158);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int dangerCount  = getIntent().getIntExtra(EXTRA_DANGER_COUNT,  0);
        int cautionCount = getIntent().getIntExtra(EXTRA_CAUTION_COUNT, 0);
        setContentView(createView(dangerCount, cautionCount));
    }

    private View createView(int dangerCount, int cautionCount) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        root.addView(topBar());
        root.addView(progress());

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(18));
        scroll.addView(content);

        content.addView(heroHeader(dangerCount, cautionCount));
        content.addView(sectionLabel());
        content.addView(expertCard("김민준 변호사", "임대차 전문", "4.9", "리뷰 127개", 0));
        content.addView(expertCard("이서연 공인중개사", "전세계약 전문", "4.7", "리뷰 89개", 10));
        content.addView(expertCard("박준호 변호사", "부동산 분쟁", "4.8", "리뷰 214개", 10));
        content.addView(infoSection());

        root.addView(scroll);
        root.addView(bottomBar());
        return root;
    }

    private View topBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(CARD);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        TextView back = text("‹", TEXT, 36, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        TextView title = text("전문가 연결", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        bar.addView(divider, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM));
        return bar;
    }

    private View progress() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(BORDER);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));

        View fill = new View(this);
        fill.setBackgroundColor(PRIMARY);
        bar.addView(fill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return bar;
    }

    private View heroHeader(int dangerCount, int cautionCount) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(20), dp(28), dp(20), dp(32));
        hero.setBackground(bottomRound(PRIMARY, dp(24)));

        hero.addView(new ShieldCheckIcon(this), new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = text("법률 상담이 필요한가요?", Color.WHITE, 20, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(14), 0, 0);
        hero.addView(title, titleParams);

        String subtitleText = dangerCount > 0
                ? "위험 조항 " + dangerCount + "건이 발견되었습니다.\n전문가와 함께 계약서를 검토하세요."
                : "전문가와 함께 계약서를 꼼꼼히 검토하세요.";
        TextView subtitle = text(subtitleText, alphaWhite(0.8f), 14, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(0, 1.6f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(8), 0, 0);
        hero.addView(subtitle, subtitleParams);

        LinearLayout pills = new LinearLayout(this);
        pills.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pillsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pillsParams.setMargins(0, dp(16), 0, 0);
        hero.addView(pills, pillsParams);
        if (dangerCount > 0)  pills.addView(heroPill("위험 " + dangerCount + "건", 0));
        if (cautionCount > 0) pills.addView(heroPill("주의 " + cautionCount + "건", dangerCount > 0 ? 8 : 0));
        return hero;
    }

    private View sectionLabel() {
        TextView label = text("추천 전문가", TEXT, 15, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), dp(20), dp(16), dp(8));
        label.setLayoutParams(params);
        return label;
    }

    private View expertCard(String name, String specialty, String rating, String reviews, int top) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundRect(CARD, dp(12), 0, 0));
        card.setElevation(dp(3));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), dp(top), dp(16), 0);
        card.setLayoutParams(params);

        TextView avatar = text("👤", PRIMARY, 23, Typeface.NORMAL);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(oval(INDIGO_TINT));
        card.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        centerParams.setMargins(dp(12), 0, dp(12), 0);
        card.addView(center, centerParams);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        center.addView(nameRow);
        nameRow.addView(text(name, TEXT, 15, Typeface.BOLD));
        nameRow.addView(specialtyPill(specialty));

        LinearLayout ratingRow = new LinearLayout(this);
        ratingRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ratingParams.setMargins(0, dp(4), 0, 0);
        center.addView(ratingRow, ratingParams);
        ratingRow.addView(text("★", STAR, 13, Typeface.BOLD));
        ratingRow.addView(withLeft(text(rating, TEXT, 13, Typeface.BOLD), 3));
        ratingRow.addView(withLeft(text("·", DOT, 13, Typeface.BOLD), 7));
        ratingRow.addView(withLeft(text(reviews, SECONDARY, 12, Typeface.NORMAL), 7));

        TextView button = text("연결하기", PRIMARY, 13, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(roundRect(CARD, dp(8), PRIMARY, Math.max(1, dp(1.5f))));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v ->
                Toast.makeText(this, "전문가 연결 서비스는 준비 중입니다.", Toast.LENGTH_SHORT).show());
        card.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        return card;
    }

    private View infoSection() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        wrapperParams.setMargins(dp(16), dp(16), dp(16), 0);
        wrapper.setLayoutParams(wrapperParams);

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(roundRect(MUTED_BG, dp(10), 0, 0));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, dp(14), 0, 0);
        wrapper.addView(row, rowParams);

        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:16705520"));
            startActivity(dial);
        });
        row.addView(text("☎", PRIMARY, 20, Typeface.BOLD), new LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView phone = text("주거임대차 분쟁 상담  1670-5520", TEXT, 14, Typeface.BOLD);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        phoneParams.setMargins(dp(10), 0, 0, 0);
        row.addView(phone, phoneParams);
        row.addView(infoPill("무료"));
        return wrapper;
    }

    private View bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(16), dp(12), dp(16), dp(24));
        bar.setBackgroundColor(CARD);
        bar.setElevation(dp(8));

        TextView button = text("전문가에게 리포트 공유하기  →", Color.WHITE, 16, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PRIMARY, dp(12), 0, 0));
        bar.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return bar;
    }

    private View heroPill(String value, int left) {
        TextView pill = text(value, Color.WHITE, 12, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(14), dp(6), dp(14), dp(6));
        pill.setBackground(roundRect(Color.argb(51, 255, 255, 255), dp(16), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(left), 0, 0, 0);
        pill.setLayoutParams(params);
        return pill;
    }

    private View specialtyPill(String value) {
        TextView pill = text(value, PRIMARY, 11, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(8), dp(2), dp(8), dp(2));
        pill.setBackground(roundRect(INDIGO_TINT, dp(8), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(6), 0, 0, 0);
        pill.setLayoutParams(params);
        return pill;
    }

    private View infoPill(String value) {
        TextView pill = text(value, SAFE, 12, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(10), dp(3), dp(10), dp(3));
        pill.setBackground(roundRect(SAFE_TINT, dp(8), 0, 0));
        return pill;
    }

    private View withLeft(View view, int left) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(left), 0, 0, 0);
        view.setLayoutParams(params);
        return view;
    }

    private TextView text(String value, int color, int size, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        textView.setTypeface(Typeface.create("Noto Sans KR", style));
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

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable bottomRound(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        float r = radius;
        drawable.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        return drawable;
    }

    private int alphaWhite(float alpha) {
        return Color.argb(Math.round(255 * alpha), 255, 255, 255);
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private class ShieldCheckIcon extends View {
        private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shield = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint check = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shieldPath = new Path();
        private final Path checkPath = new Path();

        ShieldCheckIcon(Activity activity) {
            super(activity);
            circle.setColor(Color.argb(38, 255, 255, 255));
            circle.setStyle(Paint.Style.FILL);
            shield.setColor(Color.WHITE);
            shield.setStyle(Paint.Style.STROKE);
            shield.setStrokeWidth(dp(2));
            shield.setStrokeJoin(Paint.Join.ROUND);
            check.setColor(Color.WHITE);
            check.setStyle(Paint.Style.STROKE);
            check.setStrokeWidth(dp(2.4f));
            check.setStrokeCap(Paint.Cap.ROUND);
            check.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawCircle(cx, cy, Math.min(getWidth(), getHeight()) / 2f, circle);

            shieldPath.reset();
            shieldPath.moveTo(cx, dp(14));
            shieldPath.lineTo(dp(39), dp(19));
            shieldPath.lineTo(dp(37), dp(33));
            shieldPath.quadTo(cx, dp(43), dp(19), dp(33));
            shieldPath.lineTo(dp(17), dp(19));
            shieldPath.close();
            canvas.drawPath(shieldPath, shield);

            checkPath.reset();
            checkPath.moveTo(dp(23), dp(28));
            checkPath.lineTo(dp(27), dp(32));
            checkPath.lineTo(dp(34), dp(24));
            canvas.drawPath(checkPath, check);
        }
    }
}
