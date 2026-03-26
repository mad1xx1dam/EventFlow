import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { authApi } from "../../api/authApi";
import { getApiErrorMessage } from "../../utils/apiError";
import ResendVerificationForm from "../../components/auth/ResendVerificationForm";

type VerifyStatus = "loading" | "success" | "error";

const VerifyEmailPage = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<VerifyStatus>("loading");
  const [message, setMessage] = useState("Подтверждаем email...");
  const [email, setEmail] = useState("");
  const hasVerifiedRef = useRef(false);

  useEffect(() => {
    if (hasVerifiedRef.current) {
      return;
    }

    hasVerifiedRef.current = true;

    const token = searchParams.get("token");
    const emailParam = searchParams.get("email") ?? "";

    if (emailParam) {
      setEmail(emailParam);
    }

    const verify = async () => {
      if (!token) {
        setStatus("error");
        setMessage("Токен подтверждения отсутствует");
        return;
      }

      try {
        const response = await authApi.verifyEmail(token);
        setStatus("success");
        setMessage(response.message);
      } catch (error: unknown) {
        setStatus("error");
        setMessage(getApiErrorMessage(error, "Не удалось подтвердить email"));
      }
    };

    void verify();
  }, [searchParams]);

  return (
    <div className="text-center">
      <h1 className="text-2xl font-bold text-slate-900">Подтверждение email</h1>

      <div
        className={[
          "mt-6 rounded-2xl px-5 py-4 text-sm",
          status === "loading"
            ? "border border-slate-200 bg-slate-50 text-slate-700"
            : status === "success"
              ? "border border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border border-red-200 bg-red-50 text-red-700",
        ].join(" ")}
      >
        {message}
      </div>

      {status === "error" ? (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 text-left">
          <h2 className="text-base font-semibold text-slate-900">
            Отправить письмо повторно
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Укажите email, на который нужно повторно отправить письмо для подтверждения.
          </p>

          <div className="mt-4">
            <ResendVerificationForm initialEmail={email} />
          </div>
        </div>
      ) : null}

      <Link
        to="/login"
        className="mt-6 inline-flex rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
      >
        Перейти ко входу
      </Link>
    </div>
  );
};

export default VerifyEmailPage;