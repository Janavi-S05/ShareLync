export function useScheduleActions(user) {

  async function fetchScheduleForFile(filename) {
    if (!user?.username) return null;
    try {
      const resp = await fetch(`/api/schedule/by/${user.username}`, {
        headers: { Authorization: `Bearer ${user.token}` },
      });
      if (!resp.ok) return null;
      const data = await resp.json();
      const schedules = Array.isArray(data.data) ? data.data : [];
      return schedules.find((s) => s.filename === filename) || null;
    } catch {
      return null;
    }
  }

  async function fetchAllSchedules() {
    if (!user?.username) return new Map();
    try {
      const resp = await fetch(`/api/schedule/by/${user.username}`, {
        headers: { Authorization: `Bearer ${user.token}` },
      });
      if (!resp.ok) return new Map();
      const data = await resp.json();
      const schedules = Array.isArray(data.data) ? data.data : [];
      const map = new Map();
      schedules.forEach((s) => map.set(s.filename, s));
      return map;
    } catch {
      return new Map();
    }
  }

  return { fetchScheduleForFile, fetchAllSchedules };
}