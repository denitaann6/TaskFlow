const http = require('http');

function request(options, postData) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                let parsed = null;
                try { parsed = JSON.parse(data); } catch (e) { parsed = data; }
                resolve({ statusCode: res.statusCode, headers: res.headers, body: parsed, rawBody: data });
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

async function run() {
    console.log("=================================================");
    console.log("       PHASE 2.1 USER BST LIVE VERIFICATION       ");
    console.log("=================================================\n");

    const checks = [];

    // 1. Static Resource Delivery
    console.log("[1] Checking static user-tree.html delivery...");
    const staticPage = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/user-tree.html',
        method: 'GET'
    });
    checks.push({
        name: "Static /user-tree.html served via Tomcat",
        pass: staticPage.statusCode === 200 && staticPage.rawBody.includes("User Tree &amp; Binary Search Tree (BST)"),
        details: `HTTP ${staticPage.statusCode}, size: ${staticPage.rawBody.length} bytes, guard present: ${staticPage.rawBody.includes("error=forbidden")}`
    });

    // 2. Authentication
    console.log("\n[2] Logging in as ADMIN and MEMBER...");
    const admin = await login("admin", "Admin@123");
    const member = await login("sheena", "Member@123");

    checks.push({
        name: "ADMIN Login",
        pass: admin.status === 200 && admin.body?.user?.role === 'ADMIN',
        details: `HTTP ${admin.status}, Role: ${admin.body?.user?.role}`
    });
    checks.push({
        name: "MEMBER Login",
        pass: member.status === 200 && member.body?.user?.role === 'MEMBER',
        details: `HTTP ${member.status}, Role: ${member.body?.user?.role}`
    });

    // 3. ADMIN User BST Endpoints
    console.log("\n[3] Testing ADMIN Access to User BST Endpoints...");

    // Tree Summary
    const treeRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    const hasHashInTree = treeRes.rawBody.includes("password") || treeRes.rawBody.includes("hash");
    checks.push({
        name: "ADMIN GET /api/admin/users/tree",
        pass: treeRes.statusCode === 200 && treeRes.body?.totalUsers === 17 && treeRes.body?.height === 5 && treeRes.body?.leafCount === 8 && !hasHashInTree,
        details: `HTTP ${treeRes.statusCode}, totalUsers: ${treeRes.body?.totalUsers}, height: ${treeRes.body?.height}, leafCount: ${treeRes.body?.leafCount}, secrets leaked: ${hasHashInTree}`
    });

    // Inorder Traversal
    const inorderRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree/inorder',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    const inorderList = Array.isArray(inorderRes.body) ? inorderRes.body : [];
    const isSortedAscending = inorderList.every((u, i) => i === 0 || u.id >= inorderList[i - 1].id);
    checks.push({
        name: "ADMIN GET /api/admin/users/tree/inorder (Ascending Sort)",
        pass: inorderRes.statusCode === 200 && inorderList.length === 17 && isSortedAscending,
        details: `HTTP ${inorderRes.statusCode}, count: ${inorderList.length}, sorted: ${isSortedAscending}, IDs: [${inorderList.map(u => u.id).join(', ')}]`
    });

    // Preorder Traversal (Root=9)
    const preorderRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree/preorder',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    const preorderList = Array.isArray(preorderRes.body) ? preorderRes.body : [];
    checks.push({
        name: "ADMIN GET /api/admin/users/tree/preorder (Balanced Root: 9)",
        pass: preorderRes.statusCode === 200 && preorderList.length === 17 && preorderList[0].id === 9,
        details: `HTTP ${preorderRes.statusCode}, root ID: ${preorderList[0]?.id}, count: ${preorderList.length}`
    });

    // Postorder Traversal (Last/Root=9)
    const postorderRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree/postorder',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    const postorderList = Array.isArray(postorderRes.body) ? postorderRes.body : [];
    checks.push({
        name: "ADMIN GET /api/admin/users/tree/postorder (Balanced Root: 9)",
        pass: postorderRes.statusCode === 200 && postorderList.length === 17 && postorderList[postorderList.length - 1].id === 9,
        details: `HTTP ${postorderRes.statusCode}, last ID (root): ${postorderList[postorderList.length - 1]?.id}, count: ${postorderList.length}`
    });

    // Search Existing User (ID 4 - Sheena)
    const searchMatchRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree/search?id=4',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    checks.push({
        name: "ADMIN Search BST for Existing User (ID: 4)",
        pass: searchMatchRes.statusCode === 200 && searchMatchRes.body?.found === true && searchMatchRes.body?.comparisons === 2 && searchMatchRes.body?.user?.username === 'sheena',
        details: `HTTP ${searchMatchRes.statusCode}, found: ${searchMatchRes.body?.found}, comparisons: ${searchMatchRes.body?.comparisons}, matched: ${searchMatchRes.body?.user?.displayName}`
    });

    // Search Nonexistent User (ID 99)
    const searchMissRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/admin/users/tree/search?id=99',
        method: 'GET',
        headers: { 'Cookie': admin.cookie }
    });
    checks.push({
        name: "ADMIN Search BST for Nonexistent User (ID: 99)",
        pass: searchMissRes.statusCode === 200 && searchMissRes.body?.found === false && searchMissRes.body?.comparisons === 5 && searchMissRes.body?.user === null,
        details: `HTTP ${searchMissRes.statusCode}, found: ${searchMissRes.body?.found}, comparisons: ${searchMissRes.body?.comparisons}, trace steps: ${searchMissRes.body?.decisionPath?.length}`
    });

    // 4. MEMBER Rejection (403 Forbidden)
    console.log("\n[4] Testing MEMBER Unauthorized Rejection (403 Forbidden)...");
    const endpoints = [
        "/api/admin/users/tree",
        "/api/admin/users/tree/inorder",
        "/api/admin/users/tree/preorder",
        "/api/admin/users/tree/postorder",
        "/api/admin/users/tree/search?id=4"
    ];

    for (const ep of endpoints) {
        const memRes = await request({
            hostname: 'localhost',
            port: 8085,
            path: ep,
            method: 'GET',
            headers: { 'Cookie': member.cookie }
        });
        checks.push({
            name: `MEMBER Access ${ep} -> 403 Forbidden`,
            pass: memRes.statusCode === 403,
            details: `HTTP ${memRes.statusCode} (Expected 403)`
        });
    }

    // 5. Unauthenticated Rejection (401 Unauthorized)
    console.log("\n[5] Testing Unauthenticated Rejection (401 Unauthorized)...");
    for (const ep of endpoints) {
        const unauthRes = await request({
            hostname: 'localhost',
            port: 8085,
            path: ep,
            method: 'GET'
        });
        checks.push({
            name: `Unauthenticated Access ${ep} -> 401 Unauthorized`,
            pass: unauthRes.statusCode === 401,
            details: `HTTP ${unauthRes.statusCode} (Expected 401)`
        });
    }

    // Print Results
    console.log("\n=================================================");
    console.log("             VERIFICATION SUMMARY                ");
    console.log("=================================================");
    let passed = 0;
    let failed = 0;
    checks.forEach((c) => {
        if (c.pass) passed++;
        else failed++;
        console.log(`[${c.pass ? 'PASS' : 'FAIL'}] ${c.name}`);
        console.log(`       Details: ${c.details}`);
    });

    console.log(`\nTOTAL CHECKS: ${checks.length} | PASS: ${passed} | FAIL: ${failed}`);
}

run().catch(console.error);

