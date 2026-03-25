interface GuestEmailsTextareaProps {
  value: string;
  onChange: (value: string) => void;
}

const GuestEmailsTextarea = ({ value, onChange }: GuestEmailsTextareaProps) => {
  return (
    <div className="space-y-2">
      <label htmlFor="guestEmails" className="block text-sm font-medium text-slate-700">
        Email гостей
      </label>

      <textarea
        id="guestEmails"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={6}
        placeholder="Введите email-адреса через запятую, пробел или с новой строки"
        className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-900 focus:ring-4 focus:ring-slate-100"
      />

      <p className="text-xs text-slate-500">
        После создания мероприятия приглашения будут отправлены всем указанным адресам.
      </p>
    </div>
  );
};

export default GuestEmailsTextarea;