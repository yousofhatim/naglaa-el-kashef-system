package com.example.f_puser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


public class payPeriod extends AppCompatActivity {
    private String mainCollection = "1";

    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView responseTextView;
    private EditText manualInput;
    private Button confirmBtn;
    private Uri imageUri;
    private String kidId;
    private byte[] imageBytes;
    private Map<String, String> paymentData = new HashMap<>();
    private TextView invoiceAmountView;
    private TextView paymentNumberView;
    private String[] missingFields = new String[2];
    private int missingFieldIndex = 0;


    // حط بدلها
    private String mainActivity = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_period);
        DatabaseReference Activity = FirebaseDatabase.getInstance()
                .getReference()
                .child(mainCollection)
                .child("splashActivity")
                .child("mainActivity");

        Activity.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String apiKey = snapshot.getValue(String.class);
                if (apiKey != null && !apiKey.isEmpty()) {
                    mainActivity = apiKey;
                    Log.d("API_KEY", "تم جلب الـ API Key بنجاح");
                } else {
                    Toast.makeText(payPeriod.this, "API Key غير موجود في Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(payPeriod.this, "فشل تحميل الـ API Key: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        responseTextView = findViewById(R.id.responseTextView);
        manualInput = findViewById(R.id.manualInput);
        Button selectImageBtn = findViewById(R.id.selectImageBtn);
        confirmBtn = findViewById(R.id.confirmBtn);
        invoiceAmountView = findViewById(R.id.invoiceAmount);
        paymentNumberView = findViewById(R.id.paymentNumber);
        manualInput.setVisibility(View.INVISIBLE);
        confirmBtn.setVisibility(View.INVISIBLE);


        TextView studentNameView = findViewById(R.id.studentName);

        Button copyBtn = findViewById(R.id.copyPaymentNumber);

        kidId = getIntent().getStringExtra("kidId");

        if (kidId == null || kidId.isEmpty()) {
            // لو مش جاي من الـ Intent، نحاول ناخده من SharedPreferences كـ fallback
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            kidId = prefs.getString("kidId", null);
        }

//        mainCollection = prefs.getString("mainCollection", "");

        if (kidId == null) {
            Toast.makeText(this, "kidId غير موجود.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // تحميل البيانات من Firebase
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference()
                .child(mainCollection)
                .child("data")
                .child(kidId);

        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String kidName = snapshot.child("kidName").getValue(String.class);
                String invoice = snapshot.child("invoice").getValue(String.class);
                String payNum = snapshot.child("payNum").getValue(String.class);

                studentNameView.setText("اسم الطالب: " + (kidName != null ? kidName : "-"));
                invoiceAmountView.setText("المبلغ: " + (invoice != null ? invoice : "-"));
                paymentNumberView.setText("رقم الدفع: " + (payNum != null ? payNum : "-"));

                copyBtn.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("payNum", payNum);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(payPeriod.this, "تم نسخ رقم الدفع", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(payPeriod.this, "فشل تحميل البيانات: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // عرض بيانات GPT فوق بعد شوية

        selectImageBtn.setOnClickListener(v -> openGallery());

//        confirmBtn.setOnClickListener(v -> {
//            String manual = manualInput.getText().toString().trim();
//
//            if (missingFieldIndex < missingFields.length) {
//                String currentField = missingFields[missingFieldIndex];
//
//                if (!manual.isEmpty()) {
//                    paymentData.put(currentField, manual);
//                    missingFieldIndex++;
//                    manualInput.setText(""); // فضي الخانة
//
//                    if (missingFieldIndex < missingFields.length) {
////                        setManualHint();
//                    } else {
//                        uploadImageAndStoreData(); // خلصنا
//                    }
//                } else {
//                    Toast.makeText(this, "من فضلك اكتب البيانات المطلوبة", Toast.LENGTH_SHORT).show();
//                }
//
//            } else {
//                // لو كل حاجة جاهزة
//                if (paymentData.containsKey("amount") && paymentData.containsKey("date") && paymentData.containsKey("sender")) {
//                    uploadImageAndStoreData();
//                } else {
//                    Toast.makeText(this, "البيانات غير مكتملة.", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });


//        new android.os.Handler().postDelayed(() -> {
//            String amount = paymentData.getOrDefault("amount", "-");
//            String sender = paymentData.getOrDefault("sender", "-");
//
//        }, 30000);
    }



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                InputStream iStream = getContentResolver().openInputStream(imageUri);
                imageBytes = getBytes(iStream);
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                sendBase64ImageToGPT(base64Image);
            } catch (Exception e) {
                responseTextView.setText("فشل في قراءة الصورة: " + e.getMessage());
            }
        }
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void sendBase64ImageToGPT(String base64Image) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + mainActivity);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // إعداد الجسم
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-4o");
                requestBody.put("max_tokens", 500);

                JSONArray contentArray = new JSONArray();

                JSONObject imageObject = new JSONObject();
                imageObject.put("type", "image_url");
                JSONObject imageData = new JSONObject();
                imageData.put("url", "data:image/jpeg;base64," + base64Image);
                imageObject.put("image_url", imageData);

                JSONObject textObject = new JSONObject();
                textObject.put("type", "text");
                textObject.put("text", "هل هذه صورة إيصال دفع؟ لو كانت الصورة رسالة SMS، أو نافذة منبثقة USSD، أو صورة من شاشة موبايل، أو أي إشعار مالي به مبلغ تم تحويله ورقم مرسل إليه، اعتبرها إيصال دفع صحيح، استخرج فقط التالي: المبلغ المدفوع، التاريخ، إثبات هوية المرسل يجب أن يكون رقم هاتف أو حساب إنستا باي فقط، وإثبات هوية المرسل إليه يجب أن يكون رقم هاتف، لو البيانات كلها واضحة وكاملة اكتب 1(هل هذه صورة إيصال دفع)<القيمة> 1(المبلغ)<القيمة> 1(التاريخ)<القيمة> اكتب التاريخ بصيغة am,pm-mm-hh-dd-mm-yyyy 1(إثبات هوية المرسل)<القيمة> 1(إثبات هوية المرسل إليه)<القيمة>، أما لو في أي معلومة ناقصة أو غير واضحة أو مش موجودة اكتب 0(اسم البيان)<سبب أو فراغ>، مهم جداً: أي بيان غير معروف أو غير متوفر اعتبره ناقص واكتبه بصيغة 0.");
                contentArray.put(imageObject);
                contentArray.put(textObject);

                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", contentArray);
                messages.put(message);

                requestBody.put("messages", messages);

                // إرسال الطلب
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes());
                os.flush();

                // استلام الرد
                InputStream inputStream = conn.getInputStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String responseJson = result.toString("UTF-8");
                JSONObject obj = new JSONObject(responseJson);
                String content = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                // استدعاء التحقق داخل الواجهة
                runOnUiThread(() -> {
                    responseTextView.setText(content);

                    List<String> tempMissing = new ArrayList<>();
                    if (content.contains("0(التاريخ)")) tempMissing.add("date");
                    if (content.contains("0(إثبات هوية المرسل)")) tempMissing.add("sender");

                    missingFields = tempMissing.toArray(new String[0]);
                    missingFieldIndex = 0;

                    if (missingFields.length == 0) {
                        Toast.makeText(payPeriod.this, "✅ البيانات مكتملة", Toast.LENGTH_SHORT).show();
                    }

                    // استخراج القيم من الرد
                    String Confirmation = extractBetween(content, "1(هل هذه صورة إيصال دفع)<", ">");
                    String Amount = extractBetween(content, "1(المبلغ)<", ">");
                    String Date = extractBetween(content, "1(التاريخ)<", ">");
                    String Sender = extractBetween(content, "1(إثبات هوية المرسل)<", ">");
                    String Receiver = extractBetween(content, "1(إثبات هوية المرسل إليه)<", ">");

                    // التحقق والمتابعة
                    readFromGPT(Confirmation, Amount, Date, Sender, Receiver);
                });

            } catch (Exception e) {
                runOnUiThread(() -> responseTextView.setText("فشل تحليل الصورة: " + e.getMessage()));
            }
        }).start();
    }

    private void readFromGPT(String Confirmation, String Amount, String Date, String Sender, String Receiver) {
        // تحقق إن كل البيانات موجودة ما عدا Sender
        if (Confirmation != null && !Confirmation.trim().isEmpty() &&
                Amount != null && !Amount.trim().isEmpty() &&
                Date != null && !Date.trim().isEmpty() &&
                Receiver != null && !Receiver.trim().isEmpty()) {

            // لو Sender فاضي أو نل، اطلبه يدويًا
            if (Sender == null || Sender.trim().isEmpty()) {
                manualInput.setVisibility(View.VISIBLE);
                confirmBtn.setVisibility(View.VISIBLE);

                final String[] finalSender = new String[1];

                confirmBtn.setOnClickListener(v -> {
                    String manualSender = manualInput.getText().toString().trim();

                    if (!manualSender.isEmpty()) {
                        finalSender[0] = manualSender;

                        manualInput.setVisibility(View.INVISIBLE);
                        confirmBtn.setVisibility(View.INVISIBLE);

                        Toast.makeText(this, "تم إدخال المرسل يدويًا", Toast.LENGTH_SHORT).show();

                        checkMatching(Confirmation, Amount, Date, finalSender[0], Receiver);
                    } else {
                        Toast.makeText(this, "من فضلك ادخل اسم المرسل", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "تم استخراج البيانات", Toast.LENGTH_SHORT).show();

                checkMatching(Confirmation, Amount, Date, Sender, Receiver);
            }

        } else {
            Toast.makeText(this, "البيانات غير مكتملة، برجاء إعادة رفع الإيصال", Toast.LENGTH_LONG).show();
        }
    }

    private void checkMatching(String Confirmation, String Amount, String Date, String Sender, String Receiver) {
        String numericAmount = "";
        if (Amount != null) {
            Matcher matcher = Pattern.compile("([\\d.]+)").matcher(Amount);
            if (matcher.find()) {
                numericAmount = matcher.group(1);
            }
        }

        String expectedAmount = invoiceAmountView.getText().toString().replaceAll("[^\\d.]", "");
        String expectedReceiver = paymentNumberView.getText().toString().replaceAll("\\D", "");

        boolean amountMatch = numericAmount.equals(expectedAmount);
        boolean receiverMatch = Receiver != null && Receiver.equals(expectedReceiver);

        StringBuilder statusMsg = new StringBuilder();
        if (!amountMatch) statusMsg.append("❌ المبلغ غير مطابق\n");
        else statusMsg.append("✅ المبلغ مطابق\n");

        if (!receiverMatch) statusMsg.append("❌ رقم الدفع غير مطابق");
        else statusMsg.append("✅ رقم الدفع مطابق");

        Toast.makeText(this, statusMsg.toString().trim(), Toast.LENGTH_LONG).show();

        if (amountMatch && receiverMatch) {
            storeConfirmedPaymentData(Confirmation, Amount, Date, Sender, Receiver); // ✅ لو صح، كمل
        } else {
            Toast.makeText(this, "⚠️ البيانات غير متطابقة. لن يتم الحفظ.", Toast.LENGTH_LONG).show();
        }
    }

    private void storeConfirmedPaymentData(String Confirmation, String Amount, String Date, String Sender, String Receiver) {
        if (imageBytes == null || kidId == null) return;

//        Toast.makeText(this, "📤 جاري رفع الصورة إلى Firebase", Toast.LENGTH_SHORT).show();

        getActualDateFromInternet(realDate -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);

            String todayDate = dateFormat.format(realDate);
            String monthKey = monthFormat.format(realDate);

            // إنشاء بيانات جديدة
            Map<String, String> newPaymentData = new HashMap<>();
            newPaymentData.put("confirmation", todayDate);
            newPaymentData.put("amount", Amount);
            newPaymentData.put("date", Date);
            newPaymentData.put("sender", Sender);
            newPaymentData.put("receiver", Receiver);

            StorageReference ref = FirebaseStorage.getInstance().getReference()
                    .child("receipts/" + kidId + "/" + UUID.randomUUID() + ".jpg");

            ref.putBytes(imageBytes).addOnSuccessListener(taskSnapshot ->
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        newPaymentData.put("imageUrl", uri.toString());

                        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference()
                                .child(mainCollection)
                                .child("data")
                                .child(kidId);

                        studentRef.child("subscriptionData").child(monthKey).setValue(newPaymentData);

                        studentRef.child("userEndPeriod").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String newPeriod = addOneMonth(todayDate);
                                studentRef.child("userEndPeriod").setValue(newPeriod);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("Firebase", "فشل تعديل userEndPeriod: " + error.getMessage());
                            }
                        });

                        runOnUiThread(() -> {
                            responseTextView.setText("✅ تم حفظ البيانات الجديدة بنجاح.");
                            Toast.makeText(payPeriod.this, "تم تأكيد الدفع وتخزين البيانات!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(payPeriod.this, SplashActivity.class));
                            finish();
                        });

                    })
            ).addOnFailureListener(e ->
                    runOnUiThread(() -> responseTextView.setText("❌ فشل رفع الصورة: " + e.getMessage())));
        });
    }



    private String addOneMonth(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date current = sdf.parse(date);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(current);
            cal.add(java.util.Calendar.MONTH, 1);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return "2025-01-01";
        }
    }




    private String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex == -1) return null;
        int endIndex = text.indexOf(end, startIndex + start.length());
        if (endIndex == -1) return null;
        return text.substring(startIndex + start.length(), endIndex).trim();
    }






