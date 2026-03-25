import axiosInstance from "./axios";
import type { CalendarResponse, DashboardSummaryResponse } from "../types/calendar";

export const dashboardApi = {
  async getSummary(): Promise<DashboardSummaryResponse> {
    const { data } = await axiosInstance.get<DashboardSummaryResponse>("/dashboard/summary");
    return data;
  },

  async getCalendar(year: number, month: number): Promise<CalendarResponse> {
    const { data } = await axiosInstance.get<CalendarResponse>("/calendar", {
      params: { year, month },
    });
    return data;
  },
};

export default dashboardApi;