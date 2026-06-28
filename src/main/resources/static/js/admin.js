import {
    isLoggedIn, isAdmin, logout,
    fetchPendingMembers, approveMember, rejectMember, banMember, penalizeMember,
    fetchAllMembers,
    fetchAdminOpenGames, setGameTickets, fetchGameApplications,
    allocateApplication, deallocateApplication, rejectApplication, closeGame
} from './api.js';

// Guard: must be logged in as admin
if (!isLoggedIn()) window.location.href = '/index.html';
if (!isAdmin())    window.location.href = '/dashboard.html';

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
function statusBadge(status) {
    const map = { Active: 'badge-green', Lapsed: 'badge-orange', Banned: 'badge-red', Pending: 'badge-gray' };
    return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

function appStatusBadge(status) {
    const map = { Accepted: 'badge-green', Rejected: 'badge-red', Pending: 'badge-orange' };
    return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

function toast(msg) {
    const el = document.getElementById('toast');
    el.textContent = msg.replace(/^[\u{1F300}-\u{1FFFF} ]*/u, '').trim();
    el.className = 'alert alert-success';
    el.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => el.classList.add('hidden'), 3500);
}

function toastError(msg) {
    const el = document.getElementById('toast');
    el.textContent = msg.replace(/^[\u{1F300}-\u{1FFFF} ]*/u, '').trim();
    el.className = 'alert alert-error';
    el.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => el.classList.add('hidden'), 4000);
}

async function handleResponse(res) {
    const text = await res.text();
    if (res.ok) toast(text);
    else toastError(text);
    return res.ok;
}

// ── Tab: Pending Members ──────────────────────────────────────────────────────
async function loadPendingMembers() {
    const container = document.getElementById('pending-list');
    container.innerHTML = '<p class="text-muted">Loading…</p>';
    const members = await fetchPendingMembers();
    container.innerHTML = '';

    if (!members.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">✅</div>No pending registrations.</div>';
        return;
    }

    members.forEach(m => {
        const card = document.createElement('div');
        card.className = 'item-card accent-orange';
        card.id = `pending-${m.id}`;
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">${m.title || ''} ${m.firstName} ${m.lastName}</div>
                <div class="item-card-meta">📧 ${m.email} &nbsp;·&nbsp; 📞 ${m.phoneNumber || '—'}</div>
                <div class="item-card-meta">ALSC # ${m.ALSCMembershipNumber} &nbsp;·&nbsp; DOB: ${m.dateOfBirth || '—'} &nbsp;·&nbsp; 🌍 ${m.country || '—'}</div>
            </div>
            <div class="item-card-actions">
                <button class="btn btn-success btn-sm" onclick="doApprove(${m.id})">Approve</button>
                <button class="btn btn-danger btn-sm"  onclick="doReject(${m.id})">Reject</button>
            </div>`;
        container.appendChild(card);
    });
}

window.doApprove = async (id) => {
    const ok = await handleResponse(await approveMember(id));
    if (ok) document.getElementById(`pending-${id}`)?.remove();
};

window.doReject = async (id) => {
    if (!confirm('Reject and permanently delete this registration?')) return;
    const ok = await handleResponse(await rejectMember(id));
    if (ok) document.getElementById(`pending-${id}`)?.remove();
};

document.getElementById('refresh-pending-btn').addEventListener('click', loadPendingMembers);

// ── Tab: All Members (roster) ─────────────────────────────────────────────────
async function loadRoster() {
    const container = document.getElementById('roster-list');
    container.innerHTML = '<p class="text-muted">Loading…</p>';
    const members = await fetchAllMembers();
    container.innerHTML = '';

    if (!members.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">👥</div>No members yet.</div>';
        return;
    }

    members.forEach(m => {
        const accentMap = { Active: 'accent-green', Lapsed: 'accent-orange', Banned: 'accent-red', Pending: 'accent-gray' };
        const card = document.createElement('div');
        card.className = `item-card ${accentMap[m.status] || 'accent-gray'}`;
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">${m.title || ''} ${m.firstName} ${m.lastName} ${statusBadge(m.status)}</div>
                <div class="item-card-meta">📧 ${m.email} &nbsp;·&nbsp; ALSC # ${m.ALSCMembershipNumber} &nbsp;·&nbsp; ${m.memberType}</div>
                <div class="item-card-meta">⚽ ${m.totalGamesAttended} attended &nbsp;·&nbsp; Defaults: ${m.defaultedGamesCount} &nbsp;·&nbsp; Penalty pts: ${m.customPenaltyPoints}</div>
            </div>
            <div class="item-card-actions">
                ${m.status !== 'Banned' ? `<button class="btn btn-danger btn-sm" onclick="doBan(${m.id})">Ban</button>` : ''}
                <button class="btn btn-secondary btn-sm" onclick="doPenalize(${m.id}, '${m.firstName} ${m.lastName}')">Penalize</button>
            </div>`;
        container.appendChild(card);
    });
}

