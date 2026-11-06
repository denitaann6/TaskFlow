// scratch/test_phase3.js
// Verification of Phase 3: Searching & Sorting Operations

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
  console.log('   TaskFlow Phase 3: Search & Sort Lab Tests        ');
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

  // 1. Authentication
  console.log('--- 1. Authentication & Session Verification ---');
  const adminLogin = await login('admin', 'Admin@123');
  assert(adminLogin.status === 200, 'Admin login succeeded', JSON.stringify(adminLogin.body));
  const adminCookie = adminLogin.cookie;

  const memberLogin = await login('sheena', 'Member@123');
  assert(memberLogin.status === 200, 'Member login (sheena) succeeded', JSON.stringify(memberLogin.body));
  const memberCookie = memberLogin.cookie;

  // 2. Unauthenticated check
  console.log('\n--- 2. Unauthenticated Access Protection ---');
  const unauthRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/linear?query=test',
    method: 'GET'
  });
  assert(unauthRes.statusCode === 401, 'Unauthenticated GET /api/tasks/search/linear rejected with 401', `Status: ${unauthRes.statusCode}`);

  // 3. Linear Search Tests
  console.log('\n--- 3. Linear Search Operations ---');
  // 3a. Find by Task ID
  const linIdRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/linear?query=101&searchBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(linIdRes.statusCode === 200 && linIdRes.data.found === true, 'Linear Search finds existing task by Task ID 101', JSON.stringify(linIdRes.data));
  assert(linIdRes.data.task && linIdRes.data.task.id === 101, 'Linear Search matched task has ID 101');
  assert(linIdRes.data.comparisons >= 1, `Linear Search recorded comparisons: ${linIdRes.data.comparisons}`);

  // 3b. Find by Task Name
  const linNameRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/linear?query=Database%20Design&searchBy=name',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(linNameRes.statusCode === 200 && linNameRes.data.found === true, 'Linear Search finds existing task by Task Name "Database Design"');
  assert(linNameRes.data.task && linNameRes.data.task.id === 101, 'Linear Search matched task name maps to ID 101');

  // 3c. Nonexistent task
  const linNotFound = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/linear?query=99999&searchBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(linNotFound.statusCode === 200 && linNotFound.data.found === false, 'Linear Search handles nonexistent task ID with found=false');
  assert(linNotFound.data.task === null && linNotFound.data.index === -1, 'Linear Search returns null task and index=-1 when not found');

  // 4. Binary Search Tests
  console.log('\n--- 4. Binary Search Operations ---');
  // 4a. Find by Task ID
  const binIdRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/binary?query=101&searchBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(binIdRes.statusCode === 200 && binIdRes.data.found === true, 'Binary Search finds existing task by Task ID 101', JSON.stringify(binIdRes.data));
  assert(binIdRes.data.task && binIdRes.data.task.id === 101, 'Binary Search matched task has ID 101');
  assert(binIdRes.data.sortedKeys && Array.isArray(binIdRes.data.sortedKeys), 'Binary Search verifies pre-sorted keys requirement');

  // Verify sortedKeys are strictly sorted
  const sortedKeys = binIdRes.data.sortedKeys;
  let isSorted = true;
  for (let i = 1; i < sortedKeys.length; i++) {
    if (sortedKeys[i] < sortedKeys[i - 1]) isSorted = false;
  }
  assert(isSorted, 'Binary Search input data is confirmed strictly sorted before algorithm execution');

  // 4b. Find by Task Name
  const binNameRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/binary?query=Database%20Design&searchBy=name',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(binNameRes.statusCode === 200 && binNameRes.data.found === true, 'Binary Search finds existing task by Task Name "Database Design"');
  assert(binNameRes.data.task && binNameRes.data.task.name === 'Database Design', 'Binary Search matched task name matches query');

  // 4c. Nonexistent task
  const binNotFound = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/binary?query=99999&searchBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(binNotFound.statusCode === 200 && binNotFound.data.found === false, 'Binary Search handles nonexistent task ID with found=false');
  assert(binNotFound.data.sortedIndex === -1, 'Binary Search returns sortedIndex=-1 when not found');

  // 5. Insertion Sort Tests
  console.log('\n--- 5. Insertion Sort Operations ---');
  // 5a. Sort by Task ID
  const insIdRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/sort/insertion?sortBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(insIdRes.statusCode === 200, 'Insertion Sort by Task ID returns 200 OK');
  const insIdTasks = insIdRes.data.sortedTasks;
  assert(Array.isArray(insIdTasks) && insIdTasks.length > 0, `Insertion Sort returned ${insIdTasks.length} tasks`);
  let insIdSorted = true;
  for (let i = 1; i < insIdTasks.length; i++) {
    if (insIdTasks[i].id < insIdTasks[i - 1].id) insIdSorted = false;
  }
  assert(insIdSorted, 'Insertion Sort produces correctly ordered results by Task ID ascending');
  assert(insIdRes.data.comparisons > 0, `Insertion Sort reports valid comparison count: ${insIdRes.data.comparisons}`);

  // 5b. Sort by Task Name
  const insNameRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/sort/insertion?sortBy=name',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(insNameRes.statusCode === 200, 'Insertion Sort by Task Name returns 200 OK');
  const insNameTasks = insNameRes.data.sortedTasks;
  let insNameSorted = true;
  for (let i = 1; i < insNameTasks.length; i++) {
    if (insNameTasks[i].name.localeCompare(insNameTasks[i - 1].name) < 0) insNameSorted = false;
  }
  assert(insNameSorted, 'Insertion Sort produces correctly ordered results by Task Name alphabetically');

  // 6. Quick Sort Tests
  console.log('\n--- 6. Quick Sort Operations ---');
  // 6a. Sort by Task ID
  const qkIdRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/sort/quick?sortBy=id',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(qkIdRes.statusCode === 200, 'Quick Sort by Task ID returns 200 OK');
  const qkIdTasks = qkIdRes.data.sortedTasks;
  assert(Array.isArray(qkIdTasks) && qkIdTasks.length > 0, `Quick Sort returned ${qkIdTasks.length} tasks`);
  let qkIdSorted = true;
  for (let i = 1; i < qkIdTasks.length; i++) {
    if (qkIdTasks[i].id < qkIdTasks[i - 1].id) qkIdSorted = false;
  }
  assert(qkIdSorted, 'Quick Sort produces correctly ordered results by Task ID ascending');
  assert(qkIdRes.data.comparisons > 0, `Quick Sort reports valid comparison count: ${qkIdRes.data.comparisons}`);
  assert(qkIdRes.data.swaps >= 0, `Quick Sort reports valid swap count: ${qkIdRes.data.swaps}`);

  // 6b. Sort by Task Name
  const qkNameRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/sort/quick?sortBy=name',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(qkNameRes.statusCode === 200, 'Quick Sort by Task Name returns 200 OK');
  const qkNameTasks = qkNameRes.data.sortedTasks;
  let qkNameSorted = true;
  for (let i = 1; i < qkNameTasks.length; i++) {
    if (qkNameTasks[i].name.localeCompare(qkNameTasks[i - 1].name) < 0) qkNameSorted = false;
  }
  assert(qkNameSorted, 'Quick Sort produces correctly ordered results by Task Name alphabetically');

  // 7. Member access check
  console.log('\n--- 7. Member Access Permissions ---');
  const memberSearch = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/search/linear?query=101',
    method: 'GET',
    headers: { Cookie: memberCookie }
  });
  assert(memberSearch.statusCode === 200, 'Member can access Search & Sort Lab API endpoints');

  // 8. Static Web Page Serving
  console.log('\n--- 8. Web Page Static Servicing ---');
  const pageRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/search-sort.html',
    method: 'GET'
  });
  assert(pageRes.statusCode === 200, 'search-sort.html served with 200 OK');
  assert(typeof pageRes.data === 'string' && pageRes.data.includes('Search &amp; Sort Lab'), 'search-sort.html contains Search & Sort Lab title');
  assert(pageRes.data.includes('Linear Search') && pageRes.data.includes('Binary Search'), 'search-sort.html contains Searching section');
  assert(pageRes.data.includes('Insertion Sort') && pageRes.data.includes('Quick Sort'), 'search-sort.html contains Sorting section');
  assert(pageRes.data.includes('Conceptual Performance Comparison'), 'search-sort.html contains Performance Comparison section');

  // 9. Data Integrity: Completed Task Stack (Amrith #110)
  console.log('\n--- 9. Data Integrity Verification (Amrith #110) ---');
  const stackRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/tasks/stack',
    method: 'GET',
    headers: { Cookie: adminCookie }
  });
  assert(stackRes.statusCode === 200, 'GET /api/tasks/stack succeeded with 200');
  const stackList = stackRes.data;
  assert(Array.isArray(stackList) && stackList.length >= 2, `Stack has at least 2 tasks (actual: ${stackList.length})`);
  const bottomItem = stackList[stackList.length - 1];
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

runTests().catch(err => {
  console.error('Error during test execution:', err);
  process.exit(1);
});

