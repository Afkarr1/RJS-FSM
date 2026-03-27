import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  Briefcase,
  MapPin,
  Calendar,
  User,
} from 'lucide-react';
import { adminApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

const STATUS_TABS = [
  { key: null, label: 'Semua' },
  { key: 'OPEN', label: 'Baru' },
  { key: 'ASSIGNED', label: 'Ditugaskan' },
  { key: 'IN_PROGRESS', label: 'Dikerjakan' },
  { key: 'DONE', label: 'Selesai' },
  { key: 'NEED_FOLLOWUP', label: 'Follow Up' },
  { key: 'CLOSED', label: 'Ditutup' },
];

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

const item = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' } },
};

export default function JobListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState(() => searchParams.get('status') || null);

  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      try {
        const data = await adminApi.getJobs(activeStatus);
        setJobs(data || []);
      } catch (err) {
        console.error('Gagal memuat pekerjaan:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchJobs();
  }, [activeStatus]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      timeZone: 'Asia/Jakarta',
    });
  };

  const truncate = (str, len = 60) => {
    if (!str) return '-';
    return str.length > len ? str.slice(0, len) + '...' : str;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      {/* Top bar */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-neutral-900">Pekerjaan</h1>
        <button
          onClick={() => navigate('/admin/jobs/new')}
          className="btn-primary inline-flex items-center gap-2"
        >
          <Plus className="h-4 w-4" />
          Buat Pekerjaan Baru
        </button>
      </div>

      {/* Status filter tabs */}
      <div className="mb-6 flex flex-wrap gap-2">
        {STATUS_TABS.map((tab) => {
          const isActive = activeStatus === tab.key;
          return (
            <button
              key={tab.key ?? 'all'}
              onClick={() => {
                setActiveStatus(tab.key);
                if (tab.key) setSearchParams({ status: tab.key });
                else setSearchParams({});
              }}
              className={`relative rounded-full px-4 py-2 text-sm font-semibold transition-all duration-200 active:scale-[0.96] ${
                isActive
                  ? 'bg-primary-500 text-white shadow-button'
                  : 'bg-white text-neutral-500 ring-1 ring-neutral-200 hover:bg-neutral-50 hover:text-neutral-700'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner />
      ) : jobs.length === 0 ? (
        <EmptyState
          icon={Briefcase}
          title="Tidak ada pekerjaan"
          description={
            activeStatus
              ? 'Tidak ada pekerjaan dengan status ini.'
              : 'Belum ada pekerjaan. Buat pekerjaan baru untuk memulai.'
          }
        />
      ) : (
        <motion.div
          className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3"
          variants={container}
          initial="hidden"
          animate="show"
        >
          <AnimatePresence mode="popLayout">
            {jobs.map((job) => (
              <motion.div
                key={job.id}
                variants={item}
                layout
                onClick={() => navigate(`/admin/jobs/${job.id}`)}
                className="interactive-card rounded-2xl p-5"
              >
                <div className="mb-3 flex items-start justify-between gap-3">
                  <h3 className="text-base font-semibold text-neutral-900 leading-snug">
                    {job.title}
                  </h3>
                  <StatusBadge status={job.status} />
                </div>

                <p className="mb-4 text-sm text-neutral-500">
                  {truncate(job.customerName)}
                </p>

                {job.address && (
                  <div className="mb-3 flex items-start gap-2 text-xs text-neutral-400">
                    <MapPin className="mt-0.5 h-3.5 w-3.5 flex-shrink-0" />
                    <span>{truncate(job.address, 80)}</span>
                  </div>
                )}

                <div className="flex items-center gap-4 border-t border-neutral-100 pt-3 text-xs text-neutral-400">
                  {job.assignedToName && (
                    <div className="flex items-center gap-1.5">
                      <User className="h-3.5 w-3.5" />
                      <span>{job.assignedToName}</span>
                    </div>
                  )}
                  <div className="flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>{formatDate(job.scheduledDate || job.createdAt)}</span>
                  </div>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </motion.div>
      )}
    </motion.div>
  );
}
