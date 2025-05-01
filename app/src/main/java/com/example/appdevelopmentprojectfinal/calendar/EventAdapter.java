package com.example.appdevelopmentprojectfinal.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for displaying calendar events in a RecyclerView.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    
    private List<com.example.appdevelopmentprojectfinal.calendar.Event> events;
    private Context context;
    private SimpleDateFormat dateFormat;
    private OnEventClickListener listener;
    private OnTodoCompletedListener todoCompletedListener;
    
    public interface OnEventClickListener {
        void onEventClick(com.example.appdevelopmentprojectfinal.calendar.Event event, int position);
    }
    
    public interface OnTodoCompletedListener {
        void onTodoCompleted(com.example.appdevelopmentprojectfinal.calendar.Event event, boolean isCompleted, int position);
    }
    
    public EventAdapter(Context context, List<com.example.appdevelopmentprojectfinal.calendar.Event> events) {
        this.context = context;
        this.events = events;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnTodoCompletedListener(OnTodoCompletedListener listener) {
        this.todoCompletedListener = listener;
    }
    
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        com.example.appdevelopmentprojectfinal.calendar.Event event = events.get(position);
        
        holder.titleTextView.setText(event.getTitle());
        holder.descriptionTextView.setText(event.getDescription());
        holder.timeTextView.setText(dateFormat.format(event.getDate()));
        
        // Set the color of the left indicator based on event type and completion status
        if (event.isTodo()) {
            if (event.isCompleted()) {
                // Completed TODO - Grey
                holder.typeIndicator.setBackgroundColor(context.getResources().getColor(R.color.gray_500));
                holder.countdownTextView.setText(context.getString(R.string.completed));
                holder.countdownTextView.setTextColor(context.getResources().getColor(R.color.gray_500));
            } else {
                // Incomplete TODO - Orange
                holder.typeIndicator.setBackgroundColor(context.getResources().getColor(R.color.orange_500));
                
                // Calculate and display countdown
                String countdownText = getDetailedCountdownText(event.getDate());
                holder.countdownTextView.setText(countdownText);
                
                // Set countdown text color
                setCountdownColor(holder.countdownTextView, event.getDate());
            }
            
            // Show checkbox for todos
            holder.completedCheckBox.setVisibility(View.VISIBLE);
            holder.completedCheckBox.setChecked(event.isCompleted());
            
            // Apply strikethrough text for completed todos
            if (event.isCompleted()) {
                holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.descriptionTextView.setPaintFlags(holder.descriptionTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.titleTextView.setTextColor(Color.GRAY);
                holder.descriptionTextView.setTextColor(Color.GRAY);
                holder.timeTextView.setTextColor(Color.GRAY);
            } else {
                holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.descriptionTextView.setPaintFlags(holder.descriptionTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.titleTextView.setTextColor(Color.BLACK);
                holder.descriptionTextView.setTextColor(Color.BLACK);
                holder.timeTextView.setTextColor(context.getResources().getColor(R.color.gray_500));
            }
            
            // Setup checkbox listener
            holder.completedCheckBox.setOnClickListener(v -> {
                boolean isChecked = holder.completedCheckBox.isChecked();
                
                // Update UI
                if (isChecked) {
                    holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.descriptionTextView.setPaintFlags(holder.descriptionTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.titleTextView.setTextColor(Color.GRAY);
                    holder.descriptionTextView.setTextColor(Color.GRAY);
                    holder.timeTextView.setTextColor(Color.GRAY);
                    holder.typeIndicator.setBackgroundColor(context.getResources().getColor(R.color.gray_500));
                    holder.countdownTextView.setText(context.getString(R.string.completed));
                    holder.countdownTextView.setTextColor(context.getResources().getColor(R.color.gray_500));
                } else {
                    holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    holder.descriptionTextView.setPaintFlags(holder.descriptionTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    holder.titleTextView.setTextColor(Color.BLACK);
                    holder.descriptionTextView.setTextColor(Color.BLACK);
                    holder.timeTextView.setTextColor(context.getResources().getColor(R.color.gray_500));
                    holder.typeIndicator.setBackgroundColor(context.getResources().getColor(R.color.orange_500));
                    
                    // Calculate and display countdown
                    String countdownText = getDetailedCountdownText(event.getDate());
                    holder.countdownTextView.setText(countdownText);
                    
                    // Set countdown text color
                    setCountdownColor(holder.countdownTextView, event.getDate());
                }
                
                // Notify listener
                if (todoCompletedListener != null) {
                    todoCompletedListener.onTodoCompleted(event, isChecked, holder.getAdapterPosition());
                }
            });
        } else {
            // Event - Red
            holder.typeIndicator.setBackgroundColor(context.getResources().getColor(R.color.red_500));
            
            // Hide checkbox for events
            holder.completedCheckBox.setVisibility(View.GONE);
            
            // Remove any strikethrough
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.descriptionTextView.setPaintFlags(holder.descriptionTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.titleTextView.setTextColor(Color.BLACK);
            holder.descriptionTextView.setTextColor(Color.BLACK);
            
            // Calculate and display countdown
            String countdownText = getDetailedCountdownText(event.getDate());
            holder.countdownTextView.setText(countdownText);
            
            // Set countdown text color
            setCountdownColor(holder.countdownTextView, event.getDate());
        }
        
        // Setup click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event, position);
            }
        });
    }
    
    /**
     * Get detailed countdown text including hours and minutes when appropriate
     */
    private String getDetailedCountdownText(Date eventDate) {
        Calendar now = Calendar.getInstance();
        
        // If date is in the past, just show "Expired"
        if (eventDate.getTime() < now.getTimeInMillis()) {
            return context.getString(R.string.past);
        }
        
        // Calculate time difference in milliseconds
        long diffInMillis = eventDate.getTime() - now.getTimeInMillis();
        
        // Convert to days, hours, minutes
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diffInMillis));
        
        // Format countdown text based on time remaining
        if (days > 0) {
            if (days >= 30) {
                // For distant events, just show days
                return days + " " + context.getString(R.string.days);
            } else if (days >= 2) {
                // For events more than 2 days away, show days and hours
                return days + " " + context.getString(R.string.days) + " " + hours + " " + context.getString(R.string.hours);
            } else {
                if (days == 1) {
                    if (hours == 1) {
                        return days + " " + context.getString(R.string.day_singular) + " " + hours + " " + context.getString(R.string.hour);
                    } else {
                        return days + " " + context.getString(R.string.day_singular) + " " + hours + " " + context.getString(R.string.hours);
                    }
                } else {
                    if (hours == 1) {
                        return days + " " + context.getString(R.string.days) + " " + hours + " " + context.getString(R.string.hour);
                    } else {
                        return days + " " + context.getString(R.string.days) + " " + hours + " " + context.getString(R.string.hours);
                    }
                }
            }
        } else if (hours > 0) {
            // Within the same day, but more than an hour
            if (hours == 1) {
                if (minutes == 1) {
                    return hours + " " + context.getString(R.string.hour) + " " + minutes + " " + context.getString(R.string.minute);
                } else {
                    return hours + " " + context.getString(R.string.hour) + " " + minutes + " " + context.getString(R.string.minutes);
                }
            } else {
                if (minutes == 1) {
                    return hours + " " + context.getString(R.string.hours) + " " + minutes + " " + context.getString(R.string.minute);
                } else {
                    return hours + " " + context.getString(R.string.hours) + " " + minutes + " " + context.getString(R.string.minutes);
                }
            }
        } else if (minutes > 0) {
            // Less than an hour
            if (minutes == 1) {
                return minutes + " " + context.getString(R.string.minute);
            } else {
                return minutes + " " + context.getString(R.string.minutes);
            }
        } else {
            // Less than a minute
            return context.getString(R.string.soon);
        }
    }
    
    /**
     * Set countdown text color based on date
     */
    private void setCountdownColor(TextView textView, Date eventDate) {
        // Check if event is today but already expired
        if (eventDate.getTime() < System.currentTimeMillis()) {
            // Expired events - Gray
            textView.setTextColor(context.getResources().getColor(R.color.gray_500));
            return;
        }
        
        // Calculate time difference in milliseconds
        long diffInMillis = eventDate.getTime() - System.currentTimeMillis();
        
        // Convert to days, hours
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) - TimeUnit.DAYS.toHours(days);
        
        if (days == 0 && hours <= 3) {
            // Within 3 hours - Red (urgent)
            textView.setTextColor(context.getResources().getColor(R.color.red_500));
        } else if (days == 0) {
            // Within today - Orange
            textView.setTextColor(context.getResources().getColor(R.color.orange_500));
        } else if (days <= 2) {
            // Within 2 days - Light orange
            textView.setTextColor(context.getResources().getColor(R.color.orange_300));
        } else {
            // More than 2 days - Blue
            textView.setTextColor(context.getResources().getColor(R.color.blue_500));
        }
    }
    
    /**
     * Check if two Calendar objects represent the same day
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Convert Date to Calendar
     */
    private Calendar getCalendarFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
    
    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }
    
    public void updateEvents(List<com.example.appdevelopmentprojectfinal.calendar.Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }
    
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;
        TextView countdownTextView;
        View typeIndicator;
        CheckBox completedCheckBox;
        
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_event_title);
            descriptionTextView = itemView.findViewById(R.id.tv_event_description);
            timeTextView = itemView.findViewById(R.id.tv_event_time);
            countdownTextView = itemView.findViewById(R.id.tv_event_countdown);
            typeIndicator = itemView.findViewById(R.id.view_event_type_indicator);
            completedCheckBox = itemView.findViewById(R.id.checkbox_todo_completed);
        }
    }
} 