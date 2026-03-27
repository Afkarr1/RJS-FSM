import { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen,
  Lock,
  Search,
  X,
  ChevronDown,
  User,
  Calendar,
  Wrench,
  FileText,
  Package,
  Clock,
} from 'lucide-react';
import { adminApi } from '../../api/client';
import LoadingSpinner from '../../components/LoadingSpinner';

const HISTORY_PASSWORD = '$$$$$';

function formatDate(val) {
  if (!val) return '-';
  return new Date(val).toLocaleDateString('id-ID', {
    day: '2-digit', month: 'long', year: 'numeric',
    timeZone: 'Asia/Jakarta',
  });
}

function formatDateTime(val) {
  if (!val) return '-';
  return new Date(val).toLocaleString('id-ID', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
    timeZone: 'Asia/Jakarta',
  });
}

// ─── Password Gate ────────────────────────────────────────────────────────────
function PasswordGate({ onUnlock }) {
  const [input, setInput] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (input === HISTORY_PASSWORD) {
      onUnlock();
    } else {
      setError('Password salah. Coba lagi.');
      setInput('');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex min-h-[60vh] items-center justify-center"
    >
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-50">
            <Lock className="h-8 w-8 text-primary-600" />
          </div>
          <h2 className="text-xl font-bold text-neutral-900">Akses Terbatas</h2>
          <p className="mt-1 text-sm text-neutral-500">
            Masukkan password untuk mengakses Riwayat Pekerjaan
          </p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100 space-y-4">
          {error && (
            <div className="rounded-xl bg-red-50 px-4 py-2.5 text-sm text-red-600">
              {error}
            </div>
          )}
          <input
            type="password"
            value={input}
            onChange={e => { setInput(e.target.value); setError(''); }}
            placeholder="Masukkan password"
            className="input-field"
            autoFocus
          />
          <button type="submit" disabled={!input} className="btn-primary w-full">
            Masuk
          </button>
        </form>
      </div>
    </motion.div>
  );
}

// ─── Job Card (per pekerjaan dalam expand) ────────────────────────────────────
function JobCard({ job, index }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -8 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05 }}
      className="relative pl-4 before:absolute before:left-0 before:top-0 before:h-full before:w-0.5 before:rounded-full before:bg-primary-200"
    >
      <div className="rounded-xl bg-neutral-50 p-4 ring-1 ring-neutral-100 space-y-3">
        {/* Title + date */}
        <div className="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="font-semibold text-neutral-800">{job.title}</p>
            {job.description && (
              <p className="mt-0.5 text-sm text-neutral-500 line-clamp-2">{job.description}</p>
            )}
          </div>
          <span className="shrink-0 rounded-lg bg-green-50 px-2.5 py-1 text-xs font-semibold text-green-700">
            Selesai
          </span>
        </div>

        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 text-sm">
          <InfoRow icon={Calendar} label="Tgl Dikerjakan" value={formatDate(job.scheduledDate || job.startedAt)} />
          <InfoRow icon={Clock} label="Tgl Ditutup" value={formatDateTime(job.closedAt)} />
          <InfoRow icon={User} label="Teknisi" value={job.assignedToName || '-'} />
          <InfoRow icon={Package} label="Spare Parts" value={job.spareParts || '-'} />
        </div>

        {job.closingNote && (
          <div className="flex gap-2 text-sm">
            <FileText className="mt-0.5 h-4 w-4 shrink-0 text-neutral-400" />
            <p className="text-neutral-600 italic">"{job.closingNote}"</p>
          </div>
        )}
      </div>
    </motion.div>
  );
}

function InfoRow({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-2">
      <Icon className="mt-0.5 h-3.5 w-3.5 shrink-0 text-neutral-400" />
      <div className="min-w-0">
        <p className="text-[11px] font-medium uppercase tracking-wide text-neutral-400">{label}</p>
        <p className="text-neutral-700 truncate">{value}</p>
      </div>
    </div>
  );
}

