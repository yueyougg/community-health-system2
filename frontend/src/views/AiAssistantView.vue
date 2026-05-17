<template>
  <div class="ai-assistant-view">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="header-icon"><ChatDotRound /></el-icon>
            <span>AI 健康助手</span>
          </div>
          <el-tag size="small" type="success">在线</el-tag>
        </div>
      </template>

      <div class="chat-container" ref="chatContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <el-empty description="您好！我是您的AI健康助手，有什么可以帮您的？" :image-size="100" />
        </div>
        
        <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.role]">
          <div class="avatar" v-if="msg.role === 'assistant'">
            <el-avatar :size="40" class="ai-avatar"><el-icon><Service /></el-icon></el-avatar>
          </div>
          <div class="content">
            <div class="bubble">{{ msg.content }}</div>
          </div>
          <div class="avatar" v-if="msg.role === 'user'">
            <el-avatar :size="40" class="user-avatar"><el-icon><UserFilled /></el-icon></el-avatar>
          </div>
        </div>
        
        <div v-if="loading" class="message-item assistant">
          <div class="avatar">
            <el-avatar :size="40" class="ai-avatar"><el-icon><Service /></el-icon></el-avatar>
          </div>
          <div class="content">
            <div class="bubble loading-bubble">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            class="rounded-input"
            placeholder="请输入您的健康问题... (Enter 发送)"
            @keydown.enter.prevent="sendMessage"
          ></el-input>
          <el-button type="primary" class="send-btn" round :loading="loading" @click="sendMessage" :icon="Position">发送</el-button>
        </div>
        <div class="action-bar">
          <span class="tip">AI助手可能产生误导信息，仅供参考，不作为医疗诊断依据。</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { aiApi } from '../api/modules';
import { ElMessage } from 'element-plus';
import { ChatDotRound, UserFilled, Service, Position } from '@element-plus/icons-vue';
import { useAiChatStore } from '../store/aiChat';
import { storeToRefs } from 'pinia';

const chatStore = useAiChatStore();
const { messages } = storeToRefs(chatStore);

const inputMessage = ref('');
const loading = ref(false);
const chatContainer = ref(null);

// 组件挂载时滚动到底部
onMounted(() => {
  scrollToBottom();
});

const scrollToBottom = async () => {
  await nextTick();
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};

const sendMessage = async () => {
  const content = inputMessage.value.trim();
  if (!content) return;
  
  if (loading.value) return;

  chatStore.addMessage({ role: 'user', content });
  inputMessage.value = '';
  scrollToBottom();
  
  loading.value = true;
  
  // 构建发送给后端的上下文
  const apiMessages = messages.value.map(m => ({ role: m.role, content: m.content }));
  
  try {
    const res = await aiApi.chat({ messages: apiMessages });
    chatStore.addMessage({ role: 'assistant', content: res.data });
  } catch (error) {
    // 错误处理：如果没有抛出明确错误，可能是后端返回了普通错误信息
    ElMessage.error('AI服务暂时不可用，请稍后重试');
    chatStore.addMessage({ role: 'assistant', content: '抱歉，我现在遇到了一点网络问题，请稍后再试。' });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};
</script>

<style scoped>
.ai-assistant-view {
  height: calc(100vh - 112px); /* 视口高度减去 header 和 padding */
  display: flex;
  flex-direction: column;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.header-icon {
  color: #1aa094;
  font-size: 20px;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
}

.message-item.user {
  justify-content: flex-end;
}

.ai-avatar {
  background-color: #1aa094;
  color: #fff;
}

.user-avatar {
  background-color: #409EFF;
  color: #fff;
}

.bubble {
  max-width: 600px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.assistant .bubble {
  background-color: #ffffff;
  color: #333333;
  border-top-left-radius: 0;
}

.user .bubble {
  background-color: #1aa094;
  color: #ffffff;
  border-top-right-radius: 0;
}

.input-area {
  padding: 16px 20px;
  background-color: #ffffff;
  border-top: 1px solid #ebeef5;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.rounded-input .el-input__wrapper) {
  border-radius: 24px;
  padding: 4px 20px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.rounded-input .el-input__inner) {
  height: 40px;
  line-height: 40px;
}

:deep(.rounded-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1aa094 inset;
}

.send-btn {
  height: 48px;
  padding: 0 24px;
  border-radius: 24px;
  font-size: 15px;
}

.action-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 12px;
}

.tip {
  font-size: 12px;
  color: #909399;
}

/* 加载动画 */
.loading-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
}

.dot {
  width: 6px;
  height: 6px;
  background-color: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
