package com.raiadnan.quranreader.sixteenlines;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;

import androidx.fragment.app.Fragment;

import com.raiadnan.quranreader.R;

//import static com.raiadnan.quranreader.sixteenlines.SixteenLines_Activity.app2;


public class PageFragment extends Fragment {

    private int imageResource;
    private Bitmap bitmap;
    MyApplication2 app2 = new MyApplication2();

    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    //public static Boolean nightMode=false;
    //public static Boolean highlight=false;




    public static PageFragment getInstance(int resourceID) {
        PageFragment f = new PageFragment();
        Bundle args = new Bundle();
        args.putInt("image_source", resourceID);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageResource = getArguments().getInt("image_source");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {



        View view =inflater.inflate(R.layout.fragment_page, container, false);
        //view.setRotationY(180);

        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TouchImageView imageView = (TouchImageView) view.findViewById(R.id.imageView);

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 1;
        o.inDither = false;
        bitmap = BitmapFactory.decodeResource(getResources(), imageResource, o);
        imageView.setImageBitmap(bitmap);


        if(app2.getNightmode()) {
            imageView.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
        }
        if(app2.getHighlight()){
            imageView.setColorFilter(getResources().getColor(R.color.colorOrange), PorterDuff.Mode.MULTIPLY);
        }


    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        bitmap.recycle();
        bitmap = null;
    }
}