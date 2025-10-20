import { render, screen, fireEvent } from "@testing-library/react";
import { Provider } from "react-redux";
import configureStore from "redux-mock-store";
import ReservationCard from "../components/ReservationCard";
import { Reservation } from "../types/FormData";
import { describe, expect, it, vi } from "vitest";

const mockStore = configureStore([]);

describe("ReservationCard", () => {
  const mockReservation: Reservation = {
    id: "1",
    date: "2023-10-01",
    timeFrom: "18:00",
    timeTo: "20:00",
    guestsNumber: 4,
    status: "RESERVED",
    location: "",
    locationAddress: "",
    locationId: "",
    waiterEmail: "",
    reservationId: "",
  };

  const mockLocation = [
    {
      id: "location-1",
      address: "123 Main St",
    },
  ];

  const renderWithStore = (reservation: Reservation, locationId?: string) => {
    const store = mockStore({
      locations: { locations: mockLocation },
    });

    return render(
      <Provider store={store}>
        <ReservationCard
          reservation={reservation}
          locationId={locationId}
          onEdit={vi.fn()}
          onCancel={vi.fn()}
          onFeedback={vi.fn()}
        />
      </Provider>
    );
  };

  it("renders reservation details correctly", () => {
    renderWithStore(mockReservation, "location-1");

    expect(screen.getByText("123 Main St")).toBeInTheDocument();
    expect(screen.getByTestId("date")).toHaveTextContent("Date: 2023-10-01");
    expect(screen.getByTestId("time")).toHaveTextContent("Time: 18:00 - 20:00");
    expect(screen.getByTestId("guestCount")).toHaveTextContent("Guests: 4");
    expect(screen.getByText("RESERVED")).toBeInTheDocument();
  });

  it("renders Cancel and Edit buttons for RESERVED status", () => {
    renderWithStore(mockReservation, "location-1");

    expect(screen.getByText("Cancel")).toBeInTheDocument();
    expect(screen.getByText("Edit")).toBeInTheDocument();
  });

  it("renders Update Feedback button for FINISHED status", () => {
    renderWithStore({ ...mockReservation, status: "FINISHED" }, "location-1");

    expect(screen.getByText("Update Feedback")).toBeInTheDocument();
  });

  it("renders Leave Feedback button for IN_PROGRESS status", () => {
    renderWithStore(
      { ...mockReservation, status: "IN_PROGRESS" },
      "location-1"
    );

    expect(screen.getByText("Leave Feedback")).toBeInTheDocument();
  });

  it("does not render buttons for CANCELLED status", () => {
    renderWithStore({ ...mockReservation, status: "CANCELLED" }, "location-1");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    expect(screen.queryByText("Edit")).not.toBeInTheDocument();
    expect(screen.queryByText("Update Feedback")).not.toBeInTheDocument();
    expect(screen.queryByText("Leave Feedback")).not.toBeInTheDocument();
  });

  it("calls onCancel when Cancel button is clicked", () => {
    const onCancelMock = vi.fn();
    render(
      <Provider store={mockStore({ locations: { locations: mockLocation } })}>
        <ReservationCard
          reservation={mockReservation}
          locationId="location-1"
          onCancel={onCancelMock}
        />
      </Provider>
    );

    fireEvent.click(screen.getByText("Cancel"));
    expect(onCancelMock).toHaveBeenCalled();
  });

  it("calls onEdit when Edit button is clicked", () => {
    const onEditMock = vi.fn();
    render(
      <Provider store={mockStore({ locations: { locations: mockLocation } })}>
        <ReservationCard
          reservation={mockReservation}
          locationId="location-1"
          onEdit={onEditMock}
        />
      </Provider>
    );

    fireEvent.click(screen.getByText("Edit"));
    expect(onEditMock).toHaveBeenCalled();
  });

  it("calls onFeedback when feedback button is clicked", () => {
    const onFeedbackMock = vi.fn();
    render(
      <Provider store={mockStore({ locations: { locations: mockLocation } })}>
        <ReservationCard
          reservation={{ ...mockReservation, status: "FINISHED" }}
          locationId="location-1"
          onFeedback={onFeedbackMock}
        />
      </Provider>
    );

    fireEvent.click(screen.getByText("Update Feedback"));
    expect(onFeedbackMock).toHaveBeenCalled();
  });
});
