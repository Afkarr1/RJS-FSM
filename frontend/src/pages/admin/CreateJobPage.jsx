import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Save, Search, X, UserCheck, UserPlus, Wrench, MapPin } from 'lucide-react';
import { adminApi } from '../../api/client';
import { useToast } from '../../components/Toast';

export default function CreateJobPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [techs, setTechs] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [jobType, setJobType] = useState('FIELD_SERVICE');
  const [form, setForm] = useState({
    title: '', description: '', customerName: '', customerPhone: '',
    address: '', scheduledDate: '', assignToId: '', requiresPhoto: true,
    machineSerialNo: '', estimateText: '',
  });

  // Customer search state (FIELD_SERVICE only)
  const [customers, setCustomers] = useState([]);
  const [customerSearch, setCustomerSearch] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const searchRef = useRef(null);
  const [saveToContacts, setSaveToContacts] = useState(false);

  const todayWIB = new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Jakarta' });

  useEffect(() => {
    adminApi.getTechnicians().then(setTechs).catch(() => {});
    adminApi.getCustomers().then(setCustomers).catch(() => {});
  }, []);

  useEffect(() => {
    const handler = (e) => {
      if (searchRef.current && !searchRef.current.contains(e.target)) setShowDropdown(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Reset form fields when switching job type
  const handleJobTypeChange = (type) => {
    setJobType(type);
    setForm({
      title: '', description: '', customerName: '', customerPhone: '',
      address: '', scheduledDate: '', assignToId: '',
      requiresPhoto: type === 'FIELD_SERVICE',
      machineSerialNo: '', estimateText: '',
    });
    setSelectedCustomer(null);
    setCustomerSearch('');
    setSaveToContacts(false);
  };

  const filteredCustomers = customerSearch.trim()
    ? customers.filter((c) => c.name.toLowerCase().includes(customerSearch.toLowerCase()))
    : [];

  const selectCustomer = (c) => {
    setSelectedCustomer(c);
    setCustomerSearch(c.name);
    setShowDropdown(false);
    setSaveToContacts(false);
    setForm((prev) => ({ ...prev, customerName: c.name, customerPhone: c.phoneE164 || '', address: c.address || '' }));
  };

  const clearCustomer = () => {
    setSelectedCustomer(null);
    setCustomerSearch('');
    setSaveToContacts(false);
    setForm((prev) => ({ ...prev, customerName: '', customerPhone: '', address: '' }));
  };

  const set = (k, v) => {
    if (['customerName', 'customerPhone', 'address'].includes(k)) setSelectedCustomer(null);
    setForm({ ...form, [k]: v });
  };

  const showSaveToggle = jobType === 'FIELD_SERVICE' && !selectedCustomer && form.customerName.trim().length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setSubmitting(true); setError(null);
      const payload = { ...form, jobType };
      if (!payload.assignToId) delete payload.assignToId;
      if (!payload.scheduledDate) delete payload.scheduledDate;
      if (!payload.machineSerialNo) delete payload.machineSerialNo;
      if (!payload.estimateText) delete payload.estimateText;

      const job = await adminApi.createJob(payload);

      if (saveToContacts && form.customerName.trim()) {
        try {
          await adminApi.createCustomer({
            name: form.customerName.trim(),
            phoneE164: form.customerPhone.trim() || '',
            address: form.address.trim() || '',
            machineType: '', machineNumber: '', notes: '',
          });
          toast.success(`"${form.customerName.trim()}" ditambahkan ke daftar pelanggan`);
        } catch {
          toast.error('Pekerjaan dibuat, tapi gagal menyimpan ke daftar pelanggan');
        }
      }

      navigate(`/admin/jobs/${job.id}`);
    } catch (err) {
      setError(err.message || 'Gagal membuat pekerjaan');
    } finally { setSubmitting(false); }
  };

  const isBackOffice = jobType === 'BACK_OFFICE';

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
      <button onClick={() => navigate('/admin/jobs')} className="btn-ghost flex items-center gap-2 mb-4">
        <ArrowLeft className="w-4 h-4" /> Kembali
      </button>

      <div className="max-w-2xl">
        <h1 className="text-2xl font-bold text-neutral-900 mb-6">Buat Pekerjaan Baru</h1>

        {/* Job Type Toggle */}
        <div className="flex gap-2 mb-6 p-1 bg-neutral-100 rounded-xl">
          <button
            type="button"
            onClick={() => handleJobTypeChange('FIELD_SERVICE')}
            className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg text-sm font-medium transition-all ${
              !isBackOffice ? 'bg-white shadow text-primary-700' : 'text-neutral-500 hover:text-neutral-700'
            }`}
          >
            <MapPin className="w-4 h-4" /> Lapangan
          </button>
          <button
            type="button"
            onClick={() => handleJobTypeChange('BACK_OFFICE')}
            className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg text-sm font-medium transition-all ${
              isBackOffice ? 'bg-white shadow text-orange-600' : 'text-neutral-500 hover:text-neutral-700'
            }`}
          >
            <Wrench className="w-4 h-4" /> Workshop
          </button>
        </div>

        {error && <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-700 text-sm">{error}</div>}

        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-card border border-neutral-150 p-6 space-y-5">

          {/* FIELD_SERVICE: Customer Search */}
          {!isBackOffice && (
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Pilih Customer (opsional)</label>
              <div ref={searchRef} className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400 pointer-events-none" />
                <input
                  type="text"
                  value={customerSearch}
                  onChange={(e) => { setCustomerSearch(e.target.value); setSelectedCustomer(null); setSaveToContacts(false); setShowDropdown(true); }}
                  onFocus={() => { if (customerSearch.trim()) setShowDropdown(true); }}
                  placeholder="Ketik nama customer untuk mencari..."
                  className="input-field pl-9 pr-9"
                />
                {customerSearch && (
                  <button type="button" onClick={clearCustomer} className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600">
                    <X className="h-4 w-4" />
                  </button>
                )}
                <AnimatePresence>
                  {showDropdown && filteredCustomers.length > 0 && (
                    <motion.ul initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -4 }} transition={{ duration: 0.15 }}
                      className="absolute z-20 mt-1 w-full rounded-xl border border-neutral-150 bg-white shadow-lg overflow-hidden">
                      {filteredCustomers.slice(0, 8).map((c) => (
                        <li key={c.id}>
                          <button type="button" onMouseDown={() => selectCustomer(c)}
                            className="flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-primary-50 transition-colors">
                            <UserCheck className="h-4 w-4 mt-0.5 shrink-0 text-primary-500" />
                            <div>
                              <p className="text-sm font-medium text-neutral-800">{c.name}</p>
                              <p className="text-xs text-neutral-400">
                                {[c.phoneE164, c.machineType].filter(Boolean).join(' · ') || 'Tidak ada info tambahan'}
                              </p>
                            </div>
                          </button>
                        </li>
                      ))}
                    </motion.ul>
                  )}
                </AnimatePresence>
              </div>
              {selectedCustomer && (
                <p className="mt-1.5 text-xs text-primary-600 flex items-center gap-1">
                  <UserCheck className="h-3.5 w-3.5" /> Data customer terisi otomatis.
                </p>
              )}
            </div>
          )}

          {!isBackOffice && <div className="border-t border-neutral-100" />}

          {/* Keluhan / Judul */}
          <Field
            label={isBackOffice ? 'Keluhan / Judul Pekerjaan *' : 'Judul Pekerjaan *'}
            value={form.title}
            onChange={v => set('title', v)}
            placeholder={isBackOffice ? 'Contoh: Mesin tidak bisa menyala' : 'Contoh: Service AC'}
          />
          <Field label="Deskripsi" value={form.description} onChange={v => set('description', v)} placeholder="Detail keluhan..." multiline />

          {isBackOffice ? (
            /* BACK_OFFICE: machine fields */
            <>
              <div className="grid sm:grid-cols-2 gap-4">
                <Field label="Nama Mesin" value={form.customerName} onChange={v => set('customerName', v)} placeholder="Contoh: Laptop Asus" />
                <Field label="Tipe Mesin" value={form.address} onChange={v => set('address', v)} placeholder="Contoh: VivoBook 15" />
              </div>
              <div className="grid sm:grid-cols-2 gap-4">
                <Field label="No. Seri" value={form.machineSerialNo} onChange={v => set('machineSerialNo', v)} placeholder="S/N atau IMEI" />
                <Field label="Estimasi (opsional)" value={form.estimateText} onChange={v => set('estimateText', v)} placeholder="Contoh: Rp 300.000" />
              </div>
            </>
          ) : (
            /* FIELD_SERVICE: customer fields */
            <>
              <div className="grid sm:grid-cols-2 gap-4">
                <Field label="Nama Customer" value={form.customerName} onChange={v => set('customerName', v)} placeholder="Nama lengkap" />
                <Field label="Telepon Customer" value={form.customerPhone} onChange={v => set('customerPhone', v)} placeholder="+628xxxxxxxxx" />
              </div>
              <Field label="Alamat" value={form.address} onChange={v => set('address', v)} placeholder="Alamat lengkap" multiline />
            </>
          )}

          {/* Save to contacts toggle (FIELD_SERVICE only) */}
          <AnimatePresence>
            {showSaveToggle && (
              <motion.label key="save-toggle" initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: 0.18 }}
                className="flex items-center gap-3 cursor-pointer rounded-xl border border-blue-200 bg-blue-50 px-4 py-3">
                <input type="checkbox" checked={saveToContacts} onChange={e => setSaveToContacts(e.target.checked)}
                  className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-blue-800">Tambahkan ke Daftar Pelanggan</p>
                  <p className="text-xs text-blue-600 truncate">Simpan "<span className="font-semibold">{form.customerName}</span>" ke kontak pelanggan</p>
                </div>
                <UserPlus className="h-4 w-4 shrink-0 text-blue-500" />
              </motion.label>
            )}
          </AnimatePresence>

          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Tanggal Jadwal</label>
              <input type="date" value={form.scheduledDate} min={todayWIB} onChange={e => set('scheduledDate', e.target.value)} className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Tugaskan ke Teknisi</label>
              <select value={form.assignToId} onChange={e => set('assignToId', e.target.value)} className="input-field">
                <option value="">Belum ditugaskan</option>
                {techs.map(t => <option key={t.id} value={t.id}>{t.fullName}</option>)}
              </select>
            </div>
          </div>

          <label className="flex items-center gap-3 cursor-pointer">
            <input type="checkbox" checked={form.requiresPhoto} onChange={e => set('requiresPhoto', e.target.checked)}
              className="w-5 h-5 rounded border-neutral-300 text-primary-500 focus:ring-primary-500" />
            <span className="text-sm font-medium text-neutral-700">Wajib upload foto sebelum menyelesaikan</span>
          </label>

          <button type="submit" disabled={!form.title.trim() || submitting} className="btn-primary w-full flex items-center justify-center gap-2">
            <Save className="w-4 h-4" /> {submitting ? 'Menyimpan...' : 'Buat Pekerjaan'}
          </button>
        </form>
      </div>
    </motion.div>
  );
}

function Field({ label, value, onChange, placeholder, multiline }) {
  const Tag = multiline ? 'textarea' : 'input';
  return (
    <div>
      <label className="block text-sm font-medium text-neutral-700 mb-1">{label}</label>
      <Tag value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className={`input-field ${multiline ? 'min-h-[80px] resize-y' : ''}`} />
    </div>
  );
}
