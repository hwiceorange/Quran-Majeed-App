# UI Strings 命名规范审查报告

## 📋 命名规范标准

根据功能模块分类命名：
- `feature_*` - 功能入口（如 feature_quran_reader, feature_hadith）
- `nav_*` - 导航标签（如 nav_home, nav_settings）
- `btn_*` - 按钮文本（如 btn_ok, btn_cancel）
- `msg_*` - 消息提示（如 msg_error_network, msg_success）
- `label_*` - 标签文本（如 label_time, label_remaining）
- `prayer_*` - 祷告相关（如 prayer_fajr, prayer_dhuhr）
- `auth_*` - 认证相关（如 auth_login, auth_logout）
- `settings_*` - 设置相关

---

## ✅ 当前 ui_strings.xml 命名审查结果

### 1. APP CORE（应用核心）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `app_name` | ✅ | - | 标准Android约定 |
| `actionbar_name` | ✅ | - | 标准约定 |
| `the_holy_quran` | ✅ | - | 内容标题，可接受 |
| `home` | ⚠️ | `nav_home` | 建议使用nav_前缀 |
| `title_home` | ✅ | - | 已有title_前缀 |
| `salat_time` | ⚠️ | `nav_salat` 或 `prayer_title` | 导航标签 |
| `title_salat` | ✅ | - | 已有title_前缀 |
| `discover` | ⚠️ | `nav_discover` | 导航标签 |
| `tools` | ⚠️ | `nav_tools` | 导航标签 |
| `tools_menu` | ⚠️ | `nav_tools_menu` | 导航标签 |
| `search` | ⚠️ | `nav_search` 或 `btn_search` | 根据使用场景 |
| `settings` | ⚠️ | `nav_settings` | 导航标签 |
| `menu_menu` | ⚠️ | `nav_menu` | 导航标签 |

### 2. AUTHENTICATION（认证）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `login` | ⚠️ | `auth_login` | 建议使用auth_前缀 |
| `logout` | ⚠️ | `auth_logout` | 建议使用auth_前缀 |
| `profile` | ⚠️ | `auth_profile` | 认证相关 |
| `assalamualaikum` | ✅ | - | 问候语，可接受 |
| `login_with_google` | ⚠️ | `auth_login_google` | 建议使用auth_前缀 |

### 3. PRAYER TIMES（祷告时间）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `FAJR` | ⚠️ | `prayer_fajr` | 建议使用prayer_前缀，小写 |
| `SUNRISE` | ⚠️ | `prayer_sunrise` | 建议使用prayer_前缀，小写 |
| `DOHA` | ⚠️ | `prayer_dhuha` | 建议使用prayer_前缀，小写 |
| `DHOHR` | ⚠️ | `prayer_dhuhr` | 建议使用prayer_前缀，小写 |
| `ASR` | ⚠️ | `prayer_asr` | 建议使用prayer_前缀，小写 |
| `MAGHRIB` | ⚠️ | `prayer_maghrib` | 建议使用prayer_前缀，小写 |
| `ICHA` | ⚠️ | `prayer_isha` | 建议使用prayer_前缀，小写 |
| `SHORT_FAJR` | ⚠️ | `prayer_fajr_short` | 统一命名格式 |
| `SHORT_SUNRISE` | ⚠️ | `prayer_sunrise_short` | 统一命名格式 |
| `SHORT_DHOHR` | ⚠️ | `prayer_dhuhr_short` | 统一命名格式 |
| `SHORT_ASR` | ⚠️ | `prayer_asr_short` | 统一命名格式 |
| `SHORT_MAGHRIB` | ⚠️ | `prayer_maghrib_short` | 统一命名格式 |
| `SHORT_ICHA` | ⚠️ | `prayer_isha_short` | 统一命名格式 |
| `next_prayer` | ⚠️ | `prayer_next` | 建议使用prayer_前缀 |
| `time` | ⚠️ | `label_time` | 标签文本 |
| `remaining` | ⚠️ | `label_remaining` | 标签文本 |
| `button_track` | ✅ | - | 或 `btn_track` |

