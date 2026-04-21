// pages/ai-chat/ai-chat.js
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    messages: [],
    inputText: '',
    scrollTop: 999999,
    isLoading: false,
    userAvatar: '👤'
  },

  onLoad() {
    this.initWelcomeMessage();
    this.loadUserAvatar();
  },

  initWelcomeMessage() {
    this.setData({
      messages: [
        {
          id: 'welcome',
          role: 'assistant',
          content: '你好！我是你的 Todo List 助手 📋。我可以帮你：\n\n' +
                   '• 创建任务，例如："帮我创建一个明天下午3点前完成报告的任务"\n' +
                   '• 查看任务列表，说："显示我的任务"\n' +
                   '• 完成任务，说："把\"写报告\"任务标记为完成"\n' +
                   '• 删除任务，说："删除\"买牛奶\"任务"\n' +
                   '• 分配任务给好友，说："让小明帮忙设计logo"\n' +
                   '• 查看好友列表，说："显示我的好友"\n' +
                   '• 接受任务，说："接受\"设计海报\"任务"\n\n' +
                   '有什么我可以帮你的吗？',
          timestamp: Date.now()
        }
      ]
    });
  },

  async loadUserAvatar() {
    try {
      const user = await api.getCurrentUser();
      if (user && user.avatar) {
        let avatarPath = user.avatar;
        try {
          avatarPath = await api.getAvatar(user.avatar);
        } catch (e) {
          console.error('获取头像失败:', e);
        }
        this.setData({ userAvatar: avatarPath });
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  },

  onInputChange(e) {
    this.setData({
      inputText: e.detail.value
    });
  },

  async onSendMessage() {
    const text = this.data.inputText.trim();
    if (!text || this.data.isLoading) return;

    const userMessage = {
      id: 'user_' + Date.now(),
      role: 'user',
      content: text,
      timestamp: Date.now()
    };

    this.setData({
      messages: [...this.data.messages, userMessage],
      inputText: '',
      isLoading: true
    });

    this.scrollToBottom();

    try {
      const response = await api.chat(text);

      // 处理响应
      let aiContent = '';
      let thinkContent = null;

      if (response.response !== undefined) {
        aiContent = response.response;
        if (response.thinkContent) {
          thinkContent = response.thinkContent;
        }
      } else {
        aiContent = response;
      }

      const aiResponse = {
        id: 'ai_' + Date.now(),
        role: 'assistant',
        content: aiContent,
        thinkContent: thinkContent,
        timestamp: Date.now()
      };

      this.setData({
        messages: [...this.data.messages, aiResponse],
        isLoading: false
      });

      this.scrollToBottom();
    } catch (error) {
      console.error('AI 聊天错误:', error);

      const errorResponse = {
        id: 'ai_error_' + Date.now(),
        role: 'assistant',
        content: '抱歉，我现在遇到了一些问题，请稍后再试。',
        timestamp: Date.now()
      };

      this.setData({
        messages: [...this.data.messages, errorResponse],
        isLoading: false
      });

      this.scrollToBottom();
    }
  },

  scrollToBottom() {
    setTimeout(() => {
      this.setData({ scrollTop: 999999 });
    }, 100);
  },

  onClearChat() {
    wx.showModal({
      title: '提示',
      content: '确定要清空聊天记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.initWelcomeMessage();
        }
      }
    });
  }
});