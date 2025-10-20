import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import CustomerReviews from "@/components/CustomerReviews";
import { vi, describe, beforeEach, test, expect, Mock } from "vitest";

// Mock fetch globally
global.fetch = vi.fn();

const mockReviews = {
  content: [
    {
      id: "1",
      userName: "Alice",
      userAvatarUrl: "https://example.com/avatar.jpg",
      date: "2024-05-01T12:00:00",
      rate: 5,
      comment: "Excellent service!",
    },
  ],
  totalPages: 1,
  number: 0,
  size: 6,
};

describe("CustomerReviews", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (fetch as Mock).mockResolvedValue({
      json: async () => mockReviews,
    });
  });

  test("renders component and fetches reviews", async () => {
    render(<CustomerReviews id="123" />);

    expect(await screen.findByText("Alice")).toBeInTheDocument();
    expect(screen.getByText("Excellent service!")).toBeInTheDocument();
    expect(screen.getByText("Rating: 5")).toBeInTheDocument();
  });

  test("renders and toggles tabs", async () => {
    render(<CustomerReviews id="123" />);

    const serviceTab = screen.getByRole("button", { name: /service/i });
    const cuisineTab = screen.getByRole("button", { name: /cuisine experience/i });

    expect(serviceTab).toHaveClass("border-green-500");

    fireEvent.click(cuisineTab);
    await waitFor(() => {
      expect(cuisineTab).toHaveClass("border-green-500");
    });
  });

  test("opens and selects sort option", async () => {
    render(<CustomerReviews id="123" />);

    const sortButton = screen.getByRole("button", { name: /top rated first/i });
    fireEvent.click(sortButton);

    const newestOption = await screen.findByRole("button", { name: /newest first/i });
    fireEvent.click(newestOption);

    expect(await screen.findByText("Alice")).toBeInTheDocument();
  });

  test("pagination buttons work", async () => {
    const paginatedMock = {
      ...mockReviews,
      totalPages: 3,
    };

    (fetch as Mock).mockResolvedValueOnce({
      json: async () => paginatedMock,
    });

    render(<CustomerReviews id="123" />);

    // Wait for first fetch
    await screen.findByText("Alice");

    // Check that pagination buttons are rendered
    expect(screen.getByRole("button", { name: "1" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "2" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "3" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "2" }));

    // Trigger another fetch on pagination change
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });

  test("disables prev/next on page boundaries", async () => {
    render(<CustomerReviews id="123" />);

    await screen.findByText("Alice");

    const prevBtn = screen.getByRole("button", { name: /prev/i });
    const nextBtn = screen.getByRole("button", { name: /next/i });

    expect(prevBtn).toBeDisabled();
    expect(nextBtn).toBeDisabled(); // because only one page
  });
});
