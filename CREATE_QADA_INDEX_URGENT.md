# ⚠️ URGENT: 创建 Firestore 索引

## 问题
查询失败：`The query requires an index`

## 解决方案

### 立即创建索引

**方法 1: 点击错误日志中的链接**
```
https://console.firebase.google.com/v1/r/project/quran-majeed-aa3d2/firestore/indexes?create_composite=ClZwcm9qZWN0cy9xdXJhbi1tYWplZWQtYWEzZDIvZGF0YWJhc2VzLyhkZWZhdWx0KS9jb2xsZWN0aW9uR3JvdXBzL3ByYXllcl9sb2dzL2luZGV4ZXMvXxABGgoKBnVzZXJJZBABGggKBGRhdGUQARoMCghfX25hbWVfXxAB
```

**方法 2: 手动创建**
1. 打开 Firebase Console
2. 进入 Firestore Database → Indexes
3. 创建复合索引:
   - Collection: `prayer_logs`
   - Fields:
     - `userId` (Ascending)
     - `date` (Ascending)
     - `__name__` (Ascending)

### 索引配置
```
Collection ID: prayer_logs
Fields indexed:
  - userId (Ascending)
  - date (Ascending)
  - Document ID (Ascending)

Query scope: Collection
```

## 预计时间
索引创建需要 1-2 分钟





