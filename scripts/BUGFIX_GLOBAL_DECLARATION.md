# Python 语法错误修复

## ❌ 原问题

```python
SyntaxError: name 'OUTPUT_DIR' is used prior to global declaration
```

---

## 🐛 错误原因

在 Python 中，`global` 声明必须在使用变量**之前**。

### 错误代码（修复前）

```python
def main():
    parser.add_argument(
        "--output",
        default=OUTPUT_DIR,  # ❌ 在 global 声明前使用
        ...
    )
    
    global OUTPUT_DIR  # ❌ 声明太晚了！
    OUTPUT_DIR = args.output
```

---

## ✅ 修复方案

将 `global` 声明移到函数开始处。

### 正确代码（修复后）

```python
def main():
    global OUTPUT_DIR  # ✅ 在使用前声明
    
    parser.add_argument(
        "--output",
        default=OUTPUT_DIR,  # ✅ 现在可以使用了
        ...
    )
    
    OUTPUT_DIR = args.output
```

---

## 🔧 已修复的文件

| 文件 | 修复 | 状态 |
|------|------|------|
| `sync_indonesian_tafsir.py` | 移动 `global OUTPUT_DIR` 到函数开始 | ✅ 已修复 |
| `import_tafsir_to_db.py` | 移动 `global DB_PATH, JSON_DIR` 到函数开始 | ✅ 已修复 |

---

## ✅ 验证

```bash
# 语法检查
python3 -m py_compile sync_indonesian_tafsir.py
python3 -m py_compile import_tafsir_to_db.py

# 输出: ✅ 语法检查通过
```

---

## 🚀 现在可以正常使用

```bash
# 验证 API 访问
python3 sync_indonesian_tafsir.py --verify

# 执行完整同步
python3 sync_indonesian_tafsir.py

# 导入到数据库
python3 import_tafsir_to_db.py
```

---

## 📚 Python Global 声明规则

### 规则 1：声明必须在使用前

```python
def func():
    global x  # ✅ 正确：在使用前声明
    print(x)
    x = 10
```

```python
def func():
    print(x)  # ❌ 错误：在声明前使用
    global x
    x = 10
```

---

### 规则 2：在函数开始处声明（最佳实践）

```python
def main():
    global VAR1, VAR2, VAR3  # ✅ 推荐：在函数开始处
    
    # 函数其余代码...
```

---

### 规则 3：避免过度使用 global

**不推荐：**
```python
global_var = "value"

def func():
    global global_var  # 可以工作，但不是最佳实践
    global_var = "new value"
```

**推荐：**
```python
def func(var):
    return "new value"  # 通过返回值而不是修改全局变量

result = func(global_var)
```

---

## 🎯 总结

✅ **问题：** `global` 声明在变量使用之后  
✅ **修复：** 将 `global` 声明移到函数开始处  
✅ **状态：** 两个脚本都已修复并通过语法检查  
✅ **测试：** 可以正常运行  

---

**修复时间：** 2025-11-18  
**修复状态：** ✅ 已完成  
**可以使用：** 是

