import { render, screen, waitFor } from "@testing-library/react";
import { SpecialityDishes } from "@/components/SpecialityDishes";
import { afterEach, beforeEach, describe, expect, Mock, test, vi } from "vitest";

vi.mock("@/api/endpoints", () => ({
  API: {
    LOCATIONS: "http://mock-api.com/locations",
  },
}));

describe("SpecialityDishes Component", () => {
  const mockDishes = [
    {
      id: "1",
      imageUrl: "http://example.com/dish1.jpg",
      locationId: "123",
      name: "Dish 1",
      price: "10",
      weight: "200g",
    },
    {
      id: "2",
      imageUrl: "http://example.com/dish2.jpg",
      locationId: "123",
      name: "Dish 2",
      price: "15",
      weight: "250g",
    },
  ];

  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("renders loading state initially", () => {
    (fetch as Mock).mockResolvedValueOnce({
      json: vi.fn().mockResolvedValueOnce([]),
    });

    render(<SpecialityDishes id="123" />);
    expect(screen.getByText(/Loading.../i)).toBeInTheDocument();
  });

  test("renders dishes correctly after fetching", async () => {
    (fetch as Mock).mockResolvedValueOnce({
      json: vi.fn().mockResolvedValueOnce(mockDishes),
    });

    render(<SpecialityDishes id="123" />);

    await waitFor(() => {
      expect(screen.getByText("Dish 1")).toBeInTheDocument();
      expect(screen.getByText("Dish 2")).toBeInTheDocument();
      expect(screen.getByText("10$")).toBeInTheDocument();
      expect(screen.getByText("15$")).toBeInTheDocument();
      expect(screen.getByText("200g")).toBeInTheDocument();
      expect(screen.getByText("250g")).toBeInTheDocument();
    });

    const images = screen.getAllByRole("img");
    expect(images).toHaveLength(2);
    expect(images[0]).toHaveAttribute("src", "http://example.com/dish1.jpg");
    expect(images[0]).toHaveAttribute("alt", "Dish 1");
    expect(images[1]).toHaveAttribute("src", "http://example.com/dish2.jpg");
    expect(images[1]).toHaveAttribute("alt", "Dish 2");
  });

  test("handles fetch errors gracefully", async () => {
    (fetch as Mock).mockRejectedValueOnce(new Error("Network Error"));

    render(<SpecialityDishes id="123" />);

    await waitFor(() => {
      expect(screen.queryByText(/Loading.../i)).not.toBeInTheDocument();
    });

    expect(screen.queryByText("Dish 1")).not.toBeInTheDocument();
    expect(screen.queryByText("Dish 2")).not.toBeInTheDocument();
  });
});