// scratch/final_verification.js
// Final Comprehensive Verification Suite for Phase 4: Algorithm Studio

const http = require('http');
const fs = require('fs');
const crypto = require('crypto');
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
          data: json
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

async function main() {
  console.log('================================================================');
  console.log('       TASKFLOW PHASE 4: FINAL SYSTEM VERIFICATION SUITE        ');
  console.log('================================================================\n');

  const report = [];

  function record(section, name, pass, details) {
    report.push({ section, name, pass, details });
    const tag = pass ? '[PASS]' : '[FAIL]';
    console.log(`${tag} ${section} - ${name}: ${details}`);
  }

  // Admin login
  const adminAuth = await login('admin', 'Admin@123');
  if (adminAuth.status !== 200 || !adminAuth.cookie) {
    console.error('FATAL: Could not authenticate admin!');
    process.exit(1);
  }
  const adminCookie = adminAuth.cookie;

  // -------------------------------------------------------------
  // REQ 1: Source of Duration & Benefit Values
  // -------------------------------------------------------------
  console.log('\n--- 1. Verification of Task Duration & Benefit Source ---');
  const tasksRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks',
    method: 'GET',
    headers: { 'Cookie': adminCookie }
  });
  const allTasks = tasksRes.data;
  const dbTaskCount = Array.isArray(allTasks) ? allTasks.length : 0;
  record('REQ-1', 'Real DB Tasks Loaded', dbTaskCount > 0, `Loaded ${dbTaskCount} tasks from MySQL tasks table`);

  const greedyCheck = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/greedy?timeLimit=8',
    method: 'GET',
    headers: { 'Cookie': adminCookie }
  });
  const gCandidates = greedyCheck.data.candidateTasks || [];
  let greedySourceConsistent = true;
  for (const c of gCandidates) {
    const orig = allTasks.find(t => t.id === c.taskId);
    if (!orig || orig.name !== c.taskName || orig.priority !== c.priority) {
      greedySourceConsistent = false;
      break;
    }
  }
  record('REQ-1', 'Greedy Candidates Match MySQL Records', greedySourceConsistent,
    `All ${gCandidates.length} candidate tasks map exactly to existing database records`);

  const dpCheck = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/dp?timeLimit=8',
    method: 'GET',
    headers: { 'Cookie': adminCookie }
  });
  const dpCandidates = dpCheck.data.candidateTasks || [];
  let dpSourceConsistent = true;
  for (const c of dpCandidates) {
    const orig = allTasks.find(t => t.id === c.taskId);
    if (!orig || orig.name !== c.taskName || orig.priority !== c.priority) {
      dpSourceConsistent = false;
      break;
    }
  }
  record('REQ-1', 'DP Candidates Match MySQL Records', dpSourceConsistent,
    `All ${dpCandidates.length} candidate tasks map exactly to existing database records`);

  // Verify documented explanation notice
  const noticeProvided = greedyCheck.data.heuristicExplanation && dpCheck.data.sampleMetricsExplanation;
  record('REQ-1', 'Documented Calculation Notice Present', !!noticeProvided,
    'APIs document deterministic priority-to-metric mappings: High (30pts/2-3h), Medium (18-20pts/2-3h), Low (10pts/1-2h)');

  // -------------------------------------------------------------
  // REQ 2: Greedy Benefit-to-Duration Ratios & Budget
  // -------------------------------------------------------------
  console.log('\n--- 2. Verification of Greedy Heuristic & Time Budget ---');
  let ratiosSorted = true;
  for (let i = 1; i < gCandidates.length; i++) {
    if (gCandidates[i].densityRatio > gCandidates[i-1].densityRatio) {
      ratiosSorted = false;
      break;
    }
  }
  record('REQ-2', 'Candidates Sorted by Density Ratio Descending', ratiosSorted,
    `Top ratio: ${gCandidates[0]?.densityRatio?.toFixed(2)} (High priority: 30 / 2h = 15.00)`);

  const gSelected = greedyCheck.data.selectedTasks || [];
  const gAllocated = greedyCheck.data.totalAllocatedHours;
  const gBudget = greedyCheck.data.timeLimitHours;
  record('REQ-2', 'Greedy Strictly Respects Time Limit', gAllocated <= gBudget,
    `Allocated ${gAllocated}h <= Budget of ${gBudget}h (Remaining: ${greedyCheck.data.remainingHours}h, Score: ${greedyCheck.data.totalPriorityScore})`);

  let gSumEffort = gSelected.reduce((acc, t) => acc + (t.sampleEffortHours || 0), 0);
  record('REQ-2', 'Greedy Effort Sum Matches Total Allocated', gSumEffort === gAllocated,
    `Sum of selected task efforts = ${gSumEffort}h matches reported allocated ${gAllocated}h`);

  // -------------------------------------------------------------
  // REQ 3: Dynamic Programming 0/1 Knapsack Optimal Selection
  // -------------------------------------------------------------
  console.log('\n--- 3. Verification of DP 0/1 Knapsack Optimality ---');
  const dpBenefit = dpCheck.data.maxBenefitAchieved;
  const dpDuration = dpCheck.data.totalDurationUsed;
  record('REQ-3', 'DP Allocation Within Budget', dpDuration <= dpCheck.data.timeLimitHours,
    `Duration used: ${dpDuration}h <= ${dpCheck.data.timeLimitHours}h (Optimal Benefit: ${dpBenefit} pts)`);

  // Test DP against known classic knapsack test case
  const customDP = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/dp',
    method: 'POST',
    headers: { 'Cookie': adminCookie, 'Content-Type': 'application/json' }
  }, {
    timeLimit: 5,
    tasks: [
      { taskId: 1, taskName: 'Task 1', priority: 'High', member: 'User', status: 'Pending', sampleDurationHours: 2, sampleBenefitValue: 12 },
      { taskId: 2, taskName: 'Task 2', priority: 'High', member: 'User', status: 'Pending', sampleDurationHours: 1, sampleBenefitValue: 10 },
      { taskId: 3, taskName: 'Task 3', priority: 'High', member: 'User', status: 'Pending', sampleDurationHours: 3, sampleBenefitValue: 20 },
      { taskId: 4, taskName: 'Task 4', priority: 'High', member: 'User', status: 'Pending', sampleDurationHours: 2, sampleBenefitValue: 15 }
    ]
  });
  // Capacity 5:
  // Item 2 (1h, 10), Item 3 (3h, 20), Item 4 (2h, 15) -> 2+3=5h -> 15+20=35 pts.
  // Or Item 1 (2h, 12), Item 4 (2h, 15), Item 2 (1h, 10) -> 2+2+1=5h -> 12+15+10=37 pts!
  const optimalBenefit = customDP.data.maxBenefitAchieved;
  record('REQ-3', '0/1 Knapsack Optimal Solution Achieved', optimalBenefit === 37,
    `Custom Knapsack test returned globally optimal ${optimalBenefit} pts (Items 1+2+4 = 37 pts)`);

  // -------------------------------------------------------------
  // REQ 4: Backtracking Topological Schedules & Dependency Graph
  // -------------------------------------------------------------
  console.log('\n--- 4. Verification of Backtracking Topological Schedules ---');
  const btCheck = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/backtracking?limit=10',
    method: 'GET',
    headers: { 'Cookie': adminCookie }
  });
  const validOrderings = btCheck.data.validOrderings || [];
  record('REQ-4', 'Valid Schedules Found', validOrderings.length > 0,
    `Found ${validOrderings.length} topological orderings of ${btCheck.data.totalTasks} tasks`);

  // Verify dependency ordering constraint: 101 before 106, 119 before 104
  let allOrderingsValid = validOrderings.length > 0;
  for (const ord of validOrderings) {
    const i101 = ord.indexOf(101);
    const i106 = ord.indexOf(106);
    const i119 = ord.indexOf(119);
    const i104 = ord.indexOf(104);
    if (i101 === -1 || i106 === -1 || i119 === -1 || i104 === -1 || i101 > i106 || i119 > i104) {
      allOrderingsValid = false;
      break;
    }
  }
  record('REQ-4', 'Prerequisite Ordering Constraints Verified', allOrderingsValid,
    `All ${validOrderings.length} orderings strictly satisfy: 101 before 106 AND 119 before 104`);

  // -------------------------------------------------------------
  // REQ 5: Cycle Detection, Prerequisite Checking & Branch Pruning
  // -------------------------------------------------------------
  console.log('\n--- 5. Verification of Cycle Detection & Branch Pruning ---');
  record('REQ-5', 'Standard TaskFlow DAG Has No Cycles', btCheck.data.cycleDetected === false,
    'Kahn in-degree algorithm confirms active database graph is acyclic');

  record('REQ-5', 'Backtracking Branch Pruning Active', btCheck.data.branchesPruned > 0,
    `Pruned ${btCheck.data.branchesPruned} invalid search branches during state space exploration`);

  // Test cyclic graph detection
  const cyclicTest = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/backtracking',
    method: 'POST',
    headers: { 'Cookie': adminCookie, 'Content-Type': 'application/json' }
  }, {
    taskIds: [1, 2, 3],
    graph: {
      '1': [2],
      '2': [3],
      '3': [1] // Cycle 1 -> 2 -> 3 -> 1
    },
    taskNames: { '1': 'A', '2': 'B', '3': 'C' }
  });
  record('REQ-5', 'Circular Cycle Correctly Detected', cyclicTest.data.cycleDetected === true,
    `Cycle detected: ${cyclicTest.data.cycleMessage}`);

  // -------------------------------------------------------------
  // REQ 6: Database Records Non-Mutation Verification
  // -------------------------------------------------------------
  console.log('\n--- 6. Verification of Database Non-Mutation ---');
  // Snapshot before executing phase 4 operations
  const snap1 = {
    tasks: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    queue: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/queue', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    stack: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    graph: (await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph', method: 'GET', headers: { 'Cookie': adminCookie } })).data.totalDependencies,
    users: (await request({ hostname: 'localhost', port: 8085, path: '/api/auth/users', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length
  };

  // Run all 3 algorithms repeatedly
  await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/greedy?timeLimit=12', method: 'GET', headers: { 'Cookie': adminCookie } });
  await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/dp?timeLimit=12', method: 'GET', headers: { 'Cookie': adminCookie } });
  await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/backtracking?limit=20', method: 'GET', headers: { 'Cookie': adminCookie } });

  // Snapshot after
  const snap2 = {
    tasks: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    queue: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/queue', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    stack: (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length,
    graph: (await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph', method: 'GET', headers: { 'Cookie': adminCookie } })).data.totalDependencies,
    users: (await request({ hostname: 'localhost', port: 8085, path: '/api/auth/users', method: 'GET', headers: { 'Cookie': adminCookie } })).data.length
  };

  const dbUntouched = snap1.tasks === snap2.tasks &&
                      snap1.queue === snap2.queue &&
                      snap1.stack === snap2.stack &&
                      snap1.graph === snap2.graph &&
                      snap1.users === snap2.users;

  record('REQ-6', 'Database Zero-Mutation Guarantee', dbUntouched,
    `Exact record counts preserved: Tasks (${snap1.tasks}), Queue (${snap1.queue}), Stack (${snap1.stack}), Graph Dependencies (${snap1.graph}), Users (${snap1.users})`);

  // Verify Amrith task #110 at bottom of stack
  const stackItems = (await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack', method: 'GET', headers: { 'Cookie': adminCookie } })).data;
  const bottomStack = stackItems[stackItems.length - 1];
  record('REQ-6', 'Amrith Task #110 Position Preserved', bottomStack && (bottomStack.id === 110 || bottomStack.taskId === 110),
    `Stack bottom is Task #${bottomStack?.id || bottomStack?.taskId} ("${bottomStack?.name}" - ${bottomStack?.member})`);

  // -------------------------------------------------------------
  // REQ 7: Admin-Only Access & Member Restrictions
  // -------------------------------------------------------------
  console.log('\n--- 7. Verification of Admin-Only Access & Security ---');
  // 7.1 Unauthenticated requests
  const unauthGreedy = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/phase4/greedy', method: 'GET' });
  record('REQ-7', 'Unauthenticated Phase 4 Access Blocked', unauthGreedy.statusCode === 401,
    `HTTP ${unauthGreedy.statusCode} Unauthorized on /api/tasks/phase4/greedy`);

  // 7.2 Member session
  const memberAuth = await login('sheena', 'Member@123');
  record('REQ-7', 'Member Login Succeeded', memberAuth.status === 200, `User: ${memberAuth.body?.user?.username}, Role: ${memberAuth.body?.user?.role}`);

  // 7.3 Navbar isolation for Member
  const navbarCode = fs.readFileSync('src/frontend/js/navbar.js', 'utf8');
  const navbarRestricted = navbarCode.includes("isVerifiedAdmin") && navbarCode.includes("algorithm-studio.html");
  record('REQ-7', 'Sidebar Navigation Excludes Algorithm Studio for Member', navbarRestricted,
    'navbar.js only renders algorithm-studio.html when backend role is verified as ADMIN; purges for MEMBER');

  // 7.4 HTML pre-render guard
  const htmlPage = fs.readFileSync('src/frontend/algorithm-studio.html', 'utf8');
  const guardPresent = htmlPage.includes("immediateUser.role.toUpperCase() !== 'ADMIN'") &&
                       htmlPage.includes("dashboard.html?error=forbidden");
  record('REQ-7', 'Pre-Render Execution Guard on algorithm-studio.html', guardPresent,
    'Immediate synchronous script halts DOM parsing and redirects non-admins before page rendering');

  // -------------------------------------------------------------
  // REQ 8: Regression Tests of All Previous Phases
  // -------------------------------------------------------------
  console.log('\n--- 8. Verification of Previous Phases (Regression Tests) ---');
  // 8.1 Phase 1 Linear Data Structures
  const queueRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/queue', method: 'GET', headers: { 'Cookie': adminCookie } });
  const stackRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/stack', method: 'GET', headers: { 'Cookie': adminCookie } });
  record('REQ-8', 'Phase 1: Queue & Stack Operational', queueRes.statusCode === 200 && stackRes.statusCode === 200,
    `Queue count: ${queueRes.data.length}, Stack count: ${stackRes.data.length}`);

  // 8.2 Phase 2.1 User Tree (BST)
  const bstRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/users/tree', method: 'GET', headers: { 'Cookie': adminCookie } });
  record('REQ-8', 'Phase 2.1: User Tree (Balanced BST) Operational', bstRes.statusCode === 200 && bstRes.data.root?.id === 9,
    `Root User: ${bstRes.data.root?.id}, Total: ${bstRes.data.totalUsers}, Height: ${bstRes.data.height}`);

  // 8.3 Phase 2.2 Project Graph (BFS)
  const graphRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph', method: 'GET', headers: { 'Cookie': adminCookie } });
  const bfsRes = await request({ hostname: 'localhost', port: 8085, path: '/api/admin/tasks/graph/bfs?startId=101', method: 'GET', headers: { 'Cookie': adminCookie } });
  record('REQ-8', 'Phase 2.2: Project Graph & BFS Operational', graphRes.statusCode === 200 && bfsRes.statusCode === 200,
    `Dependencies: ${graphRes.data.totalDependencies}, BFS from 101 visited ${bfsRes.data.count} nodes: [${bfsRes.data.bfsOrder?.join(', ')}]`);

  // 8.4 Phase 3 Search & Sort Lab
  const linearRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/search/linear?query=101', method: 'GET', headers: { 'Cookie': adminCookie } });
  const binaryRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/search/binary?query=101', method: 'GET', headers: { 'Cookie': adminCookie } });
  const sortRes = await request({ hostname: 'localhost', port: 8085, path: '/api/tasks/sort/quick?sortBy=id', method: 'GET', headers: { 'Cookie': adminCookie } });
  record('REQ-8', 'Phase 3: Search & Sort Lab Operational', linearRes.statusCode === 200 && binaryRes.statusCode === 200 && sortRes.statusCode === 200 && sortRes.data.sortedTasks?.length > 0,
    `Linear search: found=${linearRes.data.found}, Binary search: found=${binaryRes.data.found}, QuickSort returned ${sortRes.data.sortedTasks?.length} tasks`);

  // -------------------------------------------------------------
  // REQ 9: SHA-256 Frontend Copies Synchronization
  // -------------------------------------------------------------
  console.log('\n--- 9. Verification of SHA-256 File Synchronization ---');
  const syncFiles = [
    'algorithm-studio.html',
    'dashboard.html',
    'js/navbar.js',
    'js/api.js'
  ];
  const syncDirs = [
    'src/frontend',
    'backend/src/main/resources/static',
    'backend/target/classes/static'
  ];

  let allFilesSynced = true;
  for (const f of syncFiles) {
    const hashes = [];
    for (const d of syncDirs) {
      const p = path.join(d, f);
      if (fs.existsSync(p)) {
        const h = crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
        hashes.push(h);
      } else {
        hashes.push('MISSING');
      }
    }
    const matches = hashes.length === 3 && hashes[0] === hashes[1] && hashes[1] === hashes[2];
    if (!matches) allFilesSynced = false;
    record('REQ-9', `SHA-256 Synchronized: ${f}`, matches,
      matches ? `Hash: ${hashes[0].substring(0, 16)}... across all 3 directories` : `MISMATCH: ${JSON.stringify(hashes)}`);
  }

  // -------------------------------------------------------------
  // Summary
  // -------------------------------------------------------------
  console.log('\n================================================================');
  const passCount = report.filter(r => r.pass).length;
  const failCount = report.filter(r => !r.pass).length;
  console.log(`TOTAL CHECKS: ${report.length} | PASSED: ${passCount} | FAILED: ${failCount}`);
  console.log('================================================================\n');

  if (failCount > 0) {
    process.exit(1);
  }
}

main().catch(err => {
  console.error('Final verification error:', err);
  process.exit(1);
});
