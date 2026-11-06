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

async function verifyEndpoints() {
    console.log("1. Logging in as admin...");
    const admin = await login("admin", "Admin@123");
    console.log("Admin login status:", admin.status);
    const cookie = admin.cookie;

    const endpoints = [
        { name: "GET /api/admin/users/tree", path: "/api/admin/users/tree" },
        { name: "GET /api/admin/users/tree/inorder", path: "/api/admin/users/tree/inorder" },
        { name: "GET /api/admin/users/tree/preorder", path: "/api/admin/users/tree/preorder" },
        { name: "GET /api/admin/users/tree/postorder", path: "/api/admin/users/tree/postorder" },
        { name: "GET /api/admin/users/tree/search?id=4", path: "/api/admin/users/tree/search?id=4" }
    ];

    for (const ep of endpoints) {
        console.log(`\nTesting ${ep.name}...`);
        const res = await request({
            hostname: 'localhost',
            port: 8085,
            path: ep.path,
            method: 'GET',
            headers: { 'Cookie': cookie }
        });
        console.log(`Status: ${res.statusCode}`);
        if (Array.isArray(res.body)) {
            console.log(`Array length: ${res.body.length}`);
            console.log(`First item:`, JSON.stringify(res.body[0]));
            console.log(`Last item:`, JSON.stringify(res.body[res.body.length - 1]));
            console.log(`IDs:`, res.body.map(u => u.id).join(', '));
        } else {
            console.log(`Response keys:`, Object.keys(res.body));
            console.log(`Body sample:`, JSON.stringify(res.body, null, 2).substring(0, 300) + '...');
        }
    }
}

verifyEndpoints().catch(console.error);

