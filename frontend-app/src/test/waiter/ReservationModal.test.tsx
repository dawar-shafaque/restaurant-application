import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { Provider } from "react-redux";
import configureStore from "redux-mock-store";
import { ReservationModal } from "@/components/waiter/ReservationModal"; // Adjust the path based on your project setup
import { toast } from "react-toastify";

const mockStore = configureStore([]);
const mockedDispatch = vi.fn();

vi.mock("react-redux", async () => {
  const actual = await vi.importActual("react-redux");
  return {
    ...actual,
    useDispatch: () => mockedDispatch,
  };
});

vi.mock("../api/endpoints", () => ({
  API: {
    CUSTOMERS: "/customers", // Replace with your API paths
    WAITER_BOOKING: "/bookings/waiter",
  },
}));

describe("ReservationModal Component", () => {
  const initialStoreState = {
    locationsOption: {
      locationsDev: [
        { id: "loc1", address: "Test Location 1" },
        { id: "loc2", address: "Test Location 2" },
      ],
      loading: false,
      error: null,
    },
  };

  const renderModal = (isOpen = true, onClose = vi.fn()) =>
    render(
      <Provider store={mockStore(initialStoreState)}>
        <ReservationModal isOpen={isOpen} onClose={onClose} />
      </Provider>
    );

  it("renders the modal when isOpen is true", () => {
    renderModal();
    expect(screen.getByText("New Reservation")).toBeInTheDocument();
  });

  it("does not render the modal when isOpen is false", () => {
    renderModal(false);
    expect(screen.queryByText("New Reservation")).not.toBeInTheDocument();
  });

  it("calls onClose when the close button is clicked", () => {
    const onCloseMock = vi.fn();
    renderModal(true, onCloseMock);

    const closeButton = screen.getByTestId("close");
    fireEvent.click(closeButton);
    expect(onCloseMock).toHaveBeenCalled();
  });

  it("displays loading text when locations are being fetched", () => {
    render(
      <Provider
        store={mockStore({
          locationsOption: { locationsDev: [], loading: true, error: null },
        })}
      >
        <ReservationModal isOpen={true} onClose={vi.fn()} />
      </Provider>
    );
    expect(screen.getByText("Loading locations...")).toBeInTheDocument();
  });

  it("displays error if an error occurs while fetching locations", () => {
    render(
      <Provider
        store={mockStore({
          locationsOption: {
            locationsDev: [],
            loading: false,
            error: "Test Error",
          },
        })}
      >
        <ReservationModal isOpen={true} onClose={vi.fn()} />
      </Provider>
    );
    expect(screen.getByText("Error loading locations")).toBeInTheDocument();
  });

  it("renders location dropdown with options", () => {
    renderModal();

    const locationSelect = screen.getAllByRole("combobox")[0];
    fireEvent.change(locationSelect, { target: { value: "loc1" } });
    expect(locationSelect).toHaveValue("loc1");
  });

  it("updates the guest count correctly", () => {
    renderModal();

    const decrementButton = screen.getByRole("button", { name: "−" });
    const incrementButton = screen.getByRole("button", { name: "+" });
    const guestText = screen.getByText("1");

    // Increment
    fireEvent.click(incrementButton);
    expect(screen.getByText("2")).toBeInTheDocument();

    // Decrement
    fireEvent.click(decrementButton);
    expect(guestText).toBeInTheDocument(); // Should return to 1
  });

  it("displays customer search input and filters results dynamically", async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(["John Doe", "Jane Smith"]),
        headers: new Headers(),
        redirected: false,
        status: 200,
        statusText: "OK",
        type: "basic",
        url: "",
        clone: () => this,
        body: null,
        bodyUsed: false,
        arrayBuffer: async () => new ArrayBuffer(0),
        blob: async () => new Blob(),
        formData: async () => new FormData(),
        text: async () => "",
      })
    );

    global.fetch = mockFetch as unknown as typeof fetch;

    renderModal();
    const visitorRadio = screen.getByLabelText(/Existing Customer/i);
    fireEvent.click(visitorRadio);

    const searchInput = screen.getByPlaceholderText("Enter Customer’s Name");
    fireEvent.change(searchInput, { target: { value: "Jo" } });

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/customers?name=Jo"),
        expect.anything()
      );
      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("Jane Smith")).toBeInTheDocument();
    });
  });

  it("displays error message if reservation fails", async () => {
    const toastInfoSpy = vi.spyOn(toast, "error");
    const mockFetch = vi.fn(() =>
      Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: "Failed to make reservation" }),
      })
    );

    global.fetch = mockFetch as unknown as typeof fetch;

    renderModal();

    const submitButton = screen.getByRole("button", {
      name: /Make a Reservation/i,
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(toastInfoSpy).toHaveBeenCalledWith("Failed to make reservation");
    });
  });

  it("submits the reservation request successfully", async () => {
    const mockFetch = vi.fn((url) => {
      if (url.includes("/bookings/waiter")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({ message: "Reservation made successfully!" }),
        }) as unknown as Promise<Response>;
      }
      return Promise.reject(
        new Response("Not Found", { status: 404 })
      ) as unknown as Promise<Response>;
    });
    global.fetch = mockFetch as unknown as typeof fetch;
  
    const onCloseMock = vi.fn();
  
    // Render the modal
    renderModal(true, onCloseMock);
  
    // Fill required inputs
    const locationSelect = screen.getAllByRole("combobox")[0];
    fireEvent.change(locationSelect, { target: { value: "loc1" } });
  
    const dateInput = screen.getByLabelText(/Date/i);
    fireEvent.change(dateInput, { target: { value: "2025-05-08" } });
  
    const submitButton = screen.getByRole("button", {
      name: /Make a Reservation/i,
    });
    fireEvent.click(submitButton);
  
    // Assert that fetch is correctly called and modal closes
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/bookings/waiter"),
        expect.any(Object)
      );
      expect(onCloseMock).toHaveBeenCalled(); // This assertion will now pass
    });
  });
});
