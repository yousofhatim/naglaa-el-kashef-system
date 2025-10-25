#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <Adafruit_Fingerprint.h>

// WIFI and Firebase config
#define WIFI_SSID "1"
#define WIFI_PASSWORD "123456789"
#define API_KEY "AIzaSyDhdID2wAdkpl-Hc-8mWvMz83PNfAgRto8"
#define DATABASE_URL "https://kid-id-default-rtdb.firebaseio.com/"
#define USER_EMAIL "yousofhatim91@gmail.com"
#define USER_PASSWORD "Yy77@Yy77"
String mainCollection = "1";

#define RX_PIN 16
#define TX_PIN 17
#define GREEN_LED 13
#define RED_LED 12
HardwareSerial mySerial(1);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);
void addOrderAtFixedIndex(int orderIndex, String kidId, String kidName);


FirebaseData fbdo, streamAdd, streamID;
FirebaseAuth auth;
FirebaseConfig config;

bool add = false;
uint16_t enrollId = 0;

void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConnected to Wi-Fi");
}

void initFirebase() {
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  Firebase.begin(&config, &auth);
  Firebase.reconnectNetwork(true);
}

void setup() {
  Serial.begin(115200);
  delay(100);
  mySerial.begin(57600, SERIAL_8N1, RX_PIN, TX_PIN);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);

  connectWiFi();
  initFirebase();

  if (finger.verifyPassword()) {
    Serial.println("Fingerprint sensor detected!");
  } else {
    Serial.println("Fingerprint sensor NOT found!");
    while (1) delay(1);
  }

  if (!Firebase.RTDB.beginStream(&streamAdd, "/" + mainCollection + "/parents/add")) {
    Serial.println("Stream error on /add");
  }
  if (!Firebase.RTDB.beginStream(&streamID, "/" + mainCollection + "/parents/id")) {
    Serial.println("Stream error on /id");
  }
}

void loop() {
  if (!Firebase.ready()) return;

  if (Firebase.RTDB.readStream(&streamAdd) && streamAdd.streamAvailable()) {
    add = streamAdd.boolData();
    Serial.printf("[ADD FLAG] = %s\n", add ? "true" : "false");
  }

  if (Firebase.RTDB.readStream(&streamID) && streamID.streamAvailable()) {
    enrollId = streamID.intData();
    Serial.printf("[ID RECEIVED] = %d\n", enrollId);
  }

  if (add) {
    enrollFingerprint(enrollId);
    Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/parents/add", false);
  } else {
    checkFingerprint();
  }

  delay(50);
}

void enrollFingerprint(uint16_t id) {
  Serial.printf("Enrolling new finger for ID: %d\n", id);

  // شغلي الليد الأخضر طول البرنامج
  digitalWrite(GREEN_LED, HIGH);

  int p = -1;
  Serial.println("Place finger to enroll");

  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) continue;
    if (p != FINGERPRINT_OK) {
      Serial.println("Error capturing image");
      endEnroll(false);
      return;
    }
  }

  if (finger.image2Tz(1) != FINGERPRINT_OK) {
    Serial.println("Error converting image");
    endEnroll(false);
    return;
  }

  Serial.println("Remove finger");
  delay(2000);
  while (finger.getImage() != FINGERPRINT_NOFINGER);

  Serial.println("Place same finger again");
  p = -1;
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) continue;
    if (p != FINGERPRINT_OK) {
      Serial.println("Error capturing image");
      endEnroll(false);
      return;
    }
  }

  if (finger.image2Tz(2) != FINGERPRINT_OK) {
    Serial.println("Error converting image");
    endEnroll(false);
    return;
  }

  if (finger.createModel() != FINGERPRINT_OK) {
    Serial.println("Error creating model");
    endEnroll(false);
    return;
  }

  if (finger.storeModel(id) == FINGERPRINT_OK) {
    Serial.println("Enrollment successful!");
    endEnroll(true);
  } else {
    Serial.println("Error storing model");
    endEnroll(false);
  }
}
void endEnroll(bool success) {
  // قفل الليد الأخضر بعد ما البرنامج يخلص
  digitalWrite(GREEN_LED, LOW);

  // رجعي حالة add لـ false الأول
  Firebase.RTDB.setBool(&fbdo, "/" + mainCollection + "/parents/add", false);

  // لو فشل شغلي الأحمر بلينكاية
  if (!success) {
    for (int i = 0; i < 3; i++) {
      digitalWrite(RED_LED, HIGH);
      delay(300);
      digitalWrite(RED_LED, LOW);
      delay(300);
    }
  } else {
    digitalWrite(GREEN_LED, HIGH);
    delay(500);
    digitalWrite(GREEN_LED, LOW);
  }
}


