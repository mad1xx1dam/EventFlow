import { useState, type FormEvent } from "react";
import pollsApi from "../../api/pollsApi";
import Button from "../common/Button";
import Input from "../common/Input";
import type { PollResponse } from "../../types/poll";
import {
  getApiErrorData,
  getApiErrorMessage,
  getApiValidationErrors,
} from "../../utils/apiError";

interface PollCreateFormErrors {
  question?: string;
  options?: string;
  common?: string;
}

interface PollCreateFormProps {
  eventId: number;
  onCreated: (poll: PollResponse) => void;
}

const PollCreateForm = ({ eventId, onCreated }: PollCreateFormProps) => {
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState(["", ""]);
  const [errors, setErrors] = useState<PollCreateFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const updateOption = (index: number, value: string) => {
    setOptions((prev) => prev.map((option, i) => (i === index ? value : option)));
  };

  const addOption = () => {
    setOptions((prev) => [...prev, ""]);
  };

  const removeOption = (index: number) => {
    setOptions((prev) => {
      if (prev.length <= 2) {
        return prev;
      }

      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedQuestion = question.trim();
    const normalizedOptions = options.map((option) => option.trim()).filter(Boolean);

    const nextErrors: PollCreateFormErrors = {};

    if (!normalizedQuestion) {
      nextErrors.question = "Вопрос обязателен для заполнения";
    }

    if (normalizedOptions.length < 2) {
      nextErrors.options = "Нужно указать минимум два варианта ответа";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    setIsSubmitting(true);
    setErrors({});

    try {
      const poll = await pollsApi.createPoll(eventId, {
        question: normalizedQuestion,
        options: normalizedOptions,
      });

      setQuestion("");
      setOptions(["", ""]);
      setErrors({});
      onCreated(poll);
    } catch (error: unknown) {
      const data = getApiErrorData(error);
      const validationErrors = getApiValidationErrors(error);

      if (validationErrors) {
        setErrors({
          question:
            typeof validationErrors.question === "string"
              ? validationErrors.question
              : undefined,
          options:
            typeof validationErrors.options === "string"
              ? validationErrors.options
              : undefined,
          common:
            data?.message &&
            data.message !== "Ошибка валидации входных данных"
              ? data.message
              : undefined,
        });
      } else {
        setErrors({
          common: getApiErrorMessage(error, "Не удалось создать опрос"),
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {errors.common ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errors.common}
        </div>
      ) : null}

      <Input
        label="Вопрос"
        name="pollQuestion"
        type="text"
        placeholder="Введите вопрос для опроса"
        value={question}
        onChange={(event) => setQuestion(event.target.value)}
        error={errors.question}
      />

      <div className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-medium text-slate-700">Варианты ответа</p>
          <button
            type="button"
            onClick={addOption}
            className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Добавить вариант
          </button>
        </div>

        {errors.options ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {errors.options}
          </div>
        ) : null}

        {options.map((option, index) => (
          <div key={`option-${index}`} className="flex items-end gap-3">
            <div className="flex-1">
              <Input
                label={`Вариант ${index + 1}`}
                name={`option-${index}`}
                type="text"
                placeholder="Введите вариант ответа"
                value={option}
                onChange={(event) => updateOption(index, event.target.value)}
              />
            </div>

            <button
              type="button"
              onClick={() => removeOption(index)}
              disabled={options.length <= 2}
              className="rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Удалить
            </button>
          </div>
        ))}
      </div>

      <Button type="submit" isLoading={isSubmitting}>
        Создать опрос
      </Button>
    </form>
  );
};

export default PollCreateForm;