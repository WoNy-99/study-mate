package com.example.studymate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class AlarmHelper {

    // 📌 반복 알람 설정
    public static void setAlarms(Context context, List<ScheduleItem> scheduleList) {
        for (ScheduleItem item : scheduleList) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            int dayOfWeek = getDayOfWeek(item.getDay());
            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            calendar.set(Calendar.HOUR_OF_DAY, item.getStartHour());
            calendar.set(Calendar.MINUTE, 0);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1); // 이미 지난 시간이라면 다음 주
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("subject", item.getSubject());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    item.getSubject().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY * 7,  // 1주일 반복
                        pendingIntent
                );
            }
        }
    }

    // ❌ 알람 취소 (호출 시 동일한 scheduleList 전달 필요)
    public static void cancelAlarms(Context context) {
        // 모든 알람을 일괄 취소하기 위해 intent와 pendingIntent를 만들어야 합니다.
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0, // requestCode - 고정값
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    // 🔄 요일 문자열을 Calendar 상수로 변환
    private static int getDayOfWeek(String dayKor) {
        switch (dayKor) {
            case "일": return Calendar.SUNDAY;
            case "월": return Calendar.MONDAY;
            case "화": return Calendar.TUESDAY;
            case "수": return Calendar.WEDNESDAY;
            case "목": return Calendar.THURSDAY;
            case "금": return Calendar.FRIDAY;
            case "토": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

}