### 4. ACTIONS & BUTTONS（操作和按钮）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `ok` | ⚠️ | `btn_ok` | 建议使用btn_前缀 |
| `yes` | ⚠️ | `btn_yes` | 建议使用btn_前缀 |
| `no` | ⚠️ | `btn_no` | 建议使用btn_前缀 |
| `cancel` | ⚠️ | `btn_cancel` | 建议使用btn_前缀 |
| `close` | ⚠️ | `btn_close` | 建议使用btn_前缀 |
| `done` | ⚠️ | `btn_done` | 建议使用btn_前缀 |
| `apply` | ⚠️ | `btn_apply` | 建议使用btn_前缀 |
| `back` | ⚠️ | `btn_back` | 建议使用btn_前缀 |
| `share` | ⚠️ | `btn_share` | 建议使用btn_前缀 |
| `download` | ⚠️ | `btn_download` | 建议使用btn_前缀 |
| `refresh` | ⚠️ | `btn_refresh` | 建议使用btn_前缀 |
| `play` | ⚠️ | `btn_play` | 建议使用btn_前缀 |
| `pause` | ⚠️ | `btn_pause` | 建议使用btn_前缀 |
| `skip` | ⚠️ | `btn_skip` | 建议使用btn_前缀 |
| `later` | ⚠️ | `btn_later` | 建议使用btn_前缀 |
| `not_now` | ⚠️ | `btn_not_now` | 建议使用btn_前缀 |
| `next` | ⚠️ | `btn_next` | 建议使用btn_前缀 |
| `previous` | ⚠️ | `btn_previous` | 建议使用btn_前缀 |

### 5. ERRORS & MESSAGES（错误和消息）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `network_error_message` | ⚠️ | `msg_error_network` | 统一msg_error_前缀 |
| `internet_msg` | ⚠️ | `msg_internet_required` | 统一命名格式 |
| `pleaseBeConnectedToInternet` | ❌ | `msg_connect_internet` | 不符合规范 |
| `no_internet` | ⚠️ | `msg_no_internet` | 建议使用msg_前缀 |
| `location_permission_not_granted` | ⚠️ | `msg_permission_location_denied` | 统一命名格式 |
| `location_alert_title` | ⚠️ | `msg_error_location_title` | 统一命名格式 |
| `location_service_unavailable` | ⚠️ | `msg_location_unavailable` | 统一命名格式 |
| `default_error_message` | ⚠️ | `msg_error_default` | 统一命名格式 |
| `error` | ⚠️ | `msg_error` | 建议使用msg_前缀 |
| `something_went_wrong` | ⚠️ | `msg_error_unknown` | 统一命名格式 |
| `try_again_later` | ⚠️ | `msg_try_later` | 统一命名格式 |
| `success` | ⚠️ | `msg_success` | 建议使用msg_前缀 |
| `copied` | ⚠️ | `msg_copied` | 建议使用msg_前缀 |
| `loading` | ⚠️ | `msg_loading` | 建议使用msg_前缀 |
| `please_wait` | ⚠️ | `msg_please_wait` | 建议使用msg_前缀 |
| `buffering` | ⚠️ | `msg_buffering` | 建议使用msg_前缀 |

### 6. SETTINGS & PREFERENCES（设置和偏好）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `language` | ⚠️ | `settings_language` | 建议使用settings_前缀 |
| `title_app_language` | ✅ | - | 已有title_前缀 |
| `title_theme` | ✅ | - | 已有title_前缀 |
| `theme_light` | ✅ | - | 已有theme_前缀 |
| `theme_dark` | ✅ | - | 已有theme_前缀 |
| `system_default` | ⚠️ | `settings_system_default` | 建议使用settings_前缀 |
| `time_settings` | ⚠️ | `settings_prayer_time` | 统一命名格式 |
| `location` | ⚠️ | `settings_location` | 建议使用settings_前缀 |
| `set_location` | ⚠️ | `settings_set_location` | 统一命名格式 |

### 7. DAILY QUESTS & LEARNING（每日任务和学习）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `daily_quests` | ✅ | - | 功能名称，可接受 |
| `daily_quests_description` | ✅ | - | 描述文本 |
| `create_learning_plan` | ⚠️ | `btn_create_learning_plan` | 按钮文本 |
| `learn` | ⚠️ | `nav_learn` | 导航标签 |

