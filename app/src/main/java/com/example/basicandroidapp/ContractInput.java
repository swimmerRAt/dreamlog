package com.example.basicandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContractInput extends Activity {
    private static final int PRIMARY = Color.rgb(63, 81, 181);
    private static final int TEXT = Color.rgb(33, 33, 33);
    private static final int MUTED = Color.rgb(117, 117, 117);
    private static final int BORDER = Color.rgb(224, 224, 224);
    private static final int CAMERA_BG = Color.rgb(26, 26, 46);

    private Activity hostActivity;

    public ContractInput() {
        hostActivity = this;
    }

    ContractInput(Activity activity) {
        hostActivity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostActivity = this;
        setContentView(createView());
    }

    View createView() {
        LinearLayout root = new LinearLayout(hostActivity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CAMERA_BG);

        root.addView(createTopBar());
        root.addView(createCameraArea());
        root.addView(createActionSheet());
        return root;
    }

    private View createTopBar() {
        FrameLayout bar = new FrameLayout(hostActivity);
        bar.setBackgroundColor(Color.WHITE);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        TextView title = label("계약서 입력", TEXT, 17, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View divider = new View(hostActivity);
        divider.setBackgroundColor(BORDER);
        FrameLayout.LayoutParams dividerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1),
                Gravity.BOTTOM
        );
        bar.addView(divider, dividerParams);
        return bar;
    }

    private View createCameraArea() {
        FrameLayout cameraArea = new FrameLayout(hostActivity);
        cameraArea.setBackgroundColor(CAMERA_BG);
        cameraArea.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        TextView pill = label("계약서를 촬영하거나 불러오세요", PRIMARY, 14, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(18), dp(10), dp(18), dp(10));
        pill.setBackground(roundRect(Color.argb(230, 255, 255, 255), dp(20), 0, 0));
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        pillParams.topMargin = dp(16);
        cameraArea.addView(pill, pillParams);

        ScanGuideView scanGuide = new ScanGuideView(hostActivity);
        cameraArea.addView(scanGuide, new FrameLayout.LayoutParams(
                dp(260),
                dp(340),
                Gravity.CENTER
        ));
        return cameraArea;
    }

    private View createActionSheet() {
        LinearLayout sheet = new LinearLayout(hostActivity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setGravity(Gravity.CENTER_HORIZONTAL);
        sheet.setPadding(dp(16), dp(12), dp(16), dp(24));
        sheet.setBackground(topRoundRect(Color.WHITE, dp(20)));
        sheet.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
        ));

        View handle = new View(hostActivity);
        handle.setBackground(roundRect(BORDER, dp(2), 0, 0));
        sheet.addView(handle, new LinearLayout.LayoutParams(dp(36), dp(4)));

        LinearLayout buttons = new LinearLayout(hostActivity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
        );
        buttonsParams.setMargins(0, dp(16), 0, 0);
        buttons.setLayoutParams(buttonsParams);

        buttons.addView(createActionButton("촬영", true, 0));
        buttons.addView(createActionButton("갤러리", false, dp(10)));
        buttons.addView(createActionButton("직접 입력", false, dp(10)));
        sheet.addView(buttons);

        TextView caption = label("JPG · PNG · PDF 지원", MUTED, 11, Typeface.NORMAL);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        captionParams.setMargins(0, dp(8), 0, 0);
        sheet.addView(caption, captionParams);
        return sheet;
    }

    private View createActionButton(String label, boolean primary, int leftMargin) {
        TextView button = label(label, primary ? Color.WHITE : PRIMARY, 13, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(primary
                ? roundRect(PRIMARY, dp(10), 0, 0)
                : roundRect(Color.WHITE, dp(10), PRIMARY, Math.max(1, dp(1.5f))));
        button.setOnClickListener(view -> openOcrEditScreen());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        params.setMargins(leftMargin, 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void openOcrEditScreen() {
        Intent intent = new Intent(hostActivity, OcrEditScreen.class);
        intent.putExtra(OcrEditScreen.EXTRA_OCR_TEXT, OcrEditScreen.DEFAULT_CONTRACT_TEXT);
        hostActivity.startActivity(intent);
    }

    private TextView label(String value, int color, int size, int style) {
        TextView textView = new TextView(hostActivity);
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

    private GradientDrawable topRoundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        float r = radius;
        drawable.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return drawable;
    }

    private Typeface typeface(int style) {
        return Typeface.create("sans-serif", style);
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

    private class ScanGuideView extends View {
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bracketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ScanGuideView(Activity activity) {
            super(activity);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(PRIMARY);
            borderPaint.setStrokeWidth(dp(2));

            bracketPaint.setStyle(Paint.Style.STROKE);
            bracketPaint.setColor(Color.WHITE);
            bracketPaint.setStrokeWidth(dp(3));
            bracketPaint.setStrokeCap(Paint.Cap.SQUARE);

            textPaint.setColor(Color.argb(153, 255, 255, 255));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(typeface(Typeface.NORMAL));
            textPaint.setTextSize(sp(13));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = dp(2);
            float right = getWidth() - inset;
            float bottom = getHeight() - inset;
            float radius = dp(4);
            float bracket = dp(20);

            canvas.drawRoundRect(inset, inset, right, bottom, radius, radius, borderPaint);
            canvas.drawLine(inset, inset, inset + bracket, inset, bracketPaint);
            canvas.drawLine(inset, inset, inset, inset + bracket, bracketPaint);
            canvas.drawLine(right, inset, right - bracket, inset, bracketPaint);
            canvas.drawLine(right, inset, right, inset + bracket, bracketPaint);
            canvas.drawLine(inset, bottom, inset + bracket, bottom, bracketPaint);
            canvas.drawLine(inset, bottom, inset, bottom - bracket, bracketPaint);
            canvas.drawLine(right, bottom, right - bracket, bottom, bracketPaint);
            canvas.drawLine(right, bottom, right, bottom - bracket, bracketPaint);

            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float centerY = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText("계약서를 맞춰주세요", getWidth() / 2f, centerY, textPaint);
        }
    }
}
