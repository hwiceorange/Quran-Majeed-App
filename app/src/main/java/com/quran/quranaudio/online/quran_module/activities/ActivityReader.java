package com.quran.quranaudio.online.quran_module.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.quran.quranaudio.online.quran_module.utils.IntentUtils.INTENT_ACTION_OPEN_READER;
import static com.quran.quranaudio.online.quran_module.utils.quran.QuranUtils.doesVerseRangeEqualWhole;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_CHAPTER_NO;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_JUZ_NO;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_PENDING_SCROLL;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_READER_STYLE;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_READ_TYPE;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_VERSES;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

import com.quran.quranaudio.online.common.rate.RatePromptManager;
import com.quran.quranaudio.online.quran_module.components.quran.Quran;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel;
import com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel;
import com.quran.quranaudio.online.quran_module.reader_managers.Navigator;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.quran_module.suppliments.ReaderLayoutManager;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages;
import com.quran.quranaudio.online.quran_module.adapters.ADPReader;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation;
import com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair;
import com.quran.quranaudio.online.databinding.ActivityReaderBinding;
import com.quran.quranaudio.online.quran_module.db.readHistory.ReadHistoryDBHelper;
import com.quran.quranaudio.online.quests.helper.QuranReadingTracker;
import com.quran.quranaudio.online.features.Helper.LastSurahAndAyahHelper;

import com.quran.quranaudio.online.quran_module.utils.quran.QuranUtils;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;
import com.quran.quranaudio.online.quran_module.utils.reader.recitation.RecitationUtils;
import com.quran.quranaudio.online.quran_module.utils.reader.recitation.player.RecitationPlayerParams;
import com.quran.quranaudio.online.quran_module.utils.services.RecitationService;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader;
import com.quran.quranaudio.online.quran_module.utils.thread.runner.CallableTaskRunner;
import com.quran.quranaudio.online.quran_module.utils.thread.tasks.BaseCallableTask;
import com.quran.quranaudio.online.quran_module.utils.univ.Codes;
import com.quran.quranaudio.online.quran_module.utils.univ.Keys;
import com.quran.quranaudio.online.quran_module.utils.verse.VerseUtils;
import com.quran.quranaudio.online.quran_module.views.reader.VerseView;
import com.quran.quranaudio.online.quran_module.views.reader.verseSpinner.VerseSpinnerItem;
import com.quran.quranaudio.online.quran_module.views.readerSpinner2.adapters.VerseSelectorAdapter2;
import com.quran.quranaudio.online.quran_module.views.recitation.RecitationPlayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import kotlin.Pair;

@SuppressWarnings("deprecation")
public class ActivityReader extends ReaderPossessingActivity {
    public static final String KEY_RECITER_CHANGED = "reciter.changed";
    public static final String KEY_TRANSLATION_RECITER_CHANGED = "translation_reciter.changed";
    public static final String KEY_SCRIPT_CHANGED = "script.changed";
    public static final String KEY_TAFSIR_CHANGED = "tafsir.changed";

    public final CallableTaskRunner<ArrayList<QuranPageModel>> mPagesTaskRunner = new CallableTaskRunner<>();
    public ReaderParams mReaderParams;
    public Navigator mNavigator;
    public RecitationPlayer mPlayer;
    public boolean persistProgressDialog4PendingTask;
    public ActivityReaderBinding mBinding;
    public ReaderLayoutManager mLayoutManager;
    private boolean mProtectFromPlayerReset;
    public RecitationService mPlayerService;
    private final ServiceConnection mPlayerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            android.util.Log.d("ActivityReader", "🔌 onServiceConnected: Service connected!");
            
            if (service instanceof RecitationService.LocalBinder) {
                mPlayerService = ((RecitationService.LocalBinder) service).getService();
                android.util.Log.d("ActivityReader", "🔌 mPlayerService obtained successfully");

                if (mPlayer != null) {
                    mPlayer.setService(mPlayerService);
                    android.util.Log.d("ActivityReader", "🔌 mPlayer.setService() called");
                }

                mPlayerService.setRecitationPlayer(mPlayer, ActivityReader.this);
                android.util.Log.d("ActivityReader", "🔌 setRecitationPlayer() called");

                boolean isPlaying = mPlayerService.isPlaying();
                android.util.Log.d("ActivityReader", "🔌 mPlayerService.isPlaying() = " + isPlaying);
                
                if (!isPlaying) {
                    Chapter currChapter = mReaderParams.currChapter;
                    int currJuzNo = mReaderParams.currJuzNo;
                    QuranMeta quranMeta = mQuranMetaRef.get();
                    
                    android.util.Log.d("ActivityReader", "🔌 currChapter = " + (currChapter != null ? "Chapter " + currChapter.getChapterNumber() : "null"));
                    android.util.Log.d("ActivityReader", "🔌 readType = " + mReaderParams.readType);
                    android.util.Log.d("ActivityReader", "🔌 currJuzNo = " + currJuzNo);

                    if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ && currJuzNo > 0 && quranMeta != null) {
                        android.util.Log.d("ActivityReader", "🔌 Entering JUZ mode initialization");
                        mPlayerService.onJuzChanged(currJuzNo, quranMeta);
                    } else if (currChapter != null) {
                        android.util.Log.d("ActivityReader", "🔌 Entering CHAPTER mode initialization");
                        final int fromVerse;
                        final int toVerse;
                        Pair<Integer, Integer> verseRange = mReaderParams.verseRange;

                        // 🔥 修复：只有 verseRange 为 null 时才播放整个章节
                        if (verseRange == null) {
                            fromVerse = 1;
                            toVerse = currChapter.getVerseCount();
                        } else {
                            // verseRange 不为 null，使用它的值（无论是单节还是范围）
                            fromVerse = verseRange.getFirst();
                            toVerse = verseRange.getSecond();
                        }

                        // 🔥 修复：如果指定了起始节号，使用它作为 currentVerse
                        int currentVerse = (startVerseNo > 0) ? startVerseNo : mPlayerService.getP().getCurrentVerseNo();
                        
                        android.util.Log.d("ActivityReader", "🟡 onServiceConnected: startVerseNo = " + startVerseNo);
                        android.util.Log.d("ActivityReader", "🟡 onServiceConnected: currentVerse (calculated) = " + currentVerse);
                        android.util.Log.d("ActivityReader", "🟡 onServiceConnected: mPlayerService.getP().getCurrentVerseNo() = " + mPlayerService.getP().getCurrentVerseNo());
                        
                        // 🔥 保存 startVerseNo 用于自动播放，在重置之前保存
                        final int savedStartVerse = startVerseNo;
                        
                        if (startVerseNo > 0) {
                            android.util.Log.d("ActivityReader", "🔧 Using START_VERSE: " + startVerseNo + " for playback initialization");
                            startVerseNo = -1;  // 使用后重置，避免影响后续逻辑
                        }
                        
                        android.util.Log.d("ActivityReader", "🟡 Calling onChapterChanged:");
                        android.util.Log.d("ActivityReader", "🟡   Chapter: " + currChapter.getChapterNumber());
                        android.util.Log.d("ActivityReader", "🟡   fromVerse: " + fromVerse);
                        android.util.Log.d("ActivityReader", "🟡   toVerse: " + toVerse);
                        android.util.Log.d("ActivityReader", "🟡   currentVerse: " + currentVerse);
                        
                        mPlayerService.onChapterChanged(
                            currChapter.getChapterNumber(),
                            fromVerse,
                            toVerse,
                            currentVerse
                        );
                        
                        // 🔥 Daily Quest: 自动播放逻辑（移到这里，在 currChapter 作用域内）
                        // 关键修复：检查 autoPlayAudio 或 savedStartVerse，确保一定触发自动播放
                        if ((autoPlayAudio || savedStartVerse > 0) && !mPlayerService.isPlaying()) {
                            android.util.Log.d("ActivityReader", "🎧 AUTO_PLAY_AUDIO: Triggering automatic playback (autoPlayAudio=" + autoPlayAudio + ", savedStartVerse=" + savedStartVerse + ")");
                            
                            autoPlayAudio = false;  // 只执行一次，避免重复触发
                            
                            // 🔥 保存当前verse信息，用于自动播放
                            final int targetChapter = currChapter.getChapterNumber();
                            final int targetVerse = (savedStartVerse > 0) ? savedStartVerse : fromVerse;
                            
                            android.util.Log.d("ActivityReader", "🎧 Preparing auto-play: Surah " + targetChapter + ", Verse " + targetVerse);
                            
                            // 延迟500ms后自动播放，确保UI已准备好
                            new Handler().postDelayed(() -> {
                                if (mPlayerService != null && mPlayer != null) {
                                    android.util.Log.d("ActivityReader", "🎧 Executing auto-play: Surah " + targetChapter + ", Verse " + targetVerse);
                                    
                                    // 🔥 关键修复：直接使用我们保存的 targetChapter 和 targetVerse
                                    // 不要从服务获取，因为服务的 currentVerse 可能还未正确初始化到播放器
                                    mPlayerService.reciteVerse(new com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair(targetChapter, targetVerse));
                                    
                                    // 播放控制按钮UI也需要更新
                                    mPlayer.reveal();
                                    
                                    // 重置 startVerseNo，避免在 onResume 中重复触发
                                    startVerseNo = -1;
                                }
                            }, 500);
                        }
                    } else {
                        android.util.Log.d("ActivityReader", "🔌 ❌ currChapter is null, cannot initialize player");
                        android.util.Log.d("ActivityReader", "🔌 ❌ This means initQuran() hasn't been called yet");
                        android.util.Log.d("ActivityReader", "🔌 ❌ Will need to rely on onResume() auto-play instead");
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerService.setRecitationPlayer(null, null);
            mPlayerService = null;
        }
    };
    private ReadHistoryDBHelper mReadHistoryDBHelper;
    
    // Daily Quest: Quran Reading Tracker
    private QuranReadingTracker quranReadingTracker;
    private long sessionStartTime = 0;
    private int sessionStartPage = -1;  // 阅读会话的起始页码
    private int sessionEndPage = -1;    // 阅读会话的结束页码
    private int sessionStartSurah = -1;  // 🔥 新增：阅读会话的起始章节
    private int sessionStartAyah = -1;   // 🔥 新增：阅读会话的起始节号
    private int sessionEndSurah = -1;    // 🔥 新增：阅读会话的结束章节
    private int sessionEndAyah = -1;     // 🔥 新增：阅读会话的结束节号
    
    // 🔥 Daily Quest Step 2: 按Page阅读模式追踪
    private int lastCompletedPage = -1;  // 上次已完成计数的页码
    private int currentVisiblePage = -1;  // 当前可见的页码
    private long pageViewStartTime = 0;   // 进入某页的时间戳
    private static final long PAGE_VIEW_THRESHOLD_MS = 3000;  // 页面停留阈值：3秒
    
    // 🔥 Step 3: Juz 阅读模式追踪变量
    private int lastCompletedAyatInJuz = -1;  // Juz 模式下已完成计数的最后一节经文的全局Ayat编号
    private int currentJuzNo = -1;  // 当前正在阅读的Juz编号
    private int currentJuzFirstAyatGlobal = -1;  // 当前Juz的第一节经文的全局Ayat编号
    private int currentJuzLastAyatGlobal = -1;  // 当前Juz的最后一节经文的全局Ayat编号
    private long juzAyatViewStartTime = 0;   // 进入某Ayat的时间戳
    private static final long AYAT_VIEW_THRESHOLD_MS = 3000;  // Ayat停留阈值：3秒
    
    // 🔥 修复：防止单Verse模式重复计数
    private String lastRecordedVerseKey = "";  // 格式: "chapterNo:verseNo"
    
