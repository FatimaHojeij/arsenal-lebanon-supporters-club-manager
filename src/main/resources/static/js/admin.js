import {
    isLoggedIn, isAdmin, logout,
    fetchPendingMembers, approveMember, rejectMember, banMember, penalizeMember,
    changeMemberType, deleteMember, fetchAllMembers,
    fetchAdminOpenGames, setGameTickets, fetchGameApplications,
    allocateApplication, deallocateApplication, rejectApplication, unrejectApplication,
    closeGame, fetchOpenGames, fetchMyApplications, submitApplication, fetchMyProfile,
    createGame
} from './api.js';

// Guard: must be logged in as admin
if (!isLoggedIn()) window.location.href = '/index.html';
if (!isAdmin())    window.location.href = '/dashboard.html';

let rosterCache = [];

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

// ── Sub-tab switching (Member View) ───────────────────────────────────────────
document.querySelectorAll('.sub-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.sub-tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.sub-tab-panel').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(btn.dataset.subtab).classList.add('active');
    });
});

// ── Member View: Helpers ──────────────────────────────────────────────────────
function memberDeadlineInfo(deadlineStr) {
    if (!deadlineStr) return { text: '', isPast: false };
    const today    = new Date(); today.setHours(0,0,0,0);
    const deadline = new Date(deadlineStr); deadline.setHours(0,0,0,0);
    const days     = Math.ceil((deadline - today) / 86400000);
    if (days > 1)   return { text: `⏳ ${days} days left to apply`,   isPast: false };
    if (days === 1) return { text: `⚠️ Application closes tomorrow!`, isPast: false };
    if (days === 0) return { text: `🚨 Application closes TODAY!`,    isPast: false };
    return              { text: `Applications closed`,                 isPast: true  };
}