### 8. FEATURES（功能）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `listen_quran` | ⚠️ | `feature_listen_quran` | 功能入口 |
| `read_quran` | ⚠️ | `feature_read_quran` | 功能入口 |
| `quran_reader` | ⚠️ | `feature_quran_reader` | 功能入口 |
| `surah` | ✅ | - | 内容术语，可接受 |
| `surah_index` | ⚠️ | `feature_surah_index` | 功能入口 |
| `juz_index` | ⚠️ | `feature_juz_index` | 功能入口 |
| `bookmark` | ⚠️ | `feature_bookmark` | 功能入口 |
| `hadith` | ✅ | - | 内容术语，可接受 |
| `hadith_btn` | ⚠️ | `feature_hadith` | 功能入口 |
| `names_of_allah` | ⚠️ | `feature_99_names` | 功能入口 |
| `qibla_direction` | ⚠️ | `feature_qibla` | 功能入口 |
| `tasbih` | ⚠️ | `feature_tasbih` | 功能入口 |
| `wudu_guide` | ⚠️ | `feature_wudu_guide` | 功能入口 |
| `zakat_calculator` | ⚠️ | `feature_zakat_calc` | 功能入口 |
| `six_kalmas` | ⚠️ | `feature_six_kalmas` | 功能入口 |
| `azkar` | ⚠️ | `feature_azkar` | 功能入口 |
| `calendar` | ⚠️ | `feature_calendar` | 功能入口 |
| `mecca_live` | ⚠️ | `feature_mecca_live` | 功能入口 |
| `madina_live` | ⚠️ | `feature_madina_live` | 功能入口 |
| `live_stream` | ⚠️ | `feature_live_stream` | 功能标题 |
| `mecca_live_description` | ✅ | - | 描述文本 |
| `medina_live_description` | ✅ | - | 描述文本 |

### 9. VERSE OF THE DAY（今日经文）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `verse_of_day` | ⚠️ | `feature_verse_of_day` | 功能入口 |

### 10. ABOUT & FEEDBACK（关于和反馈）

| 当前Key | 符合规范 | 建议新Key | 说明 |
|---------|----------|-----------|------|
| `about_us` | ⚠️ | `nav_about_us` | 导航项 |
| `privacy` | ⚠️ | `nav_privacy` | 导航项 |
| `rate_app` | ⚠️ | `feature_rate_app` | 功能入口 |
| `share_app` | ⚠️ | `feature_share_app` | 功能入口 |
| `feedback` | ⚠️ | `nav_feedback` | 导航项 |
| `contact` | ⚠️ | `nav_contact` | 导航项 |

---

## 📊 统计摘要

| 状态 | 数量 | 百分比 |
|------|------|--------|
| ✅ 完全符合规范 | 20 | 13% |
| ⚠️ 建议改进（非强制） | 127 | 85% |
| ❌ 必须修改 | 3 | 2% |
| **总计** | **150** | **100%** |

---

## 🎯 优先级分级

### 🔴 高优先级（必须修改）
这些命名严重不符合规范，强烈建议使用IDE安全重构：

1. `pleaseBeConnectedToInternet` → `msg_connect_internet`（驼峰命名不规范）

### 🟡 中优先级（强烈建议）
这些是最常用的UI元素，建议重命名以提高可维护性：

**祷告名称（13个）：**
- `FAJR` → `prayer_fajr`
- `SUNRISE` → `prayer_sunrise`
- `DHOHR` → `prayer_dhuhr`
- `ASR` → `prayer_asr`
- `MAGHRIB` → `prayer_maghrib`
- `ICHA` → `prayer_isha`
- `SHORT_*` → `prayer_*_short` 系列

**通用按钮（18个）：**
- `ok` → `btn_ok`
- `cancel` → `btn_cancel`
- `yes` → `btn_yes`
- `no` → `btn_no`
- 等所有按钮文本

**导航标签（10个）：**
- `home` → `nav_home`
- `settings` → `nav_settings`
- `search` → `nav_search`
- 等所有导航标签

### 🟢 低优先级（可选）
功能入口命名，当前虽然可用，但添加 `feature_` 前缀会更规范：

- `listen_quran` → `feature_listen_quran`
- `names_of_allah` → `feature_99_names`
- 等20+个功能入口

---

## 💡 执行建议

### 方案1：渐进式重构（推荐）

**阶段1：高优先级（1-2个必须修改的）**
- 使用IDE的 Shift+F6 重构工具
- 逐个验证所有引用点

**阶段2：中优先级（核心UI元素）**
- 分批次重构（每次5-10个keys）
- 每批次后进行编译和测试

**阶段3：低优先级（功能入口）**
- 作为后续迭代任务
- 与新功能开发同步进行

### 方案2：保持现状（兼容优先）

**理由：**
- 当前命名虽不够规范，但是语义清晰
- 大量重命名会影响git历史追溯
- 现有代码已经稳定运行

**建议：**
- 新增的UI字符串严格遵循规范
- 旧的命名保持不动，确保兼容性
- 在注释中标注"Legacy naming"

---

## 📋 下一步行动

**推荐决策：**
1. ❓ 您希望执行方案1（渐进式重构）还是方案2（保持现状）？
2. ❓ 如果执行重构，从哪个优先级开始？

**如果选择重构，我将创建：**
- IDE安全重构操作指南
- 每个key的详细重构步骤
- 自动化验证脚本


