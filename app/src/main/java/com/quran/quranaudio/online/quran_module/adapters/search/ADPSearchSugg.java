package com.quran.quranaudio.online.quran_module.adapters.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quran.quranaudio.online.quran_module.activities.ActivityQuran_Search;
import com.quran.quranaudio.online.quran_module.components.search.ChapterJumpModel;
import com.quran.quranaudio.online.quran_module.components.search.JuzJumpModel;
import com.quran.quranaudio.online.quran_module.components.search.SearchHistoryModel;
import com.quran.quranaudio.online.quran_module.components.search.SearchResultModelBase;
import com.quran.quranaudio.online.quran_module.components.search.TafsirJumpModel;
import com.quran.quranaudio.online.quran_module.components.search.VerseJumpModel;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.components.search.ChapterJumpModel;
import com.quran.quranaudio.online.databinding.LytReaderJuzSpinnerItemBinding;
import com.quran.quranaudio.online.databinding.LytSearchHistoryItemBinding;
import com.quran.quranaudio.online.quran_module.vh.search.VHChapterJump;
import com.quran.quranaudio.online.quran_module.vh.search.VHJuzJump;
import com.quran.quranaudio.online.quran_module.vh.search.VHSearchResultBase;
import com.quran.quranaudio.online.quran_module.vh.search.VHTafsirJump;
import com.quran.quranaudio.online.quran_module.vh.search.VHVerseJump;
import com.quran.quranaudio.online.quran_module.widgets.IconedTextView;
import com.quran.quranaudio.online.quran_module.widgets.chapterCard.ChapterCard;

import java.util.ArrayList;

public class ADPSearchSugg extends RecyclerView.Adapter<VHSearchResultBase> {
    private final Configuration mConfigs;
    private ActivityQuran_Search mActivitySearch;
    public ArrayList<SearchResultModelBase> mSuggModels = new ArrayList<>();
    private final LayoutInflater mInflater;

    public ADPSearchSugg(Context context) {
        mInflater = LayoutInflater.from(context);
        mConfigs = context.getResources().getConfiguration();

        setHasStableIds(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSuggModels(ActivityQuran_Search activitySearch, ArrayList<SearchResultModelBase> suggModels) {
        mActivitySearch = activitySearch;
        mSuggModels = suggModels;
    }

    @Override
    public int getItemCount() {
        return mSuggModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHSearchResultBase onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        final VHSearchResultBase vh;

        SearchResultModelBase modelBase = mSuggModels.get(position);
        if (modelBase instanceof VerseJumpModel) {
            vh = new VHVerseJump(new AppCompatTextView(parent.getContext()), true);
        } else if (modelBase instanceof ChapterJumpModel) {
            vh = new VHChapterJump(new ChapterCard(parent.getContext()), true);
        } else if (modelBase instanceof JuzJumpModel) {
            vh = new VHJuzJump(LytReaderJuzSpinnerItemBinding.inflate(mInflater, parent, false), true);
        } else if (modelBase instanceof TafsirJumpModel) {
            vh = new VHTafsirJump(new IconedTextView(parent.getContext()), true);
        } else if (modelBase instanceof SearchHistoryModel) {
            vh = new VHSearchHistoryItem(LytSearchHistoryItemBinding.inflate(mInflater, parent, false));
        } else {
            vh = new VHSearchResultBase(new View(parent.getContext()));
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VHSearchResultBase holder, int position) {
        SearchResultModelBase modelBase = mSuggModels.get(position);
        holder.bind(modelBase, position);
    }

    class VHSearchHistoryItem extends VHSearchResultBase {
        private final LytSearchHistoryItemBinding mBinding;

        public VHSearchHistoryItem(LytSearchHistoryItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        @Override
        public void bind(@NonNull SearchResultModelBase parentModel, int pos) {
            if (mConfigs.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mBinding.push.setRotation(90);
            }

            SearchHistoryModel model = (SearchHistoryModel) parentModel;
            bindHistoryItem(mBinding, model);
        }

        private void bindHistoryItem(LytSearchHistoryItemBinding binding, SearchHistoryModel historyModel) {
            binding.getRoot().setOnClickListener(v -> {
                if (mActivitySearch != null) {
                    mActivitySearch.initSearch(historyModel.getText().toString(), false, false);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                removeHistoryCheckpoint(v.getContext(), historyModel);
                return true;
            });
            binding.text.setText(historyModel.getText());
            binding.push.setOnClickListener(v -> {
                if (mActivitySearch != null) {
                    mActivitySearch.pushQuery(historyModel.getText());
                }
            });
        }

        private void removeHistoryCheckpoint(Context context, SearchHistoryModel historyModel) {
            PeaceDialog.Builder builder = PeaceDialog.newBuilder(context);
            builder.setTitle(R.string.strMsgHistoryRemove);
            builder.setMessage(historyModel.getText());
            builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            builder.setNeutralButton(R.string.strLabelCancel, null);
            builder.setPositiveButton(R.string.strLabelRemove, ColorUtils.DANGER, (dialog, which) -> {
                if (mActivitySearch != null) {
                    mActivitySearch.mHistoryDBHelper.removeFromHistory(historyModel.getId(), () -> {
                        String msg = context.getString(R.string.strMsgHistoryRemoved, historyModel.getText());
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                        mSuggModels.remove(getAdapterPosition());
                        notifyItemRemoved(getAdapterPosition());
                    });
                }
            });
            builder.setFocusOnPositive(true);
            builder.show();
        }
    }
}
