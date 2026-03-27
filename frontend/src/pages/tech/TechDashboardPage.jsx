import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ClipboardList,
  Wrench,
  CheckCircle2,
  MapPin,
  Calendar,
  User,
  ChevronRight,
} from 'lucide-react';
import { techApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import StatusBadge from '../../components/StatusBadge';
import StatsCard from '../../components/StatsCard';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.08, delayChildren: 0.1 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
};

export default function TechDashboardPage() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    async function fetchJobs() {
      try {
        const data = await techApi.getJobs(false);
        setJobs(Array.isArray(data) ? data : []);
      } catch (err) {
        setError('Gagal memuat data tugas.');
      } finally {
        setLoading(false);
      }
    }
    fetchJobs();
  }, []);

  if (loading) return <LoadingSpinner />;

  const totalJobs = jobs.length;
  const inProgressJobs = jobs.filter(
    (j) => j.status === 'IN_PROGRESS' || j.status === 'ASSIGNED',
  ).length;
  const doneJobs = jobs.filter((j) => j.status === 'DONE' || j.status === 'CLOSED').length;

  const activeJobs = jobs.filter(
    (j) => j.status === 'ASSIGNED' || j.status === 'IN_PROGRESS' || j.status === 'NEED_FOLLOWUP',
  );

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
        {/* Welcome */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8"
        >
          <h1 className="text-2xl font-bold text-neutral-800 sm:text-3xl">
            Selamat datang{user?.username ? `, ${user.username}` : ''}! 👋
          </h1>
          <p className="mt-1.5 text-neutral-500">
            Berikut ringkasan tugas Anda hari ini.
          </p>
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

        {/* Stats */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-3"
        >
          <motion.div variants={itemVariants} className="cursor-pointer" onClick={() => navigate('/tech/jobs')}>
            <StatsCard icon={ClipboardList} label="Total Tugas" value={totalJobs} />
          </motion.div>
          <motion.div variants={itemVariants} className="cursor-pointer" onClick={() => navigate('/tech/jobs?filter=active')}>
            <StatsCard icon={Wrench} label="Sedang Dikerjakan" value={inProgressJobs} />
          </motion.div>
          <motion.div variants={itemVariants} className="cursor-pointer" onClick={() => navigate('/tech/jobs?filter=done')}>
            <StatsCard icon={CheckCircle2} label="Selesai" value={doneJobs} />
          </motion.div>
        </motion.div>

        {/* Active Jobs */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.3 }}
        >
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-neutral-800">Tugas Aktif</h2>
            <button
              onClick={() => navigate('/tech/jobs')}
              className="btn-ghost text-sm flex items-center gap-1"
            >
              Lihat Semua <ChevronRight size={16} />
            </button>
          </div>

          {activeJobs.length === 0 ? (
            <EmptyState
              icon={CheckCircle2}
              title="Tidak ada tugas aktif"
              description="Semua tugas telah diselesaikan. Kerja bagus!"
            />
          ) : (
            <motion.div
              variants={containerVariants}
              initial="hidden"
              animate="visible"
              className="grid grid-cols-1 gap-4 sm:grid-cols-2"
            >
              {activeJobs.map((job) => (
                <motion.div
                  key={job.id}
                  variants={itemVariants}
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
                      <MapPin size={14} className="shrink-0 text-neutral-400" />
                      <span className="truncate">{job.address || '-'}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Calendar size={14} className="shrink-0 text-neutral-400" />
                      <span>{formatDate(job.scheduledDate)}</span>
                    </div>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          )}
        </motion.div>
      </div>
    </motion.div>
  );
}
