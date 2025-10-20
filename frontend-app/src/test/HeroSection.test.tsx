import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, useNavigate } from "react-router-dom";
import { HeroSection } from "@/components/HeroSection";
import { describe, expect, it, vi } from "vitest";
import headingImage from '../assets/headingImage.jpg'

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: vi.fn(),
  };
});

describe("HeroSection Component", () => {
  it("renders the hero section with correct content", () => {
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    expect(screen.getByText(/Fresh, Organic, & Delicious/i)).toBeInTheDocument();
    expect(
      screen.getByText(
        /Experience the best farm-to-table dining with carefully crafted meals made from the freshest ingredients./i
      )
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /View Menu/i })).toBeInTheDocument();
  });

  it("navigates to the menu page when 'View Menu' button is clicked", () => {
    const navigate = vi.fn();
    vi.mocked(useNavigate).mockReturnValue(navigate);

    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    const viewMenuButton = screen.getByRole("button", { name: /View Menu/i });
    fireEvent.click(viewMenuButton);

    expect(navigate).toHaveBeenCalledWith("/view-menu");
  });

  it("applies correct styles to the background image and overlay", () => {
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    const backgroundImage = screen.getByTestId(/backgroundImage/i);
    expect(backgroundImage).toHaveStyle(
      `background-image: url(${headingImage})`
    );

    const overlay = screen.getByTestId("overlay");
    expect(overlay).toHaveClass("bg-gradient-to-b from-black/60 via-black/40 to-black/70");
  });
});