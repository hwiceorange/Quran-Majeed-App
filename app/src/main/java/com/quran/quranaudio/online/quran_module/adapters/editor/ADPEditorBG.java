/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.adapters.editor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import static android.provider.MediaStore.Images;

import com.peacedesign.android.utils.ColorUtils;
import com.quran.quranaudio.online.quran_module.components.editor.EditorBG;
import com.quran.quranaudio.online.quran_module.components.editor.VerseEditor;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.databinding.LytEditorAlphaDialogBinding;
import com.quran.quranaudio.online.quran_module.frags.editshare.FragEditorBG;
import com.quran.quranaudio.online.quran_module.utils.gesture.HoverPushOpacityEffect;
import com.quran.quranaudio.online.quran_module.utils.simplified.SimpleSeekbarChangeListener;
import com.quran.quranaudio.online.quran_module.widgets.ColorPreviewerView;
import com.quran.quranaudio.online.quran_module.widgets.bottomSheet.PeaceBottomSheet;
import com.quran.quranaudio.online.quran_module.widgets.bottomSheet.PeaceBottomSheetParams;
import com.quran.quranaudio.online.quran_module.widgets.editor.EditorBGView;
import com.quran.quranaudio.online.quran_module.widgets.editor.EditorSelectImageView;

import java.util.concurrent.atomic.AtomicReference;

public class ADPEditorBG extends RecyclerView.Adapter<ADPEditorBG.VHEditorBG> {
    private final FragEditorBG mFrag;
    private final VerseEditor mEditor;
    private int mSelected;
    private PeaceBottomSheet mDialog;

    public ADPEditorBG(FragEditorBG frag, VerseEditor editor) {
        mFrag = frag;
        mEditor = editor;
        editor.getBGs().add(0, null);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mEditor.getBGs().size();
    }

    @NonNull
    @Override
    public VHEditorBG onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mEditor.getBGs().get(viewType) == null) {
            return new VHEditorBG(new EditorSelectImageView(parent.getContext()));
        } else {
            return new VHEditorBG(new EditorBGView(parent.getContext()));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VHEditorBG holder, int position) {
        EditorBG editorBG = mEditor.getBGs().get(position);
        if (editorBG == null) {
            holder.bindFirstItem();
        } else {
            holder.bind(editorBG);
        }
    }

    public class VHEditorBG extends RecyclerView.ViewHolder {
        public VHEditorBG(@NonNull View view) {
            super(view);
            view.setOnTouchListener(new HoverPushOpacityEffect());
        }

        public void bind(EditorBG bg) {
            EditorBGView bgView = (EditorBGView) itemView;
            if (bg.getBgType() == VerseEditor.BG_TYPE_IMAGE) {
                bgView.setImage(bg.getBgImage());
            } else if (bg.getBgType() == VerseEditor.BG_TYPE_COLORS) {
                bgView.setColors(bg.getBgColors());
            }

            bgView.setSelected(mSelected == getAdapterPosition());
            itemView.setOnClickListener(v -> {
                if (mSelected == getAdapterPosition()) {
                    showAlphaDialog(itemView.getContext());
                    return;
                }

                select(getAdapterPosition());
            });
        }

        private void showAlphaDialog(Context ctx) {
            if (mDialog != null && mDialog.isShowing()) {
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(ctx);
            LytEditorAlphaDialogBinding alphaDialogBinding = LytEditorAlphaDialogBinding.inflate(inflater);

            AtomicReference<ColorPreviewerView> whiteRef = new AtomicReference<>();
            AtomicReference<ColorPreviewerView> blackRef = new AtomicReference<>();

            ColorPreviewerView whiteView = setupColorView(alphaDialogBinding.white, Color.WHITE, blackRef);
            ColorPreviewerView blackView = setupColorView(alphaDialogBinding.black, Color.BLACK, whiteRef);

            whiteRef.set(whiteView);
            blackRef.set(blackView);

            int initialProgress = (int) (mEditor.getBGAlpha() * 100);
            setProgressText(alphaDialogBinding.progressText, initialProgress);
            alphaDialogBinding.seekbar.setProgress(initialProgress);
            alphaDialogBinding.seekbar.setOnSeekBarChangeListener(new SimpleSeekbarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setProgressText(alphaDialogBinding.progressText, progress);

                    float alpha = (float) progress / 100;
                    mEditor.setBGAlpha(alpha);
                    mEditor.getListener().onBGAlphaColorChange(
                        ColorUtils.createAlphaColor(mEditor.getBGAlphaColor(), alpha));
                }
            });

            mDialog = new PeaceBottomSheet();
            PeaceBottomSheetParams p = mDialog.getParams();
            p.setHeaderTitle(ctx.getString(R.string.strTitleAlphaBG));
            p.setContentView(alphaDialogBinding.getRoot());
            p.setWindowDimAmount(0);
            mDialog.show(mFrag.getParentFragmentManager());
        }

        public void setProgressText(TextView progressView, int progress) {
            String progressTxt = progress + "%";
            progressView.setText(progressTxt);
        }

        private ColorPreviewerView setupColorView(ColorPreviewerView colorView, int color, AtomicReference<ColorPreviewerView> otherView) {
            colorView.setBackgroundColor(color);
            colorView.setSelected(mEditor.getBGAlphaColor() == color);
            colorView.setOnClickListener(v -> {
                mEditor.setBGAlphaColor(color);
                mEditor.getListener().onBGAlphaColorChange(ColorUtils.createAlphaColor(color, mEditor.getBGAlpha()));

                colorView.setSelected(true);
                if (otherView.get() != null) {
                    otherView.get().setSelected(false);
                }
            });

            colorView.setOnTouchListener(new HoverPushOpacityEffect());
            return colorView;
        }

        public void bindFirstItem() {
            itemView.setOnClickListener(v -> {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(Images.Media.EXTERNAL_CONTENT_URI, "image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

                mFrag.startActivity4Result(chooserIntent, null);
            });
        }
    }

    public void select(int position) {
        int tmpSelected = mSelected;
        mSelected = position;
        if (mSelected != tmpSelected && tmpSelected >= 0) {
            notifyItemChanged(tmpSelected);
        }
        notifyItemChanged(position);
        mEditor.getListener().onBGChange(mEditor.getBGs().get(position));
    }

    public void selectNew(int position) {
        mSelected = position;
        notifyItemInserted(position);
        mEditor.getListener().onBGChange(mEditor.getBGs().get(position));
    }
}
