import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ReservationFilter } from "@/components/waiter/ReservationFilter";


vi.mock("@/components/waiter/ReservationModal", () => ({
  ReservationModal: ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) =>
    isOpen ? (
      <div data-testid="modal">
        <button onClick={onClose}>Close Modal</button>
      </div>
    ) : null,
}));

describe("ReservationFilter", () => {
  const mockOnSearch = vi.fn();

  it("renders inputs and buttons correctly", () => {
    render(<ReservationFilter onSearch={mockOnSearch} />);
    expect(screen.getByText("+ Create New Reservation")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /search/i })).toBeInTheDocument();
    expect(screen.getAllByTestId("timeSlot")[0]).toBeInTheDocument(); // time select
    expect(screen.getAllByRole("combobox")[1]).toBeInTheDocument(); // table select
  });

  it("calls onSearch with correct values", () => {
    render(<ReservationFilter onSearch={mockOnSearch} />);
    
    const dateInput = screen.getByTestId("datePicker");
    const timeSelect = screen.getAllByRole("combobox")[0];
    const tableSelect = screen.getAllByRole("combobox")[1];
    const searchButton = screen.getByRole("button", { name: /search/i });

    fireEvent.change(dateInput, { target: { value: "2024-01-01" } });
    fireEvent.change(timeSelect, { target: { value: "12:15" } });
    fireEvent.change(tableSelect, { target: { value: "T5" } });
    fireEvent.click(searchButton);

    expect(mockOnSearch).toHaveBeenCalledWith("2024-01-01", "12:15", "T5");
  });

  it("opens and closes the modal", () => {
    render(<ReservationFilter onSearch={mockOnSearch} />);

    fireEvent.click(screen.getByText("+ Create New Reservation"));
    expect(screen.getByTestId("modal")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Close Modal"));
    expect(screen.queryByTestId("modal")).not.toBeInTheDocument();
  });
});
