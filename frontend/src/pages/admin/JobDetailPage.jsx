import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  UserPlus,
  RefreshCw,
  CheckCircle,
  XCircle,
  MapPin,
  Calendar,
  User,
  Clock,
  Image as ImageIcon,
  X as XIcon,
} from 'lucide-react';
import { adminApi, getAuthHeader } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import Modal from '../../components/Modal';
import { useToast } from '../../components/Toast';

export default function JobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const todayWIB = new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Jakarta' });

  const [job, setJob] = useState(null);
  const [photos, setPhotos] = useState([]);
  const [history, setHistory] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [loading, setLoading] = useState(true);
  const [photoBlobUrls, setPhotoBlobUrls] = useState({});
  const [lightboxUrl, setLightboxUrl] = useState(null);
  const blobUrlsRef = useRef({});

  // Modal states
  const [assignModal, setAssignModal] = useState(false);
  const [rescheduleModal, setRescheduleModal] = useState(false);
  const [closeConfirm, setCloseConfirm] = useState(false);
  const [cancelConfirm, setCancelConfirm] = useState(false);

  // Form states
  const [assignForm, setAssignForm] = useState({ technicianId: '', scheduledDate: '' });
  const [rescheduleForm, setRescheduleForm] = useState({ scheduledDate: '', technicianId: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [jobData, photosData, historyData, techsData] = await Promise.all([
          adminApi.getJob(id),
          adminApi.getJobPhotos(id).catch(() => []),
          adminApi.getJobHistory(id).catch(() => []),
          adminApi.getTechnicians().catch(() => []),
        ]);
        setJob(jobData);
        setPhotos(photosData || []);
        setHistory(historyData || []);
        setTechnicians(techsData || []);
      } catch (err) {
        toast.error('Gagal memuat detail pekerjaan');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, [id]);

  useEffect(() => {
    if (photos.length === 0) return;
    const authHeader = getAuthHeader();
    const urlMap = {};
    const promises = photos.map(async (photo) => {
      const rawUrl = photo.downloadUrl
        ? (() => { try { return new URL(photo.downloadUrl).pathname; } catch { return photo.downloadUrl; } })()
        : (photo.url || '');
      if (!rawUrl) return;
      try {
        const res = await fetch(rawUrl, { headers: authHeader });
        if (res.ok) {
          const blob = await res.blob();
          urlMap[photo.id] = URL.createObjectURL(blob);
        }
      } catch {
        // ignore individual photo errors
      }
    });
    Promise.all(promises).then(() => {
      blobUrlsRef.current = urlMap;
      setPhotoBlobUrls({ ...urlMap });
    });

    return () => {
      Object.values(blobUrlsRef.current).forEach((u) => URL.revokeObjectURL(u));
      blobUrlsRef.current = {};
    };
  }, [photos]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('id-ID', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const statusLabel = (s) => ({
    OPEN: 'Dibuat',
    ASSIGNED: 'Ditugaskan',
    IN_TRANSIT: 'Dalam Perjalanan',
    IN_PROGRESS: 'Sedang Dikerjakan',
    DONE: 'Selesai',
    NEED_FOLLOWUP: 'Butuh Follow Up',
    CLOSED: 'Ditutup',
    CANCELLED: 'Dibatalkan',
  }[s] || s);

  const handleAssign = async () => {
    if (!assignForm.technicianId) {
      toast.error('Pilih teknisi terlebih dahulu');
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.assignJob(id, {
        technicianId: assignForm.technicianId,
        scheduledDate: assignForm.scheduledDate || undefined,
      });
      toast.success('Teknisi berhasil ditugaskan');
      setAssignModal(false);
      setAssignForm({ technicianId: '', scheduledDate: '' });
      // Refresh
      const updated = await adminApi.getJob(id);
      setJob(updated);
      const updatedHistory = await adminApi.getJobHistory(id).catch(() => []);
      setHistory(updatedHistory || []);
    } catch (err) {
      toast.error(err?.message || 'Gagal menugaskan teknisi');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReschedule = async () => {
    if (!rescheduleForm.scheduledDate) {
      toast.error('Pilih tanggal jadwal ulang');
      return;
    }
    setSubmitting(true);
    try {
      await adminApi.rescheduleJob(id, {
        scheduledDate: rescheduleForm.scheduledDate,
        technicianId: rescheduleForm.technicianId || undefined,
      });
      toast.success('Pekerjaan berhasil dijadwalkan ulang');
      setRescheduleModal(false);
      setRescheduleForm({ scheduledDate: '', technicianId: '' });
      const updated = await adminApi.getJob(id);
      setJob(updated);
      const updatedHistory = await adminApi.getJobHistory(id).catch(() => []);
      setHistory(updatedHistory || []);
    } catch (err) {
      toast.error(err?.message || 'Gagal menjadwalkan ulang');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async () => {
    setSubmitting(true);
    try {
      await adminApi.cancelJob(id);
      toast.success('Pekerjaan berhasil dibatalkan');
      setCancelConfirm(false);
      const updated = await adminApi.getJob(id);
      setJob(updated);
      const updatedHistory = await adminApi.getJobHistory(id).catch(() => []);
      setHistory(updatedHistory || []);
    } catch (err) {
      toast.error(err?.message || 'Gagal membatalkan pekerjaan');
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = async () => {
    setSubmitting(true);
    try {
      await adminApi.closeJob(id);
      toast.success('Pekerjaan berhasil ditutup');
      setCloseConfirm(false);
      const updated = await adminApi.getJob(id);
      setJob(updated);
      const updatedHistory = await adminApi.getJobHistory(id).catch(() => []);
      setHistory(updatedHistory || []);
    } catch (err) {
      toast.error(err?.message || 'Gagal menutup pekerjaan');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (!job) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      {/* Back */}
      <button
        onClick={() => navigate('/admin/jobs')}
        className="btn-ghost mb-6 inline-flex items-center gap-2 -ml-2"
      >
        <ArrowLeft className="h-4 w-4" />
        Kembali ke Daftar Pekerjaan
      </button>

      {/* Job info card */}
      <div className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100">
        <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-xl font-bold text-neutral-900">{job.title}</h1>
            <div className="mt-2">
              <StatusBadge status={job.status} />
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap gap-2">
            {job.status === 'OPEN' && (
              <button
                onClick={() => setAssignModal(true)}
                className="btn-primary inline-flex items-center gap-2 text-sm"
              >
                <UserPlus className="h-4 w-4" />
                Tugaskan Teknisi
              </button>
            )}
            {job.status === 'NEED_FOLLOWUP' && (
              <button
                onClick={() => setRescheduleModal(true)}
                className="btn-primary inline-flex items-center gap-2 text-sm"
              >
                <RefreshCw className="h-4 w-4" />
                Jadwalkan Ulang
              </button>
            )}
            {job.status === 'DONE' && (
              <button
                onClick={() => setCloseConfirm(true)}
                className="btn-primary inline-flex items-center gap-2 text-sm"
              >
                <CheckCircle className="h-4 w-4" />
                Tutup Pekerjaan
              </button>
            )}
            {['OPEN', 'ASSIGNED', 'NEED_FOLLOWUP'].includes(job.status) && (
              <button
                onClick={() => setCancelConfirm(true)}
                className="btn-danger inline-flex items-center gap-2 text-sm"
              >
                <XCircle className="h-4 w-4" />
                Batalkan
              </button>
            )}
          </div>
        </div>

        {job.description && (
          <p className="mb-5 text-sm text-neutral-600 leading-relaxed">{job.description}</p>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <InfoItem icon={User} label="Customer" value={job.customerName || '-'} />
          <InfoItem icon={MapPin} label="Alamat" value={job.address || '-'} />
          <InfoItem icon={User} label="Teknisi" value={job.assignedToName || 'Belum ditugaskan'} />
          <InfoItem icon={Calendar} label="Tanggal Dijadwalkan" value={formatDate(job.scheduledDate)} />
          <InfoItem icon={Clock} label="Dibuat" value={formatDateTime(job.createdAt)} />
          {job.customerPhone && (
            <InfoItem icon={User} label="Telepon" value={job.customerPhone} />
          )}
        </div>

        {/* Timestamp milestones */}
        {(job.assignedAt || job.inTransitAt || job.startedAt || job.finishedAt || job.closedAt) && (
          <div className="mt-5 border-t border-neutral-100 pt-5">
            <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-neutral-400">Rekam Waktu</p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {job.assignedAt && <InfoItem icon={Clock} label="Ditugaskan" value={formatDateTime(job.assignedAt)} />}
              {job.inTransitAt && <InfoItem icon={Clock} label="Dalam Perjalanan" value={formatDateTime(job.inTransitAt)} />}
              {job.startedAt && <InfoItem icon={Clock} label="Mulai Dikerjakan" value={formatDateTime(job.startedAt)} />}
              {job.finishedAt && <InfoItem icon={Clock} label="Selesai Dikerjakan" value={formatDateTime(job.finishedAt)} />}
              {job.closedAt && <InfoItem icon={Clock} label="Ditutup" value={formatDateTime(job.closedAt)} />}
            </div>
          </div>
        )}
      </div>

      {/* Photo gallery */}
      <div className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100">
        <h2 className="mb-4 text-lg font-semibold text-neutral-900">Foto</h2>
        {photos.length === 0 ? (
          <div className="flex flex-col items-center py-8 text-center">
            <div className="mb-3 rounded-full bg-neutral-100 p-3">
              <ImageIcon className="h-6 w-6 text-neutral-400" />
            </div>
            <p className="text-sm text-neutral-500">Belum ada foto</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {photos.map((photo, idx) => {
              const blobUrl = photoBlobUrls[photo.id] || '';
              return (
                <motion.div
                  key={photo.id || idx}
                  onClick={() => blobUrl && setLightboxUrl(blobUrl)}
                  className={`group relative aspect-square overflow-hidden rounded-xl bg-neutral-100 ${blobUrl ? 'cursor-pointer' : 'opacity-60'}`}
                  whileHover={{ scale: blobUrl ? 1.02 : 1 }}
                  transition={{ duration: 0.2 }}
                >
                  {blobUrl ? (
                    <img
                      src={blobUrl}
                      alt={photo.fileName || `Foto ${idx + 1}`}
                      className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <ImageIcon className="h-8 w-8 text-neutral-300" />
                    </div>
                  )}
                </motion.div>
              );
            })}
          </div>
        )}
      </div>

      {/* Status history timeline */}
      <div className="rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100">
        <h2 className="mb-4 text-lg font-semibold text-neutral-900">Riwayat Status</h2>
        {history.length === 0 ? (
          <p className="py-4 text-center text-sm text-neutral-500">Belum ada riwayat</p>
        ) : (
          <div className="relative pl-6">
            {/* Vertical line */}
            <div className="absolute left-[9px] top-2 bottom-2 w-0.5 bg-neutral-200" />

            {history.map((entry, idx) => (
              <div key={entry.id || idx} className="relative mb-6 last:mb-0">
                {/* Dot */}
                <div
                  className={`absolute -left-6 top-1.5 h-[18px] w-[18px] rounded-full border-[3px] ${
                    idx === history.length - 1
                      ? 'border-primary-500 bg-primary-100'
                      : 'border-neutral-300 bg-white'
                  }`}
                />
                <div className="ml-2">
                  <p className="text-sm font-semibold text-neutral-800">
                    {statusLabel(entry.toStatus)}
                  </p>
                  {entry.fromStatus && (
                    <p className="mt-0.5 text-xs text-neutral-400">
                      dari {statusLabel(entry.fromStatus)}
                    </p>
                  )}
                  {entry.note && (
                    <p className="mt-0.5 text-sm text-neutral-500">{entry.note}</p>
                  )}
                  <p className="mt-1 text-xs text-neutral-400">
                    {formatDateTime(entry.changedAt)}
                    {entry.changedByName && ` \u2014 ${entry.changedByName}`}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Assign Modal */}
      <Modal isOpen={assignModal} onClose={() => setAssignModal(false)} title="Tugaskan Teknisi">
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">Teknisi</label>
            <select
              value={assignForm.technicianId}
              onChange={(e) => setAssignForm((f) => ({ ...f, technicianId: e.target.value }))}
              className="input-field"
            >
              <option value="">Pilih teknisi...</option>
              {technicians.map((tech) => (
                <option key={tech.id} value={tech.id}>
                  {tech.fullName || tech.username}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">
              Tanggal Jadwal <span className="text-neutral-400">(opsional)</span>
            </label>
            <input
              type="date"
              value={assignForm.scheduledDate}
              min={todayWIB}
              onChange={(e) => setAssignForm((f) => ({ ...f, scheduledDate: e.target.value }))}
              className="input-field"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setAssignModal(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleAssign} disabled={submitting} className="btn-primary">
              {submitting ? 'Menyimpan...' : 'Tugaskan'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Reschedule Modal */}
      <Modal
        isOpen={rescheduleModal}
        onClose={() => setRescheduleModal(false)}
        title="Jadwalkan Ulang"
      >
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">
              Tanggal Baru
            </label>
            <input
              type="date"
              value={rescheduleForm.scheduledDate}
              min={todayWIB}
              onChange={(e) =>
                setRescheduleForm((f) => ({ ...f, scheduledDate: e.target.value }))
              }
              className="input-field"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-neutral-700">
              Teknisi <span className="text-neutral-400">(opsional)</span>
            </label>
            <select
              value={rescheduleForm.technicianId}
              onChange={(e) =>
                setRescheduleForm((f) => ({ ...f, technicianId: e.target.value }))
              }
              className="input-field"
            >
              <option value="">Teknisi saat ini</option>
              {technicians.map((tech) => (
                <option key={tech.id} value={tech.id}>
                  {tech.fullName || tech.username}
                </option>
              ))}
            </select>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setRescheduleModal(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleReschedule} disabled={submitting} className="btn-primary">
              {submitting ? 'Menyimpan...' : 'Jadwalkan Ulang'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Cancel Confirmation Modal */}
      <Modal isOpen={cancelConfirm} onClose={() => setCancelConfirm(false)} title="Batalkan Pekerjaan" size="sm">
        <div>
          <p className="mb-4 text-sm text-neutral-600">
            Apakah Anda yakin ingin membatalkan pekerjaan <strong>{job.title}</strong>? Tindakan ini tidak dapat dibatalkan.
          </p>
          <div className="flex justify-end gap-3">
            <button onClick={() => setCancelConfirm(false)} className="btn-ghost">
              Tidak
            </button>
            <button onClick={handleCancel} disabled={submitting} className="btn-danger">
              {submitting ? 'Membatalkan...' : 'Ya, Batalkan'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Close Confirmation Modal */}
      <Modal isOpen={closeConfirm} onClose={() => setCloseConfirm(false)} title="Tutup Pekerjaan" size="sm">
        <div>
          <p className="mb-4 text-sm text-neutral-600">
            Apakah Anda yakin ingin menutup pekerjaan ini? Tindakan ini tidak dapat dibatalkan.
          </p>
          <div className="flex justify-end gap-3">
            <button onClick={() => setCloseConfirm(false)} className="btn-ghost">
              Batal
            </button>
            <button onClick={handleClose} disabled={submitting} className="btn-primary">
              {submitting ? 'Menutup...' : 'Ya, Tutup'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Photo Lightbox */}
      {lightboxUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setLightboxUrl(null)}
        >
          <button
            className="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white hover:bg-white/20"
            onClick={() => setLightboxUrl(null)}
          >
            <XIcon className="h-6 w-6" />
          </button>
          <img
            src={lightboxUrl}
            alt="Foto pekerjaan"
            className="max-h-[90vh] max-w-[90vw] rounded-xl object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </motion.div>
  );
}

function InfoItem({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-3">
      <div className="mt-0.5 rounded-lg bg-neutral-50 p-2">
        <Icon className="h-4 w-4 text-neutral-400" />
      </div>
      <div>
        <p className="text-xs font-medium text-neutral-400">{label}</p>
        <p className="text-sm font-medium text-neutral-800">{value}</p>
      </div>
    </div>
  );
}
