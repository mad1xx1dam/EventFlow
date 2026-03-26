import { useEffect, useState } from "react";

interface EventPosterUploaderProps {
  file: File | null;
  posterUrl?: string | null;
  onChange: (file: File | null) => void;
}

const EventPosterUploader = ({
  file,
  posterUrl,
  onChange,
}: EventPosterUploaderProps) => {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);

    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  const displayUrl = previewUrl || posterUrl;

  return (
    <div className="space-y-3">
      <label className="block text-sm font-medium text-slate-700">
        Постер мероприятия
      </label>

      {displayUrl ? (
        <div className="relative w-full overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 p-2">
          <img
            src={displayUrl}
            alt="Превью постера"
            className="max-h-96 w-full object-contain"
          />
          <button
            type="button"
            onClick={() => onChange(null)}
            className="absolute right-2 top-2 rounded-lg bg-white/80 p-1.5 text-xs font-semibold text-red-600 backdrop-blur-sm hover:bg-white"
          >
            Удалить
          </button>
        </div>
      ) : null}

      <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4">
        <input
          type="file"
          accept="image/*"
          onChange={(event) => onChange(event.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-700 file:mr-4 file:rounded-xl file:border-0 file:bg-slate-900 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-slate-800"
        />

        <p className="mt-3 text-sm text-slate-500">
          {file
            ? `Выбран файл: ${file.name}`
            : displayUrl
              ? "Текущий постер"
              : "Файл пока не выбран"}
        </p>
      </div>
    </div>
  );
};

export default EventPosterUploader;