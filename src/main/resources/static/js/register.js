const form    = document.getElementById('register-form');
const msgEl   = document.getElementById('response-message');
const submitBtn = document.getElementById('submit-btn');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msgEl.className = 'alert hidden';
    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting…';

    const payload = {
        title:               document.getElementById('title').value,
        firstName:           document.getElementById('firstName').value.trim(),
        lastName:            document.getElementById('lastName').value.trim(),
        email:               document.getElementById('email').value.trim(),
        password:            document.getElementById('password').value,
        phoneNumber:         document.getElementById('phoneNumber').value.trim(),
        dateOfBirth:         document.getElementById('dateOfBirth').value,
        country:             document.getElementById('country').value.trim(),
    };

    try {
        const res  = await fetch('/api/members/register', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(payload),
        });

        const text = await res.text();

        if (res.ok) {
            msgEl.className = 'alert alert-success';
            msgEl.textContent = text + ' Redirecting to login…';
            form.reset();
            setTimeout(() => window.location.href = '/index.html', 2500);
        } else {
            msgEl.className = 'alert alert-error';
            msgEl.textContent = text.replace(/^[^\w]*/, '');
        }
    } catch {
        msgEl.className = 'alert alert-error';
        msgEl.textContent = 'Network error: could not reach the server.';
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Registration';
        msgEl.classList.remove('hidden');
    }
});
