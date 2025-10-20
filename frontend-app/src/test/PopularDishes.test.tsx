import { render, screen, waitFor } from "@testing-library/react";
import { PopularDishes } from "@/components/PopularDishes"; // Adjust the path if needed
import { afterEach, beforeEach, describe, expect, it, Mock, vi } from "vitest";

describe("PopularDishes Component", () => {
  beforeEach(() => {
    // Mock the global fetch API
    global.fetch = vi.fn();
  });

  afterEach(() => {
    // Reset fetch mock after each test
    vi.resetAllMocks();
  });

  it("should render the loading state initially", () => {
    render(<PopularDishes />);
    expect(screen.getByText(/loading.../i)).toBeInTheDocument();
  });

  it("should fetch and render dishes correctly", async () => {
    // Mocking the `fetch` API to return dummy dishes
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        {
          id: "1",
          imageUrl: "https://example.com/image1.jpg",
          locationId: "loc1",
          name: "Dish 1",
          price: "10",
          weight: "200g",
        },
        {
          id: "2",
          imageUrl: "https://example.com/image2.jpg",
          locationId: "loc2",
          name: "Dish 2",
          price: "15",
          weight: "300g",
        },
      ],
    });

    render(<PopularDishes />);

    // Wait for the dishes to load and verify content
    await waitFor(() => expect(screen.getByText("Dish 1")).toBeInTheDocument());
    expect(screen.getByText("Dish 2")).toBeInTheDocument();

    // Check additional properties (price, weight, images)
    expect(screen.getByText("10$")).toBeInTheDocument();
    expect(screen.getByText("200g")).toBeInTheDocument();
    expect(screen.getByText("15$")).toBeInTheDocument();
    expect(screen.getByText("300g")).toBeInTheDocument();

    expect(
      screen.getByAltText("Dish 1").getAttribute("src")
    ).toBe("https://example.com/image1.jpg");
    expect(
      screen.getByAltText("Dish 2").getAttribute("src")
    ).toBe("https://example.com/image2.jpg");
  });

  it("should handle API errors gracefully", async () => {
    // Mock `fetch` to simulate an API error response
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: async () => ({ message: "Internal Server Error" }),
    });

    render(<PopularDishes />);

    // Ensure the loading state is displayed initially
    expect(screen.getByText(/loading.../i)).toBeInTheDocument();

    // Wait for API call to finish
    await waitFor(() => {
      // Ensure dishes are not rendered due to API error
      expect(screen.queryByText("Dish 1")).not.toBeInTheDocument();
      expect(screen.queryByText("Dish 2")).not.toBeInTheDocument();
    });
  });

  it("should render empty state if no dishes are returned", async () => {
    // Mock `fetch` to simulate an empty response from the API
    (global.fetch as Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    });

    render(<PopularDishes />);

    // Wait for the API call to complete
    await waitFor(() => {
      // Verify that no dishes are displayed and no errors are shown
      expect(screen.queryByText(/loading.../i)).not.toBeInTheDocument();
      expect(screen.queryByText("Dish 1")).not.toBeInTheDocument();
      expect(screen.queryByText("Dish 2")).not.toBeInTheDocument();
      expect(
        screen.getByText("Most Popular Dishes")
      ).toBeInTheDocument(); // Still renders header even if empty
    });
  });
});