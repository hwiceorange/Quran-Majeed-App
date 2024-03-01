package com.quran.quranaudio.online.hadith.book;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.R;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.MyViewHolder> {
    private  final BookInterface bookInterface;

    Context context;
    ArrayList<BookModel> bookModels;

    public BookAdapter(Context context, ArrayList<BookModel> bookModels, BookInterface bookInterface) {
        this.context = context;
        this.bookModels = bookModels;
        this.bookInterface = bookInterface;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recyclerview_book,parent,false);
        return new MyViewHolder(view, bookInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bookName.setText(bookModels.get(position).getBookName());
        holder.bookScholar.setText(bookModels.get(position).getBookScholar());
        holder.materialCardViewIcon.setCardBackgroundColor(bookModels.get(position).getBookIconColor());
        holder.bookIconText.setText(bookModels.get(position).getBookIconText());
    }

    @Override
    public int getItemCount() {
        return bookModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView bookName;
        TextView bookScholar;
        CardView materialCardViewIcon;
        TextView bookIconText;

        public MyViewHolder(@NonNull View itemView, BookInterface bookInterface) {
            super(itemView);

        bookName = itemView.findViewById(R.id.bookName);
        bookScholar = itemView.findViewById(R.id.bookScholar);
        materialCardViewIcon = itemView.findViewById(R.id.materialCardViewIcon);
        bookIconText = itemView.findViewById(R.id.bookIconText);

        itemView.setOnClickListener(view -> {
            if (bookInterface != null) {
                int pos = getBindingAdapterPosition();

                if(pos!=RecyclerView.NO_POSITION) {
                    bookInterface.onItemClick(pos);
                }
            }
        });
        }
    }
}
