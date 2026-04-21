// pages/calendar/calendar.js
const api = require('../../utils/api');

Page({
  data: {
    // 日历数据
    calendarData: {
      year: 2026,
      month: 4,
      days: []
    },
    // 任务数据
    tasks: [],
    // 每天的完成任务数量
    completedTasksByDate: {},
    // 今日任务（截至日期是今天的任务）
    todayTasks: []
  },

  onLoad() {
    this.initCalendar();
  },

  // 每次进入页面时自动刷新
  onShow() {
    this.initCalendar();
  },

  // 初始化日历数据
  async initCalendar() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    
    await this.loadTasks(year, month);
    
    this.setData({
      calendarData: {
        year: year,
        month: month,
        days: this.generateCalendarDays(year, month)
      }
    });
  },

  // 加载任务数据
  async loadTasks(year, month) {
    try {
      const tasks = await api.getAllTodos();
      const completedTasksByDate = this.calculateCompletedTasksByDate(tasks, year, month);
      const todayTasks = this.getTodayTasks(tasks);
      
      this.setData({
        tasks: tasks,
        completedTasksByDate: completedTasksByDate,
        todayTasks: todayTasks
      });
    } catch (error) {
      console.error('加载任务失败:', error);
      this.setData({
        tasks: [],
        completedTasksByDate: {},
        todayTasks: []
      });
    }
  },

  // 获取今日任务（截至日期是今天的任务）
  getTodayTasks(tasks) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    return tasks.filter(task => {
      if (!task.deadline) return false;
      const deadline = new Date(task.deadline);
      deadline.setHours(0, 0, 0, 0);
      return deadline >= today && deadline < tomorrow;
    });
  },

  // 计算每天的完成任务数量
  calculateCompletedTasksByDate(tasks, year, month) {
    const result = {};
    
    tasks.forEach(task => {
      if (task.completed && task.completedAt) {
        const completedDate = new Date(task.completedAt);
        const taskYear = completedDate.getFullYear();
        const taskMonth = completedDate.getMonth() + 1;
        
        // 只统计当前年月的任务
        if (taskYear === year && taskMonth === month) {
          const day = completedDate.getDate();
          const dateKey = day.toString();
          result[dateKey] = (result[dateKey] || 0) + 1;
        }
      }
    });
    
    return result;
  },

  // 生成日历天数
  generateCalendarDays(year, month) {
    const days = [];
    const daysInMonth = new Date(year, month, 0).getDate();
    const firstDayOfMonth = new Date(year, month - 1, 1).getDay();
    const { completedTasksByDate } = this.data;
    
    // 填充前面的空白
    for (let i = 0; i < firstDayOfMonth; i++) {
      days.push({ day: '', isEmpty: true });
    }
    
    // 填充日期
    for (let i = 1; i <= daysInMonth; i++) {
      const dateKey = i.toString();
      const completedCount = completedTasksByDate[dateKey] || 0;
      const intensity = this.calculateIntensity(completedCount);
      
      days.push({
        day: i,
        isEmpty: false,
        isToday: new Date().getDate() === i && new Date().getMonth() + 1 === month && new Date().getFullYear() === year,
        completedCount: completedCount,
        intensity: intensity
      });
    }
    
    return days;
  },

  // 计算颜色深度
  calculateIntensity(completedCount) {
    if (completedCount === 0) return 0;
    if (completedCount === 1) return 1;
    if (completedCount === 2) return 2;
    if (completedCount === 3) return 3;
    return 4; // 4个及以上任务
  },

  // 切换月份
  async onPrevMonth() {
    let { year, month } = this.data.calendarData;
    month--;
    if (month < 1) {
      month = 12;
      year--;
    }
    
    await this.loadTasks(year, month);
    
    this.setData({
      calendarData: {
        year: year,
        month: month,
        days: this.generateCalendarDays(year, month)
      }
    });
  },

  async onNextMonth() {
    let { year, month } = this.data.calendarData;
    month++;
    if (month > 12) {
      month = 1;
      year++;
    }
    
    await this.loadTasks(year, month);
    
    this.setData({
      calendarData: {
        year: year,
        month: month,
        days: this.generateCalendarDays(year, month)
      }
    });
  },

  // 点击日期
  onTapDate(e) {
    const { day, completedCount } = e.currentTarget.dataset;
    if (!day) return;
    wx.showToast({ 
      title: `完成了 ${completedCount || 0} 个任务`, 
      icon: 'none' 
    });
  }
});