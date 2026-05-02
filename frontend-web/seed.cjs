/*
 * This file provides frontend application configuration and wiring for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
const https = require('http');

const API_GATEWAY = 'http://localhost:8080'; // Gateway port

async function request(method, path, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(`${API_GATEWAY}${path}`);
    const options = {
      method,
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      headers: {
        'Content-Type': 'application/json',
      },
    };

    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try { resolve(JSON.parse(data)); } catch (e) { resolve(data); }
        } else {
          reject(new Error(`API Error ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('error', reject);

    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  console.log('🌱 Starting Demo Data Seeding...');

  try {
    // 1. Register Users
    console.log('Creating demo users...');
    const timestamp = Date.now();
    const adminEmail = `admin_${timestamp}@demo.com`;
    const authorEmail = `author_${timestamp}@demo.com`;
    const readerEmail = `reader_${timestamp}@demo.com`;
    
    // Create Admin
    try {
      await request('POST', '/api/auth/register', {
        username: `demo_admin_${timestamp}`,
        email: adminEmail,
        password: 'Password123!',
        fullName: 'Demo Admin',
        role: 'ADMIN'
      });
      console.log(`✅ Created Admin (Login with: ${adminEmail} / Password123!)`);
    } catch (e) { console.log('Admin creation failed: ' + e.message); }

    // Create Author
    try {
      await request('POST', '/api/auth/register', {
        username: `demo_author_${timestamp}`,
        email: authorEmail,
        password: 'Password123!',
        fullName: 'Demo Author',
        role: 'AUTHOR'
      });
      console.log('✅ Created Author');
    } catch (e) { console.log('Author creation failed: ' + e.message); }

    // Create Reader
    try {
      await request('POST', '/api/auth/register', {
        username: `demo_reader_${timestamp}`,
        email: readerEmail,
        password: 'Password123!',
        fullName: 'Demo Reader',
        role: 'READER'
      });
      console.log('✅ Created Reader');
    } catch (e) { console.log('Reader creation failed: ' + e.message); }

    // Login as Author
    const loginRes = await request('POST', '/api/auth/login', {
      email: authorEmail,
      password: 'Password123!'
    });
    let authorToken = loginRes.data.accessToken;

    // Login as Reader
    const readerLoginRes = await request('POST', '/api/auth/login', {
      email: readerEmail,
      password: 'Password123!'
    });
    const readerToken = readerLoginRes.data.accessToken;

    // 2. Create Posts (Categories and Tags will auto-create via our new logic!)
    console.log('Creating demo posts...');
    const categories = ['Technology', 'Design', 'Lifestyle', 'Business', 'Writing'];
    const tags = ['JavaScript', 'React', 'Productivity', 'Startups', 'Inspiration', 'Web Development'];
    
    let postIds = [];
    
    for (let i = 1; i <= 15; i++) {
      const cat = categories[i % categories.length];
      const tag1 = tags[i % tags.length];
      const tag2 = tags[(i + 1) % tags.length];
      
      const post = await request('POST', '/api/posts/author', {
        title: `The Future of ${cat} in 2026 - Part ${i}`,
        excerpt: `Discover the amazing trends shaping ${cat} this year. An in-depth look at what to expect.`,
        content: `<p>This is a completely auto-generated demo post about ${cat}.</p><h2>Why ${cat} matters</h2><p>Here is some compelling text about the subject, featuring insights on ${tag1} and ${tag2}.</p>`,
        categorySlug: cat.toLowerCase(),
        tagSlugs: [tag1.toLowerCase(), tag2.toLowerCase()],
        status: i > 2 ? 'PUBLISHED' : 'DRAFT', // Mix of published and drafts
        visibility: i % 4 === 0 ? 'PREMIUM' : 'PUBLIC', // Mix of premium and public
        featuredImageUrl: `https://picsum.photos/seed/${i}/800/400`,
        featured: i === 1,
        pinned: i === 2
      }, authorToken);
      
      postIds.push(post.data.postId);
      console.log(`✅ Created Post ${i}`);
      await sleep(200); // Prevent overwhelming the API
    }

    // 3. Add Comments
    console.log('Adding comments...');
    for (let i = 0; i < 5; i++) {
      await request('POST', '/api/comments/reader', {
        postId: postIds[i],
        content: 'This is an amazing article! Thanks for sharing this detailed insight.',
      }, readerToken);
      console.log(`✅ Added Comment to post ${i}`);
    }

    // 4. Add Newsletter Subscribers
    console.log('Adding newsletter subscribers...');
    for (let i = 1; i <= 10; i++) {
      try {
        await request('POST', '/api/newsletter/public/subscribe', {
          email: `subscriber${i}@demo.com`,
          fullName: `Demo Subscriber ${i}`,
          preferences: ['tech', 'news']
        });
        console.log(`✅ Initiated Subscription ${i}`);
      } catch (e) { }
    }

    console.log('🎉 Data Seeding Complete! Go check your Admin Dashboard!');

  } catch (err) {
    console.error('Error during seeding:', err.message);
  }
}

run();
