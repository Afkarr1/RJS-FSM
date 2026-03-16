import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Star, Upload, X, CheckCircle2, AlertCircle, Wrench, Camera } from 'lucide-react';
import { publicApi } from '../../api/client';

const MAX_CHARS = 1000;

function StarRating({ value, onChange, disabled }) {
  const [hovered, setHovered] = useState(0);

  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={disabled}
          onClick={() => onChange(star)}
          onMouseEnter={() => !disabled && setHovered(star)}
          onMouseLeave={() => setHovered(0)}
          className="transition-transform duration-150 disabled:cursor-not-allowed"
          style={{ transform: (hovered || value) >= star ? 'scale(1.15)' : 'scale(1)' }}
        >
          <Star
            size={36}
            className={`transition-colors duration-150 ${
              (hovered || value) >= star
                ? 'fill-amber-400 text-amber-400'
                : 'fill-neutral-200 text-neutral-200'
            }`}
          />
        </button>
      ))}
    </div>
  );
}

const RATING_LABELS = {
  1: 'Sangat Tidak Puas',
  2: 'Tidak Puas',
  3: 'Cukup',
  4: 'Puas',
  5: 'Sangat Puas',
};

export default function ReviewPage() {
  const { token } = useParams();
  const fileInputRef = useRef(null);

  const [reviewData, setReviewData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Form state
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [photos, setPhotos] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    async function fetchReview() {
      try {
        const data = await publicApi.getReview(token);
        setReviewData(data);
        if (data.submitted) setSubmitted(true);
      } catch (err) {
        if (err?.status === 404) {
          setError('Link ulasan tidak ditemukan atau sudah tidak valid.');
        } else {
          setError('Gagal memuat halaman ulasan. Silakan coba lagi.');
        }
      } finally {
        setLoading(false);
      }
    }
    fetchReview();
  }, [token]);

  useEffect(() => {
    const urls = photos.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [photos]);

  const handleFilesSelected = (files) => {
    const validFiles = Array.from(files).filter((f) => f.type.startsWith('image/'));
    if (validFiles.length === 0) return;
    setPhotos((prev) => [...prev, ...validFiles].slice(0, 5));
  };

  const handleDrop = (e) => {
    e.preventDefault();
    handleFilesSelected(e.dataTransfer.files);
  };

  const removePhoto = (index) => {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rating === 0) {
      setSubmitError('Harap berikan penilaian bintang.');
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      const formData = new FormData();
      formData.append('rating', rating);
      formData.append('comment', comment);
      photos.forEach((f) => formData.append('photos', f));
      await publicApi.submitReview(token, formData);
      setSubmitted(true);
    } catch (err) {
      if (err?.status === 409) {
        setSubmitError('Ulasan sudah pernah dikirimkan.');
      } else {
        setSubmitError('Gagal mengirimkan ulasan. Silakan coba lagi.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-neutral-50">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary-200 border-t-primary-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-neutral-50 px-4">
        <div className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-card ring-1 ring-neutral-100">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-100">
            <AlertCircle className="h-7 w-7 text-red-500" />
          </div>
          <h2 className="text-lg font-bold text-neutral-800">Link Tidak Valid</h2>
          <p className="mt-2 text-sm text-neutral-500">{error}</p>
        </div>
      </div>
    );
  }

  if (submitted) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-neutral-50 px-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
          className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-card ring-1 ring-neutral-100"
        >
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
            className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-green-100"
          >
            <CheckCircle2 className="h-9 w-9 text-green-500" />
          </motion.div>
          <h2 className="text-xl font-bold text-neutral-800">Terima Kasih!</h2>
          <p className="mt-2 text-sm text-neutral-500">
            Ulasan Anda telah berhasil dikirimkan. Kami sangat menghargai masukan Anda.
          </p>
          <div className="mt-6 rounded-xl bg-neutral-50 px-4 py-3 text-sm text-neutral-400">
            &copy; {new Date().getFullYear()} Restu Jaya Sentosa
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-50 px-4 py-10">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
        className="mx-auto w-full max-w-lg"
      >
        {/* Header */}
        <div className="mb-6 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-500 shadow-button">
            <Wrench className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-neutral-800">Berikan Ulasan Anda</h1>
          <p className="mt-1.5 text-sm text-neutral-500">
            {reviewData?.jobTitle
              ? `Pekerjaan: ${reviewData.jobTitle}`
              : 'Bantu kami meningkatkan kualitas layanan'}
          </p>
        </div>

        {/* Form Card */}
        <div className="rounded-2xl bg-white p-6 shadow-card ring-1 ring-neutral-100 sm:p-8">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Rating */}
            <div>
              <label className="mb-3 block text-sm font-semibold text-neutral-700">
                Penilaian <span className="text-red-500">*</span>
              </label>
              <div className="flex flex-col items-center gap-2">
                <StarRating value={rating} onChange={setRating} disabled={submitting} />
                <AnimatePresence mode="wait">
                  {rating > 0 && (
                    <motion.span
                      key={rating}
                      initial={{ opacity: 0, y: -4 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0 }}
                      className="text-sm font-medium text-amber-600"
                    >
                      {RATING_LABELS[rating]}
                    </motion.span>
                  )}
                </AnimatePresence>
              </div>
            </div>

            {/* Comment */}
            <div>
              <label className="mb-1.5 block text-sm font-semibold text-neutral-700">
                Komentar
              </label>
              <div className="relative">
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value.slice(0, MAX_CHARS))}
                  disabled={submitting}
                  rows={4}
                  placeholder="Ceritakan pengalaman Anda dengan layanan kami..."
                  className="w-full resize-none rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 text-sm text-neutral-800 placeholder-neutral-400 outline-none transition-all focus:border-primary-400 focus:bg-white focus:ring-2 focus:ring-primary-100 disabled:opacity-60"
                />
                <span
                  className={`absolute bottom-2.5 right-3 text-xs ${
                    comment.length >= MAX_CHARS ? 'text-red-400' : 'text-neutral-300'
                  }`}
                >
                  {comment.length}/{MAX_CHARS}
                </span>
              </div>
            </div>

            {/* Photo upload */}
            <div>
              <label className="mb-1.5 block text-sm font-semibold text-neutral-700">
                Foto (opsional, maks. 5)
              </label>

              {photos.length < 5 && (
                <div
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-neutral-200 bg-neutral-50 px-4 py-6 text-center transition-colors hover:border-primary-300 hover:bg-primary-50/40"
                >
                  <Camera size={24} className="text-neutral-300" />
                  <p className="text-sm text-neutral-500">
                    Seret foto ke sini atau{' '}
                    <span className="font-medium text-primary-600">klik untuk memilih</span>
                  </p>
                  <p className="text-xs text-neutral-400">JPG, PNG, WEBP</p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={(e) => handleFilesSelected(e.target.files)}
                    className="hidden"
                  />
                </div>
              )}

              {previews.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {previews.map((url, i) => (
                    <div
                      key={url}
                      className="group relative h-20 w-20 overflow-hidden rounded-xl ring-1 ring-neutral-200"
                    >
                      <img src={url} alt={`Preview ${i + 1}`} className="h-full w-full object-cover" />
                      <button
                        type="button"
                        onClick={() => removePhoto(i)}
                        className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-white opacity-0 shadow-sm transition-opacity group-hover:opacity-100"
                      >
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Error */}
            <AnimatePresence>
              {submitError && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-600"
                >
                  <AlertCircle size={16} className="shrink-0" />
                  {submitError}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Submit */}
            <motion.button
              type="submit"
              disabled={submitting || rating === 0}
              whileHover={!submitting && rating > 0 ? { y: -1 } : {}}
              whileTap={!submitting && rating > 0 ? { scale: 0.97 } : {}}
              className="btn-primary w-full py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
            >
              {submitting ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  Mengirimkan...
                </span>
              ) : (
                'Kirim Ulasan'
              )}
            </motion.button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-neutral-400">
          &copy; {new Date().getFullYear()} Restu Jaya Sentosa
        </p>
      </motion.div>
    </div>
  );
}
