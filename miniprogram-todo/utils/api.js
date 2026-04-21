// utils/api.js - API 通信工具封装
// 职责：与后端 API 进行通信，处理认证和数据传输

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * 安全解析日期为时间戳
 * @param {string|number} dateValue - 日期值
 * @returns {number|null} - 时间戳或 null
 */
function safeParseDate(dateValue) {
  if (!dateValue) return null;
  const date = new Date(dateValue);
  return isNaN(date.getTime()) ? null : date.getTime();
}

/**
 * 将后端 Todo 数据转换为前端格式
 * @param {Object} todo - 后端返回的 Todo 对象
 * @returns {Object} - 前端使用的 Todo 对象
 */
function transformTodoFromBackend(todo) {
  return {
    ...todo,
    createdAt: safeParseDate(todo.createdAt),
    dueDate: safeParseDate(todo.dueDate),
    completedAt: safeParseDate(todo.completedAt),
    deadline: safeParseDate(todo.dueDate)
  };
}

/**
 * 将前端 Todo 数据转换为后端格式
 * @param {Object} todo - 前端的 Todo 对象
 * @returns {Object} - 后端使用的 Todo 对象
 */
function transformTodoToBackend(todo) {
  return {
    ...todo,
    dueDate: todo.deadline ? new Date(todo.deadline).toISOString() : null
  };
}

/**
 * 从响应中提取数据（支持包装格式 {code, data, message} 和直接数组格式）
 * @param {Object} res - wx.request 响应对象
 * @returns {any} - 提取的数据
 */
function extractData(res) {
  const data = res.data;
  // 如果是包装格式，提取 data 字段
  if (data && typeof data === 'object' && 'data' in data) {
    return data.data;
  }
  return data;
}

/**
 * 封装 wx.request，添加认证 token
 * @param {Object} options - 请求选项
 * @param {boolean} requireAuth - 是否需要认证（默认 true）
 * @returns {Promise}
 */
function request(options, requireAuth = true) {
  const token = wx.getStorageSync('token');
  const header = {
    'Content-Type': 'application/json',
    ...options.header
  };
  
  if (requireAuth && token) {
    header['Authorization'] = `Bearer ${token}`;
  }
  
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: API_BASE_URL + options.url,
      header: header,
      success: (res) => {
        if (res.statusCode === 401) {
          wx.redirectTo({ url: '/pages/login/login' });
          reject(new Error('未认证'));
        } else if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(extractData(res));
        } else {
          const errorMsg = res.data?.message || `请求失败: ${res.statusCode}`;
          reject(new Error(errorMsg));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

/**
 * 用户注册
 * @param {Object} user - 用户信息
 * @returns {Promise}
 */
function register(user) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data: user
  }, false);
}

/**
 * 用户登录
 * @param {Object} credentials - 登录凭证
 * @returns {Promise}
 */
function login(credentials) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data: credentials
  }, false);
}

/**
 * 获取所有待办事项
 * @returns {Promise}
 */
async function getAllTodos() {
  const todos = await request({
    url: '/todos',
    method: 'GET'
  });
  return Array.isArray(todos) ? todos.map(transformTodoFromBackend) : [];
}

/**
 * 创建待办事项
 * @param {Object} todo - 待办事项信息
 * @returns {Promise}
 */
async function createTodo(todo) {
  const backendTodo = transformTodoToBackend(todo);
  const createdTodo = await request({
    url: '/todos',
    method: 'POST',
    data: backendTodo
  });
  return transformTodoFromBackend(createdTodo);
}

/**
 * 更新待办事项
 * @param {number} id - 待办事项 ID
 * @param {Object} todo - 待办事项信息
 * @returns {Promise}
 */
async function updateTodo(id, todo) {
  const backendTodo = transformTodoToBackend(todo);
  const updatedTodo = await request({
    url: `/todos/${id}`,
    method: 'PUT',
    data: backendTodo
  });
  return transformTodoFromBackend(updatedTodo);
}

/**
 * 删除待办事项
 * @param {number} id - 待办事项 ID
 * @returns {Promise}
 */
function deleteTodo(id) {
  return request({
    url: `/todos/${id}`,
    method: 'DELETE'
  });
}

