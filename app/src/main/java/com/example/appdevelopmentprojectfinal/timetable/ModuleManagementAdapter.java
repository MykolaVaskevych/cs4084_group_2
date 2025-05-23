package com.example.appdevelopmentprojectfinal.timetable;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.appdevelopmentprojectfinal.utils.JsonUtil;

public class ModuleManagementAdapter extends RecyclerView.Adapter<ModuleManagementAdapter.ModuleViewHolder> {

    private final List<ModuleGroup> moduleGroups = new ArrayList<>();
    private final OnModuleVisibilityChangedListener listener;

    private static final int[] MODULE_COLORS = {
            Color.parseColor("#FFCDD2"),
            Color.parseColor("#C8E6C9"),
            Color.parseColor("#BBDEFB"),
            Color.parseColor("#FFE0B2"),
            Color.parseColor("#E1BEE7"),
            Color.parseColor("#F8BBD0"),
            Color.parseColor("#D7CCC8"),
            Color.parseColor("#CFD8DC"),
            Color.parseColor("#B2EBF2"),
            Color.parseColor("#B3E5FC")
    };

    public interface OnModuleVisibilityChangedListener {
        void onModuleVisibilityChanged();
    }

    public static class ModuleGroup {
        private final Module module;
        private final List<ModuleSchedule> schedules = new ArrayList<>();
        private boolean allVisible = true;

        public ModuleGroup(Module module) {
            this.module = module;
        }

        public void addSchedule(ModuleSchedule schedule) {
            schedules.add(schedule);
            if (!schedule.isVisible()) {
                allVisible = false;
            }
        }

        public Module getModule() {
            return module;
        }

        public List<ModuleSchedule> getSchedules() {
            return schedules;
        }

        public boolean isAllVisible() {
            return allVisible;
        }

        public void setAllVisible(boolean visible) {
            allVisible = visible;
            for (ModuleSchedule schedule : schedules) {
                schedule.setVisible(visible);
            }
        }

        public String getSchedulesString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < schedules.size(); i++) {
                ModuleSchedule schedule = schedules.get(i);
                TimeSlot slot = schedule.getTimeSlot();
                builder.append(slot.getDay())
                        .append(" ")
                        .append(slot.getStartTime())
                        .append("-")
                        .append(slot.getEndTime())
                        .append(" (")
                        .append(slot.getLocation())
                        .append(")");

                if (i < schedules.size() - 1) {
                    builder.append("\n");
                }
            }
            return builder.toString();
        }
    }

    public ModuleManagementAdapter(List<ModuleSchedule> moduleSchedules, OnModuleVisibilityChangedListener listener) {
        this.listener = listener;

        Map<String, ModuleGroup> moduleMap = new HashMap<>();

        for (ModuleSchedule schedule : moduleSchedules) {
            Module module = schedule.getModule();
            String moduleCode = module.getCode();

            if (!moduleMap.containsKey(moduleCode)) {
                moduleMap.put(moduleCode, new ModuleGroup(module));
            }

            moduleMap.get(moduleCode).addSchedule(schedule);
        }

        Log.i("TAG", moduleMap.values().toString());
        moduleGroups.addAll(moduleMap.values());
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_module_management, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        ModuleGroup moduleGroup = moduleGroups.get(position);
        Module module = moduleGroup.getModule();

        holder.moduleTitle.setText(module.getCode() + ": " + module.getName());
        holder.moduleLecturer.setText(module.getLecturer());
        holder.moduleSchedule.setText(moduleGroup.getSchedulesString());

        int colorIndex = (Integer.parseInt(module.getCode().replaceAll("[^0-9]", "")) % 100) % MODULE_COLORS.length;
        holder.cardView.setCardBackgroundColor(MODULE_COLORS[colorIndex]);

        JsonUtil jsonUtil = new JsonUtil();
        Context tempContext = holder.cardView.getContext();
        Boolean moduleStatus = jsonUtil.getShowStatusForModule(tempContext, module.getCode());

        /*holder.visibilityToggle.setChecked(moduleStatus);
        moduleGroup.setAllVisible(moduleStatus);*/
        
        boolean isVisible = moduleStatus != null ? moduleStatus : true;
        holder.visibilityToggle.setChecked(isVisible);
        moduleGroup.setAllVisible(isVisible);
        
        holder.visibilityToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            moduleGroup.setAllVisible(isChecked);
            listener.onModuleVisibilityChanged();
            jsonUtil.updateShowStatusAndSave(tempContext, module.getCode(), isChecked);
        });

//        // Add scroll button listeners
//        holder.btnScrollUp.setOnClickListener(v -> {
//            RecyclerView recyclerView = (RecyclerView) holder.itemView.getParent();
//            if (recyclerView != null) {
//                recyclerView.smoothScrollBy(0, -200); // Scroll up by 200px
//            }
//        });
//
//        holder.btnScrollDown.setOnClickListener(v -> {
//            RecyclerView recyclerView = (RecyclerView) holder.itemView.getParent();
//            if (recyclerView != null) {
//                recyclerView.smoothScrollBy(0, 200); // Scroll down by 200px
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return moduleGroups.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        TextView moduleTitle;
        TextView moduleLecturer;
        TextView moduleSchedule;
        Switch visibilityToggle;
        CardView cardView;

        ImageButton btnScrollUp;
        ImageButton btnScrollDown;

        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            moduleTitle = itemView.findViewById(R.id.module_title);
            moduleLecturer = itemView.findViewById(R.id.module_lecturer);
            moduleSchedule = itemView.findViewById(R.id.module_schedule);
            visibilityToggle = itemView.findViewById(R.id.visibility_toggle);
            cardView = (CardView) itemView;

//            btnScrollUp = itemView.findViewById(R.id.btn_scroll_up);
//            btnScrollDown = itemView.findViewById(R.id.btn_scroll_down);
        }
    }
}