//    private String extractRegex(String text, String pattern) {
//        Pattern regex = Pattern.compile(pattern);
//        Matcher matcher = regex.matcher(text);
//        if (matcher.find()) {
//            return matcher.group(1).trim();
//        }
//        return "";
//    }

//    private void uploadImageAndStoreData() {
//        if (imageBytes == null || kidId == null) return;
//
//        Toast.makeText(this, "📤 جاري رفع الصورة إلى Firebase", Toast.LENGTH_SHORT).show();
//
//        getActualDateFromInternet(realDate -> {
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
//            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
//
//            String todayDate = dateFormat.format(realDate);
//            String monthKey = monthFormat.format(realDate);
//
//            paymentData.put("date", todayDate);
//
//            StorageReference ref = FirebaseStorage.getInstance().getReference()
//                    .child("receipts/" + kidId + "/" + UUID.randomUUID() + ".jpg");
//
//            ref.putBytes(imageBytes).addOnSuccessListener(taskSnapshot ->
//                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
//                        paymentData.put("imageUrl", uri.toString());
//
//                        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference()
//                                .child(mainCollection)
//                                .child("data")
//                                .child(kidId);
//
//                        studentRef.child("subscriptionData").child(monthKey).setValue(paymentData);
//
//                        studentRef.child("userEndPeriod").addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                                String newPeriod = addOneMonth(todayDate);
//                                studentRef.child("userEndPeriod").setValue(newPeriod);
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError error) {
//                                Log.e("Firebase", "فشل تعديل userEndPeriod: " + error.getMessage());
//                            }
//                        });
//
//                        runOnUiThread(() -> {
//                            responseTextView.setText("✅ تم حفظ البيانات والصورة بنجاح.");
//                            Toast.makeText(payPeriod.this, "تم تأكيد الدفع تلقائيًا!", Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(payPeriod.this, MainActivity2.class));
//                            finish();
//                        });
//
//                    })
//            ).addOnFailureListener(e ->
//                    runOnUiThread(() -> responseTextView.setText("❌ فشل رفع الصورة: " + e.getMessage())));
//        });
//    }


//    private String formatDateToStandard(String rawDate) {
//        try {
//            Locale locale = new Locale("ar", "EG");
//            SimpleDateFormat inputFormat = new SimpleDateFormat("d MMMM yyyy", locale);
//            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
//
//            String[] parts = rawDate.split("الساعة|PM|AM|مساء|صباح");
//            String dateOnly = parts[0].replaceAll("[^\\u0600-\\u06FF\\d\\s]", "").trim();
//
//            Date parsedDate = inputFormat.parse(dateOnly);
//            return outputFormat.format(parsedDate);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "2025-01-01";
//        }
//    }
    private void getActualDateFromInternet(DateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("http://worldtimeapi.org/api/timezone/Africa/Cairo");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;

                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String json = result.toString("UTF-8");
                JSONObject obj = new JSONObject(json);
                String datetime = obj.getString("datetime"); // شكلها: 2025-06-07T15:31:24.268292+02:00
                String dateOnly = datetime.split("T")[0]; // 2025-06-07

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date realDate = sdf.parse(dateOnly);
                callback.onDateReceived(realDate);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "⚠️ فشل في جلب التاريخ الحقيقي", Toast.LENGTH_SHORT).show());
                callback.onDateReceived(new Date()); // fallback لو حصل فشل
            }
        }).start();
    }

    // كولباك بسيط عشان نستنى نتيجة التاريخ
    interface DateCallback {
        void onDateReceived(Date date);
    }
}
