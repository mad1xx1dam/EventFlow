import type { InvitationResponse } from "../../types/invitation";

interface GuestListRowProps {
  invitation: InvitationResponse;
}

const statusLabelMap: Record<InvitationResponse["rsvpStatus"], string> = {
  PENDING: "Ожидает",
  GOING: "Идёт",
  MAYBE: "Возможно",
  DECLINED: "Отказался",
};

const statusClassMap: Record<InvitationResponse["rsvpStatus"], string> = {
  PENDING: "bg-slate-100 text-slate-700",
  GOING: "bg-emerald-100 text-emerald-800",
  MAYBE: "bg-amber-100 text-amber-800",
  DECLINED: "bg-red-100 text-red-800",
};

const GuestListRow = ({ invitation }: GuestListRowProps) => {
  return (
    <tr className="border-t border-slate-200">
      <td className="px-4 py-3 text-sm text-slate-900">{invitation.guestEmail}</td>
      <td className="px-4 py-3 text-sm text-slate-600">
        <span
          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusClassMap[invitation.rsvpStatus]}`}
        >
          {statusLabelMap[invitation.rsvpStatus]}
        </span>
      </td>
    </tr>
  );
};

export default GuestListRow;