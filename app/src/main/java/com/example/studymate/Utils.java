package com.example.studymate;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;

public class Utils {
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // PNG로 저장
        return stream.toByteArray();
    }

}
