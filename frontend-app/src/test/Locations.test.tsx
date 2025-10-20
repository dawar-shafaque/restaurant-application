import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach, Mock } from "vitest";
import userEvent from "@testing-library/user-event";
import Locations from "@/components/Locations";
import { Provider } from "react-redux";
import { MemoryRouter, useNavigate } from "react-router-dom";
import { configureStore } from "@reduxjs/toolkit";
import locationsReducer from "@/redux/slice"; // adjust this import to your actual slice
import { API } from "@/api/endpoints";

// Mock navigate
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: vi.fn(),
  };
});

// Mock fetch globally
global.fetch = vi.fn();

describe("Locations Component", () => {
  const mockLocations = [
    {
      address: "New York",
      averageOccupancy: 30,
      id: "1",
      totalCapacity: 50,
      imageUrl: "https://via.placeholder.com/150",
    },
    {
      address: "Los Angeles",
      averageOccupancy: 25,
      id: "2",
      totalCapacity: 40,
      imageUrl: "https://via.placeholder.com/150",
    },
  ];

  beforeEach(() => {
    (fetch as Mock).mockResolvedValueOnce({
      json: async () => mockLocations,
    });

    vi.clearAllMocks();
  });

  const renderWithStore = (ui: React.ReactNode) => {
    const store = configureStore({
      reducer: {
        locations: locationsReducer,
      },
    });

    return render(
      <Provider store={store}>
        <MemoryRouter>{ui}</MemoryRouter>
      </Provider>
    );
  };

  it("should render loading state initially", async () => {
    renderWithStore(<Locations />);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it("should fetch and display location cards", async () => {
    renderWithStore(<Locations />);

    await waitFor(() => {
      expect(screen.getByText(/New York/i)).toBeInTheDocument();
      expect(screen.getByText(/Los Angeles/i)).toBeInTheDocument();
    });
  });

  it("should navigate on location card click", async () => {
    const navigateMock = vi.fn();
    (useNavigate as Mock).mockReturnValue(navigateMock);

    renderWithStore(<Locations />);

    await waitFor(() => {
      expect(screen.getByText("New York")).toBeInTheDocument();
    });

    userEvent.click(screen.getByText("New York"));

    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith("/location/1");
    });
  });

  it("should call the correct API endpoint", async () => {
    renderWithStore(<Locations />);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(API.LOCATIONS, expect.any(Object));
    });
  });
  
});
