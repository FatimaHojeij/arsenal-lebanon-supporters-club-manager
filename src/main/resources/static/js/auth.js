import { login, isLoggedIn, isAdmin } from './api.js';

if (isLoggedIn()) {
    window.location.href = isAdmin() ? '/admin.html' : '/dashboard.html';
}

const form        = document.getElementById('login-form');
const emailInput  = document.getElementById('email');
const passInput   = document.getElementById('password');
const errorEl     = document.getElementById('login-error');
const submitBtn   = document.getElementById('login-btn');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorEl.textContent = '';
    submitBtn.disabled = true;
    submitBtn.textContent = 'Logging in…';

    try {
        const role = await login(emailInput.value.trim(), passInput.value);
        window.location.href = role === 'ADMIN' ? '/admin.html' : '/dashboard.html';
    } catch (err) {
        // Strip emoji/prefix from server message for clean display
        errorEl.textContent = err.message.replace(/^[^\w]*/, '');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Log In';
    }
});