window.doBan = async (id) => {
    if (!confirm('Ban this member?')) return;
    const ok = await handleResponse(await banMember(id));
    if (ok) loadRoster();
};

window.doPenalize = async (id, name) => {
    const pts = parseInt(prompt(`How many penalty points to add to ${name}?`));
    if (!pts || pts < 1) return;
    const ok = await handleResponse(await penalizeMember(id, pts));
    if (ok) loadRoster();
};

document.getElementById('refresh-roster-btn').addEventListener('click', loadRoster);

// ── Tab: Games & Allocation ───────────────────────────────────────────────────

// Holds the currently selected game's data so the allocation view can refresh itself
let activeGameId = null;
let availableTicketsCache = 0;

async function loadOpenGames() {
    const container = document.getElementById('open-games-list');
    container.innerHTML = '<p class="text-muted">Loading…</p>';
    const games = await fetchAdminOpenGames();
    container.innerHTML = '';

    // Hide allocation panel when refreshing game list
    document.getElementById('allocation-panel').classList.add('hidden');
    activeGameId = null;

    if (!games.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">🎮</div>No open games.</div>';
        return;
    }

    games.forEach(g => {
        const card = document.createElement('div');
        card.className = 'item-card accent-red';
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">Arsenal vs ${g.opponent}</div>
                <div class="item-card-meta">Category ${g.category || '—'} &nbsp;·&nbsp; 📅 ${g.matchDate || '—'} &nbsp;·&nbsp; Deadline: ${g.deadline || '—'}</div>
                <div class="item-card-meta">Current ticket pool: <strong>${g.availableTickets}</strong></div>
            </div>
            <div class="item-card-actions">
                <button class="btn btn-primary btn-sm" onclick="openAllocation(${g.id})">Manage Allocation</button>
            </div>`;
        container.appendChild(card);
    });
}

window.openAllocation = async (gameId) => {
    activeGameId = gameId;
    await refreshAllocationPanel();
    document.getElementById('allocation-panel').classList.remove('hidden');
    document.getElementById('allocation-panel').scrollIntoView({ behavior: 'smooth' });
};

async function refreshAllocationPanel() {
    if (!activeGameId) return;
    const data = await fetchGameApplications(activeGameId);
    if (!data) return;

    availableTicketsCache = data.availableTickets;

    document.getElementById('alloc-game-title').textContent = `Arsenal vs ${data.opponent}`;
    updateTicketCounter(data.availableTickets);

    const container = document.getElementById('alloc-apps-list');
    container.innerHTML = '';

    if (!data.applications.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">🎟️</div>No applications for this game.</div>';
        return;
    }

    data.applications.forEach((app, index) => {
        const member = app.member;
        const card = document.createElement('div');
        const accentMap = { Accepted: 'accent-green', Rejected: 'accent-gray', Pending: 'accent-orange' };
        card.className = `item-card ${accentMap[app.status] || 'accent-orange'}`;
        card.id = `app-card-${app.id}`;

        let actionHtml = '';
        if (app.status === 'Pending') {
            if (app.allOrNothing) {
                // All-or-nothing: can only grant exact amount or reject
                actionHtml = `
                    <div class="alloc-row">
                        <span class="badge badge-gold">All or Nothing (${app.ticketsRequested})</span>
                        <button class="btn btn-success btn-sm" onclick="doAllocate(${app.id}, ${app.ticketsRequested})">Grant ${app.ticketsRequested}</button>
                        <button class="btn btn-danger btn-sm"  onclick="doRejectApp(${app.id})">Reject</button>
                    </div>`;
            } else {
                actionHtml = `
                    <div class="alloc-row">
                        <input type="number" id="grant-${app.id}" value="${app.ticketsGranted}" min="1" max="${app.ticketsRequested}"
                            title="Max: ${app.ticketsRequested}" style="width:70px">
                        <span class="text-muted" style="font-size:0.8rem">of ${app.ticketsRequested} requested</span>
                        <button class="btn btn-success btn-sm" onclick="doAllocate(${app.id})">Grant</button>
                        <button class="btn btn-danger btn-sm"  onclick="doRejectApp(${app.id})">Reject</button>
                    </div>`;
            }
        } else if (app.status === 'Accepted' || app.status === 'Partially_Accepted') {
            actionHtml = `
                <div class="alloc-row">
                    <span class="text-muted" style="font-size:0.82rem">Granted: ${app.ticketsGranted} ticket(s)</span>
                    <button class="btn btn-secondary btn-sm" onclick="doDeallocate(${app.id})">↩ Undo</button>
                </div>`;
        }

        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">
                    #${index + 1} &nbsp; ${member.firstName} ${member.lastName}
                    &nbsp; ${appStatusBadge(app.status)}
                </div>
                <div class="item-card-meta">
                    Priority score: <strong>${app.calculatedPriorityScore}</strong>
                    &nbsp;·&nbsp; ALSC # ${member.ALSCMembershipNumber}
                    &nbsp;·&nbsp; Applied: ${new Date(app.appliedAt).toLocaleString()}
                </div>
                <div class="item-card-meta">
                    📊 Total attended: ${member.totalGamesAttended}
                    &nbsp;·&nbsp; This season: ${member.gamesAttendedThisSeason}
                    &nbsp;·&nbsp; Cat A: ${member.categoryAGamesThisSeason}
                    &nbsp;·&nbsp; Defaults: ${member.defaultedGamesCount}
                    &nbsp;·&nbsp; Penalty pts: ${member.customPenaltyPoints}
                </div>
                ${actionHtml ? `<div style="margin-top:10px">${actionHtml}</div>` : ''}
            </div>`;
        container.appendChild(card);
    });
}

