// utils/storage.js - 本地存储工具封装
// 职责：所有 Todo 数据的持久化读写，业务逻辑与存储解耦

const STORAGE_KEY = 'todos';

/**
 * 读取所有 Todo
 * @returns {Array} todos 数组
 */
function getAllTodos() {
  return wx.getStorageSync(STORAGE_KEY) || [];
}

/**
 * 保存所有 Todo（全量覆盖）
 * @param {Array} todos
 */
function saveTodos(todos) {
  wx.setStorageSync(STORAGE_KEY, todos);
}

/**
 * 新增一条 Todo
 * @param {Object} todo
 * @returns {Array} 更新后的数组
 */
function addTodo(todo) {
  const todos = getAllTodos();
  todos.unshift(todo); // 新增的放在最前面
  saveTodos(todos);
  return todos;
}

/**
 * 更新一条 Todo（按 id 匹配）
 * @param {Object} updated
 * @returns {Array} 更新后的数组
 */
function updateTodo(updated) {
  const todos = getAllTodos();
  const idx = todos.findIndex(t => t.id === updated.id);
  if (idx !== -1) {
    todos[idx] = { ...todos[idx], ...updated };
  }
  saveTodos(todos);
  return todos;
}

/**
 * 删除一条 Todo（按 id）
 * @param {string} id
 * @returns {Array} 更新后的数组
 */
function deleteTodo(id) {
  const todos = getAllTodos().filter(t => t.id !== id);
  saveTodos(todos);
  return todos;
}

/**
 * 切换 Todo 完成状态
 * @param {string} id
 * @returns {Array} 更新后的数组
 */
function toggleTodo(id) {
  const todos = getAllTodos();
  const todo = todos.find(t => t.id === id);
  if (todo) {
    todo.completed = !todo.completed;
    todo.completedAt = todo.completed ? Date.now() : null;
  }
  saveTodos(todos);
  return todos;
}

/**
 * 清除全部已完成项
 * @returns {Array} 更新后的数组
 */
function clearCompleted() {
  const todos = getAllTodos().filter(t => !t.completed);
  saveTodos(todos);
  return todos;
}

module.exports = {
  getAllTodos,
  saveTodos,
  addTodo,
  updateTodo,
  deleteTodo,
  toggleTodo,
  clearCompleted
};