/**
 * 切换待办事项状态
 * @param {number} id - 待办事项 ID
 * @returns {Promise}
 */
async function toggleTodo(id) {
  const updatedTodo = await request({
    url: `/todos/${id}/toggle`,
    method: 'PUT'
  });
  return transformTodoFromBackend(updatedTodo);
}

/**
 * 获取已完成的待办事项
 * @returns {Promise}
 */
async function getCompletedTodos() {
  const todos = await request({
    url: '/todos/completed',
    method: 'GET'
  });
  return Array.isArray(todos) ? todos.map(transformTodoFromBackend) : [];
}

/**
 * 获取所有待办事项（包括已删除的）
 * @returns {Promise}
 */
async function getAllTodosWithDeleted() {
  const todos = await request({
    url: '/todos/all',
    method: 'GET'
  });
  return Array.isArray(todos) ? todos.map(transformTodoFromBackend) : [];
}

/**
 * 获取未完成的待办事项
 * @returns {Promise}
 */
async function getPendingTodos() {
  const todos = await request({
    url: '/todos/pending',
    method: 'GET'
  });
  return Array.isArray(todos) ? todos.map(transformTodoFromBackend) : [];
}

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
async function getCurrentUser() {
  return await request({
    url: '/auth/me',
    method: 'GET'
  });
}

/**
 * 更新当前用户信息
 * @param {Object} userUpdate - 用户更新信息
 * @returns {Promise}
 */
async function updateCurrentUser(userUpdate) {
  return await request({
    url: '/auth/me',
    method: 'PUT',
    data: userUpdate
  });
}

/**
 * 上传头像
 * @param {string} filePath - 本地文件路径
 * @returns {Promise}
 */
function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + '/auth/upload-avatar',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        try {
          console.log('上传头像响应:', res);
          const data = JSON.parse(res.data);
          console.log('解析后的数据:', data);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            if (data.avatarUrl) {
              resolve(data);
            } else {
              reject(new Error('上传失败：未返回头像URL'));
            }
          } else {
            reject(new Error('上传失败：' + (data?.message || '服务器错误')));
          }
        } catch (e) {
          console.error('响应解析错误:', e);
          reject(new Error('上传失败：响应解析错误'));
        }
      },
      fail: (error) => {
        console.error('上传失败:', error);
        reject(new Error('上传失败：' + (error.errMsg || '网络错误')));
      }
    });
  });
}

/**
 * 获取头像数据（通过API代理）
 * @param {string} avatarUrl - 头像URL
 * @returns {Promise<string>} - 本地临时文件路径
 */
async function getAvatar(avatarUrl) {
  return new Promise((resolve, reject) => {
    if (!avatarUrl) {
      reject(new Error('头像URL为空'));
      return;
    }

    if (avatarUrl.startsWith('wxfile://')) {
      resolve(avatarUrl);
      return;
    }

    let fileName = avatarUrl;
    if (avatarUrl.startsWith(API_BASE_URL + '/auth/avatar/')) {
      fileName = avatarUrl.substring((API_BASE_URL + '/auth/avatar/').length);
    } else if (avatarUrl.startsWith(API_BASE_URL + '/uploads/avatars/')) {
      fileName = avatarUrl.substring((API_BASE_URL + '/uploads/avatars/').length);
    } else if (avatarUrl.startsWith('/uploads/avatars/')) {
      fileName = avatarUrl.substring('/uploads/avatars/'.length);
    } else if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) {
      const urlParts = avatarUrl.split('/');
      fileName = urlParts[urlParts.length - 1];
    }

    wx.request({
      url: API_BASE_URL + '/auth/avatar/' + fileName,
      method: 'GET',
      responseType: 'arraybuffer',
      success: (res) => {
        if (res.statusCode === 200) {
          const tempFilePath = wx.env.USER_DATA_PATH + '/avatar_' + Date.now() + '.png';
          wx.getFileSystemManager().writeFile({
            filePath: tempFilePath,
            data: res.data,
            encoding: 'binary',
            success: () => {
              resolve(tempFilePath);
            },
            fail: (err) => {
              console.error('写入文件失败:', err);
              reject(new Error('获取头像失败：写入文件失败'));
            }
          });
        } else {
          reject(new Error('获取头像失败：' + res.statusCode));
        }
      },
      fail: (err) => {
        console.error('获取头像失败:', err);
        reject(new Error('获取头像失败：网络错误'));
      }
    });
  });
}

