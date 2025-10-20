import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, vi, expect } from "vitest";
import { ReservationCard } from "@/components/waiter/ReservationCard";
import { WaiterReservation } from "@/types/FormData";

describe("ReservationCard", () => {
  const mockReservation: WaiterReservation = {
      reservationId: "1",
      location: "Downtown",
      tableNumber: "5",
      date: new Date("2023-01-01").toISOString(),
      timeSlot: "12:00 - 13:00",
      customerName: "John Doe",
      guestsNumber: 4,
      timeFrom: "",
      timeTo: ""
  };

  const mockOnCancel = vi.fn();
  const mockOnPostpone = vi.fn();

  it("renders reservation details correctly", () => {
    render(
      <ReservationCard
        reservation={mockReservation}
        onCancel={mockOnCancel}
        onPostpone={mockOnPostpone}
      />
    );

    expect(screen.getByText("Downtown")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByText("2023-01-01")).toBeInTheDocument();
    expect(screen.getByText("12:00 - 13:00")).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("calls onCancel when the Cancel button is clicked", () => {
    render(
      <ReservationCard
        reservation={mockReservation}
        onCancel={mockOnCancel}
        onPostpone={mockOnPostpone}
      />
    );

    fireEvent.click(screen.getByText("Cancel"));
    expect(mockOnCancel).toHaveBeenCalledWith("1");
  });

  it("calls onPostpone when the Postpone button is clicked", () => {
    render(
      <ReservationCard
        reservation={mockReservation}
        onCancel={mockOnCancel}
        onPostpone={mockOnPostpone}
      />
    );

    fireEvent.click(screen.getByText("Postpone"));
    expect(mockOnPostpone).toHaveBeenCalled();
  });
});