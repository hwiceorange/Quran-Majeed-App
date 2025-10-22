package com.quran.quranaudio.online.wudu;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Wudu Guide Activity
 * Displays step-by-step instructions for performing Wudu (ablution)
 */
public class WuduGuideActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WuduStepsAdapter adapter;
    private List<WuduStep> wuduSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        
        setContentView(R.layout.activity_wudu_guide);

        setupToolbar();
        setupRecyclerView();
        loadWuduSteps();
    }

    private void setupToolbar() {
        ImageView backBtn = findViewById(R.id.back_btn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.wudu_steps_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        wuduSteps = new ArrayList<>();
        adapter = new WuduStepsAdapter(this, wuduSteps);
        recyclerView.setAdapter(adapter);
    }

    private void loadWuduSteps() {
        wuduSteps.clear();
        
        // Step 1: Intention (Niyyah)
        wuduSteps.add(new WuduStep(
            1,
            "Make Niyyah (Intention)",
            "النية",
            "Begin by making the intention in your heart to perform wudu for the purpose of purification and worship of Allah. The intention does not need to be spoken aloud. Say 'Bismillah' (In the name of Allah) before starting.",
            "ابدأ بعقد النية في قلبك لأداء الوضوء بغرض الطهارة وعبادة الله. النية لا تحتاج أن تُنطق بصوت عالٍ. قل 'بسم الله' قبل البدء.",
            "wudu_step_01.jpg",
            "Bismillah",
            "بسم الله",
            "Bismillah"
        ));
        
        // Step 2: Wash Hands
        wuduSteps.add(new WuduStep(
            2,
            "Wash Both Hands",
            "غسل اليدين",
            "Wash your hands thoroughly up to the wrists three times. Start with your right hand, using your left hand to wash it, then wash your left hand with your right. Make sure water reaches between your fingers and under your nails. Clean your hands as you would before eating.",
            "اغسل يديك جيداً حتى الرسغين ثلاث مرات. ابدأ باليد اليمنى باستخدام اليد اليسرى لغسلها، ثم اغسل اليد اليسرى باليمنى. تأكد من وصول الماء بين الأصابع وتحت الأظافر.",
            "wudu_step_02.jpg"
        ));
        
        // Step 3: Rinse Mouth
        wuduSteps.add(new WuduStep(
            3,
            "Rinse Your Mouth (Madmadah)",
            "المضمضة",
            "Take water in your right hand and rinse your mouth thoroughly three times. Swish the water around your entire mouth, making sure it reaches all areas including teeth and gums. Spit out the water after each rinse. If you are not fasting, gargle the water.",
            "خذ الماء بيدك اليمنى وتمضمض ثلاث مرات. حرّك الماء في فمك بالكامل، تأكد من وصوله لجميع المناطق بما في ذلك الأسنان واللثة. ابصق الماء بعد كل مضمضة.",
            "wudu_step_03.jpg"
        ));
        
        // Step 4: Sniff Water into Nose
        wuduSteps.add(new WuduStep(
            4,
            "Clean Your Nose (Istinshaq)",
            "الاستنشاق",
            "Take water in your right hand and gently sniff it into your nostrils three times. Then blow out the water using your left hand to clean your nose. If you are not fasting, sniff the water deeply into your nose. Be gentle to avoid discomfort.",
            "خذ الماء بيدك اليمنى واستنشقه برفق في أنفك ثلاث مرات. ثم أخرج الماء باستخدام يدك اليسرى لتنظيف أنفك. إذا لم تكن صائماً، استنشق الماء بعمق في أنفك.",
            "wudu_step_04.jpg"
        ));
        
        // Step 5: Wash Face
        wuduSteps.add(new WuduStep(
            5,
            "Wash Your Face",
            "غسل الوجه",
            "Wash your entire face three times from the hairline to the chin and from ear to ear. Use both hands to splash water on your face and rub gently. Make sure to wash your entire face including the forehead, nose, cheeks, lips, and chin. If you have a beard, run your wet fingers through it.",
            "اغسل وجهك بالكامل ثلاث مرات من منبت الشعر إلى الذقن ومن الأذن إلى الأذن. استخدم كلتا يديك لرش الماء على وجهك وافرك برفق. إذا كانت لديك لحية، مرر أصابعك المبللة خلالها.",
            "wudu_step_05.jpg"
        ));
        
        // Step 6: Wash Arms
        wuduSteps.add(new WuduStep(
            6,
            "Wash Your Arms to the Elbows",
            "غسل الذراعين",
            "Wash your right arm from the fingertips up to and including the elbow three times. Use your left hand to wash your right arm. Then wash your left arm from fingertips to elbow three times using your right hand. Make sure water reaches all parts including the elbow itself.",
            "اغسل ذراعك الأيمن من أطراف الأصابع حتى المرفق وشامل المرفق ثلاث مرات. استخدم يدك اليسرى لغسل ذراعك الأيمن. ثم اغسل ذراعك الأيسر بنفس الطريقة.",
            "wudu_step_06.jpg"
        ));
        
        // Step 7: Wipe Head (Maseh)
        wuduSteps.add(new WuduStep(
            7,
            "Wipe Over Your Head",
            "مسح الرأس",
            "Wet your hands and wipe over your head once, starting from the front (above the forehead) to the back, and then from the back to the front. Use both hands and pass them over your entire head. For women wearing hijab, wipe over the visible hair or the scarf.",
            "بلل يديك وامسح على رأسك مرة واحدة، ابدأ من الأمام (فوق الجبهة) إلى الخلف، ثم من الخلف إلى الأمام. استخدم كلتا يديك ومررهما على رأسك بالكامل.",
            "wudu_step_07.jpg"
        ));
        
        // Step 8: Wipe Ears
        wuduSteps.add(new WuduStep(
            8,
            "Wipe Your Ears",
            "مسح الأذنين",
            "Using the same water from wiping your head, wipe the inside and outside of both ears once. Use your index fingers to clean the inside of your ears and your thumbs to wipe behind your ears. This completes the wiping of the head area.",
            "باستخدام نفس الماء من مسح رأسك، امسح داخل وخارج كلتا أذنيك مرة واحدة. استخدم سبابتيك لتنظيف داخل أذنيك وإبهاميك لمسح خلف أذنيك.",
            "wudu_step_08.jpg"
        ));
        
        // Step 9: Wash Feet
        wuduSteps.add(new WuduStep(
            9,
            "Wash Your Feet to the Ankles",
            "غسل القدمين",
            "Wash your right foot from the toes up to and including the ankles three times. Use your left hand to wash between the toes and ensure water reaches all parts. Then wash your left foot in the same manner. Make sure to wash thoroughly including the heels and between all toes.",
            "اغسل قدمك اليمنى من الأصابع حتى الكاحلين وشامل الكاحلين ثلاث مرات. استخدم يدك اليسرى للغسل بين الأصابع. ثم اغسل قدمك اليسرى بنفس الطريقة.",
            "wudu_step_09.jpg"
        ));
        
        // Step 10: Follow the Order
        wuduSteps.add(new WuduStep(
            10,
            "Perform Steps in Order (Tartib)",
            "الترتيب",
            "It is essential to perform wudu in the prescribed order without interruption: hands, mouth, nose, face, arms, head, ears, and feet. Do not skip any step or change the sequence. Each step should follow the previous one without long breaks. This order is a fundamental requirement of valid wudu.",
            "من الضروري أداء الوضوء بالترتيب المحدد دون انقطاع: اليدين، الفم، الأنف، الوجه، الذراعين، الرأس، الأذنين، والقدمين. لا تتخطى أي خطوة أو تغير التسلسل.",
            "wudu_step_10.jpg"
        ));
        
        // Step 11: Recite Dua After Wudu
        wuduSteps.add(new WuduStep(
            11,
            "Recite the Dua After Wudu",
            "دعاء بعد الوضوء",
            "After completing wudu, it is recommended to recite the shahada and supplication. This dua is highly rewarding and serves as a declaration of faith after purification. Look up toward the sky and recite the dua sincerely. The Prophet Muhammad (peace be upon him) said that whoever performs wudu perfectly and says this dua, the eight gates of Paradise will be opened for them.",
            "بعد إتمام الوضوء، يُستحب قراءة الشهادة والدعاء. هذا الدعاء عظيم الأجر ويُعتبر إعلان الإيمان بعد الطهارة. انظر إلى السماء واقرأ الدعاء بإخلاص.",
            "wudu_step_11.jpg",
            "Ash-hadu an laa ilaaha illallaahu wahdahu laa shareeka lahu, wa ash-hadu anna Muhammadan 'abduhu wa rasooluhu",
            "أشهد أن لا إله إلا الله وحده لا شريك له، وأشهد أن محمداً عبده ورسوله",
            "Ash-hadu an laa ilaaha illallaahu wahdahu laa shareeka lahu, wa ash-hadu anna Muhammadan 'abduhu wa rasooluhu"
        ));
        
        // Step 12: When Wudu is Nullified
        wuduSteps.add(new WuduStep(
            12,
            "Things That Nullify Wudu",
            "نواقض الوضوء",
            "Your wudu becomes invalid if any of the following occur: (1) Anything that exits from the private parts (urine, stool, gas, etc.), (2) Deep sleep where you lose consciousness, (3) Loss of consciousness due to illness, fainting, or intoxication, (4) Touching private parts without a barrier, (5) Eating camel meat. After any of these, you must perform wudu again before prayer. Note: Full ritual bath (ghusl) is required after sexual intercourse or completion of menstrual period.",
            "يبطل وضوءك إذا حدث أي من التالي: (١) أي شيء يخرج من السبيلين (بول، براز، ريح، إلخ)، (٢) النوم العميق، (٣) فقدان الوعي، (٤) لمس العورة بدون حائل، (٥) أكل لحم الإبل. بعد أي من هذه يجب إعادة الوضوء قبل الصلاة.",
            "wudu_step_12.jpg"
        ));
        
        adapter.notifyDataSetChanged();
    }
}

