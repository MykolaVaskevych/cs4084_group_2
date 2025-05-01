package com.example.appdevelopmentprojectfinal.marketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.model.Course;
import com.example.appdevelopmentprojectfinal.model.Module;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    public interface CourseClickListener {
        void onCourseClicked(Course course);
    }

    private static final String TAG = "CourseAdapter";
    
    private List<Course> courses;
    private CourseClickListener listener;
    private NumberFormat currencyFormat;

    public CourseAdapter(List<Course> courses, CourseClickListener listener) {
        this.courses = courses;
        this.listener = listener;
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        currencyFormat.setCurrency(Currency.getInstance("EUR"));
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCourseLogo;
        TextView tvModuleCode;
        TextView tvCourseTitle;
        TextView tvAuthor;
        RatingBar ratingBar;
        TextView tvRating;
        TextView tvPrice;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCourseLogo = itemView.findViewById(R.id.ivCourseLogo);
            tvModuleCode = itemView.findViewById(R.id.tvModuleCode);
            tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCourseClicked(courses.get(position));
                }
            });
        }

        public void bind(Course course) {
            // Set course title and author
            tvCourseTitle.setText(course.getName());
            tvAuthor.setText(itemView.getContext().getString(R.string.by_author, course.getAuthor()));

            // Set module code
            tvModuleCode.setText(course.getRelatedModule());

            // Set rating
            ratingBar.setRating((float) course.getAverageRating());
            
            int numReviews = course.getReviews() != null ? course.getReviews().size() : 0;
            tvRating.setText(itemView.getContext().getString(R.string.rating_count, course.getAverageRating(), numReviews));

            // Set price
            tvPrice.setText(currencyFormat.format(course.getPrice()));

            // Set logo based on the module code
            // TODO: do real logo stuff
            String moduleCode = course.getRelatedModule();
            Log.v(TAG, "Setting course logo color for module code: " + moduleCode);
            
            if (moduleCode != null && !moduleCode.isEmpty()) {
                // Use different colors based on the module code's first character
                char firstChar = moduleCode.charAt(0);
                switch (firstChar) {
                    case 'C':
                        ivCourseLogo.setBackgroundColor(0xFF4CAF50); // Green for Computer Science
                        Log.v(TAG, "Setting green color for CS module");
                        break;
                    case 'E':
                        ivCourseLogo.setBackgroundColor(0xFF2196F3); // Blue for Engineering
                        Log.v(TAG, "Setting blue color for Engineering module");
                        break;
                    case 'M':
                        ivCourseLogo.setBackgroundColor(0xFFFF9800); // Orange for Mathematics
                        Log.v(TAG, "Setting orange color for Mathematics module");
                        break;
                    default:
                        ivCourseLogo.setBackgroundColor(0xFF9C27B0); // Purple for others
                        Log.v(TAG, "Setting purple color for other module type");
                        break;
                }
            } else {
                // Default color if no module code
                ivCourseLogo.setBackgroundColor(0xFF9C27B0); // Purple
                Log.v(TAG, "Setting default purple color (no module code)");
            }
        }
    }
}