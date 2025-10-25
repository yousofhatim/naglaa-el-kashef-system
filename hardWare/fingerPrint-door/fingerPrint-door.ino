#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <Adafruit_Fingerprint.h>
#include <WiFiUdp.h>
#include <NTPClient.h>
#include <TimeLib.h> // مكتبة لتحويل وتحليل الوقت

String mainCollection = "1";  // المتغير الرئيسي الذي يجمع كل البيانات

// #define WIFI_SSID "HUAWEI-5d06"
// #define WIFI_PASSWORD "jm6qag4y"
#define WIFI_SSID "1"
#define WIFI_PASSWORD "123456789"
#define API_KEY "AIzaSyDhdID2wAdkpl-Hc-8mWvMz83PNfAgRto8"
#define DATABASE_URL "https://kid-id-default-rtdb.firebaseio.com/"
#define USER_EMAIL "yousofhatim91@gmail.com"
#define USER_PASSWORD "Yy77#Yy77#Yy77"
#define GREEN_LED 13
#define RED_LED 27
#define door 12


FirebaseData fbdo;
FirebaseData streamID;
FirebaseData streamAdd;
FirebaseAuth auth;
FirebaseConfig config;

#define RX_PIN 16
#define TX_PIN 17

HardwareSerial mySerial(1);  // استخدام المنفذ التسلسلي 1 على ESP32

Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 0, 60000);
uint16_t id = 0;
bool add = false;

struct DateTimeComponents {
  String fullDateTime;
  String year;
  String month;
  String day;
  String hour;
  String minute;
  String second;
};

DateTimeComponents getEgyptDateTime() {
  time_t rawTime = timeClient.getEpochTime();
  struct tm * timeinfo = gmtime(&rawTime);

  // إضافة الفرق الزمني لتوقيت مصر (UTC+2)
  timeinfo->tm_hour += 2;

  // التحقق مما إذا كان التوقيت الصيفي فعالاً
  int day = timeinfo->tm_mday;
  int month = timeinfo->tm_mon + 1; // يبدأ من 0 لذا نضيف 1
  int wday = timeinfo->tm_wday; // يوم الأسبوع (0-6 حيث 0 هو الأحد)

  bool isDST = false;

  if (month > 4 && month < 11) {
    isDST = true; // التوقيت الصيفي من مايو إلى أكتوبر
  } else if (month == 4) {
    // آخر جمعة من أبريل
    int lastFriday = day - (wday + 1) % 7;
    if (day >= lastFriday) {
      isDST = true;
    }
  } else if (month == 11) {
    // آخر جمعة من أكتوبر
    int lastFriday = day - (wday + 1) % 7;
    if (day <= lastFriday) {
      isDST = true;
    }
  }

  // إذا كان التوقيت الصيفي فعالاً، نضيف ساعة إضافية
  if (isDST) {
    timeinfo->tm_hour += 1;
  }

  // التعامل مع تجاوز الساعة
  if (timeinfo->tm_hour >= 24) {
    timeinfo->tm_hour -= 24;
    timeinfo->tm_mday += 1;
  }

  // صياغة التاريخ والوقت بالتنسيق المطلوب (YYYY-MM-DD HH:MM:SS)
  char buffer[20];
  strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", timeinfo);
  String fullDateTime = String(buffer);

  // صياغة القيم الفردية
  char y[5], m[3], d[3], h[3], min[3], s[3];
  strftime(y, sizeof(y), "%Y", timeinfo);
  strftime(m, sizeof(m), "%m", timeinfo);
  strftime(d, sizeof(d), "%d", timeinfo);
  strftime(h, sizeof(h), "%H", timeinfo);
  strftime(min, sizeof(min), "%M", timeinfo);
  strftime(s, sizeof(s), "%S", timeinfo);

  DateTimeComponents components;
  components.fullDateTime = fullDateTime;
  components.year = String(y);
  components.month = String(m);
  components.day = String(d);
  components.hour = String(h);
  components.minute = String(min);
  components.second = String(s);

  return components;
}

