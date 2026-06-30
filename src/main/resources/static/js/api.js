// ── Token & Role Helpers ──────────────────────────────────────────────────────

export function getToken()  { return localStorage.getItem('alsc_token'); }
export function getRole()   { return localStorage.getItem('alsc_role'); }
export function isAdmin()   { return getRole() === 'ADMIN'; }
export function isLoggedIn(){ return !!getToken(); }

export function saveSession(token, role) {
    localStorage.setItem('alsc_token', token);
    localStorage.setItem('alsc_role', role);
}

export function clearSession() {
    localStorage.removeItem('alsc_token');
    localStorage.removeItem('alsc_role');
}

// ── Base Fetch ────────────────────────────────────────────────────────────────

async function request(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api' + path, {
        method,
        headers,
        body: body ? JSON.stringify(body) : null,
    });

    // Session expired or forbidden → redirect to login
    if (res.status === 401 || res.status === 403) {
        clearSession();
        alert(res.text());
        window.location.href = '/index.html';
        return null;
    }

    return res;
}

export const api = {
    get:    (path)         => request('GET',    path),
    post:   (path, body)   => request('POST',   path, body),
    delete: (path)         => request('DELETE', path),
};

// ── Auth ──────────────────────────────────────────────────────────────────────

export async function login(email, password) {
    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
    });

    if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || 'Login failed');
    }

    const data = await res.json();
    // Strip the "Bearer " prefix before storing — request() adds it back when sending
    const rawToken = data.token.startsWith('Bearer ') ? data.token.slice(7) : data.token;
    saveSession(rawToken, data.role);
    return data.role;
}

export function logout() {
    clearSession();
    window.location.href = '/index.html';
}

// ── Member ────────────────────────────────────────────────────────────────────

export async function fetchMyProfile() {
    const res = await api.get('/members/me');
    return res.ok ? res.json() : null;
}

// ── Games ─────────────────────────────────────────────────────────────────────

export async function fetchOpenGames() {
    const res = await api.get('/games/open');
    return res.ok ? res.json() : [];
}

// ── Applications ──────────────────────────────────────────────────────────────

export async function fetchMyApplications() {
    const res = await api.get('/applications/my-applications');
    return res.ok ? res.json() : [];
}

export async function submitApplication(gameId, ticketsRequested, allOrNothing) {
    const res = await api.post(
        `/applications/apply?gameId=${gameId}&ticketsRequested=${ticketsRequested}&allOrNothing=${allOrNothing}`
    );
    return res;
}

// ── Admin: Members ────────────────────────────────────────────────────────────

export async function fetchAllMembers() {
    const res = await api.get('/members');
    return res.ok ? res.json() : [];
}

export async function fetchPendingMembers() {
    const res = await api.get('/admin/members/pending');
    return res.ok ? res.json() : [];
}

export async function approveMember(id) {
    return api.post(`/admin/members/${id}/approve`);
}

export async function rejectMember(id) {
    return api.delete(`/admin/members/${id}/reject`);
}

export async function banMember(id) {
    return api.post(`/admin/members/${id}/ban`);
}

export async function penalizeMember(id, points) {
    return api.post(`/admin/members/${id}/penalize?points=${points}`);
}

// ── Admin: Games & Allocation ─────────────────────────────────────────────────

export async function fetchAdminOpenGames() {
    const res = await api.get('/admin/games/open');
    return res.ok ? res.json() : [];
}

export async function setGameTickets(gameId, tickets) {
    return api.post(`/admin/games/${gameId}/set-tickets?tickets=${tickets}`);
}

export async function fetchGameApplications(gameId) {
    const res = await api.get(`/admin/games/${gameId}/applications`);
    return res.ok ? res.json() : null;
}

export async function allocateApplication(appId, ticketsGranted) {
    return api.post(`/admin/applications/${appId}/allocate?ticketsGranted=${ticketsGranted}`);
}

export async function deallocateApplication(appId) {
    return api.post(`/admin/applications/${appId}/deallocate`);
}

export async function rejectApplication(appId) {
    return api.post(`/admin/applications/${appId}/reject`);
}

export async function closeGame(gameId) {
    return api.post(`/admin/games/${gameId}/close`);
}

export async function createGame(payload) {
    return api.post('/admin/games/create', payload);
}

export async function changeMemberType(id, memberType) {
    return api.post(`/admin/members/${id}/change-type?memberType=${memberType}`);
}

export async function deleteMember(id) {
    return api.delete(`/admin/members/${id}/delete`);
}