/**
 * 搜索用户
 * @param {string} keyword - 搜索关键词
 * @returns {Promise}
 */
async function searchUsers(keyword) {
  const users = await request({
    url: `/friends/search?keyword=${encodeURIComponent(keyword)}`,
    method: 'GET'
  });
  return Array.isArray(users) ? users : [];
}

/**
 * 获取好友列表
 * @returns {Promise}
 */
async function getFriends() {
  const friends = await request({
    url: '/friends',
    method: 'GET'
  });
  return Array.isArray(friends) ? friends : [];
}

/**
 * 获取好友热力图列表
 * @returns {Promise}
 */
async function getFriendsHeatmap() {
  const heatmaps = await request({
    url: '/friends/heatmap',
    method: 'GET'
  });
  return Array.isArray(heatmaps) ? heatmaps : [];
}

/**
 * 获取单个好友的热力图
 * @param {number} friendId - 好友ID
 * @returns {Promise}
 */
async function getFriendHeatmap(friendId, year, month) {
  let url = `/friends/${friendId}/heatmap`;
  const params = [];
  
  if (year) params.push(`year=${year}`);
  if (month) params.push(`month=${month}`);
  
  if (params.length > 0) {
    url += '?' + params.join('&');
  }
  
  const heatmap = await request({
    url: url,
    method: 'GET'
  });
  return heatmap || {};
}

/**
 * 添加好友
 * @param {number} friendId - 好友ID
 * @returns {Promise}
 */
async function addFriend(friendId) {
  const result = await request({
    url: '/friends',
    method: 'POST',
    data: { friendId: friendId }
  });
  return result;
}

/**
 * 删除好友
 * @param {number} friendId - 好友ID
 * @returns {Promise}
 */
async function deleteFriend(friendId) {
  await request({
    url: `/friends/${friendId}`,
    method: 'DELETE'
  });
}

/**
 * 向好友发布任务
 * @param {Object} task - 任务信息
 * @returns {Promise}
 */
async function postTaskToFriend(task) {
  const backendTask = {
    title: task.title,
    description: task.desc || '',
    dueDate: task.deadline ? new Date(task.deadline).toISOString() : null,
    targetUserId: task.targetUserId,
    priority: 'normal',
    completed: false
  };
  const result = await request({
    url: '/todos/post',
    method: 'POST',
    data: backendTask
  });
  return result;
}

/**
 * 接收任务
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
async function acceptTask(taskId) {
  const result = await request({
    url: `/todos/${taskId}/accept`,
    method: 'PUT'
  });
  return result;
}

/**
 * 拒绝任务
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
async function rejectTask(taskId) {
  const result = await request({
    url: `/todos/${taskId}/reject`,
    method: 'PUT'
  });
  return result;
}

/**
 * 获取待接收的任务列表
 * @returns {Promise}
 */
async function getPendingTasks() {
  const tasks = await request({
    url: '/todos/pending-tasks',
    method: 'GET'
  });
  return Array.isArray(tasks) ? tasks.map(transformTodoFromBackend) : [];
}

/**
 * 发送聊天消息给 AI
 * @param {string} message - 用户消息
 * @returns {Promise<string>} - AI 回复
 */
async function chat(message) {
  const result = await request({
    url: '/chat',
    method: 'POST',
    data: { message: message }
  }, true);
  return result;
}

module.exports = {
  register,
  login,
  getCurrentUser,
  updateCurrentUser,
  uploadAvatar,
  getAvatar,
  getAllTodos,
  getAllTodosWithDeleted,
  createTodo,
  updateTodo,
  deleteTodo,
  toggleTodo,
  getCompletedTodos,
  getPendingTodos,
  searchUsers,
  getFriends,
  getFriendsHeatmap,
  getFriendHeatmap,
  addFriend,
  deleteFriend,
  postTaskToFriend,
  acceptTask,
  rejectTask,
  getPendingTasks,
  chat
};
