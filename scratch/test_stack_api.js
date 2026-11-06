const http = require('http');

async function request(options, postData) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                resolve({ statusCode: res.statusCode, headers: res.headers, body: data });
            });
        });
        req.on('error', reject);
        if (postData) req.write(postData);
        req.end();
    });
}

async function run() {
    console.log("1. Logging in as admin...");
    const loginData = JSON.stringify({ username: 'admin', password: 'Admin@123' });
    const loginRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/login',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(loginData)
        }
    }, loginData);

    console.log("Login status:", loginRes.statusCode);
    const setCookie = loginRes.headers['set-cookie'];
    if (!setCookie) {
        console.error("No cookie returned! Response:", loginRes.body);
        return;
    }
    const cookie = setCookie.map(c => c.split(';')[0]).join('; ');
    console.log("Cookie obtained:", cookie);

    console.log("\n2. Fetching /api/tasks/stack...");
    const stackRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack',
        method: 'GET',
        headers: { 'Cookie': cookie }
    });
    console.log("Stack status:", stackRes.statusCode);
    const stack = JSON.parse(stackRes.body);
    console.log("Stack items count:", stack.length);
    stack.forEach((item, index) => {
        console.log(`  [Index ${index}] id: ${item.id}, name: "${item.name}", member: "${item.member}", priority: "${item.priority}", status: "${item.status}"`);
    });

    console.log("\n3. Fetching /api/tasks/stack/peek...");
    const peekRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack/peek',
        method: 'GET',
        headers: { 'Cookie': cookie }
    });
    console.log("Peek status:", peekRes.statusCode);
    console.log("Peek response:", peekRes.body);

    console.log("\n4. Fetching /api/tasks/stack/history...");
    const histRes = await request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/tasks/stack/history',
        method: 'GET',
        headers: { 'Cookie': cookie }
    });
    console.log("History status:", histRes.statusCode);
    const history = JSON.parse(histRes.body);
    console.log("History items count:", history.length);
    history.forEach((item, index) => {
        console.log(`  [History ${index}] id: ${item.id}, taskId: ${item.taskId}, taskName: "${item.taskName}", member: "${item.member}", poppedAt: "${item.poppedAt}", originalStackOrder: ${item.originalStackOrder}`);
    });
}

run().catch(console.error);
