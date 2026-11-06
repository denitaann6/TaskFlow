const http = require('http');

function request(options, postData) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                let parsed = null;
                try { parsed = JSON.parse(data); } catch (e) { parsed = data; }
                resolve({ statusCode: res.statusCode, headers: res.headers, body: parsed });
            });
        });
        req.on('error', reject);
        if (postData) req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
        req.end();
    });
}

async function login(username, password) {
    const res = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/login',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    }, { username, password });

    let cookie = null;
    if (res.headers['set-cookie']) {
        cookie = res.headers['set-cookie'].map(c => c.split(';')[0]).join('; ');
    }
    return { status: res.statusCode, body: res.body, cookie };
}

async function runAudit() {
    console.log("=================================================");
    console.log("       TASKFLOW ROLE & PERMISSION AUDIT          ");
    console.log("=================================================\n");

    const results = [];

    // --- TEST 1: Authentication & Invalid Login ---
    console.log("[1] Testing Login & Authentication...");
    const badLogin = await login("baduser", "WrongPassword");
    results.push({
        test: "Login with invalid credentials",
        status: badLogin.status === 401 ? "PASS" : "FAIL",
        evidence: `HTTP ${badLogin.status}: ${JSON.stringify(badLogin.body)}`
    });

    const adminLogin = await login("admin", "Admin@123");
    results.push({
        test: "Admin Login (admin / Admin@123)",
        status: adminLogin.status === 200 && adminLogin.body.user && adminLogin.body.user.role === 'ADMIN' ? "PASS" : "FAIL",
        evidence: `HTTP ${adminLogin.status}, Role: ${adminLogin.body?.user?.role}, Cookie: ${adminLogin.cookie ? 'Acquired' : 'Missing'}`
    });

    const memberLogin = await login("sheena", "Member@123");
    results.push({
        test: "Member Login (sheena / Member@123)",
        status: memberLogin.status === 200 && memberLogin.body.user && memberLogin.body.user.role === 'MEMBER' ? "PASS" : "FAIL",
        evidence: `HTTP ${memberLogin.status}, Role: ${memberLogin.body?.user?.role}, Cookie: ${memberLogin.cookie ? 'Acquired' : 'Missing'}`
    });

    // --- TEST 2: Admin Authorized Access ---
    console.log("\n[2] Testing Admin Access to Queue and Stack...");
    const adminQueue = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/queue',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/queue",
        status: adminQueue.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminQueue.statusCode}, returned ${Array.isArray(adminQueue.body) ? adminQueue.body.length + ' items' : 'data'}`
    });

    const adminQueuePeek = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/queue/peek',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/queue/peek",
        status: adminQueuePeek.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminQueuePeek.statusCode}, visual present: ${!!adminQueuePeek.body?.visual}`
    });

    const adminStack = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/stack",
        status: adminStack.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminStack.statusCode}, returned ${Array.isArray(adminStack.body) ? adminStack.body.length + ' items' : 'data'}`
    });

    const adminStackPeek = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack/peek',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/stack/peek",
        status: adminStackPeek.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminStackPeek.statusCode}, visual present: ${!!adminStackPeek.body?.visual}`
    });

    const adminStackHistory = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack/history',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/stack/history",
        status: adminStackHistory.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminStackHistory.statusCode}, returned ${Array.isArray(adminStackHistory.body) ? adminStackHistory.body.length + ' records' : 'data'}`
    });

    const adminUsers = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/users',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/auth/users",
        status: adminUsers.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminUsers.statusCode}, returned ${Array.isArray(adminUsers.body) ? adminUsers.body.length + ' users' : 'data'}`
    });

    // --- TEST 3: Member Authorized Access ---
    console.log("\n[3] Testing Member Authorized Access...");
    const memberMe = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/me',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/auth/me",
        status: memberMe.statusCode === 200 && memberMe.body.role === 'MEMBER' ? "PASS" : "FAIL",
        evidence: `HTTP ${memberMe.statusCode}, user: ${memberMe.body?.username}, role: ${memberMe.body?.role}`
    });

    const memberStats = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stats/summary',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/stats/summary",
        status: memberStats.statusCode === 200 && memberStats.body.role === 'MEMBER' && memberStats.body.memberWorkload === undefined ? "PASS" : "FAIL",
        evidence: `HTTP ${memberStats.statusCode}, role: ${memberStats.body?.role}, personal total tasks: ${memberStats.body?.totalTasks}, memberWorkload excluded: ${memberStats.body?.memberWorkload === undefined}`
    });

    const memberTasks = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    const tasksArray = Array.isArray(memberTasks.body) ? memberTasks.body : [];
    const allAssignedToSheena = tasksArray.every(t => (t.member || '').toLowerCase() === 'sheena');
    results.push({
        test: "Member GET /api/tasks (My Tasks isolation)",
        status: memberTasks.statusCode === 200 && allAssignedToSheena ? "PASS" : "FAIL",
        evidence: `HTTP ${memberTasks.statusCode}, count: ${tasksArray.length}, all tasks assigned to Sheena: ${allAssignedToSheena}`
    });

    const memberTeam = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/team',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/auth/team (Team Directory)",
        status: memberTeam.statusCode === 200 && Array.isArray(memberTeam.body) ? "PASS" : "FAIL",
        evidence: `HTTP ${memberTeam.statusCode}, returned ${Array.isArray(memberTeam.body) ? memberTeam.body.length + ' members' : 'data'}`
    });

    const memberLL = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/linkedlist/traverse',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/linkedlist/traverse (DS Lab)",
        status: memberLL.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${memberLL.statusCode}, totalNodes: ${memberLL.body?.totalNodes}`
    });

    const memberBrackets = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/brackets/check?expression=' + encodeURIComponent('(){}[]'),
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/brackets/check (DS Lab)",
        status: memberBrackets.statusCode === 200 && memberBrackets.body?.balanced === true ? "PASS" : "FAIL",
        evidence: `HTTP ${memberBrackets.statusCode}, balanced: ${memberBrackets.body?.balanced}, message: "${memberBrackets.body?.message}"`
    });

    const memberBST = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/bst/search?id=102',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/bst/search (DS Lab)",
        status: memberBST.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${memberBST.statusCode}, found: ${memberBST.body?.found}`
    });

    const memberPhase4Greedy = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/phase4/greedy?timeLimit=8',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/phase4/greedy (DS Lab Phase 4)",
        status: memberPhase4Greedy.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${memberPhase4Greedy.statusCode}, selected tasks: ${memberPhase4Greedy.body?.selectedTasks?.length}`
    });

    const memberPhase4DP = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/phase4/dp?timeLimit=8',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/tasks/phase4/dp (DS Lab Phase 4)",
        status: memberPhase4DP.statusCode === 200 && memberPhase4DP.body?.maxBenefitAchieved !== undefined ? "PASS" : "FAIL",
        evidence: `HTTP ${memberPhase4DP.statusCode}, maxBenefitAchieved: ${memberPhase4DP.body?.maxBenefitAchieved}, selectedTasks: ${memberPhase4DP.body?.selectedTasks?.length}`
    });

    // --- TEST 4: Unauthorized Access Rejection (Admin-only endpoints) ---
    console.log("\n[4] Testing Unauthorized Member Rejection (403 Forbidden)...");
    const endpointsToTest = [
        { name: "GET /api/tasks/queue", path: "/api/tasks/queue", method: "GET" },
        { name: "GET /api/tasks/queue/peek", path: "/api/tasks/queue/peek", method: "GET" },
        { name: "POST /api/tasks/queue/sync", path: "/api/tasks/queue/sync", method: "POST" },
        { name: "POST /api/tasks/process-next", path: "/api/tasks/process-next", method: "POST" },
        { name: "GET /api/tasks/stack", path: "/api/tasks/stack", method: "GET" },
        { name: "GET /api/tasks/stack/peek", path: "/api/tasks/stack/peek", method: "GET" },
        { name: "POST /api/tasks/stack/pop", path: "/api/tasks/stack/pop", method: "POST" },
        { name: "POST /api/tasks/stack/sync", path: "/api/tasks/stack/sync", method: "POST" },
        { name: "GET /api/tasks/stack/history", path: "/api/tasks/stack/history", method: "GET" },
        { name: "GET /api/auth/users", path: "/api/auth/users", method: "GET" },
        { name: "DELETE /api/tasks/102", path: "/api/tasks/102", method: "DELETE" }
    ];

    for (const ep of endpointsToTest) {
        const res = await request({
            hostname: 'localhost',
            port: 8085,
            path: ep.path,
            method: ep.method,
            headers: { 'Cookie': memberLogin.cookie }
        });
        results.push({
            test: `Member ${ep.name} rejection`,
            status: res.statusCode === 403 ? "PASS" : "FAIL",
            evidence: `HTTP ${res.statusCode} (Expected 403): ${JSON.stringify(res.body)}`
        });
    }

    // IDOR Protection: Member reading another user's task
    const idorRead = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/102', // Task 102 belongs to Subu, not Sheena
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member IDOR: GET /api/tasks/102 (other member's task)",
        status: idorRead.statusCode === 403 ? "PASS" : "FAIL",
        evidence: `HTTP ${idorRead.statusCode} (Expected 403): ${JSON.stringify(idorRead.body)}`
    });

    // --- TEST 5: Backend Role Verification & Header Spoofing Resistance ---
    console.log("\n[5] Testing Backend Role Verification & Spoofing Defense...");
    const unauthQueue = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/queue',
        method: 'GET'
    });
    results.push({
        test: "Unauthenticated GET /api/tasks/queue rejection",
        status: unauthQueue.statusCode === 401 ? "PASS" : "FAIL",
        evidence: `HTTP ${unauthQueue.statusCode} (Expected 401): ${JSON.stringify(unauthQueue.body)}`
    });

    // Attempt role spoofing via headers with member cookie
    const spoofHeader = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/queue',
        method: 'GET',
        headers: {
            'Cookie': memberLogin.cookie,
            'X-User-Role': 'ADMIN',
            'Role': 'ADMIN'
        }
    });
    results.push({
        test: "Member spoofing role via headers on /api/tasks/queue",
        status: spoofHeader.statusCode === 403 ? "PASS" : "FAIL",
        evidence: `HTTP ${spoofHeader.statusCode} (Expected 403): ${JSON.stringify(spoofHeader.body)}`
    });

    // --- TEST 6: Session Termination (Logout) ---
    console.log("\n[6] Testing Logout & Session Invalidation...");
    const memberLogout = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/logout',
        method: 'POST',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member POST /api/auth/logout",
        status: memberLogout.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${memberLogout.statusCode}: ${JSON.stringify(memberLogout.body)}`
    });

    const memberPostLogoutMe = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/me',
        method: 'GET',
        headers: { 'Cookie': memberLogin.cookie }
    });
    results.push({
        test: "Member GET /api/auth/me after logout",
        status: memberPostLogoutMe.statusCode === 401 ? "PASS" : "FAIL",
        evidence: `HTTP ${memberPostLogoutMe.statusCode} (Expected 401): ${JSON.stringify(memberPostLogoutMe.body)}`
    });

    const adminLogout = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/logout',
        method: 'POST',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin POST /api/auth/logout",
        status: adminLogout.statusCode === 200 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminLogout.statusCode}: ${JSON.stringify(adminLogout.body)}`
    });

    const adminPostLogoutQueue = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/queue',
        method: 'GET',
        headers: { 'Cookie': adminLogin.cookie }
    });
    results.push({
        test: "Admin GET /api/tasks/queue after logout",
        status: adminPostLogoutQueue.statusCode === 401 ? "PASS" : "FAIL",
        evidence: `HTTP ${adminPostLogoutQueue.statusCode} (Expected 401): ${JSON.stringify(adminPostLogoutQueue.body)}`
    });

    // Print summary table
    console.log("\n=================================================");
    console.log("                AUDIT SUMMARY                    ");
    console.log("=================================================");
    let passCount = 0;
    let failCount = 0;
    results.forEach((r, idx) => {
        if (r.status === "PASS") passCount++;
        else failCount++;
        console.log(`[${r.status}] ${r.test}`);
        console.log(`       Evidence: ${r.evidence}`);
    });
    console.log(`\nTotal: ${results.length} | PASS: ${passCount} | FAIL: ${failCount}`);
}

runAudit().catch(console.error);
