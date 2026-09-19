package com.example.studymate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 인텐트에서 과목명 꺼내기
        String subject = intent.getStringExtra("subject");

        // 알림 서비스에 과목명 전달
        Intent serviceIntent = new Intent(context, NotificationService.class);
        serviceIntent.putExtra("subject", subject);
        context.startService(serviceIntent);
    }
}
