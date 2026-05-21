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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResultScreen extends Activity {
    static final String EXTRA_ANALYSIS_RESULT = "analysis_result";

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

    // API 응답 파싱 결과
    private String riskLevel = "알 수 없음";
    private String summary = "";
    private String analysisDate = "";
    private int score = 62;
    private int dangerCount = 0;
    private int cautionCount = 0;
    private int safeCount = 0;
    private List<ToxicClause> toxicClauses = new ArrayList<>();
    private final List<BuildingInfo> buildingInfoList = new ArrayList<>(); // 최대 1개
    private final List<TradeInfo> recentTrades = new ArrayList<>();

    // bottomSheet 동적 업데이트용 뷰 참조
    private TextView bottomSheetSeverityPill;
    private TextView bottomSheetClauseTitle;
    private TextView bottomSheetClauseBody;

    static class ToxicClause {
        final String clause;
        final String reason;
        final String severity;
        final String recommendation;

        ToxicClause(String clause, String reason, String severity, String recommendation) {
            this.clause = clause;
            this.reason = reason;
            this.severity = severity;
            this.recommendation = recommendation;
        }

        boolean isDanger() { return severity.contains("높") || severity.equalsIgnoreCase("danger"); }
        boolean isCaution() { return severity.contains("중") || severity.equalsIgnoreCase("caution"); }
    }

    static class BuildingInfo {
        final String address;
        final String purpose;
        final String structure;
        final String approvalDate;
        final int floorsAbove;
        final double totalArea;

        BuildingInfo(String address, String purpose, String structure,
                     int floorsAbove, String approvalDate, double totalArea) {
            this.address = address;
            this.purpose = purpose;
            this.structure = structure;
            this.floorsAbove = floorsAbove;
            this.approvalDate = approvalDate;
            this.totalArea = totalArea;
        }
    }

    static class TradeInfo {
        final String complexName;
        final String tradeType;
        final String area;
        final String deposit;
        final String monthlyRent;
        final String contractDate;
        final String floor;

        TradeInfo(String complexName, String tradeType, String area, String deposit,
                  String monthlyRent, String contractDate, String floor) {
            this.complexName = complexName;
            this.tradeType = tradeType;
            this.area = area;
            this.deposit = deposit;
            this.monthlyRent = monthlyRent;
            this.contractDate = contractDate;
            this.floor = floor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseAnalysisResult(getIntent().getStringExtra(EXTRA_ANALYSIS_RESULT));
        setContentView(createView());
    }

    private void parseAnalysisResult(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(json);
            riskLevel = obj.optString("risk_level", "알 수 없음");
            summary = obj.optString("summary", "");
            analysisDate = obj.optString("created_at", "");
            if (analysisDate.length() > 10) analysisDate = analysisDate.substring(0, 10);

            // 위험도에 따라 점수 산정
            if (riskLevel.contains("낮")) score = 85;
            else if (riskLevel.contains("높")) score = 35;
            else score = 62;

            JSONArray clauses = obj.optJSONArray("toxic_clauses");
            if (clauses != null) {
                for (int i = 0; i < clauses.length(); i++) {
                    JSONObject c = clauses.getJSONObject(i);
                    ToxicClause tc = new ToxicClause(
                            c.optString("clause"),
                            c.optString("reason"),
                            c.optString("severity"),
                            c.optString("recommendation"));
                    toxicClauses.add(tc);
                    if (tc.isDanger()) dangerCount++;
                    else if (tc.isCaution()) cautionCount++;
                    else safeCount++;
                }
            }
            parsePublicData(obj.optJSONObject("public_data"));
        } catch (Exception ignored) { /* 파싱 실패 시 기본값 유지 */ }
    }

    private void parsePublicData(JSONObject pubData) {
        if (pubData == null || !pubData.optBoolean("found", false)) return;
        JSONObject bldg = pubData.optJSONObject("building");
        if (bldg != null) {
            buildingInfoList.add(new BuildingInfo(
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
                    recentTrades.add(new TradeInfo(
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
        FrameLayout frame = new FrameLayout(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        frame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root.addView(topBar());
        root.addView(progress(0.6f));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(12));
        scroll.addView(content);

        // ✅ popupOverlay를 미리 만들어서 GONE 상태로 추가
        View popupOverlay = bottomSheet();
        popupOverlay.setVisibility(View.GONE);

        content.addView(scoreCard());
        content.addView(highlightCard(popupOverlay));  // ✅ popupOverlay 전달
        content.addView(publicDataCard());
        root.addView(scroll);
        root.addView(bottomBar());

        frame.addView(popupOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return frame;
    }

    private View bottomSheet() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(64, 0, 0, 0));

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(12), dp(20), dp(20));
        sheet.setBackground(topRound(Color.WHITE, dp(20)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        overlay.addView(sheet, params);

        // ✅ 1. 배경(딤처리 영역) 터치 시 팝업 닫기
        overlay.setOnClickListener(v -> overlay.setVisibility(View.GONE));

        // ✅ 2. sheet 터치 시 배경 클릭 이벤트 차단 (sheet 클릭해도 안 닫히게)
        sheet.setOnClickListener(v -> { /* 이벤트 소비 */ });

        // ✅ 3. 드래그 다운 시 닫기
        final float[] startY = {0};
        sheet.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                startY[0] = event.getRawY();
            } else if (action == android.view.MotionEvent.ACTION_UP && event.getRawY() - startY[0] > dp(80)) {
                overlay.setVisibility(View.GONE);
            }
            return false;
        });

        // 핸들 바
        View handle = new View(this);
        handle.setBackground(roundRect(BORDER, dp(2), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, handleParams);
        bottomSheetSeverityPill = text("⚠ 주의 조항", WARNING_TEXT, 12, Typeface.BOLD);
        bottomSheetSeverityPill.setGravity(Gravity.CENTER);
        bottomSheetSeverityPill.setPadding(dp(12), dp(4), dp(12), dp(4));
        bottomSheetSeverityPill.setBackground(roundRect(Color.rgb(255, 248, 225), dp(12), 0, 0));
        sheet.addView(withTop(bottomSheetSeverityPill, 16));

        bottomSheetClauseTitle = text("", TEXT, 17, Typeface.BOLD);
        sheet.addView(withTop(bottomSheetClauseTitle, 10));

        bottomSheetClauseBody = text("", SECONDARY, 14, Typeface.NORMAL);
        bottomSheetClauseBody.setLineSpacing(dp(7), 1.0f);
        sheet.addView(withTop(bottomSheetClauseBody, 8));
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

    private void showClauseBottomSheet(ToxicClause tc, View overlay) {
        if (tc.isDanger()) {
            bottomSheetSeverityPill.setText("⚠ 위험 조항");
            bottomSheetSeverityPill.setTextColor(DANGER);
            bottomSheetSeverityPill.setBackground(roundRect(DANGER_TINT, dp(12), 0, 0));
        } else if (tc.isCaution()) {
            bottomSheetSeverityPill.setText("⚠ 주의 조항");
            bottomSheetSeverityPill.setTextColor(WARNING_TEXT);
            bottomSheetSeverityPill.setBackground(roundRect(Color.rgb(255, 248, 225), dp(12), 0, 0));
        } else {
            bottomSheetSeverityPill.setText("✔ 안전 조항");
            bottomSheetSeverityPill.setTextColor(SAFE);
            bottomSheetSeverityPill.setBackground(roundRect(SAFE_TINT, dp(12), 0, 0));
        }
        bottomSheetClauseTitle.setText(tc.clause);
        bottomSheetClauseBody.setText(tc.reason.isEmpty() ? tc.recommendation : tc.reason);
        overlay.setVisibility(View.VISIBLE);
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
        share.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportSummary.class);
            intent.putExtra(EXTRA_ANALYSIS_RESULT, getIntent().getStringExtra(EXTRA_ANALYSIS_RESULT));
            startActivity(intent);
        });
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
        String dateLabel = analysisDate.isEmpty() ? "분석 완료" : analysisDate + " 분석";
        header.addView(text(dateLabel, Color.rgb(158, 158, 158), 12, Typeface.NORMAL));

        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.setMargins(0, dp(16), 0, 0);
        card.addView(body, bodyParams);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        body.addView(left, new LinearLayout.LayoutParams(dp(136), ViewGroup.LayoutParams.WRAP_CONTENT));
        left.addView(new ScoreGauge(this, score, 120, 36, 14), new LinearLayout.LayoutParams(dp(120), dp(120)));
        boolean isDanger = riskLevel.contains("높");
        boolean isSafe = riskLevel.contains("낮");
        String pillLabel;
        int pillBg;
        int pillFg;
        if (isDanger) {
            pillLabel = "⚠ 위험"; pillBg = DANGER_TINT; pillFg = DANGER;
        } else if (isSafe) {
            pillLabel = "✔ 안전"; pillBg = SAFE_TINT; pillFg = SAFE;
        } else {
            pillLabel = "⚠ 주의 필요"; pillBg = Color.rgb(255, 248, 225); pillFg = WARNING_TEXT;
        }
        left.addView(pill(pillLabel, pillBg, pillFg, 12));

        LinearLayout counts = new LinearLayout(this);
        counts.setOrientation(LinearLayout.VERTICAL);
        body.addView(counts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        counts.addView(riskRow(DANGER, "위험", dangerCount + "건"));
        counts.addView(riskRow(WARNING, "주의", cautionCount + "건"));
        counts.addView(riskRow(SAFE, "안전", safeCount + "건"));
        return card;
    }

    private View highlightCard(View popupOverlay) {
        LinearLayout card = card(12);
        card.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setElevation(dp(2));
        card.addView(clauseCardHeader());
        addClauseRows(card, popupOverlay);
        return card;
    }

    private View clauseCardHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("독소조항 목록", TEXT, 16, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        int total = dangerCount + cautionCount;
        int countColor = total > 0 ? DANGER : SAFE;
        header.addView(text(total + "건 발견", countColor, 13, Typeface.BOLD));
        return header;
    }

    private void addClauseRows(LinearLayout card, View popupOverlay) {
        if (toxicClauses.isEmpty()) {
            String msg = summary.isEmpty() ? "독소조항이 발견되지 않았습니다." : summary;
            TextView empty = text(msg, SECONDARY, 14, Typeface.NORMAL);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.setMargins(0, dp(12), 0, 0);
            empty.setLineSpacing(dp(6), 1f);
            card.addView(empty, ep);
            return;
        }
        for (int i = 0; i < Math.min(toxicClauses.size(), 3); i++) {
            card.addView(clauseRow(toxicClauses.get(i), popupOverlay));
        }
        if (toxicClauses.size() > 3) {
            TextView more = text("+ " + (toxicClauses.size() - 3) + "건 더 보기  ›", PRIMARY, 13, Typeface.BOLD);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mp.setMargins(0, dp(10), 0, 0);
            more.setLayoutParams(mp);
            more.setOnClickListener(v -> showClauseBottomSheet(toxicClauses.get(3), popupOverlay));
            card.addView(more);
        }
    }

    private View clauseRow(ToxicClause tc, View popupOverlay) {
        int bg;
        int fg;
        if (tc.isDanger()) { bg = DANGER_TINT; fg = DANGER; }
        else if (tc.isCaution()) { bg = WARNING_TINT; fg = WARNING_TEXT; }
        else { bg = SAFE_TINT; fg = SAFE; }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(roundRect(bg, dp(8), 0, 0));
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> showClauseBottomSheet(tc, popupOverlay));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(params);

        TextView clauseText = text(tc.clause, fg, 13, Typeface.BOLD);
        clauseText.setMaxLines(2);
        row.addView(clauseText);

        if (!tc.reason.isEmpty()) {
            TextView reasonText = text(tc.reason, TEXT, 12, Typeface.NORMAL);
            reasonText.setMaxLines(2);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, dp(4), 0, 0);
            reasonText.setLayoutParams(rp);
            row.addView(reasonText);
        }
        return row;
    }

    private View publicDataCard() {
        LinearLayout card = card(12);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(2));
        card.addView(text("공공데이터 조회 결과", TEXT, 16, Typeface.BOLD));
        card.addView(buildingRow());
        card.addView(tradeRow());
        return card;
    }

    private View buildingRow() {
        if (!buildingInfoList.isEmpty()) {
            BuildingInfo b = buildingInfoList.get(0);
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
            return detailStatusRow("⌂", "건축물대장", "정상", detail.toString(), SAFE_TINT, SAFE);
        }
        return statusRow("⌂", "건축물대장", "미조회", Color.rgb(245, 245, 245), SECONDARY);
    }

    private View tradeRow() {
        if (!recentTrades.isEmpty()) {
            TradeInfo t = recentTrades.get(0);
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
            return detailStatusRow("₩", "실거래가", "조회됨", detail.toString(), SAFE_TINT, SAFE);
        }
        return statusRow("₩", "실거래가", "미조회", Color.rgb(245, 245, 245), SECONDARY);
    }

    private View detailStatusRow(String icon, String label, String badge, String detail,
                                 int badgeBg, int badgeColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(params);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(icon, PRIMARY, 18, Typeface.BOLD),
                new LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        top.addView(text(label, TEXT, 14, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(pill(badge, badgeBg, badgeColor, 12));
        row.addView(top);

        if (!detail.isEmpty()) {
            TextView detailView = text(detail, SECONDARY, 12, Typeface.NORMAL);
            detailView.setMaxLines(2);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subParams.setMargins(dp(30), dp(2), 0, dp(4));
            detailView.setLayoutParams(subParams);
            row.addView(detailView);
        }
        return row;
    }

    private View bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(16), dp(12), dp(16), dp(24));
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));
        TextView button = text("공유하기", Color.WHITE, 16, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PRIMARY, dp(12), 0, 0));
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportSummary.class);
            intent.putExtra(EXTRA_ANALYSIS_RESULT, getIntent().getStringExtra(EXTRA_ANALYSIS_RESULT));
            startActivity(intent);
        });
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
        private final int score;
        private final int size;
        private final int numberSize;
        private final int pointSize;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScoreGauge(Activity activity, int score, int size, int numberSize, int pointSize) {
            super(activity);
            this.score = score;
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
            if (score >= 70) arc.setColor(SAFE);
            else if (score >= 40) arc.setColor(WARNING);
            else arc.setColor(DANGER);
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
            canvas.drawArc(rect, -90, score * 3.6f, false, arc);
            canvas.drawText(String.valueOf(score), getWidth() / 2f, dp(size / 2f + numberSize / 3f), number);
            canvas.drawText("점", getWidth() / 2f, dp(size / 2f + 29), point);
        }
    }
}
