import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Save } from 'lucide-react';
import { adminApi } from '../../api/client';

export default function CreateJobPage() {
  const navigate = useNavigate();
  const [techs, setTechs] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [form, setForm] = useState({
    title: '', description: '', customerName: '', customerPhone: '',
    address: '', scheduledDate: '', assignToId: '', requiresPhoto: true,
  });

  useEffect(() => { adminApi.getTechnicians().then(setTechs).catch(() => {}); }, []);

  const set = (k, v) => setForm({ ...form, [k]: v });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setSubmitting(true); setError(null);
      const payload = { ...form };
      if (!payload.assignToId) delete payload.assignToId;
      if (!payload.scheduledDate) delete payload.scheduledDate;
      const job = await adminApi.createJob(payload);
      navigate(`/admin/jobs/${job.id}`);
    } catch (err) {
      setError(err.message || 'Gagal membuat pekerjaan');
    } finally { setSubmitting(false); }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
      <button onClick={() => navigate('/admin/jobs')} className="btn-ghost flex items-center gap-2 mb-4">
        <ArrowLeft className="w-4 h-4" /> Kembali
      </button>

      <div className="max-w-2xl">
        <h1 className="text-2xl font-bold text-neutral-900 mb-6">Buat Pekerjaan Baru</h1>

        {error && <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-700 text-sm">{error}</div>}

        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-card border border-neutral-150 p-6 space-y-5">
          <Field label="Judul Pekerjaan *" value={form.title} onChange={v => set('title', v)} placeholder="Contoh: Service AC" />
          <Field label="Deskripsi" value={form.description} onChange={v => set('description', v)} placeholder="Detail pekerjaan..." multiline />
          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="Nama Customer" value={form.customerName} onChange={v => set('customerName', v)} placeholder="Nama lengkap" />
            <Field label="Telepon Customer" value={form.customerPhone} onChange={v => set('customerPhone', v)} placeholder="+628xxxxxxxxx" />
          </div>
          <Field label="Alamat" value={form.address} onChange={v => set('address', v)} placeholder="Alamat lengkap" multiline />
          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Tanggal Jadwal</label>
              <input type="date" value={form.scheduledDate} onChange={e => set('scheduledDate', e.target.value)} className="input-field" />
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
