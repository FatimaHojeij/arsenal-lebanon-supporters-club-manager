import {
    isLoggedIn, isAdmin, logout,
    fetchOpenGames, fetchMyApplications, submitApplication, fetchMyProfile
} from './api.js';

// Guard: must be logged in as a member
if (!isLoggedIn()) window.location.href = '/index.html';
if (isAdmin())     window.location.href = '/admin.html';

// ── Tab switching ─────────────────────────────────────────────────────────────
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(btn.dataset.tab).classList.add('active');
    });
});

document.getElementById('logout-btn').addEventListener('click', logout);

// ── Helpers ───────────────────────────────────────────────────────────────────
function deadlineInfo(deadlineStr) {
    if (!deadlineStr) return { text: '', isPast: false };
    const today    = new Date(); today.setHours(0,0,0,0);
    const deadline = new Date(deadlineStr); deadline.setHours(0,0,0,0);
    const days     = Math.ceil((deadline - today) / 86400000);
    if (days > 1)  return { text: `⏳ ${days} days left to apply`,         isPast: false };
    if (days === 1) return { text: `⚠️ Application closes tomorrow!`,          isPast: false };
    if (days === 0) return { text: `🚨 Application closes TODAY!`,             isPast: false };
    return             { text: `Applications closed`,              isPast: true  };
}

