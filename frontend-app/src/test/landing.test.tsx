import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, beforeEach, vi, expect } from "vitest";
import Landing from "@/pages/Landing";

// Mocking static components (Navbar, HeroSection, PopularDishes)
vi.mock("@/components/HeroSection", () => ({
  HeroSection: () => <div data-testid="hero-section">HeroSection</div>,
}));

vi.mock("@/components/Navbar", () => ({
  Navbar: () => <div data-testid="navbar">Navbar</div>,
}));

vi.mock("@/components/PopularDishes", () => ({
  PopularDishes: () => <div data-testid="popular-dishes">PopularDishes</div>,
}));

// Mocking the lazy-loaded component (Locations)
vi.mock("@/components/Locations", async () => {
    // Simulate loading delay for lazy component
    await new Promise((resolve) => setTimeout(resolve, 50));
  
    return {
      default: () => <div data-testid="locations">Locations</div>,
    };
  });

describe("Landing Component", () => {
  beforeEach(() => {
    vi.clearAllMocks(); // Reset mock state before each test
  });

  it("renders the Navbar, HeroSection, and PopularDishes components", () => {
    render(<Landing />);

    // Check if static components are rendered
    expect(screen.getByTestId("navbar")).toBeInTheDocument();
    expect(screen.getByTestId("hero-section")).toBeInTheDocument();
    expect(screen.getByTestId("popular-dishes")).toBeInTheDocument();
  });

  it("shows the fallback loading message while the lazy-loaded Locations component is being loaded", async () => {
    render(<Landing />);

    // Check if the LoadingContainer fallback is displayed
    expect(screen.getByTestId("LoadingContainer")).toBeInTheDocument();

    // Since Locations is lazy-loaded, ensure the fallback disappears after loading
    await waitFor(() => {
      expect(screen.queryByTestId("LoadingContainer")).not.toBeInTheDocument();
    });
  });

  it("renders the lazy-loaded Locations component after loading", async () => {
    render(<Landing />);

    // Wait for the Locations component to load
    const locations = await screen.findByTestId("locations");
    expect(locations).toBeInTheDocument();
  });
});