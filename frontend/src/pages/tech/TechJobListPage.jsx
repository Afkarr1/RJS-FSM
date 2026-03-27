import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Briefcase, Calendar, User } from 'lucide-react';
import { techApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.05 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: 'easeOut' } },
};

const FILTERS = [
  { key: 'all', label: 'Semua' },
  { key: 'active', label: 'Aktif' },
  { key: 'done', label: 'Selesai' },
];

const ACTIVE_STATUSES = ['ASSIGNED', 'IN_PROGRESS', 'NEED_FOLLOWUP'];
const DONE_STATUSES = ['DONE', 'CLOSED'];

export default function TechJobListPage() {
  const [allJobs, setAllJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const filter = searchParams.get('filter') || 'all';

  useEffect(() => {
    async function fetchJobs() {
      setLoading(true);
      setError('');
      try {
        const data = await techApi.getJobs(false);
        setAllJobs(Array.isArray(data) ? data : []);
      } catch (err) {
        setError('Gagal memuat daftar tugas.');
      } finally {
        setLoading(false);
      }
    }
    fetchJobs();
  }, []);

  const jobs =
    filter === 'active'
      ? allJobs.filter((j) => ACTIVE_STATUSES.includes(j.status))
      : filter === 'done'
        ? allJobs.filter((j) => DONE_STATUSES.includes(j.status))
        : allJobs;

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
      className="min-h-screen bg-neutral-50"
    >
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-6"
        >
          <h1 className="text-2xl font-bold text-neutral-800">Daftar Tugas</h1>
          <p className="mt-1 text-sm text-neutral-500">
            Kelola dan pantau seluruh tugas yang ditugaskan kepada Anda.
          </p>
        </motion.div>

        {/* Filter Tabs */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
          className="mb-6 flex items-center gap-2"
        >
          <div className="inline-flex rounded-xl bg-white p-1 shadow-sm ring-1 ring-neutral-150">
            {FILTERS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => {
                  if (tab.key === 'all') setSearchParams({});
                  else setSearchParams({ filter: tab.key });
                }}
                className={`rounded-lg px-4 py-2 text-sm font-medium transition-all duration-200 ${
                  filter === tab.key
                    ? 'bg-primary-500 text-white shadow-button'
                    : 'text-neutral-500 hover:text-neutral-700'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </motion.div>

        {/* Error */}
        {error && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-600"
          >
            {error}
          </motion.div>
        )}

        {/* Content */}
        {loading ? (
          <LoadingSpinner />
        ) : jobs.length === 0 ? (
          <EmptyState
            icon={Briefcase}
            title="Belum ada tugas"
            description={
              filter === 'active'
                ? 'Tidak ada tugas aktif saat ini.'
                : filter === 'done'
                  ? 'Belum ada tugas yang selesai.'
                  : 'Belum ada tugas yang ditugaskan kepada Anda.'
            }
          />
        ) : (
          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <AnimatePresence mode="popLayout">
              {jobs.map((job) => (
                <motion.div
                  key={job.id}
                  variants={itemVariants}
                  layout
                  whileHover={{ y: -2 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => navigate(`/tech/jobs/${job.id}`)}
                  className="interactive-card rounded-2xl p-5"
                >
                  <div className="mb-3 flex items-start justify-between gap-3">
                    <h3 className="text-base font-semibold text-neutral-800 line-clamp-2">
                      {job.title}
                    </h3>
                    <StatusBadge status={job.status} />
                  </div>

                  <div className="space-y-2 text-sm text-neutral-500">
                    <div className="flex items-center gap-2">
                      <User size={14} className="shrink-0 text-neutral-400" />
                      <span className="truncate">{job.customerName || '-'}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Calendar size={14} className="shrink-0 text-neutral-400" />
                      <span>{formatDate(job.scheduledDate)}</span>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}
