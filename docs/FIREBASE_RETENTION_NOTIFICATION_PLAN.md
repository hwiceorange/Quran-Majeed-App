# Firebase 留存通知投放方案（v1.10.3）

## 投放原则

- 只触达已经授予通知权限的用户；尊重系统通知关闭状态。
- 不在礼拜进行中的敏感时段推销订阅，不使用焦虑、羞辱或虚假紧迫文案。
- 留存内容每位用户最多每周 2 条、同日最多 1 条；邦克/用户主动设置的提醒不计入运营通知。
- 首次安装后 24 小时内不发运营通知。优先按用户本地时区发送。
- 用 Firebase A/B Testing 先做 10% 对照组 + 两个 10% 实验组，至少观察 7 天，再逐步放量。
- 每条消息必须带 `campaign_id` 和 `target`，用于 `rf_push` 的收到、展示、点击漏斗。

## 推荐活动

| campaign_id | 人群 | 本地时间 | target | 标题 | 正文 | TTL |
|---|---|---:|---|---|---|---:|
| quran_gentle_d1 | 安装满 24h、未在次日阅读 | 19:30 | quran | Continue with the Quran | A few peaceful verses are waiting. Continue from where you feel comfortable. | 12h |
| quran_continue_d3 | 3 天内读过但超过 36h 未回访 | 19:30 | quran | Your Quran reading is here | Return for a quiet moment of reflection— even a few verses matter. | 12h |
| friday_kahf | 非订阅与订阅用户；所在时区周五 | 09:00 | quran | A blessed Friday reminder | Make space for Surah Al-Kahf today and begin at your own pace. | 10h |
| dhikr_evening | 使用过 Tasbih、超过 48h 未使用 | 日落后约 45 分钟 | tasbih | A quiet moment for dhikr | Take a short pause for remembrance and continue your dhikr. | 4h |

订阅促销不建议作为首轮留存 Push。先验证内容型通知能带来真实阅读回访，再针对“看过订阅页但未购买、且 72 小时未回访”的用户做低频价值提醒，禁止直接发送未经 Play 资格校验的折扣价格。

## 多语言文案

### `quran_gentle_d1`

| 语言 | 标题 | 正文 |
|---|---|---|
| en | Continue with the Quran | A few peaceful verses are waiting. Continue from where you feel comfortable. |
| ar | واصل رحلتك مع القرآن | تنتظرك آيات تبعث السكينة. واصل القراءة بالقدر الذي يناسبك. |
| ur | قرآن کے ساتھ اپنا سفر جاری رکھیں | چند پُرسکون آیات آپ کی منتظر ہیں۔ جہاں آسان لگے وہاں سے جاری رکھیں۔ |
| id | Lanjutkan bersama Al-Qur’an | Beberapa ayat yang menenangkan menanti. Lanjutkan sesuai kenyamanan Anda. |
| ms | Teruskan bersama al-Quran | Beberapa ayat yang menenangkan menanti. Teruskan mengikut keselesaan anda. |
| tr | Kur’an ile yolculuğunuza devam edin | Huzur veren birkaç ayet sizi bekliyor. Size uygun yerden devam edin. |
| bn | কুরআনের সঙ্গে পথচলা চালিয়ে যান | প্রশান্তির কয়েকটি আয়াত অপেক্ষায় আছে। স্বাচ্ছন্দ্যমতো পড়া চালিয়ে যান। |

### `friday_kahf`

| 语言 | 标题 | 正文 |
|---|---|---|
| en | A blessed Friday reminder | Make space for Surah Al-Kahf today and begin at your own pace. |
| ar | تذكير مبارك ليوم الجمعة | خصص وقتًا اليوم لسورة الكهف وابدأ بالقدر الذي يناسبك. |
| ur | جمعہ کی بابرکت یاد دہانی | آج سورۃ الکہف کے لیے وقت نکالیں اور اپنی سہولت سے آغاز کریں۔ |
| id | Pengingat Jumat penuh berkah | Luangkan waktu untuk Surah Al-Kahfi hari ini dan mulai sesuai kemampuan. |
| ms | Peringatan Jumaat yang diberkati | Luangkan masa untuk Surah Al-Kahfi hari ini dan mulakan mengikut kemampuan. |
| tr | Bereketli cuma hatırlatması | Bugün Kehf Suresi’ne zaman ayırın ve kendi temponuzda başlayın. |
| bn | বরকতময় জুমার স্মরণিকা | আজ সূরা আল-কাহফের জন্য সময় রাখুন এবং নিজের গতিতে শুরু করুন। |

## Firebase 配置

1. 受众属性：注册 `rf_app_lang`、`rf_install_age`、`rf_subscribed`、`rf_notif_perm`；事件参数注册 `stage`、`campaign`、`target`、`source`。
2. 事件：以 `rf_push` 为唯一通知漏斗事件，按 `stage=received/displayed/blocked/opened` 分析。
3. Android 目标：最低版本 `1.10.3 (136)`，只选择 `rf_notif_perm=granted`。
4. 使用本地时区调度；启用频次限制，每周最多 2 条运营通知。
5. 通知数据必须包含：`title`、`body`、`campaign_id`、`target`。合法 target：`quran`、`prayer`、`tasbih`、`subscription`。
6. HTTP v1 / Admin SDK 数据消息设置 Android priority `HIGH`，内容提醒 TTL 4–12 小时，并使用稳定的 collapse key，防止离线后集中补发过期通知。
7. `fcm_options.analytics_label` 与 `campaign_id` 使用同一稳定名称，便于 Firebase Delivery 报告和应用内漏斗对齐。

示例 payload 位于 `firebase/retention-notifications/quran_gentle_d1.json`。发送前替换项目、目标 token/condition 和本地化文案；不要提交服务账号密钥。

## 上线验收指标

- 技术：`displayed / received`、`opened / displayed`、`blocked / received`。
- 留存：通知点击用户与 10% 无通知对照组的 D1/D7 阅读回访率。
- 价值：点击后 10 分钟内 `rf_value(type=quran_index)`、章节打开/阅读时长。
- 负向：通知权限关闭率、卸载率、崩溃率；任一显著恶化立即停止实验。
- 订阅：Push 只作为 source，不把点击当购买；用 `rf_subscription` 的 `page_open -> checkout_start -> purchase_result(success)` 计算。
