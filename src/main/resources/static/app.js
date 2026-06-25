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
const loadGamesBtn = document.getElementById('load-games-btn');
const gamesList = document.getElementById('games-list');
const loadAppsBtn = document.getElementById('load-apps-btn');
const myAppsList = document.getElementById('my-apps-list');

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

loadGamesBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    gamesList.innerHTML = "Querying live matches...";

    try {
        const response = await fetch('/api/games', {
            method: 'GET',
            headers: { 'Authorization': token }
        });

        if (response.ok) {
            const games = await response.json();
            displayGames(games);
        }
    } catch (e) {
        gamesList.innerHTML = "❌ Error connecting to game engine.";
    }
});

function displayGames(games) {
    gamesList.innerHTML = "";
    if (games.length === 0) {
        gamesList.innerHTML = "<p>No matches added to the system yet.</p>";
        return;
    }

    games.forEach(game => {
        const div = document.createElement('div');
        div.className = 'member-card'; // Reuse styled layout containers
        div.style.borderLeftColor = game.applicationsOpen ? "green" : "gray";
        div.innerHTML = `
            <strong>Arsenal vs ${game.opponent}</strong> (Category ${game.category})<br>
            📅 Date: ${game.matchDate} | 🎟️ Available Inventory: ${game.availableTickets}<br>
            ${game.applicationsOpen
            ? `<button onclick="submitApplication(${game.id})" style="padding: 6px 12px; margin-top: 8px; font-size:13px; width:auto;">Apply for Ticket</button>`
            : `<span style="color:gray;">Applications Closed</span>`}
        `;
        gamesList.appendChild(div);
    });
}

// 🎟️ Send the application request packet
async function submitApplication(gameId) {
    const token = localStorage.getItem('token');
    try {
        const response = await fetch(`/api/applications/apply?gameId=${gameId}`, {
            method: 'POST',
            headers: { 'Authorization': token }
        });
        const msg = await response.text();
        alert(msg); // Notice string line from server (e.g. Success or Active constraint block)
        loadAppsBtn.click(); // Auto-refresh historical lists
    } catch (error) {
        alert("Network fault submitting request.");
    }
}

// 📋 Load logged-in user's unique selection profile list history
loadAppsBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    myAppsList.innerHTML = "Updating allocation records...";

    try {
        const response = await fetch('/api/applications/my-applications', {
            method: 'GET',
            headers: { 'Authorization': token }
        });

        if (response.ok) {
            const apps = await response.json();
            myAppsList.innerHTML = "";
            if (apps.length === 0) {
                myAppsList.innerHTML = "<p>You haven't submitted any application allocations yet.</p>";
                return;
            }
            apps.forEach(app => {
                const div = document.createElement('div');
                div.className = 'member-card';
                div.innerHTML = `
                    <strong>Arsenal vs ${app.game.opponent}</strong><br>
                    Status: <strong style="color:${app.status === 'APPROVED' ? 'green' : app.status === 'REJECTED' ? 'red' : 'orange'}">${app.status}</strong><br>
                    <small>Registered at: ${new Date(app.appliedAt).toLocaleString()}</small>
                `;
                myAppsList.appendChild(div);
            });
        }
    } catch(e) {
        myAppsList.innerHTML = "❌ Failed loading tracking logs.";
    }
});