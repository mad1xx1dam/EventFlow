import { useState } from "react";
import { Link } from "react-router-dom";
import RegisterForm from "../../components/auth/RegisterForm";
import ResendVerificationForm from "../../components/auth/ResendVerificationForm";

interface RegisterSuccessPayload {
  message: string;
  email: string;
}

const RegisterPage = () => {
  const [successData, setSuccessData] = useState<RegisterSuccessPayload | null>(null);

  return (
    <div>
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900">Регистрация</h1>
        <p className="mt-2 text-sm text-slate-500">
          Создайте аккаунт для работы с EventFlow
        </p>
      </div>

      {successData ? (
        <div className="space-y-4">
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
            <p className="font-semibold">Регистрация прошла успешно</p>
            <p className="mt-2">{successData.message}</p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white px-5 py-5">
            <h2 className="text-base font-semibold text-slate-900">
              Не получили письмо?
            </h2>
            <p className="mt-2 text-sm text-slate-600">
              Повторно отправьте письмо для подтверждения на указанный email.
            </p>

            <div className="mt-4">
              <ResendVerificationForm initialEmail={successData.email} />
            </div>
          </div>
        </div>
      ) : (
        <RegisterForm onSuccess={setSuccessData} />
      )}

      <p className="mt-6 text-center text-sm text-slate-600">
        Уже есть аккаунт?{" "}
        <Link
          to="/login"
          className="font-semibold text-slate-900 underline-offset-4 hover:underline"
        >
          Войти
        </Link>
      </p>
    </div>
  );
};

export default RegisterPage;