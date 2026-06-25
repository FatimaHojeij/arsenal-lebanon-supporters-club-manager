const API_BASE_URL = '/api';

// DOM Element References
const loginScreen = document.getElementById('login-screen');
const dashboard = document.getElementById('dashboard');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('login-btn');
const loginError = document.getElementById('login-error');
const fetchMembersBtn = document.getElementById('fetch-members-btn');
const membersList = document.getElementById('members-list');
const logoutBtn = document.getElementById('logout-btn');

// 🔄 Check if user is already logged in on page load
window.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    if (token) {
        showDashboard();
    }
});

// 🔐 Handle Login Submit
loginBtn.addEventListener('click', async () => {
    const email = emailInput.value;
    const password = passwordInput.value;
    loginError.textContent = "";

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.text();

        if (response.ok && data.startsWith('Bearer ')) {
            localStorage.setItem('token', data); // Save token safely
            showDashboard();
        } else {
            loginError.textContent = "❌ Invalid email or password.";
        }
    } catch (error) {
        loginError.textContent = "❌ Cannot connect to backend server.";
    }
});

// 👥 Fetch Protected Member Records
fetchMembersBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    membersList.innerHTML = "Loading records...";

    try {
        const response = await fetch(`${API_BASE_URL}/members`, {
            method: 'GET',
            headers: {
                'Authorization': token, // Send the token in headers
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const members = await response.json();
            displayMembers(members);
        } else if (response.status === 403) {
            alert("Session expired or unauthorized. Logging out.");
            logout();
        }
    } catch (error) {
        membersList.innerHTML = "❌ Failed to retrieve member data.";
    }
});

// 🚪 Handle Logout
logoutBtn.addEventListener('click', logout);

// 🛠️ Helper Functions
function showDashboard() {
    loginScreen.classList.add('hidden');
    dashboard.classList.remove('hidden');
}

function logout() {
    localStorage.removeItem('token');
    loginScreen.classList.remove('hidden');
    dashboard.classList.add('hidden');
    membersList.innerHTML = "";
}

function displayMembers(members) {
    membersList.innerHTML = "";
    if (members.length === 0) {
        membersList.innerHTML = "<p>No members registered yet.</p>";
        return;
    }

    members.forEach(member => {
        const card = document.createElement('div');
        card.className = 'member-card';
        card.innerHTML = `
            <strong>${member.title || ''} ${member.firstName} ${member.lastName}</strong><br>
            📧 ${member.email} | 📞 ${member.phoneNumber || 'No Phone'}<br>
            ⚽ Matches Attended: ${member.totalGamesAttended} | Status: <span style="color: green; font-weight: bold;">${member.status}</span>
        `;
        membersList.appendChild(card);
    });
}