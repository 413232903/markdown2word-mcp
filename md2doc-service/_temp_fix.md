# 临时编译问题解决方案

当前问题：md2doc-core 模块使用了 `lombok.val` 功能，但编译时未正确处理。

## 推荐方案：使用IDE运行

由于 md2doc-core 有编译问题，建议使用 IntelliJ IDEA 直接运行：

1. 打开 IntelliJ IDEA
2. 打开项目：/Users/user/413232903.github.io/md2doc-plus
3. 等待项目加载完成（可能需要一些时间）
4. 找到文件：md2doc-service/src/main/java/cn/daydayup/dev/md2doc/service/Md2docServiceApplication.java
5. 右键点击该文件 -> Run 'Md2docServiceApplication.main()'
6. 服务将在 http://localhost:8080 启动

## 或者：修复 md2doc-core 编译问题

需要将 `lombok.val` 替换为具体类型。例如：
- `val params = ...` 改为 `WordParams params = ...`
- `val runs = ...` 改为具体的类型声明

需要在 md2doc-core 模块的以下文件中替换：
- MarkdownToWordConverter.java
- PoiWordGenerator.java  
- WordParams.java
- ChartTable.java
- PoiUtil.java

