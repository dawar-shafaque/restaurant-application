import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import StarRating from "../components/StarRating"; // Adjust the import path based on your project setup

describe("StarRating Component", () => {
  it("renders 5 star icons", () => {
    // Arrange
    const setRatingMock = vi.fn();
    render(<StarRating rating={0} setRating={setRatingMock} />);

    // Assert
    const stars = screen.getAllByTestId("ratingButton");
    expect(stars).toHaveLength(5);
  });

  it("highlights stars up to the given rating", () => {
    // Arrange
    const setRatingMock = vi.fn();
    render(<StarRating rating={3} setRating={setRatingMock} />);

    // Assert
    const stars = screen.getAllByTestId("ratingButton");
    expect(stars[0]).toHaveClass("text-yellow-500"); // Star 1 should be highlighted
    expect(stars[1]).toHaveClass("text-yellow-500"); // Star 2 should be highlighted
    expect(stars[2]).toHaveClass("text-yellow-500"); // Star 3 should be highlighted
    expect(stars[3]).toHaveClass("text-gray-300"); // Star 4 should be gray
    expect(stars[4]).toHaveClass("text-gray-300"); // Star 5 should be gray
  });

  it("sets rating to the clicked star", () => {
    // Arrange
    const setRatingMock = vi.fn();
    render(<StarRating rating={0} setRating={setRatingMock} />);

    // Act
    const stars = screen.getAllByTestId("ratingButton");
    fireEvent.click(stars[3]); // Click on the 4th star

    // Assert
    expect(setRatingMock).toHaveBeenCalledWith(4); // Verify that `setRating` was called with 4
  });

  it("updates highlights dynamically when stars are clicked", () => {
    // Arrange
    const setRatingMock = vi.fn();
    let currentRating = 0; // Maintain a live update path here
    const { rerender } = render(
      <StarRating rating={currentRating} setRating={(newRating) => {
        currentRating = newRating;
        setRatingMock(newRating);
      }} />
    );

    // Act
    const stars = screen.getAllByTestId("ratingButton");
    fireEvent.click(stars[1]); // Click on the second star
    rerender(<StarRating rating={currentRating} setRating={setRatingMock} />); // Rerender with the new rating

    // Assert
    expect(setRatingMock).toHaveBeenCalledWith(2); // Verify the rating set
    expect(stars[0]).toHaveClass("text-yellow-500"); // Star 1 should be highlighted
    expect(stars[1]).toHaveClass("text-yellow-500"); // Star 2 should be highlighted
    expect(stars[2]).toHaveClass("text-gray-300"); // Star 3 should be gray
  });
});