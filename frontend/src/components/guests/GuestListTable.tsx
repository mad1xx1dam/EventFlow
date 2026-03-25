import type { InvitationResponse } from "../../types/invitation";
import GuestListRow from "./GuestListRow";

interface GuestListTableProps {
  invitations: InvitationResponse[];
}

const GuestListTable = ({ invitations }: GuestListTableProps) => {
  if (invitations.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
        Пока нет приглашенных гостей.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200">
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Email
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                Статус
              </th>
            </tr>
          </thead>
          <tbody>
            {invitations.map((invitation) => (
              <GuestListRow key={invitation.id} invitation={invitation} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GuestListTable;