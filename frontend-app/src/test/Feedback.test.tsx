import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import FeedbackModal from "@/components/Feedback";
import { Provider } from "react-redux";
import configureStore from "redux-mock-store";
import { API } from "@/api/endpoints";

const mockStore = configureStore([]);
const mockedOnClose = vi.fn();
const mockedOnSubmit = vi.fn();

vi.mock("@/api/endpoints", () => ({
  API: {
    FEEDBACKS_POST: "/feedbacks",
  },
}));

describe("FeedbackModal Component", () => {
    type ReservationStatus =
    | "RESERVED"
    | "IN_PROGRESS"
    | "FINISHED"
    | "CANCELLED";
  const reservationMock = {
    id: "res1",
    waiterEmail: "waiter@example.com",
    location: "Main Hall",
    locationAddress: "123 Main Street",
    locationId: "loc1",
    reservationId: "res1",
    date: "2023-10-01",
    timeFrom: "18:00",
    timeTo: "20:00",
    tableNumber: 5,
    guestCount: 4,
    guestsNumber: 4,
    specialRequests: "Vegetarian meal",
    status: "RESERVED" as ReservationStatus,
  };

  const renderFeedbackModal = (open = true) =>
    render(
      <Provider store={mockStore({})}>
        <FeedbackModal
          open={open}
          onClose={mockedOnClose}
          onSubmit={mockedOnSubmit}
          reservation={reservationMock}
        />
      </Provider>
    );

  it("renders the modal when open is true", () => {
    renderFeedbackModal();
    expect(screen.getByText("Give Feedback")).toBeInTheDocument();
  });

  it("does not render the modal when open is false", () => {
    renderFeedbackModal(false);
    expect(screen.queryByText("Give Feedback")).not.toBeInTheDocument();
  });

  it("displays the service tab by default", () => {
    renderFeedbackModal();
    expect(screen.getByText("Service")).toHaveClass("font-bold");
    expect(screen.getByText("Culinary Experience")).not.toHaveClass(
      "font-bold"
    );
  });

  it("switches to the culinary tab when clicked", () => {
    renderFeedbackModal();
    const culinaryTab = screen.getByText("Culinary Experience");
    fireEvent.click(culinaryTab);
    expect(culinaryTab).toHaveClass("font-bold");
    expect(screen.getByText("Service")).not.toHaveClass("font-bold");
  });

  it("updates service rating correctly", () => {
    renderFeedbackModal();
    const stars = screen.getAllByRole("button", { name: /Rate \d stars/i });
    fireEvent.click(stars[3]); // Click the 4th star
    expect(screen.getByText("4/5 stars")).toBeInTheDocument();
  });

  it("updates culinary rating correctly", () => {
    renderFeedbackModal();
    const culinaryTab = screen.getByText("Culinary Experience");
    fireEvent.click(culinaryTab);

    const stars = screen.getAllByRole("button", { name: /Rate \d stars/i });
    fireEvent.click(stars[2]); // Click the 3rd star
    expect(screen.getByText("3/5 stars")).toBeInTheDocument();
  });

  it("updates service comment correctly", () => {
    renderFeedbackModal();
    const textarea = screen.getByPlaceholderText("Add your comment");
    fireEvent.change(textarea, { target: { value: "Great service!" } });
    expect(textarea).toHaveValue("Great service!");
  });

  it("updates culinary comment correctly", () => {
    renderFeedbackModal();
    const culinaryTab = screen.getByText("Culinary Experience");
    fireEvent.click(culinaryTab);

    const textarea = screen.getByPlaceholderText("Add your comment");
    fireEvent.change(textarea, { target: { value: "Delicious food!" } });
    expect(textarea).toHaveValue("Delicious food!");
  });

  it("calls onSubmit with feedback data when submitted", () => {
    renderFeedbackModal();

    const stars = screen.getAllByRole("button", { name: /Rate \d stars/i });
    fireEvent.click(stars[4]); // Set service rating to 5

    const textarea = screen.getByPlaceholderText("Add your comment");
    fireEvent.change(textarea, { target: { value: "Excellent service!" } });

    const submitButton = screen.getByText("Submit Feedback");
    fireEvent.click(submitButton);

    expect(mockedOnSubmit).toHaveBeenCalledWith({
      serviceRating: 5,
      serviceComment: "Excellent service!",
      cuisineRating: 0,
      cuisineComment: "",
    });
  });

  it("resets feedback and calls onClose after submission", () => {
    renderFeedbackModal();

    const stars = screen.getAllByRole("button", { name: /Rate \d stars/i });
    fireEvent.click(stars[4]); // Set service rating to 5

    const textarea = screen.getByPlaceholderText("Add your comment");
    fireEvent.change(textarea, { target: { value: "Excellent service!" } });

    const submitButton = screen.getByText("Submit Feedback");
    fireEvent.click(submitButton);

    expect(mockedOnClose).toHaveBeenCalled();
    expect(screen.getByText("0/5 stars")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Add your comment")).toHaveValue("");
  });

  it("fetches existing feedback when modal opens", async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            serviceRating: 4,
            serviceComment: "Good service",
            cuisineRating: 5,
            cuisineComment: "Amazing food",
          }),
      })
    );
    global.fetch = mockFetch as unknown as typeof fetch;

    renderFeedbackModal();

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        `${API.FEEDBACKS_POST}/${reservationMock.id}`,
        expect.any(Object)
      );
      expect(screen.getByText("4/5 stars")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("Add your comment")).toHaveValue(
        "Good service"
      );
    });
  });

  it("handles fetch error gracefully", async () => {
    // Mock `fetch` to throw an error
    const mockFetch = vi.fn(() => Promise.reject(new Error("Fetch error")));
    global.fetch = mockFetch as unknown as typeof fetch;
  
    // Mock `console.error` to track error logs
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  
    // Render the modal
    renderFeedbackModal();
  
    // Wait for the fetch call and error handling
    await waitFor(() => {
      // Assert that `fetch` was called
      expect(mockFetch).toHaveBeenCalled();
  
      // Assert that `console.error` was called to log the error
      expect(consoleErrorSpy).toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.any(Error));
    });
  
    // Restore the original `console.error` implementation
    consoleErrorSpy.mockRestore();
  });
});