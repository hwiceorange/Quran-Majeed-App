# 严重Bug修复：祷告名称本地化导致数据不一致

## 🔴 **Bug描述**

用户切换语言后，Qada Tracker显示的数据完全不同：
- 英语环境：98%完成率
- 印尼语环境：20%完成率

**症状**：相同账户，相同时区，仅切换语言，祷告记录就完全不一致。

---

## 🐛 **根本原因**

### **问题代码（QadaTrackerActivity.java，第445-453行）**

```java
private String[] getPrayerNames() {
    return new String[] {
        getString(R.string.prayer_fajr),  // ❌ 英语: "Fajr", 印尼语: "Subuh"
        getString(R.string.prayer_dhuhr), // ❌ 英语: "Dhuhr", 印尼语: "Dzuhur"
        getString(R.string.prayer_asr),
        getString(R.string.prayer_maghrib),
        getString(R.string.prayer_isha)
    };
}
```

### **Bug触发流程**

1. **英语环境下记录祷告**：
   - Firestore保存：`prayerName = "Fajr"`
   - 查询成功：`whereEqualTo("prayerName", "Fajr")` ✅

2. **切换到印尼语**：
   - `getPrayerNames()` 返回：`["Subuh", "Dzuhur", ...]`
   - 查询失败：`whereEqualTo("prayerName", "Subuh")` ❌ 找不到"Fajr"记录！
   - 结果：所有英语环境的祷告数据"消失"

3. **在印尼语环境新建记录**：
   - Firestore保存：`prayerName = "Subuh"`
   - 切换回英语：查询"Fajr"，找不到"Subuh"记录！

### **影响范围**

- ❌ `QadaTrackerActivity` - 周/月Tab数据查询
- ❌ `PrayersFragment` - Salat页面今日祷告记录
- ❌ `PrayerLogBottomSheet` - 祷告记录保存
- ❌ **所有使用本地化祷告名称进行Firestore查询的地方**

---

## ✅ **解决方案**

### **1. 创建固定的英语祷告名称常量**

**新文件：`PrayerName.kt`**

```kotlin
object PrayerName {
    // ✅ 固定的英语祷告名称（用于数据库键）
    const val FAJR = "Fajr"
    const val DHUHR = "Dhuhr"
    const val ASR = "Asr"
    const val MAGHRIB = "Maghrib"
    const val ISHA = "Isha"
    
    val ALL_PRAYERS = arrayOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    
    // ✅ 获取本地化名称（仅用于UI显示）
    fun getLocalizedName(englishName: String, context: Context): String
    
    // ✅ 获取所有本地化名称
    fun getAllLocalizedNames(context: Context): Array<String>
    
    // ✅ 将本地化名称转换回英语（向后兼容）
    fun toEnglishName(localizedName: String, context: Context): String
}
```

### **2. 修复QadaTrackerActivity**

**修复前**：
```java
private String[] getPrayerNames() {
    return new String[] {
        getString(R.string.prayer_fajr),  // ❌ 本地化名称
        // ...
    };
}

private LinearLayout createWeeklyPrayerRow(String prayerName) {
    nameView.setText(prayerName);  // ❌ 显示和查询使用同一个值
    // ...
}
```

**修复后**：
```java
// ✅ 用于数据库查询的英语名称
private String[] getPrayerNames() {
    return PrayerName.ALL_PRAYERS;
}

// ✅ 用于UI显示的本地化名称
private String[] getLocalizedPrayerNames() {
    return PrayerName.getAllLocalizedNames(this);
}

// ✅ 分离查询和显示
private LinearLayout createWeeklyPrayerRow(String prayerName, String localizedName) {
    nameView.setText(localizedName);  // 显示本地化名称
    // 但使用 prayerName (英语) 查询数据
}
```

### **3. 数据迁移策略**

由于现有数据库中已经存在本地化的祷告名称，需要进行数据迁移：

**方案A：自动修复（推荐）**
- 在`PrayerLogRepository`中添加`toEnglishName()`转换
- 查询时自动将本地化名称转换为英语
- 逐步迁移旧数据

**方案B：批量迁移脚本**
- 遍历所有用户的`prayer_logs`
- 将`prayerName`字段统一转换为英语
- 一次性修复所有数据

---

## 🔧 **修复步骤**

### **已完成**：
1. ✅ 创建`PrayerName.kt`工具类
2. ✅ 修复`QadaTrackerActivity.getPrayerNames()`
3. ✅ 添加`getLocalizedPrayerNames()`方法
4. ✅ 修复`createWeeklyPrayerRow()`使用本地化显示

### **待完成**：
1. ⏳ 修复`createMonthlyDataRow()`使用英语名称查询
2. ⏳ 修复`PrayersFragment`使用固定祷告名称
3. ⏳ 修复`PrayerLogBottomSheet`保存前转换为英语
4. ⏳ 添加向后兼容逻辑，支持旧的本地化数据
5. ⏳ 全面测试切换语言后的数据一致性

---

## 📊 **测试验证**

### **测试场景**：
1. 英语环境下记录5天的祷告
2. 切换到印尼语，验证所有祷告记录仍可见
3. 在印尼语环境下添加新祷告记录
4. 切换回英语，验证所有记录（包括印尼语环境新增的）仍可见
5. 验证Qada完成率计算正确

### **预期结果**：
- ✅ 切换语言后，所有祷告记录保持一致
- ✅ 完成率计算不受语言切换影响
- ✅ UI显示使用正确的本地化祷告名称
- ✅ 数据库查询始终使用英语祷告名称

---

## 🚨 **重要性**

**严重级别**：🔴 **CRITICAL**

**影响**：
- 用户数据不一致
- 用户信任度下降
- 可能导致用户流失

**优先级**：**P0 - 立即修复**

---

## 📝 **技术债务**

未来需要考虑：
1. 统一所有枚举类型的本地化策略
2. 创建数据库字段命名规范
3. 添加数据迁移工具类
4. 完善多语言测试用例

---

## 修复日期

2024-11-16

## 修复人员

AI Assistant (Claude)

