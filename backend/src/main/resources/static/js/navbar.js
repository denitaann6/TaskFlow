/**
 * TaskFlow Shared Navigation & Header Component
 * Renders responsive sidebar, active page indicators, role badge, and session controls.
 */

function renderAppLayout(activePage = 'dashboard', pageTitle = 'Dashboard', passedUser = null) {
  let user = passedUser;
  if (!user && typeof API !== 'undefined') {
    user = API.getUser ? API.getUser() : null;
    if (!user && API.requireAuth) {
      user = API.requireAuth();
    }
  }
  if (!user) return;

  const initials = (user.displayName || user.username || 'U').substring(0, 2).toUpperCase();

  // 1. Render Initial Sidebar
  // NOTE: By default, Pending Queue and Completed Tasks are completely omitted from the HTML markup.
  // They are ONLY added when the authenticated user's role is verified from the backend API (/api/auth/me) as ADMIN.
  // This guarantees regular members (MEMBER) never see these options under any circumstance.
  const sidebarContainer = document.getElementById('sidebar-container');
  if (sidebarContainer) {
    sidebarContainer.innerHTML = `
      <aside class="app-sidebar">
        <div class="sidebar-brand">
          <img src="assets/logo.svg" alt="TaskFlow" class="brand-icon">
          <div>
            <span class="brand-title">TaskFlow</span>
            <span class="brand-subtitle">Project Management</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <span class="nav-section-title">Navigation</span>
          
          <a href="dashboard.html" class="nav-item ${activePage === 'dashboard' ? 'active' : ''}">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
            </svg>
            <span>Dashboard</span>
          </a>

          <a href="tasks.html" class="nav-item ${activePage === 'tasks' ? 'active' : ''}">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
            </svg>
            <span id="nav-tasks-label">My Tasks</span>
          </a>

          <a href="team.html" class="nav-item ${activePage === 'team' ? 'active' : ''}">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
            <span>Team Directory</span>
          </a>

          <a href="search-sort.html" class="nav-item ${activePage === 'search-sort' ? 'active' : ''}">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM3 4h18M3 8h12M3 12h8"/>
            </svg>
            <span>Search &amp; Sort Lab</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="user-compact-card">
            <div class="user-avatar-circle">${initials}</div>
            <div class="user-info-text">
              <div class="user-info-name">${user.displayName || user.username}</div>
              <div class="user-info-role">
                <span class="role-badge member">${user.role || 'MEMBER'}</span>
              </div>
            </div>
            <button id="btn-logout" class="btn-sidebar-logout" title="Sign Out">
              <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
              </svg>
            </button>
          </div>
        </div>
      </aside>
    `;

    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        if (confirm('Are you sure you want to sign out of TaskFlow?')) {
          API.logout();
        }
      });
    }
  }

  // 2. Render Header
  const headerContainer = document.getElementById('header-container');
  if (headerContainer) {
    headerContainer.innerHTML = `
      <header class="app-header">
        <div class="header-left">
          <h2 class="page-headline">${pageTitle}</h2>
        </div>
        <div class="header-right">
          <div style="font-size: 0.85rem; color: var(--slate-500);">
            Signed in as <strong style="color: var(--navy-900);">${user.displayName || user.username}</strong>
          </div>
          <span class="role-badge member">${user.role || 'MEMBER'}</span>
        </div>
      </header>
    `;
  }

  // 3. Authenticate and verify role with trusted backend API /api/auth/me
  // Only when the verified backend role is ADMIN are Pending Queue and Completed Tasks rendered.
  if (typeof API !== 'undefined' && typeof API.getCurrentUser === 'function') {
    API.getCurrentUser().then(verifiedUser => {
      if (verifiedUser && verifiedUser.role) {
        const isVerifiedAdmin = String(verifiedUser.role).trim().toUpperCase() === 'ADMIN';

        // Keep local storage in sync with verified server state
        if (typeof API.setSession === 'function' && typeof API.getToken === 'function') {
          API.setSession(API.getToken(), verifiedUser);
        }

        if (isVerifiedAdmin) {
          // ADMIN: Update Tasks Workspace label and insert Pending Queue & Completed Tasks
          const tasksLabel = document.getElementById('nav-tasks-label');
          if (tasksLabel) tasksLabel.textContent = 'Tasks Workspace';

          const tasksLink = document.querySelector('a[href="tasks.html"]');
          if (tasksLink && !document.querySelector('a[href="pending-queue.html"]')) {
            tasksLink.insertAdjacentHTML('afterend', `
              <a href="pending-queue.html" class="nav-item ${activePage === 'pending-queue' ? 'active' : ''}" data-admin-nav="true">
                <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <span>Pending Queue</span>
              </a>

              <a href="completed-tasks.html" class="nav-item ${activePage === 'completed-tasks' ? 'active' : ''}" data-admin-nav="true">
                <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <span>Completed Tasks</span>
              </a>

              <a href="user-tree.html" class="nav-item ${activePage === 'user-tree' ? 'active' : ''}" data-admin-nav="true">
                <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
                </svg>
                <span>User Tree (BST)</span>
              </a>

              <a href="project-graph.html" class="nav-item ${activePage === 'project-graph' ? 'active' : ''}" data-admin-nav="true">
                <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
                <span>Project Graph</span>
              </a>

              <a href="algorithm-studio.html" class="nav-item ${activePage === 'algorithm-studio' ? 'active' : ''}" data-admin-nav="true">
                <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
                </svg>
                <span>Algorithm Studio</span>
              </a>
            `);
          }

          document.querySelectorAll('.role-badge').forEach(badge => {
            badge.className = 'role-badge admin';
            badge.textContent = verifiedUser.role;
          });
        } else {
          // MEMBER: Strictly guarantee admin-only navigation links are completely absent from DOM
          document.querySelectorAll('a[href="pending-queue.html"], a[href="completed-tasks.html"], a[href="user-tree.html"], a[href="project-graph.html"], a[href="algorithm-studio.html"], [data-admin-nav="true"]').forEach(el => el.remove());

          const tasksLabel = document.getElementById('nav-tasks-label');
          if (tasksLabel) tasksLabel.textContent = 'My Tasks';

          document.querySelectorAll('.role-badge').forEach(badge => {
            badge.className = 'role-badge member';
            badge.textContent = verifiedUser.role;
          });
        }

        // Update user display names
        const displayName = verifiedUser.displayName || verifiedUser.username || '';
        document.querySelectorAll('.user-info-name').forEach(el => el.textContent = displayName);
        const userInitials = displayName.substring(0, 2).toUpperCase() || 'U';
        document.querySelectorAll('.user-avatar-circle').forEach(el => el.textContent = userInitials);
      }
    }).catch(err => {
      console.warn('Backend authentication check error:', err);
      // Guarantee admin links are removed on error
      document.querySelectorAll('a[href="pending-queue.html"], a[href="completed-tasks.html"], a[href="user-tree.html"], a[href="project-graph.html"], a[href="algorithm-studio.html"], [data-admin-nav="true"]').forEach(el => el.remove());
    });
  }
}
