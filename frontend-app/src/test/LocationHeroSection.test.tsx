import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Provider, useSelector } from "react-redux";
import { describe, test, vi, expect, beforeEach } from "vitest";
import LocationHeroSection from "@/components/LocationHeroSection";
import { configureStore } from "@reduxjs/toolkit";
import locationsReducer from "@/redux/slice";

const mockStore = configureStore({
  reducer: {
    locations: locationsReducer,
  },
});

vi.mock("react-redux", async () => {
  const actual = await vi.importActual("react-redux");
  return {
    ...actual,
    useSelector: vi.fn(),
  };
});

describe("LocationHeroSection", () => {
  const mockLocation = {
    id: "1",
    address: "123 Green Street",
    rating: 4.5,
    description: "A cozy place with delicious food.",
    imageUrl: "https://example.com/image.jpg",
  };

  beforeEach(() => {
    vi.mocked(useSelector).mockImplementation((selectorFn) =>
      selectorFn({
        locations: {
          locations: [mockLocation],
          loading: false,
        },
      })
    );
  });

  test("renders location details correctly", () => {
    render(
      <Provider store={mockStore}>
        <MemoryRouter initialEntries={["/location/1"]}>
          <Routes>
            <Route path="/location/:id" element={<LocationHeroSection />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    expect(screen.getByText("Green & tasty")).toBeInTheDocument();
    expect(screen.getByText(mockLocation.address)).toBeInTheDocument();
    expect(screen.getByText(mockLocation.description)).toBeInTheDocument();
    expect(screen.getByText(mockLocation.rating.toString())).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Image" })).toHaveAttribute("src", mockLocation.imageUrl);
    expect(screen.getByRole("link", { name: "Book a Table" })).toHaveAttribute("href", "/bookTable");
  });

  test("renders fallback UI when location is not found", () => {
    vi.mocked(useSelector).mockImplementation((selectorFn) =>
        selectorFn({
          locations: {
            locations: [],
            loading: false,
          },
        })
      );

    render(
      <Provider store={mockStore}>
        <MemoryRouter initialEntries={["/location/2"]}>
          <Routes>
            <Route path="/location/:id" element={<LocationHeroSection />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    expect(screen.queryByText(mockLocation.address)).not.toBeInTheDocument();
    expect(screen.queryByText(mockLocation.description)).not.toBeInTheDocument();
  });
});