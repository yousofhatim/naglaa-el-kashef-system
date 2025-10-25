package com.example.f_puser;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class smartAssistant extends AppCompatActivity {
    private String mainCollection = "1";


    private String stage = "idle";
    private String complaintStage = null;
    private String currentComplaintId = null;

    private int currentQuestionIndex = 0;
    private Map<String, Object> applicationData = new HashMap<>();

    private LinearLayout chatBoxLayout;
    private List<Map<String, Object>> questions = new ArrayList<>();

    private boolean isHeaderShrunk = false;
    private boolean isHeaderAnimating = false;
    private String mainActivity2 = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_assistant);
        DatabaseReference Activity = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("splashActivity")
                .child("mainActivity");

        Activity.get().addOnSuccessListener(snapshot -> {
            String key = snapshot.getValue(String.class);
            if (key != null && !key.isEmpty()) {
                mainActivity2 = key;
            } else {
                mainActivity2 = "";
            }
        }).addOnFailureListener(e -> {
            mainActivity2 = "";
        });



        // الجذر
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.background);

        // الترويسة
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(20, 20, 20, 20);

        // داخلي: شعار + عنوان
        LinearLayout headerInner = new LinearLayout(this);
        headerInner.setOrientation(LinearLayout.VERTICAL);
        headerInner.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.chatgpt_logo);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(100, 100);
        logo.setLayoutParams(logoParams);

        TextView title = new TextView(this);
        title.setText("نجلاء الكاشف-GPT");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 0);

        headerInner.addView(logo);
        headerInner.addView(title);
        header.addView(headerInner);

        // صندوق الشات
        NestedScrollView scrollView = new NestedScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1));

        LinearLayout chatBox = new LinearLayout(this);
        chatBox.setOrientation(LinearLayout.VERTICAL);
        chatBox.setPadding(20, 20, 20, 20);
        chatBoxLayout = chatBox;


        scrollView.addView(chatBox);

        // إدخال المستخدم
        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.HORIZONTAL);
        inputContainer.setPadding(20, 10, 20, 10);
        inputContainer.setGravity(Gravity.CENTER_VERTICAL);
        inputContainer.setBackgroundColor(Color.parseColor("#1e1e1e"));

        EditText input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setBackgroundColor(Color.parseColor("#2a2f32"));
        input.setPadding(30, 20, 30, 20);
        input.setTextSize(16);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        input.setLayoutParams(inputParams);
        input.setBackground(getResources().getDrawable(R.drawable.rounded_input));

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(Color.WHITE);
        send.setTextSize(14);
        send.setBackgroundColor(Color.parseColor("#056162"));
        send.setBackground(getResources().getDrawable(R.drawable.rounded_button));

        inputContainer.addView(send);
        inputContainer.addView(input);

        container.addView(header);
        container.addView(scrollView);
        container.addView(inputContainer);

        setContentView(container);

        // انكماش الشعار
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();

            // Thresholds
            int shrinkThreshold = 200; // كان 100، كبرناه
            int expandThreshold = 80;  // خليته أقل شوية من shrink

            if (scrollY > shrinkThreshold && !isHeaderShrunk && !isHeaderAnimating) {
                isHeaderAnimating = true;
                animateShrink(headerInner, logo, title, () -> {
                    isHeaderShrunk = true;
                    isHeaderAnimating = false;
                });
            } else if (scrollY < expandThreshold && isHeaderShrunk && !isHeaderAnimating) {
                isHeaderAnimating = true;
                animateExpand(headerInner, logo, title, () -> {
                    isHeaderShrunk = false;
                    isHeaderAnimating = false;
                });
            }
        });


        final boolean isFirstTime = getIntent().getBooleanExtra("firstTime", false);
        final String versionKey = getIntent().getStringExtra("versionKey");

        if (isFirstTime) {
            // تعطيل الإرسال والكتابة
            input.setVisibility(View.INVISIBLE);
            send.setVisibility(View.INVISIBLE);
            input.setAlpha(0.4f); // شكل باهت
            send.setAlpha(0.4f);

            DatabaseReference db = FirebaseDatabase.getInstance().getReference(mainCollection);
            db.child("Prompt").child("newVersionMessage").get().addOnSuccessListener(snapshot -> {
                String firstMsg = snapshot.getValue(String.class);
                if (firstMsg != null && !firstMsg.isEmpty()) {
                    appendMessage(firstMsg, "bot", true, chatBoxLayout);
                } else {
                    appendMessage("مرحبًا بك في أول استخدام للتطبيق!", "bot", true, chatBoxLayout);
                }

                Button goNextButton = new Button(this);
                goNextButton.setText("استمرار إلى التطبيق");
                goNextButton.setTextColor(Color.WHITE);
                goNextButton.setTextSize(16);
                goNextButton.setBackgroundColor(Color.parseColor("#00897B"));
                goNextButton.setBackground(getResources().getDrawable(R.drawable.rounded_button));
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
                buttonParams.setMargins(0, 30, 0, 0);
                goNextButton.setLayoutParams(buttonParams);

                chatBoxLayout.addView(goNextButton);

                goNextButton.setOnClickListener(v -> {
                    // أول حاجة: نحدث القيمة
                    if (versionKey != null) {
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putBoolean(versionKey, false).apply();
                    }


                    // بعدين نرجع لـ Splash
                    startActivity(new Intent(smartAssistant.this, SplashActivity.class));
                    finish();
                });

            });
        }

        send.setOnClickListener(v -> {
            sendMessage(input, chatBoxLayout, scrollView);
        });


    }

    private void animateShrink(LinearLayout layout, ImageView logo, TextView title, Runnable onEnd) {
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        logo.animate().scaleX(0.8f).scaleY(0.8f).setDuration(200).withEndAction(() -> {
            logo.getLayoutParams().height = 80;  // بدل 60
            logo.getLayoutParams().width = 80;   // بدل 60
            logo.requestLayout();

            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);  // بدل 14
            layout.requestLayout();

            if (onEnd != null) onEnd.run();
        }).start();

    }

    private void animateExpand(LinearLayout layout, ImageView logo, TextView title, Runnable onEnd) {
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        logo.animate().scaleX(1f).scaleY(1f).setDuration(200).withEndAction(() -> {
            logo.getLayoutParams().height = 100;
            logo.getLayoutParams().width = 100;
            logo.requestLayout();

            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            layout.requestLayout();

            if (onEnd != null) onEnd.run();
        }).start();
    }


    public void updateHeaderState(ScrollView chatScrollView, View headerView) {
        int scrollY = chatScrollView.getScrollY();
        int viewHeight = chatScrollView.getHeight();
        int contentHeight = chatScrollView.getChildAt(0).getHeight();

        int distanceFromBottom = contentHeight - (scrollY + viewHeight);

        if (distanceFromBottom > 100) {
            // صغّر الهيدر
            headerView.animate().scaleX(0.8f).scaleY(0.8f).setDuration(300).start();
        } else {
            // رجّع الهيدر لحجمه الطبيعي
            headerView.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        }
    }


    public void appendMessage(String text, String className, boolean typing, LinearLayout chatBoxLayout) {
        final TextView msg = new TextView(this);
        msg.setTextSize(16);
        msg.setPadding(30, 20, 30, 20);
        msg.setTextColor(Color.WHITE);
        msg.setText("");
        msg.setLineSpacing(1.2f, 1.2f);

        // عرض الرسالة بنسبة عرض مناسبة
        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        msg.setMaxWidth(maxWidth);

        // فقاقيع الرسائل
        GradientDrawable bubble = new GradientDrawable();
        bubble.setCornerRadius(40);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10); // تباعد بين الرسائل

        if (className.equals("user")) {
            bubble.setColor(Color.parseColor("#056162"));
            bubble.setCornerRadii(new float[]{40, 40, 0, 40, 40, 40, 40, 40}); // الحافة الشمال تحت
            params.gravity = Gravity.START;
        } else {
            bubble.setColor(Color.parseColor("#262d31"));
            bubble.setCornerRadii(new float[]{0, 40, 40, 40, 40, 40, 40, 40}); // الحافة اليمين تحت
            params.gravity = Gravity.END;
        }

        msg.setLayoutParams(params);
        msg.setBackground(bubble);
        chatBoxLayout.addView(msg);

        // أنيميشن تكبير تدريجي
        msg.setScaleX(0f);
        msg.setScaleY(0f);
        msg.animate().scaleX(1f).scaleY(1f).setDuration(300).start();

        // أنيميشن كتابة الحروف
        final Handler handler = new Handler();
        final int[] index = {0};
        Runnable characterAdder = new Runnable() {
            @Override
            public void run() {
                if (index[0] <= text.length()) {
                    msg.setText(text.substring(0, index[0]));
                    index[0]++;

                    // scroll في كل مرة بيتضاف حرف
                    chatBoxLayout.post(() -> {
                        View parent = (View) chatBoxLayout.getParent();
                        if (parent instanceof ScrollView) {
                            ((ScrollView) parent).fullScroll(View.FOCUS_DOWN);
                        } else if (parent instanceof NestedScrollView) {
                            ((NestedScrollView) parent).fullScroll(View.FOCUS_DOWN);
                        }
                    });

                    handler.postDelayed(this, 15);
                }
            }
        };
        handler.postDelayed(characterAdder, 20);
    }


    public void scrollToBottom(NestedScrollView chatScrollView) {
        chatScrollView.post(() -> {
            chatScrollView.fullScroll(View.FOCUS_DOWN);
        });
    }


    public void sendQuickQuestion() {
        // TODO: send predefined question
    }

    public void sendMessage(EditText userInput, LinearLayout chatBoxLayout, NestedScrollView chatScrollView) {
        String message = userInput.getText().toString().trim();
        if (message.isEmpty()) return;

        appendMessage(message, "user", false, chatBoxLayout);
        userInput.setText("");
        scrollToBottom(chatScrollView);
        handleMessage(message);
    }


    public void handleMessage(String message) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference(mainCollection);

        db.child("autoChatOn").get().addOnSuccessListener(snapshot -> {
            boolean isActive = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));

            if (!isActive) {
                appendMessage("ورديتي خلصت مش هرد دلوقتي", "bot", true, chatBoxLayout);
                return;
            }

            // مراحل الشكوى
            if ("awaitingStudentName".equals(complaintStage)) {
                db.child("FeedbackCritical").child(currentComplaintId).child("studentName").setValue(message);
                appendMessage("يرجى كتابة رقم هاتف ولي الأمر للتواصل.", "bot", true, chatBoxLayout);
                complaintStage = "awaitingPhone";
                return;
            }

            if ("awaitingPhone".equals(complaintStage)) {
                db.child("FeedbackCritical").child(currentComplaintId).child("parentPhone").setValue(message);
                appendMessage("✅ تم تسجيل الشكوى بالكامل، وسيتم التواصل معكم قريبًا من إدارة المؤسسة.", "bot", true, chatBoxLayout);
                complaintStage = null;
                currentComplaintId = null;
                return;
            }

            // استدعاء GPT
            fetchAIReply(message, reply -> {
                if (reply.toLowerCase().contains("تقصير::")) {
                    appendMessage("نأسف على ما حدث، وتم تصعيد الأمر إلى إدارة المؤسسة لمراجعته.", "bot", true, chatBoxLayout);
                    appendMessage("يرجى كتابة اسم الطالب المتضرر.", "bot", true, chatBoxLayout);

                    long complaintId = System.currentTimeMillis();
                    currentComplaintId = String.valueOf(complaintId);
                    complaintStage = "awaitingStudentName";

                    Map<String, Object> complaintData = new HashMap<>();
                    complaintData.put("complaintMessage", message);
                    complaintData.put("date", new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
                    complaintData.put("time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                    complaintData.put("status", "pending");

                    db.child("FeedbackCritical").child(currentComplaintId).setValue(complaintData);
                    return;
                }

                if (reply.toLowerCase().contains("استفسار::")) {
                    appendMessage("احييك سؤالك مش عارف اجابتة و عشان كده هرجع للإدارة و اسألهم عن الموضوع ده...", "bot", true, chatBoxLayout);

                    db.child("NewQuestions").get().addOnSuccessListener(snapshot1 -> {
                        int count = (int) snapshot1.getChildrenCount() + 1;
                        db.child("NewQuestions").child(String.valueOf(count)).setValue(message);
                    });
                    return;
                }

                if (reply.toLowerCase().contains("اشتراك::")) {
                    reply = reply.replace("اشتراك::", "").trim();
                    appendMessage(reply, "bot", true, chatBoxLayout);
                    appendMessage("هيكون مطلوب تجهز جنبك شهادة الميلاد والبطاقات...", "bot", true, chatBoxLayout);
                    if (!"check-docs".equals(stage) && !"application".equals(stage)) {
                        stage = "check-docs";
                    }
                    return;
                }

                if (reply.toLowerCase().contains("الورق::")) {
                    if (reply.toLowerCase().contains("الورق::لا")) {
                        stage = "idle";
                        appendMessage("الأوراق المطلوبة: شهادة ميلاد، الرقم القومي للأب والأم، صور شخصية للطفل، إثبات وظيفة، أرقام تواصل.", "bot", true, chatBoxLayout);
                    } else if (reply.toLowerCase().contains("الورق::نعم")) {
                        stage = "application";
                        currentQuestionIndex = 0;
                        applicationData.clear();
                        askNextQuestion();
                    }
                    return;
                }

                if ("application".equals(stage)) {
                    Map<String, Object> currentQ = questions.get(currentQuestionIndex);
                    String key = (String) currentQ.get("key");
                    applicationData.put(key, message);
                    currentQuestionIndex++;

                    if (currentQuestionIndex < questions.size()) {
                        askNextQuestion();
                    } else {
                        saveApplication();
                    }
                } else {
                    appendMessage(reply, "bot", true, chatBoxLayout);
                }
            });

        });
    }

    private interface ReplyCallback {
        void onReply(String reply);
    }


    public void askNextQuestion() {
        Map<String, Object> q = questions.get(currentQuestionIndex);
        String text = (String) q.get("text");
        appendMessage(text, "bot", true, chatBoxLayout);
    }


    public void saveApplication() {
        appendMessage("✅ تم استلام البيانات. سيتم مراجعتها من إدارة المؤسسة.", "bot", true, chatBoxLayout);
        stage = "idle";
    }


    public void registerStudent(String entryId) {
        // TODO: register student in database
    }

    private void fetchAIReply(String message, ReplyCallback callback) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference(mainCollection);

        db.child("Prompt").child("apiPrompt").get().addOnSuccessListener(snapshot -> {
            String systemMessage = snapshot.getValue(String.class);
            if (systemMessage == null) systemMessage = "أهلاً بيك";

            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-4o");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemMessage));
                messages.put(new JSONObject().put("role", "user").put("content", message));

                requestBody.put("messages", messages);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + mainActivity2)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onReply("في مشكلة في الرد التلقائي، جرب تاني كمان شوية."));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String res = response.body().string();
                        try {
                            JSONObject obj = new JSONObject(res);
                            String content = obj.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onReply(content.trim()));
                        } catch (Exception ex) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onReply("معرفتش أرد دلوقتي، جرب تاني."));
                        }
                    }
                });

            } catch (JSONException e) {
                callback.onReply("حصلت مشكلة في تجهيز الرسالة.");
            }
        });
    }


    public void onCameraClick() {
        // TODO: open camera intent
    }

    public void onImageSelected() {
        // TODO: handle selected image
    }

    public void onConfirmClick() {
        // TODO: upload image to Firebase
    }

    public void onRetryClick() {
        // TODO: retry image selection
    }

    public void customAppendMessage(String text, String className, boolean typing) {
        // TODO: override default appendMessage with special behavior
    }

}