function memberStatusBadge(status) {
    const map = { Accepted: 'badge-green', Rejected: 'badge-red', Pending: 'badge-orange' };
    return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

// ── Member View: Open Fixtures ────────────────────────────────────────────────
async function loadMemberGames() {
    const container = document.getElementById('member-games-list');
    container.innerHTML = '<p class="text-muted">Loading fixtures…</p>';

    const [games, myApps] = await Promise.all([fetchOpenGames(), fetchMyApplications()]);
    const appliedIds = new Set(myApps.map(a => a.game.id));
    container.innerHTML = '';

    if (!games.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">⚽</div>No open fixtures right now.</div>';
        return;
    }

    games.forEach(game => {
        const { text: deadlineText, isPast } = memberDeadlineInfo(game.deadline);
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
                        <label for="admin-tix-${game.id}" style="font-size:0.8rem">Tickets</label>
                        <select id="admin-tix-${game.id}" style="width:auto;padding:5px 8px;font-size:0.85rem">
                            <option value="1">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                        </select>
                    </div>
                    <label style="display:flex;align-items:center;gap:6px;font-size:0.85rem;font-weight:600;cursor:pointer;margin-top:18px">
                        <input type="checkbox" id="admin-aon-${game.id}" style="width:auto;margin:0">
                        All or Nothing
                    </label>
                    <button class="btn btn-primary btn-sm" style="margin-top:18px"
                        onclick="adminApplyForGame(${game.id})">Apply</button>
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

window.adminApplyForGame = async (gameId) => {
    const tickets      = document.getElementById(`admin-tix-${gameId}`).value;
    const allOrNothing = document.getElementById(`admin-aon-${gameId}`).checked;
    const res  = await submitApplication(gameId, tickets, allOrNothing);
    const text = await res.text();
    alert(text);
    loadMemberGames();
    loadMemberApplications();
};

document.getElementById('refresh-member-games-btn').addEventListener('click', loadMemberGames);

// ── Member View: My Applications ──────────────────────────────────────────────
async function loadMemberApplications() {
    const container = document.getElementById('member-apps-list');
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
            <div>${memberStatusBadge(app.status)}</div>`;
        container.appendChild(card);
    });
}

document.getElementById('refresh-member-apps-btn').addEventListener('click', loadMemberApplications);

// ── Member View: Profile ──────────────────────────────────────────────────────
async function loadMemberProfile() {
    const container = document.getElementById('member-profile-panel');
    container.innerHTML = '<p class="text-muted">Loading profile…</p>';

    const p = await fetchMyProfile();
    if (!p) { container.innerHTML = '<p class="text-muted">Could not load profile.</p>'; return; }

    const statusBadgeHtml = {
        Active:  '<span class="badge badge-green">Active</span>',
        Lapsed:  '<span class="badge badge-orange">Lapsed</span>',
        Banned:  '<span class="badge badge-red">Banned</span>',
        Pending: '<span class="badge badge-gray">Pending</span>',
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
        </div>`;
}

document.getElementById('refresh-member-apps-btn').addEventListener('click', loadMemberApplications);

// ── Helpers ───────────────────────────────────────────────────────────────────
function statusBadge(status) {
    const map = { Active: 'badge-green', Lapsed: 'badge-orange', Banned: 'badge-red', Pending: 'badge-gray' };
    return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

function appStatusBadge(status) {
    const map = { Accepted: 'badge-green', Partially_Accepted: 'badge-green', Rejected: 'badge-red', Pending: 'badge-orange' };
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

function renderRoster(members) {
    const container = document.getElementById('roster-list');
    container.innerHTML = '';

    if (!members.length) {
        container.innerHTML = '<div class="empty-state"><div class="icon">👥</div>No members found.</div>';
        return;
    }

    const allTypes = ['President', 'Secretary', 'Treasurer', 'Board', 'Permanent', 'Default'];

    members.forEach(m => {
        const accentMap = { Active: 'accent-green', Lapsed: 'accent-orange', Banned: 'accent-red', Pending: 'accent-gray' };
        const card = document.createElement('div');
        card.className = `item-card ${accentMap[m.status] || 'accent-gray'}`;
        card.id = `roster-${m.id}`;

        const typeOptions = allTypes
            .map(t => `<option value="${t}" ${t === m.memberType ? 'selected' : ''}>${t}</option>`)
            .join('');

        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">${m.title || ''} ${m.firstName} ${m.lastName} ${statusBadge(m.status)}</div>
                <div class="item-card-meta">📧 ${m.email} &nbsp;·&nbsp; ALSC # ${m.ALSCMembershipNumber}</div>
                <div class="item-card-meta">⚽ ${m.totalGamesAttended} attended &nbsp;·&nbsp; Defaults: ${m.defaultedGamesCount} &nbsp;·&nbsp; Penalty pts: ${m.customPenaltyPoints}</div>
                <div class="flex-gap" style="margin-top:10px">
                    <select id="type-select-${m.id}" style="width:auto;padding:5px 8px;font-size:0.85rem">
                        ${typeOptions}
                    </select>
                    <button class="btn btn-secondary btn-sm" onclick="doChangeType(${m.id})">Update Type</button>
                </div>
            </div>
            <div class="item-card-actions">
                ${m.status !== 'Banned' ? `<button class="btn btn-danger btn-sm" onclick="doBan(${m.id})">Ban</button>` : ''}
                <button class="btn btn-secondary btn-sm" onclick="doPenalize(${m.id}, '${m.firstName} ${m.lastName}')">Penalize</button>
                <button class="btn btn-danger btn-sm" onclick="doDeleteMember(${m.id}, '${m.firstName} ${m.lastName}')">Delete</button>
            </div>`;
        container.appendChild(card);
    });
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
    rosterCache = members;
    container.innerHTML = '';
    renderRoster(members);
}

window.doChangeType = async (id) => {
    const selected = document.getElementById(`type-select-${id}`).value;
    const ok = await handleResponse(await changeMemberType(id, selected));
    if (ok) loadRoster();
};

window.doDeleteMember = async (id, name) => {
    const confirmed = confirm(
        `⚠️ Delete member "${name}"?\n\nThis action is permanent and cannot be undone.\n\nAre you sure?`
    );
    if (!confirmed) return;
    const ok = await handleResponse(await deleteMember(id));
    if (ok) document.getElementById(`roster-${id}`)?.remove();
};

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
// ── Create Game Form ──────────────────────────────────────────────────────────
document.getElementById('create-game-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const msgEl     = document.getElementById('create-game-message');
    const submitBtn = document.getElementById('create-game-btn');
    msgEl.className = 'alert hidden';
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating…';

    const ticketsVal = document.getElementById('ng-tickets').value;

    const payload = {
        opponent:         document.getElementById('ng-opponent').value.trim(),
        category:         document.getElementById('ng-category').value,
        competition:      document.getElementById('ng-competition').value,
        matchDate:        document.getElementById('ng-matchdate').value,
        deadline:         document.getElementById('ng-deadline').value,
        availableTickets: ticketsVal === '' ? 0 : parseInt(ticketsVal),
    };

    try {
        const res  = await createGame(payload);
        const text = await res.text();

        if (res.ok) {
            msgEl.className = 'alert alert-success';
            msgEl.textContent = text.replace(/^[^\w]*/, '');
            document.getElementById('create-game-form').reset();
            loadOpenGames();
        } else {
            // Validation errors come back as JSON, business errors as plain text
            let display;
            try {
                const json = JSON.parse(text);
                display = Array.isArray(json.errors) ? json.errors.join(' ') : (json.message || text);
            } catch {
                display = text.replace(/^[^\w]*/, '');
            }
            msgEl.className = 'alert alert-error';
            msgEl.textContent = display;
        }
    } catch {
        msgEl.className = 'alert alert-error';
        msgEl.textContent = 'Network error: could not reach the server.';
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Game';
        msgEl.classList.remove('hidden');
    }
});

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
        const accentMap = { Accepted: 'accent-green', Partially_Accepted: 'accent-green', Rejected: 'accent-gray', Pending: 'accent-orange' };
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
        } else if (app.status === 'Rejected') {
            actionHtml = `
        <div class="alloc-row">
            <span class="text-muted" style="font-size:0.82rem">This application was rejected</span>
            <button class="btn btn-secondary btn-sm" onclick="doUnreject(${app.id})">↩ Undo Rejection</button>
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

window.doUnreject = async (appId) => {
    const ok = await handleResponse(await unrejectApplication(appId));
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

document.getElementById('roster-search').addEventListener('input', (e) => {
    const query = e.target.value.trim().toLowerCase();
    if (!query) {
        renderRoster(rosterCache);
        return;
    }
    const filtered = rosterCache.filter(m =>
        m.firstName.toLowerCase().includes(query) ||
        m.lastName.toLowerCase().includes(query) ||
        `${m.firstName} ${m.lastName}`.toLowerCase().includes(query) ||
        String(m.ALSCMembershipNumber).includes(query)
    );
    renderRoster(filtered);
});

// ── Boot ──────────────────────────────────────────────────────────────────────
loadPendingMembers();
loadRoster();
loadOpenGames();
loadMemberGames();
loadMemberApplications();
loadMemberProfile();
