import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import LocationDetails from "@/pages/LocationDetails";
import { describe, expect, test, vi } from "vitest";

vi.mock("@/components/CustomerReviews", () => ({
  __esModule: true,
  default: ({ id }: { id: string }) => <div>Customer Reviews for {id}</div>,
}));

vi.mock("@/components/LocationHeroSection", () => ({
  __esModule: true,
  default: () => <div>Location Hero Section</div>,
}));

vi.mock("@/components/Navbar", () => ({
  __esModule: true,
  Navbar: () => <div>Navbar</div>,
}));

vi.mock("@/components/SpecialityDishes", () => ({
  __esModule: true,
  SpecialityDishes: ({ id }: { id: string }) => (
    <div>Speciality Dishes for {id}</div>
  ),
}));

describe("LocationDetails", () => {
  test("renders all components with a valid location ID", () => {
    render(
      <MemoryRouter initialEntries={["/location/123"]}>
        <Routes>
          <Route path="/location/:id" element={<LocationDetails />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText("Navbar")).toBeInTheDocument();
    expect(screen.getByText("Location Hero Section")).toBeInTheDocument();
    expect(screen.getByText("Speciality Dishes for 123")).toBeInTheDocument();
    expect(screen.getByText("Customer Reviews for 123")).toBeInTheDocument();
  });

  test("displays error message when location ID is missing", () => {
    render(
      <MemoryRouter initialEntries={["/location"]}>
        <Routes>
          <Route path="/location" element={<LocationDetails />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText("Navbar")).toBeInTheDocument();
    expect(screen.getByText("Location Hero Section")).toBeInTheDocument();
    expect(screen.getAllByText("Error: Location ID is missing.")).toHaveLength(
      2
    );
  });
});