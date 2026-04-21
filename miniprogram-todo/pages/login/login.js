// pages/login/login.js
const api = require('../../utils/api');

Page({
  data: {
    username: '',
    password: '',
    errorMessage: ''
  },

  handleUsernameInput(e) {
    this.setData({
      username: e.detail.value
    });
  },

  handlePasswordInput(e) {
    this.setData({
      password: e.detail.value
    });
  },

  async handleLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      this.setData({ errorMessage: '请输入用户名和密码' });
      return;
    }

    try {
      const response = await api.login({ username, password });
      if (response.token) {
        // 保存token到本地存储
        wx.setStorageSync('token', response.token);
        wx.setStorageSync('username', response.user?.username || username);
        // 跳转到首页
        wx.switchTab({ url: '/pages/index/index' });
      } else {
        this.setData({ errorMessage: '登录失败，请检查用户名和密码' });
      }
    } catch (error) {
      this.setData({ errorMessage: '登录失败，请稍后重试' });
      console.error('登录失败:', error);
    }
  },

  async handleRegister() {
    const { username, password } = this.data;
    if (!username || !password) {
      this.setData({ errorMessage: '请输入用户名和密码' });
      return;
    }

    try {
      const response = await api.register({ 
        username, 
        password, 
        email: `${username}@example.com` // 简化处理，实际应用中应该让用户输入邮箱
      });
      if (response.id) {
        // 注册成功后自动登录
        await this.handleLogin();
      } else {
        this.setData({ errorMessage: '注册失败，请稍后重试' });
      }
    } catch (error) {
      this.setData({ errorMessage: '注册失败，请稍后重试' });
      console.error('注册失败:', error);
    }
  }
});