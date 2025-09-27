// Enhanced error handling for CSRF-protected requests
async function postWithCsrfAndErrorHandling(url, data) {
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [getCsrfHeader()]: getCsrfToken()
            },
            body: JSON.stringify(data)
        });

        if (response.status === 403) {
            // CSRF token might be invalid/expired
            console.error('CSRF token validation failed. Reloading page...');
            showAlert('Security token expired. Please refresh the page and try again.', 'warning');
            setTimeout(() => window.location.reload(), 2000);
            return null;
        }

        return response;
    } catch (error) {
        console.error('Request failed:', error);
        throw error;
    }
}