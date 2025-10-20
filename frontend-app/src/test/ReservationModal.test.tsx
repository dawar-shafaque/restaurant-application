import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ReservationModal from "@/components/ReservationModal";
import { describe, test, vi, expect, Mock } from "vitest";
import { toast } from "react-toastify";

vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("ReservationModal", () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    location: "Test Location",
    date: "2023-10-10",
    guests: 2,
    setGuests: vi.fn(),
    timeSlot: "12:00 PM - 2:00 PM",
    tableNumber: "5",
    locationId: "123",
    tableCap: "4",
  };

  test("renders modal correctly when open", () => {
    render(<ReservationModal {...defaultProps} />);

    expect(screen.getByRole("button",{name: "Make a Reservation"})).toBeInTheDocument();
    expect(screen.getByText(/Test Location/i)).toBeInTheDocument();
    expect(screen.getByText(/2023-10-10/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Guests/i)[0]).toBeInTheDocument();
    expect(screen.getByText(/Time/i)).toBeInTheDocument();
    expect(screen.getByRole("button",{name: "Make a Reservation"})).toBeInTheDocument();
  });

  test("does not render modal when closed", () => {
    render(<ReservationModal {...defaultProps} isOpen={false} />);

    expect(screen.queryByText("Make a Reservation")).not.toBeInTheDocument();
  });

  test("calls onClose when close button is clicked", () => {
    render(<ReservationModal {...defaultProps} />);

    fireEvent.click(screen.getByTestId("closeButton"));
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  test("increments and decrements guest count correctly", () => {
    render(<ReservationModal {...defaultProps} />);

    const decrementButton = screen.getByTestId("decrement");
    const incrementButton = screen.getByTestId("increment");
    // const guestCount = screen.getByTestId("guestCount");

    fireEvent.click(incrementButton);
    expect(defaultProps.setGuests).toHaveBeenCalledWith(3);

    fireEvent.click(decrementButton);
    expect(defaultProps.setGuests).toHaveBeenCalledWith(1);
  });

  test("handles reservation submission successfully", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      })
    ) as Mock;

    render(<ReservationModal {...defaultProps} />);

    fireEvent.click(screen.getByRole("button",{name: "Make a Reservation"}));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.any(Object)
      );
      expect(toast.success).toHaveBeenCalledWith(
        "Reservation made successfully!"
      );
      expect(defaultProps.onClose).toHaveBeenCalled();
    });
  });

  test("disables reservation button and shows warning text when guests exceed table capacity", () => {
  const updatedProps = {
    ...defaultProps,
    guests: 6, // exceed table capacity
    tableCap: "5", // table capacity set to 5
  };

  render(<ReservationModal {...updatedProps} />);

  const reservationButton = screen.getByRole("button", { name: /make a reservation/i });

  // Expect the button to be disabled
  expect(reservationButton).toBeDisabled();

  // Expect the warning text to be visible
  expect(
    screen.getByText(/number of guests exceeds the table capacity/i)
  ).toBeInTheDocument();
});

  test("handles reservation failure", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: false,
        json: () =>
          Promise.resolve({ message: "Failed to make a reservation" }),
      })
    ) as Mock;

    render(<ReservationModal {...defaultProps} />);

    fireEvent.click(screen.getByRole("button", { name: "Make a Reservation" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Failed to make a reservation");
      expect(defaultProps.onClose).toHaveBeenCalled();
    });
  });

  test("handles network errors gracefully", async () => {
    global.fetch = vi.fn(() =>
      Promise.reject(new Error("Network Error"))
    ) as Mock;
    const consoleErrorMock = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    render(<ReservationModal {...defaultProps} />);

    fireEvent.click(screen.getByRole("button", { name: "Make a Reservation" }));

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(expect.any(Error));
    });

    consoleErrorMock.mockRestore();
  });
});
