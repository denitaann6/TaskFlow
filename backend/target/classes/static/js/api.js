/**
 * TaskFlow Central API Client & Session Manager
 * Connects to Spring Boot backend at http://localhost:8085
 */

const API_BASE = window.location.port === '8085' ? '' : 'http://localhost:8085';

// Global HTML Escape Utility
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
if (typeof window !== 'undefined') {
  window.escapeHtml = escapeHtml;
}

const API = {
  // Session Keys
  TOKEN_KEY: 'taskflow_session_token',
  USER_KEY: 'taskflow_session_user',

  escapeHtml(str) {
    return escapeHtml(str);
  },

  getToken() {
    return sessionStorage.getItem(this.TOKEN_KEY) || localStorage.getItem(this.TOKEN_KEY);
  },

  setSession(token, user) {
    if (token) {
      sessionStorage.setItem(this.TOKEN_KEY, token);
      localStorage.setItem(this.TOKEN_KEY, token);
    }
    if (user) {
      sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
      localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    }
  },

  clearSession() {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  },

  getUser() {
    try {
      const u = sessionStorage.getItem(this.USER_KEY) || localStorage.getItem(this.USER_KEY);
      return u ? JSON.parse(u) : null;
    } catch (e) {
      return null;
    }
  },

  isAdmin() {
    const user = this.getUser();
    return user && user.role && user.role.toUpperCase() === 'ADMIN';
  },

  // Base HTTP Request Wrapper
  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const headers = options.headers || {};
    
    // Attach token if present
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
      headers['X-Auth-Token'] = token;
    }

    if (!headers['Content-Type'] && !(options.body instanceof FormData) && options.method && options.method !== 'GET') {
      headers['Content-Type'] = 'application/json';
    }

    const config = {
      ...options,
      headers,
      credentials: 'include'
    };

    try {
      const response = await fetch(url, config);

      // Handle unauthenticated session
      if (response.status === 401) {
        this.clearSession();
        if (!window.location.pathname.endsWith('login.html')) {
          window.location.href = 'login.html?expired=true';
        }
        throw new Error('Authentication required. Please sign in.');
      }

      const text = await response.text();
      let data;
      try {
        data = text ? JSON.parse(text) : {};
      } catch (e) {
        data = { message: text };
      }

      if (!response.ok) {
        const errorMsg = data.message || data.error || `Error ${response.status}: ${response.statusText}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  },

  // Authentication Endpoints
  async login(username, password) {
    const res = await this.request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    if (res && res.token && res.user) {
      this.clearSession();
      this.setSession(res.token, res.user);
    }
    return res;
  },

  async logout() {
    try {
      await this.request('/api/auth/logout', { method: 'POST' });
    } catch (e) {
      // Ignore network errors during logout
    } finally {
      this.clearSession();
      window.location.href = 'login.html';
    }
  },

  async getCurrentUser() {
    return await this.request('/api/auth/me');
  },

  async getTeamMembers() {
    return await this.request('/api/auth/team');
  },

  async getAllUsers() {
    return await this.request('/api/auth/users');
  },

  // Tasks Management
  async getTasks(search = '') {
    const query = search ? `?search=${encodeURIComponent(search)}` : '';
    return await this.request(`/api/tasks${query}`);
  },

  async getTaskById(id) {
    return await this.request(`/api/tasks/${id}`);
  },

  async createTask(id, name, member, priority, status) {
    const params = new URLSearchParams({ id, name, member, priority, status });
    return await this.request(`/api/tasks?${params.toString()}`, {
      method: 'POST'
    });
  },

  async editTask(id, name, member, priority, status) {
    const params = new URLSearchParams();
    if (name) params.append('name', name);
    if (member) params.append('member', member);
    if (priority) params.append('priority', priority);
    if (status) params.append('status', status);

    return await this.request(`/api/tasks/${id}?${params.toString()}`, {
      method: 'PUT'
    });
  },

  async deleteTask(id) {
    return await this.request(`/api/tasks/${id}`, {
      method: 'DELETE'
    });
  },

  async updateTaskStatus(id, status) {
    const params = new URLSearchParams({ status });
    return await this.request(`/api/tasks/${id}/status?${params.toString()}`, {
      method: 'PUT'
    });
  },

  async completeTask(id) {
    return await this.request(`/api/tasks/${id}/complete`, {
      method: 'POST'
    });
  },

  async getStatsSummary() {
    return await this.request('/api/tasks/stats/summary');
  },

  // Phase 1: Custom Singly Linked List (Nodes & References)
  async traverseLinkedList() { return await this.request('/api/tasks/linkedlist/traverse'); },
  async insertLinkedList(position, id, name, member = 'Demo User', priority = 'Medium', status = 'Pending') {
    const params = new URLSearchParams({ position, id, name, member, priority, status });
    return await this.request(`/api/tasks/linkedlist/insert?${params.toString()}`, { method: 'POST' });
  },
  async deleteLinkedList(id) {
    const params = new URLSearchParams({ id });
    return await this.request(`/api/tasks/linkedlist/delete?${params.toString()}`, { method: 'DELETE' });
  },
  async resetLinkedList() {
    return await this.request('/api/tasks/linkedlist/reset', { method: 'POST' });
  },

  // Phase 1: FIFO Queue (Pending Tasks)
  async getQueue() { return await this.request('/api/tasks/queue'); },
  async peekQueue() { return await this.request('/api/tasks/queue/peek'); },
  async enqueueQueue(id, name, member = 'Unassigned', priority = 'Medium') {
    const params = new URLSearchParams({ id, name, member, priority });
    return await this.request(`/api/tasks/queue/enqueue?${params.toString()}`, { method: 'POST' });
  },
  async processNextQueue() { return await this.request('/api/tasks/process-next', { method: 'POST' }); },
  async syncQueue() { return await this.request('/api/tasks/queue/sync', { method: 'POST' }); },

  // Phase 1: LIFO Stack (Completed Tasks)
  async getStack() { return await this.request('/api/tasks/stack'); },
  async peekStack() { return await this.request('/api/tasks/stack/peek'); },
  async pushStack(id, name, status = 'Completed') {
    const params = new URLSearchParams({ id, name, status });
    return await this.request(`/api/tasks/stack/push?${params.toString()}`, { method: 'POST' });
  },
  async popStack() { return await this.request('/api/tasks/stack/pop', { method: 'POST' }); },
  async syncStack() { return await this.request('/api/tasks/stack/sync', { method: 'POST' }); },
  async getPoppedHistory() { return await this.request('/api/tasks/stack/history'); },

  // Phase 1: Stack Expression Bracket Checker
  async checkBrackets(expression) {
    const params = new URLSearchParams({ expression });
    return await this.request(`/api/tasks/brackets/check?${params.toString()}`);
  },

  // Phase 2.1: User Binary Search Tree (Admin Only)
  async getUserTree() {
    return await this.request('/api/admin/users/tree');
  },
  async getUserBSTInorder() {
    return await this.request('/api/admin/users/tree/inorder');
  },
  async getUserBSTPreorder() {
    return await this.request('/api/admin/users/tree/preorder');
  },
  async getUserBSTPostorder() {
    return await this.request('/api/admin/users/tree/postorder');
  },
  async searchUserBST(id) {
    const params = new URLSearchParams({ id });
    return await this.request(`/api/admin/users/tree/search?${params.toString()}`);
  },

  // Phase 2.2: Project Graph & Task Dependencies (Admin Only)
  async getProjectGraph() {
    return await this.request('/api/admin/tasks/graph');
  },
  async addProjectDependency(fromId, toId) {
    const params = new URLSearchParams({ fromId, toId });
    return await this.request(`/api/admin/tasks/graph/dependency?${params.toString()}`, { method: 'POST' });
  },
  async removeProjectDependency(fromId, toId) {
    const params = new URLSearchParams({ fromId, toId });
    return await this.request(`/api/admin/tasks/graph/dependency?${params.toString()}`, { method: 'DELETE' });
  },
  async runProjectGraphBFS(startId) {
    const params = new URLSearchParams({ startId });
    return await this.request(`/api/admin/tasks/graph/bfs?${params.toString()}`);
  },
  async clearProjectGraph() {
    return await this.request('/api/admin/tasks/graph/clear', { method: 'POST' });
  },

  // Phase 2: BST & Graph
  async getBSTInorder() { return await this.request('/api/tasks/bst/inorder'); },
  async getBSTPreorder() { return await this.request('/api/tasks/bst/preorder'); },
  async getBSTPostorder() { return await this.request('/api/tasks/bst/postorder'); },
  async searchBST(id) {
    const params = new URLSearchParams({ id });
    return await this.request(`/api/tasks/bst/search?${params.toString()}`);
  },
  async getGraph() { return await this.request('/api/tasks/graph'); },
  async addDependency(fromId, toId) {
    const params = new URLSearchParams({ fromId, toId });
    return await this.request(`/api/tasks/graph/dependency?${params.toString()}`, { method: 'POST' });
  },
  async bfsGraph(startId) {
    const params = new URLSearchParams({ startId });
    return await this.request(`/api/tasks/graph/bfs?${params.toString()}`);
  },
  async clearGraph() { return await this.request('/api/tasks/graph/clear', { method: 'POST' }); },

  // Phase 3: Searching & Sorting
  async linearSearch(query, searchBy = 'all') {
    const params = new URLSearchParams({ query: query || '', searchBy: searchBy || 'all' });
    return await this.request(`/api/tasks/search/linear?${params.toString()}`);
  },
  async binarySearch(queryOrId, searchBy = 'id') {
    const params = new URLSearchParams();
    if (searchBy === 'name') {
      params.append('query', queryOrId != null ? String(queryOrId) : '');
      params.append('searchBy', 'name');
    } else {
      params.append('id', queryOrId != null ? String(queryOrId) : '');
      params.append('query', queryOrId != null ? String(queryOrId) : '');
      params.append('searchBy', 'id');
    }
    return await this.request(`/api/tasks/search/binary?${params.toString()}`);
  },
  async insertionSort(sortBy = 'id') {
    const params = new URLSearchParams({ sortBy: sortBy || 'id' });
    return await this.request(`/api/tasks/sort/insertion?${params.toString()}`);
  },
  async quickSort(sortBy = 'id') {
    const params = new URLSearchParams({ sortBy: sortBy || 'id' });
    return await this.request(`/api/tasks/sort/quick?${params.toString()}`);
  },

  // Phase 4: Greedy, DP, Backtracking
  async getGreedy(timeLimit = 8) {
    return await this.request(`/api/tasks/phase4/greedy?timeLimit=${timeLimit}`);
  },
  async getDP(timeLimit = 8) {
    return await this.request(`/api/tasks/phase4/dp?timeLimit=${timeLimit}`);
  },
  async getBacktracking(limit = 10) {
    return await this.request(`/api/tasks/phase4/backtracking?limit=${limit}`);
  },

  // Guard for protected pages
  requireAuth() {
    const user = this.getUser();
    if (!user || !this.getToken()) {
      window.location.href = 'login.html';
      return null;
    }
    return user;
  },

  // Sync and validate current user against backend to prevent stale role state
  async syncCurrentUser() {
    try {
      const freshUser = await this.getCurrentUser();
      if (freshUser) {
        this.setSession(this.getToken(), freshUser);
        return freshUser;
      }
    } catch (e) {
      // Handled by request/requireAuth if unauthenticated
    }
    return this.getUser();
  },

  // UI Toast helper
  showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.style.cssText = 'position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:8px;';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    const bg = type === 'success' ? '#059669' : (type === 'error' ? '#dc2626' : '#1e293b');
    toast.style.cssText = `background:${bg};color:#fff;padding:12px 18px;border-radius:8px;font-size:0.88rem;box-shadow:0 4px 12px rgba(0,0,0,0.15);animation:fadeIn 0.2s ease;max-width:360px;`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }
};

// Explicitly attach API to window for universal global accessibility
if (typeof window !== 'undefined') {
  window.API = API;
}

