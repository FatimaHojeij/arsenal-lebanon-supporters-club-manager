import { login, forgotPassword, isLoggedIn, isAdmin } from './api.js';

if (isLoggedIn()) {
    window.location.href = isAdmin() ? '/admin.html' : '/dashboard.html';
}

const form        = document.getElementById('login-form');
const emailInput  = document.getElementById('email');
const passInput   = document.getElementById('password');
const errorEl     = document.getElementById('login-error');
const submitBtn   = document.getElementById('login-btn');
const forgotLink  = document.getElementById('forgot-password-link');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorEl.textContent = '';
    submitBtn.disabled = true;
    submitBtn.textContent = 'Logging in…';

    try {
        const authResult = await login(emailInput.value.trim(), passInput.value);
        const role = authResult.role;
        if (role === 'ADMIN') {
            const forcePasswordChange = authResult.passwordChangeRequired ? '?forcePasswordChange=1&view=change-password' : '';
            window.location.href = `/admin.html${forcePasswordChange}`;
        } else {
            const forcePasswordChange = authResult.passwordChangeRequired ? '?forcePasswordChange=1&view=change-password' : '';
            window.location.href = `/dashboard.html${forcePasswordChange}`;
        }
    } catch (err) {
        errorEl.textContent = err.message.replace(/^[^\w]*/, '');
        errorEl.classList.remove('hidden');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Log In';
    }
});

forgotLink?.addEventListener('click', async (e) => {
    e.preventDefault();
    const email = emailInput.value.trim();
    if (!email) {
        errorEl.textContent = 'Please enter your email address first.';
        errorEl.classList.remove('hidden');
        return;
    }

    try {
        const message = await forgotPassword(email);
        errorEl.textContent = message;
        errorEl.className = 'alert alert-success';
        errorEl.classList.remove('hidden');
    } catch (err) {
        errorEl.textContent = err.message.replace(/^[^\w]*/, '');
        errorEl.className = 'alert alert-error';
        errorEl.classList.remove('hidden');
    }
});
