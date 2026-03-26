import { useState, type FormEvent } from "react";
import { authApi } from "../../api/authApi";
import Input from "../common/Input";
import Button from "../common/Button";
import {
  getApiErrorData,
  getApiErrorMessage,
  getApiValidationErrors,
} from "../../utils/apiError";

interface ResendVerificationFormProps {
  initialEmail?: string;
}

interface ResendVerificationFormErrors {
  email?: string;
  common?: string;
}

const ResendVerificationForm = ({
  initialEmail = "",
}: ResendVerificationFormProps) => {
  const [email, setEmail] = useState(initialEmail);
  const [errors, setErrors] = useState<ResendVerificationFormErrors>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const nextErrors: ResendVerificationFormErrors = {};

    if (!email.trim()) {
      nextErrors.email = "Email обязателен для заполнения";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    setErrors({});
    setSuccessMessage(null);

    try {
      const response = await authApi.resendVerificationEmail({
        email: email.trim(),
      });

      setSuccessMessage(response.message);
    } catch (error: unknown) {
      const data = getApiErrorData(error);
      const validationErrors = getApiValidationErrors(error);

      if (validationErrors) {
        setErrors({
          email:
            typeof validationErrors.email === "string"
              ? validationErrors.email
              : undefined,
          common:
            data?.message &&
            data.message !== "Ошибка валидации входных данных"
              ? data.message
              : undefined,
        });
      } else {
        setErrors({
          common: getApiErrorMessage(
            error,
            "Не удалось повторно отправить письмо для подтверждения email"
          ),
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-4">
      {successMessage ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          {successMessage}
        </div>
      ) : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
        {errors.common ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {errors.common}
          </div>
        ) : null}

        <Input
          label="Email"
          name="email"
          type="email"
          placeholder="Введите email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={errors.email}
        />

        <Button type="submit" isLoading={isSubmitting}>
          Отправить письмо повторно
        </Button>
      </form>
    </div>
  );
};

export default ResendVerificationForm;