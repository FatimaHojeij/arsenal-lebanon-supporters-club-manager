import {
    isLoggedIn, isAdmin, logout,
    fetchPendingMembers, approveMember, rejectMember, banMember, penalizeMember,
    resetPenalty, changeMemberType, deleteMember, fetchAllMembers,
    fetchAdminOpenGames, setGameTickets, setGameCategory, fetchGameApplications,
    allocateApplication, deallocateApplication, rejectApplication, unrejectApplication,
    markAttendance, cancelApplication, closeGame, reopenGame, fetchOpenGames, fetchPastGames,
    fetchMyApplications, submitApplication, fetchMyProfile, createGame, changePassword
} from './api.js';

// Guard: must be logged in as admin
if (!isLoggedIn()) window.location.href = '/index.html';
if (!isAdmin())    window.location.href = '/dashboard.html';

let rosterCache = [];
let adminName = { firstName: '', lastName: '' };

// ── Tab switching ─────────────────────────────────────────────────────────────
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        const targetTab = document.getElementById(btn.dataset.tab);
        targetTab.classList.add('active');

        // Re-activate first sub-tab if none are active inside this tab
        const firstSubBtn = targetTab.querySelector('.sub-tab-btn');
        const firstSubPanel = targetTab.querySelector('.sub-tab-panel');
        if (firstSubBtn && !targetTab.querySelector('.sub-tab-btn.active')) {
            firstSubBtn.classList.add('active');
            firstSubPanel.classList.add('active');
        }
    });
});

document.getElementById('logout-btn').addEventListener('click', logout);

function isForcePasswordChange() {
    return new URLSearchParams(window.location.search).get('forcePasswordChange') === '1';
}

function getMemberProfileView() {
    const currentView = new URLSearchParams(window.location.search).get('view');
    if (currentView === 'change-password') return 'change-password';
    return isForcePasswordChange() ? 'change-password' : 'profile';
}

function updateMemberProfileView(view) {
    const nextUrl = new URL(window.location.href);
    if (view === 'change-password') {
        nextUrl.searchParams.set('view', 'change-password');
    } else {
        nextUrl.searchParams.delete('view');
        nextUrl.searchParams.delete('forcePasswordChange');
    }
    window.history.replaceState({}, '', `${nextUrl.pathname}${nextUrl.search}`);
}

function activateMemberProfileTab() {
    const profileTabBtn = document.querySelector('.tab-btn[data-tab="tab-member-view"]');
    const profileTabPanel = document.getElementById('tab-member-view');
    const profileSubBtn = document.querySelector('#tab-member-view .sub-tab-btn[data-subtab="subtab-profile"]');
    const profileSubPanel = document.getElementById('subtab-profile');

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.remove('active'));
    profileTabBtn?.classList.add('active');
    profileTabPanel?.classList.add('active');

    document.querySelectorAll('#tab-member-view .sub-tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('#tab-member-view .sub-tab-panel').forEach(panel => panel.classList.remove('active'));
    profileSubBtn?.classList.add('active');
    profileSubPanel?.classList.add('active');
}

document.getElementById('admin-password-change-panel')?.classList.add('hidden');

if (isForcePasswordChange() && !new URLSearchParams(window.location.search).get('view')) {
    updateMemberProfileView('change-password');
}

if (new URLSearchParams(window.location.search).get('view') === 'change-password' ||
    new URLSearchParams(window.location.search).get('view') === 'profile' ||
    isForcePasswordChange()) {
    activateMemberProfileTab();
}

document.getElementById('admin-change-password-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const messageEl = document.getElementById('admin-change-password-message');
    const currentPassword = document.getElementById('admin-current-password').value;
    const newPassword = document.getElementById('admin-new-password').value;

    try {
        await changePassword(currentPassword, newPassword);
        updateMemberProfileView('profile');
        window.location.href = '/admin.html?view=profile';
    } catch (err) {
        messageEl.textContent = err.message.replace(/^[^\w]*/, '');
        messageEl.className = 'alert alert-error';
        messageEl.classList.remove('hidden');
    }
});

