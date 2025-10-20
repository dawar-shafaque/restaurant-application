import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, Mock, vi } from "vitest";
import ReservationDashboard from "@/pages/waiter/Reservations";
import { API } from "@/api/endpoints";

vi.mock("@/api/endpoints", () => ({
  API: {
    RESERVATIONS: `${import.meta.env.VITE_BASE_URL2}${import.meta.env.VITE_BOOKING_WAITER}`,
  },
}));

vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock("@/components/Navbar", () => ({
  Navbar: () => <div data-testid="navbar">Navbar</div>,
}));

vi.mock("@/components/waiter/ReservationFilter", () => ({
  ReservationFilter: ({ onSearch }: { onSearch: (date: string, time: string, guests: string) => void }) => (
    <button
      data-testid="search-button"
      onClick={() => onSearch("2023-01-01", "12:00", "1")}
    >
      Search
    </button>
  ),
}));

vi.mock("@/components/waiter/ReservationCard", () => ({
  ReservationCard: ({
    reservation,
    onCancel,
    onPostpone,
  }: {
    reservation: { reservationId: string; date: string; timeSlot: string; guestsNumber: string };
    onCancel: (reservationId: string) => void;
    onPostpone: (reservationId: string) => void;
  }) => (
    <div data-testid="reservation-card">
      <p>{reservation.reservationId}</p>
      <button onClick={() => onCancel(reservation.reservationId)}>Cancel</button>
      <button onClick={() => onPostpone(reservation.reservationId)}>Postpone</button>
    </div>
  ),
}));

describe("ReservationDashboard", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  const mockReservations = [
    {
      reservationId: "1",
      date: "2023-01-01",
      timeSlot: "12:00 - 13:00",
      guestsNumber: "4 guests",
    },
  ];

  it("renders the Navbar and ReservationFilter components", () => {
    render(<ReservationDashboard />);
    expect(screen.getByTestId("navbar")).toBeInTheDocument();
    expect(screen.getByTestId("search-button")).toBeInTheDocument();
  });

  it("fetches and displays reservations on search", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockReservations),
      })
    ) as Mock;

    render(<ReservationDashboard />);
    fireEvent.click(screen.getByTestId("search-button"));

    await waitFor(() => {
      expect(screen.getByTestId("reservation-card")).toBeInTheDocument();
    });
  });

  it("handles reservation cancellation", async () => {
    global.fetch = vi.fn(( options) => {
      if (options?.method === "DELETE") {
        return Promise.resolve({ ok: true, text: () => Promise.resolve("Reservation canceled") });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve(mockReservations) });
    }) as Mock;

    render(<ReservationDashboard />);
    fireEvent.click(screen.getByTestId("search-button"));

    await waitFor(() => {
      expect(screen.getByTestId("reservation-card")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Cancel"));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        `${API.RESERVATIONS}/1`,
        expect.objectContaining({ method: "DELETE" })
      );
    });
  });

  it("handles reservation postponement", async () => {
    render(<ReservationDashboard />);
    fireEvent.click(screen.getByTestId("search-button"));

    await waitFor(() => {
      expect(screen.getByTestId("reservation-card")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Postpone"));

    // These elements must be in your actual component for this to pass
    expect(screen.getByLabelText("Time From")).toHaveValue("12:00");
    expect(screen.getByLabelText("Time To")).toHaveValue("13:00");
    expect(screen.getByLabelText("Number of Guests")).toHaveValue(4);
  });

  it("submits the edited reservation", async () => {
    global.fetch = vi.fn(( options) => {
      if (options?.method === "PATCH") {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ message: "Reservation updated" }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockReservations),
      });
    }) as Mock;

    render(<ReservationDashboard />);
    fireEvent.click(screen.getByTestId("search-button"));

    await waitFor(() => {
      expect(screen.getByTestId("reservation-card")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Postpone"));

    fireEvent.change(screen.getByLabelText("Time From"), {
      target: { value: "14:00" },
    });
    fireEvent.change(screen.getByLabelText("Time To"), {
      target: { value: "15:00" },
    });
    fireEvent.change(screen.getByLabelText("Number of Guests"), {
      target: { value: "5" },
    });

    fireEvent.click(screen.getByText("Confirm Postpone"));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        `${API.RESERVATIONS}/1`,
        expect.objectContaining({
          method: "PATCH",
          body: JSON.stringify({
            timeFrom: "14:00",
            timeTo: "15:00",
            guestsNumber: "5",
          }),
        })
      );
    });
  });
});
