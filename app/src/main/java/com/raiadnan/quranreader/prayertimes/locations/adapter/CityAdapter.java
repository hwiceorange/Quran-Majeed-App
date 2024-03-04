package com.raiadnan.quranreader.prayertimes.locations.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.locations.model.City;
import java.util.ArrayList;

public abstract class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {
    
    public ArrayList<City> cities;
    private Context context;

    public abstract void OnItemClick(City city);

    public CityAdapter(Context context2, ArrayList<City> arrayList) {
        this.context = context2;
        this.cities = arrayList;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_city, viewGroup, false));
    }

    public void setCities(ArrayList<City> arrayList) {
        this.cities = arrayList;
        notifyDataSetChanged();
    }

    @SuppressLint({"SetTextI18n"})
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        TextView access$000 = viewHolder.tvCity;
        access$000.setText(this.cities.get(i).getCity() + ", " + this.cities.get(i).getCountry());
    }

    public int getItemCount() {
        return this.cities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public TextView tvCity;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.tvCity = (TextView) view.findViewById(R.id.tv_city);
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    CityAdapter cityAdapter = CityAdapter.this;
                    cityAdapter.OnItemClick((City) cityAdapter.cities.get(ViewHolder.this.getAdapterPosition()));
                }
            });
        }


    }
}
