import { defineStore } from 'pinia';

export const useAiChatStore = defineStore('aiChat', {
  state: () => ({
    messages: [
      {
        role: 'assistant',
        content: '您好！我是您的AI健康助手。您可以向我咨询日常护理、慢性病管理、饮食建议等健康相关问题。'
      }
    ]
  }),
  actions: {
    addMessage(message) {
      this.messages.push(message);
    },
    clearMessages() {
      this.messages = [
        {
          role: 'assistant',
          content: '您好！我是您的AI健康助手。您可以向我咨询日常护理、慢性病管理、饮食建议等健康相关问题。'
        }
      ];
    }
  }
});
