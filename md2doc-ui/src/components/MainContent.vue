<template>
  <main class="main">
    <div class="container">
      <div class="converter-layout">
        <!-- 左侧 Markdown 编辑区域 -->
        <div class="markdown-panel">
          <div class="panel-header">
            <h2>Markdown 编辑器</h2>
          </div>
          
          <div class="tabs">
            <button 
              :class="{ active: activeTab === 'text' }" 
              @click="activeTab = 'text'"
            >
              文本输入
            </button>
            <button 
              :class="{ active: activeTab === 'file' }" 
              @click="activeTab = 'file'"
            >
              文件上传
            </button>
          </div>
          
          <div class="tab-content">
            <!-- 文本转换 -->
            <div v-show="activeTab === 'text'" class="tab-pane">
              <div class="form-group">
                <textarea 
                  id="markdownText"
                  v-model="markdownText" 
                  placeholder="在此输入 Markdown 内容..."
                  rows="15"
                ></textarea>
              </div>
            </div>
            
            <!-- 文件转换 -->
            <div v-show="activeTab === 'file'" class="tab-pane">
              <div class="form-group">
                <div class="file-upload" @dragover.prevent @drop.prevent="handleDrop">
                  <input 
                    type="file" 
                    id="markdownFile" 
                    accept=".md,.markdown"
                    @change="handleFileSelect"
                    ref="fileInput"
                  >
                  <div 
                    class="file-upload-area" 
                    @click="$refs.fileInput.click()"
                  >
                    <div v-if="!selectedFile" class="file-upload-placeholder">
                      <p>点击选择文件或拖拽文件到此处</p>
                      <p class="file-upload-hint">支持 .md 和 .markdown 文件</p>
                    </div>
                    <div v-else class="file-upload-selected">
                      <p>已选择文件: {{ selectedFile.name }}</p>
                      <button @click.stop="clearFile" class="clear-btn">清除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <button 
              @click="convertMarkdown" 
              :disabled="isConverting"
              class="convert-btn"
            >
              {{ isConverting ? '转换中...' : '转换为 Word' }}
            </button>
          </div>
          
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
        </div>
        
        <!-- 右侧 Word 预览区域 -->
        <div class="word-preview-panel">
          <div class="panel-header">
            <h2>Word 文档预览</h2>
          </div>
          
          <div class="preview-container">
            <div v-if="docxUrl" class="docx-preview">
              <div id="docx-preview" ref="docxPreviewRef" class="docx-preview-container"></div>
            </div>
            <div v-else class="preview-placeholder">
              <p>转换后的 Word 文档将在此处预览</p>
            </div>
          </div>
          
          <!-- 下载按钮 -->
          <div v-if="docxUrl" class="download-section">
            <button 
              @click="downloadDocx" 
              class="download-btn"
              :disabled="isDownloading"
            >
              {{ isDownloading ? '下载中...' : '下载 Word 文档' }}
            </button>
                  
            <!-- 图表预览说明 -->
            <div class="chart-notice" v-if="hasCharts">
              <p>注意：预览中可能无法完整显示图表，下载文档后可查看完整图表效果</p>
            </div>
          </div>
        </div>
      </div>
      
      <div class="features">
        <h3>支持的 Markdown 语法</h3>
        <ul>
          <li>标题 (H1-H6)</li>
          <li>段落文本</li>
          <li>表格</li>
          <li>ECharts 图表代码块 (使用 ```echarts 代码块)</li>
        </ul>
      </div>
    </div>
  </main>
</template>

<script>
import axios from 'axios'
import { renderAsync } from 'docx-preview'

export default {
  name: 'MainContent',
  data() {
    return {
      activeTab: 'text',
      markdownText: '# 示例标题\n' +
          '\n' +
          '这是段落内容。\n' +
          '\n' +
          '## 二级标题\n' +
          '\n' +
          '支持表格:\n' +
          '\n' +
          '| 姓名 | 年龄 | 城市 |\n' +
          '| ---- | ---- | ---- |\n' +
          '| 张三 | 25   | 北京 |\n' +
          '| 李四 | 30   | 上海 |\n' +
          '\n' +
          '支持代码块:\n' +
          '\n' +
          '```echarts\n' +
          '{\n' +
          '    title: {\n' +
          '        text: \'月度销售数据1\'\n' +
          '    },\n' +
          '    tooltip: {\n' +
          '        trigger: \'axis\'\n' +
          '    },\n' +
          '    xAxis: {\n' +
          '        type: \'category\',\n' +
          '        data: [\'1月\', \'2月\', \'3月\', \'4月\', \'5月\', \'6月\']\n' +
          '    },\n' +
          '    yAxis: {\n' +
          '        type: \'value\',\n' +
          '        name: \'销售额\'\n' +
          '    },\n' +
          '    series: [{\n' +
          '        name: \'销售额\',\n' +
          '        type: \'line\',\n' +
          '        data: [15.32, 15.87, 14.96, 16.23, 13.21, 13.53]\n' +
          '    }]\n' +
          '}\n' +
          '```',
      selectedFile: null,
      isConverting: false,
      error: '',
      docxUrl: null
    }
  },
  computed: {
    hasCharts() {
      // 检测Markdown内容中是否包含图表
      return this.markdownText.includes('```echarts') || 
             this.markdownText.includes('```chart');
    }
  },
  methods: {
    async convertMarkdown() {
      this.error = ''
      this.docxUrl = null
      
      try {
        let response
        this.isConverting = true
        
        if (this.activeTab === 'text') {
          // 文本输入模式
          if (!this.markdownText.trim()) {
            this.error = '请输入 Markdown 内容'
            this.isConverting = false
            return
          }
          
          response = await axios.post('/api/markdown/convert/text', {
            content: this.markdownText
          })
        } else {
          // 文件上传模式
          if (!this.selectedFile) {
            this.error = '请选择要转换的文件'
            this.isConverting = false
            return
          }
          
          const formData = new FormData()
          formData.append('file', this.selectedFile)
          
          response = await axios.post('/api/markdown/convert/file', formData)
        }
        
        // 获取返回的文件URL
        this.docxUrl = response.data.fileUrl
        
        this.$nextTick(() => {
          this.renderDocxPreview(this.docxUrl)
        })
      } catch (err) {
        this.error = '转换失败，请稍后重试'
        console.error('转换失败:', err)
      } finally {
        this.isConverting = false
      }
    },
    
    renderDocxPreview(docxUrl) {
      // 通过URL获取文档内容并渲染
      fetch(docxUrl)
        .then(response => {
          if (!response.ok) {
            throw new Error('Network response was not ok')
          }
          return response.blob()
        })
        .then(blob => {
          try {
            // 使用docx-preview渲染Word文档
            renderAsync(blob, this.$refs.docxPreviewRef, undefined, {
              className: "docx-preview", // 默认和文档样式类的类名/前缀
              inWrapper: true, // 启用围绕文档内容渲染包装器
              ignoreWidth: false, // 禁用页面渲染宽度
              ignoreHeight: false, // 禁用页面渲染高度
              ignoreFonts: false, // 禁用字体渲染
              breakPages: true, // 在分页符上启用分页
              ignoreLastRenderedPageBreak: true, // 禁用lastRenderedPageBreak元素的分页
              ignoreStyleLink: true, // 禁用样式表链接
              enableClassName: true, // 启用类名
              trimXmlDeclaration: true, // 如果为真，xml声明将在解析之前从xml文档中删除
            }).then(() => {
              console.log('文档渲染成功')
            }).catch(error => {
              console.error('文档渲染失败:', error)
              this.error = '文档渲染失败'
            })
          } catch (error) {
            console.error('渲染文档时出错:', error)
            this.error = '文档预览加载失败'
          }
        })
        .catch(error => {
          console.error('获取文档失败:', error)
          this.error = '获取文档失败'
        })
    },
    
    handleFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        this.selectedFile = file
      }
    },
    
    handleDrop(event) {
      const file = event.dataTransfer.files[0]
      if (file && (file.name.endsWith('.md') || file.name.endsWith('.markdown'))) {
        this.selectedFile = file
      } else {
        this.error = '请选择有效的 Markdown 文件 (.md 或 .markdown)'
      }
    },
    
    clearFile() {
      this.selectedFile = null
      this.$refs.fileInput.value = ''
    },
    
    downloadDocx() {
      this.isDownloading = true;
      
      // 创建一个隐藏的iframe来触发下载
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = this.docxUrl
      document.body.appendChild(iframe)
      
      // 延迟移除iframe
      setTimeout(() => {
        document.body.removeChild(iframe)
        this.isDownloading = false
      }, 1000)
    }
  }
}
</script>

