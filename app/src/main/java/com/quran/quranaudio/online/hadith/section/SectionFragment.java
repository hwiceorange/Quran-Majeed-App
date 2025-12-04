package com.quran.quranaudio.online.hadith.section;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.hadith.adapter.HadithFragment;
import com.quran.quranaudio.online.hadith.data.HadithDataHelper;
import com.quran.quranaudio.online.hadith.data.HadithDownloadDialog;
import com.quran.quranaudio.online.hadith.helper.SharedPreferencesHelper;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import java.util.ArrayList;


public class SectionFragment extends Fragment implements SectionInterface {
    ArrayList<SectionModel> sectionModels;
    String bookId = "";
    TextView actionBarTitle;
    androidx.appcompat.widget.SearchView searchView;

    public static SectionFragment newInstance(String bookId) {
        SectionFragment fragment = new SectionFragment();
        Bundle args = new Bundle();
        args.putString("BookId", bookId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookId = getArguments().getString("BookId");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_section, container, false);

        ImageView actionBarSymbol = requireActivity().findViewById(R.id.actionbar_left);
        actionBarSymbol.setImageResource(R.drawable.ic_arrow_back);
        actionBarSymbol.setOnClickListener(view -> requireActivity().onBackPressed());

        actionBarTitle = requireActivity().findViewById(R.id.actionbar_title);

        sectionModels = new ArrayList<>();
        RecyclerView recyclerView = mView.findViewById(R.id.sectionRecyclerView);
        setupSectionModels();

        SectionAdapter adapter = new SectionAdapter(requireContext(), sectionModels, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        searchView = mView.findViewById(R.id.searchView);
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.getFilter().filter(s);
                return false;
            }
        });

        return mView;
    }

    private void setupSectionModels() {
        try {
            // Note: Only urd (Urdu) and ind (Indonesian) translations exist, no English
            String language = SharedPreferencesHelper.getValue(requireContext(),"language","urd");
            
            // Check if hadith data is available, download if needed
            if (!HadithDataHelper.isHadithAvailable(requireContext(), language, bookId)) {
                if (HadithDataHelper.requiresDownload(language)) {
                    showDownloadDialog(language);
                    return;
                }
            }
            
            String everything = HadithDataHelper.getHadithData(requireContext(), language, bookId);
            
            // Fallback to Urdu if data not available (English doesn't exist)
            if (everything == null) {
                everything = HadithDataHelper.getHadithData(requireContext(), "urd", bookId);
            }
            
            if (everything == null) {
                return; // Data not available
            }
            
            JSONObject hadithBookObject = new JSONObject(everything);
            JSONObject metadata = hadithBookObject.getJSONObject("metadata");
            String bookName = metadata.getString("name");
            actionBarTitle.setText(bookName);

            JSONObject section = metadata.getJSONObject("sections");

            for (int i = 0; i < section.length(); i++) {
                String[] sectionname = new String[i + 1];
                sectionname[i] = section.getString(String.valueOf(i));

                if (!sectionname[i].isEmpty()) {
                    sectionModels.add(new SectionModel(i, sectionname[i]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showDownloadDialog(String language) {
        String languageName = getLanguageDisplayName(language);
        HadithDownloadDialog dialog = HadithDownloadDialog.newInstance(language, languageName);
        dialog.setOnDownloadCompleteListener(success -> {
            if (success && isAdded()) {
                // Reload data after download
                setupSectionModels();
            }
        });
        dialog.show(getParentFragmentManager());
    }
    
    private String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "urd": return "Urdu";
            case "ind": return "Indonesian";
            case "eng": return "English";
            case "ara": return "Arabic";
            default: return languageCode;
        }
    }

    @Override
    public void onItemClick(int position) {
        final FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, HadithFragment.newInstance(bookId, sectionModels.get(position).getSectionNumber(), sectionModels.get(position).getSectionName()), "NewFragmentTag");
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        searchView.clearFocus();
        searchView.setQuery("", false); // clear the text
    }
}