    // Daily Quest: Quran Listening Tracker
    private com.quran.quranaudio.online.quests.helper.QuranListeningTracker quranListeningTracker;
    private boolean isListeningMode = false;
    private int listeningTargetMinutes = 0;
    private boolean autoPlayAudio = false;  // 🔥 新增：自动播放标志
    private int startVerseNo = -1;  // 🔥 新增：起始节号，用于从指定位置开始播放


    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGReaderHeader);
    }

    @Override
    protected int getThemeId() {
        return R.style.Theme_QuranApp_Reader;
    }

    @Override
    protected void onPause() {
        saveReaderState();
        long retentionReadDurationMs = sessionStartTime > 0
                ? Math.max(0L, System.currentTimeMillis() - sessionStartTime) : 0L;
        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(null, this);
        }
        
        // Daily Quest: Track reading session
        if (quranReadingTracker != null && sessionStartTime > 0 && !isListeningMode) {
            // 🔥 修复：根据用户设置的阅读单位来追踪进度
            try {
                // 🔥 修复：检查是否是单Verse模式
                boolean isSingleVerseMode = mReaderParams != null && mReaderParams.isSingleVerse();
                
                if (isSingleVerseMode) {
                    // 单Verse模式：已在 initVerseRange() 中记录，这里跳过以避免重复
                    android.util.Log.d("ActivityReader", "📖 单Verse模式：跳过onStop记录（已在initVerseRange中记录）");
                } else {
                    // 非单Verse模式（章节模式、Juz模式、页面模式）：需要在onStop记录
                    
                    // 🔥 关键修复：在离开页面前，确保结束位置已更新
                    if (sessionEndSurah == -1 || sessionEndAyah == -1) {
                        android.util.Log.w("ActivityReader", "⚠️ 结束位置未更新，尝试最后一次更新");
                        updateCurrentPageNumber();
                    }
                    
                    // 计算实际阅读的 verses 数量
                    int versesRead = calculateVersesRead();
                    
                    if (versesRead > 0) {
                        // 直接记录 verses 数量，让 Tracker 根据配置决定如何处理
                        quranReadingTracker.recordVersesRead(versesRead);
                        android.util.Log.d("ActivityReader", "✅ 记录阅读进度: " + versesRead + " verses");

                        maybeTriggerSurahCompletion(versesRead);

                        com.quran.quranaudio.online.analytics.RetentionFunnel.valueAction(
                                this, "quran_read", retentionReadDurationMs, versesRead);
                        if (retentionReadDurationMs >= 30_000L) {
                            com.quran.quranaudio.online.analytics.RetentionFunnel.firstValue(
                                    this, "quran_read_30s");
                        }
                    } else if (sessionStartPage > 0 && sessionEndPage > 0) {
                        // 回退到页码追踪
                        quranReadingTracker.recordPageRange(sessionStartPage, sessionEndPage);
                        android.util.Log.d("ActivityReader", "✅ 使用页码追踪: " + sessionStartPage + "-" + sessionEndPage);
                    } else {
                        // 最后回退到时间估算
                        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
                        int pagesRead = Math.max(1, (int) (sessionDuration / 120000));
                        quranReadingTracker.recordPagesRead(pagesRead);
                        android.util.Log.d("ActivityReader", "⚠️ 使用时间估算追踪: " + pagesRead + " pages");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ActivityReader", "Failed to track reading progress", e);
            }
            
            // 检查任务完成状态
            quranReadingTracker.checkAndMarkCompleteAsync();
            
            // ⭐ 新增：保存当前位置到 Firestore（Quran Reading 任务）
            saveCurrentPositionToFirestore();
            
            // 重置会话数据
            sessionStartTime = 0;
            sessionStartPage = -1;
            sessionEndPage = -1;
            sessionStartSurah = -1;
            sessionStartAyah = -1;
            sessionEndSurah = -1;
            sessionEndAyah = -1;
            
            // 🔥 修复：重置单Verse追踪标记
            lastRecordedVerseKey = "";
            
            // 🔥 Daily Quest Step 2: 不重置lastCompletedPage，保留跨会话
            // lastCompletedPage在整个应用生命周期内保持，直到新的一天或任务完成
            currentVisiblePage = -1;
            pageViewStartTime = 0;
        }
        
        // 🔥 Daily Quest: Track listening session
        if (quranListeningTracker != null && isListeningMode) {
            // 停止追踪并记录时长
            quranListeningTracker.stopListening();
            
            // 检查是否完成任务
            if (listeningTargetMinutes > 0) {
                quranListeningTracker.checkAndMarkComplete(listeningTargetMinutes);
            }
            
            // ⭐ 新增：保存当前位置到 Firestore（Quran Listening 任务）
            saveCurrentPositionToFirestore();
            
            android.util.Log.d("ActivityReader", "🎧 Listening session ended and position saved");
        }
        
        super.onPause();
    }

    @Override
    protected void onStart() {
        bindPlayerService();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        RatePromptManager.onReaderUsage(this);

        android.util.Log.d("ActivityReader", "🔄 onResume: Called");
        android.util.Log.d("ActivityReader", "🔄 onResume: autoPlayAudio = " + autoPlayAudio);
        android.util.Log.d("ActivityReader", "🔄 onResume: startVerseNo = " + startVerseNo);
        android.util.Log.d("ActivityReader", "🔄 onResume: mPlayerService = " + (mPlayerService != null ? "connected" : "null"));

        if (mPlayer != null) {
            new Handler().postDelayed(() -> mPlayer.reveal(), 500);
        }

        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(mPlayer, this);
            android.util.Log.d("ActivityReader", "🔄 onResume: mPlayerService.isPlaying() = " + mPlayerService.isPlaying());
            
            // 🔥 关键修复：如果服务已经连接，但 onServiceConnected 没有被调用（Activity重启场景）
            // 我们需要在这里触发自动播放
            if (autoPlayAudio && !mPlayerService.isPlaying() && startVerseNo > 0) {
                android.util.Log.d("ActivityReader", "🎧 onResume: Service already connected, triggering auto-play");
                android.util.Log.d("ActivityReader", "🎧 onResume: startVerseNo = " + startVerseNo);
                
                final int targetChapter = mReaderParams.currChapter != null ? mReaderParams.currChapter.getChapterNumber() : 1;
                final int targetVerse = startVerseNo;
                
                android.util.Log.d("ActivityReader", "🎧 onResume: Preparing auto-play: Surah " + targetChapter + ", Verse " + targetVerse);
                
                // 延迟500ms后自动播放，确保UI已准备好
                new Handler().postDelayed(() -> {
                    if (mPlayerService != null && mPlayer != null && !mPlayerService.isPlaying()) {
                        android.util.Log.d("ActivityReader", "🎧 onResume: Executing auto-play: Surah " + targetChapter + ", Verse " + targetVerse);
                        
                        mPlayerService.reciteVerse(new com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair(targetChapter, targetVerse));
                        mPlayer.reveal();
                        
                        // 自动播放后重置标志，避免重复触发
                        autoPlayAudio = false;
                        startVerseNo = -1;
                    }
                }, 500);
            }
        }
        
        // Daily Quest: Initialize trackers and record session start
        if (quranReadingTracker == null) {
            quranReadingTracker = new QuranReadingTracker(this);
        }
        sessionStartTime = System.currentTimeMillis();
        
        // 🔥 修复：检查是否是新的一天，如果是则重置单Verse追踪标记
        // 这确保了每天的计数都从0开始
        if (quranReadingTracker.getTodayPagesRead() == 0) {
            lastRecordedVerseKey = "";
            android.util.Log.d("ActivityReader", "🔄 新的一天开始，重置Verse追踪标记");
        }
        
        // 🔥 新增：打印当前进度状态（用于调试）
        quranReadingTracker.logCurrentProgress();
        
        // 🔥 Daily Quest Step 2: 初始化lastCompletedPage（从今天已完成的pages数开始）
        if (lastCompletedPage == -1 && isPageReadingMode()) {
            lastCompletedPage = quranReadingTracker.getTodayPagesRead();
            android.util.Log.d("ActivityReader", "📄 初始化lastCompletedPage: " + lastCompletedPage);
        }
        
        // 🔥 记录起始页码（从LayoutManager获取）
        updateCurrentPageNumber();
        
        // 🔥 Daily Quest Step 2: 初始化当前可见页码
        updateCurrentVisiblePage();
        
        // 🔥 Daily Quest: Initialize listening tracker if in listening mode
        if (isListeningMode) {
            if (quranListeningTracker == null) {
                quranListeningTracker = new com.quran.quranaudio.online.quests.helper.QuranListeningTracker(this);
            }
            // 如果播放器正在播放，开始追踪
            if (mPlayerService != null && mPlayerService.isPlaying()) {
                quranListeningTracker.startListening();
                android.util.Log.d("ActivityReader", "🎧 Listening tracking started (player already playing)");
            }
        }

        // 情境化"本章测验"入口（纯增量，全 try-catch 兜底，任何异常都不影响阅读器）
        // 章为异步加载，分几次幂等重试以确保拿到 currChapter
        updateSurahQuizEntry();
        try {
            new Handler().postDelayed(this::updateSurahQuizEntry, 500);
            new Handler().postDelayed(this::updateSurahQuizEntry, 1500);
        } catch (Throwable ignored) {}
    }

    /**
     * 情境化联动：若当前章题量足够(≥3)，显示答题入口；点击回到主界面的标准 Quiz 模块。
     * 不再启动第二套独立答题 Activity，避免同一功能出现不同布局、进度与交互模型。
     */
    private void updateSurahQuizEntry() {
        try {
            if (mBinding == null || mBinding.btnSurahQuizEntry == null) return;
            com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter ch =
                    (mReaderParams != null) ? mReaderParams.currChapter : null;
            if (ch == null) {
                mBinding.btnSurahQuizEntry.setVisibility(android.view.View.GONE);
                return;
            }
            final int chNo = ch.getChapterNumber();
            com.quranaudio.quiz.quiz.QuestionResponse.countBySurahAsync(chNo, count -> {
                try {
                    if (isFinishing() || isDestroyed() || mBinding == null || mBinding.btnSurahQuizEntry == null) return;
                    if (count >= 3) {
                        mBinding.btnSurahQuizEntry.setVisibility(android.view.View.VISIBLE);
                        mBinding.btnSurahQuizEntry.setOnClickListener(v -> {
                            try {
                                startActivity(
                                        com.quran.quranaudio.online.navigation.QuizModuleNavigator
                                                .createIntent(ActivityReader.this)
                                );
                                finish();
                            } catch (Throwable t) {
                                android.util.Log.e("ActivityReader", "standard quiz launch failed", t);
                            }
                        });
                    } else {
                        mBinding.btnSurahQuizEntry.setVisibility(android.view.View.GONE);
                    }
                } catch (Throwable t) {
                    android.util.Log.e("ActivityReader", "surah quiz entry update failed", t);
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("ActivityReader", "surah quiz entry failed", t);
        }
    }

    /**
     * 更新当前阅读的页码和经文位置（用于Daily Quest追踪）
     */
    private void updateCurrentPageNumber() {
        try {
            if (mLayoutManager == null || mBinding == null || mBinding.readerVerses == null) {
                return;
            }
            
            int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
            
            if (firstVisiblePosition < 0) {
                return;
            }
            
            RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
            
            // 🔥 处理不同的 Adapter 类型
            if (adapter instanceof ADPReader) {
                // Translation/Verse view - 直接获取 verse 信息
                ADPReader readerAdapter = (ADPReader) adapter;
                com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel firstItem = readerAdapter.getItem(firstVisiblePosition);
                
                if (firstItem != null && firstItem.getVerse() != null) {
                    // 记录起始位置（只在会话开始时）
                    if (sessionStartSurah == -1) {
                        sessionStartSurah = firstItem.getVerse().chapterNo;
                        sessionStartAyah = firstItem.getVerse().verseNo;
                        android.util.Log.d("ActivityReader", "📖 会话起始: Surah " + sessionStartSurah + ", Ayah " + sessionStartAyah);
                    }
                }
                
                // 🔥 修复：始终更新结束位置（即使 firstItem 为 null）
                if (lastVisiblePosition >= 0) {
                    com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel lastItem = readerAdapter.getItem(lastVisiblePosition);
                    if (lastItem != null && lastItem.getVerse() != null) {
                        sessionEndSurah = lastItem.getVerse().chapterNo;
                        sessionEndAyah = lastItem.getVerse().verseNo;
                        android.util.Log.d("ActivityReader", "📖 会话结束更新: Surah " + sessionEndSurah + ", Ayah " + sessionEndAyah);
                    }
                }
            } else if (adapter instanceof ADPQuranPages) {
                // Page view - 获取 page 和 verse 信息
                ADPQuranPages pageAdapter = (ADPQuranPages) adapter;
                com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel firstPage = pageAdapter.getPageModel(firstVisiblePosition);
                
                if (firstPage != null) {
                    // 记录页码
                    if (sessionStartPage == -1) {
                        sessionStartPage = firstPage.getPageNo();
                        android.util.Log.d("ActivityReader", "📖 会话起始页: " + sessionStartPage);
                    }
                    
                    // 记录第一节经文
                    if (sessionStartSurah == -1 && firstPage.getSections() != null && !firstPage.getSections().isEmpty()) {
                        com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel firstSection = firstPage.getSections().get(0);
                        sessionStartSurah = firstSection.getChapterNo();
                        // getFromToVerses() 返回 int[]{fromVerse, toVerse}
                        int[] fromToVerses = firstSection.getFromToVerses();
                        if (fromToVerses != null && fromToVerses.length >= 1) {
                            sessionStartAyah = fromToVerses[0];  // 起始经文号
                        }
                    }
                    
                    // 持续更新结束页和结束经文
                    if (lastVisiblePosition >= 0) {
                        com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel lastPage = pageAdapter.getPageModel(lastVisiblePosition);
                        if (lastPage != null) {
                            sessionEndPage = lastPage.getPageNo();
                            
                            // 获取最后一节经文
                            if (lastPage.getSections() != null && !lastPage.getSections().isEmpty()) {
                                java.util.List<com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel> sections = lastPage.getSections();
                                com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel lastSection = sections.get(sections.size() - 1);
                                sessionEndSurah = lastSection.getChapterNo();
                                // getFromToVerses() 返回 int[]{fromVerse, toVerse}
                                int[] fromToVerses = lastSection.getFromToVerses();
                                if (fromToVerses != null && fromToVerses.length >= 2) {
                                    sessionEndAyah = fromToVerses[1];  // 结束经文号
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to update page/verse tracking", e);
        }
    }
    
    /**
     * 🔥 Daily Quest Step 2: 检查页面停留时间并计数
     */
    private void checkPageViewDuration() {
        if (!isPageReadingMode() || quranReadingTracker == null || isListeningMode) {
            return;
        }
        
        try {
            // 检查是否有有效的当前页码
            if (currentVisiblePage <= 0) {
                return;
            }
            
            // 计算停留时间
            long viewDuration = System.currentTimeMillis() - pageViewStartTime;
            
            // 🔥 关键逻辑：只有停留超过3秒 且 currentPage > lastCompletedPage 时才计数
            if (viewDuration >= PAGE_VIEW_THRESHOLD_MS && currentVisiblePage > lastCompletedPage) {
                // 计算阅读的页数（从lastCompletedPage+1到currentVisiblePage）
                int pagesRead = currentVisiblePage - Math.max(lastCompletedPage, 0);
                
                if (pagesRead > 0) {
                    quranReadingTracker.recordPagesRead(pagesRead);
                    android.util.Log.d("ActivityReader", "📄 Page计数：+" + pagesRead + " pages (Page " + 
                        (lastCompletedPage + 1) + " → " + currentVisiblePage + ")，停留时间：" + viewDuration + "ms");
                    
                    // 更新lastCompletedPage
                    lastCompletedPage = currentVisiblePage;
                    
                    // 立即检查任务完成状态
                    quranReadingTracker.checkAndMarkCompleteAsync();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to check page view duration", e);
        }
    }
    
    /**
     * 🔥 Daily Quest Step 2: 滚动时持续更新当前可见页码
     */
    private void updateCurrentVisiblePage() {
        if (!isPageReadingMode() || mBinding == null || mBinding.readerVerses == null) {
            return;
        }
        
        try {
            RecyclerView recyclerView = mBinding.readerVerses;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                
                // 获取中间可见项的位置
                int firstVisiblePos = linearLayoutManager.findFirstVisibleItemPosition();
                int lastVisiblePos = linearLayoutManager.findLastVisibleItemPosition();
                int middlePosition = (firstVisiblePos + lastVisiblePos) / 2;
                
                if (middlePosition >= 0 && middlePosition < recyclerView.getAdapter().getItemCount()) {
                    // 从 Adapter 中获取对应的页码
                    RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                    if (adapter instanceof ADPQuranPages) {
                        ADPQuranPages pagesAdapter = (ADPQuranPages) adapter;
                        if (middlePosition < pagesAdapter.getItemCount()) {
                            com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel pageModel = pagesAdapter.getPageModel(middlePosition);
                            if (pageModel != null) {
                                int newPage = pageModel.getPageNo();
                                
                                // 如果页码发生变化，重置计时器
                                if (newPage != currentVisiblePage) {
                                    currentVisiblePage = newPage;
                                    pageViewStartTime = System.currentTimeMillis();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to update current visible page", e);
        }
    }
    
    /**
     * 🔥 Step 3: 滚动时持续更新当前可见的 Juz Ayat
     */
    private void updateCurrentVisibleJuzAyat() {
        if (!isJuzReadingMode() || quranReadingTracker == null || isListeningMode) {
            return;
        }
        
        if (mBinding == null || mBinding.readerVerses == null) {
            return;
        }
        
        try {
            RecyclerView recyclerView = mBinding.readerVerses;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                
                // 获取中间可见项的位置
                int firstVisiblePos = linearLayoutManager.findFirstVisibleItemPosition();
                int lastVisiblePos = linearLayoutManager.findLastVisibleItemPosition();
                int middlePosition = (firstVisiblePos + lastVisiblePos) / 2;
                
                if (middlePosition >= 0 && middlePosition < recyclerView.getAdapter().getItemCount()) {
                    // 从 Adapter 中获取对应的 Ayat
                    RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                    if (adapter instanceof ADPQuranPages) {
                        ADPQuranPages pagesAdapter = (ADPQuranPages) adapter;
                        if (middlePosition < pagesAdapter.getItemCount()) {
                            com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel pageModel = pagesAdapter.getPageModel(middlePosition);
                            if (pageModel != null && pageModel.getSections() != null && !pageModel.getSections().isEmpty()) {
                                // 获取页面第一个Section
                                com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel firstSection = pageModel.getSections().get(0);
                                int[] fromToVerses = firstSection.getFromToVerses();
                                if (fromToVerses != null && fromToVerses.length >= 2) {
                                    int surah = firstSection.getChapterNo();
                                    int ayah = fromToVerses[0];  // from verse
                                    
                                    // 计算全局 Ayat 编号
                                    int currentGlobalAyat = calculateGlobalAyatNumber(surah, ayah);
                                    
                                    // 如果 Ayat 发生变化，重置计时器
                                    if (currentGlobalAyat != lastCompletedAyatInJuz) {
                                        juzAyatViewStartTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to update current visible Juz Ayat", e);
        }
    }
    
    /**
     * 🔥 Step 3: 检测 Juz Ayat 停留时间（用于Juz Ayat计数）
     */
    private void checkJuzAyatViewDuration() {
        if (!isJuzReadingMode() || quranReadingTracker == null || isListeningMode) {
            return;
        }
        
        if (currentJuzNo <= 0 || currentJuzFirstAyatGlobal <= 0 || currentJuzLastAyatGlobal <= 0) {
            return;
        }
        
        try {
            RecyclerView recyclerView = mBinding.readerVerses;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                
                // 获取中间可见项的位置
                int firstVisiblePos = linearLayoutManager.findFirstVisibleItemPosition();
                int lastVisiblePos = linearLayoutManager.findLastVisibleItemPosition();
                int middlePosition = (firstVisiblePos + lastVisiblePos) / 2;
                
                if (middlePosition >= 0) {
                    RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                    if (adapter instanceof ADPQuranPages) {
                        ADPQuranPages pagesAdapter = (ADPQuranPages) adapter;
                        if (middlePosition < pagesAdapter.getItemCount()) {
                            com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel pageModel = pagesAdapter.getPageModel(middlePosition);
                            if (pageModel != null && pageModel.getSections() != null && !pageModel.getSections().isEmpty()) {
                                // 获取页面第一个Section
                                com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel firstSection = pageModel.getSections().get(0);
                                int[] fromToVerses = firstSection.getFromToVerses();
                                if (fromToVerses != null && fromToVerses.length >= 2) {
                                    int surah = firstSection.getChapterNo();
                                    int ayah = fromToVerses[0];  // from verse
                                    
                                    // 计算全局 Ayat 编号
                                    int currentGlobalAyat = calculateGlobalAyatNumber(surah, ayah);
                                    
                                    // 检查是否在当前 Juz 范围内
                                    if (currentGlobalAyat < currentJuzFirstAyatGlobal || currentGlobalAyat > currentJuzLastAyatGlobal) {
                                        android.util.Log.d("ActivityReader", String.format(
                                            "⚠️ Juz boundary crossed: Current Ayat (Global %d) is outside Juz %d range (%d-%d)",
                                            currentGlobalAyat, currentJuzNo, currentJuzFirstAyatGlobal, currentJuzLastAyatGlobal
                                        ));
                                        // TODO: Handle cross-Juz boundary transition
                                        return;
                                    }
                                    
                                    // 计算停留时间
                                    long viewDuration = System.currentTimeMillis() - juzAyatViewStartTime;
                                    
                                    // 🔥 关键逻辑：只有停留超过3秒 且 currentGlobalAyat > lastCompletedAyatInJuz 时才计数
                                    if (viewDuration >= AYAT_VIEW_THRESHOLD_MS && currentGlobalAyat > lastCompletedAyatInJuz) {
                                        // 计算阅读的 Ayat 数量（从lastCompletedAyatInJuz+1到currentGlobalAyat）
                                        int ayatRead = currentGlobalAyat - lastCompletedAyatInJuz;
                                        
                                        if (ayatRead > 0) {
                                            quranReadingTracker.recordVersesRead(ayatRead);
                                            android.util.Log.d("ActivityReader", String.format(
                                                "🕌 Juz %d Ayat计数：+%d ayat (Surah %d:%d, Global %d)，停留时间：%dms",
                                                currentJuzNo, ayatRead, surah, ayah, currentGlobalAyat, viewDuration
                                            ));
                                            
                                            // 更新 lastCompletedAyatInJuz
                                            lastCompletedAyatInJuz = currentGlobalAyat;
                                            
                                            // 立即检查任务完成状态
                                            quranReadingTracker.checkAndMarkCompleteAsync();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to check Juz Ayat view duration", e);
        }
    }
    
    /**
     * 🔥 判断是否为Page阅读模式（章节滚动或Juz模式）
     */
    private boolean isPageReadingMode() {
        return mReaderParams.readType == ReaderParams.READER_READ_TYPE_CHAPTER ||
               mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ;
    }
    
    /**
     * 🔥 Step 3: 检查是否为 Juz 阅读模式
     */
    private boolean isJuzReadingMode() {
        return mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ;
    }
    
    /**
     * 🔥 Step 3: 计算全局 Ayat 编号（从Surah 1, Ayah 1 开始累加）
     * @param surah 章节号 (1-114)
     * @param ayah 节号
     * @return 全局 Ayat 编号
     */
    private int calculateGlobalAyatNumber(int surah, int ayah) {
        if (mQuranMetaRef == null || mQuranMetaRef.get() == null) {
            return -1;
        }
        
        QuranMeta quranMeta = mQuranMetaRef.get();
        int globalAyat = 0;
        
        // 累加前面所有章节的经文数
        for (int i = 1; i < surah; i++) {
            globalAyat += quranMeta.getChapterVerseCount(i);
        }
        
        // 加上当前章节的节号
        globalAyat += ayah;
        
        return globalAyat;
    }
    
    /**
     * 🔥 新方法：计算实际阅读的经文数量
     */
    private int calculateVersesRead() {
        if (sessionStartSurah <= 0 || sessionEndSurah <= 0) {
            return 0;
        }
        
        try {
            QuranMeta quranMeta = mQuranMetaRef.get();
            if (quranMeta == null) {
                return 0;
            }
            
            int totalVerses = 0;
            
            if (sessionStartSurah == sessionEndSurah) {
                // 同一章节内
                totalVerses = Math.max(0, sessionEndAyah - sessionStartAyah + 1);
            } else {
                // 跨章节（这种情况较少，但需要处理）
                // 起始章节的剩余经文
                int versesInStartSurah = quranMeta.getChapterVerseCount(sessionStartSurah) - sessionStartAyah + 1;
                
                // 中间章节的所有经文
                for (int surah = sessionStartSurah + 1; surah < sessionEndSurah; surah++) {
                    versesInStartSurah += quranMeta.getChapterVerseCount(surah);
                }
                
                // 结束章节的经文
                versesInStartSurah += sessionEndAyah;
                
                totalVerses = versesInStartSurah;
            }
            
            android.util.Log.d("ActivityReader", "📊 计算阅读量: Surah " + sessionStartSurah + ":" + sessionStartAyah + 
                              " → Surah " + sessionEndSurah + ":" + sessionEndAyah + " = " + totalVerses + " verses");
            
            return totalVerses;
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to calculate verses read", e);
            return 0;
        }
    }

    private void maybeTriggerSurahCompletion(int versesRead) {
        if (sessionStartSurah <= 0 || sessionEndSurah <= 0) {
            return;
        }

        try {
            QuranMeta quranMeta = mQuranMetaRef.get();
            if (quranMeta == null) {
                return;
            }

            if (sessionStartSurah != sessionEndSurah) {
                return;
            }

            int surahNo = sessionEndSurah;
            int totalVersesInSurah = quranMeta.getChapterVerseCount(surahNo);
            if (totalVersesInSurah <= 0) {
                return;
            }

            boolean startedFromBeginning = sessionStartAyah <= 1;
            boolean reachedEnd = sessionEndAyah >= totalVersesInSurah;
            boolean readAll = versesRead >= totalVersesInSurah;

            if (startedFromBeginning && reachedEnd && readAll) {
                android.util.Log.d("ActivityReader", "🌟 Full Surah completed: " + surahNo + ", triggering rate prompt");
                RatePromptManager.onSurahCompleted(this);
                // 此处原本会展示插屏广告，已移除。
                //
                // 读完一整章古兰经是本 App 里用户情绪最高、最接近神圣的时刻。
                // 在这一刻插全屏商业广告，在穆斯林用户的感受里不是「烦」，是冒犯，
                // 而且紧跟其后的评分请求会拿到最差的分数。
                // 插屏已改到世俗区场景（答题结算 / Qada 记录完成 / 设置页跳转），
                // 见 AdPolicy 与各场景调用点。
            }
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "Failed to evaluate surah completion", e);
        }
    }

    @Override
    protected void onDestroy() {
        unbindPlayerService();
        mBinding.readerHeader.destroy();
        if (mPlayerService != null) {
            mPlayerService.destroy();
        }

        if (mReadHistoryDBHelper != null) {
            mReadHistoryDBHelper.close();
        }

        RatePromptManager.cancelScheduledPrompt(this);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try{
        outState.putBoolean("preventRecitationPlayerReset", mPlayerService.isPlaying());

        if (mLayoutManager != null) {
            outState.putParcelable("recyclerView", mLayoutManager.onSaveInstanceState());
        }}catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (mLayoutManager != null) {
            mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable("recyclerView"));
        }
    }

    public void bindPlayerService() {
        bindService(new Intent(this, RecitationService.class), mPlayerServiceConnection,
            Context.BIND_AUTO_CREATE);
    }

    public void unbindPlayerService() {
        if (mPlayerService == null) {
            return;
        }

        try {
            unbindService(mPlayerServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!intent.getBooleanExtra(Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, false)) {
            initQuran(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            // This is an app/task exit, so it must never be monetized with an interstitial.
            launchMainActivity();
            finish();

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_reader;
    }

    @Override
    public void adjustStatusAndNavigationBar() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        // For Android 35, adjust the approach to avoid status bar overlap
        if (Build.VERSION.SDK_INT >= 35) {
            // Use solid status bar color for Android 35
            int primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);
            window.setStatusBarColor(primaryColor);
            window.setNavigationBarColor(Color.TRANSPARENT);
            
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(false); // Dark icons for better visibility
            wic.setAppearanceLightNavigationBars(isStatusBarLight());
        } else {
            // Original implementation for older versions
            int uiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiVisibility);

            window.getAttributes().flags &= ~(FLAG_TRANSLUCENT_STATUS | FLAG_TRANSLUCENT_NAVIGATION);

            int clr = Color.TRANSPARENT;
            window.setStatusBarColor(clr);
            window.setNavigationBarColor(clr);

            boolean isLight = isStatusBarLight();
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightNavigationBars(isLight);
            wic.setAppearanceLightStatusBars(isLight);
        }
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        super.preActivityInflate(savedInstanceState);
        if (savedInstanceState != null) {
            mProtectFromPlayerReset = savedInstanceState.getBoolean("preventRecitationPlayerReset", false);
        }

        mReaderParams = new ReaderParams(this);
    }

    @Override
    protected void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityReaderBinding.bind(activityView);
        mNavigator = new Navigator(this);
        initDummyBars();
        
        // 🔥 Daily Quest: 接收听力模式参数
        isListeningMode = intent.getBooleanExtra("LISTENING_MODE", false);
        listeningTargetMinutes = intent.getIntExtra("TARGET_MINUTES", 0);
        autoPlayAudio = intent.getBooleanExtra("AUTO_PLAY_AUDIO", false);  // 保存到成员变量
        startVerseNo = intent.getIntExtra("START_VERSE", -1);  // 🔥 接收起始节号
        
        android.util.Log.d("ActivityReader", "🟢 preReaderReady: Received intent extras:");
        android.util.Log.d("ActivityReader", "🟢   LISTENING_MODE = " + isListeningMode);
        android.util.Log.d("ActivityReader", "🟢   AUTO_PLAY_AUDIO = " + autoPlayAudio);
        android.util.Log.d("ActivityReader", "🟢   START_VERSE = " + startVerseNo);
        android.util.Log.d("ActivityReader", "🟢   TARGET_MINUTES = " + listeningTargetMinutes);
        
        if (isListeningMode) {
            android.util.Log.d("ActivityReader", "🎧 Listening Mode activated: target " + listeningTargetMinutes + " minutes");
        }
        
        // 🔥 Step 3: 接收 Juz 阅读模式参数
        boolean isReadingMode = intent.getBooleanExtra("READING_MODE", false);
        if (isReadingMode) {
            int targetGoal = intent.getIntExtra("TARGET_GOAL", 0);
            String targetUnit = intent.getStringExtra("TARGET_UNIT");
            android.util.Log.d("ActivityReader", "📖 Reading Mode activated: target " + targetGoal + " " + targetUnit);
        }
    }


    @Override
    protected void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        // TEST
        //        intent.putExtras(ReaderFactory.prepareChapterIntent(105));
        //        intent.putExtras(ReaderFactory.prepareSingleVerseIntent(105, 2));
        //        intent.putExtras(ReaderFactory.prepareVerseRangeIntent(2, 3, 21));
        //        intent.putExtras(ReaderFactory.prepareJuzIntent(30));
        // TEST END

        mBinding.getRoot().post(this::init);
    }

    private void init() {
        mBinding.readerHeader.setActivity(this);
        initReadHistory();
        initFloatingFooter();

        final Intent intent = getIntent();
        final String[] requestedTranslSlugs = intent.getStringArrayExtra(READER_KEY_TRANSL_SLUGS);
        if (requestedTranslSlugs == null) {
            mReaderParams.setVisibleTranslSlugs(SPReader.getSavedTranslations(this));
        } else {
            mReaderParams.setVisibleTranslSlugs(new TreeSet<>(Arrays.asList(requestedTranslSlugs)));
        }

        if (!mReaderParams.isPageReaderStyle() && (mReaderParams.getVisibleTranslSlugs() == null || mReaderParams.getVisibleTranslSlugs().isEmpty())) {
            Toast.makeText(this, R.string.strMsgTranslNoneSelected, Toast.LENGTH_SHORT).show();
        }

        mReaderParams.saveTranslChanges = intent.getBooleanExtra(READER_KEY_SAVE_TRANSL_CHANGES, true);
        mReaderParams.setReaderStyle(this,
            intent.getIntExtra(READER_KEY_READER_STYLE, mReaderParams.defaultStyle(this)));

        prepareReader(getIntent());
    }

    private void prepareReader(Intent intent) {
        initReader();
        initQuran(intent);
    }

    private void validateIntent(Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri url = intent.getData();
            if (url == null) return;

            if (url.getHost().equalsIgnoreCase("quran.com")) {
                validateQuranComIntent(intent, url);
            }
        } else if (INTENT_ACTION_OPEN_READER.equalsIgnoreCase(intent.getAction())) {
            validateQuranAppIntent(intent);
        }

        intent.setAction(null);
    }

    private void validateQuranComIntent(Intent intent, Uri url) {
        List<String> pathSegments = url.getPathSegments();
        if (pathSegments.size() >= 2) {
            String firstSeg = pathSegments.get(0);
            String secondSeg = pathSegments.get(1);

            if (firstSeg.equalsIgnoreCase("juz")) {
                int juzNo = Integer.parseInt(secondSeg);
                intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo));
            } else {
                int chapterNo = Integer.parseInt(firstSeg);

                final Pair<Integer, Integer> verseRange;
                final String[] splits = secondSeg.split("-");
                if (splits.length >= 2) {
                    verseRange = new Pair<>(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]));
                } else {
                    int verseNo = Integer.parseInt(splits[0]);
                    verseRange = new Pair<>(verseNo, verseNo);
                }

                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange));
            }
        } else if (pathSegments.size() >= 1) {
            String[] splits = pathSegments.get(0).split(":");
            int chapterNo = Integer.parseInt(splits[0]);
            if (splits.length >= 2) {
                splits = splits[1].split("-");
                final Pair<Integer, Integer> verseRange;
                if (splits.length >= 2) {
                    verseRange = new Pair<>(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]));
                } else {
                    int verseNo = Integer.parseInt(splits[0]);
                    verseRange = new Pair<>(verseNo, verseNo);
                }
                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange));
            } else {
                intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo));
            }
        }

        Set<String> parameters = url.getQueryParameterNames();
        if (parameters.contains("reading")) {
            boolean reading = url.getBooleanQueryParameter("reading", false);
            mReaderParams.setReaderStyle(this, reading ? ReaderParams.READER_STYLE_PAGE : ReaderParams.READER_STYLE_TRANSLATION);
        }
    }

    private void validateQuranAppIntent(Intent intent) {
        final String[] requestedTranslSlugs = intent.getStringArrayExtra("translations");
        if (requestedTranslSlugs != null) {
            mReaderParams.setVisibleTranslSlugs(new TreeSet<>(Arrays.asList(requestedTranslSlugs)));
        }

        if (intent.getBooleanExtra("isJuz", false)) {
            final int juzNo = intent.getIntExtra("juzNo", -1);
            intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo));
        } else {
            final int chapterNo = intent.getIntExtra("chapterNo", -1);
            int[] verses = intent.getIntArrayExtra("verses");
            int verseNo = intent.getIntExtra("verseNo", -1);
            if (verses != null) {
                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verses[0], verses[1]));
            } else if (verseNo != -1) {
                intent.putExtras(ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo));
            } else {
                intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo));
            }
        }
    }

    private void initReader() {
        mLayoutManager = new ReaderLayoutManager(this, RecyclerView.VERTICAL, false);
        mBinding.readerVerses.setItemAnimator(null);
        
        // 🔥 添加滚动监听器以追踪阅读页码和Juz Ayat
        mBinding.readerVerses.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 当滚动停止时更新页码
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateCurrentPageNumber();
                    
                    // 🔥 Daily Quest Step 2: 检测页面停留时间（用于Page计数）
                    checkPageViewDuration();
                    
                    // 🔥 Step 3: 检测 Juz Ayat 停留时间（用于Juz Ayat计数）
                    checkJuzAyatViewDuration();
                }
            }
            
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 🔥 Daily Quest Step 2: 滚动时持续更新当前可见页码
                updateCurrentVisiblePage();
                
                // 🔥 Step 3: 滚动时持续更新当前可见的 Juz Ayat
                updateCurrentVisibleJuzAyat();
            }
        });
    }

    private void resetAdapter(RecyclerView.Adapter<?> adapter) {
        // 🔍 日志 A0：在 resetAdapter 开始时检查 pendingScrollVerse
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 A0: resetAdapter() 开始 | pendingScrollVerse = [" + 
                           mNavigator.pendingScrollVerse[0] + ", " + mNavigator.pendingScrollVerse[1] + "]");
        
        mBinding.readerVerses.setAdapter(adapter);
        mBinding.readerVerses.setLayoutManager(mLayoutManager);
        mBinding.readerVerses.post(this::pendingScrollIfAny);

        saveToIntent();
        
        // 🔍 日志 A：resetAdapter 完成
        int adapterItemCount = adapter != null ? adapter.getItemCount() : 0;
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 A: resetAdapter() 完成 | Adapter ItemCount = " + adapterItemCount + 
                           " | pendingScrollIfAny 已 post() | pendingScrollVerse = [" + 
                           mNavigator.pendingScrollVerse[0] + ", " + mNavigator.pendingScrollVerse[1] + "]");
    }

    private void initDummyBars() {
        adjustStatusAndNavigationBar();

        final View navDummy = mBinding.navigationBarDummy;
        final View statusBarDummy = mBinding.readerHeader.getBinding().statusBarDummy;

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            final int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            statusBarDummy.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, statusBarHeight));

            final int navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            navDummy.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, navHeight));

            return WindowInsetsCompat.CONSUMED;
        });

        final int color = color(R.color.colorBGReaderHeader);
        navDummy.setBackgroundColor(color);
        statusBarDummy.setBackgroundColor(color);
    }

    private void initReadHistory() {
        mReadHistoryDBHelper = new ReadHistoryDBHelper(this);
    }

    private void initFloatingFooter() {
        if (!RecitationUtils.isRecitationSupported()) {
            return;
        }

        mPlayer = new RecitationPlayer(this, mPlayerService);

        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(mPlayer, this);
        }

        mBinding.floatingFooter.addView(mPlayer, 1);
    }

    private void initQuran(Intent intent) {
        try {
            validateIntent(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        QuranMeta quranMeta = mQuranMetaRef.get();

        Quran quran = mQuranRef.get();
        mReaderParams.readType = intent.getIntExtra(READER_KEY_READ_TYPE, mReaderParams.defaultReadType());
        mReaderParams.readerScript = SPReader.getSavedScript(this);
        mReaderParams.resetTextSizesStates();

        int initJuzNo = intent.getIntExtra(READER_KEY_JUZ_NO, 1);
        int initChapterNo = intent.getIntExtra(READER_KEY_CHAPTER_NO, 1);
        Pair<Integer, Integer> initVerses = resolveIntentVerseRange(intent);

        int[] pendingScroll = intent.getIntArrayExtra(READER_KEY_PENDING_SCROLL);
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 X1: initQuran() 读取 READER_KEY_PENDING_SCROLL: " + 
                           (pendingScroll != null ? "[" + pendingScroll[0] + ", " + pendingScroll[1] + "]" : "null"));
        if (pendingScroll != null) {
            mNavigator.pendingScrollVerse = pendingScroll;
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 X2: mNavigator.pendingScrollVerse 已设置为 [" + 
                               pendingScroll[0] + ", " + pendingScroll[1] + "]");
        } else {
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 X2: pendingScroll 为 null，未设置 mNavigator.pendingScrollVerse");
        }

        if (!QuranMeta.isChapterValid(initChapterNo)) {
            makeMessage(str(R.string.strMsgInvalidChapterNo, initChapterNo));

            mReaderParams.readType = mReaderParams.defaultReadType();
            initChapterNo = 1;
            initVerses = null;
        }

        if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_VERSES) {
            boolean anyError = false;
            if (initVerses == null) {
                makeMessage(str(R.string.strMsgInvalidVersesRange));
                anyError = true;
            } else if (QuranUtils.doesRangeDenoteSingle(initVerses) && !quranMeta.isVerseValid4Chapter(initChapterNo,
                initVerses.getFirst())) {
                makeMessage(str(R.string.strMsgInvalidVerseNo, initVerses.getFirst(), initChapterNo));
                anyError = true;
            } else {
                initVerses = QuranUtils.swapVerseRangeIfNeeded(initVerses);

                if (!quranMeta.isVerseRangeValid4Chapter(initChapterNo, initVerses)) {
                    String msg = str(
                        R.string.strMsgInvalidVersesRange2,
                        initVerses.getFirst(),
                        initVerses.getSecond(),
                        initChapterNo
                    );
                    makeMessage(msg);
                    initVerses = QuranUtils.correctVerseInRange(mQuranMetaRef.get(), initChapterNo, initVerses);
                }
            }


            if (anyError) {
                mReaderParams.readType = mReaderParams.defaultReadType();
                initVerses = null;
            }
        } else if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ && !QuranMeta.isJuzValid(initJuzNo)) {
            makeMessage(str(R.string.strMsgInvalidJuzNo, initJuzNo));
            initJuzNo = 1;
        }

        Chapter initialChapter = quran.getChapter(initChapterNo);

        if (initVerses == null) {
            initVerses = new Pair<>(1, initialChapter.getVerseCount());
        }

        switch (mReaderParams.readType) {
            case ReaderParams.READER_READ_TYPE_VERSES: initVerseRange(initialChapter, initVerses);
                break;
            case ReaderParams.READER_READ_TYPE_JUZ: initJuz(initJuzNo);
                break;
            case ReaderParams.READER_READ_TYPE_CHAPTER:
            default: initChapter(initialChapter);
                break;
        }
    }

    private Pair<Integer, Integer> resolveIntentVerseRange(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(READER_KEY_VERSES);

        // The verse range could be passed as a pair or a two items list (as from ShortcutUtils).

        if (serializable instanceof Pair) {
            return (Pair<Integer, Integer>) serializable;
        } else if (serializable instanceof int[]) {
            int[] verses = (int[]) serializable;
            return new Pair<>(verses[0], verses[1]);
        }

        return null;
    }

    private void makeMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void initChapter(Chapter chapter) {
        mReaderParams.readType = ReaderParams.READER_READ_TYPE_CHAPTER;
        mReaderParams.setCurrChapter(chapter);
        mReaderParams.currJuzNo = -1;

        mReaderParams.verseRange = new Pair<>(1, chapter.getVerseCount());
        mBinding.readerHeader.initChapterSelector();
        mBinding.readerHeader.selectChapterIntoSpinner(mNavigator.getCurrChapterNo());
        mBinding.readerHeader.initVerseSelector(null, chapter.getChapterNumber());
        mBinding.readerHeader.setupHeaderForReadType();
        updateVerseNumber(chapter.getChapterNumber(), 1);

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset) {
                mPlayer.onChapterChanged(
                    chapter.getChapterNumber(),
                    1,
                    chapter.getVerseCount(),
                    1,
                    false
                );
            } else {
                mPlayer.reveal();
            }
        }

        mProtectFromPlayerReset = false;

        if (mReaderParams.isPageReaderStyle()) {
            initChapterReading(chapter);
        } else {
            initChapterTranslation(chapter);
        }
        
        // 🔥 Daily Quest: 如果服务已连接但播放器未初始化，立即初始化
        // 这解决了 onServiceConnected 在 initChapter 之前被调用的竞争条件
        if (mPlayerService != null && autoPlayAudio && startVerseNo > 0 && !mPlayerService.isPlaying()) {
            android.util.Log.d("ActivityReader", "🔥 initChapter completed: Service already connected, initializing player now");
            android.util.Log.d("ActivityReader", "🔥 startVerseNo = " + startVerseNo + ", Chapter = " + chapter.getChapterNumber());
            
            // 初始化播放器
            final int targetChapter = chapter.getChapterNumber();
            final int targetVerse = startVerseNo;
            final int savedStartVerse = startVerseNo;
            
            // 重置 startVerseNo 避免重复触发
            startVerseNo = -1;
            
            android.util.Log.d("ActivityReader", "🟡 Calling onChapterChanged (from initChapter):");
            android.util.Log.d("ActivityReader", "🟡   Chapter: " + targetChapter);
            android.util.Log.d("ActivityReader", "🟡   fromVerse: 1");
            android.util.Log.d("ActivityReader", "🟡   toVerse: " + chapter.getVerseCount());
            android.util.Log.d("ActivityReader", "🟡   currentVerse: " + targetVerse);
            
            mPlayerService.onChapterChanged(
                targetChapter,
                1,
                chapter.getVerseCount(),
                targetVerse
            );
            
            // 延迟500ms自动播放
            new Handler().postDelayed(() -> {
                if (mPlayerService != null && mPlayer != null && !mPlayerService.isPlaying()) {
                    android.util.Log.d("ActivityReader", "🎧 Executing auto-play (from initChapter): Surah " + targetChapter + ", Verse " + targetVerse);
                    mPlayerService.reciteVerse(new com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair(targetChapter, targetVerse));
                    mPlayer.reveal();
                    
                    // 重置自动播放标志
                    autoPlayAudio = false;
                }
            }, 500);
        }
    }

    private void initChapterReading(Chapter chapter) {
        mReaderParams.setReaderStyle(this, ReaderParams.READER_STYLE_PAGE);

        makePages(new Pair<>(chapter.getChapterNumber(), chapter.getChapterNumber()), chapter.getPageRange());
    }

    private void initChapterTranslation(Chapter chapter) {
        mReaderParams.setReaderStyle(this, ReaderParams.READER_STYLE_TRANSLATION);

        initTranslationVerses(chapter, 1, chapter.getVerseCount());
    }

    public void initVerseRange(Chapter chapter, Pair<Integer, Integer> verseRange) {
        if (doesVerseRangeEqualWhole(mQuranMetaRef.get(), chapter.getChapterNumber(), verseRange.getFirst(),
            verseRange.getSecond())) {
            initChapter(chapter);
            return;
        }

        // 🔥 修复：单Verse模式防止重复计数
        if (quranReadingTracker != null && !isListeningMode) {
            // 检查是否是单Verse切换（用于每日任务计数）
            boolean isSingleVerseSwitch = QuranUtils.doesRangeDenoteSingle(verseRange);
            if (isSingleVerseSwitch) {
                // 生成当前Verse的唯一标识
                String currentVerseKey = chapter.getChapterNumber() + ":" + verseRange.getFirst();
                
                // 只有当这是一个新的Verse时才记录（防止重复计数）
                if (!currentVerseKey.equals(lastRecordedVerseKey)) {
                    quranReadingTracker.recordVersesRead(1);
                    lastRecordedVerseKey = currentVerseKey;
                    
                    android.util.Log.d("ActivityReader", "📖 单Verse模式：记录新Verse阅读进度 +1 (Surah " + 
                        chapter.getChapterNumber() + ", Verse " + verseRange.getFirst() + ")");
                    
                    // 立即检查任务完成状态
                    quranReadingTracker.checkAndMarkCompleteAsync();
                } else {
                    android.util.Log.d("ActivityReader", "📖 单Verse模式：跳过重复记录 (Surah " + 
                        chapter.getChapterNumber() + ", Verse " + verseRange.getFirst() + ")");
                }
            }
        }

        mReaderParams.readType = ReaderParams.READER_READ_TYPE_VERSES;
        mReaderParams.setReaderStyle(this, ReaderParams.READER_STYLE_TRANSLATION);
        mReaderParams.verseRange = verseRange;

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset && (!chapter.equals(mReaderParams.currChapter))) {
                // 🔥 修复：直接使用 verseRange 的值，不要因为是单节就重置为整个章节
                final int fromVerse = verseRange.getFirst();
                final int toVerse = verseRange.getSecond();

                mPlayer.onChapterChanged(
                    chapter.getChapterNumber(),
                    fromVerse,
                    toVerse,
                    verseRange.getFirst(),
                    false
                );
            } else {
                mPlayer.reveal();
            }
        }

        mProtectFromPlayerReset = false;

        if (!chapter.equals(mReaderParams.currChapter)) {
            mReaderParams.setCurrChapter(chapter);
            mBinding.readerHeader.initVerseSelector(null, chapter.getChapterNumber());
        }

        mBinding.readerHeader.initChapterSelector();
        mBinding.readerHeader.selectChapterIntoSpinner(mNavigator.getCurrChapterNo());
        mBinding.readerHeader.setupHeaderForReadType();
        updateVerseNumber(chapter.getChapterNumber(), verseRange.getFirst());

        initTranslationVerses(chapter, verseRange.getFirst(), verseRange.getSecond());
    }

    private void initTranslationVerses(Chapter chapter, int fromVerse, int toVerse) {
        initTranslationVersesFinal(chapter, fromVerse, toVerse);
    }

    private void initTranslationVersesFinal(Chapter chapter, int fromVerse, int toVerse) {
        mNavigator.setupNavigator();
        if (!mReaderParams.isSingleVerse()) {
            mActionController.showLoader();
        }

        new Thread(() -> {
            mVerseDecorator.refreshQuranTextFonts(
                mVerseDecorator.isKFQPCScript()
                    ? new Pair<>(chapter.getVerse(fromVerse).pageNo, chapter.getVerse(toVerse).pageNo)
                    : null
            );

            initTranslationVersesFinalAsync(chapter, fromVerse, toVerse);
        }).start();
    }

    private void initTranslationVersesFinalAsync(Chapter chapter, int fromVerse, int toVerse) {
        Set<String> slugs = mReaderParams.getVisibleTranslSlugs();
        Map<String, QuranTranslBookInfo> booksInfo = mTranslFactory.getTranslationBooksInfoValidated(slugs);
        ArrayList<ReaderRecyclerItemModel> models = new ArrayList<>();

        final int chapterNo = chapter.getChapterNumber();

        if (mReaderParams.isSingleVerse() && chapter.getVerse(fromVerse).isVOTD(this)) {
            models.add(0, new ReaderRecyclerItemModel().setViewType(ReaderParams.RecyclerItemViewType.IS_VOTD));
        }

        if (slugs == null || slugs.isEmpty()) {
            models.add(new ReaderRecyclerItemModel().setViewType(ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED));
        }

        if (chapter.canShowBismillah() && doesVerseRangeEqualWhole(mQuranMetaRef.get(), chapterNo, fromVerse,
            toVerse)) {
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
            model.setViewType(ReaderParams.RecyclerItemViewType.BISMILLAH);
            models.add(model);
        }

        List<List<Translation>> listOfTranslations = mTranslFactory.getTranslationsVerseRange(slugs, chapterNo,
            fromVerse,
            toVerse);

        for (int verseNo = fromVerse, pos = 0; verseNo <= toVerse; verseNo++, pos++) {
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
            final Verse verse = chapter.getVerse(verseNo);

            List<Translation> translations = listOfTranslations.get(pos);
            verse.setTranslations(translations);

            CharSequence translSpannable = prepareTranslSpannable(verse, translations, booksInfo);
            verse.setTranslTextSpannable(translSpannable);

            models.add(model.setViewType(ReaderParams.RecyclerItemViewType.VERSE).setVerse(verse));
        }

        runOnUiThread(() -> {
            QuranMeta.ChapterMeta chapterInfoMeta = null;
            if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_CHAPTER) {
                chapterInfoMeta = mQuranMetaRef.get().getChapterMeta(chapter.getChapterNumber());
            }
            resetAdapter(new ADPReader(this, chapterInfoMeta, models));
            mActionController.dismissLoader();
        });
    }

    public void initJuz(int juzNo) {
        mReaderParams.setCurrChapter(null);

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset && mReaderParams.currJuzNo != juzNo) {
                mPlayer.onJuzChanged(juzNo, false);
            } else {
                mPlayer.reveal();
                if (mPlayerService != null && mPlayerService.isPlaying()) {
                    mNavigator.pendingScrollVerse = new int[]{
                        mPlayerService.getP().getCurrentChapterNo(),
                        mPlayerService.getP().getCurrentVerseNo()
                    };
                    mNavigator.pendingScrollVerseHighlight = false;
                }
            }
        }

        mProtectFromPlayerReset = false;

        mBinding.readerHeader.initJuzSelector();
        mBinding.readerHeader.selectJuzIntoSpinner(juzNo);
        mBinding.readerHeader.setupHeaderForReadType();
        mNavigator.setupNavigator();

        final QuranMeta quranMeta = mQuranMetaRef.get();
        Pair<Integer, Integer> chaptersInJuz = quranMeta.getChaptersInJuz(juzNo);

        if (mReaderParams.isPageReaderStyle()) {
            initJuzReading(juzNo, quranMeta);
        } else {
            initJuzTranslation(juzNo, chaptersInJuz, quranMeta);
        }

        makeVerseSpinnerJuzItems(juzNo, chaptersInJuz, quranMeta);
        
        // 🔥 Step 3: 初始化 Juz 阅读追踪
        initJuzTracking(juzNo, quranMeta);
    }
    
    /**
     * 🔥 Step 3: 初始化 Juz 阅读追踪
     * 计算当前 Juz 的 Ayat 范围（全局编号），用于追踪阅读进度
     */
    private void initJuzTracking(int juzNo, QuranMeta quranMeta) {
        if (quranReadingTracker == null || !isJuzReadingMode()) {
            return;
        }
        
        currentJuzNo = juzNo;
        
        // 获取 Juz 中的章节范围
        Pair<Integer, Integer> chaptersInJuz = quranMeta.getChaptersInJuz(juzNo);
        int firstChapter = chaptersInJuz.getFirst();
        int lastChapter = chaptersInJuz.getSecond();
        
        // 获取 Juz 中第一个章节的节号范围
        Pair<Integer, Integer> firstChapterVerseRange = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter);
        int firstVerse = firstChapterVerseRange.getFirst();
        
        // 获取 Juz 中最后一个章节的节号范围
        Pair<Integer, Integer> lastChapterVerseRange = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter);
        int lastVerse = lastChapterVerseRange.getSecond();
        
        // 计算全局 Ayat 编号
        currentJuzFirstAyatGlobal = calculateGlobalAyatNumber(firstChapter, firstVerse);
        currentJuzLastAyatGlobal = calculateGlobalAyatNumber(lastChapter, lastVerse);
        
        // 初始化 lastCompletedAyatInJuz（从已读取的进度恢复）
        lastCompletedAyatInJuz = currentJuzFirstAyatGlobal - 1;  // 初始值设为Juz起始前一节
        
        android.util.Log.d("ActivityReader", String.format(
            "🕌 Juz %d tracking initialized: First Ayat (Global) = %d, Last Ayat (Global) = %d, Total Ayat = %d",
            juzNo, currentJuzFirstAyatGlobal, currentJuzLastAyatGlobal, 
            quranMeta.getJuzVerseCount(juzNo)
        ));
    }

    private void initJuzReading(int juzNo, QuranMeta quranMeta) {
        mReaderParams.setReaderStyle(this, ReaderParams.READER_STYLE_PAGE);

        makePages(null, quranMeta.getJuzPageRange(juzNo));
    }

    private void initJuzTranslation(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        mActionController.showLoader();
        new Thread(() -> {
            mVerseDecorator.refreshQuranTextFonts(
                mVerseDecorator.isKFQPCScript() ? mQuranMetaRef.get().getJuzPageRange(juzNo) : null
            );
            initJuzTranslationAsync(juzNo, chaptersInJuz, quranMeta);
        }).start();
    }

    private void initJuzTranslationAsync(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        ArrayList<ReaderRecyclerItemModel> models = new ArrayList<>();

        if (mReaderParams.getVisibleTranslSlugs() == null || mReaderParams.getVisibleTranslSlugs().isEmpty()) {
            models.add(new ReaderRecyclerItemModel().setViewType(ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED));
        }

        IntStream.rangeClosed(chaptersInJuz.getFirst(), chaptersInJuz.getSecond())
            .forEach(chapterNo -> {
                Pair<Integer, Integer> verses = quranMeta.getVerseRangeOfChapterInJuz(juzNo, chapterNo);
                int fromVerse = verses.getFirst();
                int toVerse = verses.getSecond();

                final boolean startOfChapter = mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ && mReaderParams.currJuzNo == juzNo && fromVerse == 1;

                if (startOfChapter) {
                    ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
                    model.setViewType(ReaderParams.RecyclerItemViewType.CHAPTER_TITLE);
                    model.setChapterNo(chapterNo);
                    models.add(model);

                    if (QuranMeta.canShowBismillah(chapterNo)) {
                        ReaderRecyclerItemModel bismillahModel = new ReaderRecyclerItemModel();
                        bismillahModel.setViewType(ReaderParams.RecyclerItemViewType.BISMILLAH);
                        models.add(bismillahModel);
                    }
                }

                makeJuzTranslationVerses(models, mQuranRef.get().getChapter(chapterNo), fromVerse, toVerse);
            });

        runOnUiThread(() -> {
            resetAdapter(new ADPReader(this, null, models));
            mActionController.dismissLoader();
        });
    }

    private void makeJuzTranslationVerses(
        ArrayList<ReaderRecyclerItemModel> models,
        Chapter chapter,
        int fromVerse,
        int toVerse
    ) {
        Set<String> slugs = mReaderParams.getVisibleTranslSlugs();
        Map<String, QuranTranslBookInfo> booksInfo = mTranslFactory.getTranslationBooksInfoValidated(slugs);

        List<List<Translation>> listOfTranslations = mTranslFactory.getTranslationsVerseRange(
            slugs,
            chapter.getChapterNumber(),
            fromVerse,
            toVerse
        );

        for (int verseNo = fromVerse, pos = 0; verseNo <= toVerse; verseNo++, pos++) {
            Verse verse = chapter.getVerse(verseNo);
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();

            List<Translation> translations = listOfTranslations.get(pos);
            verse.setTranslations(translations);

            CharSequence translSpannable = prepareTranslSpannable(verse, translations, booksInfo);
            verse.setTranslTextSpannable(translSpannable);

            models.add(model.setViewType(ReaderParams.RecyclerItemViewType.VERSE).setVerse(verse));
        }
    }

    private void makeVerseSpinnerJuzItems(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        new Thread(() -> {
            List<VerseSpinnerItem> mVerseSpinnerItems = new ArrayList<>();
            String verseNoText = str(R.string.strLabelVerseWithChapNo);

            int firstChapterInJuz = chaptersInJuz.getFirst();
            final AtomicInteger firstVerseInJuz = new AtomicInteger(-1);

            IntStream.rangeClosed(firstChapterInJuz, chaptersInJuz.getSecond())
                .forEach(chapterNo -> {
                    Pair<Integer, Integer> verses = quranMeta.getVerseRangeOfChapterInJuz(juzNo, chapterNo);

                    if (firstVerseInJuz.get() == -1) {
                        firstVerseInJuz.set(verses.getFirst());
                    }

                    IntStream.rangeClosed(verses.getFirst(), verses.getSecond())
                        .forEach(verseNo -> makeVerseSpinnerItemJuz(
                            mVerseSpinnerItems,
                            chapterNo,
                            verseNo,
                            verseNoText
                        ));
                });

            runOnUiThread(() -> {
                VerseSelectorAdapter2 adapter = new VerseSelectorAdapter2(mVerseSpinnerItems);
                mBinding.readerHeader.initVerseSelector(adapter, -1);
                updateVerseNumber(firstChapterInJuz, firstVerseInJuz.get());
            });
        }).start();
    }

    private void makeVerseSpinnerItemJuz(List<VerseSpinnerItem> list, int chapterNo, int verseNo, String verseNoText) {
        VerseSpinnerItem item = new VerseSpinnerItem(chapterNo, verseNo);
        item.setLabel(String.format(verseNoText, chapterNo, verseNo));
        list.add(item);
    }

    private void makePages(
        @Nullable Pair<Integer, Integer> chapters,
        Pair<Integer, Integer> pages
    ) {
        final QuranMeta quranMeta = mQuranMetaRef.get();

        mPagesTaskRunner.cancel();

        mPagesTaskRunner.callAsync(new BaseCallableTask<ArrayList<QuranPageModel>>() {
            @Override
            public void preExecute() {
                mActionController.showLoader();
            }

            @Override
            public ArrayList<QuranPageModel> call() {
                mVerseDecorator.refreshQuranTextFonts(
                    mVerseDecorator.isKFQPCScript() ? pages : null
                );

                return makePagesAsync(chapters, pages, quranMeta);
            }

            @Override
            public void postExecute() {
                mActionController.dismissLoader();
            }

            @Override
            public void onComplete(ArrayList<QuranPageModel> models) {
                mBinding.readerVerses.setLayoutManager(
                    new LinearLayoutManager(ActivityReader.this, RecyclerView.VERTICAL, false));

                QuranMeta.ChapterMeta chapterInfoMeta = null;
                if (chapters != null) {
                    chapterInfoMeta = quranMeta.getChapterMeta(chapters.getFirst());
                }

                resetAdapter(new ADPQuranPages(ActivityReader.this, chapterInfoMeta, models));

                mNavigator.setupNavigator();
            }
        });
    }

    private ArrayList<QuranPageModel> makePagesAsync(
        @Nullable Pair<Integer, Integer> chapterRange,
        Pair<Integer, Integer> pageRange,
        QuranMeta quranMeta
    ) {
        final boolean isJuz = chapterRange == null;
        final Quran quran = mQuranRef.get();

        ArrayList<QuranPageModel> models = new ArrayList<>();

        for (int pageNo = pageRange.getFirst(), l = pageRange.getSecond(); pageNo <= l; pageNo++) {
            QuranPageModel pageModel = createPage(
                isJuz ? quranMeta.getChaptersOnPage(pageNo) : chapterRange,
                pageNo,
                quranMeta,
                quran
            );
            pageModel.setViewType(ReaderParams.RecyclerItemViewType.READER_PAGE);
            models.add(pageModel);
        }

        return models;
    }

    private QuranPageModel createPage(
        Pair<Integer, Integer> chapterRange,
        int pageNo,
        QuranMeta quranMeta,
        Quran quran
    ) {
        ArrayList<QuranPageSectionModel> sections = new ArrayList<>();

        StringBuilder chaptersName = new StringBuilder();
        int firstChapterOnPage = chapterRange.getFirst();

        for (int chapterNo = firstChapterOnPage, toChapterNo = chapterRange.getSecond(); chapterNo <= toChapterNo; chapterNo++) {
            QuranPageSectionModel section = new QuranPageSectionModel();
            ArrayList<Verse> verses = new ArrayList<>();

            final Pair<Integer, Integer> verseRange = quranMeta.getVerseRangeOfChapterOnPage(pageNo, chapterNo);
            final int firstVerse = verseRange.getFirst();

            if (firstVerse == 1) {
                section.setShowTitle(true);
                section.setShowBismillah(QuranMeta.canShowBismillah(chapterNo));
            }

            int txtColor = color(R.color.colorText);
            SpannableStringBuilder verseContentSB = new SpannableStringBuilder();

            final int finalChapterNo = chapterNo;
            IntStream.rangeClosed(firstVerse, verseRange.getSecond())
                .forEach(verseNo -> {
                    Verse verse = quran.getVerse(finalChapterNo, verseNo);
                    verses.add(verse);

                    verseContentSB.append(" ").append(
                        mVerseDecorator.setupArabicTextQuranPage(
                            txtColor,
                            verse,
                            () -> mBinding.verseQuickActions.show(section, verse)
                        )
                    );
                });

            section.setContentSpannable(verseContentSB);
            section.setChapterNo(chapterNo);
            section.setVerses(verses);

            sections.add(section);

            chaptersName.append(chapterNo).append(". ").append(quranMeta.getChapterName(this, chapterNo));
            if (chapterNo < toChapterNo) {
                chaptersName.append(", ");
            }
        }

        return new QuranPageModel(pageNo, quranMeta.getJuzForPage(pageNo), chapterRange, chaptersName.toString(),
            sections);
    }

    public void handleVerseSpinnerSelectedVerseNo(int chapterNo, int verseNo) {
        mNavigator.jumpToVerse(chapterNo, verseNo, true);
    }

    private void pendingScrollIfAny() {
        // 🔍 日志 B：pendingScrollIfAny 开始执行
        RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
        int adapterItemCount = adapter != null ? adapter.getItemCount() : 0;
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B: pendingScrollIfAny() 开始执行 | Adapter ItemCount = " + adapterItemCount);

        int pendingChapterNo = mNavigator.pendingScrollVerse[0];
        int pendingVerseNo = mNavigator.pendingScrollVerse[1];

        boolean proceed = pendingChapterNo > 0 && pendingVerseNo > 0;
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B1: pendingChapterNo=" + pendingChapterNo + ", pendingVerseNo=" + pendingVerseNo + ", proceed初始=" + proceed);

        QuranMeta quranMeta = mQuranMetaRef.get();
        
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B2: readType=" + mReaderParams.readType + 
                           " (JUZ=" + ReaderParams.READER_READ_TYPE_JUZ + 
                           ", CHAPTER=" + ReaderParams.READER_READ_TYPE_CHAPTER + 
                           ", VERSES=" + ReaderParams.READER_READ_TYPE_VERSES + ")");

        if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_JUZ) {
            boolean validJuz = quranMeta.isVerseValid4Juz(mReaderParams.currJuzNo, pendingChapterNo, pendingVerseNo);
            proceed &= validJuz;
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B3-JUZ: validJuz=" + validJuz + ", proceed=" + proceed);
        } else if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_CHAPTER) {
            int currChapterNo = mReaderParams.currChapter != null ? mReaderParams.currChapter.getChapterNumber() : -1;
            boolean chapterMatch = pendingChapterNo == currChapterNo;
            boolean validChapter = quranMeta.isVerseValid4Chapter(pendingChapterNo, pendingVerseNo);
            proceed &= chapterMatch;
            proceed &= validChapter;
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B3-CHAPTER: currChapter=" + currChapterNo + 
                               ", chapterMatch=" + chapterMatch + ", validChapter=" + validChapter + ", proceed=" + proceed);
        } else if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_VERSES) {
            int currChapterNo = mReaderParams.currChapter != null ? mReaderParams.currChapter.getChapterNumber() : -1;
            boolean chapterMatch = pendingChapterNo == currChapterNo;
            boolean verseInRange = QuranUtils.isVerseInRange(pendingVerseNo, mReaderParams.verseRange);
            proceed &= chapterMatch;
            proceed &= verseInRange;
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B3-VERSES: currChapter=" + currChapterNo + 
                               ", chapterMatch=" + chapterMatch + ", verseInRange=" + verseInRange + ", proceed=" + proceed);
        } else {
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B3-UNKNOWN: readType 不匹配任何已知类型，proceed=false");
            proceed = false;
        }

        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 B4: 最终 proceed=" + proceed + " → " + (proceed ? "调用 scrollToVerse()" : "跳过滚动"));

        if (proceed) {
            mNavigator.scrollToVerse(pendingChapterNo, pendingVerseNo, mNavigator.pendingScrollVerseHighlight);
            updateVerseNumber(pendingChapterNo, pendingVerseNo);

            mNavigator.pendingScrollVerse = new int[]{-1, -1};
            mNavigator.pendingScrollVerseHighlight = true;

            persistProgressDialog4PendingTask = false;
            mActionController.dismissLoader();
        } else {
            mNavigator.pendingScrollVerse = new int[]{-1, -1};
        }
    }

    public void updateVerseNumber(int chapterNo, int verseNo) {
        mBinding.readerHeader.selectVerseIntoSpinner(chapterNo, verseNo);
    }

    public void onVerseRecite(int chapterNo, int verseNo, boolean reciting) {
        mActionController.onVerseRecite(chapterNo, verseNo, reciting);
        updateVerseNumber(chapterNo, verseNo);
        
        // 🔥 Daily Quest: Handle listening tracking
        if (isListeningMode && quranListeningTracker != null) {
            if (reciting) {
                // 开始播放时，恢复或开始追踪
                if (quranListeningTracker.getCurrentSessionSeconds() > 0) {
                    quranListeningTracker.resumeListening();
                } else {
                    quranListeningTracker.startListening();
                }
                android.util.Log.d("ActivityReader", "🎧 Listening tracking: " + 
                    (quranListeningTracker.getCurrentSessionSeconds() > 0 ? "resumed" : "started"));
            } else {
                // 暂停播放时，暂停追踪
                quranListeningTracker.pauseListening();
                android.util.Log.d("ActivityReader", "🎧 Listening tracking paused");
            }
        }

        if (mReaderParams.isSingleVerse()) {
            mNavigator.jumpToVerse(chapterNo, verseNo, false);
        }

        if (mPlayerService == null) {
            return;
        }

        final RecyclerView.Adapter<?> adp = mBinding.readerVerses.getAdapter();
        if (adp instanceof ADPReader) {
            onVerseReciteNonPage((ADPReader) adp, chapterNo, verseNo, reciting);
        } else if (adp instanceof ADPQuranPages) {
            onVerseRecitePage((ADPQuranPages) adp, chapterNo, verseNo, reciting);
        }
    }

    private void onVerseReciteNonPage(ADPReader adapter, int chapterNo, int verseNo, boolean reciting) {
        for (int i = 0, l = adapter.getItemCount(); i < l; i++) {
            final ReaderRecyclerItemModel item = adapter.getItem(i);

            if (item == null || item.getViewType() != ReaderParams.RecyclerItemViewType.VERSE) {
                continue;
            }

            adapter.notifyItemChanged(i);

            Verse verse = item.getVerse();
            final boolean isCurrVerse = verse.chapterNo == chapterNo && verse.verseNo == verseNo;
            final boolean bool = reciting && isCurrVerse;
            if (bool && mPlayerService.getP().getSyncWithVerse()) {
                if(mLayoutManager!=null) {
                    mLayoutManager.scrollToPositionWithOffset(i, 0);
                }
            }
        }
    }

    private void onVerseRecitePage(ADPQuranPages adapter, int chapterNo, int verseNo, boolean reciting) {
        if (mPlayerService == null) {
            return;
        }
        outer:
        for (int pos = 0, l = adapter.getItemCount(); pos < l; pos++) {
            QuranPageModel pageModel = adapter.getPageModel(pos);

            if (pageModel == null || pageModel.getViewType() != ReaderParams.RecyclerItemViewType.READER_PAGE) {
                continue;
            }

            if (!pageModel.hasChapter(chapterNo)) {
                continue;
            }

            adapter.notifyItemChanged(pos);

            for (QuranPageSectionModel section : pageModel.getSections()) {
                if (section.getChapterNo() != chapterNo) {
                    continue;
                }

                final boolean isCurrVerse = section.getChapterNo() == chapterNo && section.hasVerse(verseNo);
                final boolean bool = reciting && isCurrVerse;

                if (bool && mPlayerService.getP().getSyncWithVerse()&&mLayoutManager!=null) {
                    mNavigator.scrollToVerseOnPageValidate(pos, verseNo, mLayoutManager.findViewByPosition(pos),
                        section, false);
                    break outer;
                }
            }
        }
    }

    public void onVerseJump(int chapterNo, int verseNo) {
        if (mPlayerService == null || !mReaderParams.isSingleVerse()) {
            return;
        }

        RecitationPlayerParams recParams = mPlayerService.getP();
        if (recParams.getPreviouslyPlaying()) {
            mPlayerService.reciteVerse(new ChapterVersePair(chapterNo, verseNo));
        }
    }

    @Override
    protected void onQuranReParsed(Quran quran) {
        mActionController.showLoader();
        initQuran(getIntent());
        mActionController.dismissLoader();
    }

    private void setupOnSettingsChanged(Intent data) {
        mProtectFromPlayerReset = true;

        boolean arTextSizeChanged = SPReader.getSavedTextSizeMultArabic(this) != mReaderParams.arTextSizeMult;
        boolean translTextSizeChanged = SPReader.getSavedTextSizeMultTransl(this) != mReaderParams.translTextSizeMult;
        boolean readerStyleChanged = mReaderParams.getReaderStyle() != SPReader.getSavedReaderStyle(this);
        boolean scriptChanged = !Objects.equals(SPReader.getSavedScript(this), mReaderParams.readerScript);

        tryReciterChange();

        final Set<String> translSlugsSet;
        if (data.hasExtra(READER_KEY_TRANSL_SLUGS)) {
            String[] translSlugs = data.getStringArrayExtra(READER_KEY_TRANSL_SLUGS);
            if (translSlugs == null) {
                translSlugsSet = new TreeSet<>();
            } else {
                translSlugsSet = new TreeSet<>(Arrays.asList(translSlugs));
            }
        } else {
            translSlugsSet = mReaderParams.getVisibleTranslSlugs();
        }

        boolean translChanged = !Objects.equals(translSlugsSet, mReaderParams.getVisibleTranslSlugs());
        // Reassign translSlugs regardless of translation change.
        mReaderParams.setVisibleTranslSlugs(translSlugsSet);
        // Reassign readerStyle regardless of style change.
        mReaderParams.setReaderStyle(this, SPReader.getSavedReaderStyle(this));

        // Refresh decorator regardless of any change in it.
        mVerseDecorator.refresh();

        if (scriptChanged) {
            reparseQuran();
            return;
        }

        if (readerStyleChanged) {
            onReaderStyleChanged(arTextSizeChanged, translTextSizeChanged);
        } else {
            if (translChanged) {
                onTranslChanged(arTextSizeChanged, translTextSizeChanged);
            } else {
                applySettingsChanges(arTextSizeChanged, translTextSizeChanged, false);
            }
        }
    }

    private void tryReciterChange() {
        if (mPlayerService == null) return;

        RecitationPlayerParams params = mPlayerService.getP();

        final boolean reciterChanged = !Objects.equals(
            SPReader.getSavedRecitationSlug(this),
            params.getCurrentReciter()
        );
        final boolean translationReciterChanged = !Objects.equals(
            SPReader.getSavedRecitationTranslationSlug(this),
            params.getCurrentTranslationReciter()
        );

        final int audioOption = SPReader.getRecitationAudioOption(this);
        final boolean changed;

        if (audioOption == RecitationUtils.AUDIO_OPTION_BOTH) {
            changed = reciterChanged || translationReciterChanged;
        } else if (audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION) {
            changed = translationReciterChanged;
        } else {
            changed = reciterChanged;
        }

        if (changed) {
            mPlayerService.onReciterChanged();
            mPlayerService.onTranslationReciterChanged();
            mPlayerService.restartVerseOnConfigChange();
        }
    }

    private void onReaderStyleChanged(boolean arTextSizeChanged, boolean translTextSizeChanged) {
        mActionController.showLoader();
        initQuran(getIntent());
        applySettingsChanges(arTextSizeChanged, translTextSizeChanged, false);
    }

    private void onTranslChanged(boolean arTextSizeChanged, boolean translTextSizeChanged) {
        mActionController.showLoader();
        applySettingsChanges(arTextSizeChanged, translTextSizeChanged, true);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applySettingsChanges(boolean arTextSizeChanged, boolean translTextSizeChanged, boolean translChanged) {
        final RecyclerView recyclerView = mBinding.readerVerses;

        if (translChanged) {
            mActionController.showLoader();
            if (mPlayerService != null && mPlayerService.isPlaying() && mPlayerService.getP().getSyncWithVerse()) {
                RecitationPlayerParams P = mPlayerService.getP();
                mNavigator.pendingScrollVerse = new int[]{P.getCurrentChapterNo(), P.getCurrentVerseNo()};
            } else {
                final int firstPos = mLayoutManager.findFirstVisibleItemPosition();
                final int lastPos = mLayoutManager.findLastVisibleItemPosition();
                for (int pos = firstPos; pos <= lastPos; pos++) {
                    RecyclerView.ViewHolder vh = mBinding.readerVerses.findViewHolderForAdapterPosition(pos);
                    if (vh != null && vh.itemView instanceof VerseView) {
                        Verse verse = ((VerseView) vh.itemView).getVerse();
                        if (verse != null) {
                            mNavigator.pendingScrollVerse = new int[]{verse.chapterNo, verse.verseNo};
                            break;
                        }
                    }
                }
            }

            mNavigator.pendingScrollVerseHighlight = false;
            initQuran(getIntent());
            recyclerView.post(() -> mActionController.dismissLoader());
        } else if (arTextSizeChanged || translTextSizeChanged) {
            final RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
            if (adapter == null) {
                return;
            }
            adapter.notifyDataSetChanged();
            mReaderParams.resetTextSizesStates();
        }
    }

    private void saveToIntent() {
        final Intent intent = getIntent();
        intent.putExtra(READER_KEY_READ_TYPE, mReaderParams.readType);
        intent.putExtra(READER_KEY_JUZ_NO, mReaderParams.currJuzNo);

        if (mReaderParams.currChapter != null) {
            intent.putExtra(READER_KEY_CHAPTER_NO, mReaderParams.currChapter.getChapterNumber());
        }

        intent.putExtra(READER_KEY_VERSES, mReaderParams.verseRange);
        setIntent(intent);
    }

    private void saveReaderState() {// Get first & last visible item positions (both could be same)

        if(mLayoutManager!=null &&mBinding!=null&&mBinding.readerVerses!=null) {
            int firstPos = mLayoutManager.findFirstVisibleItemPosition();
            int lastPos = mLayoutManager.findLastVisibleItemPosition();

            if (firstPos < 0) {
                return;
            }

            RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
            if (adapter instanceof ADPReader) {
                saveTranslationViewState((ADPReader) adapter, firstPos, lastPos);
            } else if (adapter instanceof ADPQuranPages) {
                savePageViewState((ADPQuranPages) adapter, firstPos, lastPos);
            }
        }
    }

    private void saveTranslationViewState(ADPReader adapter, int firstPos, int lastPos) {
        // If the first item is not a verse item (could be chapterTitle, Bismillah etc), then loop until we get the verse item.
        ReaderRecyclerItemModel firstItem = adapter.getItem(firstPos);
        while (firstItem.getViewType() != ReaderParams.RecyclerItemViewType.VERSE && firstPos <= lastPos && firstPos >= 0) {
            firstItem = adapter.getItem(++firstPos);
        }

        ReaderRecyclerItemModel lastItem = null;
        if (lastPos >= 0) {
            // If the last item is not a verse item (could be chapterTitle, Bismillah, footer etc), then loop until we get the verse item.
            lastItem = adapter.getItem(lastPos);
            while (lastItem.getViewType() != ReaderParams.RecyclerItemViewType.VERSE && lastPos >= firstPos && lastPos >= 0) {
                lastItem = adapter.getItem(--lastPos);
            }
        }

        Verse firstVerse = firstItem.getVerse();
        // If we could not find the first verse item then Verse will be null, so exit.
        if (firstVerse == null) {
            return;
        }

        Verse lastVerse = lastItem == null ? null : lastItem.getVerse();
        // If we could not find the last verse item then Verse will be null OR both verses are not of the same chapter,
        // then use the first verse no as the last.
        final int lastVerseNo;
        if (lastVerse == null || lastVerse.chapterNo != firstVerse.chapterNo) {
            lastVerseNo = firstVerse.verseNo;
        } else {
            lastVerseNo = lastVerse.verseNo;
        }

        if (mReadHistoryDBHelper == null) {
            return;
        }

        // Finally save it.
        VerseUtils.saveLastVerses(
            this,
            mReadHistoryDBHelper,
            mQuranMetaRef.get(),
            mReaderParams.readType,
            ReaderParams.READER_STYLE_TRANSLATION,
            mReaderParams.currJuzNo,
            firstVerse.chapterNo,
            firstVerse.verseNo,
            lastVerseNo
        );
    }

    private void savePageViewState(ADPQuranPages adapter, int firstPos, int lastPos) {
        // If the first item is not a verse item (could be chapterTitle, Bismillah etc), then loop until we get the verse item.
        QuranPageModel item = adapter.getPageModel(firstPos);
        while (item.getViewType() != ReaderParams.RecyclerItemViewType.READER_PAGE && firstPos <= lastPos && firstPos >= 0) {
            item = adapter.getPageModel(++firstPos);
        }

        // Each page have many verses, so we don't need to find the last visible item.

        List<QuranPageSectionModel> sections = item.getSections();
        QuranPageSectionModel firstSection = sections.get(0);

        int[] verses = firstSection.getFromToVerses();


        // Finally save it.
        VerseUtils.saveLastVerses(
            this,
            mReadHistoryDBHelper,
            mQuranMetaRef.get(),
            mReaderParams.readType,
            ReaderParams.READER_STYLE_PAGE,
            mReaderParams.currJuzNo,
            firstSection.getChapterNo(),
            verses[0],
            verses[1]
        );
    }

    @Override
    protected void onActivityResult2(ActivityResult result) {
        super.onActivityResult2(result);

        int resultCode = result.getResultCode();
        Intent data = result.getData();
        if (data == null) {
            return;
        }

        runOnUiThread(() -> {
            if (resultCode == Codes.SETTINGS_LAUNCHER_RESULT_CODE) {
                setupOnSettingsChanged(data);
            } else if (resultCode == Codes.OPEN_REFERENCE_RESULT_CODE) {
                int chapterNo = data.getIntExtra(READER_KEY_CHAPTER_NO, -1);
                if (!QuranMeta.isChapterValid(chapterNo)) {
                    return;
                }
                Pair<Integer, Integer> verses = (Pair<Integer, Integer>) data.getSerializableExtra(READER_KEY_VERSES);
                mActionController.openVerseReference(chapterNo, verses);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        // Check if the user is touching outside the verseQuickActions view, if so, close it.
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = new Rect();
            mBinding.verseQuickActions.getGlobalVisibleRect(rect);
            if (!rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                // Delay some time so that if VerseQuickActionsView is immediately opened again, it doesn't show weird animation.
                mBinding.verseQuickActions.scheduleClose();
            }
        }


        return super.dispatchTouchEvent(ev);
    }
    
    /**
     * 保存当前阅读位置到 Firestore（用于跨设备同步）
     * 🔥 Daily Quest: 确保用户下次启动时能从正确位置继续
     */
    private void saveCurrentPositionToFirestore() {
        try {
            int currentSurah = 1;
            int currentAyah = 1;
            
            // 🔥 关键修复：优先从播放器服务获取当前播放位置（听力模式）
            if (isListeningMode && mPlayerService != null && mPlayerService.getP() != null) {
                currentSurah = mPlayerService.getP().getCurrentChapterNo();
                currentAyah = mPlayerService.getP().getCurrentVerseNo();
                android.util.Log.d("ActivityReader", "🎧 Getting position from PLAYER service: Surah " + currentSurah + ", Ayah " + currentAyah);
            } else {
                // 非听力模式：从 UI (RecyclerView) 获取当前可见位置
                if (mLayoutManager == null || mBinding == null || mBinding.readerVerses == null) {
                    android.util.Log.w("ActivityReader", "⚠️ Cannot save position: LayoutManager or RecyclerView is null");
                    return;
                }
                
                int firstPos = mLayoutManager.findFirstVisibleItemPosition();
                if (firstPos < 0) {
                    android.util.Log.w("ActivityReader", "⚠️ Cannot save position: Invalid first position");
                    return;
                }
                
                RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
                
                // 根据不同的 Adapter 类型获取当前位置
                if (adapter instanceof com.quran.quranaudio.online.quran_module.adapters.ADPReader) {
                    com.quran.quranaudio.online.quran_module.adapters.ADPReader readerAdapter = 
                        (com.quran.quranaudio.online.quran_module.adapters.ADPReader) adapter;
                    com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel firstItem = readerAdapter.getItem(firstPos);
                    if (firstItem != null && firstItem.getVerse() != null) {
                        currentSurah = firstItem.getVerse().chapterNo;
                        currentAyah = firstItem.getVerse().verseNo;
                    }
                } else if (adapter instanceof com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages) {
                    com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages pageAdapter = 
                        (com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages) adapter;
                    com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel pageModel = pageAdapter.getPageModel(firstPos);
                    if (pageModel != null && pageModel.getSections() != null && !pageModel.getSections().isEmpty()) {
                        com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel firstSection = pageModel.getSections().get(0);
                        currentSurah = firstSection.getChapterNo();
                        int[] verses = firstSection.getFromToVerses();
                        if (verses != null && verses.length > 0) {
                            currentAyah = verses[0];
                        }
                    }
                }
                android.util.Log.d("ActivityReader", "📖 Getting position from UI: Surah " + currentSurah + ", Ayah " + currentAyah);
            }
            
            // 保存到本地和 Firestore
            final int surah = currentSurah;
            final int ayah = currentAyah;
            
            // 🔥 Step 1: 始终保存到本地 SharedPreferences（无论是否登录）
            if (!isListeningMode) {
                // 阅读模式：保存到本地
                LastSurahAndAyahHelper.storeLastSurah(this, surah);
                LastSurahAndAyahHelper.storeLastAyah(this, ayah);
                android.util.Log.d("ActivityReader", "💾 Saved to local storage: Surah " + surah + ", Ayah " + ayah);
            }
            
            // 🔥 Step 2: 如果用户已登录，同时保存到 Firestore
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                android.util.Log.w("ActivityReader", "⚠️ User not logged in, saved to local storage only");
                return;  // 已保存到本地，直接返回
            }
            
            String userId = auth.getCurrentUser().getUid();
            com.google.firebase.firestore.FirebaseFirestore firestore = 
                com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            java.util.Map<String, Object> learningState = new java.util.HashMap<>();
            
            // 🔥 关键修复：区分听力模式和阅读模式，使用不同的字段
            if (isListeningMode) {
                // 听力模式：保存到 lastListenSurah 和 lastListenAyah
                learningState.put("lastListenSurah", surah);
                learningState.put("lastListenAyah", ayah);
                learningState.put("lastListenTimestamp", com.google.firebase.Timestamp.now());
                android.util.Log.d("ActivityReader", "🎧 Saving LISTENING position: Surah " + surah + ", Ayah " + ayah);
            } else {
                // 阅读模式：保存到 lastReadSurah 和 lastReadAyah
                learningState.put("lastReadSurah", surah);
                learningState.put("lastReadAyah", ayah);
                learningState.put("lastReadTimestamp", com.google.firebase.Timestamp.now());
                android.util.Log.d("ActivityReader", "📖 Saving READING position: Surah " + surah + ", Ayah " + ayah);
            }
            
            // 🔥 Step 3: 保存当前 Juz 编号（如果在 Juz 阅读模式）
            if (isJuzReadingMode() && mReaderParams != null && mReaderParams.currJuzNo > 0) {
                learningState.put("lastReadJuz", mReaderParams.currJuzNo);
                android.util.Log.d("ActivityReader", "🕌 Also saving Juz " + mReaderParams.currJuzNo + " to Firestore");
            }
            
            // 🔥 Step 4: 保存阅读模式（SURAH/JUZ/VERSES）
            if (mReaderParams != null) {
                String readMode = "";
                android.util.Log.d("ActivityReader", "🔍 DEBUG: mReaderParams.readType = " + mReaderParams.readType + 
                    ", currJuzNo = " + mReaderParams.currJuzNo +
                    ", CHAPTER=" + com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER +
                    ", JUZ=" + com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_JUZ +
                    ", VERSES=" + com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_VERSES);
                    
                if (mReaderParams.readType == com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER) {
                    readMode = "SURAH";
                } else if (mReaderParams.readType == com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_JUZ) {
                    readMode = "JUZ";
                } else if (mReaderParams.readType == com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_VERSES) {
                    readMode = "VERSES";
                }
                if (!readMode.isEmpty()) {
                    learningState.put("lastReadMode", readMode);
                    android.util.Log.d("ActivityReader", "📚 Saving reading mode: " + readMode);
                } else {
                    android.util.Log.w("ActivityReader", "⚠️ WARNING: readMode is empty! readType=" + mReaderParams.readType);
                }
            } else {
                android.util.Log.w("ActivityReader", "⚠️ WARNING: mReaderParams is null!");
            }
            
            firestore.collection("users")
                .document(userId)
                .collection("learningState")
                .document("current")
                .set(learningState, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    String logMsg = "✅ Learning state saved to Firestore: Surah " + surah + ", Ayah " + ayah;
                    if (isJuzReadingMode() && mReaderParams != null && mReaderParams.currJuzNo > 0) {
                        logMsg += ", Juz " + mReaderParams.currJuzNo;
                    }
                    android.util.Log.d("ActivityReader", logMsg);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ActivityReader", "❌ Failed to save learning state to Firestore", e);
                });
                
        } catch (Exception e) {
            android.util.Log.e("ActivityReader", "❌ Exception while saving position to Firestore", e);
        }
    }
}