// دالة لتحويل سلسلة زمنية إلى عدد الثواني منذ عام 1970
time_t convertToEpochTime(const String& dateTime) {
  // صيغة التاريخ: YYYY-MM-DD HH:MM:SS
  tmElements_t tm;
  tm.Year = dateTime.substring(0, 4).toInt() - 1970;
  tm.Month = dateTime.substring(5, 7).toInt();
  tm.Day = dateTime.substring(8, 10).toInt();
  tm.Hour = dateTime.substring(11, 13).toInt();
  tm.Minute = dateTime.substring(14, 16).toInt();
  tm.Second = dateTime.substring(17, 19).toInt();
  
  return makeTime(tm);
}

unsigned long startMillis;  // لتخزين وقت البدء
const unsigned long period = 1800000;  // المدة 30 دقيقة
const unsigned long wifiTimeout = 10000; // المدة 10 ثواني للاتصال بشبكة الواي فاي
// const unsigned long fingerprintTimeout = 4000; // المدة 4 ثواني
unsigned long lastWiFiCheckMillis = 0; // آخر وقت تم فيه التحقق من حالة الواي فاي

void setup() {
  Serial.begin(115200);
  delay(100);

  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(door, OUTPUT);

  digitalWrite(GREEN_LED, LOW);
  digitalWrite(RED_LED, LOW);
  digitalWrite(door, LOW);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  
  unsigned long wifiStart = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - wifiStart >= wifiTimeout) {
      esp_restart();  // إعادة تشغيل اللوحة إذا استغرق الاتصال بشبكة الواي فاي أكثر من 10 ثوانٍ
    }
    delay(1000);
    Serial.print(".");
  }
  Serial.println("\nConnected");

  config.api_key = API_KEY;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  config.database_url = DATABASE_URL;
  config.token_status_callback = tokenStatusCallback;
  Firebase.reconnectNetwork(true);
  fbdo.setBSSLBufferSize(2048, 1024);
  streamAdd.setBSSLBufferSize(2048, 1024);

  Firebase.begin(&config, &auth);
  streamID.setBSSLBufferSize(2048, 1024);
  streamID.keepAlive(5, 5, 1);
  
  if (!Firebase.RTDB.beginStream(&streamID, "/" + mainCollection + "/id")) {
    Serial.printf("Stream read error (ID), %s\n\n", streamID.errorReason().c_str());
    setLEDState(false, true);
  }

  streamAdd.setBSSLBufferSize(2048, 1024);
  streamAdd.keepAlive(5, 5, 1);
  
  if (!Firebase.RTDB.beginStream(&streamAdd, "/" + mainCollection + "/add")) {
    Serial.printf("Stream read error (Add), %s\n\n", streamAdd.errorReason().c_str());
    setLEDState(false, true);
  }

  mySerial.begin(57600, SERIAL_8N1, RX_PIN, TX_PIN);
  delay(100);
  
  if (finger.verifyPassword()) {
    Serial.println("Found fingerprint sensor!");
  } else {
    Serial.println("Did not find fingerprint sensor :(");
    setLEDState(false, true);
    while (1) { delay(1); }
  }

  // بدء عميل NTP
  timeClient.begin();
  timeClient.update();

  startMillis = millis();  // احفظ وقت البدء الحالي
  lastWiFiCheckMillis = millis(); // احفظ وقت بدء التحقق من الواي فاي
}

