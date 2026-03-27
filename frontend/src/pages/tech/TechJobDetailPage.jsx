import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  Play,
  CheckCircle2,
  AlertTriangle,
  Upload,
  Camera,
  X,
  ImageIcon,
  Calendar,
  User,
  MapPin,
  FileText,
  ZoomIn,
  Truck,
  Clock,
} from 'lucide-react';
import * as faceapi from 'face-api.js';
import { techApi } from '../../api/client';
import StatusBadge from '../../components/StatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useToast } from '../../components/Toast';

const pageVariants = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
};

const API_BASE = '/api';

export default function TechJobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const fileInputRef = useRef(null);

  const [job, setJob] = useState(null);
  const [photos, setPhotos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState('');

  // Face detection model
  const [modelLoaded, setModelLoaded] = useState(false);
  const [detecting, setDetecting] = useState(false);

  // Photo upload state
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);

  // Follow up modal
  const [followUpModal, setFollowUpModal] = useState(false);
  const [followUpReason, setFollowUpReason] = useState('');
  const [followUpLoading, setFollowUpLoading] = useState(false);

  // Lightbox
  const [lightboxUrl, setLightboxUrl] = useState(null);

  // Job history
  const [history, setHistory] = useState([]);

  // Load face detection model on mount
  useEffect(() => {
    faceapi.nets.tinyFaceDetector.loadFromUri('/models')
      .then(() => setModelLoaded(true))
      .catch(() => {
        // Model failed to load — face detection will be skipped
      });
  }, []);

  const fetchJob = useCallback(async () => {
    try {
      const data = await techApi.getJob(id);
      setJob(data);
    } catch (err) {
      setError('Gagal memuat detail tugas.');
    }
  }, [id]);

  const fetchPhotos = useCallback(async () => {
    try {
      const data = await techApi.getPhotos(id);
      setPhotos(Array.isArray(data) ? data : []);
    } catch {
      // photos may not exist yet
    }
  }, [id]);

  const fetchHistory = useCallback(async () => {
    try {
      const data = await techApi.getJobHistory(id);
      setHistory(Array.isArray(data) ? data : []);
    } catch {
      // history may not be available yet
    }
  }, [id]);

  useEffect(() => {
    async function load() {
      setLoading(true);
      await Promise.all([fetchJob(), fetchPhotos(), fetchHistory()]);
      setLoading(false);
    }
    load();
  }, [fetchJob, fetchPhotos, fetchHistory]);

  // Generate previews for selected files
  useEffect(() => {
    const urls = selectedFiles.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [selectedFiles]);

  const hasFaceInImage = async (file) => {
    try {
      const img = await faceapi.bufferToImage(file);
      const result = await faceapi.detectSingleFace(
        img,
        new faceapi.TinyFaceDetectorOptions({ scoreThreshold: 0.4 })
      );
      return !!result;
    } catch {
      return false;
    }
  };

  const isAllowedFile = (f) => {
    const type = f.type.toLowerCase();
    const name = f.name.toLowerCase();
    return (
      type.startsWith('image/') ||
      type === 'application/pdf' ||
      name.endsWith('.heic') ||
      name.endsWith('.heif')
    );
  };

  const needsFaceCheck = (f) => {
    const type = f.type.toLowerCase();
    const name = f.name.toLowerCase();
    return (
      type.startsWith('image/') &&
      type !== 'image/heic' &&
      type !== 'image/heif' &&
      !name.endsWith('.heic') &&
      !name.endsWith('.heif')
    );
  };

  const handleFilesSelected = async (files) => {
    const validFiles = Array.from(files).filter(isAllowedFile);
    if (validFiles.length === 0) {
      toast.error('Pilih file gambar (JPG, PNG, HEIC) atau PDF.');
      return;
    }

    const passthrough = validFiles.filter((f) => !needsFaceCheck(f));
    const toCheck = validFiles.filter(needsFaceCheck);

    if (toCheck.length === 0) {
      setSelectedFiles((prev) => [...prev, ...passthrough]);
      return;
    }

    if (!modelLoaded) {
      toast.error('Model deteksi wajah belum siap, coba lagi sebentar.');
      return;
    }

    setDetecting(true);
    const accepted = [...passthrough];
    const rejected = [];

    for (const file of toCheck) {
      const hasFace = await hasFaceInImage(file);
      if (hasFace) {
        accepted.push(file);
      } else {
        rejected.push(file.name);
      }
    }
    setDetecting(false);

    if (rejected.length > 0) {
      toast.error(`Wajah tidak terdeteksi pada: ${rejected.join(', ')}. Foto harus memuat wajah teknisi.`);
    }
    if (accepted.length > 0) {
      setSelectedFiles((prev) => [...prev, ...accepted]);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    handleFilesSelected(e.dataTransfer.files);
  };

  const removeFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) return;
    setUploading(true);
    try {
      for (const f of selectedFiles) {
        const formData = new FormData();
        formData.append('file', f);
        await techApi.uploadPhoto(id, formData);
      }
      toast.success('Foto berhasil diupload!');
      setSelectedFiles([]);
      await fetchPhotos();
      await fetchJob();
    } catch (err) {
      toast.error('Gagal mengupload foto.');
    } finally {
      setUploading(false);
    }
  };

  const handleAction = async (action) => {
    setActionLoading(action);
    try {
      if (action === 'transit') {
        await techApi.transit(id);
        toast.success('Status: Dalam Perjalanan');
      } else if (action === 'start') {
        await techApi.startJob(id);
        toast.success('Tugas dimulai!');
      } else if (action === 'finish') {
        await techApi.finishJob(id);
        toast.success('Tugas selesai!');
      }
      await Promise.all([fetchJob(), fetchHistory()]);
    } catch (err) {
      const msg = err?.message || 'Gagal melakukan aksi.';
      toast.error(msg);
    } finally {
      setActionLoading('');
    }
  };

  const handleFollowUpSubmit = async () => {
    if (!followUpReason.trim()) return;
    setFollowUpLoading(true);
    try {
      await techApi.followUp(id, followUpReason.trim());
      toast.success('Follow up berhasil ditandai.');
      setFollowUpModal(false);
      setFollowUpReason('');
      await Promise.all([fetchJob(), fetchHistory()]);
    } catch (err) {
      const msg = err?.message || 'Gagal melakukan follow up.';
      toast.error(msg);
    } finally {
      setFollowUpLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('id-ID', {
      weekday: 'long',
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

  const getPhotoUrl = (photo) => {
    if (photo.downloadUrl) {
      try { return new URL(photo.downloadUrl).pathname; } catch { return photo.downloadUrl; }
    }
    return photo.url || '';
  };

  if (loading) return <LoadingSpinner />;

  if (error || !job) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4">
        <div className="rounded-xl border border-red-200 bg-red-50 px-6 py-4 text-center">
          <p className="text-sm font-medium text-red-600">{error || 'Tugas tidak ditemukan.'}</p>
        </div>
        <button onClick={() => navigate('/tech/jobs')} className="btn-ghost text-sm">
          <ArrowLeft size={16} className="mr-1 inline" /> Kembali
        </button>
      </div>
    );
  }

  const canUploadPhoto = job.status === 'IN_TRANSIT' || job.status === 'IN_PROGRESS';
  const requiresPhoto = job.requiresPhoto;
  const finishDisabled = requiresPhoto && photos.length === 0;

  return (
    <motion.div
      variants={pageVariants}
      initial="hidden"
      animate="visible"
      className="min-h-screen bg-neutral-50"
    >
      <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
        {/* Back Button */}
        <motion.button
          initial={{ opacity: 0, x: -10 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3 }}
          onClick={() => navigate('/tech/jobs')}
          className="btn-ghost mb-6 flex items-center gap-2 text-sm"
        >
          <ArrowLeft size={18} />
          Kembali ke Daftar Tugas
        </motion.button>

        {/* Job Info Card */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
          className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100"
        >
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
            <h1 className="text-xl font-bold text-neutral-800 sm:text-2xl">{job.title}</h1>
            <StatusBadge status={job.status} />
          </div>

          {job.description && (
            <p className="mb-5 text-sm leading-relaxed text-neutral-600">{job.description}</p>
          )}

          <div className="grid grid-cols-1 gap-3 border-t border-neutral-100 pt-5 sm:grid-cols-2">
            <div className="flex items-center gap-3 text-sm text-neutral-600">
              <div className="rounded-lg bg-primary-50 p-2">
                <User size={16} className="text-primary-600" />
              </div>
              <div>
                <p className="text-xs text-neutral-400">Pelanggan</p>
                <p className="font-medium">{job.customerName || '-'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm text-neutral-600">
              <div className="rounded-lg bg-primary-50 p-2">
                <MapPin size={16} className="text-primary-600" />
              </div>
              <div>
                <p className="text-xs text-neutral-400">Alamat</p>
                <p className="font-medium">{job.address || '-'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm text-neutral-600">
              <div className="rounded-lg bg-primary-50 p-2">
                <Calendar size={16} className="text-primary-600" />
              </div>
              <div>
                <p className="text-xs text-neutral-400">Jadwal</p>
                <p className="font-medium">{formatDate(job.scheduledDate)}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm text-neutral-600">
              <div className="rounded-lg bg-primary-50 p-2">
                <FileText size={16} className="text-primary-600" />
              </div>
              <div>
                <p className="text-xs text-neutral-400">Status</p>
                <p className="font-medium capitalize">{job.status?.replace(/_/g, ' ') || '-'}</p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Action Buttons */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.2 }}
          className="mb-6"
        >
          {/* ASSIGNED: Button "Dalam Perjalanan" */}
          {job.status === 'ASSIGNED' && (
            <motion.button
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => handleAction('transit')}
              disabled={actionLoading === 'transit'}
              className="btn-primary flex w-full items-center justify-center gap-2 py-3 text-base sm:w-auto"
            >
              {actionLoading === 'transit' ? (
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
              ) : (
                <Truck size={18} />
              )}
              Dalam Perjalanan
            </motion.button>
          )}

          {/* IN_TRANSIT: Button "Mulai Kerjakan" */}
          {job.status === 'IN_TRANSIT' && (
            <motion.button
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => handleAction('start')}
              disabled={actionLoading === 'start'}
              className="btn-primary flex w-full items-center justify-center gap-2 py-3 text-base sm:w-auto"
            >
              {actionLoading === 'start' ? (
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
              ) : (
                <Play size={18} />
              )}
              Mulai Kerjakan
            </motion.button>
          )}

          {/* IN_PROGRESS: Selesai + Follow Up */}
          {job.status === 'IN_PROGRESS' && (
            <div className="flex flex-wrap gap-3">
              <motion.button
                whileHover={{ scale: 1.02, y: -1 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => handleAction('finish')}
                disabled={actionLoading === 'finish' || finishDisabled}
                className="btn-primary relative flex items-center gap-2 py-3 text-base"
                title={finishDisabled ? 'Upload foto terlebih dahulu sebelum menyelesaikan tugas' : ''}
              >
                {actionLoading === 'finish' ? (
                  <div className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                ) : (
                  <CheckCircle2 size={18} />
                )}
                Selesai
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.02, y: -1 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => fileInputRef.current?.click()}
                className="btn-secondary flex items-center gap-2 py-3"
              >
                <Camera size={18} />
                Upload Foto
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.02, y: -1 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => setFollowUpModal(true)}
                className="flex items-center gap-2 rounded-xl border-2 border-orange-200 bg-orange-50 px-6 py-3 font-semibold text-orange-700 transition-all duration-200 hover:border-orange-300 hover:bg-orange-100 active:scale-[0.97]"
              >
                <AlertTriangle size={18} />
                Butuh Follow Up
              </motion.button>
            </div>
          )}

          {finishDisabled && job.status === 'IN_PROGRESS' && (
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="mt-2 flex items-center gap-1.5 text-xs font-medium text-amber-600"
            >
              <AlertTriangle size={14} />
              Upload foto terlebih dahulu sebelum menyelesaikan tugas.
            </motion.p>
          )}
        </motion.div>

        {/* Photo Upload Section */}
        {canUploadPhoto && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.3 }}
            className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100"
          >
            <h2 className="mb-4 text-base font-semibold text-neutral-800 flex items-center gap-2">
              <Camera size={18} className="text-primary-600" />
              Upload Foto
              {!modelLoaded && (
                <span className="ml-2 text-xs font-normal text-neutral-400">(Memuat model deteksi wajah...)</span>
              )}
            </h2>

            {/* Drag & Drop Area */}
            <div
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`relative flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 transition-all duration-200 ${
                dragOver
                  ? 'border-primary-400 bg-primary-50'
                  : 'border-neutral-200 bg-neutral-50 hover:border-primary-300 hover:bg-primary-50/50'
              }`}
            >
              {detecting ? (
                <div className="flex flex-col items-center gap-2">
                  <div className="h-8 w-8 animate-spin rounded-full border-2 border-neutral-300 border-t-primary-500" />
                  <p className="text-sm text-neutral-500">Mendeteksi wajah...</p>
                </div>
              ) : (
                <>
                  <Upload
                    size={32}
                    className={`mb-3 transition-colors ${dragOver ? 'text-primary-500' : 'text-neutral-300'}`}
                  />
                  <p className="text-sm font-medium text-neutral-600">
                    Seret foto ke sini atau <span className="text-primary-600">klik untuk memilih</span>
                  </p>
                  <p className="mt-1 text-xs text-neutral-400">JPG, PNG, HEIC, PDF — foto wajah teknisi wajib untuk gambar</p>
                </>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/heic,image/heif,application/pdf,.heic,.heif,.pdf"
                multiple
                onChange={(e) => handleFilesSelected(e.target.files)}
                className="hidden"
              />
            </div>

            {/* Preview Thumbnails */}
            <AnimatePresence>
              {previews.length > 0 && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="mt-4"
                >
                  <div className="flex flex-wrap gap-3">
                    {previews.map((url, i) => {
                      const file = selectedFiles[i];
                      const isPdf = file?.type === 'application/pdf';
                      const isHeic = file?.type === 'image/heic' || file?.type === 'image/heif'
                        || file?.name?.toLowerCase().endsWith('.heic')
                        || file?.name?.toLowerCase().endsWith('.heif');
                      return (
                        <motion.div
                          key={url}
                          initial={{ opacity: 0, scale: 0.8 }}
                          animate={{ opacity: 1, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.8 }}
                          className="group relative h-20 w-20 overflow-hidden rounded-xl ring-1 ring-neutral-200"
                        >
                          {isPdf ? (
                            <div className="flex h-full w-full flex-col items-center justify-center bg-red-50">
                              <FileText size={22} className="text-red-400" />
                              <span className="mt-1 w-full truncate px-1 text-center text-[9px] text-red-400">
                                {file.name}
                              </span>
                            </div>
                          ) : isHeic ? (
                            <div className="flex h-full w-full flex-col items-center justify-center bg-neutral-100">
                              <ImageIcon size={22} className="text-neutral-400" />
                              <span className="mt-1 text-[9px] text-neutral-400">HEIC</span>
                            </div>
                          ) : (
                            <img
                              src={url}
                              alt={`Preview ${i + 1}`}
                              className="h-full w-full object-cover"
                            />
                          )}
                          <button
                            onClick={(e) => { e.stopPropagation(); removeFile(i); }}
                            className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-white opacity-0 shadow-sm transition-opacity group-hover:opacity-100"
                          >
                            <X size={12} />
                          </button>
                        </motion.div>
                      );
                    })}
                  </div>

                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={handleUpload}
                    disabled={uploading}
                    className="btn-primary mt-4 flex items-center gap-2"
                  >
                    {uploading ? (
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                    ) : (
                      <Upload size={16} />
                    )}
                    {uploading ? 'Mengupload...' : `Upload ${selectedFiles.length} Foto`}
                  </motion.button>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )}

        {/* Existing Photo Gallery */}
        {photos.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.35 }}
            className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100"
          >
            <h2 className="mb-4 text-base font-semibold text-neutral-800 flex items-center gap-2">
              <ImageIcon size={18} className="text-primary-600" />
              Foto ({photos.length})
            </h2>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
              {photos.map((photo, i) => (
                <motion.div
                  key={photo.id || i}
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: i * 0.05 }}
                  whileHover={{ scale: 1.03 }}
                  onClick={() => setLightboxUrl(getPhotoUrl(photo))}
                  className="group relative aspect-square cursor-pointer overflow-hidden rounded-xl ring-1 ring-neutral-200 transition-shadow hover:shadow-card-hover"
                >
                  <img
                    src={getPhotoUrl(photo)}
                    alt={`Foto ${i + 1}`}
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition-all group-hover:bg-black/20">
                    <ZoomIn
                      size={24}
                      className="text-white opacity-0 transition-opacity group-hover:opacity-100"
                    />
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

        {/* Job History Timeline */}
        {history.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.4 }}
            className="mb-6 rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100"
          >
            <h2 className="mb-4 text-base font-semibold text-neutral-800 flex items-center gap-2">
              <Clock size={18} className="text-primary-600" />
              Riwayat Pekerjaan
            </h2>
            <div className="relative pl-6">
              <div className="absolute left-[9px] top-2 bottom-2 w-0.5 bg-neutral-200" />
              {history.map((entry, idx) => (
                <div key={entry.id || idx} className="relative mb-5 last:mb-0">
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
          </motion.div>
        )}

        {/* Follow Up Modal */}
        <AnimatePresence>
          {followUpModal && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setFollowUpModal(false)}
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
            >
              <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.9, opacity: 0 }}
                transition={{ duration: 0.2 }}
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
              >
                <div className="mb-4 flex items-center gap-3">
                  <div className="rounded-xl bg-orange-100 p-2">
                    <AlertTriangle size={20} className="text-orange-600" />
                  </div>
                  <h3 className="text-lg font-semibold text-neutral-800">Butuh Follow Up</h3>
                </div>

                <p className="mb-4 text-sm text-neutral-500">
                  Jelaskan alasan mengapa pekerjaan ini membutuhkan tindak lanjut.
                </p>

                <textarea
                  value={followUpReason}
                  onChange={(e) => setFollowUpReason(e.target.value)}
                  placeholder="Contoh: Spare part tidak tersedia, perlu dipesan terlebih dahulu..."
                  rows={4}
                  maxLength={500}
                  className="input-field mb-1 min-h-[100px] resize-y"
                  autoFocus
                />
                <p className="mb-4 text-right text-xs text-neutral-400">
                  {followUpReason.length}/500
                </p>

                <div className="flex gap-3">
                  <button
                    onClick={() => { setFollowUpModal(false); setFollowUpReason(''); }}
                    className="btn-ghost flex-1"
                    disabled={followUpLoading}
                  >
                    Batal
                  </button>
                  <button
                    onClick={handleFollowUpSubmit}
                    disabled={!followUpReason.trim() || followUpLoading}
                    className="flex flex-1 items-center justify-center gap-2 rounded-xl border-2 border-orange-200 bg-orange-50 px-4 py-2.5 font-semibold text-orange-700 transition-all hover:bg-orange-100 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {followUpLoading ? (
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-orange-300 border-t-orange-600" />
                    ) : (
                      <AlertTriangle size={16} />
                    )}
                    Konfirmasi Follow Up
                  </button>
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Lightbox */}
        <AnimatePresence>
          {lightboxUrl && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setLightboxUrl(null)}
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
            >
              <motion.button
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="absolute right-4 top-4 rounded-full bg-white/10 p-2 text-white transition-colors hover:bg-white/20"
                onClick={() => setLightboxUrl(null)}
              >
                <X size={24} />
              </motion.button>
              <motion.img
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.8, opacity: 0 }}
                transition={{ duration: 0.3 }}
                src={lightboxUrl}
                alt="Foto detail"
                className="max-h-[85vh] max-w-[90vw] rounded-xl object-contain shadow-2xl"
                onClick={(e) => e.stopPropagation()}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}
