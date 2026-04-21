// pages/index/index.js - 主页面逻辑
const api = require('../../utils/api');
const helper = require('../../utils/helper');

Page({
  data: {
    // 辅助函数
    helper: require('../../utils/helper'),
    // Todo 数据
    allTodos: [],         // 完整数据（用于统计）
    filteredTodos: [],    // 当前筛选后展示的数据
    stats: { total: 0, completed: 0, pending: 0, percent: 0 },

    // 筛选状态：'all' | 'pending' | 'completed'
    filter: 'all',
    filterTabs: [
      { key: 'all', label: '全部' },
      { key: 'pending', label: '进行中' },
      { key: 'completed', label: '已完成' }
    ],

    // 排序状态：'newest' | 'oldest'
    sortBy: 'newest',
    sortOptions: [
      { key: 'newest', label: '最新', icon: '⏰' },
      { key: 'oldest', label: '最早', icon: '🕰️' }
    ],

    // 搜索
    searchKeyword: '',

    // 弹窗状态
    showAddModal: false,
    showDeleteConfirm: false,
    deletingId: '',
    formattedDeadline: '',
    // 自定义日期时间选择器
    showCustomPicker: false,
    years: [],
    months: [],
    days: [],
    hours: [],
    minutes: [],
    selectedYear: 0,
    selectedMonth: 0,
    selectedDay: 0,
    selectedHour: 0,
    selectedMinute: 0,

    // 新增/编辑表单
    formMode: 'add',      // 'add' | 'edit'
    formData: {
      id: '',
      title: '',
      desc: '',
      priority: 'medium',
      deadline: null
    },
    formError: '',

    // 优先级选项
    priorityOptions: [
      { key: 'high', label: '🔥 紧急', color: '#fc5c7d' },
      { key: 'medium', label: '⚡ 普通', color: '#ed8936' },
      { key: 'low', label: '🌿 轻松', color: '#48bb78' }
    ],

    // 动画控制：正在执行 toggle 动画的 id 集合（用数组模拟 Set）
    animatingIds: []
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    // 每次页面显示时刷新（从详情页返回后同步数据）
    this.loadData();
  },

  // ===== 数据层 =====

  /**
   * 从后端API加载并刷新页面数据
   */
  async loadData() {
    try {
      const allTodos = await api.getAllTodos();
      const stats = helper.calcStats(allTodos);
      const filteredTodos = this._filterTodos(allTodos);
      this.setData({ allTodos, stats, filteredTodos });
    } catch (error) {
      console.error('加载数据失败:', error);
      wx.showToast({ title: '加载失败，请稍后重试', icon: 'none' });
    }
  },

  /**
   * 根据当前筛选条件和关键词过滤数据
   * @param {Array} todos
   * @returns {Array}
   */
  _filterTodos(todos) {
    const { filter, searchKeyword, sortBy } = this.data;
    let result = todos;

    // 状态筛选
    if (filter === 'pending') result = result.filter(t => !t.completed);
    else if (filter === 'completed') result = result.filter(t => t.completed);

    // 关键词搜索（标题 + 描述）
    if (searchKeyword.trim()) {
      const kw = searchKeyword.trim().toLowerCase();
      result = result.filter(t =>
        t.title.toLowerCase().includes(kw) ||
        (t.desc && t.desc.toLowerCase().includes(kw))
      );
    }

    // 时间排序
    result.sort((a, b) => {
      if (sortBy === 'newest') {
        return b.createdAt - a.createdAt;
      } else {
        return a.createdAt - b.createdAt;
      }
    });

    // 附加展示字段（格式化时间、优先级配置）
    return result.map(t => ({
      ...t,
      formattedTime: helper.formatTime(t.createdAt),
      formattedDeadline: helper.formatDeadline(t.deadline),
      priorityConfig: helper.getPriorityConfig(t.priority)
    }));
  },

  // ===== 筛选 & 搜索 =====

  onFilterChange(e) {
    const filter = e.currentTarget.dataset.key;
    const filteredTodos = this._filterTodos(this.data.allTodos);
    this.setData({ filter, filteredTodos: [] }, () => {
      // 先清空再赋值，触发列表动画
      this.setData({
        filteredTodos: this._filterTodos(this.data.allTodos)
      });
    });
  },

  onSearchInput(e) {
    const searchKeyword = e.detail.value;
    this.setData({ searchKeyword }, () => {
      this.setData({
        filteredTodos: this._filterTodos(this.data.allTodos)
      });
    });
  },

  onSearchClear() {
    this.setData({ searchKeyword: '' }, () => {
      this.setData({
        filteredTodos: this._filterTodos(this.data.allTodos)
      });
    });
  },

  onSortChange(e) {
    const sortBy = e.currentTarget.dataset.key;
    this.setData({ sortBy }, () => {
      this.setData({
        filteredTodos: this._filterTodos(this.data.allTodos)
      });
    });
  },

  // ===== Todo 操作 =====

  /**
   * 切换完成状态（带弹跳动画）
   */
  async onToggleTodo(e) {
    const { id } = e.currentTarget.dataset;
    const { animatingIds } = this.data;

    // 防抖：动画中不重复触发
    if (animatingIds.includes(id)) return;

    this.setData({ animatingIds: [...animatingIds, id] });

    try {
      // 执行数据更新
      await api.toggleTodo(id);
      // 重新加载数据
      await this.loadData();
    } catch (error) {
      console.error('切换状态失败:', error);
      wx.showToast({ title: '操作失败，请稍后重试', icon: 'none' });
    }

    // 动画结束后清除
    setTimeout(() => {
      this.setData({
        animatingIds: this.data.animatingIds.filter(aid => aid !== id)
      });
    }, 400);
  },

  /**
   * 进入编辑模式
   */
  onEditTodo(e) {
    const { id } = e.currentTarget.dataset;
    const todo = this.data.allTodos.find(t => t.id === id);
    if (!todo) return;
    this.setData({
      showAddModal: true,
      formMode: 'edit',
      formData: {
        id: todo.id,
        title: todo.title,
        desc: todo.desc || '',
        priority: todo.priority || 'medium',
        deadline: todo.deadline || null
      },
      formattedDeadline: todo.deadline ? helper.formatDeadline(todo.deadline) : '',
      formError: ''
    });
  },

  /**
   * 触发删除确认
   */
  onDeleteTodo(e) {
    const { id } = e.currentTarget.dataset;
    this.setData({ showDeleteConfirm: true, deletingId: id });
  },

  /**
   * 确认删除
   */
  async onConfirmDelete() {
    try {
      await api.deleteTodo(this.data.deletingId);
      // 重新加载数据
      await this.loadData();
      this.setData({
        showDeleteConfirm: false,
        deletingId: ''
      });
      wx.showToast({ title: '已删除', icon: 'success', duration: 1500 });
    } catch (error) {
      console.error('删除失败:', error);
      wx.showToast({ title: '删除失败，请稍后重试', icon: 'none' });
    }
  },

  onCancelDelete() {
    this.setData({ showDeleteConfirm: false, deletingId: '' });
  },

  /**
   * 清除已完成
   */
  onClearCompleted() {
    if (this.data.stats.completed === 0) {
      wx.showToast({ title: '暂无已完成任务', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '清除确认',
      content: `将删除全部 ${this.data.stats.completed} 条已完成任务，不可恢复`,
      confirmText: '确认清除',
      confirmColor: '#fc5c7d',
      success: async (res) => {
        if (res.confirm) {
          try {
            // 获取所有已完成的待办事项
            const completedTodos = await api.getCompletedTodos();
            // 逐个删除
            for (const todo of completedTodos) {
              await api.deleteTodo(todo.id);
            }
            // 重新加载数据
            await this.loadData();
            wx.showToast({ title: '清除成功', icon: 'success' });
          } catch (error) {
            console.error('清除失败:', error);
            wx.showToast({ title: '清除失败，请稍后重试', icon: 'none' });
          }
        }
      }
    });
  },

  // ===== 新增/编辑弹窗 =====

  onShowAddModal() {
    this.setData({
      showAddModal: true,
      formMode: 'add',
      formData: { id: '', title: '', desc: '', priority: 'medium' },
      formError: ''
    });
  },

  onCloseModal() {
    this.setData({ showAddModal: false, formError: '' });
  },

  onFormTitleInput(e) {
    this.setData({ 'formData.title': e.detail.value, formError: '' });
  },

  onFormDescInput(e) {
    this.setData({ 'formData.desc': e.detail.value });
  },

  onSelectPriority(e) {
    this.setData({ 'formData.priority': e.currentTarget.dataset.key });
  },

  // 显示自定义日期时间选择器
  showDeadlinePicker() {
    const now = new Date();
    let selectedDate = now;
    
    if (this.data.formData.deadline) {
      selectedDate = new Date(this.data.formData.deadline);
    }
    
    // 初始化选择器数据
    this.initPickerData(selectedDate);
    
    this.setData({
      showCustomPicker: true
    });
  },

  // 初始化选择器数据
  initPickerData(selectedDate) {
    const now = new Date();
    const currentYear = selectedDate.getFullYear();
    const currentMonth = selectedDate.getMonth() + 1;
    const currentDay = selectedDate.getDate();
    const currentHour = selectedDate.getHours();
    const currentMinute = selectedDate.getMinutes();
    
    // 生成年份数组（当前年到后一年）
    const years = [];
    for (let i = now.getFullYear(); i <= now.getFullYear() + 1; i++) {
      years.push(i);
    }
    
    // 生成月份数组
    const months = [];
    for (let i = 1; i <= 12; i++) {
      months.push(i);
    }
    
    // 生成小时数组
    const hours = [];
    for (let i = 0; i < 24; i++) {
      hours.push(i);
    }
    
    // 生成分钟数组（每5分钟一个选项）
    const minutes = [];
    for (let i = 0; i < 60; i += 5) {
      minutes.push(i);
    }
    
    // 生成日期数组（根据年月动态生成）
    const days = this.generateDays(currentYear, currentMonth);
    
    this.setData({
      years: years,
      months: months,
      days: days,
      hours: hours,
      minutes: minutes,
      selectedYear: currentYear,
      selectedMonth: currentMonth,
      selectedDay: currentDay,
      selectedHour: currentHour,
      selectedMinute: currentMinute
    });
  },

  // 根据年月生成日期数组
  generateDays(year, month) {
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    return days;
  },

  // 选择年份
  selectYear(e) {
    const year = e.currentTarget.dataset.year;
    this.setData({
      selectedYear: year
    });
    // 更新日期数组
    this.updateDays();
  },

  // 选择月份
  selectMonth(e) {
    const month = e.currentTarget.dataset.month;
    this.setData({
      selectedMonth: month
    });
    // 更新日期数组
    this.updateDays();
  },

  // 更新日期数组
  updateDays() {
    const days = this.generateDays(this.data.selectedYear, this.data.selectedMonth);
    // 如果当前选择的日期超过了当月的天数，重置为1
    let selectedDay = this.data.selectedDay;
    if (selectedDay > days.length) {
      selectedDay = 1;
    }
    this.setData({
      days: days,
      selectedDay: selectedDay
    });
  },

  // 选择日期
  selectDay(e) {
    const day = e.currentTarget.dataset.day;
    this.setData({
      selectedDay: day
    });
  },

  // 选择小时
  selectHour(e) {
    const hour = e.currentTarget.dataset.hour;
    this.setData({
      selectedHour: hour
    });
  },

  // 选择分钟
  selectMinute(e) {
    const minute = e.currentTarget.dataset.minute;
    this.setData({
      selectedMinute: minute
    });
  },

  // 取消选择
  cancelDeadlinePicker() {
    this.setData({
      showCustomPicker: false
    });
  },

  // 确认选择
  confirmDeadlinePicker() {
    const { selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute } = this.data;
    
    // 转换为时间戳
    const deadline = new Date(selectedYear, selectedMonth - 1, selectedDay, selectedHour, selectedMinute).getTime();
    const formattedDeadline = helper.formatDeadline(deadline);
    
    this.setData({ 
      'formData.deadline': deadline,
      formattedDeadline: formattedDeadline,
      showCustomPicker: false
    });
  },

  onClearDeadline() {
    this.setData({ 
      'formData.deadline': null,
      formattedDeadline: ''
    });
  },

  /**
   * 提交表单（新增或编辑）
   */
  async onSubmitForm() {
    const { formData, formMode } = this.data;

    // 表单校验
    if (!formData.title.trim()) {
      this.setData({ formError: '请输入任务标题' });
      return;
    }
    if (formData.title.trim().length > 50) {
      this.setData({ formError: '标题不能超过 50 个字符' });
      return;
    }

    try {
      if (formMode === 'add') {
        const newTodo = {
          title: formData.title.trim(),
          desc: formData.desc.trim(),
          priority: formData.priority,
          deadline: formData.deadline
        };
        await api.createTodo(newTodo);
        wx.showToast({ title: '添加成功 🎉', icon: 'none', duration: 1500 });
      } else {
        const updatedTodo = {
          title: formData.title.trim(),
          desc: formData.desc.trim(),
          priority: formData.priority,
          deadline: formData.deadline
        };
        await api.updateTodo(Number(formData.id), updatedTodo);
        wx.showToast({ title: '更新成功 ✅', icon: 'none', duration: 1500 });
      }
      
      await this.loadData();
      this.setData({ showAddModal: false });
    } catch (error) {
      console.error('操作失败:', error);
      wx.showToast({ title: '操作失败，请稍后重试', icon: 'none' });
    }
  },

  // ===== 跳转详情 =====

  onTapDetail(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` });
  },

  // ===== 下拉刷新 =====

  onPullDownRefresh() {
    this.loadData();
    wx.stopPullDownRefresh();
    wx.showToast({ title: '已刷新', icon: 'success', duration: 800 });
  }
});
