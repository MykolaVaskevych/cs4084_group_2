package com.example.appdevelopmentprojectfinal.calendar;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Manager class for handling Firestore operations related to calendar events and todos
 */
public class CalendarFirestoreManager {
    private static final String TAG = "CalendarFirestoreManager";
    private static final String COLLECTION_EVENTS = "events";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_TYPE = "type";
    
    private static FirebaseFirestore db;
    private static CollectionReference eventsCollection;
    
    private static CalendarFirestoreManager instance;
    private ListenerRegistration eventsListener;
    
    private CalendarFirestoreManager() {
        db = FirebaseFirestore.getInstance();
        eventsCollection = db.collection(COLLECTION_EVENTS);
    }
    
    public static synchronized CalendarFirestoreManager getInstance() {
        if (instance == null) {
            instance = new CalendarFirestoreManager();
        }
        return instance;
    }
    
    /**
     * Save or update an event in Firestore
     * @param event The event to save
     * @param listener Listener to handle success/failure
     */
    public void saveEvent(Event event, final OnEventOperationListener listener) {
        if (event == null || event.getId() == null) {
            if (listener != null) listener.onError("Invalid event data");
            return;
        }
        
        Map<String, Object> eventMap = convertEventToMap(event);
        
        eventsCollection.document(event.getId())
            .set(eventMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Event saved successfully to Firestore");
                    if (listener != null) listener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error saving event to Firestore", e);
                    if (listener != null) listener.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Delete an event from Firestore
     * @param eventId The ID of the event to delete
     * @param listener Listener to handle success/failure
     */
    public void deleteEvent(String eventId, final OnEventOperationListener listener) {
        if (eventId == null) {
            if (listener != null) listener.onError("Invalid event ID");
            return;
        }
        
        eventsCollection.document(eventId)
            .delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Event deleted successfully from Firestore");
                    if (listener != null) listener.onSuccess();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error deleting event from Firestore", e);
                    if (listener != null) listener.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Load events for a specific user with real-time updates
     * @param userId The user ID to load events for
     * @param listener Listener to handle loaded events
     */
    public void loadEvents(String userId, final OnEventsLoadedListener listener) {
        if (userId == null) {
            if (listener != null) listener.onError("Invalid user ID");
            return;
        }
        
        // Remove existing listener if any
        if (eventsListener != null) {
            eventsListener.remove();
        }
        
        // Add real-time listener
        eventsListener = eventsCollection.whereEqualTo(FIELD_USER_ID, userId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening for events", error);
                    if (listener != null) listener.onError(error.getMessage());
                    return;
                }
                
                if (value != null) {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : value) {
                        Event event = convertDocumentToEvent(document);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    Log.d(TAG, "Loaded " + events.size() + " events from Firestore");
                    if (listener != null) listener.onEventsLoaded(events);
                }
            });
    }
    
    /**
     * Load todos for a specific user with real-time updates
     * @param userId The user ID to load todos for
     * @param listener Listener to handle loaded todos
     */
    public void loadTodos(String userId, final OnEventsLoadedListener listener) {
        if (userId == null) {
            if (listener != null) listener.onError("Invalid user ID");
            return;
        }
        
        // Remove existing listener if any
        if (eventsListener != null) {
            eventsListener.remove();
        }
        
        // Add real-time listener
        eventsListener = eventsCollection
            .whereEqualTo(FIELD_USER_ID, userId)
            .whereEqualTo(FIELD_TYPE, Event.TYPE_TODO)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening for todos", error);
                    if (listener != null) listener.onError(error.getMessage());
                    return;
                }
                
                if (value != null) {
                    List<Event> todos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : value) {
                        Event todo = convertDocumentToEvent(document);
                        if (todo != null) {
                            todos.add(todo);
                        }
                    }
                    Log.d(TAG, "Loaded " + todos.size() + " todos from Firestore");
                    if (listener != null) listener.onEventsLoaded(todos);
                }
            });
    }
    
    /**
     * Clean up listeners when no longer needed
     */
    public void cleanup() {
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }
    
    /**
     * Convert Firestore document to Event object
     */
    private Event convertDocumentToEvent(DocumentSnapshot document) {
        try {
            Event event = new Event();
            event.setId(document.getId());
            event.setUserId(document.getString(FIELD_USER_ID));
            event.setTitle(document.getString("title"));
            event.setDescription(document.getString("description"));
            
            // Handle date conversion from Timestamp
            Timestamp timestamp = document.getTimestamp("date");
            if (timestamp != null) {
                event.setDate(timestamp.toDate());
            }
            
            // Get type
            Long type = document.getLong(FIELD_TYPE);
            if (type != null) {
                event.setType(type.intValue());
            }
            
            // Handle completed flag for todos
            Boolean completed = document.getBoolean("isCompleted");
            if (completed != null) {
                event.setCompleted(completed);
            }
            
            return event;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to event", e);
            return null;
        }
    }
    
    /**
     * Convert Event object to Firestore map
     */
    private Map<String, Object> convertEventToMap(Event event) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", event.getId());
        eventMap.put(FIELD_USER_ID, event.getUserId());
        eventMap.put("title", event.getTitle());
        eventMap.put("description", event.getDescription());
        
        // Store date as Timestamp
        if (event.getDate() != null) {
            eventMap.put("date", new Timestamp(event.getDate()));
        }
        
        eventMap.put(FIELD_TYPE, event.getType());
        eventMap.put("isCompleted", event.isCompleted());
        
        return eventMap;
    }
    
    /**
     * Listener for event operations
     */
    public interface OnEventOperationListener {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    /**
     * Listener for loading events
     */
    public interface OnEventsLoadedListener {
        void onEventsLoaded(List<Event> events);
        void onError(String errorMessage);
    }
} 