// pages/profile/profile.js
const api = require('../../utils/api');

Page({
  data: {
    userInfo: {
      name: '加载中...',
      avatar: '👤',
      level: 1,
      points: 0
    },
    showEditModal: false,
    editForm: {
      username: '',
      email: ''
    },
    menuItems: [
      {
        id: 1,
        icon: '📋',
        title: '任务统计',
        subtitle: '查看完成情况',
        path: ''
      },
      {
        id: 2,
        icon: '⚙️',
        title: '设置',
        subtitle: '账号与隐私',
        path: ''
      },
      {
        id: 3,
        icon: '📞',
        title: '客服',
        subtitle: '在线帮助',
        path: ''
      },
      {
        id: 4,
        icon: 'ℹ️',
        title: '关于',
        subtitle: '版本 1.0.0',
        path: ''
      }
    ]
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  async loadUserInfo() {
    try {
      const user = await api.getCurrentUser();
      
      const username = user.username || 'user';
      const storageKey = 'userAvatar_' + username;
      
      let finalAvatar = wx.getStorageSync(storageKey);
      
      const backendAvatar = user.avatar;
      
      if (!finalAvatar && backendAvatar) {
        try {
          const avatarPath = await api.getAvatar(backendAvatar);
          finalAvatar = avatarPath;
          wx.setStorageSync(storageKey, avatarPath);
        } catch (avatarError) {
          console.error('获取头像失败:', avatarError);
          finalAvatar = '👤';
        }
      }
      
      if (!finalAvatar || finalAvatar === '') {
        finalAvatar = '👤';
      }
      
      this.setData({
        userInfo: {
          name: user.name || user.username || '用户',
          username: user.username || 'user',
          avatar: finalAvatar,
          level: user.level || 1,
          points: user.points || 0
        },
        editForm: {
          username: user.username || '',
          email: user.email || '',
          name: user.name || ''
        }
      });
    } catch (error) {
      console.error('加载用户信息失败:', error);
    }
  },

  onTapMenuItem(e) {
    const { id } = e.currentTarget.dataset;
    
    const menuMap = {
      1: '/pages/task-stats/task-stats',
      2: '/pages/settings/settings',
      3: '/pages/customer-service/customer-service',
      4: '/pages/about/about'
    };
    
    const path = menuMap[id];
    if (path) {
      wx.navigateTo({ url: path });
    } else {
      wx.showToast({ title: '功能开发中', icon: 'none' });
    }
  },

  onEditProfile() {
    this.setData({ showEditModal: true });
  },

  onCloseModal() {
    this.setData({ showEditModal: false });
  },

  onNameInput(e) {
    this.setData({
      'editForm.name': e.detail.value
    });
  },

  onUsernameInput(e) {
    this.setData({
      'editForm.username': e.detail.value
    });
  },

  onEmailInput(e) {
    this.setData({
      'editForm.email': e.detail.value
    });
  },

  onTapAvatar() {
    this.onChooseAvatar();
  },

  onChooseAvatar() {
    wx.showActionSheet({
      itemList: ['从相册选择', '拍照'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.chooseImageFromAlbum();
        } else if (res.tapIndex === 1) {
          this.takePhoto();
        }
      }
    });
  },

  chooseImageFromAlbum() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album'],
      success: (res) => {
        this.uploadAvatar(res.tempFiles[0].tempFilePath);
      },
      fail: (error) => {
        console.error('选择图片失败:', error);
      }
    });
  },

  takePhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: (res) => {
        this.uploadAvatar(res.tempFiles[0].tempFilePath);
      },
      fail: (error) => {
        console.error('拍照失败:', error);
      }
    });
  },

  async uploadAvatar(tempFilePath) {
    try {
      wx.showLoading({ title: '上传中...' });
      
      this.setData({
        'userInfo.avatar': tempFilePath
      });
      
      const result = await api.uploadAvatar(tempFilePath);
      
      let fullAvatarUrl = result.avatarUrl;
      if (fullAvatarUrl && fullAvatarUrl.startsWith('/')) {
        fullAvatarUrl = 'http://localhost:8080' + fullAvatarUrl;
      }
      
      if (fullAvatarUrl && fullAvatarUrl !== '') {
        try {
          await api.updateCurrentUser({ avatar: fullAvatarUrl });
          
          try {
            const avatarPath = await api.getAvatar(fullAvatarUrl);
            const username = this.data.userInfo.name;
            const storageKey = 'userAvatar_' + username;
            wx.setStorageSync(storageKey, avatarPath);
            
            this.setData({
              'userInfo.avatar': avatarPath
            });
          } catch (avatarError) {
            console.error('获取头像数据失败:', avatarError);
            const username = this.data.userInfo.name;
            const storageKey = 'userAvatar_' + username;
            wx.setStorageSync(storageKey, tempFilePath);
            this.setData({
              'userInfo.avatar': tempFilePath
            });
          }
          
          wx.hideLoading();
          wx.showToast({ title: '头像更新成功', icon: 'success' });
        } catch (updateError) {
          console.error('更新用户头像信息失败:', updateError);
          const username = this.data.userInfo.name;
          const storageKey = 'userAvatar_' + username;
          wx.setStorageSync(storageKey, tempFilePath);
          this.setData({
            'userInfo.avatar': tempFilePath
          });
          wx.hideLoading();
          wx.showToast({ title: '头像上传成功，但更新用户信息失败', icon: 'none' });
        }
      } else {
        this.setData({
          'userInfo.avatar': '👤'
        });
        wx.hideLoading();
        wx.showToast({ title: '头像上传失败：未返回头像URL', icon: 'none' });
      }
    } catch (error) {
      this.setData({
        'userInfo.avatar': '👤'
      });
      wx.hideLoading();
      console.error('上传头像失败:', error);
      const errorMsg = error.message || '未知错误';
      wx.showToast({ title: '头像更新失败: ' + errorMsg, icon: 'none' });
    }
  },

  async onSaveProfile() {
    try {
      const { username, email, name } = this.data.editForm;
      
      const updatedUser = await api.updateCurrentUser({ username, email, name });
      
      this.setData({
        'userInfo.name': updatedUser.name || updatedUser.username,
        'userInfo.username': updatedUser.username,
        'userInfo.email': updatedUser.email,
        showEditModal: false
      });
      
      wx.showToast({ title: '个人信息更新成功', icon: 'success' });
    } catch (error) {
      console.error('更新个人信息失败:', error);
      wx.showToast({ title: '更新失败，请稍后重试', icon: 'none' });
    }
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      confirmText: '确定',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.redirectTo({ url: '/pages/login/login' });
        }
      }
    });
  }
});