package com.example.appdevelopmentprojectfinal.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdevelopmentprojectfinal.R;
import com.example.appdevelopmentprojectfinal.utils.AcademicDatabaseManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for module selection in the module selection dialog
 * 
 * @author Mykola Vaskevych (22372199)
 */
public class ModuleSelectionAdapter extends RecyclerView.Adapter<ModuleSelectionAdapter.ModuleViewHolder> {
    
    private List<AcademicDatabaseManager.Module> moduleList;
    private Set<String> selectedModuleCodes;
    private Set<String> userModuleCodes;
    
    public ModuleSelectionAdapter() {
        moduleList = new ArrayList<>();
        selectedModuleCodes = new HashSet<>();
        userModuleCodes = new HashSet<>();
    }
    
    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_module_selection, parent, false);
        return new ModuleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        AcademicDatabaseManager.Module module = moduleList.get(position);
        
        holder.textModuleCode.setText(module.getCode());
        holder.textModuleName.setText(module.getName());
        holder.textModuleCredits.setText(module.getCredits() + " credits");
        holder.textModuleSemester.setText("Semester " + module.getSemester());
        
        // Show "Required" label if the module is required
        if (module.isRequired()) {
            holder.textModuleRequired.setVisibility(View.VISIBLE);
        } else {
            holder.textModuleRequired.setVisibility(View.GONE);
        }
        
        // Set checkbox state based on whether module is selected
        boolean isSelected = selectedModuleCodes.contains(module.getCode());
        boolean isAlreadyAdded = userModuleCodes.contains(module.getCode());
        
        holder.checkboxModule.setChecked(isSelected || isAlreadyAdded);
        holder.checkboxModule.setEnabled(!isAlreadyAdded);
        
        // Add click listener to the entire item
        holder.itemView.setOnClickListener(v -> {
            if (isAlreadyAdded) {
                return; // Don't allow toggling if already added to user's modules
            }
            
            boolean newCheckedState = !holder.checkboxModule.isChecked();
            holder.checkboxModule.setChecked(newCheckedState);
            
            if (newCheckedState) {
                selectedModuleCodes.add(module.getCode());
            } else {
                selectedModuleCodes.remove(module.getCode());
            }
        });
        
        // Add click listener to the checkbox
        holder.checkboxModule.setOnClickListener(v -> {
            if (isAlreadyAdded) {
                holder.checkboxModule.setChecked(true);
                return; // Don't allow toggling if already added to user's modules
            }
            
            boolean isChecked = holder.checkboxModule.isChecked();
            
            if (isChecked) {
                selectedModuleCodes.add(module.getCode());
            } else {
                selectedModuleCodes.remove(module.getCode());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return moduleList.size();
    }
    
    /**
     * Update the module list
     */
    public void setModules(List<AcademicDatabaseManager.Module> modules) {
        this.moduleList = modules;
        notifyDataSetChanged();
    }
    
    /**
     * Set the user's current modules (to disable selection)
     */
    public void setUserModules(List<String> moduleCodes) {
        this.userModuleCodes = new HashSet<>(moduleCodes);
        notifyDataSetChanged();
    }
    
    /**
     * Get the selected module codes
     */
    public List<String> getSelectedModuleCodes() {
        return new ArrayList<>(selectedModuleCodes);
    }
    
    /**
     * Clear all selections
     */
    public void clearSelections() {
        selectedModuleCodes.clear();
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder for module items
     */
    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkboxModule;
        TextView textModuleCode;
        TextView textModuleName;
        TextView textModuleCredits;
        TextView textModuleSemester;
        TextView textModuleRequired;
        
        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxModule = itemView.findViewById(R.id.checkbox_module);
            textModuleCode = itemView.findViewById(R.id.text_module_code);
            textModuleName = itemView.findViewById(R.id.text_module_name);
            textModuleCredits = itemView.findViewById(R.id.text_module_credits);
            textModuleSemester = itemView.findViewById(R.id.text_module_semester);
            textModuleRequired = itemView.findViewById(R.id.text_module_required);
        }
    }
}