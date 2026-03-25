import { useState } from "react";
import { Link } from "react-router-dom";
import RegisterForm from "../../components/auth/RegisterForm";

const RegisterPage = () => {
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  return (
    <div>
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900">Регистрация</h1>
        <p className="mt-2 text-sm text-slate-500">Создайте аккаунт для работы с EventFlow</p>
      </div>

      {successMessage ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
          <p className="font-semibold">Регистрация прошла успешно</p>
          <p className="mt-2">{successMessage}</p>
          <p className="mt-2">Проверьте почту и подтвердите email, чтобы завершить регистрацию.</p>
        </div>
      ) : (
        <RegisterForm onSuccess={setSuccessMessage} />
      )}

      <p className="mt-6 text-center text-sm text-slate-600">
        Уже есть аккаунт?{" "}
        <Link to="/login" className="font-semibold text-slate-900 underline-offset-4 hover:underline">
          Войти
        </Link>
      </p>
    </div>
  );
};

export default RegisterPage;