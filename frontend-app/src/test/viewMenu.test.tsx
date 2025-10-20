import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { vi, describe, it, expect, beforeEach } from "vitest";
import MenuPage from "@/pages/ViewMenu";
import { toast } from "react-toastify";

vi.mock("react-toastify", () => ({
  toast: {
    error: vi.fn(),
  },
}));

vi.mock("@/api/endpoints", () => ({
  API: {
    GET_DISHES: `${import.meta.env.VITE_BASE_URL}${
      import.meta.env.VITE_DISHES
    }`,
  },
}));

describe("MenuPage Component", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.setItem("token", "mock-token");
  });

  it("renders the Navbar and header image", () => {
    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Green & Tasty Restaurants/i)).toBeInTheDocument();
    expect(screen.getByText(/Menu/i)).toBeInTheDocument();
  });

  it("renders filter and sort options", () => {
    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Appetizers/i)).toBeInTheDocument();
    expect(screen.getByText(/Main Courses/i)).toBeInTheDocument();
    expect(screen.getByText(/Desserts/i)).toBeInTheDocument();
    expect(screen.getByText(/Sort by:/i)).toBeInTheDocument();
  });

  it("fetches and displays dishes based on filter and sort options", async () => {
    const mockDishes = [
      {
        id: "1",
        name: "Dish 1",
        price: "10.99",
        weight: "200g",
        previewImageUrl: "mock-image-url",
        available: true,
      },
    ];
    global.fetch = vi.fn().mockResolvedValue({
      json: vi.fn().mockResolvedValue(mockDishes),
    });

    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Dish 1/i)).toBeInTheDocument();
      expect(screen.getByText(/\$10.99/i)).toBeInTheDocument();
    });
  });

  it("handles API errors gracefully", async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error("API Error"));

    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("API Error");
    });
  });

  it("displays modal with dish details when a card is clicked", async () => {
    const mockDishes = [
      {
        id: "1",
        name: "Dish 1",
        price: "10.99",
        weight: "200g",
        previewImageUrl: "mock-image-url",
        state: true,
      },
    ];
    const mockDishDetails = {
      id: "1",
      name: "Dish 1",
      price: "10.99",
      weight: "200g",
      imageUrl: "mock-image-url",
      description: "Delicious dish",
      calories: "300",
      proteins: "10g",
      fats: "5g",
      carbohydrates: "20g",
      vitamins: "A, B, C",
    };

    global.fetch = vi
      .fn()
      .mockResolvedValueOnce({
        json: vi.fn().mockResolvedValue(mockDishes),
      })
      .mockResolvedValueOnce({
        json: vi.fn().mockResolvedValue(mockDishDetails),
      });

    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Dish 1/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Dish 1/i));

    await waitFor(() => {
      expect(screen.getByText(/Delicious dish/i)).toBeInTheDocument();
      expect(screen.getByText(/Calories:/i)).toBeInTheDocument();
      expect(screen.getByText(/300/i)).toBeInTheDocument();
    });
  });

  it("closes the modal when the close button is clicked", async () => {
    const mockDishes = [
      {
        id: "1",
        name: "Dish 1",
        price: "10.99",
        weight: "200g",
        previewImageUrl: "mock-image-url",
        available: true,
      },
    ];
    const mockDishDetails = {
      id: "1",
      name: "Dish 1",
      price: "10.99",
      weight: "200g",
      imageUrl: "mock-image-url",
      description: "Delicious dish",
      calories: "300",
      proteins: "10g",
      fats: "5g",
      carbohydrates: "20g",
      vitamins: "A, B, C",
    };

    global.fetch = vi
      .fn()
      .mockResolvedValueOnce({
        json: vi.fn().mockResolvedValue(mockDishes),
      })
      .mockResolvedValueOnce({
        json: vi.fn().mockResolvedValue(mockDishDetails),
      });

    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Dish 1/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Dish 1/i));

    await waitFor(() => {
      expect(screen.getByText(/Delicious dish/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText(/Close modal/i));

    await waitFor(() => {
      expect(screen.queryByText(/Delicious dish/i)).not.toBeInTheDocument();
    });
  });
});