// ── Sub-tab switching (Member View) ───────────────────────────────────────────
document.querySelectorAll('#tab-member-view .sub-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('#tab-member-view .sub-tab-btn')
            .forEach(b => b.classList.remove('active'));
        document.querySelectorAll('#tab-member-view .sub-tab-panel')
            .forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(btn.dataset.subtab).classList.add('active');
    });
});

// ── Sub-tab switching (Games) ─────────────────────────────────────────────────
document.querySelectorAll('#tab-games .sub-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('#tab-games .sub-tab-btn')
            .forEach(b => b.classList.remove('active'));
        document.querySelectorAll('#tab-games .sub-tab-panel')
            .forEach(p => p.classList.remove('active'));
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
                        <select id="admin-tix-${game.id}" style="width:auto;padding:5px 8px;font-size:0.85rem"
                            onchange="renderAdminTicketNameInputs(${game.id})">
                            <option value="1">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </div>
                    <label style="display:flex;align-items:center;gap:6px;font-size:0.85rem;font-weight:600;cursor:pointer;margin-top:18px">
                        <input type="checkbox" id="admin-aon-${game.id}" style="width:auto;margin:0">
                        All or Nothing
                    </label>
                    <button class="btn btn-primary btn-sm" style="margin-top:18px"
                        onclick="adminApplyForGame(${game.id})">Apply</button>
                </div>
                <div id="admin-names-container-${game.id}"></div>`;
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

window.renderAdminTicketNameInputs = (gameId) => {
    const ticketCount = parseInt(document.getElementById(`admin-tix-${gameId}`).value);
    const container = document.getElementById(`admin-names-container-${gameId}`);
    if (!container) return;

    if (ticketCount <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `<p class="text-muted" style="font-size:0.82rem;margin:10px 0 6px">
        A name must be specified for each ticket.
    </p>`;
    for (let i = 0; i < ticketCount; i++) {
        const defaultVal = i === 0 ? `${adminName.firstName} ${adminName.lastName}`.trim() : '';
        html += `<div class="form-group" style="margin-bottom:8px;max-width:320px">
            <input type="text" id="admin-name-${gameId}-${i}" placeholder="Ticket ${i + 1} holder name"
                value="${defaultVal}" required>
        </div>`;
    }
    container.innerHTML = html;
};

window.adminApplyForGame = async (gameId) => {
    const tickets      = document.getElementById(`admin-tix-${gameId}`).value;
    const allOrNothing = document.getElementById(`admin-aon-${gameId}`).checked;

    let ticketHolderNames = [];
    if (parseInt(tickets) > 1) {
        for (let i = 0; i < parseInt(tickets); i++) {
            const val = document.getElementById(`admin-name-${gameId}-${i}`)?.value.trim();
            if (!val) {
                alert('Please enter a name for each ticket.');
                return;
            }
            ticketHolderNames.push(val);
        }
    }

    const res  = await submitApplication(gameId, tickets, allOrNothing, ticketHolderNames);
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

    const profileView = getMemberProfileView();

    if (profileView === 'change-password') {
        activateMemberProfileTab();
        container.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:12px">
                <h3 style="margin:0">Change Password</h3>
                <button id="member-profile-back-btn" type="button" class="btn btn-secondary btn-sm">← Back to Profile</button>
            </div>
            <div class="item-card accent-orange" style="flex-direction:column">
                <form id="member-profile-change-password-form" class="flex-gap" style="flex-wrap:wrap;align-items:flex-end">
                    <div class="form-group" style="margin:0;min-width:220px">
                        <label for="member-profile-current-password">Current Password</label>
                        <input type="password" id="member-profile-current-password" required>
                    </div>
                    <div class="form-group" style="margin:0;min-width:220px">
                        <label for="member-profile-new-password">New Password</label>
                        <input type="password" id="member-profile-new-password" minlength="8" required>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm">Update Password</button>
                </form>
                <div id="member-profile-change-password-message" class="alert hidden" style="margin-top:12px"></div>
            </div>`;

        document.getElementById('member-profile-back-btn')?.addEventListener('click', () => {
            updateMemberProfileView('profile');
            loadMemberProfile();
        });

        document.getElementById('member-profile-change-password-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const messageEl = document.getElementById('member-profile-change-password-message');
            const currentPassword = document.getElementById('member-profile-current-password').value;
            const newPassword = document.getElementById('member-profile-new-password').value;

            try {
                await changePassword(currentPassword, newPassword);
                updateMemberProfileView('profile');
                window.location.href = '/admin.html?view=profile';
            } catch (err) {
                messageEl.textContent = err.message.replace(/^[^\w]*/, '');
                messageEl.className = 'alert alert-error';
                messageEl.classList.remove('hidden');
            }
        });

        setTimeout(() => document.getElementById('member-profile-current-password')?.focus(), 0);
        return;
    }

    container.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:12px">
            <h3 style="margin:0">My Profile</h3>
            <button id="member-profile-change-password-link" type="button" class="btn btn-warning btn-sm">Change Password</button>
        </div>
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

    document.getElementById('member-profile-change-password-link')?.addEventListener('click', () => {
        updateMemberProfileView('change-password');
        loadMemberProfile();
    });
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
                ${m.customPenaltyPoints > 0 ? `<button class="btn btn-secondary btn-sm" onclick="doResetPenalty(${m.id}, '${m.firstName} ${m.lastName}')">Reset Points</button>` : ''}
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

