import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ReservationsPage from "../pages/CustomerReservation";
import { API } from "@/api/endpoints";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/api/endpoints", () => ({
  API: {
    RESERVATIONS: "/api/reservations",
    DELETE_RESERVATION: "/api/reservations/delete",
    FEEDBACKS_POST: "/api/feedbacks",
  },
}));

vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock("@/components/Navbar", () => ({
  Navbar: () => <div data-testid="navbar">Navbar</div>,
}));

vi.mock("@/components/ReservationCard", () => ({
  default: ({ onEdit, onCancel, onFeedback }: { onEdit: () => void; onCancel: () => void; onFeedback: () => void }) => (
    <div>
      <button onClick={onEdit} data-testid="edit-button">
        Edit
      </button>
      <button onClick={onCancel} data-testid="cancel-button">
        Cancel
      </button>
      <button onClick={onFeedback} data-testid="feedback-button">
        Feedback
      </button>
    </div>
  ),
}));

vi.mock("@/components/Feedback", () => ({
  default: ({ open, onClose, onSubmit }: { open: boolean; onClose: () => void; onSubmit: (feedback: { comment: string }) => void }) =>
    open ? (
      <div>
        <button onClick={onClose} data-testid="close-feedback">
          Close Feedback
        </button>
        <button onClick={() => onSubmit({ comment: "Test feedback" })} data-testid="submit-feedback">
          Submit Feedback
        </button>
      </div>
    ) : null,
}));

describe("ReservationsPage", () => {
  beforeEach(() => {
    vi.spyOn(global, "fetch").mockImplementation((url) => {
      if (url === API.RESERVATIONS) {
        return Promise.resolve(new Response(JSON.stringify([
          {
            id: "1",
            date: "2023-10-10",
            timeFrom: "12:00",
            timeTo: "14:00",
            guestsNumber: 4,
            locationId: "123",
          },
        ]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }));
      }
      return Promise.resolve(new Response(JSON.stringify({}), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }));
    });
    sessionStorage.setItem("token", "test-token");
    sessionStorage.setItem("username", "test-user");
    sessionStorage.setItem("role", "admin");
  });

  afterEach(() => {
    vi.restoreAllMocks();
    sessionStorage.clear();
  });

  it("renders the navbar and reservations", async () => {
    render(<ReservationsPage />);

    expect(screen.getByTestId("navbar")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("Hello, test-user (admin)")).toBeInTheDocument());
    expect(screen.getByTestId("edit-button")).toBeInTheDocument();
  });

  it("handles reservation edit", async () => {
    render(<ReservationsPage />);

    await waitFor(() => screen.getByTestId("edit-button"));
    fireEvent.click(screen.getByTestId("edit-button"));

    const timeFromInput = screen.getByLabelText("Time From");
    fireEvent.change(timeFromInput, { target: { value: "13:00" } });

    const saveButton = screen.getByText("Save Changes");
    fireEvent.click(saveButton);

    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining("/api/reservations/1"), expect.any(Object)));
  });

  it("handles reservation cancellation", async () => {
    render(<ReservationsPage />);

    await waitFor(() => screen.getByTestId("cancel-button"));
    fireEvent.click(screen.getByTestId("cancel-button"));

    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining("/api/reservations/delete/1"), expect.any(Object)));
  });

  it("handles feedback submission", async () => {
    render(<ReservationsPage />);

    await waitFor(() => screen.getByTestId("feedback-button"));
    fireEvent.click(screen.getByTestId("feedback-button"));

    await waitFor(() => screen.getByTestId("submit-feedback"));
    fireEvent.click(screen.getByTestId("submit-feedback"));

    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining("/api/feedbacks"), expect.any(Object)));
  });
});