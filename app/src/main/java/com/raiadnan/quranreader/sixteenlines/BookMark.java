package com.raiadnan.quranreader.sixteenlines;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.shaheendevelopers.ads.sdk.format.BannerAd;
import java.util.ArrayList;


public class BookMark extends AppCompatActivity {

    BannerAd.Builder bannerAd;
    ListView bkMarkList;
    BookMarkArrayAdapter adapter;
    //public static Boolean checkboxShown=false;
    //public static ArrayList<Integer> checks=new ArrayList<Integer>();
    ArrayList<Integer> PgNoOfBkMarks=new ArrayList<>();
    ArrayList<String> paraName= new ArrayList<>();
    ArrayList<String> SuraName= new ArrayList<>();

    public static MyApplication2 MyApp2 =new MyApplication2();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_mark);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        ImageView imgFavorite = (ImageView) findViewById(R.id.back_btn);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookMark.this.finish();
            }
        });

        ImageView delete_bookmark = (ImageView) findViewById(R.id.del_bkmarks);
        delete_bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Button bkdeletebtn=(Button)findViewById(R.id.bkmrk_delete_btn);
                final Button bkcancelbtn=(Button)findViewById(R.id.bkmrk_cancel_btn);

                bkcancelbtn.setVisibility(View.VISIBLE);
                bkdeletebtn.setVisibility(View.VISIBLE);


                MyApp2.setCheckboxShown(true);
                //bkMarkList.deferNotifyDataSetChanged();
                bkMarkList.setAdapter(adapter);


                bkcancelbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        MyApp2.setCheckboxShown(false);
                        //checkboxShown=false;
                        bkcancelbtn.setVisibility(View.GONE);
                        bkdeletebtn.setVisibility(View.GONE);

                        bkMarkList.setAdapter(adapter);
                    }
                });



                bkdeletebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        for(int i=0;i<MyApp2.getChecks().size();i++){

                            if(MyApp2.getChecks().get(i)==1){

                                int index=i+1;

                                //remove items from the list here for example from ArryList
                                MyApp2.getChecks().remove(i);
                                PgNoOfBkMarks.remove(i);
                                paraName.remove(i);
                                SuraName.remove(i);
                                //similarly remove other items from the list from that particular postion
                                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor =settings.edit();

                                editor.remove("bookmark_"+index);
                                editor.putInt("bookmark_no",settings.getInt("bookmark_no",0)-1);
                                editor.apply();



                                i--;
                            }
                        }
                        MyApp2.setCheckboxShown(false);
                        bkcancelbtn.setVisibility(View.GONE);
                        bkdeletebtn.setVisibility(View.GONE);
                        bkMarkList.setAdapter(adapter);
                    }
                });
            }
        });


        //ArrayList<Integer> PgNoOfBkMarks=new ArrayList<>();
        //ArrayList<String> paraName= new ArrayList<>();
        //ArrayList<String> SuraName= new ArrayList<>();

        for(int index =1; index<=settings.getInt("bookmark_no",0);index++){

            PgNoOfBkMarks.add(index-1,settings.getInt("bookmark_"+index,0));


            MyApp2.getChecks().add(index-1,0);

            for(int para=1;para<=30;para++){
                int nextpara;
                if(para<30) {
                    nextpara = para + 1;
                }else{
                    nextpara =para;
                }
                if(PgNoOfBkMarks.get(index-1)<=PageNo("para"+para)&&PgNoOfBkMarks.get(index-1)>PageNo("para"+nextpara)){  //constrainging page no of paras to get to the bookmarked para

                    paraName.add(index-1,getStringResourceByName("para"+para));
                }else if(para==nextpara&&PgNoOfBkMarks.get(index-1)<=PageNo("para"+para)){
                    paraName.add(index-1,getStringResourceByName("para"+para));
                }
            }

            for(int sura=1;sura<=114;sura++){

                int nextSura;
                if(sura<114) {
                    nextSura = sura + 1;
                }else{
                    nextSura =sura;
                }
                if(PgNoOfBkMarks.get(index-1)<=PageNo("sura"+sura)&&PgNoOfBkMarks.get(index-1)>PageNo("sura"+nextSura)){      //constrainging page no of suras to get to the bookmarked para

                    SuraName.add(index-1,getStringResourceByName("sura"+sura));
                }else if(sura==nextSura&&PgNoOfBkMarks.get(index-1)<=PageNo("sura"+sura)){
                    SuraName.add(index-1,getStringResourceByName("sura"+sura));
                }
            }


        }

        bkMarkList =(ListView)findViewById(R.id.bkListView);
        adapter = new BookMarkArrayAdapter(this, SuraName,paraName,PgNoOfBkMarks);
        bkMarkList.setAdapter(adapter);
        loadBannerAd();

    }

    private void loadBannerAd() {
        bannerAd = new BannerAd.Builder(this)
                .setAdStatus(Constant.AD_STATUS)
                .setAdNetwork(Constant.AD_NETWORK)
                .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
                .setAdMobBannerId(Constant.ADMOB_BANNER_ID)
                .setGoogleAdManagerBannerId(Constant.GOOGLE_AD_MANAGER_BANNER_ID)
                .setFanBannerId(Constant.FAN_BANNER_ID)
                .setUnityBannerId(Constant.UNITY_BANNER_ID)
                .setAppLovinBannerId(Constant.APPLOVIN_BANNER_ID)
                .setAppLovinBannerZoneId(Constant.APPLOVIN_BANNER_ZONE_ID)
                .setDarkTheme(false)
                .build();
    }

    private String getStringResourceByName(String aString) {
        int resId = getResources().getIdentifier(aString, "string", getApplicationContext().getPackageName());
        return getString(resId);
    }

    private int PageNo(String paraOrSurah){
        int code_pg_no;
        int total =550;
        int converter;
        int res2Id= this.getResources().getIdentifier(paraOrSurah,"integer",this.getApplicationContext().getPackageName());

        int page_no = this.getApplicationContext().getResources().getInteger(res2Id);

        converter = page_no-1;
        code_pg_no =total-converter;
        return code_pg_no;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
