package com.example.studymate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class ScheduleActivity extends AppCompatActivity {
    private Map<String, Integer> timeToIndex;
    private TableLayout scheduleGrid;
    private String[] days = {"월", "화", "수", "목", "금"};
    private String[] times;
    private boolean isAlarmOn = true;

    private Map<TextView, SubjectBlock> blockMap = new HashMap<>();
    private static final String PREF_NAME = "SchedulePrefs";
    private static final String SCHEDULE_KEY = "SavedSchedule";

    private void initializeTimeToIndexMap() {
        timeToIndex = new HashMap<>();
        for (int i = 0; i < times.length; i++) {
            timeToIndex.put(times[i], i);
        }
    }
    private static class SubjectBlock {
        String subject;
        int dayIndex;
        int startIndex;
        int endIndex;
        int color;

        SubjectBlock(String subject, int dayIndex, int startIndex, int endIndex, int color) {
            this.subject = subject;
            this.dayIndex = dayIndex;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.color = color;
        }

        JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("subject", subject);
            obj.put("dayIndex", dayIndex);
            obj.put("startIndex", startIndex);
            obj.put("endIndex", endIndex);
            obj.put("color", color);
            return obj;
        }

        static SubjectBlock fromJson(JSONObject obj) throws JSONException {
            return new SubjectBlock(
                    obj.getString("subject"),
                    obj.getInt("dayIndex"),
                    obj.getInt("startIndex"),
                    obj.getInt("endIndex"),
                    obj.getInt("color")
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        times = getResources().getStringArray(R.array.times_array);
        scheduleGrid = findViewById(R.id.schedule_grid);
        initializeTimeToIndexMap();

        ImageButton btnAddSchedule = findViewById(R.id.image_add_schedule);
        ImageButton btnHome = findViewById(R.id.image_home);
        ImageButton btnToggleAlarm = findViewById(R.id.image_toggle_alarm);

        btnAddSchedule.setOnClickListener(v -> showAddScheduleDialog());

        btnHome.setOnClickListener(v -> {
            saveScheduleToPreferences();
            Intent intent = new Intent(ScheduleActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnToggleAlarm.setOnClickListener(v -> {
            isAlarmOn = !isAlarmOn;
            String message = isAlarmOn ? "알람이 활성화되었습니다." : "알람이 비활성화되었습니다.";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            if (isAlarmOn) {
                List<ScheduleItem> scheduleList = buildScheduleItemList();
                AlarmHelper.setAlarms(this, scheduleList);
            } else {
                AlarmHelper.cancelAlarms(this);
            }
        });

        loadScheduleFromPreferences();

        if (isAlarmOn) {
            List<ScheduleItem> scheduleList = buildScheduleItemList();
            AlarmHelper.setAlarms(this, scheduleList);
        }
    }

    private List<ScheduleItem> buildScheduleItemList() {
        List<ScheduleItem> scheduleList = new ArrayList<>();
        for (SubjectBlock block : new HashSet<>(blockMap.values())) {
            String day = days[block.dayIndex];

            // ✅ "HH:mm" 형식 처리
            String startTimeStr = times[block.startIndex];
            String endTimeStr = times[block.endIndex];

            int startHour = Integer.parseInt(startTimeStr.split(":")[0]);
            int endHour = Integer.parseInt(endTimeStr.split(":")[0]);

            scheduleList.add(new ScheduleItem(block.subject, day, startHour, endHour));
        }
        return scheduleList;
    }

    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_schedule, null);

        Spinner spinnerDay = dialogView.findViewById(R.id.spinner_day);
        Spinner spinnerStartTime = dialogView.findViewById(R.id.spinner_start_time);
        Spinner spinnerEndTime = dialogView.findViewById(R.id.spinner_end_time);
        EditText editSubject = dialogView.findViewById(R.id.edit_subject);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_schedule);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this, R.array.days_array, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this, R.array.times_array, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartTime.setAdapter(timeAdapter);
        spinnerEndTime.setAdapter(timeAdapter);

        btnAdd.setOnClickListener(v -> {
            String day = spinnerDay.getSelectedItem().toString();
            String subject = editSubject.getText().toString().trim();
            String startTime = spinnerStartTime.getSelectedItem().toString();
            String endTime = spinnerEndTime.getSelectedItem().toString();

            if (subject.isEmpty()) {
                Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (timeToIndex.get(startTime) > timeToIndex.get(endTime)) {
                Toast.makeText(this, "종료 시간은 시작 시간 이후여야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 중복 체크
            if (isOverlapping(day, startTime, endTime)) {
                Toast.makeText(this, "해당 시간대에 이미 등록된 과목이 있습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            addScheduleBlock(day, subject, startTime, endTime);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    // 🔸 중복 체크 함수
    private boolean isOverlapping(String day, String startTime, String endTime) {
        int dayIndex = Arrays.asList(days).indexOf(day);
        int startIndex = timeToIndex.get(startTime);
        int endIndex = timeToIndex.get(endTime);

        for (int i = 0; i < scheduleGrid.getChildCount(); i++) {
            View view = scheduleGrid.getChildAt(i);
            if (!(view instanceof TableRow)) continue;

            TableRow row = (TableRow) view;
            if (row.getChildCount() < days.length + 1) continue;

            TextView timeView = (TextView) row.getChildAt(0);
            String rowTime = timeView.getText().toString().replace("시", "").trim();
            if (!rowTime.contains(":")) rowTime += ":00";

            int rowIndex = timeToIndex.getOrDefault(rowTime, -1);
            if (rowIndex >= startIndex && rowIndex <= endIndex) {
                TextView cell = (TextView) row.getChildAt(dayIndex + 1);
                if (blockMap.containsKey(cell)) return true;
            }
        }
        return false;
    }

    private void addScheduleBlock(String day, String subject, String startTime, String endTime) {
        int color = getRandomColor();
        addScheduleBlock(day, subject, startTime, endTime, color);
    }

    private void addScheduleBlock(String day, String subject, String startTime, String endTime, int color) {
        int dayIndex = Arrays.asList(days).indexOf(day);
        int startIndex = timeToIndex.get(startTime);
        int endIndex = timeToIndex.get(endTime);

        SubjectBlock block = new SubjectBlock(subject, dayIndex, startIndex, endIndex, color);

        for (int i = 0; i < scheduleGrid.getChildCount(); i++) {
            View view = scheduleGrid.getChildAt(i);
            if (!(view instanceof TableRow)) continue;

            TableRow row = (TableRow) view;
            TextView timeView = (TextView) row.getChildAt(0);
            String rowTime = timeView.getText().toString().replace("시", "").trim();
            if (!rowTime.contains(":")) rowTime += ":00";

            int rowIndex = timeToIndex.getOrDefault(rowTime, -1);
            if (rowIndex >= startIndex && rowIndex <= endIndex) {
                TextView cell = (TextView) row.getChildAt(dayIndex + 1);

                cell.setBackgroundColor(color);
                blockMap.put(cell, block);

                if (rowIndex == startIndex) {
                    cell.setText(subject);
                    cell.setOnClickListener(v -> showEditSubjectDialog(block));
                } else {
                    cell.setText("");
                    cell.setOnClickListener(null);
                }
            }
        }
    }

    private void showEditSubjectDialog(SubjectBlock block) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_subject, null);

        EditText editNewSubject = dialogView.findViewById(R.id.edit_new_subject);
        Button btnEdit = dialogView.findViewById(R.id.btn_edit_subject);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_subject);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_edit);

        editNewSubject.setText(block.subject);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnEdit.setOnClickListener(v -> {
            String newSubject = editNewSubject.getText().toString().trim();
            if (!newSubject.isEmpty()) {
                block.subject = newSubject;
                updateBlockDisplay(block);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            clearBlock(block);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void updateBlockDisplay(SubjectBlock block) {
        for (int i = 0; i < scheduleGrid.getChildCount(); i++) {
            View view = scheduleGrid.getChildAt(i);
            if (!(view instanceof TableRow)) continue;

            TableRow row = (TableRow) view;
            TextView timeView = (TextView) row.getChildAt(0);
            String rowTime = timeView.getText().toString().replace("시", "").trim();
            if (!rowTime.contains(":")) rowTime += ":00";

            int rowIndex = timeToIndex.getOrDefault(rowTime, -1);
            if (rowIndex >= block.startIndex && rowIndex <= block.endIndex) {
                TextView cell = (TextView) row.getChildAt(block.dayIndex + 1);
                cell.setBackgroundColor(block.color);
                blockMap.put(cell, block);

                if (rowIndex == block.startIndex) {
                    cell.setText(block.subject);
                    cell.setOnClickListener(v -> showEditSubjectDialog(block));
                } else {
                    cell.setText("");
                    cell.setOnClickListener(null);
                }
            }
        }
    }

    private void clearBlock(SubjectBlock block) {
        for (int i = 0; i < scheduleGrid.getChildCount(); i++) {
            View view = scheduleGrid.getChildAt(i);
            if (!(view instanceof TableRow)) continue;

            TableRow row = (TableRow) view;
            TextView timeView = (TextView) row.getChildAt(0);
            String rowTime = timeView.getText().toString().replace("시", "").trim();
            if (!rowTime.contains(":")) rowTime += ":00";

            int rowIndex = timeToIndex.getOrDefault(rowTime, -1);
            if (rowIndex >= block.startIndex && rowIndex <= block.endIndex) {
                TextView cell = (TextView) row.getChildAt(block.dayIndex + 1);
                cell.setText("");
                cell.setBackgroundResource(R.drawable.cell_border);
                cell.setOnClickListener(null);
                blockMap.remove(cell);
            }
        }
    }

    private int getRandomColor() {
        Random random = new Random();
        return Color.rgb(
                100 + random.nextInt(156),
                100 + random.nextInt(156),
                100 + random.nextInt(156)
        );
    }

    private void saveScheduleToPreferences() {
        JSONArray array = new JSONArray();
        for (SubjectBlock block : new HashSet<>(blockMap.values())) {
            try {
                array.put(block.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(SCHEDULE_KEY, array.toString()).apply();
    }

    private void loadScheduleFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedJson = prefs.getString(SCHEDULE_KEY, null);
        if (savedJson == null) return;

        try {
            JSONArray array = new JSONArray(savedJson);
            for (int i = 0; i < array.length(); i++) {
                SubjectBlock block = SubjectBlock.fromJson(array.getJSONObject(i));
                String start = times[block.startIndex];
                String end = times[block.endIndex];
                addScheduleBlock(days[block.dayIndex], block.subject, start, end, block.color);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