void loop() {
  if (millis() - startMillis >= period) {
    esp_restart();  // إعادة تشغيل اللوحة
  }

  // التحقق من حالة الواي فاي بشكل دوري
  if (millis() - lastWiFiCheckMillis >= 5000) { // التحقق كل 5 ثواني
    if (WiFi.status() != WL_CONNECTED) {
      esp_restart(); // إعادة تشغيل اللوحة إذا كان الواي فاي غير متصل
    }
    lastWiFiCheckMillis = millis();
  }

  if (!Firebase.ready())
    return;

  timeClient.update();
  DateTimeComponents dt = getEgyptDateTime(); 

  if (!Firebase.RTDB.readStream(&streamAdd)) {
    Serial.printf("Stream read error (Add), %s\n\n", streamAdd.errorReason().c_str());
    setLEDState(false, true);
  }
  if (streamAdd.streamTimeout()) {
    Serial.println("Stream (Add) timed out, resuming...\n");
    setLEDState(false, true);
  }
  if (streamAdd.streamAvailable()) {
    add = streamAdd.boolData();
    Serial.printf("Received Add flag from stream: %s\n", add ? "true" : "false");
  }

  // تحكم في الـ LED الأخضر بناءً على قيمة add
  digitalWrite(GREEN_LED, add ? HIGH : LOW);

  if (!Firebase.RTDB.readStream(&streamID)) {
    Serial.printf("Stream read error (ID), %s\n\n", streamID.errorReason().c_str());
    setLEDState(false, true);
  }
  if (streamID.streamTimeout()) {
    Serial.println("Stream (ID) timed out, resuming...\n");
    setLEDState(false, true);
  }
  if (streamID.streamAvailable()) {
    id = streamID.intData();
    Serial.printf("Received ID from stream: %d\n", id);
  }

  if (add) {
    Serial.print("Enrolling new fingerprint for ID #");
    Serial.println(id);

    if (getFingerprintEnroll()) {
      Serial.println("Successfully enrolled fingerprint!");
    } else {
      Serial.println("Failed to enroll fingerprint.");
      setLEDState(false, true);
    }

    if (Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/add", false)) {
      Serial.println("Reset add flag to false.");
    } else {
      Serial.printf("Error resetting add flag: %s\n", fbdo.errorReason().c_str());
      setLEDState(false, true);
    }

  } else {
    unsigned long fingerprintStart = millis();  // احفظ وقت بدء التحقق من البصمة
    if (finger.getImage() == FINGERPRINT_OK) {
      // if (millis() - fingerprintStart >= fingerprintTimeout) {
      //   esp_restart();  // إعادة تشغيل اللوحة إذا استغرق التحقق من البصمة أكثر من 4 ثوانٍ
      // }
      
      uint8_t fingerprintID = getFingerprintID();
      if (fingerprintID != -1) {
        String fingerModulePath = "/" + mainCollection + "/fengerModule/" + String(fingerprintID) + "/kidId";
        if (Firebase.RTDB.getString(&fbdo, fingerModulePath)) {
          String kidId = fbdo.stringData();
          Serial.printf("Found kidId: %s\n", kidId.c_str());
          String lastDatePath = "/" + mainCollection + "/data/" + kidId + "/lastDate";

          if (Firebase.RTDB.getString(&fbdo, lastDatePath)) {
            String lastDateTime = fbdo.stringData();
            Serial.printf("Last fingerprint time: %s\n", lastDateTime.c_str());

            // تحويل وقت البصمة الأخير والوقت الحالي إلى إبوك تايم
            time_t lastEpochTime = convertToEpochTime(lastDateTime);
            time_t currentEpochTime = convertToEpochTime(dt.fullDateTime);

            // حساب الفرق بالثواني
            long difference = currentEpochTime - lastEpochTime;

            // تحقق إذا كانت المدة أقل من 5 دقائق (300 ثانية)
            if (difference < 1800) {
              Serial.println("Fingerprint rejected: Less than 30 minutes since the last scan.");
              setLEDState(false, true); // إضاءة الـ LED الأحمر لرفض البصمة
              return; // رفض البصمة
            }
          } else {
            Serial.printf("Error getting lastDate: %s\n", fbdo.errorReason().c_str());
            // تجاهل تشغيل الليد الأحمر عند عدم وجود الشايلد lastDate
            // setLEDState(false, true);
          }

          // تحديث آخر وقت تسجيل للبصمة في قاعدة البيانات
          if (Firebase.RTDB.setString(&fbdo, lastDatePath, dt.fullDateTime)) {
            Serial.println("Successfully recorded time and date.");
          } else {
            Serial.printf("Error recording time and date: %s\n", fbdo.errorReason().c_str());
            setLEDState(false, true);
          }

          String isInClassPath = "/" + mainCollection + "/data/" + kidId + "/isInClass";
          if (Firebase.RTDB.getBool(&fbdo, isInClassPath)) {
            bool isInClass = fbdo.boolData();
            Serial.printf("Current isInClass state: %s\n", isInClass ? "true" : "false");

            // Toggle isInClass value
            if (Firebase.RTDB.setBool(&fbdo, isInClassPath, !isInClass)) {
              Serial.printf("Successfully toggled isInClass to: %s\n", !isInClass ? "true" : "false");
              setLEDState(true, false); // إضاءة الـ LED الأخضر للنجاح

              // الحصول على اسم الطالب
              String kidNamePath = "/" + mainCollection + "/data/" + kidId + "/kidName";
              if (Firebase.RTDB.getString(&fbdo, kidNamePath)) {
                String kidName = fbdo.stringData();
                Serial.printf("Found KidName: %s\n", kidName.c_str());

                // تجهيز المسار الجديد وإرسال البيانات
                String datePath = "/" + mainCollection + "/dateData/" + dt.year + "/" + dt.month + "/" + dt.day;
                String recordPath = datePath + "/" + kidId + (isInClass ? "/checkOut" : "/checkIn");

                // إرسال البيانات إلى Firebase
                if (Firebase.RTDB.setString(&fbdo, recordPath, dt.hour + ":" + dt.minute + ":" + dt.second)) {
                  Serial.println("Successfully recorded time and date.");
                } else {
                  Serial.printf("Error recording time and date: %s\n", fbdo.errorReason().c_str());
                  setLEDState(false, true);
                }
              } else {
                Serial.printf("Error getting KidName: %s\n", fbdo.errorReason().c_str());
                setLEDState(false, true);
              }
            } else {
              Serial.printf("Error toggling isInClass: %s\n", fbdo.errorReason().c_str());
              setLEDState(false, true);
            }
          } else {
            Serial.printf("Error getting isInClass state: %s\n", fbdo.errorReason().c_str());
            setLEDState(false, true);
          }
        } else {
          Serial.printf("Error getting kidId: %s\n", fbdo.errorReason().c_str());
          setLEDState(false, true);
        }
      }
    }
  }
}

