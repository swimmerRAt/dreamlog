package com.example.basicandroidapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportSummary extends Activity {
    private static final int BACKGROUND  = Color.rgb(248, 249, 250);
    private static final int PRIMARY     = Color.rgb(63, 81, 181);
    private static final int TEXT        = Color.rgb(33, 33, 33);
    private static final int SECONDARY   = Color.rgb(117, 117, 117);
    private static final int BORDER      = Color.rgb(224, 224, 224);
    private static final int DANGER      = Color.rgb(244, 67, 54);
    private static final int DANGER_TINT = Color.rgb(255, 235, 238);
    private static final int WARNING     = Color.rgb(255, 193, 7);
    private static final int WARNING_TEXT = Color.rgb(245, 127, 23);
    private static final int WARNING_TINT = Color.rgb(255, 248, 225);
    private static final int SAFE        = Color.rgb(76, 175, 80);
    private static final int SAFE_TINT   = Color.rgb(232, 245, 233);

    private String address = "";
    private String riskLevel = "중간";
    private String summary = "";
    private int score = 62;
    private int dangerCount = 0;
    private int cautionCount = 0;
    private final List<AnalysisResultScreen.ToxicClause> dangerClauses = new ArrayList<>();
    private final List<AnalysisResultScreen.ToxicClause> cautionClauses = new ArrayList<>();
    private final List<AnalysisResultScreen.BuildingInfo> buildingInfoList = new ArrayList<>();
    private final List<AnalysisResultScreen.TradeInfo> recentTrades = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseAnalysisResult(getIntent().getStringExtra(AnalysisResultScreen.EXTRA_ANALYSIS_RESULT));
        setContentView(createView());
    }

    private void parseAnalysisResult(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(json);
            address = obj.optString("address", "");
            riskLevel = obj.optString("risk_level", "중간");
            summary = obj.optString("summary", "");

            if (riskLevel.contains("낮")) score = 85;
            else if (riskLevel.contains("높")) score = 35;
            else score = 62;

            JSONArray clauses = obj.optJSONArray("toxic_clauses");
            if (clauses != null) {
                for (int i = 0; i < clauses.length(); i++) {
                    JSONObject c = clauses.getJSONObject(i);
                    AnalysisResultScreen.ToxicClause tc = new AnalysisResultScreen.ToxicClause(
                            c.optString("clause"),
                            c.optString("reason"),
                            c.optString("severity"),
                            c.optString("recommendation"));
                    if (tc.isDanger()) { dangerClauses.add(tc); dangerCount++; }
                    else { cautionClauses.add(tc); cautionCount++; }
                }
            }
            parsePublicData(obj.optJSONObject("public_data"));
        } catch (Exception ignored) { /* 파싱 실패 시 기본값 유지 */ }
    }

    private void parsePublicData(JSONObject pubData) {
        if (pubData == null || !pubData.optBoolean("found", false)) return;
        JSONObject bldg = pubData.optJSONObject("building");
        if (bldg != null) {
            buildingInfoList.add(new AnalysisResultScreen.BuildingInfo(
                    bldg.optString("address"),
                    bldg.optString("purpose"),
                    bldg.optString("structure"),
                    bldg.optInt("floors_above", 0),
                    bldg.optString("approval_date"),
                    bldg.optDouble("total_area", 0)));
        }
        JSONArray trades = pubData.optJSONArray("recent_trades");
        if (trades != null) {
            for (int i = 0; i < trades.length(); i++) {
                try {
                    JSONObject t = trades.getJSONObject(i);
                    recentTrades.add(new AnalysisResultScreen.TradeInfo(
                            t.optString("complex_name"),
                            t.optString("trade_type"),
                            t.optString("area"),
                            t.optString("deposit"),
                            t.optString("monthly_rent", "0"),
                            t.optString("contract_date"),
                            t.optString("floor")));
                } catch (Exception ignored) { /* no-op */ }
            }
        }
    }

    private View createView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.addView(topBar());
        root.addView(progress(0.8f));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        scroll.addView(content);

        content.addView(scoreCard());
        if (!dangerClauses.isEmpty()) {
            content.addView(clauseCard("위험 조항", dangerCount + "건", DANGER,
                    clausesToRows(dangerClauses)));
        }
        if (!cautionClauses.isEmpty()) {
            content.addView(clauseCard("주의 조항", cautionCount + "건", WARNING,
                    clausesToRows(cautionClauses)));
        }
        content.addView(publicDataCard());
        content.addView(consultBanner());
        root.addView(scroll);
        root.addView(bottomBar());
        return root;
    }

    private String[][] clausesToRows(List<AnalysisResultScreen.ToxicClause> clauses) {
        String[][] rows = new String[clauses.size()][2];
        for (int i = 0; i < clauses.size(); i++) {
            AnalysisResultScreen.ToxicClause tc = clauses.get(i);
            rows[i][0] = tc.clause;
            rows[i][1] = tc.reason.isEmpty() ? tc.recommendation : tc.reason;
        }
        return rows;
    }

    private View topBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView back = text("‹", TEXT, 34, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(
                dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
        TextView title = text("종합 리포트", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView download = text("↓", PRIMARY, 26, Typeface.BOLD);
        download.setGravity(Gravity.CENTER);
        download.setClickable(true);
        download.setFocusable(true);
        download.setOnClickListener(v -> savePdf());
        bar.addView(download, new FrameLayout.LayoutParams(
                dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        bar.addView(divider, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM));
        return bar;
    }

    private View scoreCard() {
        LinearLayout card = card(0);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(2));
        card.addView(new ScoreCircle(this, score),
                new LinearLayout.LayoutParams(dp(80), dp(80)));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        columnParams.setMargins(dp(14), 0, 0, 0);
        card.addView(column, columnParams);

        String cardTitle = address.isEmpty() ? "분석된 계약서" : address;
        column.addView(text(cardTitle, TEXT, 15, Typeface.BOLD));

        String riskLabel;
        if (riskLevel.contains("낮")) riskLabel = "안전한 계약서입니다";
        else if (riskLevel.contains("높")) riskLabel = "위험한 조항이 포함되어 있습니다";
        else riskLabel = "일부 주의가 필요합니다";
        column.addView(withTop(text(riskLabel, SECONDARY, 13, Typeface.NORMAL), 4));

        LinearLayout tags = new LinearLayout(this);
        LinearLayout.LayoutParams tagsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagsParams.setMargins(0, dp(8), 0, 0);
        column.addView(tags, tagsParams);

        int pillBg;
        int pillFg;
        String pillLabel;
        if (riskLevel.contains("높")) {
            pillLabel = "⚠ 위험"; pillBg = DANGER_TINT; pillFg = DANGER;
        } else if (riskLevel.contains("낮")) {
            pillLabel = "✔ 안전"; pillBg = SAFE_TINT; pillFg = SAFE;
        } else {
            pillLabel = "⚠ 주의 필요"; pillBg = WARNING_TINT; pillFg = WARNING_TEXT;
        }
        tags.addView(pill(pillLabel, pillBg, pillFg, 12, 12));
        if (dangerCount > 0) {
            tags.addView(withLeft(pill("위험 " + dangerCount + "건", DANGER_TINT, DANGER, 12, 12), 6));
        }
        return card;
    }

    private View clauseCard(String title, String count, int accent, String[][] rows) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setBackground(roundRect(Color.WHITE, dp(12), 0, 0));
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerParams.setMargins(0, dp(12), 0, 0);
        outer.setLayoutParams(outerParams);

        View accentBar = new View(this);
        accentBar.setBackground(roundRect(accent, dp(4), 0, 0));
        outer.addView(accentBar, new LinearLayout.LayoutParams(
                dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(16), dp(16), dp(6));
        outer.addView(card, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header);
        header.addView(text(title, TEXT, 16, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
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
        row.addView(center, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
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
        card.addView(buildingRow());
        card.addView(tradeRow());
        return card;
    }

    private View buildingRow() {
        if (!buildingInfoList.isEmpty()) {
            AnalysisResultScreen.BuildingInfo b = buildingInfoList.get(0);
            StringBuilder detail = new StringBuilder();
            if (!b.purpose.isEmpty()) detail.append(b.purpose);
            if (b.floorsAbove > 0) {
                if (detail.length() > 0) detail.append(" · ");
                detail.append(b.floorsAbove).append("층");
            }
            if (!b.approvalDate.isEmpty()) {
                if (detail.length() > 0) detail.append(" · ");
                detail.append(b.approvalDate).append(" 사용승인");
            }
            return detailDataRow("건축물대장", detail.toString(), "정상", SAFE_TINT, SAFE);
        }
        return dataRow("건축물대장", "조회 결과 없음", "미조회", Color.rgb(245, 245, 245), SECONDARY);
    }

    private View tradeRow() {
        if (!recentTrades.isEmpty()) {
            AnalysisResultScreen.TradeInfo t = recentTrades.get(0);
            StringBuilder detail = new StringBuilder();
            if (!t.complexName.isEmpty()) detail.append(t.complexName).append(" · ");
            detail.append(t.tradeType).append(" 보증금 ").append(t.deposit).append("만원");
            if (!"0".equals(t.monthlyRent) && !t.monthlyRent.isEmpty()) {
                detail.append(" / 월세 ").append(t.monthlyRent).append("만원");
            }
            if (t.contractDate.length() >= 6) {
                detail.append(" (").append(t.contractDate, 0, 4)
                      .append("년 ").append(t.contractDate, 4, 6).append("월)");
            }
            return detailDataRow("실거래가", detail.toString(), "조회됨", SAFE_TINT, SAFE);
        }
        return dataRow("실거래가", "조회 결과 없음", "미조회", Color.rgb(245, 245, 245), SECONDARY);
    }

    private View detailDataRow(String label, String detail, String badge, int badgeBg, int badgeColor) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        wrapper.addView(dataRow(label, "", badge, badgeBg, badgeColor));

        if (!detail.isEmpty()) {
            TextView detailView = text(detail, SECONDARY, 12, Typeface.NORMAL);
            detailView.setMaxLines(2);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(dp(82), 0, 0, dp(4));
            detailView.setLayoutParams(p);
            wrapper.addView(detailView);
        }
        return wrapper;
    }

    private View consultBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setClickable(true);
        banner.setFocusable(true);
        banner.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExpertComment.class);
            intent.putExtra(ExpertComment.EXTRA_DANGER_COUNT,  dangerCount);
            intent.putExtra(ExpertComment.EXTRA_CAUTION_COUNT, cautionCount);
            startActivity(intent);
        });
        banner.setBackground(roundRect(Color.rgb(232, 234, 246), dp(12), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        String bannerSub;
        if (dangerCount > 0) bannerSub = "위험 조항 " + dangerCount + "건이 발견되었습니다.";
        else if (cautionCount > 0) bannerSub = "주의 조항 " + cautionCount + "건을 확인해주세요.";
        else bannerSub = summary.isEmpty() ? "계약서 검토가 완료되었습니다." : summary;
        texts.addView(withTop(text(bannerSub, Color.rgb(92, 107, 192), 13, Typeface.NORMAL), 4));

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
        bar.addView(bottomButton("PDF 저장", Color.WHITE, PRIMARY, PRIMARY, 0, false,
                this::savePdf));
        bar.addView(bottomButton("카카오톡 공유", Color.rgb(254, 229, 0),
                Color.rgb(25, 25, 25), 0, dp(10), true,
                this::shareReport));
        return bar;
    }

    private View dataRow(String left, String center, String badge, int bg, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(left, TEXT, 13, Typeface.BOLD),
                new LinearLayout.LayoutParams(dp(82), dp(44)));
        TextView c = text(center, SECONDARY, 13, Typeface.NORMAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(c, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(pill(badge, bg, color, 12, 8));
        return row;
    }

    private View bottomButton(String label, int bg, int fg, int border, int left, boolean kakao,
                              Runnable onClick) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> onClick.run());
        button.setBackground(roundRect(bg, dp(12), border,
                border == 0 ? 0 : Math.max(1, dp(1.5f))));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(top), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private View progress(float ratio) {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(BORDER);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        View fill = new View(this);
        fill.setBackgroundColor(PRIMARY);
        bar.addView(fill, new FrameLayout.LayoutParams(
                dp(375 * ratio), ViewGroup.LayoutParams.MATCH_PARENT));
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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(top), 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    private View withLeft(View v, int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), 0, 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    private void savePdf() {
        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
        if (pm == null) {
            Toast.makeText(this, "인쇄 서비스를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String jobName = address.isEmpty() ? "계약서_분석_리포트" : address.replace(" ", "_") + "_리포트";
        pm.print(jobName, new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                                 CancellationSignal signal, LayoutResultCallback callback, Bundle extras) {
                callback.onLayoutFinished(
                        new PrintDocumentInfo.Builder("계약서_분석_리포트.pdf")
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(1)
                                .build(), !newAttrs.equals(oldAttrs));
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor dest,
                                CancellationSignal signal, WriteResultCallback callback) {
                PdfDocument pdf = new PdfDocument();
                PdfDocument.Page page = pdf.startPage(
                        new PdfDocument.PageInfo.Builder(595, 842, 1).create());
                drawReportOnCanvas(page.getCanvas());
                pdf.finishPage(page);
                try (FileOutputStream fos = new FileOutputStream(dest.getFileDescriptor())) {
                    pdf.writeTo(fos);
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                } finally {
                    pdf.close();
                }
            }
        }, new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build());
    }

    private void drawReportOnCanvas(Canvas canvas) {
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextSize(18);
        titlePaint.setColor(Color.BLACK);

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setTextSize(12);
        bodyPaint.setColor(Color.BLACK);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setTextSize(11);
        subPaint.setColor(Color.GRAY);

        int x = 40;
        int y = 50;

        canvas.drawText("계약서 안전 분석 리포트", x, y, titlePaint); y += 30;
        canvas.drawText("─────────────────────────────────────────────", x, y, subPaint); y += 20;
        if (!address.isEmpty()) { canvas.drawText("주소: " + address, x, y, bodyPaint); y += 20; }
        canvas.drawText("위험도: " + riskLevel, x, y, bodyPaint); y += 20;
        canvas.drawText("안전 점수: " + score + "점", x, y, bodyPaint); y += 40;

        y = drawClauses(canvas, "[ 위험 조항 " + dangerCount + "건 ]",
                dangerClauses, titlePaint, bodyPaint, subPaint, y);
        y = drawClauses(canvas, "[ 주의 조항 " + cautionCount + "건 ]",
                cautionClauses, titlePaint, bodyPaint, subPaint, y);
        drawSummary(canvas, summary, titlePaint, bodyPaint, y);
    }

    // x=40, lineH=20, pageH=800 은 PDF 고정값
    private int drawClauses(Canvas canvas, String header,
                             List<AnalysisResultScreen.ToxicClause> clauses,
                             Paint titleP, Paint bodyP, Paint subP, int y) {
        if (clauses.isEmpty()) return y;
        canvas.drawText(header, 40, y, titleP); y += 20;
        for (AnalysisResultScreen.ToxicClause tc : clauses) {
            if (y > 800) break;
            canvas.drawText("• " + clip(tc.clause, 60), 50, y, bodyP); y += 20;
            if (!tc.reason.isEmpty()) {
                canvas.drawText("  → " + clip(tc.reason, 65), 50, y, subP); y += 20;
            }
        }
        return y + 8;
    }

    private void drawSummary(Canvas canvas, String text, Paint titleP, Paint bodyP, int y) {
        if (text.isEmpty() || y > 800) return;
        canvas.drawText("[ 종합 의견 ]", 40, y, titleP); y += 20;
        String s = text;
        while (s.length() > 60 && y <= 800) {
            canvas.drawText(s.substring(0, 60), 40, y, bodyP);
            s = s.substring(60);
            y += 20;
        }
        if (!s.isEmpty() && y <= 800) canvas.drawText(s, 40, y, bodyP);
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private void shareReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 계약서 안전 분석 리포트\n\n");
        if (!address.isEmpty()) sb.append("📍 주소: ").append(address).append("\n");
        sb.append("⚡ 위험도: ").append(riskLevel).append("\n");
        sb.append("🔒 안전 점수: ").append(score).append("점\n\n");
        if (!dangerClauses.isEmpty()) {
            sb.append("🚨 위험 조항 ").append(dangerCount).append("건\n");
            for (AnalysisResultScreen.ToxicClause tc : dangerClauses) {
                sb.append("• ").append(tc.clause).append("\n");
            }
            sb.append("\n");
        }
        if (!cautionClauses.isEmpty()) {
            sb.append("⚠ 주의 조항 ").append(cautionCount).append("건\n");
            for (AnalysisResultScreen.ToxicClause tc : cautionClauses) {
                sb.append("• ").append(tc.clause).append("\n");
            }
            sb.append("\n");
        }
        if (!summary.isEmpty()) sb.append("💬 ").append(summary).append("\n");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "계약서 분석 리포트");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        intent.setPackage("com.kakao.talk");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // 카카오톡 미설치 시 일반 공유 시트로 대체
            intent.setPackage(null);
            startActivity(Intent.createChooser(intent, "리포트 공유"));
        }
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(BORDER);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
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
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private class ScoreCircle extends View {
        private final int score;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint point  = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScoreCircle(Activity activity, int score) {
            super(activity);
            this.score = score;
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(dp(8));
            track.setStrokeCap(Paint.Cap.ROUND);
            track.setColor(BORDER);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(dp(8));
            arc.setStrokeCap(Paint.Cap.ROUND);
            if (score >= 70) arc.setColor(SAFE);
            else if (score >= 40) arc.setColor(WARNING);
            else arc.setColor(DANGER);
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
            canvas.drawArc(rect, -90, score * 3.6f, false, arc);
            canvas.drawText(String.valueOf(score), getWidth() / 2f, dp(39), number);
            canvas.drawText("점", getWidth() / 2f, dp(57), point);
        }
    }
}
