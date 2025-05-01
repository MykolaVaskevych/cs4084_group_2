package com.example.appdevelopmentprojectfinal.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying the user's modules in the profile page
 * 
 * @author Mykola Vaskevych (22372199)
 */
public class UserModulesAdapter extends RecyclerView.Adapter<UserModulesAdapter.ModuleViewHolder> {
    
    private List<String> moduleCodesList;
    private Map<String, AcademicDatabaseManager.Module> modulesDataMap;
    private OnModuleRemoveListener moduleRemoveListener;
    
    /**
     * Listener for module removal events
     */
    public interface OnModuleRemoveListener {
        void onModuleRemove(String moduleCode);
    }
    
    public UserModulesAdapter(OnModuleRemoveListener listener) {
        moduleCodesList = new ArrayList<>();
        modulesDataMap = new HashMap<>();
        this.moduleRemoveListener = listener;
    }
    
    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_module, parent, false);
        return new ModuleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        String moduleCode = moduleCodesList.get(position);
        AcademicDatabaseManager.Module moduleData = modulesDataMap.get(moduleCode);
        
        holder.textModuleCode.setText(moduleCode);
        
        if (moduleData != null) {
            holder.textModuleName.setText(moduleData.getName());
            holder.textModuleCredits.setText(moduleData.getCredits() + " credits");
            holder.textModuleSemester.setText("Semester " + moduleData.getSemester());
        } else {
            holder.textModuleName.setText("Unknown Module");
            holder.textModuleCredits.setText("");
            holder.textModuleSemester.setText("");
        }
        
        holder.btnRemoveModule.setOnClickListener(v -> {
            if (moduleRemoveListener != null) {
                moduleRemoveListener.onModuleRemove(moduleCode);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return moduleCodesList.size();
    }
    
    /**
     * Update the modules list
     */
    public void setModules(List<String> moduleCodes) {
        this.moduleCodesList = moduleCodes;
        notifyDataSetChanged();
    }
    
    /**
     * Update module data for a specific module
     */
    public void updateModuleData(AcademicDatabaseManager.Module module) {
        if (module != null) {
            modulesDataMap.put(module.getCode(), module);
            
            // Notify item changed if it's in the list
            int position = moduleCodesList.indexOf(module.getCode());
            if (position >= 0) {
                notifyItemChanged(position);
            }
        }
    }
    
    /**
     * ViewHolder for module items
     */
    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        TextView textModuleCode;
        TextView textModuleName;
        TextView textModuleCredits;
        TextView textModuleSemester;
        ImageButton btnRemoveModule;
        
        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            textModuleCode = itemView.findViewById(R.id.text_module_code);
            textModuleName = itemView.findViewById(R.id.text_module_name);
            textModuleCredits = itemView.findViewById(R.id.text_module_credits);
            textModuleSemester = itemView.findViewById(R.id.text_module_semester);
            btnRemoveModule = itemView.findViewById(R.id.btn_remove_module);
        }
    }
}