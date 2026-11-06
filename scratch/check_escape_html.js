const fs = require('fs');

const html = fs.readFileSync('src/frontend/user-tree.html', 'utf8');

// Extract script blocks
const scripts = html.match(/<script[\s\S]*?<\/script>/gi);
console.log(`Found ${scripts.length} script tags.`);

// Check for escapeHtml definition vs usages
const hasDefinition = /function\s+escapeHtml/i.test(html) || /const\s+escapeHtml/i.test(html) || /var\s+escapeHtml/i.test(html);
const usages = (html.match(/escapeHtml\s*\(/g) || []).length;

console.log(`escapeHtml defined in user-tree.html: ${hasDefinition}`);
console.log(`escapeHtml call count in user-tree.html: ${usages}`);

