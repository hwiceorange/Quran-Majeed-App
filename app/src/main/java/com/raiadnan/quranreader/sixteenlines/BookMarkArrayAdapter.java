package com.raiadnan.quranreader.sixteenlines;

import static com.raiadnan.quranreader.sixteenlines.BookMark.MyApp2;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;


import com.raiadnan.quranreader.R;

public class BookMarkArrayAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<String> surah;
    private final ArrayList<String> juz;
    private final ArrayList<Integer> page;
    LayoutInflater inflater;

    //MyApplication app2 = new MyApplication();


    public BookMarkArrayAdapter(Context context, ArrayList<String> surah, ArrayList<String> juz, ArrayList<Integer> page) {
        this.context = context;
        this.surah = surah;
        this.juz =juz;
        this.page=page;
        inflater =(LayoutInflater.from(context));

    }


    @Override
    public int getCount() {
        return surah.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    static class ViewHolder{

        private TextView surah;
        private TextView juz;
        private TextView page;
        private CheckBox chkbx;
    }


    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder mViewHolder = new ViewHolder();


        //View juzContents = inflater.inflate(R.layout.activity_juz_contents,parent,false);

        //LinearLayout scrllayout = (LinearLayout)juzContents.findViewById(R.id.scrollViewlayout);

        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null)convertView = vi.inflate(R.layout.bookmark_button,parent,false);

        mViewHolder.surah= (TextView)convertView.findViewById(R.id.bookmark_surah);
        mViewHolder.juz = (TextView) convertView.findViewById(R.id.bookmark_juz);
        mViewHolder.page = (TextView)convertView.findViewById(R.id.bookmark_page);
        mViewHolder.chkbx=(CheckBox)convertView.findViewById(R.id.checkbox);

        mViewHolder.surah.setText(surah.get(position));
        mViewHolder.juz.setText(juz.get(position));

        int code_pg_no=page.get(position);
        int total =549;
        int ShownPageNo=total-code_pg_no+1;

        mViewHolder.page.setText(""+ShownPageNo);



        convertView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                int code_pg_no;
                int total =549;
                int converter;

                //int res2Id= context.getResources().getIdentifier(page[position],"integer",context.getApplicationContext().getPackageName());

                //int page_no = context.getApplicationContext().getResources().getInteger(res2Id);
                //converter = page_no+2;
                //code_pg_no =total-converter;

                Intent i = new Intent(context.getApplicationContext(), ReadActivity.class);
                ReadActivity.ITEM =page.get(position);
                context.startActivity(i);
            }
        });

        if(MyApp2.getCheckboxShown()){
            mViewHolder.chkbx.setVisibility(View.VISIBLE);
        }


        mViewHolder.chkbx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(((CheckBox)v).isChecked()){
                    MyApp2.getChecks().set(position, 1);
                }
                else{
                    MyApp2.getChecks().set(position, 0);
                }

            }
        });

        return convertView;
    }




}


