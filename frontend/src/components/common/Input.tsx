import { forwardRef, type InputHTMLAttributes } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id, className = "", ...props }, ref) => {
    const inputId = id ?? props.name;

    return (
      <div className="w-full">
        <label htmlFor={inputId} className="mb-2 block text-sm font-medium text-slate-700">
          {label}
        </label>

        <input
          ref={ref}
          id={inputId}
          className={[
            "w-full rounded-xl border bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400",
            error
              ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100"
              : "border-slate-300 focus:border-slate-900 focus:ring-4 focus:ring-slate-100",
            className,
          ].join(" ")}
          {...props}
        />

        {error ? <p className="mt-2 text-sm text-red-600">{error}</p> : null}
      </div>
    );
  }
);

Input.displayName = "Input";

export default Input;