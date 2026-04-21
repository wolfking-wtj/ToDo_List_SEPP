// pages/task-accept/task-accept.js
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    tasks: [],
    loading: true,
    userAvatarMap: {}
  },

  onLoad() {
    console.log('任务接收页面加载');
    this.loadTasks();
  },

  async loadTasks() {
    try {
      this.setData({ loading: true });

      const tasks = await api.getPendingTasks();

      if (tasks.length === 0) {
        wx.showToast({ title: '没有待接收的任务', icon: 'none' });
        setTimeout(() => wx.navigateBack(), 1500);
        return;
      }

      const avatarMap = {};
      for (let task of tasks) {
        if (task.sender && task.sender.avatar) {
          try {
            const avatarPath = await api.getAvatar(task.sender.avatar);
            avatarMap[task.sender.id] = avatarPath;
          } catch (e) {
            console.error('获取头像失败:', e);
          }
        }
      }

      const formattedTasks = tasks.map(task => ({
        ...task,
        formattedDeadline: task.dueDate ? helper.formatDeadline(task.dueDate) : '',
        formattedCreatedAt: task.createdAt ? helper.formatTime(task.createdAt) : ''
      }));

      this.setData({
        tasks: formattedTasks,
        userAvatarMap: avatarMap,
        loading: false
      });

      wx.setNavigationBarTitle({ title: '待接收任务' });
    } catch (error) {
      console.error('加载任务失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
      this.setData({ loading: false });
    }
  },

  getSenderAvatar(senderId) {
    return this.data.userAvatarMap[senderId] || null;
  },

  async onAccept(e) {
    const { taskId } = e.currentTarget.dataset;

    wx.showModal({
      title: '确认接收',
      content: '确定要接收此任务吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.acceptTask(taskId);
            wx.showToast({
              title: '任务已接收',
              icon: 'success',
              duration: 1500
            });
            this.loadTasks();
          } catch (error) {
            console.error('接收任务失败:', error);
            wx.showToast({ title: '接收失败', icon: 'none' });
          }
        }
      }
    });
  },

  async onReject(e) {
    const { taskId } = e.currentTarget.dataset;

    wx.showModal({
      title: '确认拒绝',
      content: '确定要拒绝此任务吗？',
      confirmColor: '#fc5c7d',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.rejectTask(taskId);
            wx.showToast({
              title: '已拒绝',
              icon: 'success',
              duration: 1500
            });
            this.loadTasks();
          } catch (error) {
            console.error('拒绝任务失败:', error);
            wx.showToast({ title: '拒绝失败', icon: 'none' });
          }
        }
      }
    });
  }
});