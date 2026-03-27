import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users,
  Plus,
  Search,
  Pencil,
  Trash2,
  X,
  Phone,
  MapPin,
  Cpu,
  Hash,
  FileText,
  ChevronDown,
} from 'lucide-react';
import { adminApi } from '../../api/client';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useToast } from '../../components/Toast';

const EMPTY_FORM = {
  name: '', phoneE164: '', address: '', machineType: '', machineNumber: '', notes: '',
};

export default function CustomerListPage() {
  const toast = useToast();
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Modal states
  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null); // null = create, object = edit
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Expanded row for mobile
  const [expandedId, setExpandedId] = useState(null);

  const fetchCustomers = useCallback(async () => {
    try {
      const data = await adminApi.getCustomers(search || undefined);
      setCustomers(Array.isArray(data) ? data : []);
    } catch {
      toast.error('Gagal memuat data customer.');
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    setLoading(true);
    const timer = setTimeout(() => fetchCustomers(), search ? 300 : 0);
    return () => clearTimeout(timer);
  }, [fetchCustomers, search]);

  const openCreate = () => {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalOpen(true);
  };

  const openEdit = (customer) => {
    setEditTarget(customer);
    setForm({
      name: customer.name || '',
      phoneE164: customer.phoneE164 || '',
      address: customer.address || '',
      machineType: customer.machineType || '',
      machineNumber: customer.machineNumber || '',
      notes: customer.notes || '',
    });
    setFormError('');
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setFormError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) { setFormError('Nama customer wajib diisi.'); return; }
    setSubmitting(true);
    setFormError('');
    try {
      if (editTarget) {
        await adminApi.updateCustomer(editTarget.id, form);
        toast.success('Customer berhasil diperbarui.');
      } else {
        await adminApi.createCustomer(form);
        toast.success('Customer berhasil ditambahkan.');
      }
      closeModal();
      await fetchCustomers();
    } catch (err) {
      setFormError(err.message || 'Terjadi kesalahan.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await adminApi.deleteCustomer(deleteTarget.id);
      toast.success(`Customer "${deleteTarget.name}" dihapus.`);
      setDeleteTarget(null);
      await fetchCustomers();
    } catch (err) {
      toast.error(err.message || 'Gagal menghapus customer.');
    } finally {
      setDeleting(false);
    }
  };

  const setField = (key, val) => setForm((prev) => ({ ...prev, [key]: val }));

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50">
            <Users className="h-5 w-5 text-primary-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-neutral-900">Database Customer</h1>
            <p className="text-sm text-neutral-500">
              Total{' '}
              <span className="font-semibold text-primary-600">{customers.length}</span>{' '}
              customer terdaftar
            </p>
          </div>
        </div>
        <button
          onClick={openCreate}
          className="btn-primary flex items-center gap-2 self-start sm:self-auto"
        >
          <Plus className="h-4 w-4" />
          Tambah Customer
        </button>
      </div>

      {/* Search */}
      <div className="mb-5 relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari nama customer..."
          className="input-field pl-9"
        />
        {search && (
          <button
            onClick={() => setSearch('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner />
      ) : customers.length === 0 ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex flex-col items-center justify-center rounded-2xl bg-white py-16 shadow-card ring-1 ring-neutral-100"
        >
          <div className="mb-4 rounded-full bg-neutral-100 p-4">
            <Users className="h-8 w-8 text-neutral-400" />
          </div>
          <p className="text-base font-semibold text-neutral-600">
            {search ? 'Tidak ada customer ditemukan' : 'Belum ada customer'}
          </p>
          <p className="mt-1 text-sm text-neutral-400">
            {search ? 'Coba kata kunci lain.' : 'Klik "+ Tambah Customer" untuk memulai.'}
          </p>
        </motion.div>
      ) : (
        <>
          {/* Desktop table */}
          <div className="hidden overflow-hidden rounded-2xl bg-white shadow-card ring-1 ring-neutral-100 lg:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-100 bg-neutral-50 text-left">
                  <th className="px-5 py-3.5 font-semibold text-neutral-600">Nama</th>
                  <th className="px-5 py-3.5 font-semibold text-neutral-600">Telepon</th>
                  <th className="px-5 py-3.5 font-semibold text-neutral-600">Tipe Mesin</th>
                  <th className="px-5 py-3.5 font-semibold text-neutral-600">No. Mesin</th>
                  <th className="px-5 py-3.5 font-semibold text-neutral-600">Alamat</th>
                  <th className="px-5 py-3.5 font-semibold text-neutral-600 text-right">Aksi</th>
                </tr>
              </thead>
              <tbody>
                <AnimatePresence mode="popLayout">
                  {customers.map((c) => (
                    <motion.tr
                      key={c.id}
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0 }}
                      className="border-b border-neutral-50 last:border-0 hover:bg-neutral-50/70 transition-colors"
                    >
                      <td className="px-5 py-4 font-medium text-neutral-800">{c.name}</td>
                      <td className="px-5 py-4 text-neutral-600">{c.phoneE164 || '-'}</td>
                      <td className="px-5 py-4 text-neutral-600">{c.machineType || '-'}</td>
                      <td className="px-5 py-4 text-neutral-600">{c.machineNumber || '-'}</td>
                      <td className="max-w-[200px] px-5 py-4 text-neutral-600 truncate">{c.address || '-'}</td>
                      <td className="px-5 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEdit(c)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-primary-50 hover:text-primary-600"
                            title="Edit"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(c)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-red-50 hover:text-red-600"
                            title="Hapus"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="space-y-3 lg:hidden">
            <AnimatePresence mode="popLayout">
              {customers.map((c) => (
                <motion.div
                  key={c.id}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="rounded-2xl bg-white p-4 shadow-card ring-1 ring-neutral-100"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-neutral-800 truncate">{c.name}</p>
                      {c.phoneE164 && (
                        <p className="mt-0.5 text-sm text-neutral-500">{c.phoneE164}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        onClick={() => setExpandedId(expandedId === c.id ? null : c.id)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100"
                      >
                        <ChevronDown className={`h-4 w-4 transition-transform ${expandedId === c.id ? 'rotate-180' : ''}`} />
                      </button>
                      <button
                        onClick={() => openEdit(c)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-primary-50 hover:text-primary-600"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => setDeleteTarget(c)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-red-50 hover:text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                  <AnimatePresence>
                    {expandedId === c.id && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="overflow-hidden"
                      >
                        <div className="mt-3 space-y-2 border-t border-neutral-100 pt-3 text-sm text-neutral-600">
                          {c.machineType && (
                            <div className="flex gap-2">
                              <Cpu className="h-4 w-4 shrink-0 text-neutral-400 mt-0.5" />
                              <span>{c.machineType}</span>
                            </div>
                          )}
                          {c.machineNumber && (
                            <div className="flex gap-2">
                              <Hash className="h-4 w-4 shrink-0 text-neutral-400 mt-0.5" />
                              <span>{c.machineNumber}</span>
                            </div>
                          )}
                          {c.address && (
                            <div className="flex gap-2">
                              <MapPin className="h-4 w-4 shrink-0 text-neutral-400 mt-0.5" />
                              <span>{c.address}</span>
                            </div>
                          )}
                          {c.notes && (
                            <div className="flex gap-2">
                              <FileText className="h-4 w-4 shrink-0 text-neutral-400 mt-0.5" />
                              <span>{c.notes}</span>
                            </div>
                          )}
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        </>
      )}

      {/* Create / Edit Modal */}
      <AnimatePresence>
        {modalOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeModal}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
          >
            <motion.div
              initial={{ scale: 0.92, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.92, opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-lg rounded-2xl bg-white shadow-2xl"
            >
              {/* Modal header */}
              <div className="flex items-center justify-between border-b border-neutral-100 px-6 py-4">
                <h2 className="text-lg font-semibold text-neutral-900">
                  {editTarget ? 'Edit Customer' : 'Tambah Customer Baru'}
                </h2>
                <button
                  onClick={closeModal}
                  className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* Modal body */}
              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                {formError && (
                  <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
                    {formError}
                  </div>
                )}

                <FormField
                  label="Nama Customer *"
                  icon={Users}
                  value={form.name}
                  onChange={(v) => setField('name', v)}
                  placeholder="Nama lengkap"
                />
                <FormField
                  label="Nomor Telepon"
                  icon={Phone}
                  value={form.phoneE164}
                  onChange={(v) => setField('phoneE164', v)}
                  placeholder="+628xxxxxxxxx"
                />
                <FormField
                  label="Alamat"
                  icon={MapPin}
                  value={form.address}
                  onChange={(v) => setField('address', v)}
                  placeholder="Alamat lengkap"
                  multiline
                />

                <div className="grid grid-cols-2 gap-3">
                  <FormField
                    label="Tipe Mesin"
                    icon={Cpu}
                    value={form.machineType}
                    onChange={(v) => setField('machineType', v)}
                    placeholder="Contoh: AC Split"
                  />
                  <FormField
                    label="No. Mesin"
                    icon={Hash}
                    value={form.machineNumber}
                    onChange={(v) => setField('machineNumber', v)}
                    placeholder="Nomor seri"
                  />
                </div>

                <FormField
                  label="Catatan"
                  icon={FileText}
                  value={form.notes}
                  onChange={(v) => setField('notes', v)}
                  placeholder="Catatan tambahan (opsional)"
                  multiline
                />

                <div className="flex gap-3 pt-2">
                  <button
                    type="button"
                    onClick={closeModal}
                    className="btn-ghost flex-1"
                  >
                    Batal
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="btn-primary flex-1"
                  >
                    {submitting ? 'Menyimpan...' : editTarget ? 'Simpan Perubahan' : 'Tambah Customer'}
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Delete Confirm Modal */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setDeleteTarget(null)}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
          >
            <motion.div
              initial={{ scale: 0.92, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.92, opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl"
            >
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100">
                <Trash2 className="h-6 w-6 text-red-500" />
              </div>
              <h3 className="text-lg font-semibold text-neutral-900">Hapus Customer?</h3>
              <p className="mt-2 text-sm text-neutral-500">
                Customer <span className="font-semibold text-neutral-700">"{deleteTarget.name}"</span> akan dihapus permanen. Tindakan ini tidak dapat dibatalkan.
              </p>
              <div className="mt-5 flex gap-3">
                <button
                  onClick={() => setDeleteTarget(null)}
                  className="btn-ghost flex-1"
                >
                  Batal
                </button>
                <button
                  onClick={handleDelete}
                  disabled={deleting}
                  className="flex-1 rounded-xl bg-red-500 px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-red-600 disabled:opacity-60"
                >
                  {deleting ? 'Menghapus...' : 'Ya, Hapus'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function FormField({ label, icon: Icon, value, onChange, placeholder, multiline }) {
  const Tag = multiline ? 'textarea' : 'input';
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-neutral-700">{label}</label>
      <div className="relative">
        {Icon && (
          <Icon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400 pointer-events-none" style={multiline ? { top: '14px', transform: 'none' } : {}} />
        )}
        <Tag
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={`input-field ${Icon ? 'pl-9' : ''} ${multiline ? 'min-h-[72px] resize-y' : ''}`}
        />
      </div>
    </div>
  );
}
