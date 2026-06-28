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

    // Add this function above the submit listener:
    function validateForm(payload) {
        const errors = [];

        if (!payload.firstName || payload.firstName.length < 2)
            errors.push('First name must be at least 2 characters.');
        if (!payload.lastName || payload.lastName.length < 2)
            errors.push('Last name must be at least 2 characters.');

        const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!payload.email || !emailRe.test(payload.email))
            errors.push('Please enter a valid email address.');

        if (!payload.password || payload.password.length < 8)
            errors.push('Password must be at least 8 characters.');

        if (!payload.dateOfBirth)
            errors.push('Date of birth is required.');
        else if (new Date(payload.dateOfBirth) >= new Date())
            errors.push('Date of birth must be in the past.');

        if (!payload.country || !payload.country.trim())
            errors.push('Country is required.');

        return errors;
    }

// Then at the top of the submit handler, after building payload:
    const validationErrors = validateForm(payload);
    if (validationErrors.length) {
        msgEl.className = 'alert alert-error';
        msgEl.textContent = validationErrors.join(' ');
        msgEl.classList.remove('hidden');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Registration';
        return;
    }

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
