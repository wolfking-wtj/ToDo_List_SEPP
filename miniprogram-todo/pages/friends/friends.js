// pages/friends/friends.js
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    friends: [],
    heatmaps: [],
    loading: true,
    selectedFriendId: null,
    selectedHeatmap: null,
    showHeatmap: false,
    showAddFriendModal: false,
    showPostTaskModal: false,
    pendingTasks: [],
    searchKeyword: '',
    searchResults: [],
    currentMonth: '',
    heatmapDays: [],
    taskFormData: {
      title: '',
      desc: '',
      deadline: null
    },
    taskFormattedDeadline: '',
    showTaskCustomPicker: false,
    taskSelectedYear: 0,
    taskSelectedMonth: 0,
    taskSelectedDay: 0,
    taskSelectedHour: 0,
    taskSelectedMinute: 0,
    formError: ''
  },

  onLoad() {
    this.loadFriends();
    this.loadPendingTasks();
    this.updateCurrentMonth();
  },

  onShow() {
    this.loadFriends();
    this.loadPendingTasks();
  },

  updateCurrentMonth() {
    const now = new Date();
    this.setData({
      currentMonth: `${now.getFullYear()}年${now.getMonth() + 1}月`
    });
  },

  async loadFriends() {
    this.setData({ loading: true });
    
    try {
      const friends = await api.getFriends();
      const heatmaps = await api.getFriendsHeatmap();
      
      this.setData({
        friends: friends || [],
        heatmaps: heatmaps || [],
        loading: false
      });
    } catch (error) {
      console.error('加载好友失败:', error);
      this.setData({
        friends: [],
        heatmaps: [],
        loading: false
      });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  onTapFriend(e) {
    const { id } = e.currentTarget.dataset;
    const { currentMonth } = this.data;
    const [year, month] = currentMonth.match(/(\d+)年(\d+)月/).slice(1);
    this.showFriendHeatmap(id, year, month);
  },

  async showFriendHeatmap(friendId, year, month) {
    try {
      const heatmap = await api.getFriendHeatmap(friendId, year, month);
      
      const heatmapDays = this.generateHeatmapDays(heatmap.heatmapData || {}, heatmap.weekdayMap || {});
      
      const selectedHeatmap = {
        ...heatmap,
        friendName: heatmap.friendName || this.data.selectedHeatmap?.friendName,
        friendAvatar: heatmap.friendAvatar || this.data.selectedHeatmap?.friendAvatar
      };
      
      if (!this.data.selectedHeatmap || !this.data.selectedHeatmap.totalCompleted) {
        selectedHeatmap.totalCompleted = heatmap.totalCompleted;
        selectedHeatmap.activeDays = heatmap.activeDays;
        selectedHeatmap.maxCount = heatmap.maxCount;
      } else {
        selectedHeatmap.totalCompleted = this.data.selectedHeatmap.totalCompleted;
        selectedHeatmap.activeDays = this.data.selectedHeatmap.activeDays;
        selectedHeatmap.maxCount = this.data.selectedHeatmap.maxCount;
      }
      
      this.setData({
        selectedFriendId: friendId,
        selectedHeatmap: selectedHeatmap,
        heatmapDays: heatmapDays,
        showHeatmap: true
      });
    } catch (error) {
      console.error('加载热力图失败:', error);
      wx.showToast({ title: '加载热力图失败', icon: 'none' });
    }
  },

  generateHeatmapDays(heatmapData, weekdayMap) {
    const days = [];
    
    const sortedDates = Object.keys(heatmapData).sort();
    
    for (let i = 0; i < 42; i++) {
      const date = sortedDates[i] || '';
      const count = heatmapData[date] || 0;
      const intensity = this.calculateIntensity(count);
      const weekday = weekdayMap[date] || 0;
      
      let day = '';
      if (date) {
        const dateObj = new Date(date + 'T00:00:00');
        day = dateObj.getDate();
      }
      
      days.push({
        date: date,
        day: day,
        weekday: weekday,
        count: count,
        intensity: intensity
      });
    }
    
    return days;
  },

  calculateIntensity(count) {
    if (count === 0) return 0;
    if (count <= 2) return 1;
    if (count <= 4) return 2;
    if (count <= 6) return 3;
    return 4;
  },

  getWeekdayName(weekday) {
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    return weekdays[weekday - 1] || '';
  },

  onCloseHeatmap() {
    this.setData({
      showHeatmap: false,
      selectedFriendId: null,
      selectedHeatmap: null,
      heatmapDays: []
    });
  },

  onSearchInput(e) {
    const keyword = e.detail.value.trim();
    this.setData({ searchKeyword: keyword });
    
    if (keyword.length >= 2) {
      this.searchUsers(keyword);
    } else {
      this.setData({ searchResults: [] });
    }
  },

  onSearchClear() {
    this.setData({ searchKeyword: '', searchResults: [] });
  },

  async searchUsers(keyword) {
    try {
      console.log('开始搜索用户，关键字:', keyword);
      const results = await api.searchUsers(keyword);
      console.log('搜索结果:', results);
      this.setData({ searchResults: results || [] });
    } catch (error) {
      console.error('搜索用户失败:', error);
    }
  },

  async onTapUser(e) {
    const { user } = e.currentTarget.dataset;
    
    if (user.isFriend) {
      wx.showToast({ title: '已经是好友', icon: 'none' });
      return;
    }
    
    try {
      await api.addFriend(user.id);
      
      wx.showToast({ title: '添加成功', icon: 'success' });
      
      this.setData({
        showAddFriendModal: false,
        searchKeyword: '',
        searchResults: []
      });
      
      await this.loadFriends();
    } catch (error) {
      console.error('添加好友失败:', error);
      wx.showToast({ title: error.message || '添加失败', icon: 'none' });
    }
  },

  onOpenAddFriend() {
    this.setData({ showAddFriendModal: true, searchKeyword: '', searchResults: [] });
  },

  onCloseAddFriend() {
    this.setData({ showAddFriendModal: false, searchKeyword: '', searchResults: [] });
  },

  onAddFriend() {
    this.onOpenAddFriend();
  },

  onViewPendingTasks() {
    console.log('点击了查看按钮');
    wx.navigateTo({
      url: '/pages/task-accept/task-accept',
      success: function(res) {
        console.log('导航成功', res);
      },
      fail: function(res) {
        console.log('导航失败', res);
      }
    });
  },

  async loadPendingTasks() {
    try {
      console.log('开始加载待接收任务');
      const pendingTasks = await api.getPendingTasks();
      console.log('待接收任务:', pendingTasks);
      this.setData({ pendingTasks });
    } catch (error) {
      console.error('加载待接收任务失败:', error);
    }
  },

  onPostTask(e) {
    const { friend } = e.currentTarget.dataset;
    
    this.setData({
      showPostTaskModal: true,
      selectedFriend: friend,
      taskFormData: {
        title: '',
        desc: '',
        deadline: null
      },
      taskFormattedDeadline: '',
      formError: ''
    });
    
    const now = new Date();
    this.initTaskPickerData(now);
  },

  onClosePostTask() {
    this.setData({
      showPostTaskModal: false,
      selectedFriend: null,
      taskFormData: {
        title: '',
        desc: '',
        deadline: null
      },
      taskFormattedDeadline: '',
      showTaskCustomPicker: false,
      formError: ''
    });
  },

  onTaskTitleInput(e) {
    this.setData({ 'taskFormData.title': e.detail.value, formError: '' });
  },

  onTaskDescInput(e) {
    this.setData({ 'taskFormData.desc': e.detail.value });
  },

  showTaskDeadlinePicker() {
    const now = new Date();
    let selectedDate = now;
    
    if (this.data.taskFormData.deadline) {
      selectedDate = new Date(this.data.taskFormData.deadline);
    }
    
    this.initTaskPickerData(selectedDate);
    
    this.setData({
      showTaskCustomPicker: true
    });
  },

  initTaskPickerData(selectedDate) {
    const now = new Date();
    const currentYear = selectedDate.getFullYear();
    const currentMonth = selectedDate.getMonth() + 1;
    const currentDay = selectedDate.getDate();
    const currentHour = selectedDate.getHours();
    const currentMinute = selectedDate.getMinutes();
    
    const years = [];
    for (let i = now.getFullYear(); i <= now.getFullYear() + 1; i++) {
      years.push(i);
    }
    
    const months = [];
    for (let i = 1; i <= 12; i++) {
      months.push(i);
    }
    
    const hours = [];
    for (let i = 0; i < 24; i++) {
      hours.push(i);
    }
    
    const minutes = [];
    for (let i = 0; i < 60; i += 5) {
      minutes.push(i);
    }
    
    const days = this.generateTaskDays(currentYear, currentMonth);
    
    this.setData({
      years: years,
      months: months,
      days: days,
      hours: hours,
      minutes: minutes,
      taskSelectedYear: currentYear,
      taskSelectedMonth: currentMonth,
      taskSelectedDay: currentDay,
      taskSelectedHour: currentHour,
      taskSelectedMinute: currentMinute
    });
  },

  generateTaskDays(year, month) {
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    return days;
  },

  selectTaskYear(e) {
    const year = e.currentTarget.dataset.year;
    this.setData({
      taskSelectedYear: year
    });
    this.updateTaskDays();
  },

  selectTaskMonth(e) {
    const month = e.currentTarget.dataset.month;
    this.setData({
      taskSelectedMonth: month
    });
    this.updateTaskDays();
  },

  updateTaskDays() {
    const days = this.generateTaskDays(this.data.taskSelectedYear, this.data.taskSelectedMonth);
    let selectedDay = this.data.taskSelectedDay;
    if (selectedDay > days.length) {
      selectedDay = 1;
    }
    this.setData({
      days: days,
      taskSelectedDay: selectedDay
    });
  },

  selectTaskDay(e) {
    const day = e.currentTarget.dataset.day;
    this.setData({
      taskSelectedDay: day
    });
  },

  selectTaskHour(e) {
    const hour = e.currentTarget.dataset.hour;
    this.setData({
      taskSelectedHour: hour
    });
  },

  selectTaskMinute(e) {
    const minute = e.currentTarget.dataset.minute;
    this.setData({
      taskSelectedMinute: minute
    });
  },

  cancelTaskDeadlinePicker() {
    this.setData({
      showTaskCustomPicker: false
    });
  },

  confirmTaskDeadlinePicker() {
    const { taskSelectedYear, taskSelectedMonth, taskSelectedDay, taskSelectedHour, taskSelectedMinute } = this.data;
    
    const deadline = new Date(taskSelectedYear, taskSelectedMonth - 1, taskSelectedDay, taskSelectedHour, taskSelectedMinute).getTime();
    const taskFormattedDeadline = helper.formatDeadline(deadline);
    
    this.setData({ 
      'taskFormData.deadline': deadline,
      taskFormattedDeadline: taskFormattedDeadline,
      showTaskCustomPicker: false
    });
  },

  onClearTaskDeadline() {
    this.setData({ 
      'taskFormData.deadline': null,
      taskFormattedDeadline: ''
    });
  },

  async onSubmitPostTask() {
    const { taskFormData, selectedFriend } = this.data;

    if (!taskFormData.title.trim()) {
      this.setData({ formError: '请输入任务标题' });
      return;
    }
    if (taskFormData.title.trim().length > 50) {
      this.setData({ formError: '标题不能超过 50 个字符' });
      return;
    }

    try {
      const newTask = {
        title: taskFormData.title.trim(),
        desc: taskFormData.desc.trim(),
        deadline: taskFormData.deadline,
        targetUserId: selectedFriend.id
      };
      await api.postTaskToFriend(newTask);
      wx.showToast({ title: '任务发布成功 🎉', icon: 'success', duration: 1500 });
      this.onClosePostTask();
    } catch (error) {
      console.error('发布任务失败:', error);
      wx.showToast({ title: '发布失败，请稍后重试', icon: 'none' });
    }
  },

  onPrevMonth() {
    let { currentMonth } = this.data;
    const [year, month] = currentMonth.match(/(\d+)年(\d+)月/).slice(1);
    let monthNum = parseInt(month) - 1;
    if (monthNum < 1) {
      monthNum = 12;
      currentMonth = `${parseInt(year) - 1}年${monthNum}月`;
    } else {
      currentMonth = `${year}年${monthNum}月`;
    }
    this.setData({ currentMonth });
    this.updateFriendHeatmapData(year, monthNum);
  },

  onNextMonth() {
    let { currentMonth } = this.data;
    const [year, month] = currentMonth.match(/(\d+)年(\d+)月/).slice(1);
    let monthNum = parseInt(month) + 1;
    if (monthNum > 12) {
      monthNum = 1;
      currentMonth = `${parseInt(year) + 1}年${monthNum}月`;
    } else {
      currentMonth = `${year}年${monthNum}月`;
    }
    this.setData({ currentMonth });
    this.updateFriendHeatmapData(year, monthNum);
  },

  updateFriendHeatmapData(year, month) {
    const { selectedFriendId, showHeatmap } = this.data;
    if (selectedFriendId && showHeatmap) {
      this.getFriendHeatmapData(selectedFriendId, year, month);
    }
  },

  async getFriendHeatmapData(friendId, year, month) {
    try {
      const heatmap = await api.getFriendHeatmap(friendId, year, month);
      
      const heatmapDays = this.generateHeatmapDays(heatmap.heatmapData || {}, heatmap.weekdayMap || {});
      
      this.setData({
        'selectedHeatmap.heatmapData': heatmap.heatmapData,
        'selectedHeatmap.weekdayMap': heatmap.weekdayMap,
        heatmapDays: heatmapDays
      });
    } catch (error) {
      console.error('加载热力图失败:', error);
      wx.showToast({ title: '加载热力图失败', icon: 'none' });
    }
  },
  
  async onDeleteFriend() {
    const { selectedFriendId } = this.data;
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这位好友吗？',
      confirmColor: '#fc5c7d',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteFriend(selectedFriendId);
            
            wx.showToast({ 
              title: '已删除', 
              icon: 'success',
              duration: 1500
            });
            
            this.onCloseHeatmap();
            await this.loadFriends();
          } catch (error) {
            console.error('删除好友失败:', error);
            wx.showToast({ title: '删除失败', icon: 'none' });
          }
        }
      }
    });
  }
});
