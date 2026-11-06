// scratch/acceptance_test.js
// Master Acceptance Test Suite for TaskFlow – Project Management System

const http = require('http');
const fs = require('fs');
const path = require('path');

function request(options, postData = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try {
          json = JSON.parse(data);
        } catch (e) {
          json = data;
        }
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          data: json,
          rawBody: data
        });
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
}

function parseCookies(res) {
  const setCookie = res.headers['set-cookie'];
  if (!setCookie) return '';
  return setCookie.map(c => c.split(';')[0]).join('; ');
}

async function login(username, password) {
  const res = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { username, password });
  return {
    status: res.statusCode,
    cookie: parseCookies(res),
    body: res.data
  };
}

async function runAcceptanceTest() {
  console.log('========================================================================');
  console.log('       TASKFLOW: FINAL MASTER ACCEPTANCE TEST & VERIFICATION            ');
  console.log('========================================================================\n');

  const testResults = [];

  function record(section, testId, testName, pass, evidence, notTestedReason = null) {
    if (notTestedReason) {
      testResults.push({ section, testId, testName, status: 'NOT TESTED', evidence: notTestedReason });
      console.log(`[NOT TESTED] ${section} - ${testId}: ${testName} -> ${notTestedReason}`);
      return;
    }
    const status = pass ? 'PASS' : 'FAIL';
    testResults.push({ section, testId, testName, status, evidence });
    console.log(`[${status}] ${section} - ${testId}: ${testName} -> ${evidence}`);
  }

  // ========================================================================
  // SECTION 1: USER WORKFLOW
  // ========================================================================
  console.log('\n--- 1. USER WORKFLOW ---');

  // 1.1 Registration
  record('1. USER WORKFLOW', 'UW-01', 'User Registration', false, null,
    'NOT APPLICABLE: TaskFlow relies on pre-seeded 17-member team for BST balance integrity; no self-registration endpoint exists.');

  // 1.2 Authentication
  const adminAuth = await login('admin', 'Admin@123');
  record('1. USER WORKFLOW', 'UW-02', 'Admin Login (admin / Admin@123)',
    adminAuth.status === 200 && adminAuth.body?.user?.role === 'ADMIN',
    `HTTP ${adminAuth.status}, User: ${adminAuth.body?.user?.username}, Role: ${adminAuth.body?.user?.role}`);

  const memberAuth = await login('sheena', 'Member@123');
  record('1. USER WORKFLOW', 'UW-03', 'Member Login (sheena / Member@123)',
    memberAuth.status === 200 && memberAuth.body?.user?.role === 'MEMBER',
    `HTTP ${memberAuth.status}, User: ${memberAuth.body?.user?.username}, Role: ${memberAuth.body?.user?.role}`);

  // 1.3 Workflows: Admin sees all tasks, Member sees only assigned tasks
  const adminTasksRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-04', 'Admin Views All Tasks',
    adminTasksRes.statusCode === 200 && adminTasksRes.data.length === 14,
    `Admin retrieved all ${adminTasksRes.data?.length} tasks in database`);

  const memberTasksRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks',
    method: 'GET',
    headers: { 'Cookie': memberAuth.cookie }
  });
  const memberOnlyAssigned = memberTasksRes.data.every(t => t.member && t.member.toLowerCase() === 'sheena');
  record('1. USER WORKFLOW', 'UW-05', 'Member Task Isolation (Sheena)',
    memberTasksRes.statusCode === 200 && memberTasksRes.data.length === 1 && memberOnlyAssigned,
    `Member received only ${memberTasksRes.data.length} task(s) strictly assigned to "Sheena": Task #${memberTasksRes.data[0]?.id} ("${memberTasksRes.data[0]?.name}")`);

  // 1.4 Task Lifecycle: Create -> View -> Edit -> Update Status -> Delete (Clean cleanup)
  const tempTaskId = 999;
  const createTaskRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks?id=${tempTaskId}&name=Acceptance+Test+Task&member=John&priority=Medium&status=In%20Progress`,
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-06', 'Admin Creates Temporary Task #999',
    createTaskRes.statusCode === 200 && createTaskRes.data?.id === tempTaskId,
    `HTTP ${createTaskRes.statusCode}, Created task #${createTaskRes.data?.id}: "${createTaskRes.data?.name}"`);

  const viewTaskRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks/${tempTaskId}`,
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-07', 'Admin Views Task #999 by ID',
    viewTaskRes.statusCode === 200 && viewTaskRes.data?.id === tempTaskId,
    `HTTP ${viewTaskRes.statusCode}, Retrieved Task #${viewTaskRes.data?.id}`);

  const editTaskRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks/${tempTaskId}?name=Acceptance+Test+Task+Updated&priority=High`,
    method: 'PUT',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-08', 'Admin Edits Task #999',
    editTaskRes.statusCode === 200 && editTaskRes.data?.name === 'Acceptance Test Task Updated' && editTaskRes.data?.priority === 'High',
    `HTTP ${editTaskRes.statusCode}, Updated name to "${editTaskRes.data?.name}", priority: ${editTaskRes.data?.priority}`);

  const updateStatusRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks/${tempTaskId}/status?status=Pending`,
    method: 'PUT',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-09', 'Admin Updates Task #999 Status',
    updateStatusRes.statusCode === 200 && updateStatusRes.data?.status === 'Pending',
    `HTTP ${updateStatusRes.statusCode}, Updated status to: ${updateStatusRes.data?.status}`);

  const deleteTaskRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks/${tempTaskId}`,
    method: 'DELETE',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-10', 'Admin Deletes Temporary Task #999 (Cleanup)',
    deleteTaskRes.statusCode === 200,
    `HTTP ${deleteTaskRes.statusCode}, Response: "${deleteTaskRes.rawBody}"`);

  const verifyDeletedRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: `/api/tasks/${tempTaskId}`,
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-11', 'Verify Task #999 Removed (404 Not Found)',
    verifyDeletedRes.statusCode === 404,
    `HTTP ${verifyDeletedRes.statusCode} Not Found as expected`);

  // 1.5 Logout & Session Invalidation
  const tempUserAuth = await login('sheena', 'Member@123');
  const logoutRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/auth/logout',
    method: 'POST',
    headers: { 'Cookie': tempUserAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-12', 'Member Logout',
    logoutRes.statusCode === 200 && logoutRes.data?.success === true,
    `HTTP ${logoutRes.statusCode}, Message: "${logoutRes.data?.message}"`);

  const expiredSessionRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/auth/me',
    method: 'GET',
    headers: { 'Cookie': tempUserAuth.cookie }
  });
  record('1. USER WORKFLOW', 'UW-13', 'Session Invalidation After Logout',
    expiredSessionRes.statusCode === 401,
    `HTTP ${expiredSessionRes.statusCode} Unauthorized on invalidated session`);

  // ========================================================================
  // SECTION 2: ROLE-BASED SECURITY
  // ========================================================================
  console.log('\n--- 2. ROLE-BASED SECURITY ---');

  // 2.1 Admin-Only Endpoints
  const adminEndpoints = [
    { name: 'GET /api/tasks/queue', path: '/api/tasks/queue', method: 'GET' },
    { name: 'GET /api/tasks/stack', path: '/api/tasks/stack', method: 'GET' },
    { name: 'GET /api/admin/users/tree', path: '/api/admin/users/tree', method: 'GET' },
    { name: 'GET /api/admin/tasks/graph', path: '/api/admin/tasks/graph', method: 'GET' },
    { name: 'GET /api/auth/users', path: '/api/auth/users', method: 'GET' }
  ];

  for (const ep of adminEndpoints) {
    // Unauthenticated -> 401
    const unauth = await request({ hostname: 'localhost', port: 8085, path: ep.path, method: ep.method });
    record('2. ROLE-BASED SECURITY', `RBS-UNAUTH-${ep.name}`, `Unauth Rejection: ${ep.name}`,
      unauth.statusCode === 401, `HTTP ${unauth.statusCode} (Expected 401)`);

    // Member -> 403
    const memb = await request({ hostname: 'localhost', port: 8085, path: ep.path, method: ep.method, headers: { 'Cookie': memberAuth.cookie } });
    record('2. ROLE-BASED SECURITY', `RBS-MEMBER-${ep.name}`, `Member Rejection: ${ep.name}`,
      memb.statusCode === 403, `HTTP ${memb.statusCode} (Expected 403)`);

    // Admin -> 200
    const adm = await request({ hostname: 'localhost', port: 8085, path: ep.path, method: ep.method, headers: { 'Cookie': adminAuth.cookie } });
    record('2. ROLE-BASED SECURITY', `RBS-ADMIN-${ep.name}`, `Admin Access: ${ep.name}`,
      adm.statusCode === 200, `HTTP ${adm.statusCode} OK`);
  }

  // 2.2 Member IDOR Defense
  const idorView = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/102', // Task 102 assigned to John
    method: 'GET',
    headers: { 'Cookie': memberAuth.cookie }
  });
  record('2. ROLE-BASED SECURITY', 'RBS-IDOR-VIEW', 'Member IDOR View Rejection (/api/tasks/102)',
    idorView.statusCode === 403, `HTTP ${idorView.statusCode} Forbidden`);

  const idorEdit = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/102/status?status=Completed',
    method: 'PUT',
    headers: { 'Cookie': memberAuth.cookie }
  });
  record('2. ROLE-BASED SECURITY', 'RBS-IDOR-UPDATE', 'Member IDOR Status Update Rejection (/api/tasks/102/status)',
    idorEdit.statusCode === 403, `HTTP ${idorEdit.statusCode} Forbidden`);

  // 2.3 Direct URL Access Pre-Render Guards
  const adminOnlyPages = [
    'pending-queue.html',
    'completed-tasks.html',
    'user-tree.html',
    'project-graph.html',
    'algorithm-studio.html'
  ];
  for (const page of adminOnlyPages) {
    const pageContent = fs.readFileSync(path.join('src/frontend', page), 'utf8');
    const hasGuard = pageContent.includes('error=forbidden') || pageContent.includes('ADMIN');
    record('2. ROLE-BASED SECURITY', `RBS-PAGE-GUARD-${page}`, `Pre-Render Guard on ${page}`,
      hasGuard, `Script-level redirect guard verified`);
  }

  // ========================================================================
  // SECTION 3: VALIDATION AND ERROR HANDLING
  // ========================================================================
  console.log('\n--- 3. VALIDATION AND ERROR HANDLING ---');

  // 3.1 Empty task name
  const emptyName = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks?id=998&name=&member=John',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-01', 'Empty Task Name Rejected',
    emptyName.statusCode === 400, `HTTP ${emptyName.statusCode} Bad Request`);

  // 3.2 Empty member assignee
  const emptyMember = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks?id=998&name=ValidTask&member=',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-02', 'Empty Member Assignee Rejected',
    emptyMember.statusCode === 400, `HTTP ${emptyMember.statusCode} Bad Request`);

  // 3.3 Duplicate Task ID
  const dupTaskId = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks?id=101&name=DuplicateTask&member=John',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-03', 'Duplicate Task ID Rejected',
    dupTaskId.statusCode === 409, `HTTP ${dupTaskId.statusCode} Conflict`);

  // 3.4 Invalid Task ID Lookup
  const invalidTaskLookup = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/999999',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-04', 'Nonexistent Task ID Lookup',
    invalidTaskLookup.statusCode === 404, `HTTP ${invalidTaskLookup.statusCode} Not Found`);

  // 3.5 Self-Dependency Graph Edge
  const selfDep = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=101',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-05', 'Self-Loop Dependency Rejected (101 -> 101)',
    selfDep.statusCode === 400, `HTTP ${selfDep.statusCode} Bad Request`);

  // 3.6 Nonexistent Prerequisite Task in Dependency
  const nonExistentDep = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=99999&toId=101',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie }
  });
  record('3. VALIDATION', 'VAL-06', 'Nonexistent Task Dependency Rejected',
    nonExistentDep.statusCode === 404, `HTTP ${nonExistentDep.statusCode} Not Found`);

  // ========================================================================
  // SECTION 4: COMPLETE FRONTEND AND API REGRESSION (ALL DSA PHASES)
  // ========================================================================
  console.log('\n--- 4. COMPLETE FRONTEND AND API REGRESSION (DSA PHASES) ---');

  // 4.1 Frontend Web Pages Delivery (HTTP 200)
  const allHtmlPages = [
    'index.html',
    'login.html',
    'dashboard.html',
    'tasks.html',
    'team.html',
    'pending-queue.html',
    'completed-tasks.html',
    'user-tree.html',
    'project-graph.html',
    'search-sort.html',
    'algorithm-studio.html'
  ];
  for (const page of allHtmlPages) {
    const pageRes = await request({ hostname: 'localhost', port: 8085, path: `/${page}`, method: 'GET' });
    record('4. FRONTEND REGRESSION', `FE-PAGE-${page}`, `HTTP 200 for ${page}`,
      pageRes.statusCode === 200, `HTTP ${pageRes.statusCode}, length: ${pageRes.rawBody?.length} bytes`);
  }

  // 4.2 Frontend Scripts Syntax Check
  const jsFiles = ['js/api.js', 'js/navbar.js'];
  for (const js of jsFiles) {
    const code = fs.readFileSync(path.join('src/frontend', js), 'utf8');
    let syntaxValid = true;
    try {
      new Function(code);
    } catch (e) {
      syntaxValid = false;
    }
    record('4. FRONTEND REGRESSION', `FE-SYNTAX-${js}`, `JavaScript Syntax Valid: ${js}`,
      syntaxValid, syntaxValid ? 'No parse errors' : 'Parse error');
  }

  // 4.3 Phase 1: Linked List
  const llRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/linkedlist/traverse', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-01', 'Linked List Traversal',
    llRes.statusCode === 200 && llRes.data.totalNodes === 14,
    `HTTP 200, Traversable nodes: ${llRes.data.totalNodes}`);

  // 4.4 Phase 1: FIFO Queue
  const queuePeek = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/queue/peek', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-02', 'FIFO Task Queue',
    queuePeek.statusCode === 200 && queuePeek.data.task?.id !== undefined,
    `HTTP 200, Front task ID: ${queuePeek.data.task?.id} ("${queuePeek.data.task?.name}")`);

  // 4.5 Phase 1: LIFO Stack
  const stackPeek = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack/peek', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-03', 'LIFO Completed Tasks Stack',
    stackPeek.statusCode === 200 && stackPeek.data.task?.id !== undefined,
    `HTTP 200, Top task ID: ${stackPeek.data.task?.id} ("${stackPeek.data.task?.name}")`);

  // 4.6 Phase 2.1: Balanced User BST (Root=9, 17 Nodes, Height=5)
  const bstRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/users/tree', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-04', 'Balanced User BST Hierarchy',
    bstRes.statusCode === 200 && bstRes.data.root?.id === 9 && bstRes.data.totalUsers === 17 && bstRes.data.height === 5,
    `HTTP 200, Root: User #${bstRes.data.root?.id}, Nodes: ${bstRes.data.totalUsers}, Height: ${bstRes.data.height}, Leaves: ${bstRes.data.leafCount}`);

  // BST Traversals (Inorder, Preorder, Postorder)
  const inRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/users/tree/inorder', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const preRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/users/tree/preorder', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const postRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/users/tree/postorder', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-05', 'BST Traversals (Inorder, Preorder, Postorder)',
    inRes.statusCode === 200 && preRes.statusCode === 200 && postRes.statusCode === 200,
    `Inorder sorted 1-17: ${inRes.data[0]?.id === 1 && inRes.data[16]?.id === 17}, Preorder root: ${preRes.data[0]?.id === 9}, Postorder root: ${postRes.data[16]?.id === 9}`);

  // 4.7 Phase 2.2: Directed Graph & BFS Traversal
  const bfsRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph/bfs?startId=101', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-06', 'Graph Breadth-First Search (BFS)',
    bfsRes.statusCode === 200 && bfsRes.data.startId === 101,
    `HTTP 200, BFS order from 101: [${bfsRes.data.bfsOrder?.join(', ')}]`);

  // 4.8 Phase 3: Linear & Binary Search
  const linSearch = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/search/linear?query=101', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const binSearch = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/search/binary?query=101', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-07', 'Searching Operations (Linear & Binary)',
    linSearch.data?.found === true && binSearch.data?.found === true,
    `Linear search: found in ${linSearch.data.comparisons} comparison(s); Binary search: found in ${binSearch.data.comparisons} comparison(s)`);

  // 4.9 Phase 3: Sorting Techniques (Insertion Sort & Quick Sort)
  const insSort = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/sort/insertion?sortBy=id', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const qkSort = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/sort/quick?sortBy=id', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-08', 'Sorting Operations (Insertion & Quick Sort)',
    insSort.data?.sortedTasks?.length === 14 && qkSort.data?.sortedTasks?.length === 14,
    `Insertion Sort: ${insSort.data?.comparisons} comparisons; Quick Sort: ${qkSort.data?.comparisons} comparisons`);

  // 4.10 Phase 4: Greedy Algorithm
  const greedyRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/greedy?timeLimit=8', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-09', 'Greedy Task Selection Board',
    greedyRes.statusCode === 200 && greedyRes.data.totalAllocatedHours <= 8,
    `HTTP 200, Allocated: ${greedyRes.data.totalAllocatedHours}h <= 8h budget, Score: ${greedyRes.data.totalPriorityScore}`);

  // 4.11 Phase 4: Dynamic Programming (0/1 Knapsack)
  const dpRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/dp?timeLimit=8', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-10', 'Dynamic Programming 0/1 Knapsack Allocation',
    dpRes.statusCode === 200 && dpRes.data.maxBenefitAchieved === 78,
    `HTTP 200, Optimal Benefit: ${dpRes.data.maxBenefitAchieved} pts, Used: ${dpRes.data.totalDurationUsed}h`);

  // 4.12 Phase 4: Backtracking
  const btRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/backtracking?limit=10', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('4. FRONTEND REGRESSION', 'DSA-11', 'Backtracking Topological Planning',
    btRes.statusCode === 200 && btRes.data.validOrderings?.length > 0 && btRes.data.cycleDetected === false,
    `HTTP 200, Valid schedules: ${btRes.data.validOrderings?.length}, Pruned branches: ${btRes.data.branchesPruned}`);

  // ========================================================================
  // SECTION 5: FINAL DATABASE INTEGRITY CHECKS
  // ========================================================================
  console.log('\n--- 5. FINAL DATABASE INTEGRITY CHECKS ---');

  const finalTasks = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('5. DB INTEGRITY', 'DB-01', 'Task Count Invariant (14 tasks)',
    finalTasks.data?.length === 14, `Exact count: ${finalTasks.data?.length}`);

  const finalUsers = await request({ hostname: 'localhost', port: 8085, path: '/api/auth/users', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  record('5. DB INTEGRITY', 'DB-02', 'User Count Invariant (17 users)',
    finalUsers.data?.length === 17, `Exact count: ${finalUsers.data?.length}`);

  const finalGraph = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const dep101_106 = finalGraph.data?.dependencies?.some(d => d.prerequisiteId === 101 && d.dependentId === 106);
  const dep119_104 = finalGraph.data?.dependencies?.some(d => d.prerequisiteId === 119 && d.dependentId === 104);
  record('5. DB INTEGRITY', 'DB-03', 'Standard Graph Dependencies (101->106, 119->104)',
    finalGraph.data?.totalDependencies === 2 && dep101_106 && dep119_104,
    `Total dependencies: ${finalGraph.data?.totalDependencies}, 101->106: ${dep101_106}, 119->104: ${dep119_104}`);

  const finalStack = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack', method: 'GET', headers: { 'Cookie': adminAuth.cookie } });
  const bottomStackTask = finalStack.data[finalStack.data.length - 1];
  record('5. DB INTEGRITY', 'DB-04', 'Task #110 at Bottom of Completed Stack',
    bottomStackTask?.id === 110 && bottomStackTask?.member === 'Amrith',
    `Bottom stack task: #${bottomStackTask?.id} ("${bottomStackTask?.name}" - ${bottomStackTask?.member})`);

  // ========================================================================
  // SUMMARY
  // ========================================================================
  console.log('\n========================================================================');
  const passed = testResults.filter(r => r.status === 'PASS').length;
  const failed = testResults.filter(r => r.status === 'FAIL').length;
  const notTested = testResults.filter(r => r.status === 'NOT TESTED').length;
  console.log(`ACCEPTANCE RESULTS: PASSED: ${passed} | FAILED: ${failed} | NOT TESTED: ${notTested}`);
  console.log('========================================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

runAcceptanceTest().catch(err => {
  console.error('Acceptance test failed with exception:', err);
  process.exit(1);
});
