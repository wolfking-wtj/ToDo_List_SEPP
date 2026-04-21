// app.js - 小程序入口文件
App({
  /**
   * 小程序初始化时执行
   * 全局数据和工具函数在此挂载
   */
  onLaunch() {
    console.log('Todo App 启动');
    // 初始化本地存储（首次启动创建示例数据）
    this.initStorage();
  },

  /**
   * 初始化存储：首次启动写入演示数据
   */
  initStorage() {
    const todos = wx.getStorageSync('todos');
    if (!todos) {
      const demoTodos = [
        {
          id: this.generateId(),
          title: '阅读《代码整洁之道》',
          desc: '重点关注命名规范和函数职责单一原则',
          priority: 'high',
          completed: false,
          createdAt: Date.now() - 86400000 * 2,
          dueDate: ''
        },
        {
          id: this.generateId(),
          title: '完成团队代码评审',
          desc: '本周 PR 的代码审查，关注架构设计',
          priority: 'high',
          completed: false,
          createdAt: Date.now() - 86400000,
          dueDate: ''
        },
        {
          id: this.generateId(),
          title: '重构用户模块',
          desc: '提取公共逻辑到 utils，降低耦合度',
          priority: 'medium',
          completed: false,
          createdAt: Date.now() - 3600000 * 3,
          dueDate: ''
        },
        {
          id: this.generateId(),
          title: '搭建单元测试框架',
          desc: '引入 Jest，目标覆盖率 80%+',
          priority: 'medium',
          completed: true,
          createdAt: Date.now() - 86400000 * 5,
          dueDate: ''
        },
        {
          id: this.generateId(),
          title: '编写 API 接口文档',
          desc: '使用 Swagger 自动生成，同步给前端团队',
          priority: 'low',
          completed: true,
          createdAt: Date.now() - 86400000 * 7,
          dueDate: ''
        }
      ];
      wx.setStorageSync('todos', demoTodos);
    }
  },

  /**
   * 生成唯一 ID（时间戳 + 随机数）
   * @returns {string}
   */
  generateId() {
    return `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
  },

  globalData: {
    userInfo: null
  }
});