function statusBadge(status) {
    const map = {
        Accepted: 'badge-green',
        Rejected: 'badge-red',
        Pending:  'badge-orange',
    };
    return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

// ── Games Tab ─────────────────────────────────────────────────────────────────
async function loadGames() {
    const container = document.getElementById('games-list');
    container.innerHTML = '<p class="text-muted">Loading fixtures…</p>';

    const [games, myApps] = await Promise.all([fetchOpenGames(), fetchMyApplications()]);
    const appliedIds = new Set(myApps.map(a => a.game.id));

    container.innerHTML = '';

    if (!games.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">⚽</div>No open fixtures right now.</div>';
        return;
    }

    games.forEach(game => {
        const { text: deadlineText, isPast } = deadlineInfo(game.deadline);
        const hasApplied = appliedIds.has(game.id);
        const isOpen     = game.applicationsOpen && !isPast;

        let accent = 'accent-gray';
        let actionHtml = '';

        if (hasApplied) {
            accent = 'accent-red';
            actionHtml = `<span class="badge badge-red" style="margin-top:10px">✓ Already Applied</span>`;
        } else if (!isOpen) {
            actionHtml = `<span class="text-muted" style="font-size:0.82rem;margin-top:8px;display:block">Registration closed</span>`;
        } else {
            accent = 'accent-green';
            actionHtml = `
                <div class="flex-gap" style="margin-top:12px">
                    <div class="form-group" style="margin:0">
                        <label for="tix-${game.id}" style="font-size:0.8rem">Tickets</label>
                        <select id="tix-${game.id}" style="width:auto;padding:5px 8px;font-size:0.85rem">
                            <option value="1">1</option>
                            <option value="2">2</option>
                        </select>
                    </div>
                    <label style="display:flex;align-items:center;gap:6px;font-size:0.85rem;font-weight:600;cursor:pointer;margin-top:18px">
                        <input type="checkbox" id="aon-${game.id}" style="width:auto;margin:0">
                        All or Nothing
                    </label>
                    <button class="btn btn-primary btn-sm" style="margin-top:18px"
                        onclick="applyForGame(${game.id})">Apply</button>
                </div>`;
        }

        const card = document.createElement('div');
        card.className = `item-card ${accent}`;
        card.style.flexDirection = 'column';
        card.innerHTML = `
            <div style="width:100%">
                <div class="item-card-title">Arsenal vs ${game.opponent}</div>
                <div class="item-card-meta">
                    Category ${game.category || '—'}
                    &nbsp;·&nbsp; 📅 ${game.matchDate || '—'}
                    &nbsp;·&nbsp; Deadline: ${game.deadline || '—'}
                    &nbsp;·&nbsp; ${deadlineText}
                </div>
                ${actionHtml}
            </div>`;
        container.appendChild(card);
    });
}

window.applyForGame = async (gameId) => {
    const tickets    = document.getElementById(`tix-${gameId}`).value;
    const allOrNothing = document.getElementById(`aon-${gameId}`).checked;

    const res  = await submitApplication(gameId, tickets, allOrNothing);
    const text = await res.text();
    alert(text);
    loadGames();
    loadMyApplications();
};

document.getElementById('refresh-games-btn').addEventListener('click', loadGames);

// ── My Applications Tab ───────────────────────────────────────────────────────
async function loadMyApplications() {
    const container = document.getElementById('my-apps-list');
    container.innerHTML = '<p class="text-muted">Loading…</p>';

    const apps = await fetchMyApplications();
    container.innerHTML = '';

    if (!apps.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">🎟️</div>No applications yet.</div>';
        return;
    }

    apps.forEach(app => {
        const card = document.createElement('div');
        const accentMap = { Accepted: 'accent-green', Rejected: 'accent-gray', Pending: 'accent-orange' };
        card.className = `item-card ${accentMap[app.status] || 'accent-gray'}`;
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">Arsenal vs ${app.game.opponent}</div>
                <div class="item-card-meta">
                    ${app.ticketsRequested} ticket(s) requested
                    ${app.allOrNothing ? '&nbsp;·&nbsp;<span class="badge badge-gold">All or Nothing</span>' : ''}
                    &nbsp;·&nbsp; Applied: ${new Date(app.appliedAt).toLocaleString()}
                    &nbsp;·&nbsp; Priority score: <strong>${app.calculatedPriorityScore}</strong>
                </div>
            </div>
            <div>${statusBadge(app.status)}</div>`;
        container.appendChild(card);
    });
}

document.getElementById('refresh-apps-btn').addEventListener('click', loadMyApplications);

// ── Profile Tab ───────────────────────────────────────────────────────────────
async function loadProfile() {
    const container = document.getElementById('profile-panel');
    container.innerHTML = '<p class="text-muted">Loading profile…</p>';

    const p = await fetchMyProfile();
    if (!p) { container.innerHTML = '<p class="text-muted">Could not load profile.</p>'; return; }

    const statusBadgeHtml = {
        Active: '<span class="badge badge-green">Active</span>',
        Lapsed: '<span class="badge badge-orange">Lapsed</span>',
        Banned: '<span class="badge badge-red">Banned</span>',
        Pending:'<span class="badge badge-gray">Pending</span>',
    }[p.status] || p.status;

    container.innerHTML = `
        <div class="profile-grid">
            <div class="profile-field"><label>Name</label><span>${p.firstName} ${p.lastName}</span></div>
            <div class="profile-field"><label>ALSC #</label><span>${p.alscMembershipNumber}</span></div>
            <div class="profile-field"><label>Email</label><span>${p.email}</span></div>
            <div class="profile-field"><label>Phone</label><span>${p.phoneNumber || '—'}</span></div>
            <div class="profile-field"><label>Status</label><span>${statusBadgeHtml}</span></div>
            <div class="profile-field"><label>Member Since</label><span>${p.joinDate}</span></div>
            <div class="profile-field"><label>Expires</label><span>${p.expiryDate}</span></div>
            <div class="profile-field"><label>Country</label><span>${p.country || '—'}</span></div>
        </div>
        <hr class="divider">
        <h4 style="margin-bottom:12px">Attendance Stats</h4>
        <div class="profile-grid">
            <div class="profile-field"><label>Total Games Attended</label><span>${p.totalGamesAttended}</span></div>
            <div class="profile-field"><label>This Season</label><span>${p.gamesAttendedThisSeason}</span></div>
            <div class="profile-field"><label>Category A This Season</label><span>${p.categoryAGamesThisSeason}</span></div>
            <div class="profile-field"><label>Defaulted Games</label><span>${p.defaultedGamesCount}</span></div>
            <div class="profile-field"><label>Penalty Points</label><span>${p.customPenaltyPoints}</span></div>
        </div>`;
}

// ── Boot ──────────────────────────────────────────────────────────────────────
loadGames();
loadMyApplications();
loadProfile();
