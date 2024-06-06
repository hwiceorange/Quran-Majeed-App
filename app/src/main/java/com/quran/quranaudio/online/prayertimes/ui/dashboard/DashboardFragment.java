package com.quran.quranaudio.online.prayertimes.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.prayertimes.ui.timingtable.TimingTableActivity;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;

import java.util.ArrayList;


public class DashboardFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.dashboard_recycler_view);

        ArrayList<DashboardModel> dashboardModelArrayList = new ArrayList<>();

        int heads[] = {
              //  R.string.title_qibla_direction,
                R.string.title_calendar,
                R.string.gregorian_hijri_calendar};
            //    R.string.quran,
             //   R.string.names_view_title,
             //   R.string.daily_verse_title};

        int subs[] = {
             //   R.string.desc_qibla_direction,
                R.string.desc_calendar_view_title,
                R.string.desc_gregorian_hijri_calendar};
           //     R.string.desc_quran,
              //  R.string.ndesc_ames_view_title,
             //   R.string.daily_verse__description};

        int images[] = {
               // R.drawable.ic_compass_24dp,
                R.drawable.ic_table_24dp,
                R.drawable.ic_calendar_24dp};
             //   R.drawable.ic_quran_24dp,
             //   R.drawable.ic_alah_24dp,
             //   R.drawable.ic_book_24dp};

        Intent intents[] = {
                //new Intent(getActivity(), CompassActivity.class),
                new Intent(getActivity(), TimingTableActivity.class),
                new Intent(getActivity(), CalendarActivity.class),
              /*  new Intent(getActivity(), QuranIndexActivity.class),
                new Intent(getActivity(), NamesActivity.class),
                new Intent(getActivity(), DailyVerseActivity.class)*/
        };

        for (int count = 0; count < heads.length; count++) {
            DashboardModel dashboardModel = new DashboardModel();
            dashboardModel.setHead(getResources().getString(heads[count]));
            dashboardModel.setSub(getResources().getString(subs[count]));
            dashboardModel.setImage(images[count]);
            dashboardModel.setIntent(intents[count]);
            dashboardModelArrayList.add(dashboardModel);
        }
        recyclerView.setLayoutManager(new GridLayoutManager(requireActivity().getApplicationContext(), 2));
        DashboardAdapter dashboardAdapter = new DashboardAdapter(dashboardModelArrayList, getActivity());
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(dashboardAdapter);

        return root;
    }
}