window.doResetPenalty = async (id, name) => {
    if (!confirm(`Reset all penalty points for ${name} to 0?`)) return;
    const ok = await handleResponse(await resetPenalty(id));
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

// ── Tab: Attendance ───────────────────────────────────────────────────────────
let activeAttendanceGameId = null;

async function loadPastGames() {
    const container = document.getElementById('past-games-list');
    container.innerHTML = '<p class="text-muted">Loading…</p>';

    const games = await fetchPastGames();
    container.innerHTML = '';

    document.getElementById('attendance-panel').classList.add('hidden');
    activeAttendanceGameId = null;

    if (!games.length) {
        container.innerHTML =
            '<div class="empty-state"><div class="icon">📅</div>' +
            'No past games yet.</div>';
        return;
    }

    games.forEach(g => {
        const today = new Date(); today.setHours(0, 0, 0, 0);
        const matchDate = new Date(g.matchDate); matchDate.setHours(0, 0, 0, 0);
        const isMatchPassed = !!g.matchDate && matchDate < today;
        const isClosedUpcomingGame = g.applicationsOpen === false && !!g.matchDate && !isMatchPassed;

        const card = document.createElement('div');
        card.className = 'item-card accent-gray';
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">Arsenal vs ${g.opponent}</div>
                <div class="item-card-meta">
                    Category ${g.category || '—'}
                    &nbsp;·&nbsp; 📅 ${g.matchDate}
                    &nbsp;·&nbsp; ${g.competition.replace(/_/g, ' ')}
                </div>
            </div>
            <div class="item-card-actions">
                ${isClosedUpcomingGame ? `
                    <button class="btn btn-warning btn-sm"
                        onclick="doReopenGame(${g.id})">
                        Re-open Game
                    </button>
                ` : ''}
                ${isMatchPassed ? `
                    <button class="btn btn-primary btn-sm"
                        onclick="openAttendancePanel(${g.id})">
                        Mark Attendance
                    </button>
                ` : ''}
            </div>`;
        container.appendChild(card);
    });
}

window.openAttendancePanel = async (gameId) => {
    activeAttendanceGameId = gameId;
    await refreshAttendancePanel();
    document.getElementById('attendance-panel').classList.remove('hidden');
    document.getElementById('attendance-panel')
        .scrollIntoView({ behavior: 'smooth' });
};

async function refreshAttendancePanel() {
    if (!activeAttendanceGameId) return;
    const data = await fetchGameApplications(activeAttendanceGameId);
    if (!data) return;

    document.getElementById('attendance-game-title').textContent =
        `Arsenal vs ${data.opponent}`;

    const container = document.getElementById('attendance-apps-list');
    container.innerHTML = '';

    const today = new Date(); today.setHours(0, 0, 0, 0);
    const matchDate = new Date(data.matchDate); matchDate.setHours(0, 0, 0, 0);
    if (matchDate >= today) {
        container.innerHTML =
            '<div class="empty-state"><div class="icon">🗓️</div>' +
            'Attendance can only be marked after the match date has passed.</div>';
        return;
    }

    // Only show accepted/partially accepted applications
    const relevant = data.applications.filter(a =>
        a.status === 'Accepted' || a.status === 'Partially_Accepted'
    );

    if (!relevant.length) {
        container.innerHTML =
            '<div class="empty-state"><div class="icon">🎟️</div>' +
            'No accepted applications for this game.</div>';
        return;
    }

    relevant.forEach(app => {
        const card = document.createElement('div');

        let actionHtml;
        if (app.attended === null || app.attended === undefined) {
            actionHtml = `
                <div class="alloc-row">
                    <button class="btn btn-success btn-sm"
                        onclick="doMarkAttendance(${app.id}, true)">
                        ✅ Attended
                    </button>
                    <button class="btn btn-danger btn-sm"
                        onclick="doMarkAttendance(${app.id}, false)">
                        ❌ Defaulted
                    </button>
                </div>`;
        } else {
            const outcomeHtml = app.attended
                ? '<span class="badge badge-green">✅ Attended</span>'
                : '<span class="badge badge-red">❌ Defaulted</span>';
            actionHtml = `<div class="alloc-row">${outcomeHtml}</div>`;
        }

        const accentMap = {
            Accepted: 'accent-green',
            Partially_Accepted: 'accent-green-light'
        };
        card.className = `item-card ${accentMap[app.status]}`;
        card.id = `att-card-${app.id}`;
        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">
                    ${app.member.firstName} ${app.member.lastName}
                    &nbsp; ${appStatusBadge(app.status)}
                </div>
                <div class="item-card-meta">
                    ALSC # ${app.member.ALSCMembershipNumber}
                    &nbsp;·&nbsp; Granted: ${app.ticketsGranted} ticket(s)
                </div>
                <div style="margin-top:10px">${actionHtml}</div>
            </div>`;
        container.appendChild(card);
    });
}

// Update doMarkAttendance to refresh whichever panel is active
window.doMarkAttendance = async (appId, attended) => {
    const label = attended ? 'Attended' : 'Defaulted';
    if (!confirm(
        `Mark as ${label}? This updates the member's stats and cannot be undone.`
    )) return;
    const ok = await handleResponse(await markAttendance(appId, attended));
    if (ok) {
        // Refresh whichever panel triggered this
        if (activeAttendanceGameId) refreshAttendancePanel();
        else refreshAllocationPanel();
    }
};

document.getElementById('refresh-attendance-btn')
    .addEventListener('click', loadPastGames);

document.getElementById('close-attendance-btn').addEventListener('click', () => {
    document.getElementById('attendance-panel').classList.add('hidden');
    activeAttendanceGameId = null;
});

async function refreshAllocationPanel() {
    if (!activeGameId) return;
    const data = await fetchGameApplications(activeGameId);
    if (!data) return;

    availableTicketsCache = data.availableTickets;

    document.getElementById('alloc-game-title').textContent =
        `Arsenal vs ${data.opponent} (Category: ${data.category})`;
    updateTicketCounter(data.availableTickets);

    document.getElementById('alloc-category-select').value = data.category;
    document.getElementById('alloc-category-warning').classList.toggle('hidden', data.category !== 'NA');


    // Determine time window once for all cards
    const today     = new Date(); today.setHours(0, 0, 0, 0);
    const matchDate = new Date(data.matchDate); matchDate.setHours(0, 0, 0, 0);
    const matchPassed = matchDate < today;

    const container = document.getElementById('alloc-apps-list');
    container.innerHTML = '';

    if (!data.applications.length) {
        container.innerHTML =
            '<div class="empty-state"><div class="icon">🎟️</div>' +
            'No applications for this game.</div>';
        return;
    }

    data.applications.forEach((app, index) => {
        const member = app.member;
        const card   = document.createElement('div');

        const accentMap = {
            Accepted:           'accent-green',
            Partially_Accepted: 'accent-green',
            Rejected:           'accent-gray',
            Pending:            'accent-orange'
        };
        card.className = `item-card ${accentMap[app.status] || 'accent-orange'}`;
        card.id = `app-card-${app.id}`;

        let actionHtml = '';

        if (app.status === 'Pending') {
            // ── Pending: allocate or reject ───────────────────────────────
            if (app.allOrNothing) {
                actionHtml = `
                    <div class="alloc-row">
                        <span class="badge badge-gold">
                            All or Nothing (${app.ticketsRequested})
                        </span>
                        <button class="btn btn-success btn-sm"
                            onclick="doAllocate(${app.id}, ${app.ticketsRequested})">
                            Grant ${app.ticketsRequested}
                        </button>
                        <button class="btn btn-danger btn-sm"
                            onclick="doRejectApp(${app.id})">Reject</button>
                    </div>`;
            } else {
                actionHtml = `
                    <div class="alloc-row">
                        <input type="number" id="grant-${app.id}"
                            value="${app.ticketsGranted || 1}"
                            min="1" max="${app.ticketsRequested}"
                            title="Max: ${app.ticketsRequested}"
                            style="width:70px">
                        <span class="text-muted" style="font-size:0.8rem">
                            of ${app.ticketsRequested} requested
                        </span>
                        <button class="btn btn-success btn-sm"
                            onclick="doAllocate(${app.id})">Grant</button>
                        <button class="btn btn-danger btn-sm"
                            onclick="doRejectApp(${app.id})">Reject</button>
                    </div>`;
            }

        } else if (app.status === 'Accepted' || app.status === 'Partially_Accepted') {
            // ── Accepted: behaviour depends on whether match has passed ───
            if (!matchPassed) {
                // Pre-match: undo allocation or cancel entirely
                actionHtml = `
                    <div class="alloc-row">
                        <span class="text-muted" style="font-size:0.82rem">
                            Granted: ${app.ticketsGranted} ticket(s)
                        </span>
                        <button class="btn btn-secondary btn-sm"
                            onclick="doDeallocate(${app.id})">↩ Undo</button>
                        <button class="btn btn-danger btn-sm"
                            onclick="doCancelApp(${app.id})">🚫 Cancel</button>
                    </div>`;

            } else if (app.attended === null || app.attended === undefined) {
                // Post-match: attendance not yet marked
                actionHtml = `
                    <div class="alloc-row">
                        <span class="text-muted" style="font-size:0.82rem">
                            Granted: ${app.ticketsGranted} ticket(s)
                        </span>
                        <button class="btn btn-success btn-sm"
                            onclick="doMarkAttendance(${app.id}, true)">
                            ✅ Attended
                        </button>
                        <button class="btn btn-danger btn-sm"
                            onclick="doMarkAttendance(${app.id}, false)">
                            ❌ Defaulted
                        </button>
                    </div>`;

            } else {
                // Post-match: already marked — locked
                const outcomeHtml = app.attended
                    ? '<span class="badge badge-green">✅ Attended</span>'
                    : '<span class="badge badge-red">❌ Defaulted</span>';
                actionHtml = `
                    <div class="alloc-row">
                        <span class="text-muted" style="font-size:0.82rem">
                            Granted: ${app.ticketsGranted} ticket(s)
                        </span>
                        ${outcomeHtml}
                    </div>`;
            }

        } else if (app.status === 'Rejected') {
            // ── Rejected: undo rejection if game still open ───────────────
            actionHtml = `
                <div class="alloc-row">
                    <span class="text-muted" style="font-size:0.82rem">
                        This application was rejected
                    </span>
                    <button class="btn btn-secondary btn-sm"
                        onclick="doUnreject(${app.id})">↩ Undo Rejection</button>
                </div>`;
        }

        card.innerHTML = `
            <div class="item-card-body">
                <div class="item-card-title">
                    #${index + 1} &nbsp;
                    ${member.firstName} ${member.lastName}
                    &nbsp; ${appStatusBadge(app.status)}
                </div>
                <div class="item-card-meta">
                    Priority score: <strong>${app.calculatedPriorityScore}</strong>
                    &nbsp;·&nbsp; ALSC # ${member.ALSCMembershipNumber}
                    &nbsp;·&nbsp; Applied: ${new Date(app.appliedAt).toLocaleString()}
                </div>
                ${app.ticketHolderNames && app.ticketHolderNames.length ? `
                <div class="item-card-meta">
                    🎫 Ticket holders: ${app.ticketHolderNames.join(', ')}
                </div>` : ''}
                <div class="item-card-meta">
                    📊 Total attended: ${member.totalGamesAttended}
                    &nbsp;·&nbsp; This season: ${member.gamesAttendedThisSeason}
                    &nbsp;·&nbsp; Cat A: ${member.categoryAGamesThisSeason}
                    &nbsp;·&nbsp; Defaults: ${member.defaultedGamesCount}
                    &nbsp;·&nbsp; Penalty pts: ${member.customPenaltyPoints}
                </div>
                ${actionHtml
            ? `<div style="margin-top:10px">${actionHtml}</div>`
            : ''}
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

window.doMarkAttendance = async (appId, attended) => {
    const label = attended ? 'Attended' : 'Defaulted';
    if (!confirm(
        `Mark as ${label}? This updates the member's stats and cannot be undone.`
    )) return;
    const ok = await handleResponse(await markAttendance(appId, attended));
    if (ok) refreshAllocationPanel();
};

window.doCancelApp = async (appId) => {
    if (!confirm(
        'Cancel this application? The member will not be notified and their ' +
        'tickets will be returned to the pool.'
    )) return;
    const ok = await handleResponse(await cancelApplication(appId));
    if (ok) refreshAllocationPanel();
};

window.doReopenGame = async (gameId) => {
    if (!confirm('Re-open this game so allocations can be adjusted again?')) return;
    const ok = await handleResponse(await reopenGame(gameId));
    if (ok) {
        loadPastGames();
        loadOpenGames();
    }
};

// Set tickets input
document.getElementById('set-tickets-btn').addEventListener('click', async () => {
    const val = parseInt(document.getElementById('set-tickets-input').value);
    if (!activeGameId || isNaN(val) || val < 0) { toastError('Enter a valid ticket count.'); return; }
    const ok = await handleResponse(await setGameTickets(activeGameId, val));
    if (ok) refreshAllocationPanel();
});

// Set category button
document.getElementById('set-category-btn').addEventListener('click', async () => {
    const val = document.getElementById('alloc-category-select').value;
    if (!activeGameId) return;
    if (val === 'NA') { toastError('Select A, B, or C — NA is not a valid final category.'); return; }
    const ok = await handleResponse(await setGameCategory(activeGameId, val));
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
loadPastGames();
loadMemberGames();
loadMemberApplications();
loadMemberProfile();
fetchMyProfile().then(p => { if (p) adminName = { firstName: p.firstName, lastName: p.lastName }; });
