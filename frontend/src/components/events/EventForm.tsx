import { useMemo, useState, type FormEvent } from "react";
import type { EventRequest, EventResponse } from "../../types/event";
import Input from "../common/Input";
import Button from "../common/Button";
import EventMapPicker from "../map/EventMapPicker";
import LocationSearchInput from "../map/LocationSearchInput";
import EventPosterUploader from "./EventPosterUploader";
import { getApiErrorData } from "../../utils/apiError";

interface EventFormState {
  title: string;
  description: string;
  startsAt: string;
  address: string;
  lat: number | null;
  lon: number | null;
  poster: File | null;
}

interface EventFormErrors {
  title?: string;
  startsAt?: string;
  address?: string;
  common?: string;
}

interface EventFormSubmitPayload {
  event: EventRequest;
  poster: File | null;
}

interface EventFormProps {
  mode: "create" | "edit";
  initialEvent?: EventResponse | null;
  initialStartsAt?: string;
  isSubmitting?: boolean;
  submitLabel: string;
  onSubmit: (payload: EventFormSubmitPayload) => Promise<void>;
}

const toDateTimeLocalValue = (value: string) => {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const timezoneOffset = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - timezoneOffset * 60_000);

  return localDate.toISOString().slice(0, 16);
};

const buildInitialState = (
  initialEvent?: EventResponse | null,
  initialStartsAt?: string
): EventFormState => ({
  title: initialEvent?.title ?? "",
  description: initialEvent?.description ?? "",
  startsAt: initialEvent?.startsAt
    ? toDateTimeLocalValue(initialEvent.startsAt)
    : initialStartsAt ?? "",
  address: initialEvent?.address ?? "",
  lat: initialEvent?.lat ?? null,
  lon: initialEvent?.lon ?? null,
  poster: null,
});

const EventForm = ({
  mode,
  initialEvent,
  initialStartsAt,
  isSubmitting = false,
  submitLabel,
  onSubmit,
}: EventFormProps) => {
  const initialState = useMemo(
    () => buildInitialState(initialEvent, initialStartsAt),
    [initialEvent, initialStartsAt]
  );

  const [form, setForm] = useState<EventFormState>(initialState);
  const [errors, setErrors] = useState<EventFormErrors>({});

  const validate = () => {
    const nextErrors: EventFormErrors = {};

    if (!form.title.trim()) {
      nextErrors.title = "Название обязательно для заполнения";
    }

    if (!form.startsAt) {
      nextErrors.startsAt = "Дата и время обязательны для заполнения";
    }

    if (!form.address.trim()) {
      nextErrors.address = "Адрес обязателен для заполнения";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setErrors({});

    try {
      await onSubmit({
        event: {
          title: form.title.trim(),
          description: form.description.trim() || null,
          startsAt: new Date(form.startsAt).toISOString(),
          address: form.address.trim(),
          lat: form.lat,
          lon: form.lon,
        },
        poster: form.poster,
      });
    } catch (error: unknown) {
      const data = getApiErrorData(error);
      const fallbackMessage =
        mode === "create"
          ? "Не удалось создать мероприятие"
          : "Не удалось обновить мероприятие";

      if (data?.errors) {
        setErrors({
          title: typeof data.errors.title === "string" ? data.errors.title : undefined,
          startsAt:
            typeof data.errors.startsAt === "string" ? data.errors.startsAt : undefined,
          address: typeof data.errors.address === "string" ? data.errors.address : undefined,
          common:
            data.message && data.message !== "Ошибка валидации входных данных"
              ? data.message
              : undefined,
        });
      } else {
        setErrors({
          common:
            typeof data?.message === "string" && data.message.trim()
              ? data.message
              : fallbackMessage,
        });
      }
    }
  };

  return (
    <form
      key={initialEvent?.id ?? `create-${initialStartsAt ?? "empty"}`}
      onSubmit={handleSubmit}
      className="space-y-6"
    >
      {errors.common ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errors.common}
        </div>
      ) : null}

      <div className="grid gap-6">
        <Input
          label="Название"
          name="title"
          type="text"
          placeholder="Введите название мероприятия"
          value={form.title}
          onChange={(event) =>
            setForm((prev) => ({
              ...prev,
              title: event.target.value,
            }))
          }
          error={errors.title}
        />

        <div>
          <label
            htmlFor="description"
            className="mb-2 block text-sm font-medium text-slate-700"
          >
            Описание
          </label>
          <textarea
            id="description"
            rows={5}
            value={form.description}
            onChange={(event) =>
              setForm((prev) => ({
                ...prev,
                description: event.target.value,
              }))
            }
            placeholder="Опишите мероприятие"
            className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-900 focus:ring-4 focus:ring-slate-100"
          />
        </div>

        <Input
          label="Дата и время начала"
          name="startsAt"
          type="datetime-local"
          value={form.startsAt}
          onChange={(event) =>
            setForm((prev) => ({
              ...prev,
              startsAt: event.target.value,
            }))
          }
          error={errors.startsAt}
        />

        <LocationSearchInput
          value={form.address}
          onChange={(value) =>
            setForm((prev) => ({
              ...prev,
              address: value,
            }))
          }
          error={errors.address}
        />

        <EventMapPicker
          lat={form.lat}
          lon={form.lon}
          onLocationChange={({ lat, lon, address }) => {
            setForm((prev) => ({
              ...prev,
              lat,
              lon,
              address,
            }));
          }}
        />

        <EventPosterUploader
          file={form.poster}
          posterUrl={initialEvent?.posterUrl ?? null}
          onChange={(poster) =>
            setForm((prev) => ({
              ...prev,
              poster,
            }))
          }
        />
      </div>

      <Button type="submit" isLoading={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
};

export default EventForm;