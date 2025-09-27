# CSRF Protection Implementation

This application uses Spring Security's CSRF protection for all POST requests.

## How it Works

### Backend Configuration
- **SecurityConfig.java**: Enables CSRF with cookie-based token repository
- **CookieCsrfTokenRepository.withHttpOnlyFalse()**: Allows JavaScript access to CSRF tokens

### Frontend Implementation
- **CSRF tokens** are embedded in HTML templates via meta tags:
  ```html
  <meta name="_csrf" th:content="${_csrf.token}"/>
  <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
  ```

### JavaScript Integration
- **csrf-utils.js**: Centralized CSRF utilities for all AJAX requests
- **postWithCsrfAndErrorHandling()**: Enhanced function with automatic error handling
- **Automatic token inclusion**: All POST requests include the CSRF token in headers

## Usage Example

```javascript
// Simple POST with CSRF protection
const response = await postWithCsrf('/api/endpoint', { data: 'value' });

// POST with enhanced error handling (recommended)
const response = await postWithCsrfAndErrorHandling('/api/endpoint', { data: 'value' });
```

## Security Benefits
- ✅ Prevents Cross-Site Request Forgery attacks
- ✅ Validates all state-changing operations
- ✅ Automatic token expiration and refresh
- ✅ User-friendly error messages for expired tokens

## Files Involved
- `SecurityConfig.java` - CSRF configuration
- `csrf-utils.js` - JavaScript utilities
- `foosball.html` - Template with CSRF meta tags
- `foosball.js` - Example implementation