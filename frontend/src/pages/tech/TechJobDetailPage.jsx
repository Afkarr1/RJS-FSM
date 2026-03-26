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
} from 'lucide-react';
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

  // Photo upload state
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);

  // Lightbox
  const [lightboxUrl, setLightboxUrl] = useState(null);

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

  useEffect(() => {
    async function load() {
      setLoading(true);
      await Promise.all([fetchJob(), fetchPhotos()]);
      setLoading(false);
    }
    load();
  }, [fetchJob, fetchPhotos]);

  // Generate previews for selected files
  useEffect(() => {
    const urls = selectedFiles.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [selectedFiles]);

  const handleFilesSelected = (files) => {
    const validFiles = Array.from(files).filter((f) => f.type.startsWith('image/'));
    if (validFiles.length === 0) {
      toast.error('Pilih file gambar (JPG, PNG, dll).');
      return;
    }
    setSelectedFiles((prev) => [...prev, ...validFiles]);
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
      const formData = new FormData();
      selectedFiles.forEach((f) => formData.append('photos', f));
      await techApi.uploadPhoto(id, formData);
      toast.success('Foto berhasil diupload!');
      setSelectedFiles([]);
      await fetchPhotos();
    } catch (err) {
      toast.error('Gagal mengupload foto.');
    } finally {
      setUploading(false);
    }
  };

  const handleAction = async (action) => {
    setActionLoading(action);
    try {
      if (action === 'start') {
        await techApi.startJob(id);
        toast.success('Tugas dimulai!');
      } else if (action === 'finish') {
        await techApi.finishJob(id);
        toast.success('Tugas selesai!');
      } else if (action === 'followup') {
        await techApi.followUp(id);
        toast.success('Follow up berhasil ditandai.');
      }
      await fetchJob();
    } catch (err) {
      const msg = err?.message || 'Gagal melakukan aksi.';
      toast.error(msg);
    } finally {
      setActionLoading('');
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

  const getPhotoUrl = (photo) => {
    if (photo.url) return photo.url;
    if (photo.id) return `${API_BASE}/tech/jobs/${id}/photos/${photo.id}`;
    return '';
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

  const canUploadPhoto = job.status === 'IN_PROGRESS' || job.status === 'ASSIGNED';
  const requiresPhoto = job.requiresPhoto;
  const photoUploaded = photos.length > 0 || selectedFiles.length > 0;
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
          {job.status === 'ASSIGNED' && (
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
                {finishDisabled && (
                  <span className="absolute -top-10 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-neutral-800 px-3 py-1.5 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100 pointer-events-none shadow-lg">
                    Upload foto terlebih dahulu
                  </span>
                )}
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
                onClick={() => handleAction('followup')}
                disabled={actionLoading === 'followup'}
                className="flex items-center gap-2 rounded-xl border-2 border-orange-200 bg-orange-50 px-6 py-3 font-semibold text-orange-700 transition-all duration-200 hover:border-orange-300 hover:bg-orange-100 active:scale-[0.97]"
              >
                {actionLoading === 'followup' ? (
                  <div className="h-5 w-5 animate-spin rounded-full border-2 border-orange-300 border-t-orange-600" />
                ) : (
                  <AlertTriangle size={18} />
                )}
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
            </h2>

            {/* Drag & Drop Area */}
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragOver(true);
              }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`relative flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 transition-all duration-200 ${
                dragOver
                  ? 'border-primary-400 bg-primary-50'
                  : 'border-neutral-200 bg-neutral-50 hover:border-primary-300 hover:bg-primary-50/50'
              }`}
            >
              <Upload
                size={32}
                className={`mb-3 transition-colors ${
                  dragOver ? 'text-primary-500' : 'text-neutral-300'
                }`}
              />
              <p className="text-sm font-medium text-neutral-600">
                Seret foto ke sini atau <span className="text-primary-600">klik untuk memilih</span>
              </p>
              <p className="mt-1 text-xs text-neutral-400">JPG, PNG, WEBP</p>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
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
                    {previews.map((url, i) => (
                      <motion.div
                        key={url}
                        initial={{ opacity: 0, scale: 0.8 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.8 }}
                        className="group relative h-20 w-20 overflow-hidden rounded-xl ring-1 ring-neutral-200"
                      >
                        <img
                          src={url}
                          alt={`Preview ${i + 1}`}
                          className="h-full w-full object-cover"
                        />
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            removeFile(i);
                          }}
                          className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-white opacity-0 shadow-sm transition-opacity group-hover:opacity-100"
                        >
                          <X size={12} />
                        </button>
                      </motion.div>
                    ))}
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
