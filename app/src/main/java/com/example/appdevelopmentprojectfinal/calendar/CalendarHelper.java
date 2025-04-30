package com.example.appdevelopmentprojectfinal.calendar;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendar functionality helper class
 * Provides methods to use both Firestore and local storage
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
     * Save event (both to Firestore and local storage)
     * @param context Context
     * @param event Event to save
     * @param onComplete Completion callback
     */
    public static void saveEvent(Context context, Event event, OnEventOperationListener onComplete) {
        // First save to local storage
        boolean localSaved = CalendarLocalStorage.saveEvent(context, event);
        
        // Then try to save to Firestore
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
                    if (localSaved) {
                        // At least saved locally
                        onComplete.onSuccess();
                    } else {
                        onComplete.onError("Both local and remote save failed");
                    }
                }
            }
        });
    }
    
    /**
     * Delete event (from both Firestore and local storage)
     * @param context Context
     * @param event Event to delete
     * @param onComplete Completion callback
     */
    public static void deleteEvent(Context context, Event event, OnEventOperationListener onComplete) {
        // First delete from local storage
        boolean localDeleted = CalendarLocalStorage.deleteEvent(context, event.getId());
        
        // Then try to delete from Firestore
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
                    if (localDeleted) {
                        // At least deleted locally
                        onComplete.onSuccess();
                    } else {
                        onComplete.onError("Both local and remote delete failed");
                    }
                }
            }
        });
    }
    
    /**
     * Load all events (merge Firestore and local storage results)
     * @param context Context
     * @param userId User ID
     * @param listener Events loaded listener
     */
    public static void loadAllEvents(Context context, String userId, OnEventsLoadedListener listener) {
        // First load events from local storage
        List<Event> localEvents = CalendarLocalStorage.loadEvents(context);
        
        // Then load events from Firestore and merge
        CalendarFirestoreManager.getInstance().loadEvents(userId, new CalendarFirestoreManager.OnEventsLoadedListener() {
            @Override
            public void onEventsLoaded(List<Event> remoteEvents) {
                // Merge event lists, remote events take priority
                List<Event> mergedEvents = mergeEvents(localEvents, remoteEvents);
                
                // Callback with merged event list
                if (listener != null) {
                    listener.onEventsLoaded(mergedEvents);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load events from Firestore: " + errorMessage);
                // If remote load fails, only return local events
                if (listener != null) {
                    listener.onEventsLoaded(localEvents);
                }
            }
        });
    }
    
    /**
     * Merge local and remote event lists
     * If there are events with the same ID, remote events take priority
     */
    private static List<Event> mergeEvents(List<Event> localEvents, List<Event> remoteEvents) {
        List<Event> mergedEvents = new ArrayList<>();
        Map<String, Boolean> eventIdMap = new HashMap<>();
        
        // Add all remote events
        if (remoteEvents != null) {
            for (Event event : remoteEvents) {
                mergedEvents.add(event);
                eventIdMap.put(event.getId(), true);
            }
        }
        
        // Add local-only events
        if (localEvents != null) {
            for (Event event : localEvents) {
                if (!eventIdMap.containsKey(event.getId())) {
                    mergedEvents.add(event);
                }
            }
        }
        
        return mergedEvents;
    }
    
    /**
     * Sync local events to Firestore
     * @param context Context
     * @param userId User ID
     * @param listener Sync completion listener
     */
    public static void syncLocalEventsToFirestore(Context context, String userId, OnSyncCompletedListener listener) {
        List<Event> localEvents = CalendarLocalStorage.loadEvents(context);
        
        // Set user ID for all events
        for (Event event : localEvents) {
            if (event.getUserId() == null || event.getUserId().isEmpty()) {
                event.setUserId(userId);
            }
        }
        
        final int[] syncedCount = {0};
        final int[] failedCount = {0};
        final int totalCount = localEvents.size();
        
        if (totalCount == 0) {
            if (listener != null) {
                listener.onSyncCompleted(0, 0);
            }
            return;
        }
        
        for (Event event : localEvents) {
            CalendarFirestoreManager.getInstance().saveEvent(event, new CalendarFirestoreManager.OnEventOperationListener() {
                @Override
                public void onSuccess() {
                    syncedCount[0]++;
                    checkIfCompleted();
                }
                
                @Override
                public void onError(String errorMessage) {
                    failedCount[0]++;
                    checkIfCompleted();
                }
                
                private void checkIfCompleted() {
                    if (syncedCount[0] + failedCount[0] >= totalCount) {
                        if (listener != null) {
                            listener.onSyncCompleted(syncedCount[0], failedCount[0]);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Check if two dates are the same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Event operation completion listener
     */
    public interface OnEventOperationListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    /**
     * Event loaded listener
     */
    public interface OnEventsLoadedListener {
        void onEventsLoaded(List<Event> events);
    }
    
    /**
     * Sync completion listener
     */
    public interface OnSyncCompletedListener {
        void onSyncCompleted(int syncedCount, int failedCount);
    }
} 