// ─── Customer Card ─────────────────────────────────────────────────────────────
function CustomerCard({ customerName, jobs }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      className="overflow-hidden rounded-2xl bg-white shadow-card ring-1 ring-neutral-100"
    >
      <button
        onClick={() => setExpanded(v => !v)}
        className="flex w-full items-center justify-between px-5 py-4 text-left hover:bg-neutral-50/70 transition-colors"
      >
        <div className="flex items-center gap-3 min-w-0">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-100 text-sm font-bold text-primary-600">
            {customerName?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-neutral-800 truncate">{customerName || '(Tanpa Nama)'}</p>
            <p className="text-xs text-neutral-400">{jobs.length} pekerjaan selesai</p>
          </div>
        </div>
        <ChevronDown
          className={`h-5 w-5 shrink-0 text-neutral-400 transition-transform duration-200 ${expanded ? 'rotate-180' : ''}`}
        />
      </button>

      <AnimatePresence initial={false}>
        {expanded && (
          <motion.div
            key="expand"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.22 }}
            className="overflow-hidden"
          >
            <div className="border-t border-neutral-100 px-5 py-4 space-y-3">
              {jobs.map((job, i) => (
                <JobCard key={job.id} job={job} index={i} />
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────
export default function CustomerHistoryPage() {
  const [unlocked, setUnlocked] = useState(false);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  useEffect(() => {
    if (!unlocked) return;
    setLoading(true);
    adminApi.getJobs('CLOSED')
      .then(data => setJobs(Array.isArray(data) ? data : []))
      .catch(() => setJobs([]))
      .finally(() => setLoading(false));
  }, [unlocked]);

  // Group by customerName (case-insensitive, trimmed)
  const grouped = useMemo(() => {
    const map = new Map();
    jobs.forEach(job => {
      const key = (job.customerName || '').trim().toLowerCase();
      const displayName = (job.customerName || '').trim() || '(Tanpa Nama)';
      if (!map.has(key)) map.set(key, { displayName, jobs: [] });
      map.get(key).jobs.push(job);
    });
    // Sort each customer's jobs by closedAt desc
    map.forEach(v => v.jobs.sort((a, b) => new Date(b.closedAt) - new Date(a.closedAt)));
    // Convert to array sorted by customer name
    return Array.from(map.values()).sort((a, b) =>
      a.displayName.localeCompare(b.displayName, 'id')
    );
  }, [jobs]);

  const filtered = useMemo(() => {
    if (!search.trim()) return grouped;
    const q = search.toLowerCase();
    return grouped.filter(c => c.displayName.toLowerCase().includes(q));
  }, [grouped, search]);

  if (!unlocked) return <PasswordGate onUnlock={() => setUnlocked(true)} />;

  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50">
            <BookOpen className="h-5 w-5 text-primary-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-neutral-900">Riwayat Pekerjaan</h1>
            {!loading && (
              <p className="text-sm text-neutral-500">
                <span className="font-semibold text-primary-600">{grouped.length}</span> customer ·{' '}
                <span className="font-semibold text-primary-600">{jobs.length}</span> pekerjaan selesai
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Search */}
      <div className="mb-5 relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
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
      ) : filtered.length === 0 ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex flex-col items-center justify-center rounded-2xl bg-white py-16 shadow-card ring-1 ring-neutral-100"
        >
          <div className="mb-4 rounded-full bg-neutral-100 p-4">
            <BookOpen className="h-8 w-8 text-neutral-400" />
          </div>
          <p className="text-base font-semibold text-neutral-600">
            {search ? 'Tidak ada customer ditemukan' : 'Belum ada riwayat pekerjaan'}
          </p>
          <p className="mt-1 text-sm text-neutral-400">
            {search ? 'Coba kata kunci lain.' : 'Riwayat akan muncul setelah pekerjaan ditutup.'}
          </p>
        </motion.div>
      ) : (
        <div className="space-y-3">
          <AnimatePresence mode="popLayout">
            {filtered.map(c => (
              <CustomerCard
                key={c.displayName}
                customerName={c.displayName}
                jobs={c.jobs}
              />
            ))}
          </AnimatePresence>
        </div>
      )}
    </motion.div>
  );
}
