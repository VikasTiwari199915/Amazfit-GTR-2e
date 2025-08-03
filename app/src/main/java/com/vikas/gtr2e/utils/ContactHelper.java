package com.vikas.gtr2e.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactHelper {
    public static String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                     Uri.encode(phoneNumber));
        
        try (Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return phoneNumber; // Return original number if no name found
    }
}