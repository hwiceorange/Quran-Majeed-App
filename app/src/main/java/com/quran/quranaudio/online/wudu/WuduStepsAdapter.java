package com.quran.quranaudio.online.wudu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.R;

import java.util.List;

/**
 * Adapter for Wudu Steps RecyclerView
 */
public class WuduStepsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_STEP = 0;
    private static final int VIEW_TYPE_DISCLAIMER = 1;

    private Context context;
    private List<WuduStep> steps;

    public WuduStepsAdapter(Context context, List<WuduStep> steps) {
        this.context = context;
        this.steps = steps;
    }

    @Override
    public int getItemViewType(int position) {
        // Last item is the disclaimer
        return position == steps.size() ? VIEW_TYPE_DISCLAIMER : VIEW_TYPE_STEP;
    }

    @Override
    public int getItemCount() {
        // Steps + 1 disclaimer
        return steps.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DISCLAIMER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_wudu_disclaimer, parent, false);
            return new DisclaimerViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_wudu_step, parent, false);
            return new StepViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof StepViewHolder) {
            WuduStep step = steps.get(position);
            ((StepViewHolder) holder).bind(step);
        }
        // Disclaimer view doesn't need binding (static content)
    }

    class StepViewHolder extends RecyclerView.ViewHolder {
        TextView stepTitle, stepTitleArabic, stepDescription, stepDescriptionArabic;
        TextView duaText, duaArabic, duaTransliteration;
        ImageView stepImage;
        LinearLayout duaContainer;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepTitle = itemView.findViewById(R.id.step_title);
            stepTitleArabic = itemView.findViewById(R.id.step_title_arabic);
            stepDescription = itemView.findViewById(R.id.step_description);
            stepDescriptionArabic = itemView.findViewById(R.id.step_description_arabic);
            stepImage = itemView.findViewById(R.id.step_image);
            duaContainer = itemView.findViewById(R.id.dua_container);
            duaText = itemView.findViewById(R.id.dua_text);
            duaArabic = itemView.findViewById(R.id.dua_arabic);
            duaTransliteration = itemView.findViewById(R.id.dua_transliteration);
        }

        public void bind(WuduStep step) {
            // Set title
            stepTitle.setText(step.getStepNumber() + ". " + step.getTitle());
            stepTitleArabic.setText(step.getTitleArabic());
            
            // Set description
            stepDescription.setText(step.getDescription());
            stepDescriptionArabic.setText(step.getDescriptionArabic());
            
            // Load image
            int imageResId = context.getResources().getIdentifier(
                step.getImageName().replace(".jpg", ""),
                "drawable",
                context.getPackageName()
            );
            
            if (imageResId != 0) {
                stepImage.setImageResource(imageResId);
                stepImage.setVisibility(View.VISIBLE);
            } else {
                stepImage.setVisibility(View.GONE);
            }
            
            // Show/hide dua section
            if (step.hasDua()) {
                duaContainer.setVisibility(View.VISIBLE);
                duaArabic.setText(step.getDuaArabic());
                duaText.setText(step.getDua());
                if (step.getDuaTransliteration() != null) {
                    duaTransliteration.setText(step.getDuaTransliteration());
                    duaTransliteration.setVisibility(View.VISIBLE);
                } else {
                    duaTransliteration.setVisibility(View.GONE);
                }
            } else {
                duaContainer.setVisibility(View.GONE);
            }
        }
    }

    class DisclaimerViewHolder extends RecyclerView.ViewHolder {
        public DisclaimerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