uint8_t getFingerprintID() {
  uint8_t p = finger.image2Tz();
  if (p != FINGERPRINT_OK) return -1;

  p = finger.fingerFastSearch();
  if (p != FINGERPRINT_OK) return -1;

  return finger.fingerID;
}

bool getFingerprintEnroll() {
  int p = -1;
  Serial.println("Waiting for valid finger to enroll as ID " + String(id));
  unsigned long enrollStart = millis();  // احفظ وقت بدء التسجيل

  while (p != FINGERPRINT_OK) {
    // if (millis() - enrollStart >= fingerprintTimeout) {
    //   esp_restart();  // إعادة تشغيل اللوحة إذا استغرق التسجيل أكثر من 4 ثوانٍ
    // }

    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(100);
    } else if (p == FINGERPRINT_IMAGEFAIL) {
      Serial.println("Image capture failed");
      setLEDState(false, true);
      
      // تحديث حالة الفشل
      Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
      
      return false;
    } else if (p != FINGERPRINT_OK) {
      Serial.println("Unknown error");
      setLEDState(false, true);

      // تحديث حالة الفشل
      Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
      
      return false;
    }
  }

  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    Serial.println("Image conversion failed");
    setLEDState(false, true);

    // تحديث حالة الفشل
    Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
    
    return false;
  }

  Serial.println("Remove finger");
  delay(2000);
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    // if (millis() - enrollStart >= fingerprintTimeout) {
    //   esp_restart();  // إعادة تشغيل اللوحة إذا استغرق التسجيل أكثر من 4 ثوانٍ
    // }
    p = finger.getImage();
  }
  Serial.println("Press the same finger again");

  p = -1;
  while (p != FINGERPRINT_OK) {
    // if (millis() - enrollStart >= fingerprintTimeout) {
    //   esp_restart();  // إعادة تشغيل اللوحة إذا استغرق التسجيل أكثر من 4 ثوانٍ
    // }

    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(100);
    } else if (p == FINGERPRINT_IMAGEFAIL) {
      Serial.println("Image capture failed");
      setLEDState(false, true);

      // تحديث حالة الفشل
      Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
      
      return false;
    } else if (p != FINGERPRINT_OK) {
      Serial.println("Unknown error");
      setLEDState(false, true);

      // تحديث حالة الفشل
      Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
      
      return false;
    }
  }

  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    Serial.println("Image conversion failed");
    setLEDState(false, true);

    // تحديث حالة الفشل
    Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
    
    return false;
  }

  Serial.print("Creating model for #");
  Serial.println(id);
  p = finger.createModel();
  if (p != FINGERPRINT_OK) {
    if (p == FINGERPRINT_PACKETRECIEVEERR) {
      Serial.println("Communication error");
      setLEDState(false, true);
    } else if (p == FINGERPRINT_ENROLLMISMATCH) {
      Serial.println("Fingerprints did not match");
      setLEDState(false, true);
    } else {
      Serial.println("Unknown error");
      setLEDState(false, true);
    }

    // تحديث حالة الفشل
    Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
    
    return false;
  }

  Serial.print("ID ");
  Serial.println(id);
  p = finger.storeModel(id);
  if (p != FINGERPRINT_OK) {
    if (p == FINGERPRINT_PACKETRECIEVEERR) {
      Serial.println("Communication error");
      setLEDState(false, true);
    } else if (p == FINGERPRINT_BADLOCATION) {
      Serial.println("Could not store in that location. Trying a different ID...");
      // تجربة معرف تخزين مختلف
      id++;
      if (id > 1000) {
        Serial.println("No available locations. Please try again with a different ID.");
        return false;
      }
      return getFingerprintEnroll(); // إعادة محاولة التخزين بمعرف جديد
    } else if (p == FINGERPRINT_FLASHERR) {
      Serial.println("Error writing to flash");
      setLEDState(false, true);
    } else {
      Serial.println("Unknown error");
      setLEDState(false, true);
    }

    // تحديث حالة الفشل
    Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/failed", true);
    
    return false;
  }

  return true;
}

void setLEDState(bool success, bool error) {
  if (success) {
    digitalWrite(GREEN_LED, HIGH);
    digitalWrite(door, HIGH);
    delay(200); // إضاءة LED لمدة 200 مللي ثانية
    digitalWrite(door, LOW);
    digitalWrite(GREEN_LED, LOW);

    delay(200); // إطفاء LED لمدة 200 مللي ثانية
    digitalWrite(GREEN_LED, HIGH);
    delay(200); // إضاءة LED لمدة 200 مللي ثانية
    digitalWrite(GREEN_LED, LOW);

  } else {
    digitalWrite(door, LOW);

    digitalWrite(GREEN_LED, LOW);
  }

  if (error) {

    digitalWrite(RED_LED, HIGH);
    delay(200); // إضاءة LED لمدة 200 مللي ثانية
    digitalWrite(RED_LED, LOW);
    delay(200); // إطفاء LED لمدة 200 مللي ثانية
    digitalWrite(RED_LED, HIGH);
    delay(200); // إضاءة LED لمدة 200 مللي ثانية
    digitalWrite(RED_LED, LOW);
  } else {
    
    digitalWrite(RED_LED, LOW);
  }
}