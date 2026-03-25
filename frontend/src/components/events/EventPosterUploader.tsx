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
  return (
    <div className="space-y-3">
      <label className="block text-sm font-medium text-slate-700">Постер мероприятия</label>

      <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4">
        <input
          type="file"
          accept="image/*"
          onChange={(event) => onChange(event.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-700 file:mr-4 file:rounded-xl file:border-0 file:bg-slate-900 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-slate-800"
        />

        {file ? (
          <p className="mt-3 text-sm text-slate-600">Выбран файл: {file.name}</p>
        ) : posterUrl ? (
          <p className="mt-3 text-sm text-slate-600">Текущий постер уже загружен</p>
        ) : (
          <p className="mt-3 text-sm text-slate-500">Файл пока не выбран</p>
        )}
      </div>
    </div>
  );
};

export default EventPosterUploader;