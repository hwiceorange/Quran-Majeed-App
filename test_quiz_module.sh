#!/bin/bash

# Quiz Module Integration Test Script
# 用于验证 Quiz 模块是否正确集成到 FragMain 中

echo "=========================================="
echo "Quiz Module Integration Test"
echo "=========================================="
echo ""

# 检查布局文件
echo "1. 检查布局文件 (frag_main.xml)..."
if grep -q "quiz_entry_view" app/src/main/res/layout/frag_main.xml; then
    echo "   ✅ Quiz entry view 已添加到 frag_main.xml"
else
    echo "   ❌ Quiz entry view 未找到在 frag_main.xml"
fi
echo ""

# 检查 Java 文件
echo "2. 检查 FragMain.java..."

if grep -q "QuizRepository" app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java; then
    echo "   ✅ QuizRepository 导入已添加"
else
    echo "   ❌ QuizRepository 导入未找到"
fi

if grep -q "initializeQuizModule" app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java; then
    echo "   ✅ initializeQuizModule() 方法已添加"
else
    echo "   ❌ initializeQuizModule() 方法未找到"
fi

if grep -q "bindCurrentQuizQuestion" app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java; then
    echo "   ✅ bindCurrentQuizQuestion() 方法已添加"
else
    echo "   ❌ bindCurrentQuizQuestion() 方法未找到"
fi

if grep -q "isQuizSupportedLanguage" app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java; then
    echo "   ✅ isQuizSupportedLanguage() 方法已添加"
else
    echo "   ❌ isQuizSupportedLanguage() 方法未找到"
fi

if grep -q "handleQuizOptionSelected" app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java; then
    echo "   ✅ handleQuizOptionSelected() 方法已添加"
else
    echo "   ❌ handleQuizOptionSelected() 方法未找到"
fi
echo ""

# 检查 Quiz 相关文件是否存在
echo "3. 检查 Quiz 相关文件..."

if [ -f "app/src/main/java/com/quran/quranaudio/online/home/quiz/QuizRepository.kt" ]; then
    echo "   ✅ QuizRepository.kt 存在"
else
    echo "   ❌ QuizRepository.kt 不存在"
fi

if [ -f "app/src/main/java/com/quran/quranaudio/online/home/quiz/QuizQuestion.kt" ]; then
    echo "   ✅ QuizQuestion.kt 存在"
else
    echo "   ❌ QuizQuestion.kt 不存在"
fi

if [ -f "app/src/main/java/com/quran/quranaudio/online/quiz/ui/QuizResultActivity.kt" ]; then
    echo "   ✅ QuizResultActivity.kt 存在"
else
    echo "   ❌ QuizResultActivity.kt 不存在"
fi

if [ -f "app/src/main/res/layout/view_daily_quran_quiz.xml" ]; then
    echo "   ✅ view_daily_quran_quiz.xml 布局文件存在"
else
    echo "   ❌ view_daily_quran_quiz.xml 布局文件不存在"
fi
echo ""

# 检查题目数量
echo "4. 检查可用题目..."
quiz_count=$(grep -c "QuizQuestion(" app/src/main/java/com/quran/quranaudio/online/home/quiz/QuizRepository.kt 2>/dev/null || echo "0")
echo "   📝 当前可用题目数量: $quiz_count"
echo ""

# 总结
echo "=========================================="
echo "测试完成！"
echo ""
echo "下一步："
echo "1. 编译并安装应用到设备"
echo "2. 将系统和应用语言设置为英语"
echo "3. 打开应用主页"
echo "4. 滚动到 Verse of the Day 卡片下方"
echo "5. 应该能看到 Daily Quran Quiz 答题卡片"
echo "=========================================="

