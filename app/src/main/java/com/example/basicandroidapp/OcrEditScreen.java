package com.example.basicandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OcrEditScreen extends Activity {
    @Deprecated
    static final String EXTRA_OCR_TEXT = "ocr_text";
    static final String EXTRA_CONTRACT_TEXT = ApiContract.EXTRA_CONTRACT_TEXT;
    static final String DEFAULT_CONTRACT_TEXT =
            "전세 임대차계약서\n\n"
                    + "임대인 김OO과 임차인 이OO은 아래 부동산에 대하여 임대차계약을 체결한다.\n\n"
                    + "소재지: 서울시 마포구 아현동 OO아파트 101동 1203호\n"
                    + "보증금: 금 삼억 이천만원정\n"
                    + "계약 기간은 2024년 1월 15일부터 2026년 1월 14일까지 2년으로 한다.\n\n"
                    + "임대인은 별도 통보 없이 계약을 해지할 수 있다.\n"
                    + "전세금 반환이 지연되는 경우 임대인은 책임을 면할 수 있다.\n"
                    + "수리비 일체는 임차인이 부담한다.";

    private static final int BACKGROUND = Color.rgb(248, 249, 250);
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int SECONDARY = Color.rgb(117, 117, 117);
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int WARNING_TINT = Color.rgb(255, 248, 225);
    private static final int WARNING_BAR = Color.rgb(255, 193, 7);
    private static final int WARNING_TEXT = Color.rgb(121, 85, 72);
    private static final int INDIGO_TINT = Color.rgb(232, 234, 246);
    private static final int COUNT_TEXT = Color.rgb(158, 158, 158);

    private EditText contractTextInput;
    private TextView countText;

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
        root.addView(createProgressBar(0.4f));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, dp(18));
        scroll.addView(content);

        TextView subtitle = text("OCR로 추출된 텍스트를 확인하고\n틀린 부분은 직접 수정하세요", SECONDARY, 14, Typeface.NORMAL);
        subtitle.setLineSpacing(0, 1.6f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(dp(20), 0, dp(20), dp(12));
        content.addView(subtitle, subtitleParams);

        content.addView(createWarningBanner());
        content.addView(createTextCard());
        content.addView(createKeywordArea());

        root.addView(scroll);
        root.addView(createBottomBar());
        return root;
    }

    private View createTopBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        TextView back = text("‹", TEXT, 36, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        TextView title = text("텍스트 확인", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView step = text("2 / 5", PRIMARY, 12, Typeface.BOLD);
        step.setGravity(Gravity.CENTER);
        step.setPadding(dp(10), dp(4), dp(10), dp(4));
        step.setBackground(roundRect(INDIGO_TINT, dp(12), 0, 0));
        FrameLayout.LayoutParams stepParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(28),
                Gravity.END | Gravity.CENTER_VERTICAL
        );
        stepParams.rightMargin = dp(16);
        bar.addView(step, stepParams);

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        bar.addView(divider, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.BOTTOM));
        return bar;
    }

    private View createWarningBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackground(roundRect(WARNING_TINT, dp(10), 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), 0, dp(16), dp(12));
        banner.setLayoutParams(params);

        View accent = new View(this);
        accent.setBackgroundColor(WARNING_BAR);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
        accentParams.setMargins(0, dp(12), 0, dp(12));
        banner.addView(accent, accentParams);

        TextView message = text("⚠️  인식이 불정확한 부분은 수정 후 분석을 시작하세요", WARNING_TEXT, 13, Typeface.NORMAL);
        message.setPadding(dp(11), dp(12), dp(14), dp(12));
        banner.addView(message, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return banner;
    }

    private View createTextCard() {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(roundRect(Color.WHITE, dp(12), BORDER, Math.max(1, dp(1.5f))));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320));
        params.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(params);

        contractTextInput = new EditText(this);
        contractTextInput.setText(getInitialText());
        contractTextInput.setSelection(contractTextInput.length());
        contractTextInput.setTextColor(TEXT);
        contractTextInput.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14));
        contractTextInput.setTypeface(typeface(Typeface.NORMAL));
        contractTextInput.setLineSpacing(0, 1.8f);
        contractTextInput.setGravity(Gravity.TOP | Gravity.START);
        contractTextInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        contractTextInput.setSingleLine(false);
        contractTextInput.setVerticalScrollBarEnabled(true);
        contractTextInput.setBackgroundColor(Color.TRANSPARENT);
        contractTextInput.setPadding(0, 0, 0, dp(26));
        card.addView(contractTextInput, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        countText = text(characterCountText(), COUNT_TEXT, 12, Typeface.NORMAL);
        countText.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams countParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM
        );
        card.addView(countText, countParams);

        contractTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                countText.setText(characterCountText());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return card;
    }

    private View createKeywordArea() {
        LinearLayout area = new LinearLayout(this);
        area.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(16), 0, dp(16), 0);
        area.setLayoutParams(params);

        TextView label = text("자주 쓰는 조항 추가", SECONDARY, 12, Typeface.NORMAL);
        area.addView(label);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);

        String[] chips = {"보증금", "월세", "계약 기간", "특약사항", "수리비"};
        for (String chip : chips) {
            row.addView(chip(chip));
        }
        scroll.addView(row);
        area.addView(scroll);
        return area;
    }

    private View createBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(16), dp(12), dp(16), dp(24));
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));

        TextView button = text("분석 시작하기  →", Color.WHITE, 16, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PRIMARY, dp(12), 0, 0));
        button.setOnClickListener(view -> {
            Intent intent = new Intent(OcrEditScreen.this, AnalysisResultScreen.class);
            intent.putExtra(EXTRA_CONTRACT_TEXT, contractTextInput.getText().toString());
            startActivity(intent);
        });
        bar.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return bar;
    }

    private View chip(String value) {
        TextView chip = text(value, PRIMARY, 12, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));
        chip.setBackground(roundRect(INDIGO_TINT, dp(16), PRIMARY, dp(1)));
        chip.setOnClickListener(view -> insertClause(value));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private void insertClause(String value) {
        int start = Math.max(contractTextInput.getSelectionStart(), 0);
        String insertion = start > 0 ? "\n" + value + ": " : value + ": ";
        contractTextInput.getText().insert(start, insertion);
        contractTextInput.requestFocus();
    }

    private View createProgressBar(float ratio) {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(BORDER);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));

        View fill = new View(this);
        fill.setBackgroundColor(PRIMARY);
        bar.addView(fill, new FrameLayout.LayoutParams(dp(375 * ratio), ViewGroup.LayoutParams.MATCH_PARENT));
        return bar;
    }

    private String getInitialText() {
        String value = getIntent().getStringExtra(EXTRA_CONTRACT_TEXT);
        if (value == null) {
            value = getIntent().getStringExtra(EXTRA_OCR_TEXT);
        }
        return value == null || value.trim().isEmpty() ? DEFAULT_CONTRACT_TEXT : value;
    }

    private String characterCountText() {
        return (contractTextInput == null ? DEFAULT_CONTRACT_TEXT.length() : contractTextInput.length()) + "자";
    }

    private TextView text(String value, int color, int size, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(size));
        textView.setTypeface(typeface(style));
        return textView;
    }

    private Typeface typeface(int style) {
        return Typeface.create("Noto Sans KR", style);
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
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
