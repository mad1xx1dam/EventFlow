import { useEffect, useState } from "react";
import dashboardApi from "../../api/dashboardApi";
import EventCalendar from "../../components/calendar/EventCalendar";
import type { DashboardSummaryResponse } from "../../types/calendar";

const DashboardPage = () => {
  const today = new Date();

  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(true);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  useEffect(() => {
    const loadSummary = async () => {
      setIsLoadingSummary(true);
      setSummaryError(null);

      try {
        const response = await dashboardApi.getSummary();
        setSummary(response);
      } catch {
        setSummaryError("Не удалось загрузить данные дашборда");
      } finally {
        setIsLoadingSummary(false);
      }
    };

    void loadSummary();
  }, []);

  const handlePrevMonth = () => {
    setMonth((currentMonth) => {
      if (currentMonth === 1) {
        setYear((currentYear) => currentYear - 1);
        return 12;
      }

      return currentMonth - 1;
    });
  };

  const handleNextMonth = () => {
    setMonth((currentMonth) => {
      if (currentMonth === 12) {
        setYear((currentYear) => currentYear + 1);
        return 1;
      }

      return currentMonth + 1;
    });
  };

  return (
    <div className="space-y-6">
      <section>
        <div className="mb-4">
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">Дашборд</h1>
          <p className="mt-2 text-sm text-slate-500">
            Общая статистика и календарь ваших мероприятий
          </p>
        </div>

        {isLoadingSummary ? (
          <div className="grid gap-4 md:grid-cols-2">
            <div className="h-32 animate-pulse rounded-3xl border border-slate-200 bg-slate-100" />
            <div className="h-32 animate-pulse rounded-3xl border border-slate-200 bg-slate-100" />
          </div>
        ) : summaryError ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {summaryError}
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <p className="text-sm font-medium text-slate-500">Создано мероприятий</p>
              <p className="mt-3 text-4xl font-bold tracking-tight text-slate-900">
                {summary?.createdEventsCount ?? 0}
              </p>
              <p className="mt-2 text-sm text-slate-500">Все мероприятия, созданные вами</p>
            </div>

            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
              <p className="text-sm font-medium text-slate-500">Принятые приглашения</p>
              <p className="mt-3 text-4xl font-bold tracking-tight text-slate-900">
                {summary?.acceptedInvitationsCount ?? 0}
              </p>
              <p className="mt-2 text-sm text-slate-500">Мероприятия, где вы участвуете как гость</p>
            </div>
          </div>
        )}
      </section>

      <EventCalendar
        year={year}
        month={month}
        onPrevMonth={handlePrevMonth}
        onNextMonth={handleNextMonth}
      />
    </div>
  );
};

export default DashboardPage;