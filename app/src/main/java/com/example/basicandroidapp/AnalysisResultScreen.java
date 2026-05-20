package com.example.basicandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
    private static final int DANGER = Color.rgb(244, 67, 54);
    private static final int DANGER_TINT = Color.rgb(255, 235, 238);
    private static final int WARNING = Color.rgb(255, 193, 7);
    private static final int WARNING_TEXT = Color.rgb(245, 127, 23);
    private static final int WARNING_TINT = Color.rgb(255, 253, 231);
    private static final int SAFE = Color.rgb(76, 175, 80);
    private static final int SAFE_TINT = Color.rgb(232, 245, 233);

    private String contractText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contractText = getIntent().getStringExtra(ApiContract.EXTRA_CONTRACT_TEXT);
        setContentView(createView());
    }

    private View createView() {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        frame.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(topBar());
        root.addView(progress(0.6f));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        scroll.addView(content);
        content.addView(scoreCard());
        content.addView(highlightCard());
        content.addView(publicDataCard());
        root.addView(scroll);
        root.addView(bottomBar());

        frame.addView(bottomSheet(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return frame;
    }

    private View topBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView back = text("‹", TEXT, 34, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
        TextView title = text("분석 결과", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView share = text("↗", PRIMARY, 24, Typeface.BOLD);
        share.setGravity(Gravity.CENTER);
        share.setOnClickListener(v -> startActivity(new Intent(this, ReportSummary.class)));
        bar.addView(share, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        bar.addView(divider, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM));
        return bar;
    }

    private View scoreCard() {
        LinearLayout card = card(0);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setElevation(dp(2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header);
        header.addView(text("종합 안전 점수", TEXT, 16, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(text("2024.01.15 분석", Color.rgb(158, 158, 158), 12, Typeface.NORMAL));

        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.setMargins(0, dp(16), 0, 0);
        card.addView(body, bodyParams);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        body.addView(left, new LinearLayout.LayoutParams(dp(136), ViewGroup.LayoutParams.WRAP_CONTENT));
        left.addView(new ScoreGauge(this, 120, 36, 14), new LinearLayout.LayoutParams(dp(120), dp(120)));
        left.addView(pill("⚠ 주의 필요", Color.rgb(255, 248, 225), WARNING_TEXT, 12));

        LinearLayout counts = new LinearLayout(this);
        counts.setOrientation(LinearLayout.VERTICAL);
        body.addView(counts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        counts.addView(riskRow(DANGER, "위험", "2건"));
        counts.addView(riskRow(WARNING, "주의", "3건"));
        counts.addView(riskRow(SAFE, "안전", "5건"));
        return card;
    }

    private View highlightCard() {
        LinearLayout card = card(12);
        card.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setElevation(dp(2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header);
        header.addView(text("원문 분석", TEXT, 16, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(text("전체보기", PRIMARY, 13, Typeface.NORMAL));

        TextView body = text("", TEXT, 14, Typeface.NORMAL);
        body.setLineSpacing(dp(9), 1.0f);
        body.setText(highlightedText());
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200));
        bodyParams.setMargins(0, dp(12), 0, 0);
        card.addView(body, bodyParams);

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.setMargins(0, dp(12), 0, dp(12));
        card.addView(divider, dividerParams);

        TextView detail = text("위험 조항 2개 상세 보기  ›", PRIMARY, 13, Typeface.BOLD);
        card.addView(detail);
        return card;
    }

    private SpannableString highlightedText() {
        String fallbackText = "계약 기간은 2년으로 한다 [안전]\n\n"
                + "임대인은 별도 통보 없이 계약을 해지할 수 있다 [위험]\n\n"
                + "수리비 일체는 임차인이 부담한다 [주의]\n\n"
                + "전세금 반환이 지연되는 경우 임대인은 책임을 면할 수 있다.";
        String value = contractText == null || contractText.trim().isEmpty() ? fallbackText : contractText;
        SpannableString span = new SpannableString(value);
        mark(span, value, "계약 기간은 2년으로 한다", SAFE_TINT, SAFE);
        mark(span, value, "[안전]", SAFE_TINT, SAFE);
        mark(span, value, "임대인은 별도 통보 없이 계약을 해지할 수 있다", DANGER_TINT, DANGER);
        mark(span, value, "[위험]", DANGER_TINT, DANGER);
        mark(span, value, "수리비 일체는 임차인이 부담한다", WARNING_TINT, WARNING_TEXT);
        mark(span, value, "[주의]", WARNING_TINT, WARNING_TEXT);
        return span;
    }

    private void mark(SpannableString span, String source, String target, int bg, int fg) {
        int start = source.indexOf(target);
        if (start >= 0) {
            span.setSpan(new BackgroundColorSpan(bg), start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(fg), start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(Typeface.BOLD), start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private View publicDataCard() {
        LinearLayout card = card(12);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(2));
        card.addView(text("공공데이터 조회 결과", TEXT, 16, Typeface.BOLD));
        card.addView(statusRow("▣", "등기부등본", "근저당 있음", DANGER_TINT, DANGER));
        card.addView(statusRow("⌂", "건축물대장", "정상", SAFE_TINT, SAFE));
        card.addView(statusRow("₩", "실거래가", "시세 대비 높음", DANGER_TINT, DANGER));
        return card;
    }

    private View bottomSheet() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 0, 0, 0));
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(12), dp(20), dp(20));
        sheet.setBackground(topRound(Color.WHITE, dp(20)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        overlay.addView(sheet, params);

        View handle = new View(this);
        handle.setBackground(roundRect(BORDER, dp(2), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, handleParams);
        sheet.addView(withTop(pill("⚠ 주의 조항", Color.rgb(255, 248, 225), WARNING_TEXT, 12), 16));
        sheet.addView(withTop(text("수리비 일체는 임차인이 부담한다", TEXT, 17, Typeface.BOLD), 10));
        TextView body = text("임차인이 모든 수리비를 부담하도록 하는 조항은\n민법 제623조(임대인의 수선의무)에 위반될 수 있습니다.", SECONDARY, 14, Typeface.NORMAL);
        body.setLineSpacing(dp(7), 1.0f);
        sheet.addView(withTop(body, 8));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        d.setMargins(0, dp(16), 0, dp(16));
        sheet.addView(divider, d);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        sheet.addView(buttons);
        buttons.addView(button("법적 근거 보기", Color.WHITE, PRIMARY, PRIMARY, 0));
        buttons.addView(button("안전 수정안 보기", PRIMARY, Color.WHITE, 0, dp(10)));
        return overlay;
    }

    private View bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(16), dp(12), dp(16), dp(24));
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));
        TextView button = text("공유하기", Color.WHITE, 16, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PRIMARY, dp(12), 0, 0));
        button.setOnClickListener(v -> startActivity(new Intent(this, ReportSummary.class)));
        bar.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return bar;
    }

    private View riskRow(int color, String label, String count) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        row.setLayoutParams(params);
        TextView dot = text("●", color, 12, Typeface.BOLD);
        row.addView(dot, new LinearLayout.LayoutParams(dp(18), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(text(label, color, 13, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(count, TEXT, 15, Typeface.BOLD));
        return row;
    }

    private View statusRow(String icon, String label, String status, int bg, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        params.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(params);
        row.addView(text(icon, PRIMARY, 18, Typeface.BOLD), new LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(text(label, TEXT, 14, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(pill(status, bg, color, 12));
        return row;
    }

    private View button(String label, int bg, int fg, int border, int left) {
        TextView b = text(label, fg, 14, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundRect(bg, dp(10), border, border == 0 ? 0 : Math.max(1, dp(1.5f))));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.setMargins(left, 0, 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private LinearLayout card(int top) {
        LinearLayout card = new LinearLayout(this);
        card.setBackground(roundRect(Color.WHITE, dp(12), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(top), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private TextView text(String value, int color, int size, int style) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        t.setTypeface(Typeface.create("sans-serif", style));
        return t;
    }

    private View pill(String value, int bg, int color, int size) {
        TextView p = text(value, color, size, Typeface.BOLD);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(12), dp(4), dp(12), dp(4));
        p.setBackground(roundRect(bg, dp(12), 0, 0));
        return p;
    }

    private View withTop(View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(top), 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    private View progress(float ratio) {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(BORDER);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        View fill = new View(this);
        fill.setBackgroundColor(PRIMARY);
        bar.addView(fill, new FrameLayout.LayoutParams(dp(375 * ratio), ViewGroup.LayoutParams.MATCH_PARENT));
        return bar;
    }

    private GradientDrawable roundRect(int color, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (width > 0) d.setStroke(width, stroke);
        return d;
    }

    private GradientDrawable topRound(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        float r = radius;
        d.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return d;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private class ScoreGauge extends View {
        private final int size;
        private final int numberSize;
        private final int pointSize;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScoreGauge(Activity activity, int size, int numberSize, int pointSize) {
            super(activity);
            this.size = size;
            this.numberSize = numberSize;
            this.pointSize = pointSize;
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(dp(10));
            track.setStrokeCap(Paint.Cap.ROUND);
            track.setColor(BORDER);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(dp(10));
            arc.setStrokeCap(Paint.Cap.ROUND);
            arc.setColor(WARNING);
            number.setTextAlign(Paint.Align.CENTER);
            number.setColor(TEXT);
            number.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            number.setTextSize(sp(numberSize));
            point.setTextAlign(Paint.Align.CENTER);
            point.setColor(SECONDARY);
            point.setTextSize(sp(pointSize));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float inset = dp(8);
            RectF rect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawArc(rect, -90, 360, false, track);
            canvas.drawArc(rect, -90, 223.2f, false, arc);
            canvas.drawText("62", getWidth() / 2f, dp(size / 2f + numberSize / 3f), number);
            canvas.drawText("점", getWidth() / 2f, dp(size / 2f + 29), point);
        }
    }
}
