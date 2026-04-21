// pages/task-stats/task-stats.js
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    allTodos: [],
    filteredTodos: [],
    stats: { total: 0, completed: 0, pending: 0, percent: 0 },
    filter: 'all',
    filterTabs: [
      { key: 'all', label: '全部' },
      { key: 'pending', label: '进行中' },
      { key: 'completed', label: '已完成' }
    ],
    currentDate: ''
  },

  onLoad() {
    this.loadData();
    this.updateCurrentDate();
  },

  onShow() {
    this.loadData();
  },

  updateCurrentDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekdays[now.getDay()];
    
    this.setData({
      currentDate: `${year}年${month}月${day}日 ${weekday}`
    });
  },

  async loadData() {
    try {
      const allTodos = await api.getAllTodosWithDeleted();
      const stats = helper.calcStats(allTodos);
      const filteredTodos = this._filterTodos(allTodos);
      
      // 为每个任务添加展示字段
      const formattedTodos = filteredTodos.map(todo => ({
        ...todo,
        formattedTime: helper.formatTime(todo.createdAt),
        formattedDeadline: helper.formatDeadline(todo.deadline),
        priorityConfig: helper.getPriorityConfig(todo.priority),
        isDeleted: todo.deleted
      }));
      
      this.setData({ 
        allTodos, 
        stats, 
        filteredTodos: formattedTodos 
      });
    } catch (error) {
      console.error('加载数据失败:', error);
      wx.showToast({ title: '加载失败，请稍后重试', icon: 'none' });
    }
  },

  _filterTodos(todos) {
    const { filter } = this.data;
    let result = todos;

    if (filter === 'pending') {
      result = result.filter(t => !t.completed);
    } else if (filter === 'completed') {
      result = result.filter(t => t.completed);
    }

    // 按创建时间倒序排列
    result.sort((a, b) => b.createdAt - a.createdAt);

    return result;
  },

  onFilterChange(e) {
    const filter = e.currentTarget.dataset.key;
    const filteredTodos = this._filterTodos(this.data.allTodos);
    
    const formattedTodos = filteredTodos.map(todo => ({
      ...todo,
      formattedTime: helper.formatTime(todo.createdAt),
      formattedDeadline: helper.formatDeadline(todo.deadline),
      priorityConfig: helper.getPriorityConfig(todo.priority),
      isDeleted: todo.deleted
    }));
    
    this.setData({ 
      filter, 
      filteredTodos: formattedTodos 
    });
  },

  onToggleTodo(e) {
    const { id, status } = e.currentTarget.dataset;
    this.toggleTodoStatus(id, status);
  },

  async toggleTodoStatus(id, currentStatus) {
    try {
      await api.toggleTodo(id);
      await this.loadData();
      wx.showToast({ 
        title: currentStatus ? '已恢复' : '已完成', 
        icon: 'success',
        duration: 1500
      });
    } catch (error) {
      console.error('切换状态失败:', error);
      wx.showToast({ title: '操作失败，请稍后重试', icon: 'none' });
    }
  },

  onDeleteTodo(e) {
    const { id } = e.currentTarget.dataset;
    this.deleteTodo(id);
  },

  async deleteTodo(id) {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条任务吗？此操作不可恢复。',
      confirmColor: '#fc5c7d',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteTodo(id);
            await this.loadData();
            wx.showToast({ 
              title: '已删除', 
              icon: 'success',
              duration: 1500
            });
          } catch (error) {
            console.error('删除失败:', error);
            wx.showToast({ title: '删除失败，请稍后重试', icon: 'none' });
          }
        }
      }
    });
  },

  onTapTodo(e) {
    const { todo } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/detail/detail?id=${todo.id}`
    });
  }
});
