import { useMemo } from "react";

interface GuestEmailsTextareaProps {
  value: string;
  onChange: (value: string) => void;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/i;

const parseRawTokens = (value: string) => {
  return value
    .split(/[\s,;]+/)
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);
};

const GuestEmailsTextarea = ({ value, onChange }: GuestEmailsTextareaProps) => {
  const parsed = useMemo(() => {
    const rawTokens = parseRawTokens(value);
    const uniqueValidEmails: string[] = [];
    const invalidEmails: string[] = [];
    const duplicateEmails: string[] = [];

    const seen = new Set<string>();
    const duplicateSeen = new Set<string>();

    for (const token of rawTokens) {
      if (!EMAIL_PATTERN.test(token)) {
        invalidEmails.push(token);
        continue;
      }

      if (seen.has(token)) {
        if (!duplicateSeen.has(token)) {
          duplicateEmails.push(token);
          duplicateSeen.add(token);
        }
        continue;
      }

      seen.add(token);
      uniqueValidEmails.push(token);
    }

    return {
      uniqueValidEmails,
      invalidEmails,
      duplicateEmails,
      rawCount: rawTokens.length,
    };
  }, [value]);

  const hasPreview =
    parsed.uniqueValidEmails.length > 0 ||
    parsed.invalidEmails.length > 0 ||
    parsed.duplicateEmails.length > 0;

  return (
    <div className="space-y-3">
      <label htmlFor="guestEmails" className="block text-sm font-medium text-slate-700">
        Email гостей
      </label>

      <textarea
        id="guestEmails"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={6}
        placeholder="Введите email-адреса через запятую, пробел, точку с запятой или с новой строки"
        className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-900 focus:ring-4 focus:ring-slate-100"
      />

      <p className="text-xs text-slate-500">
        Можно вставить сразу несколько адресов. Повторяющиеся email не будут отправлены повторно.
      </p>

      {hasPreview ? (
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="flex flex-wrap gap-3 text-xs text-slate-600">
            <span className="rounded-full border border-slate-200 bg-white px-3 py-1">
              Найдено адресов: {parsed.rawCount}
            </span>
            <span className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-emerald-800">
              Валидных: {parsed.uniqueValidEmails.length}
            </span>
            <span className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-amber-800">
              Дубликатов: {parsed.duplicateEmails.length}
            </span>
            <span className="rounded-full border border-red-200 bg-red-50 px-3 py-1 text-red-700">
              Невалидных: {parsed.invalidEmails.length}
            </span>
          </div>

          {parsed.uniqueValidEmails.length > 0 ? (
            <div className="mt-4 space-y-2">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Будут отправлены
              </p>
              <div className="flex flex-wrap gap-2">
                {parsed.uniqueValidEmails.map((email) => (
                  <span
                    key={email}
                    className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-800"
                  >
                    {email}
                  </span>
                ))}
              </div>
            </div>
          ) : null}

          {parsed.duplicateEmails.length > 0 ? (
            <div className="mt-4 space-y-2">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Дубликаты
              </p>
              <div className="flex flex-wrap gap-2">
                {parsed.duplicateEmails.map((email) => (
                  <span
                    key={email}
                    className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-medium text-amber-800"
                  >
                    {email}
                  </span>
                ))}
              </div>
            </div>
          ) : null}

          {parsed.invalidEmails.length > 0 ? (
            <div className="mt-4 space-y-2">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Невалидные адреса
              </p>
              <div className="flex flex-wrap gap-2">
                {parsed.invalidEmails.map((email, index) => (
                  <span
                    key={`${email}-${index}`}
                    className="rounded-full border border-red-200 bg-red-50 px-3 py-1 text-xs font-medium text-red-700"
                  >
                    {email}
                  </span>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
};

export default GuestEmailsTextarea;