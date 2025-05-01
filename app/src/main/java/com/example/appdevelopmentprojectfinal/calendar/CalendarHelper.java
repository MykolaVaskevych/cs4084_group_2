package com.example.appdevelopmentprojectfinal.calendar;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Calendar functionality helper class
 * Provides methods to use Firestore for event storage
 */
public class CalendarHelper {
    private static final String TAG = "CalendarHelper";
    
    /**
     * Format date to display string
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date);
    }
    
    /**
     * Format time to display string
     */
    public static String formatTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(date);
    }
    
    /**
     * Save event to Firestore
     * @param context Context
     * @param event Event to save
     * @param onComplete Completion callback
     */
    public static void saveEvent(Context context, Event event, OnEventOperationListener onComplete) {
        CalendarFirestoreManager.getInstance().saveEvent(event, new CalendarFirestoreManager.OnEventOperationListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Event successfully saved to Firestore");
                if (onComplete != null) {
                    onComplete.onSuccess();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to save event to Firestore: " + errorMessage);
                if (onComplete != null) {
                    onComplete.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Delete event from Firestore
     * @param context Context
     * @param event Event to delete
     * @param onComplete Completion callback
     */
    public static void deleteEvent(Context context, Event event, OnEventOperationListener onComplete) {
        CalendarFirestoreManager.getInstance().deleteEvent(event.getId(), new CalendarFirestoreManager.OnEventOperationListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Event successfully deleted from Firestore");
                if (onComplete != null) {
                    onComplete.onSuccess();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to delete event from Firestore: " + errorMessage);
                if (onComplete != null) {
                    onComplete.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Load events from Firestore
     * @param context Context
     * @param userId User ID
     * @param listener Events loaded listener
     */
    public static void loadAllEvents(Context context, String userId, OnEventsLoadedListener listener) {
        CalendarFirestoreManager.getInstance().loadEvents(userId, new CalendarFirestoreManager.OnEventsLoadedListener() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                if (listener != null) {
                    listener.onEventsLoaded(events);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load events from Firestore: " + errorMessage);
                if (listener != null) {
                    listener.onEventsLoaded(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * Check if two dates are the same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    public interface OnEventOperationListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    public interface OnEventsLoadedListener {
        void onEventsLoaded(List<Event> events);
    }
} 