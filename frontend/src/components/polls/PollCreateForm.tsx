import { useState, type FormEvent } from "react";
import pollsApi from "../../api/pollsApi";
import Button from "../common/Button";
import Input from "../common/Input";
import type { PollResponse } from "../../types/poll";
import { getApiErrorMessage } from "../../utils/apiError";

interface PollCreateFormProps {
  eventId: number;
  onCreated: (poll: PollResponse) => void;
}

const PollCreateForm = ({ eventId, onCreated }: PollCreateFormProps) => {
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState(["", ""]);
  const [error, setError] = useState<string | null>(null);
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

    if (!normalizedQuestion) {
      setError("Вопрос обязателен для заполнения");
      return;
    }

    if (normalizedOptions.length < 2) {
      setError("Нужно указать минимум два варианта ответа");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const poll = await pollsApi.createPoll(eventId, {
        question: normalizedQuestion,
        options: normalizedOptions,
      });

      setQuestion("");
      setOptions(["", ""]);
      onCreated(poll);
    } catch (error: unknown) {
      setError(getApiErrorMessage(error, "Не удалось создать опрос"));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      <Input
        label="Вопрос"
        name="pollQuestion"
        type="text"
        placeholder="Введите вопрос для опроса"
        value={question}
        onChange={(event) => setQuestion(event.target.value)}
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