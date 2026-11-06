// scratch/test_phase2_2.js
// Verification of Phase 2.2: Basic Graph Representation and BFS Traversal

const http = require('http');

const BASE_URL = 'http://localhost:8085';

function request(options, postData = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
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
  return setCookie.map((c) => c.split(';')[0]).join('; ');
}

async function login(username, password) {
  const res = await request(
    {
      hostname: 'localhost',
      port: 8085,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
    { username, password }
  );
  return {
    status: res.statusCode,
    cookie: parseCookies(res),
    body: res.data,
  };
}

async function runTests() {
  console.log('====================================================');
  console.log('   TaskFlow Phase 2.2 Live System Verification      ');
  console.log('====================================================\n');

  let passes = 0;
  let fails = 0;

  function assert(condition, message, details = '') {
    if (condition) {
      console.log(`[PASS] ${message}`);
      passes++;
    } else {
      console.error(`[FAIL] ${message} - ${details}`);
      fails++;
    }
  }

  // 1. Verify Authentication & Sessions
  console.log('--- 1. Authentication ---');
  const adminLogin = await login('admin', 'Admin@123');
  assert(adminLogin.status === 200, 'Admin login succeeded', JSON.stringify(adminLogin.body));
  const adminCookie = adminLogin.cookie;

  const memberLogin = await login('sheena', 'Member@123');
  assert(memberLogin.status === 200, 'Member login (sheena) succeeded', JSON.stringify(memberLogin.body));
  const memberCookie = memberLogin.cookie;

  // 2. Unauthenticated and Member Security Checks
  console.log('\n--- 2. Security & Role Permissions (/api/admin/tasks/graph) ---');
  const unauthRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph',
    method: 'GET',
  });
  assert(unauthRes.statusCode === 401, 'Unauthenticated request to GET /api/admin/tasks/graph rejected with 401', `Status: ${unauthRes.statusCode}`);

  const memberGraphRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph',
    method: 'GET',
    headers: { Cookie: memberCookie },
  });
  assert(memberGraphRes.statusCode === 403, 'Member request to GET /api/admin/tasks/graph rejected with 403', `Status: ${memberGraphRes.statusCode}`);

  const memberAddRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=102',
    method: 'POST',
    headers: { Cookie: memberCookie },
  });
  assert(memberAddRes.statusCode === 403, 'Member request to POST dependency rejected with 403', `Status: ${memberAddRes.statusCode}`);

  const memberBfsRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/bfs?startId=101',
    method: 'GET',
    headers: { Cookie: memberCookie },
  });
  assert(memberBfsRes.statusCode === 403, 'Member request to GET BFS rejected with 403', `Status: ${memberBfsRes.statusCode}`);

  // 3. Admin Access & Initial Graph State
  console.log('\n--- 3. Admin Project Graph Operations ---');
  const adminGraphRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph',
    method: 'GET',
    headers: { Cookie: adminCookie },
  });
  assert(adminGraphRes.statusCode === 200, 'Admin can access GET /api/admin/tasks/graph', `Status: ${adminGraphRes.statusCode}`);
  assert(typeof adminGraphRes.data.adjacencyList === 'object', 'Adjacency list returned as object');
  assert(Array.isArray(adminGraphRes.data.tasks), 'Task nodes list returned');
  console.log(`Initial graph: ${adminGraphRes.data.totalTasks} tasks, ${adminGraphRes.data.totalDependencies} dependencies`);

  // Clear any existing graph dependencies for reproducible test
  await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/clear',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });

  // 4. Invalid Dependency Validation
  console.log('\n--- 4. Dependency Validation Checks ---');
  // 4a. Self-dependency
  const selfDepRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=101',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(selfDepRes.statusCode === 400, 'Self-dependency (101 -> 101) rejected with 400 Bad Request', `Status: ${selfDepRes.statusCode}`);

  // 4b. Nonexistent task ID
  const nonExistRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=99999',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(nonExistRes.statusCode === 404, 'Nonexistent task (101 -> 99999) rejected with 404 Not Found', `Status: ${nonExistRes.statusCode}`);

  // 5. Valid Dependency Creation
  console.log('\n--- 5. Directed Dependency Creation & Adjacency List ---');
  const add1 = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=102',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(add1.statusCode === 200 && add1.data.success === true, 'Successfully added edge 101 -> 102', JSON.stringify(add1.data));

  // 5b. Duplicate edge rejection
  const dupRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=102',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(dupRes.statusCode === 400, 'Duplicate dependency edge (101 -> 102) rejected with 400 Bad Request', `Status: ${dupRes.statusCode}`);

  // Add more edges: 101 -> 103 and 102 -> 104
  const add2 = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=101&toId=103',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(add2.statusCode === 200, 'Successfully added edge 101 -> 103');

  const add3 = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=102&toId=104',
    method: 'POST',
    headers: { Cookie: adminCookie },
  });
  assert(add3.statusCode === 200, 'Successfully added edge 102 -> 104');

  // Verify Graph State
  const graphAfterAdd = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph',
    method: 'GET',
    headers: { Cookie: adminCookie },
  });
  const adjList = graphAfterAdd.data.adjacencyList;
  assert(adjList['101'] && adjList['101'].includes(102) && adjList['101'].includes(103), 'Adjacency list contains 101 -> [102, 103]', JSON.stringify(adjList['101']));
  assert(adjList['102'] && adjList['102'].includes(104), 'Adjacency list contains 102 -> [104]', JSON.stringify(adjList['102']));
  assert(graphAfterAdd.data.totalDependencies === 3, `Total dependencies is 3 (actual: ${graphAfterAdd.data.totalDependencies})`);

  // 6. BFS Traversal
  console.log('\n--- 6. BFS Graph Traversal ---');
  const bfsRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/bfs?startId=101',
    method: 'GET',
    headers: { Cookie: adminCookie },
  });
  assert(bfsRes.statusCode === 200, 'BFS from task 101 succeeded with 200 OK');
  const bfsOrder = bfsRes.data.bfsOrder;
  console.log('BFS Order from 101:', bfsOrder);
  assert(bfsOrder[0] === 101, 'BFS starts at root 101');
  assert((bfsOrder[1] === 102 && bfsOrder[2] === 103) || (bfsOrder[1] === 103 && bfsOrder[2] === 102), 'BFS Level 1 visits 102 and 103');
  assert(bfsOrder[3] === 104, 'BFS Level 2 visits 104');
  assert(bfsRes.data.count === 4, `BFS visited task count is 4 (actual: ${bfsRes.data.count})`);

  // 7. Remove Edge
  console.log('\n--- 7. Dependency Edge Removal ---');
  const delRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/dependency?fromId=102&toId=104',
    method: 'DELETE',
    headers: { Cookie: adminCookie },
  });
  assert(delRes.statusCode === 200 && delRes.data.success === true, 'Successfully removed edge 102 -> 104');

  // Verify removed
  const bfsAfterDel = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/tasks/graph/bfs?startId=101',
    method: 'GET',
    headers: { Cookie: adminCookie },
  });
  assert(!bfsAfterDel.data.bfsOrder.includes(104), 'BFS from 101 no longer reaches 104 after edge deletion');

  // 8. Frontend Assets & Static Resources Verification
  console.log('\n--- 8. Frontend Assets & Static Servicing ---');
  const pageRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/project-graph.html',
    method: 'GET',
  });
  assert(pageRes.statusCode === 200, 'project-graph.html served successfully via Spring Boot');
  assert(typeof pageRes.data === 'string' && pageRes.data.includes('Project Graph &amp; Task Dependencies'), 'project-graph.html contains Project Graph &amp; Task Dependencies heading');

  // 9. Data Integrity Verification (Phase 1 Stack & Amrith #110)
  console.log('\n--- 9. Data Integrity: Completed Task Stack (Amrith #110) ---');
  const stackRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/stack',
    method: 'GET',
    headers: { Cookie: adminCookie },
  });
  assert(stackRes.statusCode === 200, 'GET /api/tasks/stack succeeded with 200');
  const stackList = stackRes.data;
  assert(Array.isArray(stackList) && stackList.length >= 2, `Stack has at least 2 tasks (actual: ${stackList.length})`);
  const bottomItem = stackList[stackList.length - 1]; // bottom of LIFO display is last element
  assert(bottomItem.id === 110, `Bottom of active stack is Amrith's task #110 (actual ID: ${bottomItem.id})`);
  assert(bottomItem.member === 'Amrith', `Task #110 member is Amrith (actual: ${bottomItem.member})`);

  console.log('\n====================================================');
  console.log(`TOTAL PASSES: ${passes}`);
  console.log(`TOTAL FAILS:  ${fails}`);
  console.log('====================================================\n');

  if (fails > 0) {
    process.exit(1);
  }
}

runTests().catch((err) => {
  console.error('Error during test execution:', err);
  process.exit(1);
});
