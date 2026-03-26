import { useMemo, useState, type FormEvent } from "react";
import Input from "../common/Input";
import Button from "../common/Button";
import ResendVerificationForm from "./ResendVerificationForm";
import useAuth from "../../hooks/useAuth";
import {
  getApiErrorData,
  getApiErrorMessage,
  getApiValidationErrors,
} from "../../utils/apiError";

interface LoginFormProps {
  onSuccess?: () => void;
}

interface LoginFormErrors {
  email?: string;
  password?: string;
  common?: string;
}

const EMAIL_NOT_VERIFIED_MESSAGES = [
  "Аккаунт не активирован",
  "email ещё не подтверждён",
  "email еще не подтвержден",
];

const LoginForm = ({ onSuccess }: LoginFormProps) => {
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<LoginFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showResendBlock, setShowResendBlock] = useState(false);

  const normalizedEmail = useMemo(() => email.trim().toLowerCase(), [email]);

  const validate = (): boolean => {
    const nextErrors: LoginFormErrors = {};

    if (!email.trim()) {
      nextErrors.email = "Email обязателен для заполнения";
    }

    if (!password.trim()) {
      nextErrors.password = "Пароль обязателен для заполнения";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const shouldShowResend = (message?: string) => {
    if (!message) {
      return false;
    }

    const normalizedMessage = message.toLowerCase();

    return EMAIL_NOT_VERIFIED_MESSAGES.some((item) =>
      normalizedMessage.includes(item.toLowerCase())
    );
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    setErrors({});
    setShowResendBlock(false);

    try {
      await login({
        email: normalizedEmail,
        password,
      });

      onSuccess?.();
    } catch (error: unknown) {
      const data = getApiErrorData(error);
      const validationErrors = getApiValidationErrors(error);

      if (validationErrors) {
        const commonMessage =
          data?.message && data.message !== "Ошибка валидации входных данных"
            ? data.message
            : undefined;

        setErrors({
          email:
            typeof validationErrors.email === "string"
              ? validationErrors.email
              : undefined,
          password:
            typeof validationErrors.password === "string"
              ? validationErrors.password
              : undefined,
          common: commonMessage,
        });

        setShowResendBlock(shouldShowResend(commonMessage));
      } else {
        const commonMessage = getApiErrorMessage(error, "Не удалось выполнить вход");

        setErrors({
          common: commonMessage,
        });

        setShowResendBlock(shouldShowResend(commonMessage));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      <form className="space-y-5" onSubmit={handleSubmit}>
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

        <Input
          label="Пароль"
          name="password"
          type="password"
          placeholder="Введите пароль"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={errors.password}
        />

        <Button type="submit" isLoading={isSubmitting}>
          Войти
        </Button>
      </form>

      {showResendBlock ? (
        <div className="rounded-2xl border border-slate-200 bg-white px-5 py-5">
          <h2 className="text-base font-semibold text-slate-900">
            Email ещё не подтверждён
          </h2>

          <p className="mt-2 text-sm text-slate-600">
            Проверьте почту или отправьте письмо для подтверждения повторно.
          </p>

          <div className="mt-4">
            <ResendVerificationForm initialEmail={normalizedEmail} />
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default LoginForm;