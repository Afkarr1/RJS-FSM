const API_BASE = '/api';

export function getAuthHeader() {
  const creds = localStorage.getItem('rjs_auth');
  if (!creds) return {};
  return { Authorization: `Basic ${creds}` };
}

async function request(method, path, { body, isFormData, isPublic } = {}) {
  const headers = isPublic ? {} : getAuthHeader();
  if (body && !isFormData) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: isFormData ? body : body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    localStorage.removeItem('rjs_auth');
    localStorage.removeItem('rjs_user');
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Request failed' }));
    throw { status: res.status, ...err };
  }

  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  get: (path, opts) => request('GET', path, opts),
  post: (path, body, opts) => request('POST', path, { body, ...opts }),
  put: (path, body, opts) => request('PUT', path, { body, ...opts }),
  delete: (path, opts) => request('DELETE', path, opts),
  upload: (path, formData) => request('POST', path, { body: formData, isFormData: true }),
};

// Auth
export const authApi = {
  login: async (username, password) => {
    const trimmedUsername = username.trim();
    const creds = btoa(`${trimmedUsername}:${password}`);
    const res = await fetch(`${API_BASE}/me`, {
      headers: { Authorization: `Basic ${creds}` },
    });
    if (res.status === 401) {
      throw new Error('Username atau password salah');
    }
    if (!res.ok) {
      throw new Error('Login gagal');
    }
    const data = await res.json();
    const authorities = data.authorities || [];
    if (authorities.includes('ROLE_ADMIN')) {
      return { creds, role: 'ADMIN', username: trimmedUsername };
    }
    if (authorities.includes('ROLE_TECHNICIAN')) {
      return { creds, role: 'TECHNICIAN', username: trimmedUsername };
    }
    throw new Error('Role tidak dikenali');
  },
};

// Admin APIs
export const adminApi = {
  getUsers: () => api.get('/admin/users'),
  getTechnicians: () => api.get('/admin/users/technicians'),
  getUser: (id) => api.get(`/admin/users/${id}`),
  createUser: (data) => api.post('/admin/users', data),
  updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
  setUserActive: (id, active) => api.put(`/admin/users/${id}/active`, { active }),
  resetPassword: (id, newPassword) => api.put(`/admin/users/${id}/password`, { newPassword }),

  getJobs: (status) => api.get(`/admin/jobs${status ? `?status=${status}` : ''}`),
  getJob: (id) => api.get(`/admin/jobs/${id}`),
  createJob: (data) => api.post('/admin/jobs', data),
  assignJob: (id, data) => api.post(`/admin/jobs/${id}/assign`, data),
  rescheduleJob: (id, data) => api.post(`/admin/jobs/${id}/reschedule`, data),
  closeJob: (id, data) => api.post(`/admin/jobs/${id}/close`, data || {}),
  cancelJob: (id) => api.post(`/admin/jobs/${id}/cancel`),
  getJobHistory: (id) => api.get(`/admin/jobs/${id}/history`),
  getJobPhotos: (id) => api.get(`/admin/jobs/${id}/photos`),

  getAuditLogs: () => api.get('/admin/audit/logs'),
  getLoginAudit: () => api.get('/admin/audit/logins'),

  getCustomers: (search) => api.get(`/admin/customers${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  createCustomer: (data) => api.post('/admin/customers', data),
  updateCustomer: (id, data) => api.put(`/admin/customers/${id}`, data),
  deleteCustomer: (id) => api.delete(`/admin/customers/${id}`),
};

// Tech APIs
export const techApi = {
  getJobs: (activeOnly = false) => api.get(`/tech/jobs?activeOnly=${activeOnly}`),
  getJob: (id) => api.get(`/tech/jobs/${id}`),
  transit: (id) => api.post(`/tech/jobs/${id}/transit`),
  startJob: (id) => api.post(`/tech/jobs/${id}/start`),
  finishJob: (id) => api.post(`/tech/jobs/${id}/finish`),
  followUp: (id, reason) => api.post(`/tech/jobs/${id}/followup`, { reason }),
  uploadPhoto: (jobId, formData) => api.upload(`/tech/jobs/${jobId}/photos`, formData),
  getPhotos: (jobId) => api.get(`/tech/jobs/${jobId}/photos`),
  getJobHistory: (id) => api.get(`/tech/jobs/${id}/history`),
};

// Public APIs
export const publicApi = {
  getReview: (token) => api.get(`/public/reviews/${token}`, { isPublic: true }),
  submitReview: (token, formData) =>
    request('POST', `/public/reviews/${token}`, { body: formData, isFormData: true, isPublic: true }),
};
