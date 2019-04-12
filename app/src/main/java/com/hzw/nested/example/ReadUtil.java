package com.hzw.nested.example;

import android.content.Context;
import android.content.SharedPreferences;

public class ReadUtil {

    private static final String FILE_NAME = "NestedViewGroup";
    private static final String READ_KEY = "nested_read";


    public static void saveRead(Context context, int position) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(READ_KEY, position);
        editor.apply();
    }


    public static int getRead(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sp.getInt(READ_KEY, 0);
    }

}
