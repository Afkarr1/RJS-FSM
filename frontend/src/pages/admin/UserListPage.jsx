import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Plus,
  Edit3,
  KeyRound,
  ToggleLeft,
  ToggleRight,
  Users,
} from 'lucide-react';
import { adminApi } from '../../api/client';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';
import Modal from '../../components/Modal';
import { useToast } from '../../components/Toast';

export default function UserListPage() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal states
  const [createModal, setCreateModal] = useState(false);
  const [editModal, setEditModal] = useState(false);
  const [resetModal, setResetModal] = useState(false);
  const [toggleConfirm, setToggleConfirm] = useState(null);

  const [selectedUser, setSelectedUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form states
  const [createForm, setCreateForm] = useState({
    username: '',
    fullName: '',
    password: '',
    role: 'TECHNICIAN',
    phoneE164: '',
  });
  const [editForm, setEditForm] = useState({ fullName: '', role: '', phoneE164: '' });
  const [newPassword, setNewPassword] = useState('');

  const fetchUsers = async () => {
    try {
      const data = await adminApi.getUsers();
      setUsers(data || []);
    } catch (err) {
      toast.error('Gagal memuat daftar pengguna');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleCreate = async () => {
    if (!createForm.username || !createForm.password) {
      toast.error('Username dan password wajib diisi');
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.createUser(createForm);
      toast.success('Pengguna berhasil dibuat');
      setCreateModal(false);
      setCreateForm({ username: '', fullName: '', password: '', role: 'TECHNICIAN', phoneE164: '' });
      fetchUsers();
    } catch (err) {
      toast.error(err?.message || 'Gagal membuat pengguna');
    } finally {
      setSubmitting(false);
    }
  };

  const openEdit = (user) => {
    setSelectedUser(user);
    setEditForm({
      fullName: user.fullName || '',
      role: user.role || 'TECHNICIAN',
      phoneE164: user.phoneE164 || '',
    });
    setEditModal(true);
  };

  const handleEdit = async () => {
    setSubmitting(true);
    try {
      await adminApi.updateUser(selectedUser.id, editForm);
      toast.success('Pengguna berhasil diperbarui');
      setEditModal(false);
      fetchUsers();
    } catch (err) {
      toast.error(err?.message || 'Gagal memperbarui pengguna');
    } finally {
      setSubmitting(false);
    }
  };

  const openResetPassword = (user) => {
    setSelectedUser(user);
    setNewPassword('');
    setResetModal(true);
  };

  const handleResetPassword = async () => {
    if (!newPassword) {
      toast.error('Password baru wajib diisi');
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.resetPassword(selectedUser.id, newPassword);
      toast.success('Password berhasil direset');
      setResetModal(false);
    } catch (err) {
      toast.error(err?.message || 'Gagal mereset password');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleActive = async () => {
    if (!toggleConfirm) return;
    setSubmitting(true);
    try {
      const newActive = !toggleConfirm.active;
      await adminApi.setUserActive(toggleConfirm.id, newActive);
      toast.success(newActive ? 'Pengguna diaktifkan' : 'Pengguna dinonaktifkan');
      setToggleConfirm(null);
      fetchUsers();
    } catch (err) {
      toast.error(err?.message || 'Gagal mengubah status pengguna');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      {/* Top bar */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-neutral-900">Pengguna</h1>
        <button
          onClick={() => setCreateModal(true)}
          className="btn-primary inline-flex items-center gap-2"
        >
          <Plus className="h-4 w-4" />
          Tambah Pengguna
        </button>
      </div>

      {/* Users list */}
      {users.length === 0 ? (
        <EmptyState
          icon={Users}
          title="Belum ada pengguna"
          description="Tambahkan pengguna baru untuk memulai."
        />
      ) : (
        <div className="rounded-2xl bg-white shadow-card ring-1 ring-neutral-100 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-100 bg-neutral-50/50">
                  <th className="px-6 py-3 font-semibold text-neutral-500">Nama</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Username</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Role</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Telepon</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Status</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Aksi</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u, idx) => (
                  <tr
                    key={u.id}
                    className={`border-b border-neutral-50 transition-colors hover:bg-primary-50/30 ${
                      idx % 2 === 0 ? 'bg-white' : 'bg-neutral-50/30'
                    }`}
                  >
                    <td className="px-6 py-3.5 font-medium text-neutral-800">
                      {u.fullName || '-'}
                    </td>
                    <td className="px-6 py-3.5 text-neutral-600">{u.username}</td>
                    <td className="px-6 py-3.5">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          u.role === 'ADMIN'
                            ? 'bg-purple-50 text-purple-700 ring-1 ring-purple-200'
                            : 'bg-blue-50 text-blue-700 ring-1 ring-blue-200'
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-neutral-500">{u.phoneE164 || '-'}</td>
                    <td className="px-6 py-3.5">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          u.active !== false
                            ? 'bg-green-50 text-green-700'
                            : 'bg-neutral-100 text-neutral-500'
                        }`}
                      >
                        <span
                          className={`h-1.5 w-1.5 rounded-full ${
                            u.active !== false ? 'bg-green-500' : 'bg-neutral-400'
                          }`}
                        />
                        {u.active !== false ? 'Aktif' : 'Nonaktif'}
                      </span>
                    </td>
                    <td className="px-6 py-3.5">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => openEdit(u)}
                          className="rounded-lg p-2 text-neutral-400 transition-colors hover:bg-neutral-100 hover:text-neutral-600"
                          title="Edit"
                        >
                          <Edit3 className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => openResetPassword(u)}
                          className="rounded-lg p-2 text-neutral-400 transition-colors hover:bg-neutral-100 hover:text-neutral-600"
                          title="Reset Password"
                        >
                          <KeyRound className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setToggleConfirm(u)}
                          className="rounded-lg p-2 text-neutral-400 transition-colors hover:bg-neutral-100 hover:text-neutral-600"
                          title={u.active !== false ? 'Nonaktifkan' : 'Aktifkan'}
                        >
                          {u.active !== false ? (
                            <ToggleRight className="h-4 w-4 text-green-500" />
                          ) : (
                            <ToggleLeft className="h-4 w-4" />
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create User Modal */}
      <Modal isOpen={createModal} onClose={() => setCreateModal(false)} title="Tambah Pengguna">
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Username</label>
            <input
              type="text"
              value={createForm.username}
              onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))}
              className="input-field"
              placeholder="Masukkan username"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Nama Lengkap</label>
            <input
              type="text"
              value={createForm.fullName}
              onChange={(e) => setCreateForm((f) => ({ ...f, fullName: e.target.value }))}
              className="input-field"
              placeholder="Masukkan nama lengkap"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Password</label>
            <input
              type="password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
              className="input-field"
              placeholder="Masukkan password"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Role</label>
            <select
              value={createForm.role}
              onChange={(e) => setCreateForm((f) => ({ ...f, role: e.target.value }))}
              className="input-field"
            >
              <option value="TECHNICIAN">Teknisi</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Telepon</label>
            <input
              type="tel"
              value={createForm.phoneE164}
              onChange={(e) => setCreateForm((f) => ({ ...f, phoneE164: e.target.value }))}
              className="input-field"
              placeholder="+628123456789"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setCreateModal(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleCreate} disabled={submitting} className="btn-primary">
              {submitting ? 'Menyimpan...' : 'Simpan'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Edit User Modal */}
      <Modal isOpen={editModal} onClose={() => setEditModal(false)} title="Edit Pengguna">
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Nama Lengkap</label>
            <input
              type="text"
              value={editForm.fullName}
              onChange={(e) => setEditForm((f) => ({ ...f, fullName: e.target.value }))}
              className="input-field"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Role</label>
            <select
              value={editForm.role}
              onChange={(e) => setEditForm((f) => ({ ...f, role: e.target.value }))}
              className="input-field"
            >
              <option value="TECHNICIAN">Teknisi</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Telepon</label>
            <input
              type="tel"
              value={editForm.phoneE164}
              onChange={(e) => setEditForm((f) => ({ ...f, phoneE164: e.target.value }))}
              className="input-field"
              placeholder="+628123456789"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setEditModal(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleEdit} disabled={submitting} className="btn-primary">
              {submitting ? 'Menyimpan...' : 'Simpan'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Reset Password Modal */}
      <Modal
        isOpen={resetModal}
        onClose={() => setResetModal(false)}
        title="Reset Password"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-500">
            Reset password untuk <strong>{selectedUser?.username}</strong>
          </p>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">
              Password Baru
            </label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="input-field"
              placeholder="Masukkan password baru"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setResetModal(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleResetPassword} disabled={submitting} className="btn-primary">
              {submitting ? 'Mereset...' : 'Reset Password'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Toggle Active Confirmation */}
      <Modal
        isOpen={!!toggleConfirm}
        onClose={() => setToggleConfirm(null)}
        title={toggleConfirm?.active !== false ? 'Nonaktifkan Pengguna' : 'Aktifkan Pengguna'}
        size="sm"
      >
        <div>
          <p className="mb-4 text-sm text-neutral-600">
            {toggleConfirm?.active !== false
              ? `Apakah Anda yakin ingin menonaktifkan ${toggleConfirm?.username}?`
              : `Apakah Anda yakin ingin mengaktifkan kembali ${toggleConfirm?.username}?`}
          </p>
          <div className="flex justify-end gap-3">
            <button onClick={() => setToggleConfirm(null)} className="btn-ghost">
              Batal
            </button>
            <button
              onClick={handleToggleActive}
              disabled={submitting}
              className={toggleConfirm?.active !== false ? 'btn-danger' : 'btn-primary'}
            >
              {submitting
                ? 'Memproses...'
                : toggleConfirm?.active !== false
                ? 'Nonaktifkan'
                : 'Aktifkan'}
            </button>
          </div>
        </div>
      </Modal>
    </motion.div>
  );
}