void checkFingerprint() {
  if (finger.getImage() != FINGERPRINT_OK) return;
  if (finger.image2Tz() != FINGERPRINT_OK) return;
  
  if (finger.fingerFastSearch() != FINGERPRINT_OK) {
    Serial.println("Fingerprint not recognized!");
    setLEDState(false, true);
    return;
  }

  uint16_t fid = finger.fingerID;
  Serial.printf("Fingerprint recognized. ID = %d\n", fid);

  String basePath = "/" + mainCollection + "/parents/fengerModule/" + String(fid);
  fetchAndWriteOrders(basePath);
  delay(2000);
}


void fetchAndWriteOrders(String basePath) {
  int nextIndex = findNextOrderIndex();
  int originalIndex = nextIndex;

  Serial.printf("Starting to add new orders from index = %d\n", nextIndex);

  int index = 1;
  while (true) {
    String kidIdPath = basePath + "/kidId" + String(index);
    String kidNamePath = basePath + "/kidName" + String(index);

    if (!Firebase.RTDB.getString(&fbdo, kidIdPath) || fbdo.stringData() == "") {
      Serial.println("No more kidId found. Stopping loop.");
      break;
    }
    String kidId = fbdo.stringData();

    if (!Firebase.RTDB.getString(&fbdo, kidNamePath) || fbdo.stringData() == "") {
      Serial.println("No kidName for this ID. Skipping.");
      setLEDState(false, true);
      index++;
      continue;
    }
    String kidName = fbdo.stringData();

    addOrderAtFixedIndex(nextIndex, kidId, kidName);
    nextIndex++;
    index++;
  }

  // تحديث orderlast بعد ما تخلصي
  if (nextIndex > originalIndex) {
    Firebase.RTDB.setInt(&fbdo, "/" + mainCollection + "/orderlast", nextIndex - 1);
    Serial.printf("Updated orderlast to %d\n", nextIndex - 1);
  }
}




int findNextOrderIndex() {
  String path = "/" + mainCollection + "/orderlast";
  int lastOrder = 0;

  if (Firebase.RTDB.getInt(&fbdo, path)) {
    lastOrder = fbdo.intData();
    Serial.printf("Last order from Firebase: %d\n", lastOrder);
  } else {
    Serial.printf("Failed to read orderlast. Reason: %s\n", fbdo.errorReason().c_str());
  }

  return lastOrder + 1;
}





void addOrderAtFixedIndex(int orderIndex, String kidId, String kidName) {
  String orderPath = "/" + mainCollection + "/orders/" + String(orderIndex);

  FirebaseJson json;
  json.set("kidId", kidId);
  json.set("kidName", kidName);
  json.set("type", "near");

 if (Firebase.RTDB.setJSON(&fbdo, orderPath, &json)) {
  Serial.printf("Order %d added: %s - %s\n", orderIndex, kidId.c_str(), kidName.c_str());
  setLEDState(true, false);
 } else {
  Serial.printf("Error adding order %d: %s\n", orderIndex, fbdo.errorReason().c_str());
  setLEDState(false, true);
 }

}
void setLEDState(bool success, bool error) {


  if (success) {
    Serial.println("LED GREEN ON");
    digitalWrite(GREEN_LED, HIGH);
    delay(500);
    digitalWrite(GREEN_LED, LOW);
    delay(200);
  }

  if (error) {
    Serial.println("LED BUILT-IN ON");
    digitalWrite(RED_LED, HIGH);
    delay(500);
    digitalWrite(RED_LED, LOW);
    delay(200);
  }

}


