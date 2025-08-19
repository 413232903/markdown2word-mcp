<template>
  <footer class="footer">
    <div class="container">
      <p>© 2025 md2doc-plus - Markdown 转 Word 工具</p>
      <p class="footer-links">
        <a href="https://github.com" target="_blank">GitHub</a> | 
        <a href="#" @click.prevent="showHelp">使用帮助</a>
      </p>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'Footer',
  methods: {
    showHelp() {
      alert('使用帮助：\n\n1. 选择转换方式：文本转换或文件转换\n2. 输入 Markdown 内容或上传 Markdown 文件\n3. 点击"转换为 Word"按钮\n4. 下载生成的 Word 文档')
    }
  }
}
</script>

<style scoped>
.footer {
  background: #333;
  color: white;
  text-align: center;
  padding: 2rem 0;
  margin-top: auto;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

p {
  margin: 0.5rem 0;
}

.footer-links a {
  color: #667eea;
  text-decoration: none;
  margin: 0 0.5rem;
}

.footer-links a:hover {
  text-decoration: underline;
}
</style>