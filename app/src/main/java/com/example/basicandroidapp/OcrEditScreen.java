package com.example.basicandroidapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OcrEditScreen extends Activity {
    static final String EXTRA_CONTRACT_TEXT = ApiContract.EXTRA_CONTRACT_TEXT;
    static final String EXTRA_MODE = "mode";
    static final String MODE_CAMERA = "camera";
    static final String MODE_GALLERY = "gallery";
    static final String MODE_TEXT = "text";

    static final String DEFAULT_CONTRACT_TEXT =
            "전세 임대차계약서\n\n"
                    + "임대인 김OO과 임차인 이OO은 아래 부동산에 대하여 임대차계약을 체결한다.\n\n"
                    + "소재지: 서울시 마포구 아현동 OO아파트 101동 1203호\n"
                    + "보증금: 금 삼억 이천만원정\n"
                    + "계약 기간은 2024년 1월 15일부터 2026년 1월 14일까지 2년으로 한다.\n\n"
                    + "임대인은 별도 통보 없이 계약을 해지할 수 있다.\n"
                    + "전세금 반환이 지연되는 경우 임대인은 책임을 면할 수 있다.\n"
                    + "수리비 일체는 임차인이 부담한다.";

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final int REQUEST_CAMERA = 2001;
    private static final int REQUEST_GALLERY = 2002;
    private static final int REQUEST_CAMERA_PERM = 3001;

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

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private EditText contractTextInput;
    private TextView countText;
    private FrameLayout loadingOverlay;
    private Uri cameraImageUri;
    private String cachedOcrText = null;
    private String cachedAnalysisJson = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createView());

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_CAMERA.equals(mode)) {
            launchCamera();
        } else if (MODE_GALLERY.equals(mode)) {
            launchGallery();
        }
    }

    // ──────────────────────── 카메라 / 갤러리 ────────────────────────

    private void launchCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERM);
            return;
        }
        try {
            File imagesDir = new File(getCacheDir(), "images");
            imagesDir.mkdirs();
            File imageFile = new File(imagesDir, "contract_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "카메라를 실행할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERM
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Uri imageUri = null;
        if (requestCode == REQUEST_CAMERA) {
            imageUri = cameraImageUri;
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            imageUri = data.getData();
        }

        if (imageUri != null) {
            uploadImageForOcr(imageUri);
        }
    }

    // ──────────────────────── API 호출 ────────────────────────

    private void uploadImageForOcr(Uri imageUri) {
        showLoading(true);
        contractTextInput.setText("OCR 처리 중...");
        contractTextInput.setEnabled(false);

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) {
                    runOnUiThread(() -> showError("이미지를 읽을 수 없습니다."));
                    return;
                }
                byte[] imageBytes = toBytes(inputStream);

                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) mimeType = "image/jpeg";

                RequestBody fileBody = RequestBody.create(imageBytes, MediaType.parse(mimeType));
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "contract.jpg", fileBody)
                        .build();

                String token = getToken();
                Request request = new Request.Builder()
                        .url(ApiContract.BASE_URL + "/contract/analyze")
                        .addHeader("Authorization", "Bearer " + token)
                        .post(requestBody)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String ocrText = json.optString("original_text", "");
                        cachedOcrText = ocrText;
                        cachedAnalysisJson = responseBody;
                        runOnUiThread(() -> {
                            showLoading(false);
                            contractTextInput.setEnabled(true);
                            contractTextInput.setText(ocrText);
                            contractTextInput.setSelection(ocrText.length());
                        });
                    } else {
                        runOnUiThread(() -> showError("OCR 처리에 실패했습니다."));
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> showError("서버에 연결할 수 없습니다."));
            } catch (Exception e) {
                runOnUiThread(() -> showError("오류가 발생했습니다."));
            }
        }).start();
    }

    private static byte[] toBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }

    private void analyzeText() {
        String contractText = contractTextInput.getText().toString().trim();
        if (contractText.isEmpty()) {
            Toast.makeText(this, "계약서 텍스트를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cachedAnalysisJson != null && contractText.equals(cachedOcrText)) {
            Intent intent = new Intent(this, AnalysisResultScreen.class);
            intent.putExtra(EXTRA_CONTRACT_TEXT, contractText);
            intent.putExtra(AnalysisResultScreen.EXTRA_ANALYSIS_RESULT, cachedAnalysisJson);
            startActivity(intent);
            return;
        }

        showLoading(true);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("text", contractText);

                String token = getToken();
                Request request = new Request.Builder()
                        .url(ApiContract.BASE_URL + "/contract/analyze-text")
                        .addHeader("Authorization", "Bearer " + token)
                        .post(RequestBody.create(body.toString(), JSON_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        String analysisJson = responseBody;
                        runOnUiThread(() -> {
                            showLoading(false);
                            Intent intent = new Intent(OcrEditScreen.this, AnalysisResultScreen.class);
                            intent.putExtra(EXTRA_CONTRACT_TEXT, contractText);
                            intent.putExtra(AnalysisResultScreen.EXTRA_ANALYSIS_RESULT, analysisJson);
                            startActivity(intent);
                        });
                    } else {
                        runOnUiThread(() -> showError("분석에 실패했습니다."));
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> showError("서버에 연결할 수 없습니다."));
            } catch (Exception e) {
                runOnUiThread(() -> showError("오류가 발생했습니다."));
            }
        }).start();
    }

    private String getToken() {
        return getSharedPreferences(Login.PREFS_NAME, MODE_PRIVATE)
                .getString(Login.KEY_TOKEN, "");
    }

    // ──────────────────────── UI 헬퍼 ────────────────────────

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        showLoading(false);
        contractTextInput.setEnabled(true);
        contractTextInput.setText("");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ──────────────────────── View 생성 ────────────────────────

    private View createView() {
        FrameLayout frame = new FrameLayout(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        frame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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

        // 로딩 오버레이
        loadingOverlay = new FrameLayout(this);
        loadingOverlay.setBackgroundColor(Color.argb(128, 0, 0, 0));
        loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.setClickable(true);

        ProgressBar spinner = new ProgressBar(this);
        loadingOverlay.addView(spinner, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));

        frame.addView(loadingOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        return frame;
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
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28), Gravity.END | Gravity.CENTER_VERTICAL);
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
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        card.addView(contractTextInput, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        countText = text(characterCountText(), COUNT_TEXT, 12, Typeface.NORMAL);
        countText.setGravity(Gravity.CENTER);
        card.addView(countText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM));

        contractTextInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* no-op */ }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                countText.setText(characterCountText());
            }
            @Override public void afterTextChanged(Editable s) { /* no-op */ }
        });
        return card;
    }

    private View createKeywordArea() {
        LinearLayout area = new LinearLayout(this);
        area.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(16), 0, dp(16), 0);
        area.setLayoutParams(params);

        area.addView(text("자주 쓰는 조항 추가", SECONDARY, 12, Typeface.NORMAL));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);

        for (String chip : new String[]{"보증금", "월세", "계약 기간", "특약사항", "수리비"}) {
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
        button.setOnClickListener(view -> analyzeText());
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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_CAMERA.equals(mode) || MODE_GALLERY.equals(mode)) {
            return "";
        }
        String value = getIntent().getStringExtra(EXTRA_CONTRACT_TEXT);
        return value == null || value.trim().isEmpty() ? DEFAULT_CONTRACT_TEXT : value;
    }

    private String characterCountText() {
        return (contractTextInput == null ? 0 : contractTextInput.length()) + "자";
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
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
