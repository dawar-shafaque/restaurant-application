import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import configureStore from "redux-mock-store";
import {thunk} from "redux-thunk";
import userEvent from "@testing-library/user-event";
import BookTable from "@/pages/BookTable";
import { afterEach, beforeEach, describe, expect, it, Mock, vi } from "vitest";

// Mock the API endpoint
global.fetch = vi.fn();

const mockStore = configureStore([thunk as never]);

describe("BookTable Component", () => {
  let store: ReturnType<typeof mockStore>;

  beforeEach(() => {
    store = mockStore({
      tables: {
        tables: [],
      },
      locations: {
        locationsOption: [],
      },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should render all fields and buttons correctly", () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );

    // Assert that the Navbar, dropdown, date picker, time picker, guest counter, and button render
    expect(screen.getByText("Green & Tasty Restaurants")).toBeInTheDocument();
    expect(screen.getByText("Find a Table")).toBeInTheDocument();
    expect(screen.getByText("Location")).toBeInTheDocument(); // Initial location dropdown title
    expect(screen.getByTestId(/guests/i)).toBeInTheDocument();
  });

  it("should fetch and display locations in location dropdown", async () => {
    // Mock fetch response for locations
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { id: "1", address: "123 Example St" },
        { id: "2", address: "456 Sunset Blvd" },
      ],
    });

    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );

    // Wait for locations to be fetched and rendered in dropdown
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/select-options"),
        expect.anything()
      );
    });

    const dropdownButton = screen.getByText("Location");
    await userEvent.click(dropdownButton);

    // After dropdown is opened, locations should appear
    expect(screen.getByText("123 Example St")).toBeInTheDocument();
    expect(screen.getByText("456 Sunset Blvd")).toBeInTheDocument();
  });

  it("should select a location from the dropdown", async () => {
    // Mock fetch response for locations
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { id: "1", address: "123 Example St" },
        { id: "2", address: "456 Sunset Blvd" },
      ],
    });

    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() =>
      expect(screen.getByText("Location")).toBeInTheDocument()
    );

    // Click on dropdown and select location
    await userEvent.click(screen.getByText("Location"));
    const locationOption = screen.getByText("123 Example St");
    await userEvent.click(locationOption);

    // Assert that the location is selected
    expect(screen.getByText("123 Example St")).toBeInTheDocument();
  });

  it("should update date and time inputs correctly", () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );

    // Update date picker
    const dateInput = screen.getByLabelText(
      (_, element) =>
        element?.tagName.toLowerCase() === "input" &&
        element.getAttribute("type") === "date"
    );
    fireEvent.change(dateInput, { target: { value: "2023-12-25" } });
    expect(dateInput).toHaveValue("2023-12-25");

    // Update time picker
    const timeInput = screen.getByLabelText(
      (_, element) =>
        element?.tagName.toLowerCase() === "input" &&
        element.getAttribute("type") === "time"
    );
    fireEvent.change(timeInput, { target: { value: "18:30" } });
    expect(timeInput).toHaveValue("18:30");
  });

  it("should increment and decrement guest counter", async () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );

    // Find increment and decrement buttons
    const incrementButton = screen.getByTestId(/increment/i);
    const decrementButton = screen.getByTestId(/decrement/i);
    const guestCount = screen.getByTestId(/guestCount/i);

    // Assert initial guest count
    expect(guestCount).toHaveTextContent("1");
    // Increment guest count
    await userEvent.click(incrementButton);
    expect(guestCount).toHaveTextContent("2");

    // Decrement guest count
    await userEvent.click(decrementButton);
    expect(screen.getByText("1")).toBeInTheDocument();
  });

  it("should dispatch fetchTables with the correct parameters on Find a Table click", async () => {
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { id: "1", address: "123 Example St" },
        { id: "2", address: "456 Sunset Blvd" },
      ],
    });

    render(
      <Provider store={store}>
        <MemoryRouter>
          <BookTable />
        </MemoryRouter>
      </Provider>
    );
    const locationDropdownTrigger = await screen.findByText(/location/i); // Grab the dropdown button labeled "Location"
    await userEvent.click(locationDropdownTrigger); // Open the dropdown

    // Wait for options to appear and select one
    await waitFor(() => {
      expect(screen.getByText("123 Example St")).toBeInTheDocument();
    });
    const locationOption = screen.getByText("123 Example St");
    userEvent.click(locationOption);
    // Set date and time
    const dateInput = screen.getByLabelText(
      (_, el) =>
        el?.tagName === "INPUT" && (el as HTMLInputElement).type === "date"
    );
    fireEvent.change(dateInput, { target: { value: "2023-12-25" } });

    const timeInput = screen.getByLabelText(
      (_, el) =>
        el?.tagName === "INPUT" && (el as HTMLInputElement).type === "time"
    );
    fireEvent.change(timeInput, { target: { value: "19:00" } });

    // Click button to dispatch
    await userEvent.click(screen.getByText("Find a Table"));

    await waitFor(() => {
      const actions = store.getActions();
      expect(actions).toContainEqual(
        expect.objectContaining({
          type: "tables/fetchTables/pending",
          meta: expect.objectContaining({
            arg: {
              locationId: "1",
              date: "2023-12-25",
              guests: 1,
              time: "19:00",
            },
          }),
        })
      );
    });
  });
});
