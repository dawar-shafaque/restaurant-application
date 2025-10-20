import { render, screen, fireEvent } from "@testing-library/react";
import { Provider } from "react-redux";
import configureStore from "redux-mock-store";
import TableList from "@/components/TableList";
import { Table } from "@/types/FormData";
import { beforeEach, describe, expect, it } from "vitest";

// Mock Redux store
const mockStore = configureStore([]);

const mockTables: Table[] = [
  {
    id: 1,
    tableNumber: "5",
    guestCapacity: "4",
    availableSlots: ["6:00 PM", "8:00 PM"],
    locationId: "loc123",
    locationAddress: "123 Main St",
    seating:2, 
    image:"data:image/png;base64,someBase64Data",
  },
];

const mockLocation = {
  id: "loc123",
  address: "123 Main St",
  description: "Nice place",
  averageOccupancy: 80,
  totalCapacity: 20,
  rating: "4.5",
  imageUrl: "data:image/png;base64,someBase64Data",
};

describe("TableList Component", () => {
let store: ReturnType<typeof mockStore>;

  beforeEach(() => {
    store = mockStore({
      locations: {
        locations: [mockLocation],
        loading: false,
      },
    });
  });

  it("renders the number of tables", () => {
    render(
      <Provider store={store}>
        <TableList tables={mockTables} date="2025-05-05" />
      </Provider>
    );

    expect(screen.getByText("1 tables available")).toBeInTheDocument();
  });

  it("renders the table image and address", () => {
    render(
      <Provider store={store}>
        <TableList tables={mockTables} date="2025-05-05" />
      </Provider>
    );

    const image = screen.getByAltText("Restaurant") as HTMLImageElement;
    expect(image).toBeInTheDocument();
    expect(image.src).toContain("data:image/png;base64");

    expect(screen.getByText("123 Main St")).toBeInTheDocument();
  });

  it("renders available time slots as buttons", () => {
    render(
      <Provider store={store}>
        <TableList tables={mockTables} date="2025-05-05" />
      </Provider>
    );

    expect(screen.getByText("6:00 PM")).toBeInTheDocument();
    expect(screen.getByText("8:00 PM")).toBeInTheDocument();
  });

  it("opens modal on clicking a time slot", () => {
    render(
      <Provider store={store}>
        <TableList tables={mockTables} date="2025-05-05" />
      </Provider>
    );

    fireEvent.click(screen.getByText("6:00 PM"));

    // Modal content verification
    expect(screen.getAllByText("123 Main St")[0]).toBeInTheDocument(); // address inside modal
    expect(screen.getByTestId("guestCount")).toHaveTextContent("1"); // guest count input

  });
});
