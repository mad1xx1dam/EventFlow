import { useState, type FormEvent } from "react";
import Input from "../common/Input";
import Button from "../common/Button";
import useAuth from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";

interface LoginFormProps {
  onSuccess?: () => void;
}

interface LoginFormErrors {
  email?: string;
  password?: string;
  common?: string;
}

const LoginForm = ({ onSuccess }: LoginFormProps) => {
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<LoginFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    setErrors({});

    try {
      await login({
        email: email.trim(),
        password,
      });

      onSuccess?.();
    } catch (error: unknown) {
      setErrors({
        common: getApiErrorMessage(error, "Не удалось выполнить вход"),
      });
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
  );
};

export default LoginForm;