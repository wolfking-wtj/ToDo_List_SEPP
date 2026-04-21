// pages/add-friend/add-friend.js
const api = require('../../utils/api');

Page({
  data: {
    searchKeyword: '',
    searchResults: [],
    loading: false,
    showResults: false
  },

  onLoad() {
  },

  onSearchInput(e) {
    const keyword = e.detail.value.trim();
    this.setData({ searchKeyword: keyword });
    
    if (keyword.length >= 2) {
      this.searchUsers(keyword);
    } else {
      this.setData({ searchResults: [], showResults: false });
    }
  },

  async searchUsers(keyword) {
    this.setData({ loading: true, showResults: true });
    
    try {
      const results = await api.searchUsers(keyword);
      
      this.setData({
        searchResults: results || [],
        loading: false
      });
    } catch (error) {
      console.error('搜索用户失败:', error);
      this.setData({ loading: false });
      wx.showToast({ title: '搜索失败', icon: 'none' });
    }
  },

  async onTapUser(e) {
    const { user } = e.currentTarget.dataset;
    
    if (user.isFriend) {
      wx.showToast({ title: '已经是好友', icon: 'none' });
      return;
    }
    
    try {
      const result = await api.addFriend(user.id);
      
      wx.showToast({ title: '添加成功', icon: 'success' });
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (error) {
      console.error('添加好友失败:', error);
      wx.showToast({ title: error.message || '添加失败', icon: 'none' });
    }
  },

  onClose() {
    wx.navigateBack();
  }
});
