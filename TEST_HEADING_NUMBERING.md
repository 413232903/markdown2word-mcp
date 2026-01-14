# 标题编号重置修复测试

## 修复前的问题

不同级别的标题下的内容存在连续编号，编号没有重置。

例如：
```markdown
# 第一章
## 第一节
### 第一小节
## 第二节

# 第二章
## 第一节
```

**修复前可能的结果：**
```
一、第一章
1、第一节
1）第一小节
2、第二节
二、第二章
3、第一节  ← 错误：应该重置为1
```

## 修复后的预期结果

```
一、第一章
1、第一节
1）第一小节
2、第二节
二、第一章
1、第一节  ← 正确：重置为1
```

## 修复说明

### 根本原因
1. `parentLevels` 使用简单的键（level）只记录了直接父级的编号
2. 当进入 H1 时，没有记录 H1 的父级（level=0），导致后续 H2 无法检测 H1 编号变化
3. 从子标题跳回父级时，只检查了直接父级，没有检查所有更高级别

### 修复方案
1. **改进键结构**：使用复合键 `level_parentLevel` 记录每个级别的所有父级编号
   - 例如：H2 的父级记录为 `2_1`（H2 的 H1 父级）
   - H3 的父级记录包括 `3_1`（H3 的 H1 父级）和 `3_2`（H3 的 H2 父级）

2. **完整记录父级**：在 `level > lastLevel` 分支中，记录所有父级（包括 level=0）
   ```java
   for (int parentLevel = 0; parentLevel < level; parentLevel++) {
       int currentParentNumber = (parentLevel == 0) ? 0 : levelCounters.getOrDefault(parentLevel, 0);
       parentLevels.put(level + "_" + parentLevel, currentParentNumber);
   }
   ```

3. **检查所有父级变化**：在 `level < lastLevel` 分支中，检查所有父级（1 到 level-1）
   ```java
   for (int parentLevel = 1; parentLevel < level; parentLevel++) {
       int currentParentNumber = levelCounters.getOrDefault(parentLevel, 0);
       Integer lastParentNumber = parentLevels.get(level + "_" + parentLevel);

       if (lastParentNumber != null && currentParentNumber != lastParentNumber) {
           parentLevelChanged = true;
           break;
       }
   }
   ```

## 测试用例

### 用例 1：基本多级标题
```markdown
# 第一章
## 第一节
### 第一小节
### 第二小节
## 第二节
# 第二章
## 第一节
```

**期望输出：**
```
一、第一章
1、第一节
1）第一小节
2）第二小节
2、第二节
二、第二章
1、第一节
```

### 用例 2：复杂嵌套
```markdown
# 第一章
## 第一节
### 第一小节
#### 更深层级
### 第二小节
## 第二节
### 第一小节
# 第二章
## 第一节
### 第一小节
## 第二节
```

**期望输出：**
```
一、第一章
1、第一节
1）第一小节
1.1.1 更深层级
2）第二小节
2、第二节
1）第一小节
二、第二章
1、第一节
1）第一小节
2、第二节
```

### 用例 3：同级标题连续编号
```markdown
# 第一章
## 第一节
## 第二节
## 第三节
# 第二章
## 第一节
## 第二节
```

**期望输出：**
```
一、第一章
1、第一节
2、第二节
3、第三节
二、第二章
1、第一节
2、第二节
```

## 修改的文件

1. **Java 版本**：
   - `md2doc-core/src/main/java/cn/daydayup/dev/md2doc/core/template/DynamicWordDocumentCreator.java`
   - 修改了 `HeaderNumbering` 内部类的 `enterLevel()` 方法

2. **Python 版本**：
   - `md2doc-service-python/src/md2doc_mcp/core/template_creator.py`
   - 修改了 `HeaderNumbering` 类的 `enter_level()` 方法

## 编译和测试

### Java 版本
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动服务
cd md2doc-service
mvn spring-boot:run
```

### Python 版本
```bash
cd md2doc-service-python
pip install -e .

# 运行测试
pytest tests/
```

## 验证步骤

1. 使用测试 Markdown 文件转换
2. 检查生成的 Word 文档
3. 验证标题编号是否符合预期
4. 特别注意以下场景：
   - 从一个 H1 切换到另一个 H1 后，H2 是否重置
   - 同一个 H1 下的多个 H2 是否连续编号
   - 跨越多个层级的复杂嵌套是否正确
