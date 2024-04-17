package com.quran.quranaudio.quiz.base;

public class MainTabChangeEvent {
    public static String TO_QUIZ = "to_quiz";
    public String toType;

    public MainTabChangeEvent(String type) {
        this.toType = type;
    }
}
