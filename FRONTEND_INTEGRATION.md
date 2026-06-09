# Frontend Integration Guide

## 🔗 Backend API Access

Backend is running on: `http://localhost:8080`

### Available Endpoints

#### Health Check
```
GET http://localhost:8080/api/health
```

**Response:**
```json
{
  "status": "UP",
  "message": "Flowiq Backend is running successfully",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2026-06-09T14:26:23.567",
  "environment": "development"
}
```

#### Ping
```
GET http://localhost:8080/api/health/ping
```

**Response:**
```
pong
```

---

## 🌐 CORS Configuration

CORS is configured to allow requests from:
- `http://localhost:3000` (Next.js dev server)
- `http://localhost:3001` (alternative port)
- Production domain (when deployed)

### Allowed Methods
- GET
- POST
- PUT
- DELETE
- PATCH
- OPTIONS

### Allowed Headers
- Authorization
- Content-Type
- Accept
- X-Requested-With
- Origin

---

## 🧪 Testing from Frontend

### Option 1: Using Fetch API

```typescript
// Test health endpoint
async function testBackendConnection() {
  try {
    const response = await fetch('http://localhost:8080/api/health');
    const data = await response.json();
    console.log('Backend status:', data);
  } catch (error) {
    console.error('Backend connection failed:', error);
  }
}
```

### Option 2: Using Axios (Recommended)

```typescript
import axios from 'axios';

// Already configured in frontend: src/services/api.ts
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Test health
const response = await apiClient.get('/health');
console.log(response.data);
```

### Option 3: Update Frontend API Base URL

In `flowiq-frontend/src/services/api.ts`:

```typescript
// Change this line:
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

// Or set environment variable in .env.local:
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

---

## ✅ Verification Steps

1. **Backend is running:**
   ```bash
   curl http://localhost:8080/api/health/ping
   # Should return: pong
   ```

2. **CORS is working:**
   ```bash
   curl -H "Origin: http://localhost:3000" \
        -H "Access-Control-Request-Method: GET" \
        -X OPTIONS http://localhost:8080/api/health
   # Should return 200 with CORS headers
   ```

3. **Frontend can connect:**
   - Open browser console on `http://localhost:3000`
   - Run: `fetch('http://localhost:8080/api/health').then(r => r.json()).then(console.log)`
   - Should see health data, no CORS errors

---

## 🚀 Next Steps for Full Integration

### 1. Update Frontend Service
In `flowiq-frontend/src/services/api.ts`, update base URL:
```typescript
const API_BASE_URL = "http://localhost:8080/api";
```

### 2. Test Health Check in Frontend
Create a test component:
```tsx
// app/test-backend/page.tsx
'use client';

import { useState, useEffect } from 'react';

export default function TestBackend() {
  const [status, setStatus] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/health')
      .then(res => res.json())
      .then(setStatus)
      .catch(err => setError(err.message));
  }, []);

  if (error) return <div>Error: {error}</div>;
  if (!status) return <div>Loading...</div>;

  return (
    <div>
      <h1>Backend Status: {status.status}</h1>
      <pre>{JSON.stringify(status, null, 2)}</pre>
    </div>
  );
}
```

### 3. Navigate to Test Page
Open: `http://localhost:3000/test-backend`

Should see backend health data without CORS errors! ✅

---

## 📝 Troubleshooting

### CORS Error: "blocked by CORS policy"
**Solution:** Make sure backend is running and CORS is configured.

### Connection Refused
**Solution:** Start backend with `./mvnw spring-boot:run`

### Wrong Port
**Solution:** Backend runs on 8080, frontend on 3000. Don't confuse them!

---

## 🎯 Ready for Development

✅ Backend running on port 8080  
✅ CORS configured for frontend  
✅ Health endpoints working  
✅ Ready for API integration

Start building your API endpoints and consuming them from frontend!

---

**Last Updated:** June 9, 2026  
**Backend Version:** 0.0.1-SNAPSHOT  
**Status:** Ready for Integration 🚀
