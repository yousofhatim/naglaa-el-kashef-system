//package com.example.f_pdelivery;
//
//import android.app.Service;
//import android.content.Intent;
//import android.media.MediaPlayer;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.widget.Toast;
//
//import java.io.IOException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//
//public class UdpBackgroundService extends Service {
//
//    private boolean serverRunning = true;
//    private Handler handler;
//    private MediaPlayer mediaPlayer;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        handler = new Handler(Looper.getMainLooper());
//        mediaPlayer = MediaPlayer.create(this, R.raw.bell);
//
//        startUdpServer();
//    }
//
//    private void startUdpServer() {
//        new Thread(() -> {
//            DatagramSocket socket = null;
//            try {
//                socket = new DatagramSocket(8888);
//                byte[] buffer = new byte[1024];
//
//                while (serverRunning) {
//                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                    socket.receive(packet);
//
//                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
//
//                    handler.post(() -> {
//                        if (mediaPlayer != null) mediaPlayer.start();
//                        Toast.makeText(getApplicationContext(), "تم الاستلام: " + message, Toast.LENGTH_SHORT).show();
//                    });
//
//                    // ابعتي له رد تأكيد
//                    String ack = "✅ تم تسجيل طلب";
//                    byte[] ackData = ack.getBytes();
//                    DatagramPacket ackPacket = new DatagramPacket(
//                            ackData,
//                            ackData.length,
//                            packet.getAddress(),
//                            packet.getPort()
//                    );
//                    socket.send(ackPacket);
//                }
//
//            } catch (IOException e) {
//                handler.post(() -> Toast.makeText(getApplicationContext(), "خطأ في السيرفر: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            } finally {
//                if (socket != null && !socket.isClosed()) {
//                    socket.close();
//                }
//            }
//        }).start();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        serverRunning = false;
//        if (mediaPlayer != null) mediaPlayer.release();
//        super.onDestroy();
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}
//
