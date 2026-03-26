import { useState, type FormEvent } from "react";
import { authApi } from "../../api/authApi";
import Input from "../common/Input";
import Button from "../common/Button";
import {
  getApiErrorData,
  getApiErrorMessage,
  getApiValidationErrors,
} from "../../utils/apiError";

interface RegisterFormProps {
  onSuccess?: (payload: { message: string; email: string }) => void;
}

interface RegisterFormErrors {
  name?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  common?: string;
}

const RegisterForm = ({ onSuccess }: RegisterFormProps) => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errors, setErrors] = useState<RegisterFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (): boolean => {
    const nextErrors: RegisterFormErrors = {};

    if (!name.trim()) {
      nextErrors.name = "Имя обязательно для заполнения";
    }

    if (!email.trim()) {
      nextErrors.email = "Email обязателен для заполнения";
    }

    if (!password.trim()) {
      nextErrors.password = "Пароль обязателен для заполнения";
    }

    if (!confirmPassword.trim()) {
      nextErrors.confirmPassword = "Подтверждение пароля обязательно для заполнения";
    }

    if (password && confirmPassword && password !== confirmPassword) {
      nextErrors.confirmPassword = "Пароли не совпадают";
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

    try {
      const normalizedEmail = email.trim();

      const response = await authApi.register({
        name: name.trim(),
        email: normalizedEmail,
        password,
        confirmPassword,
      });

      onSuccess?.({
        message: response.message,
        email: normalizedEmail,
      });
    } catch (error: unknown) {
      const data = getApiErrorData(error);
      const validationErrors = getApiValidationErrors(error);

      if (validationErrors) {
        setErrors({
          name:
            typeof validationErrors.name === "string"
              ? validationErrors.name
              : undefined,
          email:
            typeof validationErrors.email === "string"
              ? validationErrors.email
              : undefined,
          password:
            typeof validationErrors.password === "string"
              ? validationErrors.password
              : undefined,
          confirmPassword:
            typeof validationErrors.confirmPassword === "string"
              ? validationErrors.confirmPassword
              : undefined,
          common:
            data?.message &&
            data.message !== "Ошибка валидации входных данных"
              ? data.message
              : undefined,
        });
      } else {
        setErrors({
          common: getApiErrorMessage(error, "Не удалось выполнить регистрацию"),
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="space-y-5" onSubmit={handleSubmit}>
      {errors.common ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errors.common}
        </div>
      ) : null}

      <Input
        label="Имя"
        name="name"
        type="text"
        placeholder="Введите имя"
        autoComplete="name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        error={errors.name}
      />

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
        autoComplete="new-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        error={errors.password}
      />

      <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
        Пароль должен содержать от 8 до 100 символов, включая хотя бы одну цифру,
        одну заглавную и одну строчную букву.
      </div>

      <Input
        label="Подтверждение пароля"
        name="confirmPassword"
        type="password"
        placeholder="Повторите пароль"
        autoComplete="new-password"
        value={confirmPassword}
        onChange={(event) => setConfirmPassword(event.target.value)}
        error={errors.confirmPassword}
      />

      <Button type="submit" isLoading={isSubmitting}>
        Зарегистрироваться
      </Button>
    </form>
  );
};

export default RegisterForm;