function updateTicketCounter(count) {
    availableTicketsCache = count;
    const el = document.getElementById('ticket-counter-value');
    if (el) el.textContent = count;
}

window.doAllocate = async (appId, fixedTickets = null) => {
    const granted = fixedTickets ?? parseInt(document.getElementById(`grant-${appId}`)?.value);
    if (!granted || granted < 1) { toastError('Enter a valid ticket count.'); return; }
    const ok = await handleResponse(await allocateApplication(appId, granted));
    if (ok) refreshAllocationPanel();
};

window.doDeallocate = async (appId) => {
    const ok = await handleResponse(await deallocateApplication(appId));
    if (ok) refreshAllocationPanel();
};

window.doRejectApp = async (appId) => {
    const ok = await handleResponse(await rejectApplication(appId));
    if (ok) refreshAllocationPanel();
};

// Set tickets input
document.getElementById('set-tickets-btn').addEventListener('click', async () => {
    const val = parseInt(document.getElementById('set-tickets-input').value);
    if (!activeGameId || isNaN(val) || val < 0) { toastError('Enter a valid ticket count.'); return; }
    const ok = await handleResponse(await setGameTickets(activeGameId, val));
    if (ok) refreshAllocationPanel();
});

// Close game button
document.getElementById('close-game-btn').addEventListener('click', async () => {
    if (!activeGameId) return;
    if (!confirm('Close this game? All remaining Pending applications will be auto-rejected.')) return;
    const ok = await handleResponse(await closeGame(activeGameId));
    if (ok) {
        document.getElementById('allocation-panel').classList.add('hidden');
        activeGameId = null;
        loadOpenGames();
    }
});

document.getElementById('refresh-games-btn').addEventListener('click', loadOpenGames);

// ── Boot ──────────────────────────────────────────────────────────────────────
loadPendingMembers();
loadRoster();
loadOpenGames();
