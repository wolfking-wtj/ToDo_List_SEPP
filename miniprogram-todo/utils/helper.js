// utils/helper.js - 通用辅助函数

/**
 * 生成唯一 ID
 * @returns {string}
 */
function generateId() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
}

/**
 * 格式化时间戳
 * @param {number} timestamp
 * @returns {string} 如 "今天 14:30" / "昨天" / "3天前" / "04-01"
 */
function formatTime(timestamp) {
  if (!timestamp) return '';
  const now = new Date();
  const date = new Date(timestamp);
  const diffMs = now - date;
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffDays === 0) {
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    return `今天 ${h}:${m}`;
  }
  if (diffDays === 1) return '昨天';
  if (diffDays < 7) return `${diffDays}天前`;

  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}-${day}`;
}

/**
 * 优先级配置
 */
const PRIORITY_CONFIG = {
  high: { label: '紧急', color: '#fc5c7d', bg: '#fff5f7', icon: '🔥' },
  medium: { label: '普通', color: '#ed8936', bg: '#fffaf0', icon: '⚡' },
  low: { label: '轻松', color: '#48bb78', bg: '#f0fff4', icon: '🌿' }
};

/**
 * 获取优先级展示配置
 * @param {string} priority
 * @returns {Object}
 */
function getPriorityConfig(priority) {
  return PRIORITY_CONFIG[priority] || PRIORITY_CONFIG.medium;
}

/**
 * 统计 Todo 数据
 * @param {Array} todos
 * @returns {Object}
 */
function calcStats(todos) {
  const total = todos.length;
  const completed = todos.filter(t => t.completed).length;
  const pending = total - completed;
  const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
  return { total, completed, pending, percent };
}

/**
 * 格式化截至时间
 * @param {number} timestamp
 * @returns {string} 如 "今天 14:30" / "明天 10:00" / "后天 15:30" / "04-01 14:30"
 */
function formatDeadline(timestamp) {
  if (!timestamp) return '';
  const now = new Date();
  const date = new Date(timestamp);
  const diffMs = date - now;
  const diffDays = Math.floor(diffMs / 86400000);

  const h = date.getHours().toString().padStart(2, '0');
  const m = date.getMinutes().toString().padStart(2, '0');
  const timeStr = `${h}:${m}`;

  if (diffDays === 0) {
    return `今天 ${timeStr}`;
  }
  if (diffDays === 1) {
    return `明天 ${timeStr}`;
  }
  if (diffDays === 2) {
    return `后天 ${timeStr}`;
  }
  if (diffDays < 7) {
    const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const dayName = dayNames[date.getDay()];
    return `${dayName} ${timeStr}`;
  }

  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}-${day} ${timeStr}`;
}

module.exports = {
  generateId,
  formatTime,
  formatDeadline,
  getPriorityConfig,
  calcStats,
  PRIORITY_CONFIG
};
