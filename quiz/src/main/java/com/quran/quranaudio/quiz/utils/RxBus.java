package com.quran.quranaudio.quiz.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import io.reactivex.functions.Consumer;
import kotlin.jvm.functions.Function0;

public class RxBus {
    private final ConcurrentHashMap<String, Set<Consumer>>
            maps = new ConcurrentHashMap<>();
    private static final RxBus instance;

    static {
        instance = new RxBus();
    }

    public static RxBus INSTANCE() {
        return instance;
    }

    public void post(Object o) {
        Tasks.INSTANCE.postByUI(() -> {
            String key = o.getClass().getName();
            Set<Consumer> observerSet = maps.get(key);
            if (observerSet != null && !observerSet.isEmpty()) {
                Iterator<Consumer> iterator = observerSet.iterator();
                while (iterator.hasNext()) {
                    Consumer currConsumer = iterator.next();
                    try {
                        if (currConsumer != null) {
                            currConsumer.accept(o);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void post(Object o, Function0 doneCallback) {
        Tasks.INSTANCE.postByUI(() -> {
            try {
                String key = o.getClass().getName();
                Set<Consumer> observerSet = maps.get(key);
                if (observerSet != null && !observerSet.isEmpty()) {
                    Iterator<Consumer> iterator = observerSet.iterator();
                    while (iterator.hasNext()) {
                        Consumer currConsumer = iterator.next();
                        try {
                            if (currConsumer != null) {
                                currConsumer.accept(o);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneCallback.invoke();
            }
        });
    }

    private <T> void doSubscribe(LifecycleOwner owner, Class<T> type, Consumer<? super T> consumer) {
        String key = type.getName();
        if (maps.get(key) != null) {
            maps.get(key).add(consumer);
        } else {
            Set<Consumer> ObserverMap = Collections.synchronizedSet(new LinkedHashSet<>());
            ObserverMap.add(consumer);
            maps.put(key, ObserverMap);
        }
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                doUnSubscribe(type, consumer);
            }
        });
    }


    public <T> void register(LifecycleOwner owner, Class<T> tClass, Consumer<T> action) {
        doSubscribe(owner, tClass, action);
    }

    private <T> void doUnSubscribe(Class<T> type, Consumer<? super T> consumer) {
        String key = type.getName();
        Set<Consumer> observerSet = maps.get(key);
        if (observerSet != null && !observerSet.isEmpty()) {
            Iterator<Consumer> iterator = observerSet.iterator();
            while (iterator.hasNext()) {
                Consumer currConsumer = iterator.next();
                if (consumer == currConsumer) {
                    iterator.remove();
                }
            }
        }
        if (observerSet == null || observerSet.isEmpty()) {
            maps.remove(key);
        }
    }
}
