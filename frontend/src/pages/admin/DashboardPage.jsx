import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Briefcase,
  Wrench,
  CheckCircle,
  AlertTriangle,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { adminApi } from '../../api/client';
import StatsCard from '../../components/StatsCard';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35, ease: 'easeOut' } },
};

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await adminApi.getJobs();
        setJobs(data || []);
      } catch (err) {
        setError('Gagal memuat data dashboard');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <LoadingSpinner />;

  const totalJobs = jobs.length;
  const inProgress = jobs.filter((j) => j.status === 'IN_PROGRESS').length;
  const done = jobs.filter((j) => j.status === 'DONE').length;
  const followUp = jobs.filter((j) => j.status === 'NEED_FOLLOWUP').length;

  const recentJobs = [...jobs]
    .sort((a, b) => new Date(b.createdAt || b.scheduledDate || 0) - new Date(a.createdAt || a.scheduledDate || 0))
    .slice(0, 10);

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      timeZone: 'Asia/Jakarta',
    });
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      {/* Welcome */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-neutral-900">
          Selamat datang, {user?.username}
        </h1>
        <p className="mt-1 text-sm text-neutral-500">
          Berikut ringkasan pekerjaan hari ini.
        </p>
      </div>

      {error && (
        <div className="mb-6 rounded-xl bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">
          {error}
        </div>
      )}

      {/* Stats */}
      <motion.div
        className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
        variants={container}
        initial="hidden"
        animate="show"
      >
        <motion.div variants={item} className="cursor-pointer" onClick={() => navigate('/admin/jobs')}>
          <StatsCard icon={Briefcase} label="Total Pekerjaan" value={totalJobs} />
        </motion.div>
        <motion.div variants={item} className="cursor-pointer" onClick={() => navigate('/admin/jobs?status=IN_PROGRESS')}>
          <StatsCard icon={Wrench} label="Sedang Dikerjakan" value={inProgress} />
        </motion.div>
        <motion.div variants={item} className="cursor-pointer" onClick={() => navigate('/admin/jobs?status=DONE')}>
          <StatsCard icon={CheckCircle} label="Selesai" value={done} />
        </motion.div>
        <motion.div variants={item} className="cursor-pointer" onClick={() => navigate('/admin/jobs?status=NEED_FOLLOWUP')}>
          <StatsCard icon={AlertTriangle} label="Butuh Follow Up" value={followUp} />
        </motion.div>
      </motion.div>

      {/* Recent Jobs Table */}
      <motion.div
        className="rounded-2xl bg-white shadow-card ring-1 ring-neutral-100"
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3, duration: 0.4 }}
      >
        <div className="border-b border-neutral-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-neutral-900">Pekerjaan Terbaru</h2>
        </div>

        {recentJobs.length === 0 ? (
          <EmptyState
            icon={Briefcase}
            title="Belum ada pekerjaan"
            description="Buat pekerjaan baru untuk memulai."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-100 bg-neutral-50/50">
                  <th className="px-6 py-3 font-semibold text-neutral-500">Judul</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Customer</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Status</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Teknisi</th>
                  <th className="px-6 py-3 font-semibold text-neutral-500">Tanggal</th>
                </tr>
              </thead>
              <tbody>
                {recentJobs.map((job, idx) => (
                  <tr
                    key={job.id}
                    onClick={() => navigate(`/admin/jobs/${job.id}`)}
                    className={`cursor-pointer transition-colors hover:bg-primary-50/50 ${
                      idx % 2 === 0 ? 'bg-white' : 'bg-neutral-50/30'
                    }`}
                  >
                    <td className="px-6 py-3.5 font-medium text-neutral-800">{job.title}</td>
                    <td className="px-6 py-3.5 text-neutral-600">{job.customerName || '-'}</td>
                    <td className="px-6 py-3.5">
                      <StatusBadge status={job.status} />
                    </td>
                    <td className="px-6 py-3.5 text-neutral-600">{job.assignedToName || '-'}</td>
                    <td className="px-6 py-3.5 text-neutral-500">
                      {formatDate(job.scheduledDate || job.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </motion.div>
    </motion.div>
  );
}