<style scoped>
.main {
  flex: 1;
  padding: 2rem 0;
}

.container {
  max-width: 100%;
  height: 100%;
  margin: 0 auto;
  padding: 0 1rem;
}

.converter-layout {
  display: flex;
  gap: 2rem;
  height: 70vh;
  margin-bottom: 2rem;
}

.markdown-panel {
  flex: 1;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
}

.word-preview-panel {
  flex: 1;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
}

.panel-header h2 {
  color: #333;
  margin-bottom: 1rem;
  text-align: center;
}

.tabs {
  display: flex;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.tabs button {
  flex: 1;
  padding: 0.8rem;
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  color: #666;
  transition: all 0.3s ease;
}

.tabs button.active {
  color: #667eea;
  border-bottom: 3px solid #667eea;
  font-weight: bold;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
  color: #333;
}

textarea {
  width: 100%;
  height: calc(100% - 100px);
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-family: 'Courier New', monospace;
  font-size: 1rem;
  resize: vertical;
  transition: border-color 0.3s ease;
}

textarea:focus {
  outline: none;
  border-color: #667eea;
}

.file-upload {
  position: relative;
  height: 100%;
}

#markdownFile {
  display: none;
}

.file-upload-area {
  border: 2px dashed #ddd;
  border-radius: 5px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 80%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.file-upload-area:hover {
  border-color: #667eea;
  background-color: #f9f9ff;
}

.file-upload-placeholder p {
  color: #999;
  margin: 0.5rem 0;
}

.file-upload-hint {
  font-size: 0.9rem;
}

.file-upload-selected {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-upload-selected p {
  color: #333;
  margin: 0;
}

.clear-btn {
  background: #ff6b6b;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.clear-btn:hover {
  background: #ff5252;
}

.convert-btn {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: opacity 0.3s ease;
}

.convert-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.convert-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.preview-container {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 5px;
  overflow: auto;
  background: #f9f9f9;
  max-height: 60vh; /* 限制预览区域最大高度 */
}

.docx-preview-container {
  width: 100%;
  height: 100%;
  padding: 10px;
}

.docx-preview :deep(.docx-wrapper) {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin: 0 auto;
  padding: 20px;
  max-width: 100%; /* 确保内容不超出容器 */
}

.preview-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #999;
}

.download-section {
  margin-top: 1rem;
  text-align: center;
  position: relative; /* 为子元素绝对定位提供基准 */
}

.download-btn {
  padding: 0.8rem 1.5rem;
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 1rem;
  cursor: pointer;
  transition: opacity 0.3s ease;
}

.download-btn:hover {
  opacity: 0.9;
}

.error-message {
  color: #ff6b6b;
  text-align: center;
  padding: 1rem;
  background: #fff5f5;
  border-radius: 5px;
  border: 1px solid #fed7d7;
}

.features {
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.features h3 {
  color: #333;
  margin-bottom: 1rem;
  text-align: center;
}

.features ul {
  list-style-type: none;
  padding: 0;
}

.features li {
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
  color: #666;
}

.features li:last-child {
  border-bottom: none;
}

.features li::before {
  content: "✓";
  color: #4caf50;
  margin-right: 0.5rem;
}

.chart-notice {
  margin-top: 1rem;
  padding: 0.8rem;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 5px;
  color: #856404;
  font-size: 0.9rem;
}

.chart-notice p {
  margin: 0;
  text-align: center;
}

@media (max-width: 768px) {
  .converter-layout {
    flex-direction: column;
    height: auto;
  }
  
  .markdown-panel,
  .word-preview-panel {
    width: 100%;
  }
}
</style>