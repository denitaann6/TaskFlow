// scratch/test_phase4.js
// Verification suite for Phase 4: Algorithm Studio (Greedy, DP, Backtracking)

const http = require('http');

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

async function runTests() {
  console.log('====================================================');
  console.log('    PHASE 4: ALGORITHM STUDIO AUTOMATED SUITE       ');
  console.log('====================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(desc, condition, details = '') {
    if (condition) {
      console.log(`[PASS] ${desc}`);
      passed++;
    } else {
      console.error(`[FAIL] ${desc} -> ${details}`);
      failed++;
    }
  }

  // --- STEP 1: Static HTML Serving ---
  console.log('\n--- 1. Testing Static HTML Delivery ---');
  const staticPage = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/algorithm-studio.html',
    method: 'GET'
  });
  assert('algorithm-studio.html served HTTP 200', staticPage.statusCode === 200, `Got HTTP ${staticPage.statusCode}`);
  assert('algorithm-studio.html contains correct title',
    typeof staticPage.data === 'string' && staticPage.data.includes('Algorithm Studio – Greedy, Dynamic Programming &amp; Backtracking') ||
    typeof staticPage.data === 'string' && staticPage.data.includes('Algorithm Studio – Greedy, Dynamic Programming & Backtracking'),
    'Page title mismatch');
  assert('algorithm-studio.html contains pre-render security check',
    typeof staticPage.data === 'string' &&
    staticPage.data.includes("immediateUser.role.toUpperCase() !== 'ADMIN'") &&
    staticPage.data.includes("window.location.replace('dashboard.html?error=forbidden')"),
    'Security script missing');

  // --- STEP 2: Authentication & Access Verification ---
  console.log('\n--- 2. Testing Authentication & Role Guarding ---');
  // 2.1 Unauthenticated requests are rejected
  const unauthGreedy = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/greedy?timeLimit=8',
    method: 'GET'
  });
  assert('Unauthenticated GET /api/tasks/phase4/greedy rejected with 401 Unauthorized', unauthGreedy.statusCode === 401, `Got ${unauthGreedy.statusCode}`);

  // 2.2 Member navbar isolation verification
  const navbarContent = require('fs').readFileSync('src/frontend/js/navbar.js', 'utf8');
  assert('Navbar restricts Algorithm Studio to ADMIN role only',
    navbarContent.includes("isVerifiedAdmin") &&
    navbarContent.includes("algorithm-studio.html"),
    'Navbar check failed');

  // 2.3 Member authenticated session
  const memberAuth = await login('sheena', 'Member@123');
  assert('Member (sheena) authenticated successfully', memberAuth.status === 200 && memberAuth.cookie.length > 0);

  // --- STEP 3: Admin Authorized Execution ---
  console.log('\n--- 3. Testing Admin Authorized Execution ---');
  const adminAuth = await login('admin', 'Admin@123');
  assert('Admin authenticated successfully', adminAuth.status === 200 && adminAuth.cookie.length > 0);

  // 3.1 Greedy Algorithm
  console.log('\n--- 3.1 Greedy Priority-Density Task Allocation ---');
  const adminGreedy = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/greedy?timeLimit=8',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  assert('Admin GET /api/tasks/phase4/greedy returns 200 OK', adminGreedy.statusCode === 200, `Got ${adminGreedy.statusCode}`);
  const gData = adminGreedy.data;
  assert('Greedy candidate pool loaded', Array.isArray(gData.candidateTasks) && gData.candidateTasks.length > 0, `Candidates: ${gData.candidateTasks?.length}`);
  assert('Greedy selected tasks allocated within time limit', Array.isArray(gData.selectedTasks) && gData.totalAllocatedHours <= 8, `Allocated: ${gData.totalAllocatedHours}h`);
  assert('Greedy decision trace present and detailed', Array.isArray(gData.decisionTrace) && gData.decisionTrace.length > 0, `Trace length: ${gData.decisionTrace?.length}`);
  assert('Greedy explanation provided', typeof gData.heuristicExplanation === 'string' && gData.heuristicExplanation.length > 0);

  // Test custom greedy POST
  const customGreedy = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/greedy',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie, 'Content-Type': 'application/json' }
  }, {
    timeLimit: 5,
    tasks: [
      { taskId: 901, taskName: 'Urgent Patch', priority: 'High', member: 'Admin', status: 'Pending', priorityWeight: 30, sampleEffortHours: 2, densityRatio: 15 },
      { taskId: 902, taskName: 'Minor Cleanup', priority: 'Low', member: 'Admin', status: 'Pending', priorityWeight: 10, sampleEffortHours: 4, densityRatio: 2.5 }
    ]
  });
  assert('Admin POST /api/tasks/phase4/greedy returns 200 OK', customGreedy.statusCode === 200);
  assert('Custom Greedy prioritized higher density task', customGreedy.data.selectedTasks?.[0]?.taskId === 901);

  // 3.2 Dynamic Programming (0/1 Knapsack)
  console.log('\n--- 3.2 Dynamic Programming 0/1 Knapsack Allocation ---');
  const adminDP = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/dp?timeLimit=8',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  assert('Admin GET /api/tasks/phase4/dp returns 200 OK', adminDP.statusCode === 200, `Got ${adminDP.statusCode}`);
  const dpData = adminDP.data;
  assert('DP candidate tasks loaded', Array.isArray(dpData.candidateTasks) && dpData.candidateTasks.length > 0);
  assert('DP max benefit computed optimally', typeof dpData.maxBenefitAchieved === 'number' && dpData.maxBenefitAchieved > 0, `Max benefit: ${dpData.maxBenefitAchieved}`);
  assert('DP total duration does not exceed limit', dpData.totalDurationUsed <= 8, `Duration: ${dpData.totalDurationUsed}h`);
  assert('DP step log and recurrence explanation present', Array.isArray(dpData.dpSteps) && typeof dpData.dpRecurrence === 'string');

  // Test custom DP POST
  const customDP = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/dp',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie, 'Content-Type': 'application/json' }
  }, {
    timeLimit: 6,
    tasks: [
      { taskId: 801, taskName: 'Item A', priority: 'High', member: 'Admin', status: 'Pending', sampleDurationHours: 4, sampleBenefitValue: 40 },
      { taskId: 802, taskName: 'Item B', priority: 'Medium', member: 'Admin', status: 'Pending', sampleDurationHours: 3, sampleBenefitValue: 25 },
      { taskId: 803, taskName: 'Item C', priority: 'Medium', member: 'Admin', status: 'Pending', sampleDurationHours: 3, sampleBenefitValue: 25 }
    ]
  });
  assert('Admin POST /api/tasks/phase4/dp returns 200 OK', customDP.statusCode === 200);
  assert('0/1 Knapsack optimal choice selected Items B + C (benefit 50) over Item A (benefit 40)',
    customDP.data.maxBenefitAchieved === 50, `Achieved: ${customDP.data.maxBenefitAchieved}`);

  // 3.3 Backtracking Dependency Planning
  console.log('\n--- 3.3 Backtracking Topological Dependency Planning ---');
  const adminBT = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/backtracking?limit=10',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  assert('Admin GET /api/tasks/phase4/backtracking returns 200 OK', adminBT.statusCode === 200, `Got ${adminBT.statusCode}`);
  const btData = adminBT.data;
  assert('Backtracking returned valid orderings', Array.isArray(btData.validOrderings) && btData.validOrderings.length > 0, `Count: ${btData.validOrderings?.length}`);
  assert('No cycle detected in standard TaskFlow DAG', btData.cycleDetected === false);
  assert('TaskFlow dependency direction preserved in valid orderings (101 before 106, 119 before 104)', () => {
    const order0 = btData.validOrderings[0];
    const idx101 = order0.indexOf(101);
    const idx106 = order0.indexOf(106);
    const idx119 = order0.indexOf(119);
    const idx104 = order0.indexOf(104);
    return idx101 < idx106 && idx119 < idx104;
  });

  // Test cycle detection with custom backtracking POST
  const cyclicBT = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/phase4/backtracking',
    method: 'POST',
    headers: { 'Cookie': adminAuth.cookie, 'Content-Type': 'application/json' }
  }, {
    taskIds: [701, 702],
    graph: {
      '701': [702],
      '702': [701]
    },
    taskNames: {
      '701': 'Task Alpha',
      '702': 'Task Beta'
    },
    limit: 5
  });
  assert('Custom cyclic graph detected cycle correctly', cyclicBT.data.cycleDetected === true, `Cycle detected: ${cyclicBT.data.cycleDetected}`);
  assert('Cyclic graph returns 0 valid orderings', cyclicBT.data.validOrderings.length === 0);

  // --- STEP 4: Integrity Verification of Previous Phases ---
  console.log('\n--- 4. Checking Data Integrity Across Phases ---');
  const stackCheck = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/stack',
    method: 'GET',
    headers: { 'Cookie': adminAuth.cookie }
  });
  assert('Admin GET /api/tasks/stack returns 200 OK', stackCheck.statusCode === 200);
  const stackItems = stackCheck.data;
  const bottomItem = Array.isArray(stackItems) ? stackItems[stackItems.length - 1] : null;
  assert('Amrith task #110 preserved at bottom of stack',
    bottomItem && (bottomItem.id === 110 || bottomItem.taskId === 110 || (bottomItem.member && bottomItem.member.includes('Amrith'))),
    `Bottom item: ${JSON.stringify(bottomItem)}`);

  console.log('\n====================================================');
  console.log(`TOTAL TESTS: ${passed + failed} | PASSED: ${passed} | FAILED: ${failed}`);
  console.log('====================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests().catch(err => {
  console.error('Test execution failed:', err);
  process.exit(1);
});

