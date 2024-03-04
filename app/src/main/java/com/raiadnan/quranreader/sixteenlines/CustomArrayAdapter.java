package com.raiadnan.quranreader.sixteenlines;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.raiadnan.quranreader.R;


public class CustomArrayAdapter extends BaseAdapter {
    private final Context context;
    private final String[] eng;
    private final String[] arb;
    private final String[] srNum;
    private final String[] surahNum;
    LayoutInflater inflater;


    public CustomArrayAdapter(Context context, String[] eng, String[] arb,String[] srNum, String[] surahNum){
        this.context = context;
        this.eng = eng;
        this.arb =arb;
        this.srNum=srNum;
        this.surahNum=surahNum;
        inflater =(LayoutInflater.from(context));

    }


    @Override
    public int getCount() {
        return eng.length;
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

        private TextView eng;
        private TextView arb;
        private TextView srNum;
        private TextView surahNum;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder = new ViewHolder();


        //View juzContents = inflater.inflate(R.layout.activity_juz_contents,parent,false);

        //LinearLayout scrllayout = (LinearLayout)juzContents.findViewById(R.id.scrollViewlayout);

        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null)convertView = vi.inflate(R.layout.custom_button,parent,false);

        mViewHolder.eng= (TextView)convertView.findViewById(R.id.engBtn);
        mViewHolder.arb = (TextView) convertView.findViewById(R.id.arbBtn);
         //  mViewHolder.srNum = (TextView) convertView.findViewById(R.id.count_sr);
        mViewHolder.surahNum = (TextView) convertView.findViewById(R.id.count_sr);



        mViewHolder.eng.setText(eng[position]);
        mViewHolder.arb.setText(arb[position]);
        //   mViewHolder.srNum.setText(srNum[position]);
        mViewHolder.surahNum.setText(surahNum[position]);





        convertView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                int code_pg_no;
                int total =549;
                int converter;

                int res2Id= context.getResources().getIdentifier(srNum[position],"integer",context.getApplicationContext().getPackageName());

                int page_no = context.getApplicationContext().getResources().getInteger(res2Id);
                converter = page_no-1;
                code_pg_no =total-converter;

                Intent i = new Intent(context.getApplicationContext(), ReadActivity.class);
                ReadActivity.ITEM =code_pg_no;
                context.startActivity(i);
            }
        });

        return convertView;
    }




}

