// pages/detail/detail.js - 任务详情页
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    todo: null,
    priorityConfig: null,
    formattedCreatedAt: '',
    formattedCompletedAt: ''
  },

  onLoad(options) {
    const { id } = options;
    if (!id) {
      wx.navigateBack();
      return;
    }
    this.todoId = id;
    this.loadTodo();
  },

  onShow() {
    // 从编辑返回后刷新
    if (this.todoId) this.loadTodo();
  },

  /**
   * 加载单条 Todo 数据
   */
  async loadTodo() {
    try {
      const todos = await api.getAllTodos();
      const todo = todos.find(t => t.id.toString() === this.todoId);
      if (!todo) {
        wx.showToast({ title: '任务不存在', icon: 'none' });
        setTimeout(() => wx.navigateBack(), 1200);
        return;
      }

      this.setData({
        todo,
        priorityConfig: helper.getPriorityConfig(todo.priority),
        formattedCreatedAt: helper.formatTime(todo.createdAt),
        formattedCompletedAt: todo.completedAt
          ? helper.formatTime(todo.completedAt)
          : ''
      });

      wx.setNavigationBarTitle({ title: todo.completed ? '✅ 已完成' : '📌 任务详情' });
    } catch (error) {
      console.error('加载任务详情失败:', error);
      wx.showToast({ title: '加载失败，请稍后重试', icon: 'none' });
    }
  },

  /**
   * 切换完成状态
   */
  async onToggle() {
    try {
      await api.toggleTodo(this.todoId);
      // 重新加载数据
      await this.loadTodo();
      const todo = this.data.todo;
      wx.showToast({
        title: todo.completed ? '任务完成 🎉' : '已标记为未完成',
        icon: 'none',
        duration: 1500
      });
    } catch (error) {
      console.error('切换状态失败:', error);
      wx.showToast({ title: '操作失败，请稍后重试', icon: 'none' });
    }
  },

  /**
   * 删除任务
   */
  onDelete() {
    wx.showModal({
      title: '删除确认',
      content: '此操作不可恢复，确定要删除这条任务吗？',
      confirmText: '确认删除',
      confirmColor: '#fc5c7d',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteTodo(this.todoId);
            wx.showToast({ title: '已删除', icon: 'success', duration: 1200 });
            setTimeout(() => wx.navigateBack(), 1200);
          } catch (error) {
            console.error('删除失败:', error);
            wx.showToast({ title: '删除失败，请稍后重试', icon: 'none' });
          }
        }
      }
    });
  },

  /**
   * 复制任务标题
   */
  onCopyTitle() {
    wx.setClipboardData({
      data: this.data.todo.title,
      success: () => {
        wx.showToast({ title: '已复制标题', icon: 'success' });
      }
    });
  }
});
