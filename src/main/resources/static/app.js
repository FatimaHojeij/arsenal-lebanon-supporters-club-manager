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

// 🔄 Fetch all open match fixtures
loadGamesBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    gamesList.innerHTML = "Querying live matches...";

    try {
        // 1️⃣ First, fetch the user's current applications to see what they've already joined
        const historyResponse = await fetch('/api/applications/my-applications', {
            method: 'GET',
            headers: { 'Authorization': token }
        });

        let appliedGameIds = new Set();
        if (historyResponse.ok) {
            const myApps = await historyResponse.ok ? await historyResponse.json() : [];
            // Extract just the game IDs into a quick-lookup Set
            appliedGameIds = new Set(myApps.map(app => app.game.id));
        }

        // 2️⃣ Next, fetch the live game list
        const gamesResponse = await fetch('/api/games', {
            method: 'GET',
            headers: { 'Authorization': token }
        });

        if (gamesResponse.ok) {
            const games = await gamesResponse.json();
            // Pass the applied game IDs down to the display function
            displayGames(games, appliedGameIds);
        }
    } catch (e) {
        gamesList.innerHTML = "❌ Error connecting to game engine.";
    }
});

// 🎨 Render the game grid conditionally
function displayGames(games, appliedGameIds) {
    gamesList.innerHTML = "";
    if (games.length === 0) {
        gamesList.innerHTML = "<p>No matches added to the system yet.</p>";
        return;
    }

    games.forEach(game => {
        // 🗓️ Calculate countdown tracking
        let countdownText = "";
        let isPastDeadline = false;

        if (game.deadline) {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const deadline = new Date(game.deadline);
            deadline.setHours(0, 0, 0, 0);

            const timeDiff = deadline.getTime() - today.getTime();
            const daysLeft = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));

            if (daysLeft > 1) countdownText = `⏳ **${daysLeft} days left** to apply`;
            else if (daysLeft === 1) countdownText = `⚠️ **1 day left** – Closes tomorrow!`;
            else if (daysLeft === 0) countdownText = `🚨 **Closes TODAY!**`;
            else {
                countdownText = `❌ Applications closed`;
                isPastDeadline = true;
            }
        }

        // 🛑 Check condition: Has this user already submitted a form for this game?
        const hasAlreadyApplied = appliedGameIds.has(game.id);
        const isOpen = game.applicationsOpen && !isPastDeadline;

        // Determine what status action template badge to project
        let actionAreaHtml = "";

        if (hasAlreadyApplied) {
            // Hide selection tools entirely and show verification string
            actionAreaHtml = `<span style="color: #ef0107; font-weight: bold; font-size: 14px; display: inline-block; margin-top: 10px;">✓ Already Applied</span>`;
        } else if (!isOpen) {
            actionAreaHtml = `<span style="color: gray; font-size: 13px; display: inline-block; margin-top: 8px;">Registration Window Closed</span>`;
        } else {
            // Standard action interface panel layout
            actionAreaHtml = `
               <div style="margin-top: 10px; display: flex; flex-wrap: wrap; gap: 15px; align-items: center;">
                   <div>
                       <label for="tickets-${game.id}" style="font-size: 13px;">Quantity:</label>
                       <select id="tickets-${game.id}" style="width: auto; padding: 4px; margin: 0;">
                           <option value="1">1 Ticket</option>
                           <option value="2">2 Tickets</option>
                       </select>
                   </div>
                   
                   <div style="display: flex; align-items: center; gap: 5px;">
                       <input type="checkbox" id="aon-${game.id}" style="width: auto; margin: 0;">
                       <label for="aon-${game.id}" style="font-size: 13px; font-weight: normal; cursor: pointer;">All or Nothing</label>
                   </div>

                   <button onclick="submitApplication(${game.id})" style="padding: 6px 12px; font-size:13px; width:auto; margin: 0;">Apply</button>
               </div>
            `;
        }

        const div = document.createElement('div');
        div.className = 'member-card';
        // Give applied cards a distinct, subtle left border indicator tone
        div.style.borderLeftColor = hasAlreadyApplied ? "#ef0107" : (isOpen ? "green" : "gray");
        div.innerHTML = `
            <strong>Arsenal vs ${game.opponent}</strong> (Category ${game.category})<br>
            📅 Match Date: ${game.matchDate} | Deadline: ${game.deadline} ${countdownText}<br>
            ${actionAreaHtml}
        `;
        gamesList.appendChild(div);
    });
}

// 🎟️ Tweak your application submit callback slightly to clear/refresh properly
async function submitApplication(gameId) {
    const token = localStorage.getItem('token');
    const ticketsRequested = document.getElementById(`tickets-${gameId}`).value;
    const allOrNothing = document.getElementById(`aon-${gameId}`).checked;

    try {
        const url = `/api/applications/apply?gameId=${gameId}&ticketsRequested=${ticketsRequested}&allOrNothing=${allOrNothing}`;
        const response = await fetch(url, { method: 'POST', headers: { 'Authorization': token } });
        const msg = await response.text();
        alert(msg);

        // 🔥 Trigger the load list click automatically to re-evaluate history Sets and update button layouts
        loadGamesBtn.click();
        loadAppsBtn.click();
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