import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Wrench, Lock, RefreshCw, Clock, User, CheckCircle2, PauseCircle, Play } from 'lucide-react';
import { adminApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useToast } from '../../components/Toast';

const WORKSHOP_PIN = '1234';

const STATUS_LABEL = {
  OPEN: 'Dibuat', ASSIGNED: 'Ditugaskan', IN_PROGRESS: 'Dikerjakan',
  PENDING: 'Menunggu Sparepart', DONE: 'Selesai', CANCELLED: 'Dibatalkan',
};

export default function AdminWorkshopPage() {
  const toast = useToast();
  const [unlocked, setUnlocked] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [pinError, setPinError] = useState(false);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);

  const handlePinSubmit = () => {
    if (pinInput === WORKSHOP_PIN) {
      setUnlocked(true);
      setPinError(false);
    } else {
      setPinError(true);
      setPinInput('');
      setTimeout(() => setPinError(false), 2000);
    }
  };

  const fetchJobs = useCallback(async () => {
    setLoading(true);
    try {
      const all = await adminApi.getJobs();
      const workshop = (Array.isArray(all) ? all : []).filter(
        (j) => j.jobType === 'BACK_OFFICE' && j.status !== 'CLOSED' && j.status !== 'CANCELLED'
      );
      setJobs(workshop);
    } catch {
      toast.error('Gagal memuat data workshop');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    if (unlocked) fetchJobs();
  }, [unlocked, fetchJobs]);

  const formatDateTime = (s) => {
    if (!s) return '-';
    return new Date(s).toLocaleString('id-ID', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta',
    });
  };

  if (!unlocked) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex min-h-[60vh] items-center justify-center"
      >
        <div className="w-full max-w-sm">
          <div className="mb-6 flex flex-col items-center gap-3 text-center">
            <div className={`flex h-16 w-16 items-center justify-center rounded-2xl shadow-lg transition-colors ${
              pinError ? 'bg-red-100' : 'bg-orange-100'
            }`}>
              <Lock size={28} className={pinError ? 'text-red-500' : 'text-orange-600'} />
            </div>
            <h1 className="text-2xl font-bold text-neutral-900">Monitor Workshop</h1>
            <p className="text-sm text-neutral-500">Masukkan PIN untuk mengakses section teknisi internal</p>
          </div>

          <div className="rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100">
            <label className="mb-2 block text-sm font-medium text-neutral-700">PIN Akses</label>
            <input
              type="password"
              value={pinInput}
              onChange={(e) => setPinInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handlePinSubmit()}
              placeholder="Masukkan PIN"
              maxLength={10}
              className={`input-field mb-1 text-center text-lg tracking-[0.5em] ${
                pinError ? 'border-red-300 ring-red-200' : ''
              }`}
              autoFocus
            />
            {pinError && (
              <p className="mb-3 text-center text-xs font-medium text-red-500">PIN salah, coba lagi</p>
            )}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.97 }}
              onClick={handlePinSubmit}
              className="btn-primary mt-3 w-full"
            >
              Masuk
            </motion.button>
          </div>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-100">
            <Wrench size={20} className="text-orange-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-neutral-900">Monitor Workshop</h1>
            <p className="text-sm text-neutral-500">Pekerjaan aktif teknisi internal</p>
          </div>
        </div>
        <button
          onClick={fetchJobs}
          disabled={loading}
          className="btn-ghost flex items-center gap-2 text-sm"
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : jobs.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-neutral-400">
          <Wrench size={40} className="mb-3 opacity-30" />
          <p className="text-sm">Tidak ada pekerjaan workshop aktif</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {jobs.map((job, idx) => (
            <motion.div
              key={job.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.05 }}
              className="rounded-2xl bg-white p-5 shadow-card ring-1 ring-neutral-100"
            >
              <div className="mb-3 flex items-start justify-between gap-2">
                <h3 className="text-sm font-semibold text-neutral-800 leading-snug">{job.title}</h3>
                <StatusBadge status={job.status} />
              </div>

              {job.customerName && (
                <div className="mb-2 flex items-center gap-2 text-xs text-neutral-500">
                  <Wrench size={12} className="shrink-0 text-orange-400" />
                  <span>{job.customerName}</span>
                </div>
              )}

              {job.assignedToName && (
                <div className="mb-2 flex items-center gap-2 text-xs text-neutral-500">
                  <User size={12} className="shrink-0 text-primary-400" />
                  <span>{job.assignedToName}</span>
                </div>
              )}

              {job.machineSerialNo && (
                <div className="mb-2 flex items-center gap-2 text-xs text-neutral-500">
                  <span className="font-mono text-neutral-400">SN:</span>
                  <span className="font-mono">{job.machineSerialNo}</span>
                </div>
              )}

              {job.status === 'PENDING' && job.pendingAt && (
                <div className="mt-3 flex items-center gap-1.5 rounded-lg bg-amber-50 px-2.5 py-1.5 text-xs font-medium text-amber-700">
                  <PauseCircle size={12} />
                  <span>Pending sejak {formatDateTime(job.pendingAt)}</span>
                </div>
              )}

              <div className="mt-3 border-t border-neutral-50 pt-2.5 text-xs text-neutral-400">
                <Clock size={11} className="mr-1 inline" />
                {formatDateTime(job.scheduledDate || job.createdAt)}
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </motion.div>
  );
}
