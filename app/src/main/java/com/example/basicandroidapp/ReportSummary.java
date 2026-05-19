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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ReportSummary extends Activity {
    private static final int BACKGROUND = Color.rgb(248, 249, 250);
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int DANGER = Color.rgb(244, 67, 54);
    private static final int DANGER_TINT = Color.rgb(255, 235, 238);
    private static final int WARNING = Color.rgb(255, 193, 7);
    private static final int WARNING_TEXT = Color.rgb(245, 127, 23);
    private static final int WARNING_TINT = Color.rgb(255, 248, 225);
    private static final int SAFE = Color.rgb(76, 175, 80);
    private static final int SAFE_TINT = Color.rgb(232, 245, 233);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createView());
    }

    private View createView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.addView(topBar());
        root.addView(progress(0.8f));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        scroll.addView(content);
        content.addView(scoreCard());
        content.addView(clauseCard("위험 조항", "2건", DANGER, new String[][]{
                {"임대인 일방 해지 조항", "민법 제635조 위반 가능성"},
                {"전세금 반환 지연 면책", "주택임대차보호법 위반 소지"}
        }));
        content.addView(clauseCard("주의 조항", "3건", WARNING, new String[][]{
                {"수리비 세입자 부담", "민법 제623조 확인 필요"},
                {"묵시적 갱신 거부 특약", "갱신 거절 조건 명확화 필요"},
                {"근저당 설정 특약", "보증금 회수 위험 가능성"}
        }));
        content.addView(publicDataCard());
        content.addView(consultBanner());
        root.addView(scroll);
        root.addView(bottomBar());
        return root;
    }

    private View topBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView back = text("‹", TEXT, 34, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
        TextView title = text("종합 리포트", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView download = text("↓", PRIMARY, 26, Typeface.BOLD);
        download.setGravity(Gravity.CENTER);
        bar.addView(download, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        bar.addView(divider, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM));
        return bar;
    }

    private View scoreCard() {
        LinearLayout card = card(0);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(2));
        card.addView(new ScoreCircle(this), new LinearLayout.LayoutParams(dp(80), dp(80)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        columnParams.setMargins(dp(14), 0, 0, 0);
        card.addView(column, columnParams);
        column.addView(text("서울시 마포구 아현동 OO아파트", TEXT, 15, Typeface.BOLD));
        column.addView(withTop(text("전세 보증금 3억 2천만원", SECONDARY, 13, Typeface.NORMAL), 4));
        LinearLayout tags = new LinearLayout(this);
        LinearLayout.LayoutParams tagsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagsParams.setMargins(0, dp(8), 0, 0);
        column.addView(tags, tagsParams);
        tags.addView(pill("⚠ 주의 필요", WARNING_TINT, WARNING_TEXT, 12, 12));
        tags.addView(withLeft(pill("위험 2건", DANGER_TINT, DANGER, 12, 12), 6));
        return card;
    }

    private View clauseCard(String title, String count, int accent, String[][] rows) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setBackground(roundRect(Color.WHITE, dp(12), 0, 0));
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerParams.setMargins(0, dp(12), 0, 0);
        outer.setLayoutParams(outerParams);
        View accentBar = new View(this);
        accentBar.setBackground(roundRect(accent, dp(4), 0, 0));
        outer.addView(accentBar, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(16), dp(16), dp(6));
        outer.addView(card, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header);
        header.addView(text(title, TEXT, 16, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(text(count, accent, 14, Typeface.BOLD));
        for (int i = 0; i < rows.length; i++) {
            card.addView(clauseRow(rows[i][0], rows[i][1], accent));
            if (i < rows.length - 1) card.addView(divider());
        }
        return outer;
    }

    private View clauseRow(String title, String sub, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        TextView icon = text("⚠", color, 20, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(40)));
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        row.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        center.addView(text(title, TEXT, 14, Typeface.BOLD));
        center.addView(withTop(text(sub, SECONDARY, 12, Typeface.NORMAL), 2));
        TextView arrow = text("›", Color.rgb(158, 158, 158), 24, Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(40)));
        return row;
    }

    private View publicDataCard() {
        LinearLayout card = card(12);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.addView(text("공공데이터 조회", TEXT, 16, Typeface.BOLD));
        card.addView(dataRow("등기부등본", "근저당 2건 확인", "위험", DANGER_TINT, DANGER));
        card.addView(dataRow("건축물대장", "주거용 정상 등록", "안전", SAFE_TINT, SAFE));
        card.addView(dataRow("실거래가", "시세 대비 8% 초과", "위험", DANGER_TINT, DANGER));
        return card;
    }

    private View consultBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setClickable(true);
        banner.setFocusable(true);
        banner.setOnClickListener(v -> startActivity(new Intent(this, ExpertComment.class)));
        banner.setBackground(roundRect(Color.rgb(232, 234, 246), dp(12), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        banner.setLayoutParams(params);
        View accent = new View(this);
        accent.setBackground(roundRect(PRIMARY, dp(4), 0, 0));
        banner.addView(accent, new LinearLayout.LayoutParams(dp(4), dp(72)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(12), dp(14), dp(12), dp(14));
        banner.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        texts.addView(text("전문가 상담을 권장합니다", PRIMARY, 15, Typeface.BOLD));
        texts.addView(withTop(text("위험 조항 2건이 발견되었습니다.", Color.rgb(92, 107, 192), 13, Typeface.NORMAL), 4));
        TextView arrow = text("›", Color.WHITE, 26, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(oval(PRIMARY));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        arrowParams.setMargins(0, 0, dp(16), 0);
        banner.addView(arrow, arrowParams);
        return banner;
    }

    private View bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(16), dp(12), dp(16), dp(24));
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));
        bar.addView(bottomButton("PDF 저장", Color.WHITE, PRIMARY, PRIMARY, 0, false));
        bar.addView(bottomButton("카카오톡 공유", Color.rgb(254, 229, 0), Color.rgb(25, 25, 25), 0, dp(10), true));
        return bar;
    }

    private View dataRow(String left, String center, String badge, int bg, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(left, TEXT, 13, Typeface.BOLD), new LinearLayout.LayoutParams(dp(82), dp(44)));
        TextView c = text(center, SECONDARY, 13, Typeface.NORMAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(c, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(pill(badge, bg, color, 12, 8));
        return row;
    }

    private View bottomButton(String label, int bg, int fg, int border, int left, boolean kakao) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setBackground(roundRect(bg, dp(12), border, border == 0 ? 0 : Math.max(1, dp(1.5f))));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(left, 0, 0, 0);
        button.setLayoutParams(params);
        if (kakao) {
            TextView icon = text("톡", Color.rgb(254, 229, 0), 9, Typeface.BOLD);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(oval(fg));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
            iconParams.setMargins(0, 0, dp(6), 0);
            button.addView(icon, iconParams);
        }
        button.addView(text(label, fg, 14, Typeface.BOLD));
        return button;
    }

    private LinearLayout card(int top) {
        LinearLayout card = new LinearLayout(this);
        card.setBackground(roundRect(Color.WHITE, dp(12), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(top), 0, 0);
        card.setLayoutParams(params);
        return card;
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

    private TextView text(String value, int color, int size, int style) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        t.setTypeface(Typeface.create("sans-serif", style));
        return t;
    }

    private View pill(String value, int bg, int color, int size, int radius) {
        TextView p = text(value, color, size, Typeface.BOLD);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(10), dp(4), dp(10), dp(4));
        p.setBackground(roundRect(bg, dp(radius), 0, 0));
        return p;
    }

    private View withTop(View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(top), 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    private View withLeft(View v, int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), 0, 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(BORDER);
        d.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return d;
    }

    private GradientDrawable roundRect(int color, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (width > 0) d.setStroke(width, stroke);
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private class ScoreCircle extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScoreCircle(Activity activity) {
            super(activity);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(dp(8));
            track.setStrokeCap(Paint.Cap.ROUND);
            track.setColor(BORDER);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(dp(8));
            arc.setStrokeCap(Paint.Cap.ROUND);
            arc.setColor(WARNING);
            number.setTextAlign(Paint.Align.CENTER);
            number.setColor(TEXT);
            number.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            number.setTextSize(sp(28));
            point.setTextAlign(Paint.Align.CENTER);
            point.setColor(SECONDARY);
            point.setTextSize(sp(12));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float inset = dp(6);
            RectF rect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawArc(rect, -90, 360, false, track);
            canvas.drawArc(rect, -90, 223.2f, false, arc);
            canvas.drawText("62", getWidth() / 2f, dp(39), number);
            canvas.drawText("점", getWidth() / 2f, dp(57), point);
        }
    }
}
